
from datetime import datetime, timezone
from math import isfinite
from threading import Lock
from typing import Any
from event_aggregator import AndroidEventAggregator

from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

import ranscope_m_unified_backend as model_pipeline


app = FastAPI(
    title="RANSCOPE-M API",
    description="Telemetry ingestion and ransomware detection API",
    version="0.2.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:5174",
        "http://127.0.0.1:5174",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_WEIGHTS = {
    "MPVG": 0.18,
    "SFIM": 0.26,
    "EAP": 0.31,
    "CAIT": 0.25,
}
DETECTION_THRESHOLD = 0.50

latest_telemetry: dict[str, Any] | None = None
latest_detection: dict[str, Any] | None = None
latest_android_window: dict[str, Any] | None = None
websocket_clients: set[WebSocket] = set()
android_aggregator = AndroidEventAggregator()

# Retain the most recently received individual Android event.
latest_android_event: dict[str, Any] | None = None

detector = None
model_load_error: str | None = None
detection_lock = Lock()


class TelemetryRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=100)
    timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc)
    )
    telemetry: dict[str, Any]


class DetectionRequest(BaseModel):
    device_id: str = Field(min_length=1, max_length=100)
    timestamp: datetime = Field(
        default_factory=lambda: datetime.now(timezone.utc)
    )
    window: dict[str, Any]


def required_feature_columns() -> list[str]:
    """Return every unique feature required by all four trained models."""
    return sorted({
        column
        for columns in model_pipeline.LAYER_COLUMNS.values()
        for column in columns
    })


REQUIRED_FEATURES = required_feature_columns()


@app.on_event("startup")
async def load_detection_models():
    """Load the pretrained artifact once when the API starts."""
    global detector, model_load_error

    try:
        for layer in model_pipeline.FILES:
            model_pipeline.RUNS.setdefault(layer, {})

        detector = model_pipeline.RansomwareEarlyWarning()
        model_load_error = None
        print("RANSCOPE-M: all four pretrained models loaded.")
    except Exception as exc:
        detector = None
        model_load_error = str(exc)
        print(f"RANSCOPE-M model loading failed: {exc}")


async def broadcast(payload: dict[str, Any]) -> None:
    """Broadcast an update to connected dashboard clients."""
    disconnected = set()

    for client in websocket_clients.copy():
        try:
            await client.send_json(payload)
        except Exception:
            disconnected.add(client)

    websocket_clients.difference_update(disconnected)


def validate_window(window: dict[str, Any]) -> dict[str, Any]:
    """Reject incomplete or non-numeric model feature windows."""
    missing = [
        name for name in REQUIRED_FEATURES
        if name not in window
    ]

    if missing:
        raise HTTPException(
            status_code=422,
            detail={
                "message": (
                    "Incomplete feature window. No model inference was run."
                ),
                "missing_features": missing,
                "required_feature_count": len(REQUIRED_FEATURES),
            },
        )

    validated = dict(window)

    for name in REQUIRED_FEATURES:
        value = validated[name]

        if isinstance(value, bool):
            raise HTTPException(
                status_code=422,
                detail=f"Feature '{name}' must be numeric, not boolean.",
            )

        try:
            numeric_value = float(value)
        except (TypeError, ValueError):
            raise HTTPException(
                status_code=422,
                detail=f"Feature '{name}' must be numeric.",
            )

        if not isfinite(numeric_value):
            raise HTTPException(
                status_code=422,
                detail=f"Feature '{name}' must be finite.",
            )

        validated[name] = numeric_value

    return validated


@app.get("/")
async def root():
    return {
        "project": "RANSCOPE-M",
        "message": "Backend is running",
        "docs": "/docs",
    }


@app.get("/health")
async def health():
    return {
        "status": "healthy",
        "service": "ranscope-m-backend",
        "timestamp": datetime.now(timezone.utc).isoformat(),
    }


@app.get("/api/status")
async def api_status():
    return {
        "backend": "online",
        "telemetry_received": latest_telemetry is not None,
        "detection_available": latest_detection is not None,
        "connected_websocket_clients": len(websocket_clients),
        "model_inference": (
            "ready" if detector is not None else "unavailable"
        ),
        "model_load_error": model_load_error,
        "required_feature_count": len(REQUIRED_FEATURES),
        "model_weights": MODEL_WEIGHTS,
        "threshold": DETECTION_THRESHOLD,
    }


@app.post("/api/telemetry")
async def receive_telemetry(request: TelemetryRequest):
    """Accept and broadcast raw telemetry without inventing a prediction."""
    global latest_telemetry

    latest_telemetry = {
        "device_id": request.device_id,
        "timestamp": request.timestamp.isoformat(),
        "telemetry": request.telemetry,
    }

    event = {
        "event": "telemetry_update",
        "telemetry": latest_telemetry,
        "detection": latest_detection,
    }

    await broadcast(event)

    return {
        "accepted": True,
        "telemetry": latest_telemetry,
        "detection": None,
        "message": (
            "Telemetry received. Submit a complete feature window "
            "to /api/detections/run for model inference."
        ),
    }

@app.post("/api/telemetry/window")
async def receive_android_window(request: TelemetryRequest):
    """Store Android feature windows separately from individual Frida events."""
    global latest_android_window

    telemetry = request.telemetry

    if telemetry.get("event") != "android_window_complete":
        raise HTTPException(
            status_code=422,
            detail="Expected event='android_window_complete'.",
        )

    feature_window = telemetry.get("feature_window")
    if not isinstance(feature_window, dict):
        raise HTTPException(
            status_code=422,
            detail="feature_window must be a JSON object.",
        )

    latest_android_window = {
        "device_id": request.device_id,
        "timestamp": request.timestamp.isoformat(),
        "telemetry": telemetry,
    }

    await broadcast({
        "event": "android_window_update",
        "android_window": latest_android_window,
    })

    return {
        "accepted": True,
        "android_window": latest_android_window,
        "inference_run": False,
        "message": "Android feature window stored; no inference was run.",
    }


# NEW ENDPOINT: individual Android/Frida monitoring events

@app.post("/api/telemetry/event")
async def receive_telemetry_event(request: dict[str, Any]):
    """Receive an Android event and update the in-memory feature aggregator."""
    global latest_telemetry, latest_android_event

    device_id = request.get("device_id", "ranscope-android")
    telemetry_event = request.get("telemetry", {})
    raw_timestamp = request.get("timestamp")

    if not isinstance(device_id, str) or not device_id.strip():
        raise HTTPException(
            status_code=422,
            detail="device_id must be a non-empty string.",
        )

    if not isinstance(telemetry_event, dict):
        raise HTTPException(
            status_code=422,
            detail="telemetry must contain a JSON object.",
        )

    try:
        if raw_timestamp is None:
            timestamp_text = datetime.now(timezone.utc).isoformat()
        elif isinstance(raw_timestamp, (int, float)) and not isinstance(
            raw_timestamp, bool
        ):
            timestamp_text = datetime.fromtimestamp(
                raw_timestamp, timezone.utc
            ).isoformat()
        elif isinstance(raw_timestamp, str):
            parsed_timestamp = datetime.fromisoformat(
                raw_timestamp.replace("Z", "+00:00")
            )
            if parsed_timestamp.tzinfo is None:
                parsed_timestamp = parsed_timestamp.replace(
                    tzinfo=timezone.utc
                )
            timestamp_text = parsed_timestamp.isoformat()
        else:
            raise ValueError("Unsupported timestamp format.")
    except (ValueError, OverflowError, OSError):
        raise HTTPException(
            status_code=422,
            detail="timestamp must be a valid ISO or Unix timestamp.",
        )

    # Add the actual observed event to the running aggregator.
    accepted = android_aggregator.add_event(telemetry_event)

    if not accepted:
        raise HTTPException(
            status_code=422,
            detail="Event must contain valid layer and event fields.",
        )

    snapshot = android_aggregator.snapshot()

    latest_android_event = {
        "device_id": device_id,
        "timestamp": timestamp_text,
        "telemetry": telemetry_event,
    }

    latest_telemetry = {
        "device_id": device_id,
        "timestamp": timestamp_text,
        "telemetry": telemetry_event,
        "aggregation": snapshot,
    }

    await broadcast({
        "event": "telemetry_update",
        "telemetry": latest_telemetry,
        "detection": latest_detection,
    })

    return {
        "accepted": True,
        "device_id": device_id,
        "event_type": telemetry_event.get("event", "unknown"),
        "layer": telemetry_event.get("layer", "unknown"),
        "aggregation": snapshot,
        "inference_run": False,
        "message": (
            "Event aggregated. Model inference remains disabled "
            "until all required features are genuinely available."
        ),
    }



@app.post("/api/detections/run")
async def run_detection(request: DetectionRequest):
    """Run the actual pretrained models on one complete telemetry window."""
    global latest_detection, latest_telemetry

    if detector is None:
        raise HTTPException(
            status_code=503,
            detail={
                "message": "The pretrained models are not available.",
                "reason": model_load_error,
            },
        )

    features = validate_window(request.window)

    timestamp = request.timestamp
    if timestamp.tzinfo is None:
        timestamp = timestamp.replace(tzinfo=timezone.utc)

    timestamp_text = timestamp.isoformat()

    # The detector uses shared per-layer RUNS state internally.
    # Serialize pushes to prevent concurrent requests from mixing that state.
    try:
        with detection_lock:
            output = detector.push(features)
    except Exception as exc:
        raise HTTPException(
            status_code=500,
            detail=f"Model inference failed: {type(exc).__name__}: {exc}",
        )

    layer_probabilities = {
        name: float(output["p_layers"][name])
        for name in MODEL_WEIGHTS
    }

    final_probability = float(output["p_final"])
    verdict = (
        "MALICIOUS"
        if final_probability >= DETECTION_THRESHOLD
        else "BENIGN"
    )

    latest_telemetry = {
        "device_id": request.device_id,
        "timestamp": timestamp_text,
        "telemetry": features,
    }

    latest_detection = {
        "device_id": request.device_id,
        "timestamp": timestamp_text,
        "model_outputs": {
            name.lower(): probability
            for name, probability in layer_probabilities.items()
        },
        "model_weights": {
            name.lower(): weight
            for name, weight in MODEL_WEIGHTS.items()
        },
        "final_probability": round(final_probability, 6),
        "risk_level": verdict,
        "threshold": DETECTION_THRESHOLD,
        "alert": bool(output["alert"]),
        "window": int(output["window"]),
        "inference_status": "real_model_inference",
    }

    await broadcast({
        "event": "detection_update",
        "telemetry": latest_telemetry,
        "detection": latest_detection,
    })

    return {
        "accepted": True,
        "telemetry": latest_telemetry,
        "detection": latest_detection,
        "message": "Inference completed using the four pretrained models.",
    }


@app.get("/api/telemetry/latest")
async def get_latest_telemetry():
    return {
        "telemetry": latest_telemetry,
        "android_window": latest_android_window,
    }


@app.get("/api/detections/latest")
async def get_latest_detection():
    return {"detection": latest_detection}


@app.websocket("/ws/live")
async def live_updates(websocket: WebSocket):
    await websocket.accept()
    websocket_clients.add(websocket)

    try:
        await websocket.send_json({
            "event": "connection",
            "message": "Connected to RANSCOPE-M live updates",
            "telemetry": latest_telemetry,
            "detection": latest_detection,
        })

        while True:
            await websocket.receive_text()

    except WebSocketDisconnect:
        pass
    finally:
        websocket_clients.discard(websocket)

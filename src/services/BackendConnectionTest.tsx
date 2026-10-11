
import { useCallback, useEffect, useState } from "react";
import {
  connectLiveUpdates,
  getBackendStatus,
  runDetection,
  type BackendStatus,
  type DetectionData,
  type TelemetryEvent,
} from "./api";

type TelemetryRow = {
  id: string;
  deviceId: string;
  timestamp: string;
  eventType: string;
  fields: Record<string, unknown>;
};

const REQUIRED_FEATURES = [
  "CAIT_camera_id_list_count",
  "CAIT_camera_open_count",
  "EAP_cipher_doFinal_count",
  "MPVG_checkSelfPermission_count",
  "MPVG_requestPermissions_count",
  "SFIM_file_delete_count",
  "SFIM_file_read_open_count",
  "SFIM_file_rename_count",
  "SFIM_file_write_open_count",
  "avg_depth",
  "avg_entropy",
  "close_write_count",
  "create_count",
  "csv_count",
  "delete_count",
  "docx_count",
  "entropy_observation_count",
  "file_event_count",
  "jpg_count",
  "json_count",
  "log_count",
  "max_entropy",
  "md_count",
  "modify_count",
  "moved_from_count",
  "moved_to_count",
  "pdf_count",
  "png_count",
  "txt_count",
  "unique_path_count",
];

const MODEL_LABELS: Record<string, string> = {
  mpvg: "MPVG",
  sfim: "SFIM",
  eap: "EAP",
  cait: "CAIT",
};

function numericWindow(
  fields: Record<string, unknown>,
): Record<string, number> | null {
  const result: Record<string, number> = {};

  for (const feature of REQUIRED_FEATURES) {
    const value = fields[feature];

    if (
      value === undefined ||
      value === null ||
      typeof value === "boolean" ||
      (typeof value !== "number" && typeof value !== "string") ||
      String(value).trim() === ""
    ) {
      return null;
    }

    const number = Number(value);
    if (!Number.isFinite(number)) return null;

    result[feature] = number;
  }

  return result;
}

function percent(value: number | undefined): string {
  return typeof value === "number" && Number.isFinite(value)
    ? `${(value * 100).toFixed(1)}%`
    : "—";
}

export default function BackendConnectionTest() {
  const [status, setStatus] = useState<BackendStatus | null>(null);
  const [connected, setConnected] = useState(false);
  const [error, setError] = useState("");
  const [events, setEvents] = useState<TelemetryRow[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [detection, setDetection] = useState<DetectionData | null>(null);
  const [running, setRunning] = useState(false);
  const [detectionMessage, setDetectionMessage] = useState("");

  const loadStatus = useCallback(async () => {
    try {
      const result = await getBackendStatus();
      setStatus(result);
      setError("");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Backend unavailable",
      );
    }
  }, []);

  useEffect(() => {
    let active = true;

    const refreshStatus = async () => {
      try {
        const result = await getBackendStatus();
        if (active) setStatus(result);
      } catch (err) {
        if (active) {
          setError(
            err instanceof Error ? err.message : "Backend unavailable",
          );
        }
      }
    };

    void refreshStatus();

    const socket = connectLiveUpdates(
      (event: TelemetryEvent) => {
        if (!active) return;

        if (event.detection) {
          setDetection(event.detection);
          setDetectionMessage("Received a detection update from the backend.");
        }

        if (!event.telemetry) return;

        const telemetry = event.telemetry;
        const row: TelemetryRow = {
          id: `${telemetry.device_id}-${telemetry.timestamp}`,
          deviceId: telemetry.device_id,
          timestamp: telemetry.timestamp,
          eventType: String(
            telemetry.telemetry.event_type ?? "telemetry_event",
          ),
          fields: telemetry.telemetry,
        };

        setEvents((previous) => {
          const updated = previous.filter((item) => item.id !== row.id);
          return [row, ...updated].slice(0, 50);
        });

        setSelectedId((previous) => previous ?? row.id);
        void refreshStatus();
      },
      () => {
        if (active) setConnected(true);
      },
      () => {
        if (active) setConnected(false);
      },
      () => {
        if (active) {
          setConnected(false);
          setError("Live connection error. Check that the backend is running.");
        }
      },
    );

    return () => {
      active = false;
      socket.close();
    };
  }, []);

  const selected = events.find((event) => event.id === selectedId);
  const selectedWindow = selected ? numericWindow(selected.fields) : null;
  const detectionReady = status?.model_inference === "ready";

  async function handleStartDetection() {
    if (!selected || !selectedWindow || running) return;

    setRunning(true);
    setError("");
    setDetectionMessage("");

    try {
      const response = await runDetection(
        selectedWindow,
        selected.deviceId,
      );

      if (response.detection) {
        setDetection(response.detection);
        setDetectionMessage(
          "Inference completed by the backend's pretrained models.",
        );
      } else {
        setDetectionMessage("The backend accepted the request but returned no detection.");
      }

      await loadStatus();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Detection request failed",
      );
    } finally {
      setRunning(false);
    }
  }

  function formatValue(value: unknown): string {
    if (value === null || value === undefined) return "—";
    if (typeof value === "object") {
      try {
        return JSON.stringify(value);
      } catch {
        return String(value);
      }
    }
    return String(value);
  }

  return (
    <section className="mb-6 space-y-5">
      <div className="rounded-xl border border-white/10 bg-[#11151d] p-5">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h2 className="text-lg font-semibold">
              RANSCOPE-M Telemetry Review
            </h2>
            <p className="mt-1 text-sm text-white/50">
              Android telemetry and pretrained-model inference
            </p>
          </div>

          <span
            className={`rounded-full border px-3 py-1 text-xs ${
              connected
                ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
                : "border-red-500/30 bg-red-500/10 text-red-400"
            }`}
          >
            {connected ? "Live connection online" : "Live connection offline"}
          </span>
        </div>

        {error && (
          <p className="mt-4 rounded-lg border border-red-500/20 p-3 text-sm text-red-400">
            {error}
          </p>
        )}

        <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-lg border border-white/5 bg-white/[0.03] p-3">
            <p className="text-xs text-white/50">REST API</p>
            <p className="mt-2 font-medium">
              {status?.backend === "online" ? "Online" : "Unavailable"}
            </p>
          </div>

          <div className="rounded-lg border border-white/5 bg-white/[0.03] p-3">
            <p className="text-xs text-white/50">Telemetry events</p>
            <p className="mt-2 text-xl font-semibold">{events.length}</p>
          </div>

          <div className="rounded-lg border border-white/5 bg-white/[0.03] p-3">
            <p className="text-xs text-white/50">Model inference</p>
            <p className="mt-2 font-medium">
              {detectionReady ? "Ready" : "Unavailable"}
            </p>
            <p className="mt-1 text-xs text-white/40">
              {status?.required_feature_count ?? 30} required features
            </p>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap items-center gap-3">
          <button
            type="button"
            disabled={!selected || !selectedWindow || !detectionReady || running}
            onClick={handleStartDetection}
            title={
              !selected
                ? "Select a telemetry event first"
                : !selectedWindow
                  ? "This event does not contain all 30 numeric model features"
                  : !detectionReady
                    ? "The model inference service is not ready"
                    : "Run the pretrained models on this complete feature window"
            }
            className="rounded-lg bg-cyan-500 px-4 py-2.5 text-sm font-semibold text-slate-950 transition hover:bg-cyan-400 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {running ? "Running inference…" : "▶ Start Detection"}
          </button>

          <p className="text-xs text-white/50">
            {!selected
              ? "Waiting for a telemetry event."
              : !selectedWindow
                ? "Selected event is missing required numeric features."
                : "Ready to submit a complete feature window."}
          </p>
        </div>
      </div>

      {detection && (
        <div className="rounded-xl border border-white/10 bg-[#11151d] p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h3 className="text-lg font-semibold">Detection Results</h3>
              <p className="mt-1 text-sm text-white/50">
                Device: {detection.device_id} ·{" "}
                {new Date(detection.timestamp).toLocaleString()}
              </p>
            </div>

            <span
              className={`rounded-full border px-3 py-1 text-sm font-semibold ${
                detection.risk_level === "MALICIOUS"
                  ? "border-red-500/30 bg-red-500/10 text-red-400"
                  : "border-emerald-500/30 bg-emerald-500/10 text-emerald-400"
              }`}
            >
              {detection.risk_level}
            </span>
          </div>

          <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {Object.entries(MODEL_LABELS).map(([key, label]) => {
              const score = detection.model_outputs[key];
              const weight = detection.model_weights[key];

              return (
                <div
                  key={key}
                  className="rounded-lg border border-white/10 bg-white/[0.03] p-4"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-sm font-medium">{label}</span>
                    <span className="text-xs text-white/40">
                      Weight {percent(weight)}
                    </span>
                  </div>
                  <p className="mt-3 text-2xl font-semibold">{percent(score)}</p>
                  <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white/10">
                    <div
                      className="h-full rounded-full bg-cyan-400"
                      style={{
                        width: `${Math.max(0, Math.min(100, (score ?? 0) * 100))}%`,
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
            <div className="rounded-lg border border-white/10 p-4">
              <p className="text-xs text-white/50">Weighted final probability</p>
              <p className="mt-2 text-2xl font-semibold">
                {percent(detection.final_probability)}
              </p>
            </div>
            <div className="rounded-lg border border-white/10 p-4">
              <p className="text-xs text-white/50">Decision threshold</p>
              <p className="mt-2 text-2xl font-semibold">
                {percent(detection.threshold)}
              </p>
            </div>
            <div className="rounded-lg border border-white/10 p-4">
              <p className="text-xs text-white/50">Alert state</p>
              <p className="mt-2 text-2xl font-semibold">
                {detection.alert ? "Alert" : "No alert"}
              </p>
            </div>
          </div>

          <p className="mt-4 text-xs text-white/50">
            {detectionMessage} Inference status: {detection.inference_status}.
            Results apply to the submitted feature window only.
          </p>
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-white/10 bg-[#11151d]">
        <div className="border-b border-white/10 p-5">
          <h3 className="font-semibold">Incoming Telemetry</h3>
          <p className="mt-1 text-sm text-white/50">
            Select an event to inspect its received fields.
          </p>
        </div>

        {events.length === 0 ? (
          <div className="p-8 text-center text-sm text-white/50">
            Waiting for telemetry. Send a request to POST /api/telemetry.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-white/[0.03] text-xs text-white/50">
                <tr>
                  <th className="px-5 py-3 font-medium">Device</th>
                  <th className="px-5 py-3 font-medium">Event type</th>
                  <th className="px-5 py-3 font-medium">Timestamp</th>
                  <th className="px-5 py-3 font-medium">Action</th>
                </tr>
              </thead>
              <tbody>
                {events.map((event) => (
                  <tr
                    key={event.id}
                    className={`border-t border-white/5 ${
                      selectedId === event.id ? "bg-cyan-500/5" : ""
                    }`}
                  >
                    <td className="px-5 py-4">{event.deviceId}</td>
                    <td className="px-5 py-4">{event.eventType}</td>
                    <td className="px-5 py-4 text-white/60">
                      {new Date(event.timestamp).toLocaleString()}
                    </td>
                    <td className="px-5 py-4">
                      <button
                        type="button"
                        onClick={() => setSelectedId(event.id)}
                        className="text-cyan-400 hover:text-cyan-300"
                      >
                        {selectedId === event.id ? "Selected" : "Inspect"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {selected && (
          <div className="border-t border-white/10 p-5">
            <h4 className="font-medium">Selected Event Details</h4>
            <p className="mt-1 text-xs text-white/50">
              {selected.deviceId} · {selected.eventType}
            </p>
            <div className="mt-4 space-y-2">
              {Object.entries(selected.fields).map(([key, value]) => (
                <div
                  key={key}
                  className="grid grid-cols-1 gap-1 rounded-lg bg-white/[0.03] px-3 py-2 sm:grid-cols-[220px_1fr]"
                >
                  <span className="text-sm text-white/50">{key}</span>
                  <span className="break-all text-sm">{formatValue(value)}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      <p className="px-1 text-xs text-white/40">
        Up to 50 events are retained in this frontend session. Raw Android
        events must be aggregated into the required feature window before
        inference; missing features are never replaced with invented values.
      </p>
    </section>
  );
}

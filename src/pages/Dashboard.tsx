
import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  Monitor,
  ShieldCheck,
  Wifi,
} from "lucide-react";

import StatCard from "../components/StatCard";
import RiskOverview from "../components/RiskOverview";
import ModelAnalysis from "../components/ModelAnalysis";
import RiskHistory from "../components/RiskHistory";
import ActivityFeed from "../components/ActivityFeed";
import AlertPanel from "../components/AlertPanel";

import {
  getBackendStatus,
  getLatestDetection,
  connectLiveUpdates,
  type BackendStatus,
  type DetectionResponse,
  type TelemetryEvent,
} from "../services/api";

type Detection = NonNullable<DetectionResponse["detection"]>;

const emptyModels = {
  mpvg: 0,
  sfim: 0,
  eap: 0,
  cait: 0,
};

function normalizeDetection(
  detection: Detection | null | undefined,
): Detection | null {
  return detection ?? null;
}

function getModelOutput(
  outputs: Record<string, number>,
  model: string,
): number {
  return outputs[model] ?? outputs[model.toUpperCase()] ?? 0;
}

export default function Dashboard() {
  const [backendStatus, setBackendStatus] =
    useState<BackendStatus | null>(null);

  const [detection, setDetection] = useState<Detection | null>(null);
  const [connectionError, setConnectionError] = useState("");

  const refreshDashboard = useCallback(async () => {
    try {
      const [status, latest] = await Promise.all([
        getBackendStatus(),
        getLatestDetection(),
      ]);

      setBackendStatus(status);
      setDetection(normalizeDetection(latest.detection));
      setConnectionError("");
    } catch (error) {
      setConnectionError(
        error instanceof Error
          ? error.message
          : "Could not connect to the backend.",
      );
    }
  }, []);

  useEffect(() => {
    void refreshDashboard();

    const intervalId = window.setInterval(() => {
      void refreshDashboard();
    }, 5000);

    const socket = connectLiveUpdates((event: TelemetryEvent) => {
      if (event.event === "detection_update" && event.detection) {
        setDetection(event.detection as Detection);
      }

      if (event.event === "connection") {
        void refreshDashboard();
      }
    });

    return () => {
      window.clearInterval(intervalId);

      if (
        socket.readyState === WebSocket.CONNECTING ||
        socket.readyState === WebSocket.OPEN
      ) {
        socket.close();
      }
    };
  }, [refreshDashboard]);

  const models = detection
    ? {
        mpvg: getModelOutput(detection.model_outputs, "mpvg"),
        sfim: getModelOutput(detection.model_outputs, "sfim"),
        eap: getModelOutput(detection.model_outputs, "eap"),
        cait: getModelOutput(detection.model_outputs, "cait"),
      }
    : emptyModels;

  const finalProbability = detection?.final_probability ?? 0;
  const isMalicious = detection?.risk_level === "MALICIOUS";

  const riskLevel = !detection
    ? "UNKNOWN"
    : isMalicious
      ? "CRITICAL"
      : "LOW";

  return (
    <div className="pb-10">
      <div className="mb-7 flex items-end justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight text-white">
            Overview
          </h1>

          <p className="mt-1.5 text-sm text-white/35">
            Monitor your protected infrastructure and detection activity.
          </p>
        </div>

        <div className="hidden items-center gap-2 text-xs text-white/25 md:flex">
          <span
            className={`h-1.5 w-1.5 rounded-full ${
              backendStatus?.backend === "online"
                ? "bg-emerald-400"
                : "bg-red-400"
            }`}
          />

          {backendStatus?.backend === "online"
            ? "Backend connected"
            : "Backend disconnected"}
        </div>
      </div>

      {connectionError && (
        <div className="mb-5 rounded-xl border border-red-400/20 bg-red-400/5 p-4 text-sm text-red-300">
          Backend connection error: {connectionError}
        </div>
      )}

      <div className="mb-5 rounded-xl border border-white/[0.07] bg-[#0b0f14] p-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <p className="text-xs font-medium uppercase tracking-wider text-white/40">
              Latest inference
            </p>

            <p className="mt-1 text-sm text-white/75">
              {!detection
                ? "No detection has been run yet."
                : `${detection.risk_level} · ${Math.round(
                    detection.final_probability * 100,
                  )}% probability`}
            </p>

            {detection && (
              <p className="mt-1 text-xs text-white/35">
                Device: {detection.device_id} ·{" "}
                {new Date(detection.timestamp).toLocaleString()}
              </p>
            )}
          </div>

          <span
            className={`rounded-full px-3 py-1.5 text-xs font-medium ${
              !detection
                ? "bg-white/5 text-white/40"
                : isMalicious
                  ? "bg-red-400/10 text-red-300"
                  : "bg-emerald-400/10 text-emerald-300"
            }`}
          >
            {!detection
              ? "AWAITING INFERENCE"
              : isMalicious
                ? "MALICIOUS"
                : "BENIGN"}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          title="Protected Devices"
          value="—"
          subtitle="Device inventory not connected"
          icon={Monitor}
        />

        <StatCard
          title="Online"
          value={backendStatus?.backend === "online" ? "1" : "0"}
          subtitle="Backend service status"
          icon={Wifi}
          accent="green"
        />

        <StatCard
          title="Warnings"
          value={detection?.alert ? "1" : "0"}
          subtitle={
            detection?.alert
              ? "Backend raised an alert"
              : "No current backend alert"
          }
          icon={AlertTriangle}
          accent={detection?.alert ? "yellow" : undefined}
        />

        <StatCard
          title="Critical"
          value={detection && isMalicious ? "1" : "0"}
          subtitle={
            !detection
              ? "Awaiting model inference"
              : isMalicious
                ? "Malicious verdict"
                : "Latest verdict is benign"
          }
          icon={ShieldCheck}
          accent={detection && isMalicious ? "yellow" : "green"}
        />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-2">
        <RiskOverview
          probability={finalProbability}
          riskLevel={riskLevel}
        />

        <ModelAnalysis
          models={models}
          finalProbability={finalProbability}
        />
      </div>

      <div className="mt-5">
        <RiskHistory />
      </div>

      <div className="mt-5 grid grid-cols-1 gap-5 xl:grid-cols-2">
        <ActivityFeed />
        <AlertPanel />
      </div>

      {!detection && (
        <p className="mt-4 text-xs text-white/30">
          Model scores remain at zero until the backend returns an actual
          detection. Device inventory, activity history and alert history
          have not yet been connected to persistent backend data.
        </p>
      )}
    </div>
  );
}

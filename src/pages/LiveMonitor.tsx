import { useCallback, useEffect, useState } from "react";
import type { ReactNode } from "react";
import {
  Activity,
  Cpu,
  Radio,
  AlertTriangle,
  CheckCircle2,
  ShieldAlert,
  Wifi,
  WifiOff,
  RefreshCw,
} from "lucide-react";

import {
  connectLiveUpdates,
  getLatestTelemetry,
  getLatestDetection,
  getBackendStatus,
  type AndroidAggregation,
  type DetectionData,
  type TelemetryData,
  type TelemetryEvent,
  type BackendStatus,
} from "../services/api";

type SocketStatus = "connecting" | "connected" | "disconnected" | "error";

export default function LiveMonitor() {
  const [liveTelemetry, setLiveTelemetry] =
    useState<TelemetryData | null>(null);
  const [latestDetection, setLatestDetection] =
    useState<DetectionData | null>(null);
  const [backendStatus, setBackendStatus] =
    useState<BackendStatus | null>(null);
  const [socketStatus, setSocketStatus] =
    useState<SocketStatus>("connecting");
  const [telemetryError, setTelemetryError] = useState("");
  const [detectionError, setDetectionError] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [lastRefresh, setLastRefresh] = useState<Date | null>(null);

  const handleLiveMessage = useCallback((event: TelemetryEvent) => {
    if (event.event === "telemetry_update" && event.telemetry) {
      setLiveTelemetry(event.telemetry);
      setTelemetryError("");
    }

    if (event.event === "detection_update" && event.detection) {
      setLatestDetection(event.detection);
      setDetectionError("");
    }

    if (event.event === "connection") {
      if (event.telemetry) {
        setLiveTelemetry(event.telemetry);
      }

      if (event.detection) {
        setLatestDetection(event.detection);
      }
    }
  }, []);

  const refreshData = useCallback(async () => {
    setRefreshing(true);

    try {
      const [statusResult, telemetryResult, detectionResult] =
        await Promise.allSettled([
          getBackendStatus(),
          getLatestTelemetry(),
          getLatestDetection(),
        ]);

      if (statusResult.status === "fulfilled") {
        setBackendStatus(statusResult.value);
      } else {
        setBackendStatus(null);
      }

      if (telemetryResult.status === "fulfilled") {
        setLiveTelemetry(telemetryResult.value.telemetry);
        setTelemetryError("");
      } else {
        setTelemetryError(
          telemetryResult.reason instanceof Error
            ? telemetryResult.reason.message
            : "Could not retrieve Android telemetry.",
        );
      }

      if (detectionResult.status === "fulfilled") {
        setLatestDetection(detectionResult.value.detection);
        setDetectionError("");
      } else {
        setDetectionError(
          detectionResult.reason instanceof Error
            ? detectionResult.reason.message
            : "Could not retrieve the latest detection.",
        );
      }

      setLastRefresh(new Date());
    } finally {
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    let active = true;

    void refreshData();

    const pollId = window.setInterval(() => {
      if (active) void refreshData();
    }, 3000);

    const socket = connectLiveUpdates(
      (event) => {
        if (active) handleLiveMessage(event);
      },
      () => {
        if (active) setSocketStatus("connected");
      },
      () => {
        if (active) setSocketStatus("disconnected");
      },
      () => {
        if (active) setSocketStatus("error");
      },
    );

    return () => {
      active = false;
      window.clearInterval(pollId);
      socket.close();
    };
  }, [handleLiveMessage, refreshData]);

  const aggregation: AndroidAggregation | undefined =
    liveTelemetry?.aggregation;

  const liveEvent = liveTelemetry?.telemetry;

  const liveLayer =
    typeof liveEvent?.layer === "string"
      ? liveEvent.layer
      : "Unknown";

  const liveEventName =
    typeof liveEvent?.event === "string"
      ? liveEvent.event
      : "Unknown";

  const nativeFeatures =
    liveEvent?.feature_window &&
    typeof liveEvent.feature_window === "object" &&
    !Array.isArray(liveEvent.feature_window)
      ? (liveEvent.feature_window as Record<string, unknown>)
      : null;

  const isAndroidWindow =
    liveEventName === "android_window_complete";

  const backendOnline = backendStatus?.backend === "online";

  const modelReady =
    backendStatus?.model_inference === "ready" &&
    !backendStatus?.model_load_error;

  const probability = latestDetection?.final_probability;
  const verdict = latestDetection?.risk_level;
  const outputs = latestDetection?.model_outputs;
  const weights = latestDetection?.model_weights;

  const formatProbability = (value?: number) =>
    typeof value === "number" && Number.isFinite(value)
      ? `${(value * 100).toFixed(2)}%`
      : "—";

  const formatTimestamp = (value?: string) => {
    if (!value) return "—";

    const date = new Date(value);

    return Number.isNaN(date.getTime())
      ? value
      : date.toLocaleString();
  };

  const formatValue = (value: unknown): string => {
    if (value == null) return "—";

    if (
      typeof value === "string" ||
      typeof value === "number" ||
      typeof value === "boolean"
    ) {
      return String(value);
    }

    try {
      return JSON.stringify(value) ?? "—";
    } catch {
      return String(value);
    }
  };

  return (
    <div className="space-y-6 pb-10">
      {/* Page heading */}
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Live Detection Monitor
          </h1>
          <p className="mt-1.5 text-sm text-white/40">
            Real Android telemetry, backend health and RANSCOPE
            four-layer detection results.
          </p>
        </div>

        <button
          onClick={() => void refreshData()}
          disabled={refreshing}
          className="inline-flex items-center gap-2 rounded-lg border border-white/10 px-3 py-2 text-sm text-white/70 transition hover:bg-white/5 disabled:opacity-50"
        >
          <RefreshCw
            size={15}
            className={refreshing ? "animate-spin" : ""}
          />
          Refresh
        </button>
      </div>

      {/* System status */}
      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <Metric
          icon={
            backendOnline ? (
              <Wifi size={18} />
            ) : (
              <WifiOff size={18} />
            )
          }
          label="BACKEND STATUS"
          value={
            backendStatus
              ? backendOnline
                ? "Online"
                : "Offline"
              : "Checking…"
          }
          detail={
            backendStatus
              ? `${backendStatus.connected_websocket_clients} WebSocket client(s)`
              : "Waiting for backend status"
          }
          tone={backendOnline ? "good" : "warning"}
        />

        <Metric
          icon={<Cpu size={18} />}
          label="MODEL PIPELINE"
          value={
            backendStatus
              ? modelReady
                ? "Ready"
                : "Unavailable"
              : "Checking…"
          }
          detail={
            backendStatus?.model_load_error ||
            (modelReady
              ? "Four pretrained detection layers configured"
              : "Model readiness has not been confirmed")
          }
          tone={modelReady ? "good" : "warning"}
        />

        <Metric
          icon={<ShieldAlert size={18} />}
          label="LATEST FUSED SCORE"
          value={formatProbability(probability)}
          detail={
            verdict
              ? `Latest backend verdict: ${verdict}`
              : "No detection result available yet"
          }
          tone={
            verdict === "MALICIOUS"
              ? "danger"
              : verdict === "BENIGN"
                ? "good"
                : "neutral"
          }
        />
      </div>

      {/* Live Android telemetry */}
      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <Radio size={18} className="text-blue-300" />
              <h2 className="font-semibold">
                Live Android Telemetry
              </h2>
            </div>

            <p className="mt-2 text-sm text-white/45">
              Events received from the Android monitoring pipeline.
            </p>
          </div>

          <span
            className={`rounded-full border px-3 py-1.5 text-xs ${
              socketStatus === "connected"
                ? "border-emerald-400/30 bg-emerald-400/10 text-emerald-300"
                : socketStatus === "error"
                  ? "border-rose-400/30 bg-rose-400/10 text-rose-300"
                  : "border-amber-400/30 bg-amber-400/10 text-amber-300"
            }`}
          >
            {socketStatus === "connected"
              ? "WebSocket connected"
              : socketStatus === "connecting"
                ? "Connecting…"
                : socketStatus === "error"
                  ? "WebSocket error"
                  : "WebSocket disconnected"}
          </span>
        </div>

        {telemetryError && (
          <ErrorMessage message={telemetryError} />
        )}

        <div className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <LiveMetric
            label="LATEST LAYER"
            value={liveTelemetry ? liveLayer : "—"}
          />

          <LiveMetric
            label="LATEST EVENT"
            value={
              liveTelemetry ? liveEventName : "Waiting for events"
            }
          />

          <LiveMetric
            label="OBSERVED EVENTS"
            value={
              aggregation
                ? String(aggregation.observed_event_count)
                : "—"
            }
          />

          <LiveMetric
            label="OBSERVED PATHS"
            value={
              aggregation
                ? String(aggregation.unique_observed_paths)
                : "—"
            }
          />
        </div>

        {liveTelemetry && (
          <div className="mt-5 rounded-xl border border-white/[0.07] bg-white/[0.02] p-4">
            <h3 className="text-sm font-medium">
              Latest observed event
            </h3>

            <div className="mt-3 grid grid-cols-1 gap-2 text-xs sm:grid-cols-2">
              <Detail
                label="Device"
                value={liveTelemetry.device_id}
              />

              <Detail
                label="Timestamp"
                value={formatTimestamp(liveTelemetry.timestamp)}
              />

              {Object.entries(liveEvent ?? {})
                .filter(
                  ([key]) =>
                    !["timestamp", "package"].includes(key),
                )
                .map(([key, value]) => (
                  <Detail
                    key={key}
                    label={key}
                    value={formatValue(value)}
                  />
                ))}
            </div>
          </div>
        )}

        {/* Aggregator status */}
        <div className="mt-5 flex items-start gap-3 rounded-xl border border-amber-400/20 bg-amber-400/[0.04] p-4">
          {aggregation?.complete ? (
            <CheckCircle2
              size={18}
              className="mt-0.5 shrink-0 text-emerald-300"
            />
          ) : (
            <AlertTriangle
              size={18}
              className="mt-0.5 shrink-0 text-amber-300"
            />
          )}

          <div>
            <p className="text-sm font-medium">
              {aggregation?.complete
                ? "Aggregator reports all required features available"
                : "Waiting for a complete feature window"}
            </p>

            <p className="mt-1 text-xs leading-5 text-white/50">
              {aggregation
                ? `${aggregation.available.length} features available; ${aggregation.unavailable.length} unavailable.`
                : "No Android aggregation snapshot is available yet."}
              {" "}
              Live telemetry is not automatically submitted for inference by this page.
            </p>
          </div>
        </div>

        {aggregation && aggregation.unavailable.length > 0 && (
          <div className="mt-4">
            <h3 className="text-xs font-semibold uppercase tracking-wider text-white/45">
              Unavailable features
            </h3>

            <div className="mt-2 flex flex-wrap gap-2">
              {aggregation.unavailable.map((feature) => (
                <span
                  key={feature}
                  className="rounded-md border border-amber-400/20 bg-amber-400/[0.04] px-2 py-1 font-mono text-xs text-amber-200"
                >
                  {feature}
                </span>
              ))}
            </div>
          </div>
        )}

        {lastRefresh && (
          <p className="mt-4 text-xs text-white/30">
            Last checked: {lastRefresh.toLocaleTimeString()}
          </p>
        )}
      </section>

      {/* Android Feature Window */}
      {isAndroidWindow && nativeFeatures && (
        <section className="rounded-2xl border border-blue-400/20 bg-[#0c1017] p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="font-semibold">
                Android Feature Window
              </h2>
              <p className="mt-1.5 text-sm text-white/45">
                Features measured by the Android file monitor.
              </p>
            </div>

            <span className="rounded-full border border-blue-400/20 bg-blue-400/10 px-3 py-1.5 text-xs text-blue-200">
              Raw telemetry · Not yet inferred
            </span>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {Object.entries(nativeFeatures).map(([name, value]) => (
              <Detail
                key={name}
                label={name}
                value={formatValue(value)}
              />
            ))}
          </div>

          {Object.keys(nativeFeatures).length === 0 && (
            <p className="mt-4 text-sm text-white/45">
              No feature values were included in this window.
            </p>
          )}
        </section>
      )}

      {/* Latest real detection */}
      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="font-semibold">
              Latest Backend Detection
            </h2>
            <p className="mt-1.5 text-sm text-white/45">
              Results retrieved from the backend detection endpoint.
            </p>
          </div>

          {latestDetection && (
            <span
              className={`rounded-full border px-3 py-1.5 text-xs ${
                latestDetection.alert ||
                latestDetection.risk_level === "MALICIOUS"
                  ? "border-rose-400/30 bg-rose-400/10 text-rose-300"
                  : "border-emerald-400/20 bg-emerald-400/5 text-emerald-300"
              }`}
            >
              {latestDetection.alert
                ? "ALERT TRIGGERED"
                : latestDetection.risk_level}
            </span>
          )}
        </div>

        {detectionError && (
          <ErrorMessage message={detectionError} />
        )}

        {!latestDetection ? (
          <div className="mt-5 rounded-xl border border-dashed border-white/10 p-8 text-center">
            <ShieldAlert
              size={28}
              className="mx-auto text-white/25"
            />

            <p className="mt-3 text-sm font-medium text-white/70">
              No detection result available
            </p>

            <p className="mt-1 text-xs leading-5 text-white/40">
              A result will appear when the backend has completed a
              valid inference request. Model readiness alone does
              not mean a detection has occurred.
            </p>
          </div>
        ) : (
          <>
            <div className="mt-5 flex flex-wrap items-end justify-between gap-4">
              <div>
                <p className="text-xs text-white/40">
                  Fused probability
                </p>
                <p className="mt-1 text-4xl font-semibold">
                  {formatProbability(
                    latestDetection.final_probability,
                  )}
                </p>
              </div>

              <div className="rounded-lg border border-white/10 px-3 py-2 text-sm">
                Verdict:{" "}
                <strong>{latestDetection.risk_level}</strong>
              </div>
            </div>

            <div className="mt-6 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {(
                [
                  ["MPVG", "mpvg"],
                  ["SFIM", "sfim"],
                  ["EAP", "eap"],
                  ["CAIT", "cait"],
                ] as const
              ).map(([name, key]) => {
                const score = outputs?.[key];
                const weight = weights?.[key];

                return (
                  <div
                    key={name}
                    className="rounded-xl border border-white/[0.07] bg-white/[0.02] p-4"
                  >
                    <div className="flex justify-between gap-3">
                      <span className="text-sm font-medium">
                        {name}
                      </span>
                      <span className="text-sm text-white/70">
                        {formatProbability(score)}
                      </span>
                    </div>

                    <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-white/[0.07]">
                      <div
                        className="h-full rounded-full bg-blue-400 transition-all"
                        style={{
                          width: `${
                            typeof score === "number" &&
                            Number.isFinite(score)
                              ? Math.max(
                                  0,
                                  Math.min(100, score * 100),
                                )
                              : 0
                          }%`,
                        }}
                      />
                    </div>

                    <p className="mt-2 text-xs text-white/35">
                      Fusion weight:{" "}
                      {typeof weight === "number"
                        ? `${(weight * 100).toFixed(0)}%`
                        : "—"}
                    </p>
                  </div>
                );
              })}
            </div>

            <div className="mt-5 grid grid-cols-1 gap-2 text-xs sm:grid-cols-2">
              <Detail
                label="Device"
                value={latestDetection.device_id}
              />

              <Detail
                label="Detection window"
                value={
                  latestDetection.window == null
                    ? "—"
                    : String(latestDetection.window)
                }
              />

              <Detail
                label="Inference status"
                value={latestDetection.inference_status}
              />

              <Detail
                label="Threshold"
                value={formatProbability(latestDetection.threshold)}
              />

              <Detail
                label="Timestamp"
                value={formatTimestamp(latestDetection.timestamp)}
              />
            </div>
          </>
        )}
      </section>

      {/* Backend diagnostics */}
      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
        <div className="flex items-center gap-2">
          <Activity size={18} className="text-blue-300" />
          <h2 className="font-semibold">Backend Diagnostics</h2>
        </div>

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          <LiveMetric
            label="REQUIRED FEATURES"
            value={
              backendStatus?.required_feature_count == null
                ? "—"
                : String(backendStatus.required_feature_count)
            }
          />

          <LiveMetric
            label="CONNECTED CLIENTS"
            value={
              backendStatus?.connected_websocket_clients == null
                ? "—"
                : String(backendStatus.connected_websocket_clients)
            }
          />

          <LiveMetric
            label="DETECTION THRESHOLD"
            value={formatProbability(backendStatus?.threshold)}
          />
        </div>

        {backendStatus?.model_weights && (
          <div className="mt-4 flex flex-wrap gap-2">
            {Object.entries(backendStatus.model_weights).map(
              ([name, weight]) => (
                <span
                  key={name}
                  className="rounded-lg border border-white/10 px-3 py-2 text-xs text-white/60"
                >
                  {name}:{" "}
                  {typeof weight === "number"
                    ? `${(weight * 100).toFixed(0)}%`
                    : "—"}
                </span>
              ),
            )}
          </div>
        )}

        {backendStatus?.model_load_error && (
          <ErrorMessage
            message={`Model load error: ${backendStatus.model_load_error}`}
          />
        )}
      </section>
    </div>
  );
}

function LiveMetric({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-white/[0.07] bg-white/[0.02] p-4">
      <p className="text-[10px] font-semibold tracking-[0.12em] text-white/40">
        {label}
      </p>
      <p className="mt-2 break-words text-sm font-medium">
        {value}
      </p>
    </div>
  );
}

function Detail({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="min-w-0 rounded-lg bg-black/20 p-2">
      <p className="text-[10px] uppercase tracking-wider text-white/35">
        {label}
      </p>
      <p className="mt-1 break-words text-white/75">
        {value}
      </p>
    </div>
  );
}

function Metric({
  icon,
  label,
  value,
  detail,
  tone = "neutral",
}: {
  icon: ReactNode;
  label: string;
  value: string;
  detail: string;
  tone?: "good" | "warning" | "danger" | "neutral";
}) {
  const toneClass = {
    good: "text-emerald-300",
    warning: "text-amber-300",
    danger: "text-rose-300",
    neutral: "text-white",
  }[tone];

  return (
    <div className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
      <div className="flex items-center gap-2 text-white/40">
        {icon}
        <span className="text-[10px] font-semibold tracking-[0.14em]">
          {label}
        </span>
      </div>

      <div className={`mt-4 text-2xl font-semibold ${toneClass}`}>
        {value}
      </div>

      <p className="mt-1 break-words text-xs text-white/35">
        {detail}
      </p>
    </div>
  );
}

function ErrorMessage({ message }: { message: string }) {
  return (
    <div className="mt-4 flex items-start gap-2 rounded-lg border border-rose-400/20 bg-rose-400/5 p-3 text-sm text-rose-300">
      <AlertTriangle size={16} className="mt-0.5 shrink-0" />
      <span className="break-words">{message}</span>
    </div>
  );
}
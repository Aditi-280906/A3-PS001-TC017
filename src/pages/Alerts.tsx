import { useState } from "react";
import {
  AlertTriangle,
  Bell,
  CheckCircle2,
  Clock,
  ShieldAlert,
} from "lucide-react";

type Severity = "CRITICAL" | "HIGH" | "MEDIUM" | "LOW";
type AlertStatus = "OPEN" | "ACKNOWLEDGED" | "RESOLVED";

interface SecurityAlert {
  id: string;
  title: string;
  description: string;
  severity: Severity;
  status: AlertStatus;
  device: string;
  time: string;
}

const initialAlerts: SecurityAlert[] = [
  {
    id: "ALT-001",
    title: "Elevated file activity",
    description: "Illustrative alert for unusual file-operation volume.",
    severity: "HIGH",
    status: "OPEN",
    device: "Android Emulator",
    time: "Demo event",
  },
  {
    id: "ALT-002",
    title: "Permission activity changed",
    description: "Illustrative change in permission-related behaviour.",
    severity: "MEDIUM",
    status: "ACKNOWLEDGED",
    device: "Development Workstation",
    time: "Demo event",
  },
  {
    id: "ALT-003",
    title: "Detection pipeline ready",
    description: "The dashboard is awaiting live backend integration.",
    severity: "LOW",
    status: "RESOLVED",
    device: "RANSCOPE",
    time: "Demo event",
  },
];

const severityStyles: Record<Severity, string> = {
  CRITICAL: "bg-rose-400/10 text-rose-300 border-rose-400/20",
  HIGH: "bg-orange-400/10 text-orange-300 border-orange-400/20",
  MEDIUM: "bg-amber-400/10 text-amber-300 border-amber-400/20",
  LOW: "bg-sky-400/10 text-sky-300 border-sky-400/20",
};

export default function Alerts() {
  const [alerts, setAlerts] = useState(initialAlerts);
  const [filter, setFilter] = useState("ALL");

  const filteredAlerts = alerts.filter(
    (alert) => filter === "ALL" || alert.status === filter
  );

  const openCount = alerts.filter(
    (alert) => alert.status === "OPEN"
  ).length;

  const acknowledgedCount = alerts.filter(
    (alert) => alert.status === "ACKNOWLEDGED"
  ).length;

  function updateStatus(id: string, status: AlertStatus) {
    setAlerts((current) =>
      current.map((alert) =>
        alert.id === id ? { ...alert, status } : alert
      )
    );
  }

  return (
    <div className="space-y-6 pb-10">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Security Alerts
          </h1>
          <p className="mt-1.5 text-sm text-white/40">
            Review, acknowledge, and resolve detection alerts.
          </p>
        </div>

        <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-3 py-1.5 text-xs text-amber-300">
          DEMO DATA
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Summary
          icon={<Bell size={18} />}
          label="TOTAL ALERTS"
          value={String(alerts.length)}
        />
        <Summary
          icon={<AlertTriangle size={18} />}
          label="OPEN"
          value={String(openCount)}
        />
        <Summary
          icon={<Clock size={18} />}
          label="ACKNOWLEDGED"
          value={String(acknowledgedCount)}
        />
      </div>

      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017]">
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-white/[0.07] p-5">
          <div>
            <h2 className="font-semibold">Alert Centre</h2>
            <p className="mt-1 text-xs text-white/35">
              Sample alerts; no live security events are connected.
            </p>
          </div>

          <select
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
            className="rounded-lg border border-white/[0.08] bg-[#10151e] px-3 py-2 text-sm text-white/70 outline-none"
          >
            <option value="ALL">All statuses</option>
            <option value="OPEN">Open</option>
            <option value="ACKNOWLEDGED">Acknowledged</option>
            <option value="RESOLVED">Resolved</option>
          </select>
        </div>

        <div className="divide-y divide-white/[0.06]">
          {filteredAlerts.map((alert) => (
            <article key={alert.id} className="p-5">
              <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-center">
                <div className="flex gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-white/[0.04]">
                    {alert.severity === "LOW" ? (
                      <CheckCircle2 size={19} className="text-sky-300" />
                    ) : (
                      <ShieldAlert
                        size={19}
                        className={
                          alert.severity === "CRITICAL" ||
                          alert.severity === "HIGH"
                            ? "text-orange-300"
                            : "text-amber-300"
                        }
                      />
                    )}
                  </div>

                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-medium text-white/90">
                        {alert.title}
                      </h3>
                      <span
                        className={`rounded-full border px-2 py-0.5 text-[10px] ${severityStyles[alert.severity]}`}
                      >
                        {alert.severity}
                      </span>
                    </div>

                    <p className="mt-1 text-sm text-white/45">
                      {alert.description}
                    </p>

                    <p className="mt-2 text-xs text-white/30">
                      {alert.id} · {alert.device} · {alert.time}
                    </p>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-3 lg:justify-end">
                  <span className="text-xs text-white/45">
                    {alert.status}
                  </span>

                  {alert.status === "OPEN" && (
                    <button
                      onClick={() =>
                        updateStatus(alert.id, "ACKNOWLEDGED")
                      }
                      className="rounded-lg border border-white/10 px-3 py-2 text-xs text-white/70 transition hover:bg-white/[0.05]"
                    >
                      Acknowledge
                    </button>
                  )}

                  {alert.status !== "RESOLVED" && (
                    <button
                      onClick={() => updateStatus(alert.id, "RESOLVED")}
                      className="rounded-lg bg-white px-3 py-2 text-xs font-medium text-black transition hover:bg-white/80"
                    >
                      Resolve
                    </button>
                  )}
                </div>
              </div>
            </article>
          ))}

          {filteredAlerts.length === 0 && (
            <div className="p-12 text-center text-sm text-white/35">
              No alerts match this status.
            </div>
          )}
        </div>
      </section>

      <p className="text-xs leading-5 text-white/35">
        Alert changes are currently held in frontend state only. They are not
        persisted or connected to actual model predictions.
      </p>
    </div>
  );
}

function Summary({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
      <div className="flex items-center gap-2 text-white/40">
        {icon}
        <span className="text-[10px] font-semibold tracking-[0.14em]">
          {label}
        </span>
      </div>
      <div className="mt-4 text-2xl font-semibold">{value}</div>
    </div>
  );
}

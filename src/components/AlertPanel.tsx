import {
  AlertTriangle,
  CheckCircle2,
  ChevronRight,
} from "lucide-react";

interface Alert {
  id: string;
  title: string;
  description: string;
  device: string;
  probability: number;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  time: string;
}

const alerts: Alert[] = [
  {
    id: "ALT-001",
    title: "Elevated ransomware probability",
    description: "EAP entropy activity increased",
    device: "WORKSTATION-07",
    probability: 42,
    severity: "MEDIUM",
    time: "21:51",
  },
];

const severityStyles = {
  LOW: "bg-emerald-400/10 text-emerald-400",
  MEDIUM: "bg-amber-400/10 text-amber-400",
  HIGH: "bg-orange-400/10 text-orange-400",
  CRITICAL: "bg-red-400/10 text-red-400",
};

export default function AlertPanel() {
  return (
    <div className="rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-6">

      {/* Header */}
      <div className="flex items-start justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            Active Alerts
          </p>

          <p className="mt-1 text-sm text-white/40">
            Security events requiring attention
          </p>
        </div>

        <div className="rounded-full bg-amber-400/10 px-2.5 py-1 text-[10px] font-medium text-amber-400">
          1 ACTIVE
        </div>

      </div>

      {/* Alerts */}
      <div className="mt-6">

        {alerts.length === 0 ? (

          <div className="flex flex-col items-center justify-center py-10">

            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-emerald-400/10">

              <CheckCircle2
                size={22}
                className="text-emerald-400"
              />

            </div>

            <p className="mt-4 text-sm font-medium text-white/60">
              No active threats
            </p>

            <p className="mt-1 text-xs text-white/25">
              All protected devices are operating normally.
            </p>

          </div>

        ) : (

          <div className="space-y-3">

            {alerts.map((alert) => (

              <div
                key={alert.id}
                className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-4"
              >

                <div className="flex items-start gap-3">

                  <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-amber-400/10">

                    <AlertTriangle
                      size={17}
                      className="text-amber-400"
                    />

                  </div>

                  <div className="min-w-0 flex-1">

                    <div className="flex items-start justify-between gap-3">

                      <div>

                        <p className="text-xs font-medium text-white/75">
                          {alert.title}
                        </p>

                        <p className="mt-1 text-[10px] text-white/30">
                          {alert.description}
                        </p>

                      </div>

                      <span
                        className={`shrink-0 rounded-full px-2 py-1 text-[9px] font-medium ${severityStyles[alert.severity]}`}
                      >
                        {alert.severity}
                      </span>

                    </div>

                    <div className="mt-4 flex items-center justify-between">

                      <div>

                        <p className="text-[9px] uppercase tracking-wider text-white/20">
                          Device
                        </p>

                        <p className="mt-1 text-[10px] text-white/45">
                          {alert.device}
                        </p>

                      </div>

                      <div className="text-right">

                        <p className="text-[9px] uppercase tracking-wider text-white/20">
                          Probability
                        </p>

                        <p className="mt-1 text-sm font-medium text-amber-400">
                          {alert.probability}%
                        </p>

                      </div>

                    </div>

                  </div>

                </div>

                <button className="mt-4 flex w-full items-center justify-between border-t border-white/[0.05] pt-3 text-[10px] text-white/25 transition hover:text-white/60">

                  <span>
                    {alert.id} · {alert.time}
                  </span>

                  <ChevronRight size={13} />

                </button>

              </div>

            ))}

          </div>

        )}

      </div>

    </div>
  );
}
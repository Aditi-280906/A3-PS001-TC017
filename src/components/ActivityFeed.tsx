import {
  Activity,
  BrainCircuit,
  CheckCircle2,
  Radio,
  ShieldCheck,
} from "lucide-react";

const activities = [
  {
    time: "21:55:04",
    title: "Telemetry received",
    description: "New system window processed",
    icon: Radio,
  },
  {
    time: "21:55:04",
    title: "MPVG prediction completed",
    description: "Probability: 18%",
    icon: BrainCircuit,
  },
  {
    time: "21:55:05",
    title: "SFIM prediction completed",
    description: "Probability: 31%",
    icon: Activity,
  },
  {
    time: "21:55:05",
    title: "EAP prediction completed",
    description: "Probability: 42%",
    icon: Activity,
  },
  {
    time: "21:55:05",
    title: "CAIT prediction completed",
    description: "Probability: 21%",
    icon: BrainCircuit,
  },
  {
    time: "21:55:05",
    title: "Weighted result calculated",
    description: "Final probability: 30%",
    icon: ShieldCheck,
  },
];

export default function ActivityFeed() {
  return (
    <div className="rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-6">

      {/* Header */}
      <div className="flex items-start justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            Recent Activity
          </p>

          <p className="mt-1 text-sm text-white/40">
            Latest detection events
          </p>
        </div>

        <div className="flex items-center gap-2">

          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-40" />

            <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
          </span>

          <span className="text-[10px] text-emerald-400/70">
            LIVE
          </span>

        </div>

      </div>

      {/* Activity list */}
      <div className="mt-6 space-y-1">

        {activities.map((activity, index) => {

          const Icon = activity.icon;

          return (
            <div
              key={`${activity.time}-${index}`}
              className="group flex items-start gap-3 rounded-xl px-2 py-3 transition hover:bg-white/[0.025]"
            >

              {/* Icon */}
              <div className="mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-white/[0.04] text-white/40 transition group-hover:bg-white/[0.07] group-hover:text-white/70">

                <Icon
                  size={14}
                  strokeWidth={1.8}
                />

              </div>

              {/* Content */}
              <div className="min-w-0 flex-1">

                <div className="flex items-center justify-between gap-3">

                  <p className="truncate text-xs font-medium text-white/70">
                    {activity.title}
                  </p>

                  <span className="shrink-0 text-[9px] text-white/20">
                    {activity.time}
                  </span>

                </div>

                <p className="mt-1 text-[10px] text-white/30">
                  {activity.description}
                </p>

              </div>

              {/* Completion */}
              <CheckCircle2
                size={13}
                className="mt-1 shrink-0 text-emerald-400/50"
              />

            </div>
          );
        })}

      </div>

    </div>
  );
}
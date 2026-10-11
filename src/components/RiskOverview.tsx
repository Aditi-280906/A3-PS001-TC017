import { ShieldCheck, Activity } from "lucide-react";

interface RiskOverviewProps {
  probability: number;
  riskLevel: string;
}

export default function RiskOverview({
  probability,
  riskLevel,
}: RiskOverviewProps) {
  const percentage = Math.round(probability * 100);

  const circumference = 2 * Math.PI * 82;
  const progress = circumference * probability;

  return (
    <div className="rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-6">

      <div className="flex items-center justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            Current System Risk
          </p>

          <p className="mt-1 text-sm text-white/40">
            Weighted detection result
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-full border border-emerald-400/10 bg-emerald-400/[0.05] px-3 py-1.5">

          <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />

          <span className="text-[10px] font-medium uppercase tracking-wider text-emerald-400">
            Live
          </span>

        </div>

      </div>

      <div className="mt-8 flex items-center justify-center">

        <div className="relative h-[210px] w-[210px]">

          <svg
            className="h-full w-full -rotate-90"
            viewBox="0 0 200 200"
          >

            <circle
              cx="100"
              cy="100"
              r="82"
              fill="none"
              stroke="rgba(255,255,255,0.05)"
              strokeWidth="10"
            />

            <circle
              cx="100"
              cy="100"
              r="82"
              fill="none"
              stroke="rgba(255,255,255,0.8)"
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={circumference - progress}
            />

          </svg>

          <div className="absolute inset-0 flex flex-col items-center justify-center">

            <div className="text-5xl font-semibold tracking-tight text-white">
              {percentage}%
            </div>

            <div className="mt-1 flex items-center gap-1.5 text-xs text-white/35">
              <ShieldCheck size={13} />
              {riskLevel} RISK
            </div>

          </div>

        </div>

      </div>

      <div className="mt-5 flex items-center justify-center gap-2 text-xs text-white/30">

        <Activity size={13} />

        <span>
          Updated just now
        </span>

      </div>

    </div>
  );
}
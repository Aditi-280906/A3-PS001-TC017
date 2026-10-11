import {
  BrainCircuit,
  Eye,
  Gauge,
  Network,
} from "lucide-react";

interface ModelAnalysisProps {
  models: {
    mpvg: number;
    sfim: number;
    eap: number;
    cait: number;
  };
  finalProbability: number;
}

const modelInfo = [
  {
    key: "mpvg",
    name: "MPVG",
    description: "Permission & volume behaviour",
    icon: BrainCircuit,
    weight: 0.18,
  },
  {
    key: "sfim",
    name: "SFIM",
    description: "File-system behaviour",
    icon: Network,
    weight: 0.26,
  },
  {
    key: "eap",
    name: "EAP",
    description: "Entropy activity",
    icon: Gauge,
    weight: 0.31,
  },
  {
    key: "cait",
    name: "CAIT",
    description: "System call behaviour",
    icon: Eye,
    weight: 0.25,
  },
] as const;

export default function ModelAnalysis({
  models,
  finalProbability,
}: ModelAnalysisProps) {
  return (
    <div className="rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-6">

      <div className="flex items-start justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            Detection Layers
          </p>

          <p className="mt-1 text-sm text-white/40">
            Four independent model outputs
          </p>
        </div>

        <div className="rounded-lg bg-white/[0.04] px-2.5 py-1.5 text-[10px] text-white/30">
          WEIGHTED
        </div>

      </div>

      <div className="mt-6 space-y-4">

        {modelInfo.map((model) => {

          const Icon = model.icon;
          const value = models[model.key];
          const percentage = Math.round(value * 100);

          return (
            <div key={model.key}>

              <div className="flex items-center justify-between">

                <div className="flex items-center gap-3">

                  <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/[0.04] text-white/50">
                    <Icon size={15} strokeWidth={1.8} />
                  </div>

                  <div>
                    <div className="flex items-center gap-2">

                      <span className="text-xs font-medium text-white/80">
                        {model.name}
                      </span>

                      <span className="text-[9px] text-white/20">
                        W {model.weight}
                      </span>

                    </div>

                    <p className="mt-0.5 text-[10px] text-white/25">
                      {model.description}
                    </p>
                  </div>

                </div>

                <span className="text-sm font-medium text-white/80">
                  {percentage}%
                </span>

              </div>

              <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-white/[0.05]">

                <div
                  className="h-full rounded-full bg-white/70 transition-all duration-700"
                  style={{
                    width: `${percentage}%`,
                  }}
                />

              </div>

            </div>
          );
        })}

      </div>

      <div className="mt-6 border-t border-white/[0.06] pt-5">

        <div className="flex items-end justify-between">

          <div>
            <p className="text-[10px] uppercase tracking-[0.13em] text-white/25">
              Final weighted probability
            </p>

            <p className="mt-1 text-xs text-white/35">
              Based on current time window
            </p>
          </div>

          <p className="text-2xl font-semibold text-white">
            {Math.round(finalProbability * 100)}%
          </p>

        </div>

      </div>

    </div>
  );
}
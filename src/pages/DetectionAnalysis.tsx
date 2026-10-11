import { ChartNoAxesCombined, Info, ShieldAlert } from "lucide-react";

const results = [
  { name: "MPVG", accuracy: 67.64, precision: 63.64, recall: 100.0, f1: 77.78, fpr: 74.63, auc: 75.34 },
  { name: "SFIM", accuracy: 99.68, precision: 100.0, recall: 99.43, f1: 99.71, fpr: 0.0, auc: 99.43 },
  { name: "EAP", accuracy: 80.58, precision: 74.68, recall: 99.43, f1: 85.29, fpr: 44.03, auc: 89.22 },
  { name: "CAIT", accuracy: 98.71, precision: 98.31, recall: 99.43, f1: 98.86, fpr: 2.24, auc: 98.84 },
  { name: "Weighted Fusion", accuracy: 99.03, precision: 98.86, recall: 99.43, f1: 99.15, fpr: 1.49, auc: 99.57 },
];

const scenarios = [
  { name: "B1 — Backup and copy", label: "Benign", runs: 73, mpvg: 23.3, sfim: 100, eap: 19.2, cait: 97.3, fusion: 97.3 },
  { name: "B2 — Archive and compress", label: "Benign", runs: 61, mpvg: 27.9, sfim: 100, eap: 100, cait: 98.4, fusion: 100 },
  { name: "R1 — In-place rewrite", label: "Malicious", runs: 58, mpvg: 100, sfim: 100, eap: 100, cait: 100, fusion: 100 },
  { name: "R2 — Copy, encrypt, delete", label: "Malicious", runs: 58, mpvg: 100, sfim: 100, eap: 100, cait: 100, fusion: 100 },
  { name: "R3 — Staged and slow", label: "Malicious", runs: 59, mpvg: 100, sfim: 98.3, eap: 98.3, cait: 98.3, fusion: 98.3 },
];

function pct(value: number) {
  return `${value.toFixed(2)}%`;
}

export default function DetectionAnalysis() {
  return (
    <div className="space-y-6 pb-10">
      <div>
        <div className="flex items-center gap-2 text-xs font-semibold tracking-[0.16em] text-white/35">
          <ChartNoAxesCombined size={15} />
          MODEL EVALUATION
        </div>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight">Detection Analysis</h1>
        <p className="mt-1.5 text-sm text-white/40">
          Experimental performance of four independent detection models and weighted fusion.
        </p>
      </div>

      <div className="rounded-xl border border-amber-400/20 bg-amber-400/[0.05] p-4">
        <div className="flex items-start gap-3">
          <Info size={19} className="mt-0.5 shrink-0 text-amber-300" />
          <div>
            <p className="text-sm font-medium text-amber-200">Preliminary synthetic-data evaluation</p>
            <p className="mt-1 text-xs leading-5 text-white/50">
              These are the existing out-of-fold cross-validation results on 309 synthetic runs,
              using a 0.50 decision threshold. They are not evidence of equivalent real-world
              ransomware detection performance. Scenario templates and synthetic feature patterns
              may inflate measured performance.
            </p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="FUSION ACCURACY" value="99.03%" detail="Synthetic-data CV" />
        <SummaryCard label="FUSION RECALL" value="99.43%" detail="174 TP · 1 FN" />
        <SummaryCard label="FUSION F1 SCORE" value="99.15%" detail="Precision and recall" />
        <SummaryCard label="FALSE POSITIVE RATE" value="1.49%" detail="2 FP · 132 TN" />
      </div>

      <section className="overflow-hidden rounded-2xl border border-white/[0.08] bg-[#0c1017]">
        <div className="border-b border-white/[0.07] p-5">
          <h2 className="font-semibold">Model Performance Comparison</h2>
          <p className="mt-1 text-xs text-white/35">
            All values are percentages. Fusion combines the four outputs using fixed weights.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[800px] text-left text-xs">
            <thead className="bg-white/[0.025] text-white/40">
              <tr>
                {["Model", "Accuracy", "Precision", "Recall", "F1 Score", "FPR", "ROC AUC"].map((label) => (
                  <th key={label} className="px-4 py-3 font-medium">{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {results.map((row) => (
                <tr key={row.name} className={`border-t border-white/[0.06] ${row.name === "Weighted Fusion" ? "bg-white/[0.035]" : ""}`}>
                  <td className="px-4 py-4 font-medium text-white/85">
                    {row.name}
                    {row.name === "Weighted Fusion" && <div className="mt-1 text-[10px] text-emerald-300">Combined output</div>}
                  </td>
                  {[row.accuracy, row.precision, row.recall, row.f1, row.fpr, row.auc].map((value, index) => (
                    <td key={index} className={`px-4 py-4 tabular-nums ${row.name === "Weighted Fusion" ? "font-semibold text-emerald-300" : "text-white/65"}`}>
                      {pct(value)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
        <div className="flex items-center gap-2">
          <ShieldAlert size={18} className="text-emerald-300" />
          <h2 className="font-semibold">Weighted Fusion — Confusion Matrix</h2>
        </div>
        <p className="mt-1 text-xs text-white/35">309 evaluated runs · Rows are actual labels, columns are predicted labels.</p>
        <div className="mt-5 grid grid-cols-[1fr_1fr_1fr] gap-2 text-center text-xs">
          <div />
          <div className="rounded-lg bg-white/[0.04] p-3 text-white/50">Predicted benign</div>
          <div className="rounded-lg bg-white/[0.04] p-3 text-white/50">Predicted ransomware</div>

          <div className="flex items-center justify-center text-white/50">Actual benign</div>
          <div className="rounded-xl border border-emerald-400/20 bg-emerald-400/[0.08] p-5">
            <div className="text-2xl font-semibold text-emerald-300">132</div>
            <div className="mt-1 text-white/50">True negatives</div>
          </div>
          <div className="rounded-xl border border-amber-400/20 bg-amber-400/[0.07] p-5">
            <div className="text-2xl font-semibold text-amber-300">2</div>
            <div className="mt-1 text-white/50">False positives</div>
          </div>

          <div className="flex items-center justify-center text-white/50">Actual ransomware</div>
          <div className="rounded-xl border border-amber-400/20 bg-amber-400/[0.07] p-5">
            <div className="text-2xl font-semibold text-amber-300">1</div>
            <div className="mt-1 text-white/50">False negative</div>
          </div>
          <div className="rounded-xl border border-emerald-400/20 bg-emerald-400/[0.08] p-5">
            <div className="text-2xl font-semibold text-emerald-300">174</div>
            <div className="mt-1 text-white/50">True positives</div>
          </div>
        </div>
      </section>

      <section className="overflow-hidden rounded-2xl border border-white/[0.08] bg-[#0c1017]">
        <div className="border-b border-white/[0.07] p-5">
          <h2 className="font-semibold">Scenario-wise Results</h2>
          <p className="mt-1 text-xs text-white/35">
            Percentage of runs correctly classified within each scenario.
          </p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[900px] text-left text-xs">
            <thead className="bg-white/[0.025] text-white/40">
              <tr>
                {["Scenario", "Class", "Runs", "MPVG", "SFIM", "EAP", "CAIT", "Fusion"].map((label) => (
                  <th key={label} className="px-4 py-3 font-medium">{label}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {scenarios.map((row) => (
                <tr key={row.name} className="border-t border-white/[0.06]">
                  <td className="px-4 py-4 font-medium text-white/80">{row.name}</td>
                  <td className="px-4 py-4">
                    <span className={row.label === "Benign" ? "text-sky-300" : "text-rose-300"}>{row.label}</span>
                  </td>
                  <td className="px-4 py-4 text-white/60">{row.runs}</td>
                  {[row.mpvg, row.sfim, row.eap, row.cait, row.fusion].map((value, index) => (
                    <td key={index} className="px-4 py-4 tabular-nums text-white/65">{value.toFixed(1)}%</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="p-4 text-xs leading-5 text-white/35">
          Important: perfect results on templated synthetic scenarios do not establish generalization
          to unseen ransomware families or realistic benign workloads.
        </p>
      </section>

      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
        <h2 className="font-semibold">Weighted Fusion Configuration</h2>
        <p className="mt-1 text-xs text-white/40">Fixed weights · Decision threshold 0.50</p>
        <div className="mt-5 space-y-4">
          {[
            { name: "MPVG", weight: 18, color: "#60a5fa" },
            { name: "SFIM", weight: 26, color: "#a78bfa" },
            { name: "EAP", weight: 31, color: "#fbbf24" },
            { name: "CAIT", weight: 25, color: "#2dd4bf" },
          ].map((model) => (
            <div key={model.name}>
              <div className="mb-2 flex justify-between text-xs">
                <span className="text-white/70">{model.name}</span>
                <span className="text-white/45">{model.weight}%</span>
              </div>
              <div className="h-1.5 overflow-hidden rounded-full bg-white/[0.07]">
                <div className="h-full rounded-full" style={{ width: `${model.weight}%`, background: model.color }} />
              </div>
            </div>
          ))}
        </div>
        <div className="mt-5 rounded-lg bg-white/[0.025] p-3 font-mono text-xs leading-6 text-white/55">
          Pfinal = 0.18 × P(MPVG) + 0.26 × P(SFIM)
          <br />
          + 0.31 × P(EAP) + 0.25 × P(CAIT)
        </div>
      </section>
    </div>
  );
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <div className="rounded-2xl border border-white/[0.08] bg-[#0c1017] p-5">
      <p className="text-[10px] font-semibold tracking-[0.13em] text-white/35">{label}</p>
      <p className="mt-3 text-2xl font-semibold">{value}</p>
      <p className="mt-1 text-xs text-white/35">{detail}</p>
    </div>
  );
}

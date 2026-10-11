import { useState } from "react";
import { Activity, Clock, Search, ShieldAlert } from "lucide-react";

type Prediction = "BENIGN" | "RANSOMWARE";

const demoHistory = [
  {
    id: "RUN-0309",
    device: "Android Emulator",
    prediction: "BENIGN" as Prediction,
    risk: 18.4,
    time: "Demo record 1",
  },
  {
    id: "RUN-0308",
    device: "Android Emulator",
    prediction: "RANSOMWARE" as Prediction,
    risk: 82.6,
    time: "Demo record 2",
  },
  {
    id: "RUN-0307",
    device: "Development Workstation",
    prediction: "BENIGN" as Prediction,
    risk: 12.1,
    time: "Demo record 3",
  },
  {
    id: "RUN-0306",
    device: "Android Emulator",
    prediction: "RANSOMWARE" as Prediction,
    risk: 76.3,
    time: "Demo record 4",
  },
  {
    id: "RUN-0305",
    device: "Development Workstation",
    prediction: "BENIGN" as Prediction,
    risk: 23.8,
    time: "Demo record 5",
  },
];

export default function History() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");

  const filteredHistory = demoHistory.filter((item) => {
    const matchesQuery =
      item.id.toLowerCase().includes(query.toLowerCase()) ||
      item.device.toLowerCase().includes(query.toLowerCase());

    const matchesFilter =
      filter === "ALL" || item.prediction === filter;

    return matchesQuery && matchesFilter;
  });

  const ransomwareCount = demoHistory.filter(
    (item) => item.prediction === "RANSOMWARE"
  ).length;

  const benignCount = demoHistory.filter(
    (item) => item.prediction === "BENIGN"
  ).length;

  return (
    <div className="space-y-6 pb-10">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Detection History
          </h1>
          <p className="mt-1.5 text-sm text-white/40">
            Review previous detection runs and their fused risk scores.
          </p>
        </div>

        <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-3 py-1.5 text-xs text-amber-300">
          DEMO DATA
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Summary
          icon={<Activity size={18} />}
          label="TOTAL RUNS"
          value={String(demoHistory.length)}
          detail="Illustrative records"
        />
        <Summary
          icon={<ShieldAlert size={18} />}
          label="RANSOMWARE PREDICTIONS"
          value={String(ransomwareCount)}
          detail="Demo classifications"
        />
        <Summary
          icon={<Clock size={18} />}
          label="BENIGN PREDICTIONS"
          value={String(benignCount)}
          detail="Demo classifications"
        />
      </div>

      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017]">
        <div className="flex flex-col gap-4 border-b border-white/[0.07] p-5 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 className="font-semibold">Detection Records</h2>
            <p className="mt-1 text-xs text-white/35">
              These are sample records, not actual emulator results.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="flex items-center gap-2 rounded-lg border border-white/[0.08] px-3 py-2">
              <Search size={15} className="text-white/35" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search records..."
                className="w-full bg-transparent text-sm outline-none placeholder:text-white/25 sm:w-48"
              />
            </div>

            <select
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              className="rounded-lg border border-white/[0.08] bg-[#10151e] px-3 py-2 text-sm text-white/70 outline-none"
            >
              <option value="ALL">All predictions</option>
              <option value="BENIGN">Benign</option>
              <option value="RANSOMWARE">Ransomware</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="bg-white/[0.025] text-xs text-white/40">
              <tr>
                <th className="px-5 py-4 font-medium">Run ID</th>
                <th className="px-5 py-4 font-medium">Device</th>
                <th className="px-5 py-4 font-medium">Prediction</th>
                <th className="px-5 py-4 font-medium">Fused risk</th>
                <th className="px-5 py-4 font-medium">Record time</th>
              </tr>
            </thead>

            <tbody>
              {filteredHistory.map((item) => (
                <tr
                  key={item.id}
                  className="border-t border-white/[0.06]"
                >
                  <td className="px-5 py-4 font-mono text-xs text-white/55">
                    {item.id}
                  </td>

                  <td className="px-5 py-4 text-white/75">
                    {item.device}
                  </td>

                  <td className="px-5 py-4">
                    <span
                      className={`rounded-full border px-2.5 py-1 text-[10px] ${
                        item.prediction === "RANSOMWARE"
                          ? "border-rose-400/20 bg-rose-400/10 text-rose-300"
                          : "border-emerald-400/20 bg-emerald-400/10 text-emerald-300"
                      }`}
                    >
                      {item.prediction}
                    </span>
                  </td>

                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <div className="h-1.5 w-20 overflow-hidden rounded-full bg-white/[0.07]">
                        <div
                          className={`h-full rounded-full ${
                            item.risk >= 75
                              ? "bg-rose-400"
                              : item.risk >= 50
                                ? "bg-amber-400"
                                : "bg-emerald-400"
                          }`}
                          style={{ width: `${item.risk}%` }}
                        />
                      </div>
                      <span className="text-xs text-white/65">
                        {item.risk.toFixed(1)}%
                      </span>
                    </div>
                  </td>

                  <td className="px-5 py-4 text-xs text-white/40">
                    {item.time}
                  </td>
                </tr>
              ))}

              {filteredHistory.length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    className="px-5 py-12 text-center text-sm text-white/35"
                  >
                    No records match your search.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="border-t border-white/[0.07] p-4 text-xs text-white/35">
          History is currently held in frontend sample data. Persistent
          records will be supplied by the backend during integration.
        </div>
      </section>
    </div>
  );
}

function Summary({
  icon,
  label,
  value,
  detail,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  detail: string;
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
      <p className="mt-1 text-xs text-white/35">{detail}</p>
    </div>
  );
}

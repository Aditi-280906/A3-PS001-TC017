import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const riskHistory = [
  { time: "21:20", risk: 12 },
  { time: "21:25", risk: 15 },
  { time: "21:30", risk: 18 },
  { time: "21:35", risk: 16 },
  { time: "21:40", risk: 22 },
  { time: "21:45", risk: 26 },
  { time: "21:50", risk: 30 },
  { time: "21:55", risk: 28 },
];

export default function RiskHistory() {
  return (
    <div className="rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-6">

      {/* Header */}
      <div className="flex items-start justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            Risk History
          </p>

          <p className="mt-1 text-sm text-white/40">
            Weighted probability over time
          </p>
        </div>

        <div className="flex items-center gap-2 rounded-lg border border-white/[0.06] bg-white/[0.025] px-3 py-1.5">

          <span className="h-1.5 w-1.5 rounded-full bg-white/60" />

          <span className="text-[10px] text-white/40">
            Last 40 minutes
          </span>

        </div>

      </div>

      {/* Chart */}
      <div className="mt-7 h-[260px] w-full">

        <ResponsiveContainer width="100%" height="100%">

          <AreaChart
            data={riskHistory}
            margin={{
              top: 10,
              right: 5,
              left: -20,
              bottom: 0,
            }}
          >

            <defs>
              <linearGradient
                id="riskGradient"
                x1="0"
                y1="0"
                x2="0"
                y2="1"
              >
                <stop
                  offset="0%"
                  stopColor="#ffffff"
                  stopOpacity={0.16}
                />

                <stop
                  offset="100%"
                  stopColor="#ffffff"
                  stopOpacity={0}
                />
              </linearGradient>
            </defs>

            <CartesianGrid
              stroke="rgba(255,255,255,0.05)"
              vertical={false}
            />

            <XAxis
              dataKey="time"
              tick={{
                fill: "rgba(255,255,255,0.25)",
                fontSize: 10,
              }}
              axisLine={false}
              tickLine={false}
            />

            <YAxis
              domain={[0, 100]}
              tick={{
                fill: "rgba(255,255,255,0.25)",
                fontSize: 10,
              }}
              axisLine={false}
              tickLine={false}
              tickFormatter={(value) => `${value}%`}
            />

            <Tooltip
              contentStyle={{
                background: "#11151b",
                border: "1px solid rgba(255,255,255,0.08)",
                borderRadius: "10px",
                color: "#fff",
                fontSize: "12px",
              }}
              labelStyle={{
                color: "rgba(255,255,255,0.45)",
                marginBottom: "4px",
              }}
              formatter={(value) => [`${value}%`, "Risk"]}
            />

            <Area
              type="monotone"
              dataKey="risk"
              stroke="rgba(255,255,255,0.8)"
              strokeWidth={2}
              fill="url(#riskGradient)"
              dot={false}
              activeDot={{
                r: 4,
                fill: "#ffffff",
              }}
            />

          </AreaChart>

        </ResponsiveContainer>

      </div>

      {/* Threshold */}
      <div className="mt-2 flex items-center justify-between border-t border-white/[0.05] pt-4">

        <div className="flex items-center gap-2">

          <span className="h-1.5 w-1.5 rounded-full bg-amber-400" />

          <span className="text-[10px] text-white/30">
            Detection threshold
          </span>

        </div>

        <span className="text-[11px] font-medium text-white/50">
          50%
        </span>

      </div>

    </div>
  );
}
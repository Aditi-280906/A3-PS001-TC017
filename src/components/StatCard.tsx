import type { LucideIcon } from "lucide-react";

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle: string;
  icon: LucideIcon;
  accent?: "neutral" | "green" | "yellow" | "red";
}

const accentStyles = {
  neutral: {
    icon: "bg-white/[0.06] text-white/70",
    value: "text-white",
  },

  green: {
    icon: "bg-emerald-400/10 text-emerald-400",
    value: "text-emerald-400",
  },

  yellow: {
    icon: "bg-amber-400/10 text-amber-400",
    value: "text-amber-400",
  },

  red: {
    icon: "bg-red-400/10 text-red-400",
    value: "text-red-400",
  },
};

export default function StatCard({
  title,
  value,
  subtitle,
  icon: Icon,
  accent = "neutral",
}: StatCardProps) {
  const styles = accentStyles[accent];

  return (
    <div className="group rounded-2xl border border-white/[0.07] bg-[#0b0f14] p-5 transition-all duration-300 hover:border-white/[0.12] hover:bg-[#0d1117]">

      <div className="flex items-start justify-between">

        <div>
          <p className="text-[11px] font-medium uppercase tracking-[0.14em] text-white/35">
            {title}
          </p>

          <p
            className={`mt-3 text-3xl font-semibold tracking-tight ${styles.value}`}
          >
            {value}
          </p>

          <p className="mt-1.5 text-xs text-white/30">
            {subtitle}
          </p>
        </div>

        <div
          className={`flex h-10 w-10 items-center justify-center rounded-xl ${styles.icon}`}
        >
          <Icon size={19} strokeWidth={1.8} />
        </div>

      </div>

    </div>
  );
}
import {
  Activity,
  Bell,
  ChartNoAxesCombined,
  Cpu,
  History,
  LayoutDashboard,
  Monitor,
  Settings,
  ShieldCheck,
} from "lucide-react";
import type { PageName } from "../App";

const navigation: {
  section: string;
  items: { name: PageName; icon: typeof LayoutDashboard }[];
}[] = [
  {
    section: "OVERVIEW",
    items: [{ name: "Dashboard", icon: LayoutDashboard }],
  },
  {
    section: "MONITORING",
    items: [
      { name: "Devices", icon: Monitor },
      { name: "Live Monitor", icon: Activity },
      { name: "Detection Analysis", icon: ChartNoAxesCombined },
    ],
  },
  {
    section: "SECURITY",
    items: [
      { name: "Alerts", icon: Bell },
      { name: "History", icon: History },
    ],
  },
  {
    section: "SYSTEM",
    items: [
      { name: "Reports", icon: ShieldCheck },
      { name: "Settings", icon: Settings },
    ],
  },
];

export default function Sidebar({
  activePage,
  onNavigate,
}: {
  activePage: PageName;
  onNavigate: (page: PageName) => void;
}) {
  return (
    <aside className="flex h-screen w-[250px] shrink-0 flex-col border-r border-white/[0.06] bg-[#0a0d12]">
      <div className="flex h-[76px] items-center border-b border-white/[0.06] px-6">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-black">
            <Cpu size={19} strokeWidth={2.2} />
          </div>
          <div>
            <div className="text-[16px] font-semibold tracking-wide text-white">RANSCOPE</div>
            <div className="text-[10px] tracking-[0.18em] text-white/35">THREAT INTELLIGENCE</div>
          </div>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-5">
        {navigation.map((group) => (
          <div key={group.section} className="mb-6">
            <div className="mb-2 px-3 text-[10px] font-semibold tracking-[0.16em] text-white/25">
              {group.section}
            </div>
            <div className="space-y-1">
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = item.name === activePage;
                return (
                  <button
                    key={item.name}
                    onClick={() => onNavigate(item.name)}
                    className={`group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-[13px] transition-all duration-200 ${
                      isActive
                        ? "bg-white/[0.08] text-white"
                        : "text-white/45 hover:bg-white/[0.04] hover:text-white/80"
                    }`}
                  >
                    <Icon
                      size={17}
                      strokeWidth={1.8}
                      className={isActive ? "text-white" : "text-white/35 group-hover:text-white/70"}
                    />
                    <span>{item.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="border-t border-white/[0.06] p-4">
        <div className="rounded-xl border border-white/[0.06] bg-white/[0.025] p-3">
          <div className="flex items-center gap-2">
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-50" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
            </span>
            <span className="text-[11px] font-medium text-white/70">UI operational</span>
          </div>
          <div className="mt-1 pl-4 text-[10px] text-white/25">Backend integration pending</div>
        </div>
      </div>
    </aside>
  );
}

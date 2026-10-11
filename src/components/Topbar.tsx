import {
  Bell,
  ChevronDown,
  Search,
} from "lucide-react";

export default function Topbar() {
  return (
    <header className="flex h-[76px] items-center justify-between border-b border-white/[0.06] bg-[#080b10]/95 px-7 backdrop-blur-xl">

      {/* Page heading */}
      <div>

        <div className="text-[11px] font-medium uppercase tracking-[0.16em] text-white/30">
          Security Operations
        </div>

        <div className="mt-1 text-[17px] font-medium text-white">
          Command Center
        </div>

      </div>

      {/* Right side */}
      <div className="flex items-center gap-5">

        {/* Search */}
        <button
          className="flex h-9 w-9 items-center justify-center rounded-lg text-white/35 transition hover:bg-white/[0.05] hover:text-white"
          aria-label="Search"
        >
          <Search size={18} strokeWidth={1.8} />
        </button>

        {/* Notifications */}
        <button
          className="relative flex h-9 w-9 items-center justify-center rounded-lg text-white/35 transition hover:bg-white/[0.05] hover:text-white"
          aria-label="Notifications"
        >
          <Bell size={18} strokeWidth={1.8} />

          <span className="absolute right-[7px] top-[6px] h-1.5 w-1.5 rounded-full bg-red-400" />
        </button>

        {/* Divider */}
        <div className="h-7 w-px bg-white/[0.08]" />

        {/* User */}
        <button className="flex items-center gap-3">

          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/[0.08] text-xs font-medium text-white">
            AD
          </div>

          <div className="hidden text-left sm:block">

            <div className="text-[12px] font-medium text-white/80">
              Administrator
            </div>

            <div className="text-[10px] text-white/30">
              System Admin
            </div>

          </div>

          <ChevronDown
            size={15}
            className="text-white/30"
          />

        </button>

      </div>

    </header>
  );
}
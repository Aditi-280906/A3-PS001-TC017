import {
  Activity,
  Monitor,
  Search,
  ShieldCheck,
  Wifi,
  WifiOff,
} from "lucide-react";
import { useState } from "react";

const initialDevices = [
  {
    id: "DEV-001",
    name: "Android Emulator",
    machine: "Pixel 7 · API 35",
    status: "ONLINE",
    risk: 30,
    lastSeen: "Demo session",
  },
  {
    id: "DEV-002",
    name: "Development Workstation",
    machine: "macOS · Apple Silicon",
    status: "ONLINE",
    risk: 12,
    lastSeen: "Demo session",
  },
  {
    id: "DEV-003",
    name: "Test Device 03",
    machine: "Android test environment",
    status: "OFFLINE",
    risk: 0,
    lastSeen: "Not connected",
  },
];

export default function Devices() {
  const [query, setQuery] = useState("");
  const [filter, setFilter] = useState("ALL");

  const filteredDevices = initialDevices.filter((device) => {
    const matchesQuery =
      device.name.toLowerCase().includes(query.toLowerCase()) ||
      device.id.toLowerCase().includes(query.toLowerCase()) ||
      device.machine.toLowerCase().includes(query.toLowerCase());

    const matchesFilter =
      filter === "ALL" || device.status === filter;

    return matchesQuery && matchesFilter;
  });

  const onlineCount = initialDevices.filter(
    (device) => device.status === "ONLINE"
  ).length;

  return (
    <div className="space-y-6 pb-10">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Protected Devices
          </h1>
          <p className="mt-1.5 text-sm text-white/40">
            Manage endpoints and inspect their monitoring status.
          </p>
        </div>

        <span className="rounded-full border border-amber-400/20 bg-amber-400/10 px-3 py-1.5 text-xs text-amber-300">
          DEMO DATA
        </span>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Summary
          icon={<Monitor size={18} />}
          label="REGISTERED DEVICES"
          value={String(initialDevices.length)}
          detail="Sample inventory"
        />
        <Summary
          icon={<Wifi size={18} />}
          label="ONLINE"
          value={String(onlineCount)}
          detail="Simulated connection status"
        />
        <Summary
          icon={<ShieldCheck size={18} />}
          label="MONITORED"
          value={String(onlineCount)}
          detail="Backend connection pending"
        />
      </div>

      <section className="rounded-2xl border border-white/[0.08] bg-[#0c1017]">
        <div className="flex flex-col gap-4 border-b border-white/[0.07] p-5 md:flex-row md:items-center md:justify-between">
          <div>
            <h2 className="font-semibold">Device Inventory</h2>
            <p className="mt-1 text-xs text-white/35">
              Sample devices for the RANSCOPE interface.
            </p>
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <div className="flex items-center gap-2 rounded-lg border border-white/[0.08] px-3 py-2">
              <Search size={15} className="text-white/35" />
              <input
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Search devices..."
                className="w-full bg-transparent text-sm outline-none placeholder:text-white/25 sm:w-48"
              />
            </div>

            <select
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              className="rounded-lg border border-white/[0.08] bg-[#10151e] px-3 py-2 text-sm text-white/70 outline-none"
            >
              <option value="ALL">All devices</option>
              <option value="ONLINE">Online</option>
              <option value="OFFLINE">Offline</option>
            </select>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="bg-white/[0.025] text-xs text-white/40">
              <tr>
                <th className="px-5 py-4 font-medium">Device</th>
                <th className="px-5 py-4 font-medium">Device ID</th>
                <th className="px-5 py-4 font-medium">Connection</th>
                <th className="px-5 py-4 font-medium">Risk score</th>
                <th className="px-5 py-4 font-medium">Last seen</th>
              </tr>
            </thead>

            <tbody>
              {filteredDevices.map((device) => (
                <tr
                  key={device.id}
                  className="border-t border-white/[0.06]"
                >
                  <td className="px-5 py-4">
                    <div className="flex items-center gap-3">
                      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/[0.05] text-white/60">
                        <Monitor size={18} />
                      </div>
                      <div>
                        <div className="font-medium text-white/85">
                          {device.name}
                        </div>
                        <div className="mt-1 text-xs text-white/35">
                          {device.machine}
                        </div>
                      </div>
                    </div>
                  </td>

                  <td className="px-5 py-4 font-mono text-xs text-white/50">
                    {device.id}
                  </td>

                  <td className="px-5 py-4">
                    <span
                      className={`inline-flex items-center gap-2 rounded-full px-2.5 py-1 text-xs ${
                        device.status === "ONLINE"
                          ? "bg-emerald-400/10 text-emerald-300"
                          : "bg-white/[0.05] text-white/40"
                      }`}
                    >
                      {device.status === "ONLINE" ? (
                        <Wifi size={13} />
                      ) : (
                        <WifiOff size={13} />
                      )}
                      {device.status}
                    </span>
                  </td>

                  <td className="px-5 py-4">
                    {device.status === "ONLINE" ? (
                      <div className="flex items-center gap-3">
                        <div className="h-1.5 w-20 overflow-hidden rounded-full bg-white/[0.07]">
                          <div
                            className="h-full rounded-full bg-sky-400"
                            style={{ width: `${device.risk}%` }}
                          />
                        </div>
                        <span className="text-xs text-white/65">
                          {device.risk}%
                        </span>
                      </div>
                    ) : (
                      <span className="text-xs text-white/30">—</span>
                    )}
                  </td>

                  <td className="px-5 py-4 text-xs text-white/40">
                    {device.lastSeen}
                  </td>
                </tr>
              ))}

              {filteredDevices.length === 0 && (
                <tr>
                  <td
                    colSpan={5}
                    className="px-5 py-12 text-center text-sm text-white/35"
                  >
                    No devices match your search.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center gap-2 border-t border-white/[0.07] p-4 text-xs text-white/35">
          <Activity size={14} />
          Device records are currently illustrative; live telemetry is not connected.
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

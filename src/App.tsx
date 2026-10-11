
import { useState } from "react";

import Sidebar from "./components/Sidebar";
import Topbar from "./components/Topbar";

import Dashboard from "./pages/Dashboard";
import LiveMonitor from "./pages/LiveMonitor";
import DetectionAnalysis from "./pages/DetectionAnalysis";
import Devices from "./pages/Devices";
import Alerts from "./pages/Alerts";
import History from "./pages/History";

export type PageName =
  | "Dashboard"
  | "Devices"
  | "Live Monitor"
  | "Detection Analysis"
  | "Alerts"
  | "History"
  | "Reports"
  | "Settings";

export default function App() {
  const [activePage, setActivePage] =
    useState<PageName>("Dashboard");

  const renderPage = () => {
    switch (activePage) {
      case "Dashboard":
        return <Dashboard />;

      case "Devices":
        return <Devices />;

      case "Live Monitor":
        return <LiveMonitor />;

      case "Detection Analysis":
        return <DetectionAnalysis />;

      case "Alerts":
        return <Alerts />;

      case "History":
        return <History />;

      case "Reports":
      case "Settings":
        return (
          <div className="py-12">
            <h1 className="text-2xl font-semibold">
              {activePage}
            </h1>

            <p className="mt-2 text-sm text-white/40">
              This section will be implemented next.
            </p>
          </div>
        );

      default:
        return <Dashboard />;
    }
  };

  return (
    <div className="flex h-screen overflow-hidden bg-[#07090d] text-white">
      <Sidebar
        activePage={activePage}
        onNavigate={setActivePage}
      />

      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar />

        <main className="flex-1 overflow-y-auto">
          <div className="mx-auto w-full max-w-[1600px] p-7">
            {renderPage()}
          </div>
        </main>
      </div>
    </div>
  );
}

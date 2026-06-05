import { useState } from "react";
import DashboardView from "./DashboardView";
import AccountView from "./AccountView";
import usePopupContext from "../hooks/usePopupContext";
import { LayoutDashboard, Wallet, FileText, X, ExternalLink } from "lucide-react";

export default function App() {
  const [selectedAccount, setSelectedAccount] = useState<number | null>(null);
  const [currentPopup, setCurrentPopup] = usePopupContext();

  return (
    <div className="min-h-screen bg-gray-50">
      <nav className="bg-white border-b border-gray-200 px-8 py-4">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center gap-3">
            <div className="bg-blue-600 p-2 rounded-lg">
              <Wallet className="w-5 h-5 text-white" />
            </div>
            <h1 className="text-xl font-semibold text-gray-900">Budgeting App</h1>
            <div className="h-6 w-px bg-gray-300 mx-2" />
            <div className="flex items-center gap-2">
              {selectedAccount === null ? (
                <LayoutDashboard className="w-4 h-4 text-blue-600" />
              ) : (
                <FileText className="w-4 h-4 text-blue-600" />
              )}
              <h2 className="text-gray-700 font-medium">{selectedAccount === null ? "Dashboard" : "Account"}</h2>
            </div>
          </div>
          <a
            href="/swagger-ui/index.html"
            className="flex items-center gap-2 px-4 py-2 text-sm text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
          >
            API Docs
            <ExternalLink className="w-3.5 h-3.5" />
          </a>
        </div>
      </nav>

      <main className="p-8 max-w-7xl mx-auto">
        {selectedAccount === null ? (
          <DashboardView setSelectedAccount={setSelectedAccount} />
        ) : (
          <AccountView selectedAccount={selectedAccount} setSelectedAccount={setSelectedAccount} />
        )}
      </main>
      {currentPopup && (
        <div
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
          onClick={() => setCurrentPopup(null)}
        >
          <div
            className="bg-white rounded-xl shadow-xl min-w-[320px] max-w-md relative"
            onClick={(e) => e.stopPropagation()}
          >
            <button
              onClick={() => setCurrentPopup(null)}
              className="absolute top-4 right-4 text-gray-400 hover:text-red-500 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
            <div className="p-6 pt-12">{currentPopup}</div>
          </div>
        </div>
      )}
    </div>
  );
}

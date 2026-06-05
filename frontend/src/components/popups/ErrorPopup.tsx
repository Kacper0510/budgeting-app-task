import { AlertCircle } from "lucide-react";
import usePopupContext from "../../hooks/usePopupContext";

export default function ErrorPopup({ message }: { message: string }) {
  const setCurrentPopup = usePopupContext()[1];
  return (
    <div>
      <div className="flex flex-col gap-3">
        <div className="flex gap-2 items-center bg-red-50 p-2 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-500" />
          <h3 className="text-lg pt-1 font-semibold text-gray-900 mb-1">Error</h3>
        </div>
        <div className="flex-1 px-3">
          <p className="text-gray-600">{message}</p>
        </div>
      </div>
      <div className="flex justify-center mt-6">
        <button
          onClick={() => setCurrentPopup(null)}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
        >
          Got it
        </button>
      </div>
    </div>
  );
}

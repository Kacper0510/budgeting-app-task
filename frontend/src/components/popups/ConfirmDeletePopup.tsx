import { AlertCircle } from "lucide-react";
import usePopupContext from "../../hooks/usePopupContext";

export default function ConfirmDeletePopup({ onConfirm, itemName }: { onConfirm: () => void; itemName: string }) {
  const setCurrentPopup = usePopupContext()[1];
  return (
    <div>
      <div className="flex flex-col gap-3">
        <div className="flex gap-2 items-center bg-red-50 p-2 rounded-lg">
          <AlertCircle className="w-5 h-5 text-red-500" />
          <h3 className="text-lg pt-1 font-semibold text-gray-900 mb-1">Confirm Delete</h3>
        </div>
        <div className="flex-1 px-3">
          <p className="text-gray-600">Are you sure you want to delete "{itemName}"?</p>
        </div>
      </div>
      <div className="flex justify-center gap-3 mt-6">
        <button
          onClick={() => setCurrentPopup(null)}
          className="px-4 py-2 bg-gray-200 text-gray-700 text-sm font-medium rounded-lg hover:bg-gray-300 transition-colors cursor-pointer"
        >
          Cancel
        </button>
        <button
          onClick={() => {
            onConfirm();
            setCurrentPopup(null);
          }}
          className="px-4 py-2 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 transition-colors cursor-pointer"
        >
          Delete
        </button>
      </div>
    </div>
  );
}

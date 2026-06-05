import axios from "axios";
import { Wallet, DollarSign } from "lucide-react";
import useMutationNotify from "../../hooks/useMutationNotify";
import usePopupContext from "../../hooks/usePopupContext";
import { useState } from "react";

type CreateAccountRequest = {
  name: string;
  initialBalance: number;
};

export default function AddAccountPopup() {
  const setPopup = usePopupContext()[1];

  const addAccount = useMutationNotify({
    queryKey: ["accounts"],
    mutationFn: (request: CreateAccountRequest) => axios.post("/api/accounts", request),
  });

  const [name, setName] = useState("");
  const [initialBalance, setInitialBalance] = useState(0);

  return (
    <form
      onSubmit={() => {
        addAccount({ name: name.trim(), initialBalance: Math.round(initialBalance * 100) });
        setPopup(null);
      }}
      className="p-6"
    >
      <div className="flex items-start gap-3 mb-6">
        <div className="bg-blue-50 p-2 rounded-lg">
          <Wallet className="w-5 h-5 text-blue-600" />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Create New Account</h3>
          <p className="text-sm text-gray-500 mt-1">Add a new bank account or wallet</p>
        </div>
      </div>

      <div className="space-y-4">
        {/* Account Name Field */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
            Account Name
          </label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., Checking Account, Savings, Cash"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
            autoFocus
          />
        </div>

        {/* Initial Balance Field */}
        <div>
          <label htmlFor="balance" className="block text-sm font-medium text-gray-700 mb-1">
            Initial Balance
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <DollarSign className="w-4 h-4 text-gray-400" />
            </div>
            <input
              id="balance"
              type="number"
              step="0.01"
              value={initialBalance}
              onChange={(e) => setInitialBalance(parseFloat(e.target.value) || 0)}
              className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
              placeholder="0.00"
            />
          </div>
          <p className="text-xs text-gray-500 mt-1">Starting balance for this account</p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex justify-center gap-3 mt-6 pt-4 border-t border-gray-100">
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer flex items-center gap-2"
        >
          Create Account
        </button>
      </div>
    </form>
  );
}

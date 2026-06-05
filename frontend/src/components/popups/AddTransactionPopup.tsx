import axios from "axios";
import { Plus, DollarSign, Tag, TrendingUp, TrendingDown } from "lucide-react";
import useMutationNotify from "../../hooks/useMutationNotify";
import usePopupContext from "../../hooks/usePopupContext";
import type { Category, CreateTransactionRequest, TransactionType } from "../../types";
import useQueryNotify from "../../hooks/useQueryNotify";
import { useState } from "react";

export default function AddTransactionPopup({ accountId }: { accountId: number }) {
  const setPopup = usePopupContext()[1];

  const categories = useQueryNotify<Category[]>({
    queryKey: ["categories"],
    queryFn: () => axios.get("/api/categories").then((res) => res.data),
  });

  const addTransaction = useMutationNotify({
    queryKey: ["account"],
    mutationFn: (request: CreateTransactionRequest) => axios.post(`/api/accounts/${accountId}/transactions`, request),
  });

  const [amount, setAmount] = useState<number>(0);
  const [type, setType] = useState<TransactionType>("EXPENSE");
  const [categoryId, setCategoryId] = useState<number | null>(null);
  const [description, setDescription] = useState("");

  if (categories.isLoading) {
    return (
      <div className="p-6 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const categoriesData = categories.data || [];

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        if (!categoryId) return;
        addTransaction({
          amount: Math.round(amount * 100),
          type,
          categoryId,
          description: description.trim() || null,
        });
        setPopup(null);
      }}
      className="p-6"
    >
      <div className="flex items-start gap-3 mb-6">
        <div className="bg-blue-50 p-2 rounded-lg">
          <Plus className="w-5 h-5 text-blue-600" />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Add New Transaction</h3>
          <p className="text-sm text-gray-500 mt-1">Record an income or expense</p>
        </div>
      </div>

      <div className="space-y-4">
        {/* Transaction Type Toggle */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Transaction Type</label>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => setType("EXPENSE")}
              className={`flex-1 flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-colors cursor-pointer ${
                type === "EXPENSE" ? "bg-red-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              <TrendingDown className="w-4 h-4" />
              Expense
            </button>
            <button
              type="button"
              onClick={() => setType("INCOME")}
              className={`flex-1 flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium rounded-lg transition-colors cursor-pointer ${
                type === "INCOME" ? "bg-green-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
              }`}
            >
              <TrendingUp className="w-4 h-4" />
              Income
            </button>
          </div>
        </div>

        {/* Amount Field */}
        <div>
          <label htmlFor="amount" className="block text-sm font-medium text-gray-700 mb-1">
            Amount
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <DollarSign className="w-4 h-4 text-gray-400" />
            </div>
            <input
              id="amount"
              type="number"
              step="0.01"
              value={amount ?? ""}
              onChange={(e) => setAmount(parseFloat(e.target.value) || 0)}
              className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
              placeholder="0.00"
              autoFocus
            />
          </div>
        </div>

        {/* Category Field */}
        <div>
          <label htmlFor="category" className="block text-sm font-medium text-gray-700 mb-1">
            Category
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Tag className="w-4 h-4 text-gray-400" />
            </div>
            <select
              id="category"
              value={categoryId ?? ""}
              onChange={(e) => setCategoryId(parseInt(e.target.value) || null)}
              className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow appearance-none bg-white cursor-pointer"
            >
              <option value="">Select a category</option>
              {categoriesData.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </select>
          </div>
          {categoriesData.length === 0 && (
            <p className="text-xs text-red-600 mt-1">No categories available. Please create a category first.</p>
          )}
        </div>

        {/* Description Field */}
        <div>
          <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-1">
            Description (Optional)
          </label>
          <input
            id="description"
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="e.g., Weekly groceries, Salary, Rent payment"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
          />
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex justify-center gap-3 mt-6 pt-4 border-t border-gray-100">
        <button
          type="submit"
          disabled={!amount || amount <= 0 || !categoryId || categoriesData.length === 0}
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <Plus className="w-4 h-4" />
          Add Transaction
        </button>
      </div>
    </form>
  );
}

import axios from "axios";
import { Tag, DollarSign } from "lucide-react";
import useMutationNotify from "../../hooks/useMutationNotify";
import usePopupContext from "../../hooks/usePopupContext";
import { useState } from "react";

import type { Category } from "../../types";

type CreateCategoryRequest = Omit<Category, "id">;

export default function AddCategoryPopup() {
  const setPopup = usePopupContext()[1];

  const addCategory = useMutationNotify({
    queryKey: ["categories"],
    mutationFn: (request: CreateCategoryRequest) => axios.post("/api/categories", request),
  });

  const [name, setName] = useState("");
  const [budgetLimit, setBudgetLimit] = useState<number | null>(null);
  const [hasBudgetLimit, setHasBudgetLimit] = useState(false);

  return (
    <form
      onSubmit={() => {
        addCategory({ name: name.trim(), budgetLimit: hasBudgetLimit ? Math.round(budgetLimit! * 100) : null });
        setPopup(null);
      }}
      className="p-6"
    >
      <div className="flex items-start gap-3 mb-6">
        <div className="bg-blue-50 p-2 rounded-lg">
          <Tag className="w-5 h-5 text-blue-600" />
        </div>
        <div className="flex-1">
          <h3 className="text-lg font-semibold text-gray-900">Create New Category</h3>
          <p className="text-sm text-gray-500 mt-1">Add a new transaction category</p>
        </div>
      </div>

      <div className="space-y-4">
        {/* Category Name Field */}
        <div>
          <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-1">
            Category Name
          </label>
          <input
            id="name"
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g., Groceries, Rent, Entertainment"
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
            autoFocus
          />
        </div>

        {/* Budget Limit Field */}
        <div>
          <div className="flex items-center gap-2 mb-1">
            <input
              id="hasBudgetLimit"
              type="checkbox"
              checked={hasBudgetLimit}
              onChange={(e) => setHasBudgetLimit(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded border-gray-300 focus:ring-blue-500"
            />
            <label htmlFor="hasBudgetLimit" className="text-sm font-medium text-gray-700">
              Set budget limit
            </label>
          </div>

          {hasBudgetLimit && (
            <div className="relative mt-1">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <DollarSign className="w-4 h-4 text-gray-400" />
              </div>
              <input
                id="budgetLimit"
                type="number"
                step="0.01"
                value={budgetLimit ?? 0}
                onChange={(e) => setBudgetLimit(parseFloat(e.target.value) || 0)}
                className="w-full pl-8 pr-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-shadow"
                placeholder="0.00"
              />
            </div>
          )}
          <p className="text-xs text-gray-500 mt-1">Monthly spending limit (optional)</p>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex justify-center gap-3 mt-6 pt-4 border-t border-gray-100">
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer flex items-center gap-2"
        >
          Create Category
        </button>
      </div>
    </form>
  );
}

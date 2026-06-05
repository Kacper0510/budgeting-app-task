import axios from "axios";
import {
  Plus,
  Trash2,
  AlertCircle,
  Wallet,
  TrendingUp,
  TrendingDown,
  PieChart,
  DollarSign,
  Calendar,
} from "lucide-react";
import useQueryNotify from "../hooks/useQueryNotify";
import type { Account, Category, SummaryResponse } from "../types";
import usePopupContext from "../hooks/usePopup";
import AddAccountPopup from "./AddAccountPopup";
import AddCategoryPopup from "./AddCategoryPopup";
import useMutationNotify from "../hooks/useMutationNotify";
import { useState } from "react";

export default function DashboardView({
  setSelectedAccount,
}: {
  setSelectedAccount: (accountId: number | null) => void;
}) {
  const setPopup = usePopupContext()[1];
  const [summaryDays, setSummaryDays] = useState<number>(30);

  const summary = useQueryNotify<SummaryResponse>({
    queryKey: ["summary", summaryDays],
    queryFn: () => axios.get(`/api/summary?days=${summaryDays}`).then((res) => res.data),
  });
  const accounts = useQueryNotify<Account[]>({
    queryKey: ["accounts"],
    queryFn: () => axios.get("/api/accounts").then((res) => res.data),
  });
  const categories = useQueryNotify<Category[]>({
    queryKey: ["categories"],
    queryFn: () => axios.get("/api/categories").then((res) => res.data),
  });
  const deleteAccount = useMutationNotify({
    mutationFn: (accountId: number) => axios.delete(`/api/accounts/${accountId}`),
    queryKey: ["accounts"],
  });
  const deleteCategory = useMutationNotify({
    mutationFn: (categoryId: number) => axios.delete(`/api/categories/${categoryId}`),
    queryKey: ["categories"],
  });

  if (
    summary.isLoading ||
    accounts.isLoading ||
    categories.isLoading ||
    summary.isError ||
    accounts.isError ||
    categories.isError
  ) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const summaryData = summary.data;
  const accountsData = accounts.data || [];
  const categoriesData = categories.data || [];

  const daysOptions = [7, 30, 90, 365];

  return (
    <div className="space-y-8">
      {/* Summary Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <PieChart className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Summary</h3>
          </div>

          {/* Days selector */}
          <div className="flex items-center gap-2 bg-white border border-gray-200 rounded-lg p-1">
            <Calendar className="w-4 h-4 text-gray-500 ml-2" />
            {daysOptions.map((days) => (
              <button
                key={days}
                onClick={() => setSummaryDays(days)}
                className={`px-3 py-1.5 text-sm font-medium rounded-md transition-colors cursor-pointer ${
                  summaryDays === days ? "bg-blue-600 text-white" : "text-gray-600 hover:bg-gray-100"
                }`}
              >
                {days}d
              </button>
            ))}
          </div>
        </div>

        {summaryData && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
              {/* Total Income Card */}
              <div className="bg-linear-to-br from-green-50 to-green-100 rounded-xl p-5 border border-green-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-green-700">Total Income</span>
                  <TrendingUp className="w-5 h-5 text-green-600" />
                </div>
                <p className="text-2xl font-bold text-green-900">${summaryData.total.INCOME?.toFixed(2) ?? "0.00"}</p>
              </div>

              {/* Total Expense Card */}
              <div className="bg-linear-to-br from-red-50 to-red-100 rounded-xl p-5 border border-red-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-red-700">Total Expense</span>
                  <TrendingDown className="w-5 h-5 text-red-600" />
                </div>
                <p className="text-2xl font-bold text-red-900">${summaryData.total.EXPENSE?.toFixed(2) ?? "0.00"}</p>
              </div>

              {/* Net Balance Card */}
              <div className="bg-linear-to-br from-blue-50 to-blue-100 rounded-xl p-5 border border-blue-200">
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-blue-700">Net Balance</span>
                  <DollarSign className="w-5 h-5 text-blue-600" />
                </div>
                <p className="text-2xl font-bold text-blue-900">
                  ${((summaryData.total.INCOME ?? 0) - (summaryData.total.EXPENSE ?? 0)).toFixed(2)}
                </p>
              </div>
            </div>

            {/* Income by Category */}
            {summaryData.byCategory.INCOME && Object.keys(summaryData.byCategory.INCOME).length > 0 && (
              <div className="mb-6">
                <h4 className="text-md font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <TrendingUp className="w-4 h-4 text-green-600" />
                  Income by Category
                </h4>
                <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-gray-50 border-b border-gray-200">
                      <tr>
                        <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Category</th>
                        <th className="text-right px-6 py-3 text-sm font-semibold text-gray-900">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(summaryData.byCategory.INCOME).map(([category, amount], index, array) => (
                        <tr key={category} className={index !== array.length - 1 ? "border-b border-gray-200" : ""}>
                          <td className="px-6 py-3 text-gray-900">
                            {categoriesData.find((cat) => cat.id.toString() === category)?.name || "Unknown"}
                          </td>
                          <td className="px-6 py-3 text-right text-green-600 font-medium">${amount.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Expenses by Category */}
            {summaryData.byCategory.EXPENSE && Object.keys(summaryData.byCategory.EXPENSE).length > 0 && (
              <div className="mb-6">
                <h4 className="text-md font-semibold text-gray-900 mb-3 flex items-center gap-2">
                  <TrendingDown className="w-4 h-4 text-red-600" />
                  Expenses by Category
                </h4>
                <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
                  <table className="w-full">
                    <thead className="bg-gray-50 border-b border-gray-200">
                      <tr>
                        <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Category</th>
                        <th className="text-right px-6 py-3 text-sm font-semibold text-gray-900">Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(summaryData.byCategory.EXPENSE).map(([category, amount], index, array) => (
                        <tr key={category} className={index !== array.length - 1 ? "border-b border-gray-200" : ""}>
                          <td className="px-6 py-3 text-gray-900">
                            {categoriesData.find((cat) => cat.id.toString() === category)?.name || "Unknown"}
                          </td>
                          <td className="px-6 py-3 text-right text-red-600 font-medium">${amount.toFixed(2)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {/* Budget Warnings */}
            {summaryData.budgetLimitWarnings && summaryData.budgetLimitWarnings.length > 0 && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-xl p-4">
                <div className="flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 text-yellow-600" />
                  <p className="text-sm text-yellow-800">
                    {summaryData.budgetLimitWarnings.length} category(ies) have exceeded their budget limits
                  </p>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Accounts Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Wallet className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Accounts</h3>
          </div>
          <button
            onClick={() => setPopup(<AddAccountPopup />)}
            className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Add Account
          </button>
        </div>

        {accountsData.length === 0 ? (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
            <p className="text-gray-500">No accounts yet. Click "Add Account" to get started.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {accountsData.map((account) => (
              <div
                key={account.id}
                onClick={() => setSelectedAccount(account.id)}
                className="bg-white border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow cursor-pointer"
              >
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-gray-900">{account.name}</h4>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteAccount(account.id);
                    }}
                    className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                    title="Delete account"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
                <p className="text-2xl font-bold text-gray-900">${account.balance.toFixed(2)}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Categories Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <PieChart className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Categories</h3>
          </div>
          <button
            onClick={() => setPopup(<AddCategoryPopup />)}
            className="flex items-center gap-2 px-3 py-1.5 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            Add Category
          </button>
        </div>

        {categoriesData.length === 0 ? (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
            <p className="text-gray-500">No categories yet. Click "Add Category" to get started.</p>
          </div>
        ) : (
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Name</th>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Budget Limit</th>
                  <th className="text-right px-6 py-3 text-sm font-semibold text-gray-900">Actions</th>
                </tr>
              </thead>
              <tbody>
                {categoriesData.map((category, index) => (
                  <tr
                    key={category.id}
                    className={index !== categoriesData.length - 1 ? "border-b border-gray-200" : ""}
                  >
                    <td className="px-6 py-3 text-gray-900">{category.name}</td>
                    <td className="px-6 py-3 text-gray-600">
                      {category.budgetLimit ? `$${category.budgetLimit.toFixed(2)}` : "No limit"}
                    </td>
                    <td className="px-6 py-3 text-right">
                      <button
                        onClick={() => deleteCategory(category.id)}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                        title="Delete category"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

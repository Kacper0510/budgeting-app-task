import axios from "axios";
import {
  ArrowLeft,
  Plus,
  Trash2,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Calendar,
  Filter,
  X,
  Download,
} from "lucide-react";
import usePopupContext from "../hooks/usePopupContext";
import useQueryNotify from "../hooks/useQueryNotify";
import type { Category, GetTransactionsRequest, TransactionResponse } from "../types";
import AddTransactionPopup from "./popups/AddTransactionPopup";
import useMutationNotify from "../hooks/useMutationNotify";
import ConfirmDeletePopup from "./popups/ConfirmDeletePopup";
import { formatCurrency } from "../utils";
import { useState } from "react";

export default function AccountView({
  selectedAccount,
  setSelectedAccount,
}: {
  selectedAccount: number;
  setSelectedAccount: (accountId: number | null) => void;
}) {
  const setPopup = usePopupContext()[1];
  const [filters, setFilters] = useState<GetTransactionsRequest>({
    from: null,
    to: null,
    categoryId: null,
  });
  const [showFilters, setShowFilters] = useState(false);

  const details = useQueryNotify({
    queryKey: ["account", selectedAccount, "details"],
    queryFn: () => axios.get(`/api/accounts/${selectedAccount}`).then((res) => res.data),
  });
  const categories = useQueryNotify<Category[]>({
    queryKey: ["categories"],
    queryFn: () => axios.get("/api/categories").then((res) => res.data),
  });
  const transactions = useQueryNotify({
    queryKey: ["account", selectedAccount, "transactions", filters],
    queryFn: () =>
      axios.get(`/api/accounts/${selectedAccount}/transactions`, { params: filters }).then((res) => res.data),
  });

  const deleteTransaction = useMutationNotify({
    queryKey: ["account"], // it needs to update summary, accounts list - everything
    mutationFn: (transactionId: number) =>
      axios.delete(`/api/accounts/${selectedAccount}/transactions/${transactionId}`),
  });

  if (details.isLoading || transactions.isLoading || details.isError || transactions.isError) {
    return (
      <div className="flex items-center justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  const account = details.data;
  const transactionsData = transactions.data || [];
  const categoriesData = categories.data || [];

  const clearFilters = () => {
    setFilters({ from: null, to: null, categoryId: null });
  };

  const hasActiveFilters = filters.from || filters.to || filters.categoryId;

  const formatDateToLocalDateTime = (dateString: string | null): string | null => {
    if (!dateString) return null;
    return `${dateString}T00:00:00`;
  };

  const formatLocalDateTimeToDate = (dateTime: string | null): string => {
    if (!dateTime) return "";
    return dateTime.split("T")[0];
  };

  return (
    <div className="space-y-6">
      {/* Header with back button */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex flex-1 items-center gap-3">
          <button
            onClick={() => setSelectedAccount(null)}
            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors cursor-pointer"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <h2 className="text-2xl font-bold text-gray-900">{account.name}</h2>
            <p className="text-sm text-gray-500 mt-1">Account Overview</p>
          </div>
        </div>
        <a
          href={`/api/accounts/${selectedAccount}/transactions/export`}
          download="transactions.csv"
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
        >
          <Download className="w-4 h-4" />
          Export to CSV
        </a>
        <button
          onClick={() => setPopup(<AddTransactionPopup accountId={selectedAccount} />)}
          className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg hover:bg-blue-700 transition-colors cursor-pointer"
        >
          <Plus className="w-4 h-4" />
          Add Transaction
        </button>
      </div>

      {/* Account Balance Card */}
      <div className="bg-linear-to-br from-blue-50 to-indigo-50 rounded-xl p-6 border border-blue-200">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-blue-700">Current Balance</span>
          <DollarSign className="w-5 h-5 text-blue-600" />
        </div>
        <p className="text-3xl font-bold text-blue-900">{formatCurrency(account.balance)}</p>
      </div>

      {/* Transactions Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Calendar className="w-5 h-5 text-blue-600" />
            <h3 className="text-lg font-semibold text-gray-900">Transactions</h3>
          </div>
          <button
            onClick={() => setShowFilters(!showFilters)}
            className={`flex items-center gap-2 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors cursor-pointer ${
              hasActiveFilters ? "bg-blue-600 text-white" : "bg-gray-100 text-gray-700 hover:bg-gray-200"
            }`}
          >
            <Filter className="w-4 h-4" />
            Filters
            {hasActiveFilters && <span className="ml-1 w-2 h-2 bg-yellow-400 rounded-full"></span>}
          </button>
        </div>

        {/* Filters Panel */}
        {showFilters && (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-4 mb-4">
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-sm font-semibold text-gray-900">Filter Transactions</h4>
              {hasActiveFilters && (
                <button
                  onClick={clearFilters}
                  className="text-xs text-red-600 hover:text-red-700 flex items-center gap-1 cursor-pointer"
                >
                  <X className="w-3 h-3" />
                  Clear all
                </button>
              )}
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">From Date</label>
                <input
                  type="date"
                  value={formatLocalDateTimeToDate(filters.from)}
                  onChange={(e) => {
                    const from = formatDateToLocalDateTime(e.target.value || null);
                    if (filters.to && from && new Date(from) > new Date(filters.to)) {
                      // If the selected "from" date is after the current "to" date, reset the "to" date
                      setFilters({ ...filters, from, to: null });
                    } else {
                      setFilters({ ...filters, from });
                    }
                  }}
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">To Date</label>
                <input
                  type="date"
                  value={formatLocalDateTimeToDate(filters.to)}
                  onChange={(e) => {
                    const to = formatDateToLocalDateTime(e.target.value || null);
                    if (filters.from && to && new Date(to) < new Date(filters.from)) {
                      // If the selected "to" date is before the current "from" date, reset the "from" date
                      setFilters({ ...filters, from: null, to });
                    } else {
                      setFilters({ ...filters, to });
                    }
                  }}
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-700 mb-1">Category</label>
                <select
                  value={filters.categoryId || ""}
                  onChange={(e) =>
                    setFilters({ ...filters, categoryId: e.target.value ? Number(e.target.value) : null })
                  }
                  className="w-full px-3 py-2 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow appearance-none bg-white cursor-pointer"
                >
                  <option value="">All Categories</option>
                  {categoriesData.map((category) => (
                    <option key={category.id} value={category.id}>
                      {category.name}
                    </option>
                  ))}
                </select>
                {categoriesData.length === 0 && <p className="text-xs text-red-600 mt-1">No categories available.</p>}
              </div>
            </div>
          </div>
        )}

        {transactionsData.length === 0 ? (
          <div className="bg-gray-50 border border-gray-200 rounded-lg p-8 text-center">
            <p className="text-gray-500">
              {hasActiveFilters
                ? "No transactions match your filters. Try clearing the filters."
                : "No transactions yet. Click 'Add Transaction' to get started."}
            </p>
          </div>
        ) : (
          <div className="bg-white border border-gray-200 rounded-lg overflow-hidden">
            <table className="w-full">
              <thead className="bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Date</th>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Description</th>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Category</th>
                  <th className="text-left px-6 py-3 text-sm font-semibold text-gray-900">Type</th>
                  <th className="text-right px-6 py-3 text-sm font-semibold text-gray-900">Amount</th>
                  <th className="text-right px-6 py-3 text-sm font-semibold text-gray-900">Actions</th>
                </tr>
              </thead>
              <tbody>
                {transactionsData
                  .map((_: TransactionResponse, index: number) => transactionsData[transactionsData.length - 1 - index])
                  .map((transaction: TransactionResponse, index: number) => (
                    <tr
                      key={transaction.id}
                      className={index !== transactionsData.length - 1 ? "border-b border-gray-200" : ""}
                    >
                      <td className="px-6 py-3 text-sm text-gray-600">
                        {new Date(transaction.timestamp).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-3 text-gray-900">{transaction.description || "-"}</td>
                      <td className="px-6 py-3 text-sm text-gray-600">
                        {categoriesData.find((c) => c.id === transaction.category)?.name || "Unknown"}
                      </td>
                      <td className="px-6 py-3">
                        <span
                          className={`inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded-full ${
                            transaction.type === "INCOME" ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                          }`}
                        >
                          {transaction.type === "INCOME" ? (
                            <TrendingUp className="w-3 h-3" />
                          ) : (
                            <TrendingDown className="w-3 h-3" />
                          )}
                          {transaction.type}
                        </span>
                      </td>
                      <td
                        className={`px-6 py-3 text-right font-medium ${
                          transaction.type === "INCOME" ? "text-green-600" : "text-red-600"
                        }`}
                      >
                        {formatCurrency(transaction.amount)}
                      </td>
                      <td className="px-6 py-3 text-right">
                        <button
                          onClick={() =>
                            setPopup(
                              <ConfirmDeletePopup
                                itemName={transaction.description || "-"}
                                onConfirm={() => deleteTransaction(transaction.id)}
                              />,
                            )
                          }
                          className="p-1.5 text-red-600 hover:bg-red-50 rounded-lg transition-colors cursor-pointer"
                          title="Delete transaction"
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

export type TransactionType = "INCOME" | "EXPENSE";

export type SummaryResponse = {
  total: Record<TransactionType, number>;
  byCategory: Record<TransactionType, Record<string, number>>;
  budgetLimitWarnings: number[];
};

export type Account = {
  id: number;
  name: string;
  balance: number;
};

export type Category = {
  id: number;
  name: string;
  budgetLimit: number | null;
};

export type TransactionResponse = {
  id: number;
  amount: number;
  type: TransactionType;
  category: number;
  description: string | null;
  timestamp: string;
};

export type CreateAccountRequest = {
  name: string;
  initialBalance: number;
};

export type CreateCategoryRequest = Omit<Category, "id">;

export type CreateTransactionRequest = {
  amount: number;
  type: TransactionType;
  categoryId: number;
  description?: string | null;
};

export type GetTransactionsRequest = {
  from: string | null;
  to: string | null;
  categoryId: number | null;
};

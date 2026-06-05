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
}

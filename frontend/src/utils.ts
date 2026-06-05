export function formatCurrency(amount?: number | null): string {
  if (amount === null || amount === undefined) return "$0.00";
  return `$${(amount / 100).toFixed(2)}`;
}

export default function AccountView({
  selectedAccount,
  setSelectedAccount,
}: {
  selectedAccount: number | null;
  setSelectedAccount: (accountId: number | null) => void;
}) {
  return (
    <div>
      <h2>Account View {selectedAccount}</h2>
      <p>This is where account details will be displayed.</p>
      <button
        onClick={() => setSelectedAccount(null)}
        className="mt-4 px-4 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition-colors"
      >
        Back to Dashboard
      </button>
    </div>
  );
}

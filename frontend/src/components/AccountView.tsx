export default function AccountView({
  selectedAccount,
  setSelectedAccount,
}: {
  selectedAccount: number | null;
  setSelectedAccount: (accountId: number | null) => void;
}) {
  return (
    <div>
      <h2>Account View</h2>
      <p>This is where account details will be displayed.</p>
    </div>
  );
}

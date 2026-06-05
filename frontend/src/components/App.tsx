import { useState } from "react";

export default function App({ children }: { children?: React.ReactNode }) {
  const [selectedAccount, setSelectedAccount] = useState<number | null>(null);
  const [currentPopup, setCurrentPopup] = useState<React.ReactNode | null>(null);

  return (
    <div>
      <nav>
        <h1>Budgeting App</h1>
        <a href="/swagger-ui/index.html">Go to API docs</a>
      </nav>
      <main>
        <p>Selected account: {selectedAccount}</p>
        <button onClick={() => setCurrentPopup(<div>Popup content</div>)}>Show popup</button>
        {children}
      </main>
      <div hidden={!currentPopup}>{currentPopup}</div>
    </div>
  );
}

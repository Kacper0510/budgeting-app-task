import { createContext, useContext, useState } from "react";

type PopupContextType = [
  popup: React.ReactNode | null,
  setPopup: (popup: React.ReactNode | null) => void,
];

const PopupContext = createContext<PopupContextType>([null, () => {}]);

export function PopupProvider({ children }: { children: React.ReactNode }) {
  const state = useState<React.ReactNode | null>(null);
  return <PopupContext value={state}>{children}</PopupContext>;
}

export default function usePopupContext() {
  return useContext(PopupContext);
}

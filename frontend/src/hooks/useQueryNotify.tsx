import { useQuery } from "@tanstack/react-query";
import { useEffect } from "react";
import usePopupContext from "./usePopup";
import ErrorPopup from "../components/ErrorPopup";

export default function useQueryNotify<
  TData = unknown,
  TError = unknown,
  TQueryKey extends readonly unknown[] = readonly unknown[],
>({ queryKey, queryFn }: { queryKey: TQueryKey; queryFn: () => Promise<TData> }) {
  const setPopup = usePopupContext()[1];
  const query = useQuery<TData, TError, TData, TQueryKey>({ queryKey, queryFn });

  useEffect(() => {
    if (query.isError) {
      const error = query.error as any;
      setPopup(<ErrorPopup message={error?.response?.data?.detail ?? error.message} />);
      console.log(error);
    }
  }, [query.isError, query.error]);

  return query;
}

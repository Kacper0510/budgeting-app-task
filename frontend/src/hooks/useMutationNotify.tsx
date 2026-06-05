import { useMutation, useQueryClient, type UseMutateFunction, type QueryKey } from "@tanstack/react-query";
import usePopupContext from "./usePopupContext";
import ErrorPopup from "../components/popups/ErrorPopup";

export default function useMutationNotify<TData = unknown, TError = unknown, TVariables = void, TContext = unknown>({
  mutationFn,
  queryKey,
  additionalOnSuccess = () => {},
}: {
  mutationFn: (variables: TVariables) => Promise<TData>;
  queryKey: QueryKey;
  additionalOnSuccess?: (data: TData) => void;
}): UseMutateFunction<TData, TError, TVariables, TContext> {
  const queryClient = useQueryClient();
  const setPopup = usePopupContext()[1];

  const mutation = useMutation<TData, TError, TVariables, TContext>({
    mutationFn,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey });
      additionalOnSuccess(data);
    },
    onError: (error: any) => {
      setPopup(<ErrorPopup message={error?.response?.data?.detail ?? error.message} />);
      console.log(error);
    },
  });
  return mutation.mutate;
}

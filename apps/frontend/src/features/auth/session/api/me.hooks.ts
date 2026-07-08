'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSession } from 'next-auth/react';

import { clientApi } from '@/lib/api';
import { qk } from '@/lib/queryKeys';

import type { paths } from '@app/api-types';

type Me = paths['/api/v1/users/me']['get']['responses']['200']['content']['application/json'];
export type UpdateMeInput =
  paths['/api/v1/users/me']['patch']['requestBody']['content']['application/json'];

export function useMe() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: qk.me(),
    queryFn: () => clientApi<Me>(session?.accessToken, '/api/v1/users/me'),
    enabled: !!session?.accessToken,
  });
}

export function useUpdateMe() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: UpdateMeInput) =>
      clientApi<Me>(session?.accessToken, '/api/v1/users/me', {
        method: 'PATCH',
        body: JSON.stringify(input),
      }),
    onSuccess: (me) => {
      queryClient.setQueryData(qk.me(), me);
    },
  });
}

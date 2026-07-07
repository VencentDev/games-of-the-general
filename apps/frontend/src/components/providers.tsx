'use client';

import { MutationCache, QueryCache, QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { SessionProvider, signOut } from 'next-auth/react';
import { useState } from 'react';
import { Toaster as SonnerToaster } from 'sonner';

import type { ReactNode } from 'react';
import { ApiError } from '@/lib/api';

let authFailureRedirecting = false;

export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(
    () =>
      new QueryClient({
        mutationCache: new MutationCache({
          onError: redirectOnAuthFailure,
        }),
        queryCache: new QueryCache({
          onError: redirectOnAuthFailure,
        }),
        defaultOptions: {
          queries: {
            refetchOnWindowFocus: false,
            staleTime: 30_000,
          },
        },
      }),
  );

  return (
    <SessionProvider refetchInterval={5 * 60} refetchOnWindowFocus>
      <QueryClientProvider client={client}>
        {children}
        <SonnerToaster richColors position="top-right" />
        {process.env.NODE_ENV === 'development' && <ReactQueryDevtools />}
      </QueryClientProvider>
    </SessionProvider>
  );
}

function redirectOnAuthFailure(error: Error) {
  if (!(error instanceof ApiError) || error.status !== 401) {
    return;
  }

  if (authFailureRedirecting) {
    return;
  }

  authFailureRedirecting = true;
  void signOut({ redirectTo: loginRedirectUrl() });
}

function loginRedirectUrl() {
  if (typeof window === 'undefined') {
    return '/login?callbackUrl=/lobby';
  }

  const callbackUrl = `${window.location.pathname}${window.location.search}`;
  return `/login?callbackUrl=${encodeURIComponent(callbackUrl)}`;
}

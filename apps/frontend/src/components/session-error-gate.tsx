'use client';

import type { ReactNode } from 'react';
import { signOut, useSession } from 'next-auth/react';
import { usePathname, useSearchParams } from 'next/navigation';
import { useEffect, useRef } from 'react';

export function SessionErrorGate({ children }: { children: ReactNode }) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { data: session, status } = useSession();
  const redirectingRef = useRef(false);

  useEffect(() => {
    if (redirectingRef.current) {
      return;
    }

    if (status !== 'unauthenticated' && session?.error !== 'RefreshAccessTokenError') {
      return;
    }

    redirectingRef.current = true;
    void signOut({ redirectTo: signupRedirectUrl(pathname, searchParams.toString()) });
  }, [pathname, searchParams, session?.error, status]);

  return <>{children}</>;
}

function signupRedirectUrl(pathname: string, search: string) {
  const callbackUrl = `${pathname}${search ? `?${search}` : ''}`;
  return `/signup?callbackUrl=${encodeURIComponent(callbackUrl)}`;
}

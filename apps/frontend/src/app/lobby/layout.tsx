import type { ReactNode } from 'react';
import { redirect } from 'next/navigation';

import { SessionErrorGate } from '@/components/session-error-gate';
import { auth } from '@/lib/auth';

export default async function LobbyLayout({ children }: { children: ReactNode }) {
  const session = await auth();

  if (!session || session.error) {
    redirect('/signup?callbackUrl=/lobby');
  }

  return <SessionErrorGate>{children}</SessionErrorGate>;
}

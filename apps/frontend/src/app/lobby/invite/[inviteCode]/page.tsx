import type { Metadata } from 'next';

import { InviteMatchPageContent } from '@/features/lobby/components/invite-match-page-content';

export const metadata: Metadata = {
  title: 'Match Invite - Games of the General',
};

export default async function InviteMatchPage({
  params,
}: {
  params: Promise<{ inviteCode: string }>;
}) {
  const { inviteCode } = await params;

  return <InviteMatchPageContent inviteCode={inviteCode} />;
}

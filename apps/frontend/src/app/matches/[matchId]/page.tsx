import type { Metadata } from 'next';

import { MatchRoomPageContent } from '@/features/lobby/components/match-room-page-content';

export const metadata: Metadata = {
  title: 'Match Room - Games of the General',
};

export default async function MatchRoomPage({ params }: { params: Promise<{ matchId: string }> }) {
  const { matchId } = await params;

  return <MatchRoomPageContent matchId={matchId} />;
}

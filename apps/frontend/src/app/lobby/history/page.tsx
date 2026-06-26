import type { Metadata } from 'next';

import { MatchHistoryPageContent } from '@/features/lobby/components/match-history-page-content';

export const metadata: Metadata = {
  title: 'Match History - Games of the General',
  description: 'Review Games of the General matches you created or joined.',
};

export default function MatchHistoryPage() {
  return <MatchHistoryPageContent />;
}

import type { Metadata } from 'next';

import { FindMatchPageContent } from '@/features/lobby/components/find-match-page-content';

export const metadata: Metadata = {
  title: 'Find Match - Games of the General',
  description: 'Browse public Games of the General matches waiting for players.',
};

export default function FindMatchPage() {
  return <FindMatchPageContent />;
}

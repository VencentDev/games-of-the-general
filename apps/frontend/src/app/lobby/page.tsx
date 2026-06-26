import type { Metadata } from 'next';

import { LobbyPageContent } from '@/features/lobby/components/lobby-page-content';

export const metadata: Metadata = {
  title: 'Lobby - Games of the General',
  description: 'Create, find, and review Games of the General matches.',
};

export default function LobbyPage() {
  return <LobbyPageContent />;
}

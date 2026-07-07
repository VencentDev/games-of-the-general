'use client';

import { ArrowLeft, History } from 'lucide-react';
import Link from 'next/link';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { useMatchHistory } from '@/features/lobby/api/lobby.hooks';

export function MatchHistoryPageContent() {
  const history = useMatchHistory();
  const matches = history.data ?? [];

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#f5f1e6] px-4 py-6 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4] md:px-8">
      <section className="mx-auto max-w-7xl space-y-5">
        <div className="border-b border-[#8a7b62]/20 pb-5 dark:border-white/10">
          <Button asChild variant="ghost" className="mb-3 px-0">
            <Link href="/lobby">
              <ArrowLeft className="mr-2 size-4" />
              Lobby
            </Link>
          </Button>
          <h1 className="flex items-center gap-2 text-3xl font-black tracking-normal">
            <History className="size-7 text-[#8f2f24] dark:text-[#f29a7f]" />
            Match history
          </h1>
        </div>

        <div className="overflow-hidden rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
          <div className="grid grid-cols-[1fr_8rem_7rem_8rem_8rem] gap-3 border-b border-[#8a7b62]/20 px-4 py-3 text-xs font-black uppercase tracking-[0.14em] text-[#7c735f] dark:border-white/10 dark:text-[#b8b09e]">
            <span>Match</span>
            <span>Status</span>
            <span>Time</span>
            <span>Players</span>
            <span>Created</span>
          </div>
          <div className="divide-y divide-[#8a7b62]/20 dark:divide-white/10">
            {history.isLoading ? <EmptyRow text="Loading match history..." /> : null}
            {!history.isLoading && matches.length === 0 ? (
              <EmptyRow text="Created and joined matches will appear here." />
            ) : null}
            {matches.map((match) => (
              <div
                key={match.id}
                className="grid grid-cols-[1fr_8rem_7rem_8rem_8rem] items-center gap-3 px-4 py-4"
              >
                <div className="min-w-0">
                  <p className="truncate font-black">{match.name}</p>
                  <p className="mt-1 truncate text-sm text-[#655c51] dark:text-[#c9c1b4]">
                    {match.mode}
                  </p>
                </div>
                <Badge variant="outline" className="w-fit border-[#8a7b62]/30">
                  {formatStatus(match.status)}
                </Badge>
                <span className="text-sm font-bold">
                  {formatPreparation(match.preparationSeconds)}
                </span>
                <span className="text-sm text-[#655c51] dark:text-[#c9c1b4]">
                  {match.seats.length}/2 seated
                </span>
                <span className="text-sm text-[#655c51] dark:text-[#c9c1b4]">
                  {formatDate(match.createdAt)}
                </span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}

function EmptyRow({ text }: { text: string }) {
  return <p className="px-4 py-8 text-center text-sm text-[#655c51] dark:text-[#c9c1b4]">{text}</p>;
}

function formatStatus(status: string) {
  return status
    .toLowerCase()
    .split('_')
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(' ');
}

function formatDate(value: string | null) {
  if (!value) {
    return 'Pending';
  }

  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(
    new Date(value),
  );
}

function formatPreparation(seconds: number) {
  if (seconds === 0) {
    return 'Unlimited';
  }

  if (seconds === 60) {
    return '1 min';
  }

  return '1:30';
}

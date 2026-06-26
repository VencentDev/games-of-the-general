'use client';

import { ArrowLeft, RefreshCw, Search, Users } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useJoinMatch, usePublicMatches } from '@/features/lobby/api/lobby.hooks';

type TimeFilter = 'ALL' | '30' | '60' | '90';

export function FindMatchPageContent() {
  const router = useRouter();
  const publicMatches = usePublicMatches();
  const joinMatch = useJoinMatch();
  const [timeFilter, setTimeFilter] = useState<TimeFilter>('ALL');

  const matches = useMemo(() => {
    const rows = publicMatches.data ?? [];

    if (timeFilter === 'ALL') {
      return rows;
    }

    return rows.filter((match) => match.preparationSeconds === Number(timeFilter));
  }, [publicMatches.data, timeFilter]);

  function join(matchId: string) {
    joinMatch.mutate(matchId, {
      onSuccess: (match) => router.push(`/matches/${match.id}`),
    });
  }

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#f5f1e6] px-4 py-6 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4] md:px-8">
      <section className="mx-auto max-w-7xl space-y-5">
        <div className="flex flex-col gap-4 border-b border-[#8a7b62]/20 pb-5 dark:border-white/10 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <Button asChild variant="ghost" className="mb-3 px-0">
              <Link href="/lobby">
                <ArrowLeft className="mr-2 size-4" />
                Lobby
              </Link>
            </Button>
            <h1 className="flex items-center gap-2 text-3xl font-black tracking-normal">
              <Search className="size-7 text-[#8f2f24] dark:text-[#f29a7f]" />
              Find match
            </h1>
          </div>
          <div className="flex flex-col gap-2 sm:w-72">
            <Select
              value={timeFilter}
              onValueChange={(value) => setTimeFilter(value as TimeFilter)}
            >
              <SelectTrigger className="border-[#8a7b62]/30 bg-white/70 dark:border-white/15 dark:bg-white/5">
                <SelectValue placeholder="Filter by preparation time" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All preparation times</SelectItem>
                <SelectItem value="30">30 seconds</SelectItem>
                <SelectItem value="60">1 minute</SelectItem>
                <SelectItem value="90">1 minute 30 seconds</SelectItem>
              </SelectContent>
            </Select>
            <Button
              type="button"
              variant="outline"
              className="border-[#8a7b62]/35 bg-white/70"
              onClick={() => void publicMatches.refetch()}
            >
              <RefreshCw className="mr-2 size-4" />
              Refresh
            </Button>
          </div>
        </div>

        <div className="overflow-hidden rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
          <div className="grid grid-cols-[1fr_7rem_7rem_6rem] gap-3 border-b border-[#8a7b62]/20 px-4 py-3 text-xs font-black uppercase tracking-[0.14em] text-[#7c735f] dark:border-white/10 dark:text-[#b8b09e]">
            <span>Match</span>
            <span>Time</span>
            <span>Players</span>
            <span className="text-right">Action</span>
          </div>
          <div className="divide-y divide-[#8a7b62]/20 dark:divide-white/10">
            {publicMatches.isLoading ? <EmptyRow text="Loading public matches..." /> : null}
            {!publicMatches.isLoading && matches.length === 0 ? (
              <EmptyRow text="No public matches match the selected preparation time." />
            ) : null}
            {matches.map((match) => (
              <div
                key={match.id}
                className="grid grid-cols-[1fr_7rem_7rem_6rem] items-center gap-3 px-4 py-4"
              >
                <div className="min-w-0">
                  <p className="truncate font-black">{match.name}</p>
                  <p className="mt-1 truncate text-sm text-[#655c51] dark:text-[#c9c1b4]">
                    {match.mode}
                  </p>
                </div>
                <span className="text-sm font-bold">
                  {formatPreparation(match.preparationSeconds)}
                </span>
                <Badge variant="outline" className="w-fit border-[#6f7b4a]/30 text-[#566033]">
                  {match.seats.length}/2
                </Badge>
                <Button
                  type="button"
                  variant="outline"
                  className="justify-self-end border-[#8a7b62]/35 bg-white/70"
                  disabled={joinMatch.isPending || match.seats.length >= 2}
                  onClick={() => join(match.id)}
                >
                  <Users className="mr-2 size-4" />
                  Join
                </Button>
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

function formatPreparation(seconds: number) {
  if (seconds === 30) {
    return '30 sec';
  }

  if (seconds === 60) {
    return '1 min';
  }

  return '1:30';
}

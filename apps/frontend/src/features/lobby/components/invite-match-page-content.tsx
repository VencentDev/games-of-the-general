'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { ArrowLeft, Lock, Users } from 'lucide-react';

import { Button } from '@/components/ui/button';
import { useJoinMatch, useMatchByInviteCode } from '@/features/lobby/api/lobby.hooks';

export function InviteMatchPageContent({ inviteCode }: { inviteCode: string }) {
  const match = useMatchByInviteCode(inviteCode);
  const joinMatch = useJoinMatch();
  const router = useRouter();

  return (
    <main className="-mx-4 -my-6 flex min-h-[calc(100svh-3.5rem)] items-center bg-[#f5f1e6] px-4 py-8 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4]">
      <section className="mx-auto w-full max-w-xl rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-6 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
        <Button asChild variant="ghost" className="mb-5 px-0">
          <Link href="/lobby">
            <ArrowLeft className="mr-2 size-4" />
            Lobby
          </Link>
        </Button>

        <div className="space-y-3">
          <p className="flex items-center gap-2 text-sm font-black uppercase tracking-[0.18em] text-[#8f2f24] dark:text-[#f29a7f]">
            <Lock className="size-4" />
            Private invite
          </p>
          <h1 className="text-3xl font-black tracking-normal">
            {match.data?.name ?? 'Loading match...'}
          </h1>
          <p className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
            Invite code {inviteCode}. Join to take the open seat and begin setup when both players
            are present.
          </p>
        </div>

        {match.error ? (
          <p className="mt-5 rounded-md border border-[#8f2f24]/25 bg-[#fff3df] px-3 py-2 text-sm text-[#8f2f24] dark:border-[#f29a7f]/30 dark:bg-[#8f2f24]/15 dark:text-[#f29a7f]">
            This invite could not be loaded.
          </p>
        ) : null}

        <Button
          className="mt-6 w-full bg-[#2c3520] text-[#fff8ea] hover:bg-[#202817] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
          disabled={!match.data || joinMatch.isPending || match.data.seats.length >= 2}
          onClick={() =>
            match.data &&
            joinMatch.mutate(match.data.id, {
              onSuccess: (joinedMatch) => router.push(`/matches/${joinedMatch.id}`),
            })
          }
          type="button"
        >
          <Users className="mr-2 size-4" />
          {joinMatch.isPending ? 'Joining...' : 'Join match'}
        </Button>
      </section>
    </main>
  );
}

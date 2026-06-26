'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  Copy,
  Lock,
  MessageSquare,
  Radio,
  Send,
  Shield,
  Swords,
  Users,
} from 'lucide-react';

import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { useMatchSocket } from '@/features/lobby/api/match-socket.hook';
import { useLeaveMatch, useMatch } from '@/features/lobby/api/lobby.hooks';

export function MatchRoomPageContent({ matchId }: { matchId: string }) {
  const router = useRouter();
  const match = useMatch(matchId);
  const leaveMatch = useLeaveMatch();
  const socket = useMatchSocket(matchId);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const shouldShowInvite = match.data?.visibility === 'PRIVATE' && match.data.seats.length === 1;
  const inviteLink = useMemo(() => {
    if (!match.data) {
      return '';
    }

    const base = typeof window === 'undefined' ? '' : window.location.origin;
    return `${base}/lobby/invite/${match.data.inviteCode}`;
  }, [match.data]);

  useEffect(() => {
    if (shouldShowInvite) {
      setInviteOpen(true);
    }
  }, [shouldShowInvite]);

  function leave() {
    leaveMatch.mutate(matchId, {
      onSuccess: () => router.push('/lobby'),
    });
  }

  async function copyInviteLink() {
    if (!inviteLink) {
      return;
    }

    await navigator.clipboard.writeText(inviteLink);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  }

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#f5f1e6] px-4 py-6 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4] md:px-8">
      <section className="mx-auto flex max-w-7xl flex-col gap-5">
        <div className="flex flex-col gap-3 border-b border-[#8a7b62]/20 pb-5 dark:border-white/10 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <Button asChild variant="ghost" className="mb-3 px-0">
              <Link href="/lobby">
                <ArrowLeft className="mr-2 size-4" />
                Lobby
              </Link>
            </Button>
            <h1 className="text-3xl font-black tracking-normal">
              {match.data?.name ?? 'Loading match...'}
            </h1>
            <p className="mt-2 text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
              Match room only. Board setup and movement are intentionally not active yet.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge variant="outline" className="border-[#6f7b4a]/30 text-[#566033]">
              {match.data?.status ?? 'LOADING'}
            </Badge>
            <Badge variant="outline" className="border-[#8a7b62]/30">
              {socket.connected ? 'Realtime connected' : 'Realtime idle'}
            </Badge>
          </div>
        </div>

        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_22rem]">
          <section className="rounded-lg border border-[#8a7b62]/20 bg-[#2c3520] p-4 text-[#fff8ea] shadow-sm dark:border-[#d7bd73]/20">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-black">
                <Shield className="size-5 text-[#d7bd73]" />
                Board preview
              </h2>
              <Badge className="bg-[#d7bd73] text-[#15130d]">8 x 9</Badge>
            </div>
            <div className="grid aspect-[9/8] grid-cols-9 overflow-hidden rounded-md border border-[#d7bd73]/35 bg-[#53442c]">
              {Array.from({ length: 72 }, (_, index) => {
                const row = Math.floor(index / 9);
                const isSetup = row < 3 || row > 4;

                return (
                  <div
                    key={index}
                    className={[
                      'border border-[#2b2419]/35',
                      isSetup ? 'bg-[#caa765]' : 'bg-[#8f7650]',
                    ].join(' ')}
                  />
                );
              })}
            </div>
          </section>

          <aside className="space-y-5">
            <section className="rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-5 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
              <h2 className="flex items-center gap-2 text-lg font-black">
                <Users className="size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
                Players
              </h2>
              <div className="mt-4 space-y-2">
                {(match.data?.seats ?? []).map((seat) => (
                  <div
                    key={seat.userId}
                    className="flex items-center justify-between rounded-md border border-[#8a7b62]/20 bg-white/50 p-3 text-sm dark:border-white/10 dark:bg-white/5"
                  >
                    <span className="font-black">{seat.side}</span>
                    <span className="text-[#655c51] dark:text-[#c9c1b4]">
                      {seat.ready ? 'Ready' : 'Not ready'}
                    </span>
                  </div>
                ))}
              </div>
              <Button
                className="mt-5 w-full bg-[#8f2f24] text-[#fff8ea] hover:bg-[#76251c]"
                disabled={leaveMatch.isPending || !match.data}
                onClick={leave}
                type="button"
              >
                <Swords className="mr-2 size-4" />
                {leaveMatch.isPending ? 'Leaving...' : 'Leave match'}
              </Button>
            </section>

            <section className="rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-5 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
              <h2 className="flex items-center gap-2 text-lg font-black">
                <MessageSquare className="size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
                Chat
              </h2>
              <div className="mt-4 h-48 rounded-md border border-[#8a7b62]/20 bg-white/50 p-3 text-sm text-[#655c51] dark:border-white/10 dark:bg-white/5 dark:text-[#c9c1b4]">
                Chat UI placeholder. Messaging will be wired after match rooms are stable.
              </div>
              <div className="mt-3 flex gap-2">
                <Input disabled placeholder="Message..." className="bg-white/70 dark:bg-white/5" />
                <Button disabled variant="outline" className="shrink-0">
                  <Send className="size-4" />
                </Button>
              </div>
            </section>

            <section className="rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-4 text-sm shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
              <p className="flex items-center gap-2 font-black">
                <Radio className="size-4 text-[#8f2f24] dark:text-[#f29a7f]" />
                Realtime events
              </p>
              <p className="mt-2 text-[#655c51] dark:text-[#c9c1b4]">
                {socket.events.length} room events received.
              </p>
            </section>
          </aside>
        </div>
      </section>

      <Dialog open={Boolean(shouldShowInvite && inviteOpen)} onOpenChange={setInviteOpen}>
        <DialogContent className="border-[#8a7b62]/25 bg-[#fbf8ef] text-[#201b16] dark:border-white/10 dark:bg-[#181b15] dark:text-[#f6f0e4] sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-2xl font-black tracking-normal">
              <Lock className="size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
              Private invite link
            </DialogTitle>
            <DialogDescription className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
              Send this link to the other player so they can join this private match.
            </DialogDescription>
          </DialogHeader>
          <div className="flex gap-2">
            <Input
              readOnly
              value={inviteLink}
              className="border-[#8a7b62]/25 bg-white/75 text-xs dark:border-white/15 dark:bg-white/5"
            />
            <Button
              type="button"
              variant="outline"
              className="shrink-0 border-[#8a7b62]/35 bg-white/70"
              onClick={copyInviteLink}
            >
              <Copy className="mr-2 size-4" />
              {copied ? 'Copied' : 'Copy'}
            </Button>
          </div>
          <DialogFooter>
            <Button
              type="button"
              className="bg-[#2c3520] text-[#fff8ea] hover:bg-[#202817] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
              onClick={() => setInviteOpen(false)}
            >
              Done
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </main>
  );
}

'use client';

import { History, Plus, Search, Settings, Shield } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { SignOutButton } from '@/components/sign-out-button';
import { ThemeToggle } from '@/components/theme-toggle';
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
import { Label } from '@/components/ui/label';
import {
  useActiveMatch,
  useCancelFindMatch,
  useCreateMatch,
  useFindMatch,
  useGameModel,
  usePlayerLobbySettings,
  usePublicMatches,
} from '@/features/lobby/api/lobby.hooks';
import { FindingMatchDialog, PreparationTimeOptions } from './find-match-dialog';

export function LobbyPageContent() {
  const router = useRouter();
  const activeMatch = useActiveMatch();
  const cancelFindMatch = useCancelFindMatch();
  const createMatch = useCreateMatch();
  const findMatch = useFindMatch();
  const publicMatches = usePublicMatches();
  const gameModel = useGameModel();
  const settings = usePlayerLobbySettings();
  const [createOpen, setCreateOpen] = useState(false);
  const [findOpen, setFindOpen] = useState(false);
  const [isQueued, setIsQueued] = useState(false);
  const [matchFound, setMatchFound] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [preparationSeconds, setPreparationSeconds] = useState('60');
  const [findPreparationSeconds, setFindPreparationSeconds] = useState('60');
  const shouldCancelOnUnmountRef = useRef(false);
  const redirectingRef = useRef(false);
  const cancelQueuedSearchRef = useRef<() => void>(() => undefined);

  const activePlayers = useMemo(
    () => (publicMatches.data ?? []).reduce((total, match) => total + match.seats.length, 0),
    [publicMatches.data],
  );

  const redirectToMatch = useCallback(
    (matchId: string) => {
      redirectingRef.current = true;
      shouldCancelOnUnmountRef.current = false;
      setMatchFound(true);
      setIsQueued(false);
      router.replace(`/matches/${matchId}`);
    },
    [router],
  );

  useEffect(() => {
    shouldCancelOnUnmountRef.current = isQueued;
  }, [isQueued]);

  useEffect(() => {
    cancelQueuedSearchRef.current = () => {
      cancelFindMatch.mutate();
    };
  }, [cancelFindMatch]);

  useEffect(() => {
    if (
      activeMatch.data?.id &&
      activeMatch.fetchStatus === 'idle' &&
      activeMatch.isFetchedAfterMount
    ) {
      redirectToMatch(activeMatch.data.id);
    }
  }, [
    activeMatch.data?.id,
    activeMatch.fetchStatus,
    activeMatch.isFetchedAfterMount,
    redirectToMatch,
  ]);

  useEffect(() => {
    if (!isQueued) {
      return undefined;
    }

    const interval = window.setInterval(() => {
      void activeMatch.refetch();
    }, 1_500);

    return () => window.clearInterval(interval);
  }, [activeMatch, isQueued]);

  useEffect(() => {
    return () => {
      if (shouldCancelOnUnmountRef.current && !redirectingRef.current) {
        cancelQueuedSearchRef.current();
      }
    };
  }, []);

  function create() {
    createMatch.mutate(
      {
        name: 'Command table',
        visibility: 'PRIVATE',
        mode: 'Classic hidden ranks',
        preparationSeconds: Number(preparationSeconds),
      },
      {
        onSuccess: (match) => {
          setCreateOpen(false);
          router.push(`/matches/${match.id}`);
        },
      },
    );
  }

  function startSearch() {
    findMatch.mutate(
      { preparationSeconds: Number(findPreparationSeconds) },
      {
        onSuccess: (response) => {
          if (response.match?.id) {
            redirectToMatch(response.match.id);
            return;
          }

          if (response.status === 'QUEUED') {
            setIsQueued(true);
          }
        },
      },
    );
  }

  function cancelSearch() {
    shouldCancelOnUnmountRef.current = false;
    setFindOpen(false);
    setIsQueued(false);
    setMatchFound(false);
    cancelFindMatch.mutate();
  }

  const isSearching = findMatch.isPending || isQueued;

  return (
    <main className="h-screen overflow-hidden bg-[radial-gradient(circle_at_58%_42%,rgba(238,123,81,0.16),transparent_34%),linear-gradient(90deg,#ffffff_0%,#faf7ef_48%,#f1e7d3_100%)] p-2 text-[#16130d] dark:bg-[radial-gradient(circle_at_58%_42%,rgba(162,95,47,0.36),transparent_34%),linear-gradient(90deg,#070b05_0%,#121407_45%,#201309_100%)] dark:text-[#fffaf0] md:p-3">
      <section className="mx-auto grid h-full min-h-0 max-w-[104rem] gap-3 rounded-lg border border-[#ded7c8] bg-white/72 p-3 shadow-2xl shadow-black/10 backdrop-blur-sm dark:border-[#3b321e] dark:bg-[#11150c]/82 dark:shadow-black/50 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <RulesBoard model={gameModel.data} activePlayers={activePlayers} />

        <aside className="flex min-h-0 flex-col justify-center gap-3 rounded-lg border border-[#d8c8a8] bg-white/82 p-3 shadow-sm dark:border-[#5b5036] dark:bg-[#1b140d]/72">
          <div className="border-b border-[#ded7c8] pb-3 dark:border-[#5b5036]">
            <p className="font-mono text-[10px] font-black uppercase tracking-[0.18em] text-[#ee7b51]">
              Command panel
            </p>
            <h2 className="mt-1 text-xl font-black tracking-normal">Lobby ops</h2>
            <p className="mt-1 text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
              Create a private table, find an opponent, or review your recent campaigns.
            </p>
          </div>
          <ActionButton icon={Plus} label="Create match" onClick={() => setCreateOpen(true)} />
          <ActionButton
            icon={Search}
            label="Find match"
            onClick={() => setFindOpen(true)}
            disabled={isSearching || cancelFindMatch.isPending}
          />
          <ActionLink href="/lobby/history" icon={History} label="History" />
          <ActionButton icon={Settings} label="Settings" onClick={() => setSettingsOpen(true)} />
        </aside>
      </section>

      <CreateMatchDialog
        createError={createMatch.error}
        isCreating={createMatch.isPending}
        onCreate={create}
        onOpenChange={setCreateOpen}
        onPreparationSecondsChange={setPreparationSeconds}
        open={createOpen}
        preparationSeconds={preparationSeconds}
      />

      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} settings={settings.data} />

      <FindingMatchDialog
        cancelPending={cancelFindMatch.isPending}
        isSearching={isSearching}
        matchFound={matchFound}
        onOpenChange={(open) => {
          if (!isSearching && !matchFound) {
            setFindOpen(open);
          }
        }}
        onCancel={cancelSearch}
        onPreparationSecondsChange={setFindPreparationSeconds}
        onStart={startSearch}
        open={findOpen || isSearching}
        preparationSeconds={findPreparationSeconds}
        searchPending={findMatch.isPending}
      />
    </main>
  );
}

function RulesBoard({
  activePlayers,
  model,
}: {
  activePlayers: number;
  model?: { rows: number; columns: number; piecesPerPlayer: number; vacantSetupSquares: number };
}) {
  const rows = model?.rows ?? 8;
  const columns = model?.columns ?? 9;

  return (
    <section className="flex min-h-0 flex-col rounded-lg border border-[#d8c8a8] bg-white/88 p-3 text-[#16130d] shadow-xl shadow-black/10 dark:border-[#5b5036] dark:bg-[#1b140d]/72 dark:text-[#fffaf0]">
      <div className="mb-3 flex flex-col gap-3 border-b border-[#ded7c8] pb-3 dark:border-[#5b5036] sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="font-mono text-[10px] font-black uppercase tracking-[0.18em] text-[#ee7b51]">
            Ready room
          </p>
          <h1 className="mt-1 flex items-center gap-2 text-2xl font-black tracking-normal">
            <Shield className="size-7 text-[#9b5d19] dark:text-[#e8d18b]" />
            Command board
          </h1>
          <p className="mt-2 text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
            Classic hidden-rank board reference for Games of the General.
          </p>
        </div>
        <div className="flex gap-2">
          <Badge className="bg-[#11130f] text-white dark:bg-[#ee7b51]">
            {activePlayers} active players
          </Badge>
          <Badge
            variant="outline"
            className="border-[#11130f]/30 text-[#16130d] dark:border-[#e8d18b]/40 dark:text-[#fffaf0]"
          >
            {rows} x {columns}
          </Badge>
        </div>
      </div>

      <div className="grid flex-1 aspect-[9/8] min-h-0 grid-cols-9 overflow-hidden rounded-md border border-[#c6a46b] bg-[#ead7af] shadow-inner dark:border-black dark:bg-[#6b572d]">
        {Array.from({ length: rows * columns }, (_, index) => {
          const row = Math.floor(index / columns);
          const isSetup = row < 3 || row > 4;

          return (
            <div
              key={index}
              className={[
                'border border-[#a47738]/24 dark:border-black',
                isSetup ? 'bg-[#f0ddaf] dark:bg-[#7b6335]' : 'bg-[#d3b174] dark:bg-[#594a26]',
              ].join(' ')}
            />
          );
        })}
      </div>

      <dl className="mt-3 grid grid-cols-3 gap-2 text-center">
        {[
          [String(model?.piecesPerPlayer ?? 21), 'pieces'],
          [String(model?.vacantSetupSquares ?? 6), 'open setup'],
          ['1', 'flag'],
        ].map(([value, label]) => (
          <div
            key={label}
            className="rounded-md border border-[#11130f]/20 bg-[#11130f] p-2.5 text-white dark:border-[#5b5036] dark:bg-[#11150c]/72 dark:text-[#fffaf0]"
          >
            <dt className="text-lg font-black">{value}</dt>
            <dd className="text-[10px] font-bold uppercase tracking-[0.14em] text-white/70 dark:text-[#c9c0aa]">
              {label}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function ActionButton({
  disabled,
  icon: Icon,
  label,
  onClick,
}: {
  disabled?: boolean;
  icon: typeof Plus;
  label: string;
  onClick: () => void;
}) {
  return (
    <Button
      type="button"
      className="h-16 justify-start rounded-lg border border-[#11130f] bg-[#11130f] px-5 text-left text-lg font-black text-white shadow-sm hover:bg-[#2a2d22] dark:border-[#5b5036] dark:bg-[#0e1209]/80 dark:text-[#fff6df] dark:hover:border-[#e8d18b]/70 dark:hover:bg-[#191d10]"
      disabled={disabled}
      onClick={onClick}
    >
      <Icon className="mr-3 size-5 text-[#ee7b51]" />
      {label}
    </Button>
  );
}

function ActionLink({
  href,
  icon: Icon,
  label,
}: {
  href: string;
  icon: typeof Plus;
  label: string;
}) {
  return (
    <Button
      asChild
      className="h-16 justify-start rounded-lg border border-[#11130f] bg-[#11130f] px-5 text-left text-lg font-black text-white shadow-sm hover:bg-[#2a2d22] dark:border-[#5b5036] dark:bg-[#0e1209]/80 dark:text-[#fff6df] dark:hover:border-[#e8d18b]/70 dark:hover:bg-[#191d10]"
    >
      <Link href={href}>
        <Icon className="mr-3 size-5 text-[#ee7b51]" />
        {label}
      </Link>
    </Button>
  );
}

function CreateMatchDialog({
  createError,
  isCreating,
  onCreate,
  onOpenChange,
  onPreparationSecondsChange,
  open,
  preparationSeconds,
}: {
  createError: Error | null;
  isCreating: boolean;
  onCreate: () => void;
  onOpenChange: (open: boolean) => void;
  onPreparationSecondsChange: (value: string) => void;
  open: boolean;
  preparationSeconds: string;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-sm border-[#d8c8a8] bg-white text-[#16130d] dark:border-[#5b5036] dark:bg-[#11150c] dark:text-[#fffaf0]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black tracking-normal">Create match</DialogTitle>
          <DialogDescription className="text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
            Create a private invite table and choose its preparation time.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <div className="space-y-2">
            <Label>Preparation time</Label>
            <PreparationTimeOptions
              value={preparationSeconds}
              onValueChange={onPreparationSecondsChange}
            />
          </div>

          {createError ? (
            <p className="rounded-md border border-[#8f2f24]/25 bg-[#fff3df] px-3 py-2 text-sm text-[#8f2f24] dark:border-[#f29a7f]/30 dark:bg-[#8f2f24]/15 dark:text-[#f29a7f]">
              Could not create match. Check the backend session and try again.
            </p>
          ) : null}
        </div>

        <DialogFooter>
          <Button
            type="button"
            className="w-full bg-[#11130f] text-white hover:bg-[#2a2d22] dark:bg-[#ee7b51] dark:hover:bg-[#ff8b5e]"
            disabled={isCreating}
            onClick={onCreate}
          >
            <Plus className="mr-2 size-4" />
            {isCreating ? 'Creating...' : 'Create match'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

function SettingsDialog({
  onOpenChange,
  open,
  settings,
}: {
  onOpenChange: (open: boolean) => void;
  open: boolean;
  settings?: { challengeReveal: string; invitePrivacy: string; reconnectSeconds: number };
}) {
  const rows = settings
    ? [
        ['Challenge reveal', settingLabel(settings.challengeReveal)],
        ['Invite privacy', settingLabel(settings.invitePrivacy)],
        ['Reconnect window', `${settings.reconnectSeconds} seconds`],
      ]
    : [
        ['Challenge reveal', 'Loading settings...'],
        ['Invite privacy', 'Loading settings...'],
        ['Reconnect window', 'Loading settings...'],
      ];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-[#d8c8a8] bg-white text-[#16130d] dark:border-[#5b5036] dark:bg-[#11150c] dark:text-[#fffaf0]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black tracking-normal">Settings</DialogTitle>
          <DialogDescription className="text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
            Current lobby defaults for match visibility and reconnect behavior.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          <div className="rounded-lg border border-[#d8c8a8] bg-[#f7f1e4]/70 p-4 dark:border-[#5b5036] dark:bg-[#1b140d]/60">
            <div className="flex items-center justify-between gap-4">
              <div>
                <p className="text-sm font-black">Appearance</p>
                <p className="mt-1 text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
                  Switch between light, dark, and system theme.
                </p>
              </div>
              <ThemeToggle />
            </div>
          </div>
          {rows.map(([title, body]) => (
            <div
              key={title}
              className="rounded-lg border border-[#d8c8a8] bg-[#f7f1e4]/70 p-4 dark:border-[#5b5036] dark:bg-[#1b140d]/60"
            >
              <p className="text-sm font-black">{title}</p>
              <p className="mt-1 text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">{body}</p>
            </div>
          ))}
          <div className="rounded-lg border border-[#d8c8a8] bg-[#f7f1e4]/70 p-4 dark:border-[#5b5036] dark:bg-[#1b140d]/60">
            <p className="text-sm font-black">Account</p>
            <p className="mt-1 text-sm leading-6 text-[#6c6559] dark:text-[#c9c0aa]">
              Leave this session and return to sign in.
            </p>
            <div className="mt-3">
              <SignOutButton />
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}

function settingLabel(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part[0].toUpperCase() + part.slice(1))
    .join(' ');
}

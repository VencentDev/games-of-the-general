'use client';

import { Eye, History, Lock, Plus, Search, Settings, Shield } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';

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
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  type MatchVisibility,
  useCreateMatch,
  useGameModel,
  usePlayerLobbySettings,
  usePublicMatches,
} from '@/features/lobby/api/lobby.hooks';

const PREP_TIME_OPTIONS = [
  { label: 'No time', value: '0' },
  { label: '30 seconds', value: '30' },
  { label: '1 minute', value: '60' },
  { label: '1 minute 30 seconds', value: '90' },
];

export function LobbyPageContent() {
  const router = useRouter();
  const createMatch = useCreateMatch();
  const publicMatches = usePublicMatches();
  const gameModel = useGameModel();
  const settings = usePlayerLobbySettings();
  const [createOpen, setCreateOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const [visibility, setVisibility] = useState<MatchVisibility>('PUBLIC');
  const [preparationSeconds, setPreparationSeconds] = useState('60');

  const activePlayers = useMemo(
    () => (publicMatches.data ?? []).reduce((total, match) => total + match.seats.length, 0),
    [publicMatches.data],
  );

  function create() {
    createMatch.mutate(
      {
        name: 'Command table',
        visibility,
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

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#f5f1e6] px-4 py-6 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4] md:px-8">
      <section className="mx-auto grid min-h-[calc(100svh-7rem)] max-w-7xl gap-5 lg:grid-cols-[minmax(0,1fr)_22rem]">
        <RulesBoard model={gameModel.data} activePlayers={activePlayers} />

        <aside className="flex flex-col justify-center gap-3">
          <ActionButton icon={Plus} label="Create match" onClick={() => setCreateOpen(true)} />
          <ActionLink href="/lobby/find" icon={Search} label="Find match" />
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
        onVisibilityChange={setVisibility}
        open={createOpen}
        preparationSeconds={preparationSeconds}
        visibility={visibility}
      />

      <SettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} settings={settings.data} />
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
    <section className="flex min-h-[36rem] flex-col rounded-lg border border-[#8a7b62]/20 bg-[#2c3520] p-5 text-[#fff8ea] shadow-sm dark:border-[#d7bd73]/20">
      <div className="mb-5 flex flex-col gap-3 border-b border-[#d7bd73]/20 pb-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="flex items-center gap-2 text-2xl font-black tracking-normal">
            <Shield className="size-6 text-[#d7bd73]" />
            Rules baseline
          </h1>
          <p className="mt-2 text-sm leading-6 text-[#d8d0bd]">
            Classic hidden-rank board reference for Games of the General.
          </p>
        </div>
        <div className="flex gap-2">
          <Badge className="bg-[#d7bd73] text-[#15130d]">{activePlayers} active players</Badge>
          <Badge variant="outline" className="border-[#d7bd73]/35 text-[#fff8ea]">
            {rows} x {columns}
          </Badge>
        </div>
      </div>

      <div className="grid flex-1 aspect-[9/8] min-h-0 grid-cols-9 overflow-hidden rounded-md border border-[#d7bd73]/35 bg-[#53442c]">
        {Array.from({ length: rows * columns }, (_, index) => {
          const row = Math.floor(index / columns);
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

      <dl className="mt-5 grid grid-cols-3 gap-2 text-center">
        {[
          [String(model?.piecesPerPlayer ?? 21), 'pieces'],
          [String(model?.vacantSetupSquares ?? 6), 'open setup'],
          ['1', 'flag'],
        ].map(([value, label]) => (
          <div key={label} className="rounded-md bg-white/10 p-3">
            <dt className="text-xl font-black">{value}</dt>
            <dd className="text-[10px] font-bold uppercase tracking-[0.14em] text-[#d8d0bd]">
              {label}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  );
}

function ActionButton({
  icon: Icon,
  label,
  onClick,
}: {
  icon: typeof Plus;
  label: string;
  onClick: () => void;
}) {
  return (
    <Button
      type="button"
      className="h-16 justify-start rounded-lg border border-[#8a7b62]/25 bg-[#fbf8ef] px-5 text-left text-lg font-black text-[#201b16] shadow-sm hover:bg-white/80 dark:border-white/10 dark:bg-white/[0.04] dark:text-[#f6f0e4] dark:hover:bg-white/10"
      onClick={onClick}
    >
      <Icon className="mr-3 size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
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
      className="h-16 justify-start rounded-lg border border-[#8a7b62]/25 bg-[#fbf8ef] px-5 text-left text-lg font-black text-[#201b16] shadow-sm hover:bg-white/80 dark:border-white/10 dark:bg-white/[0.04] dark:text-[#f6f0e4] dark:hover:bg-white/10"
    >
      <Link href={href}>
        <Icon className="mr-3 size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
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
  onVisibilityChange,
  open,
  preparationSeconds,
  visibility,
}: {
  createError: Error | null;
  isCreating: boolean;
  onCreate: () => void;
  onOpenChange: (open: boolean) => void;
  onPreparationSecondsChange: (value: string) => void;
  onVisibilityChange: (value: MatchVisibility) => void;
  open: boolean;
  preparationSeconds: string;
  visibility: MatchVisibility;
}) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="border-[#8a7b62]/25 bg-[#fbf8ef] text-[#201b16] dark:border-white/10 dark:bg-[#181b15] dark:text-[#f6f0e4] sm:max-w-xl">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black tracking-normal">Create match</DialogTitle>
          <DialogDescription className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
            Choose visibility and preparation time before creating the table.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-5">
          <div className="grid gap-3 sm:grid-cols-2">
            <VisibilityOption
              active={visibility === 'PUBLIC'}
              description="Visible in Find Match"
              icon={Eye}
              label="Public"
              onSelect={() => onVisibilityChange('PUBLIC')}
            />
            <VisibilityOption
              active={visibility === 'PRIVATE'}
              description="Invite link only"
              icon={Lock}
              label="Private"
              onSelect={() => onVisibilityChange('PRIVATE')}
            />
          </div>

          <div className="space-y-2">
            <Label>Preparation time</Label>
            <Select value={preparationSeconds} onValueChange={onPreparationSecondsChange}>
              <SelectTrigger className="border-[#8a7b62]/30 bg-white/70 dark:border-white/15 dark:bg-white/5">
                <SelectValue placeholder="Select preparation time" />
              </SelectTrigger>
              <SelectContent>
                {PREP_TIME_OPTIONS.map((option) => (
                  <SelectItem key={option.value} value={option.value}>
                    {option.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {createError ? (
            <p className="rounded-md border border-[#8f2f24]/25 bg-[#fff3df] px-3 py-2 text-sm text-[#8f2f24] dark:border-[#f29a7f]/30 dark:bg-[#8f2f24]/15 dark:text-[#f29a7f]">
              Could not create match. Check the backend session and try again.
            </p>
          ) : null}
        </div>

        <DialogFooter className="gap-2 sm:justify-between">
          <Button
            type="button"
            className="bg-[#2c3520] text-[#fff8ea] hover:bg-[#202817] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
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

function VisibilityOption({
  active,
  description,
  icon: Icon,
  label,
  onSelect,
}: {
  active: boolean;
  description: string;
  icon: typeof Eye;
  label: string;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={[
        'cursor-pointer rounded-lg border p-4 text-left transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#8f2f24]',
        active
          ? 'border-[#8f2f24] bg-[#fff3df] dark:border-[#f29a7f] dark:bg-[#8f2f24]/15'
          : 'border-[#8a7b62]/25 bg-white/55 hover:bg-white/80 dark:border-white/10 dark:bg-white/5 dark:hover:bg-white/10',
      ].join(' ')}
    >
      <span className="flex items-center gap-2 text-sm font-black">
        <Icon className="size-4 text-[#8f2f24] dark:text-[#f29a7f]" />
        {label}
      </span>
      <span className="mt-2 block text-xs leading-5 text-[#655c51] dark:text-[#c9c1b4]">
        {description}
      </span>
    </button>
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
      <DialogContent className="border-[#8a7b62]/25 bg-[#fbf8ef] text-[#201b16] dark:border-white/10 dark:bg-[#181b15] dark:text-[#f6f0e4]">
        <DialogHeader>
          <DialogTitle className="text-2xl font-black tracking-normal">Settings</DialogTitle>
          <DialogDescription className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
            Current lobby defaults for match visibility and reconnect behavior.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-3">
          {rows.map(([title, body]) => (
            <div
              key={title}
              className="rounded-lg border border-[#8a7b62]/20 bg-white/50 p-4 dark:border-white/10 dark:bg-white/5"
            >
              <p className="text-sm font-black">{title}</p>
              <p className="mt-1 text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">{body}</p>
            </div>
          ))}
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

'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  Check,
  Copy,
  DoorOpen,
  Loader2,
  Lock,
  RotateCcw,
  Send,
  Settings,
  Smile,
  Volume2,
} from 'lucide-react';

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
import { useToast } from '@/components/ui/use-toast';
import { useMatchSocket } from '@/features/lobby/api/match-socket.hook';
import {
  type BoardSquare,
  type GameState,
  type MatchSeat,
  type MoveHistory,
  type PieceDefinition,
  type PieceInstance,
  type PieceType,
  type PlayerSide,
  useGameModel,
  useGameState,
  useLeaveMatch,
  useLegalMoves,
  useMarkReady,
  useMatch,
  useMoveHistory,
  useMovePiece,
  useUpdateSetup,
} from '@/features/lobby/api/lobby.hooks';
import { cn } from '@/lib/utils';

const BOARD_ROWS = 8;
const BOARD_COLUMNS = 9;

type EmptySquare = BoardSquare & {
  piece: null;
};

export function MatchRoomPageContent({ matchId }: { matchId: string }) {
  const router = useRouter();
  const { toast } = useToast();
  const match = useMatch(matchId);
  const gameModel = useGameModel();
  const gameState = useGameState(matchId);
  const moveHistory = useMoveHistory(matchId);
  const leaveMatch = useLeaveMatch();
  const updateSetup = useUpdateSetup(matchId);
  const markReady = useMarkReady(matchId);
  const movePiece = useMovePiece(matchId);
  const socket = useMatchSocket(matchId);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [selectedPieceId, setSelectedPieceId] = useState<string | null>(null);

  const state = gameState.data;
  const viewerSide = state?.viewerSide;
  const selectedPiece = state?.ownPieces.find((piece) => piece.id === selectedPieceId) ?? null;
  const legalMoves = useLegalMoves(
    matchId,
    selectedPieceId,
    state?.phase === 'PLAYING' && selectedPiece?.status === 'ACTIVE',
  );

  const pieceDefinitions = useMemo(() => {
    return new Map((gameModel.data?.pieces ?? []).map((piece) => [piece.type, piece]));
  }, [gameModel.data?.pieces]);

  const board = useMemo(() => {
    const boardByPosition = new Map(
      (state?.board ?? []).map((square) => [
        positionKey(square.position.row, square.position.column),
        square,
      ]),
    );

    return Array.from({ length: BOARD_ROWS * BOARD_COLUMNS }, (_, index) => {
      const row = Math.floor(index / BOARD_COLUMNS);
      const column = index % BOARD_COLUMNS;
      return (
        boardByPosition.get(positionKey(row, column)) ?? {
          position: { row, column },
          piece: null,
        }
      );
    }) as Array<BoardSquare | EmptySquare>;
  }, [state?.board]);

  const displayedBoard = useMemo(() => {
    const boardByPosition = new Map(
      board.map((square) => [positionKey(square.position.row, square.position.column), square]),
    );

    return Array.from({ length: BOARD_ROWS * BOARD_COLUMNS }, (_, index) => {
      const displayRow = Math.floor(index / BOARD_COLUMNS);
      const displayColumn = index % BOARD_COLUMNS;
      const actualPosition = toActualPosition(displayRow, displayColumn, viewerSide);

      return (
        boardByPosition.get(positionKey(actualPosition.row, actualPosition.column)) ?? {
          position: actualPosition,
          piece: null,
        }
      );
    }) as Array<BoardSquare | EmptySquare>;
  }, [board, viewerSide]);

  const legalTargets = useMemo(() => {
    return new Map(
      (legalMoves.data ?? []).map((move) => [
        positionKey(move.position.row, move.position.column),
        move,
      ]),
    );
  }, [legalMoves.data]);

  const seats = useMemo(() => {
    const currentSeats = match.data?.seats ?? [];
    if (!viewerSide) {
      return currentSeats;
    }

    return currentSeats.slice().sort((first) => (first.side === viewerSide ? 1 : -1));
  }, [match.data?.seats, viewerSide]);

  const activePlacedCount =
    state?.ownPieces.filter((piece) => piece.status === 'ACTIVE').length ?? 0;
  const ownReady = Boolean(match.data?.seats.find((seat) => seat.side === viewerSide)?.ready);
  const canMarkReady = state?.phase === 'SETUP' && activePlacedCount === 21 && !ownReady;
  const canEditSetup = state?.phase === 'SETUP' && !ownReady;
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

  useEffect(() => {
    if (!selectedPieceId || state?.ownPieces.some((piece) => piece.id === selectedPieceId)) {
      return;
    }

    setSelectedPieceId(null);
  }, [selectedPieceId, state?.ownPieces]);

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

  function handleSquareClick(square: BoardSquare) {
    if (!state || !viewerSide) {
      return;
    }

    const visibleOwnPiece =
      square.piece?.visible && square.piece.side === viewerSide ? square.piece : null;

    if (visibleOwnPiece) {
      setSelectedPieceId(visibleOwnPiece.id);
      return;
    }

    if (!selectedPieceId) {
      return;
    }

    if (state.phase === 'SETUP') {
      placeSelectedPiece(state, square.position.row, square.position.column);
      return;
    }

    if (state.phase === 'PLAYING') {
      moveSelectedPiece(square.position.row, square.position.column);
    }
  }

  function placeSelectedPiece(currentState: GameState, row: number, column: number) {
    if (!viewerSide || !selectedPieceId || !canEditSetup) {
      return;
    }

    if (!isSetupRow(row, viewerSide)) {
      toast({
        title: 'Invalid setup square',
        description: `${viewerSide} can only place pieces in its three setup rows.`,
      });
      return;
    }

    const occupiedByOwnPiece = currentState.ownPieces.find(
      (piece) => piece.status === 'ACTIVE' && piece.row === row && piece.column === column,
    );

    if (occupiedByOwnPiece) {
      setSelectedPieceId(occupiedByOwnPiece.id);
      return;
    }

    updateSetup.mutate([{ pieceId: selectedPieceId, row, column }], {
      onError: (error) => showMutationError(error, toast),
    });
  }

  function moveSelectedPiece(row: number, column: number) {
    if (!selectedPieceId) {
      return;
    }

    const legalTarget = legalTargets.get(positionKey(row, column));
    if (!legalTarget) {
      return;
    }

    movePiece.mutate(
      { pieceId: selectedPieceId, toRow: row, toColumn: column },
      {
        onSuccess: () => setSelectedPieceId(null),
        onError: (error) => showMutationError(error, toast),
      },
    );
  }

  function unplaceSelectedPiece() {
    if (!selectedPieceId || !canEditSetup) {
      return;
    }

    updateSetup.mutate([{ pieceId: selectedPieceId, row: null, column: null }], {
      onError: (error) => showMutationError(error, toast),
    });
  }

  function autoFillSetup() {
    if (!state || !viewerSide || !canEditSetup) {
      return;
    }

    const setupRows = viewerSide === 'RED' ? [0, 1, 2] : [5, 6, 7];
    const setupSquares = setupRows.flatMap((row) =>
      Array.from({ length: BOARD_COLUMNS }, (_, column) => ({ row, column })),
    );
    const pieces = state.ownPieces
      .slice()
      .sort((first, second) => comparePieces(first, second, pieceDefinitions))
      .map((piece, index) => ({
        pieceId: piece.id,
        row: setupSquares[index]?.row ?? null,
        column: setupSquares[index]?.column ?? null,
      }));

    updateSetup.mutate(pieces, {
      onError: (error) => showMutationError(error, toast),
    });
  }

  function readySetup() {
    markReady.mutate(undefined, {
      onError: (error) => showMutationError(error, toast),
    });
  }

  const busy = updateSetup.isPending || markReady.isPending || movePiece.isPending;
  const lastMoves = (moveHistory.data ?? []).slice(-8).reverse();
  const bottomSeat = viewerSide ? seats.find((seat) => seat.side === viewerSide) : seats[1];
  const topSeat = viewerSide ? seats.find((seat) => seat.side !== viewerSide) : seats[0];

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#111315] p-3 text-[#ece8df] md:p-4">
      <section className="mx-auto grid min-h-[calc(100svh-5.5rem)] max-w-[104rem] grid-rows-[1fr_auto] rounded-lg border border-white/15 bg-[radial-gradient(circle_at_center,#202326_0%,#111416_55%,#0b0d0e_100%)] shadow-2xl">
        <div className="grid gap-3 p-4 xl:grid-cols-[minmax(0,1fr)_18rem] 2xl:grid-cols-[minmax(0,1fr)_20rem]">
          <section className="flex min-w-0 items-center justify-center">
            <div className="w-full max-w-[64rem] rounded-lg border border-white/10 bg-white/[0.06] p-3 shadow-xl">
              {topSeat ? (
                <PlayerBar
                  seat={topSeat}
                  active={state?.currentTurn === topSeat.side}
                  viewerSide={viewerSide}
                  capturedPieces={
                    state?.capturedPieces.filter((piece) => piece.side === topSeat.side) ?? []
                  }
                  definitions={pieceDefinitions}
                />
              ) : (
                <div className="mb-3 rounded-md border border-white/10 bg-white/[0.04] px-3 py-2 text-sm text-white/55">
                  Loading opponent...
                </div>
              )}

              <div className="grid grid-cols-[0.875rem_1fr] gap-2">
                <div
                  className={cn(
                    'rounded-full border border-white/10 transition-colors',
                    turnRailTone(state?.currentTurn, viewerSide),
                  )}
                  title={turnRailLabel(state?.currentTurn, viewerSide, state?.phase)}
                />
                <div className="grid aspect-[9/8] grid-cols-9 overflow-hidden rounded-md border border-[#6b4d2f] bg-[#e5be84] shadow-inner">
                  {displayedBoard.map((square) => {
                    const key = positionKey(square.position.row, square.position.column);
                    const target = legalTargets.get(key);
                    const isOwnSetupSquare = Boolean(
                      viewerSide && isSetupRow(square.position.row, viewerSide),
                    );
                    const isSelected =
                      selectedPiece?.status === 'ACTIVE' &&
                      selectedPiece.row === square.position.row &&
                      selectedPiece.column === square.position.column;

                    return (
                      <button
                        key={key}
                        type="button"
                        className={cn(
                          'relative flex min-h-0 min-w-0 items-center justify-center border border-[#8d6842]/70 bg-[#e7c795] outline-none transition focus-visible:z-10 focus-visible:ring-2 focus-visible:ring-[#f6e09f]',
                          isOwnSetupSquare && 'bg-[#edcf9c]',
                          square.piece && 'hover:bg-[#f0d7ad]',
                          !square.piece && 'hover:bg-[#dfb979]',
                          target && !target.attack && 'bg-[#8b985d] hover:bg-[#98a86a]',
                          target?.attack && 'bg-[#a44536] hover:bg-[#b94f3f]',
                          isSelected && 'z-10 ring-4 ring-[#f6e09f]',
                        )}
                        disabled={busy || gameState.isLoading}
                        onClick={() => handleSquareClick(square)}
                      >
                        <span
                          className={cn(
                            'flex size-[72%] items-center justify-center rounded-full border-2 text-[clamp(0.6rem,1.35vw,1.05rem)] font-black shadow-md',
                            pieceTone(square.piece?.side),
                            !square.piece && 'border-transparent bg-transparent shadow-none',
                            square.piece &&
                              !square.piece.visible &&
                              'border-[#111315] bg-[#202326] text-[#f7ecd8]',
                          )}
                        >
                          {squareLabel(square, pieceDefinitions)}
                        </span>
                      </button>
                    );
                  })}
                </div>
              </div>

              {bottomSeat ? (
                <div className="mt-3">
                  <PlayerBar
                    seat={bottomSeat}
                    active={state?.currentTurn === bottomSeat.side}
                    viewerSide={viewerSide}
                    capturedPieces={
                      state?.capturedPieces.filter((piece) => piece.side === bottomSeat.side) ?? []
                    }
                    definitions={pieceDefinitions}
                  />
                </div>
              ) : null}

              {state?.phase === 'SETUP' ? (
                <div className="mt-3 rounded-md border border-white/10 bg-black/20 p-3">
                  <div className="mb-3 flex items-center justify-between gap-3">
                    <span className="text-xs font-semibold text-white/60">
                      Setup {activePlacedCount}/21
                    </span>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="h-8 border-white/15 bg-white/5 text-white hover:bg-white/10"
                        disabled={!canEditSetup || updateSetup.isPending}
                        onClick={autoFillSetup}
                      >
                        <RotateCcw className="mr-2 size-3.5" />
                        Auto fill
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="h-8 border-white/15 bg-white/5 text-white hover:bg-white/10"
                        disabled={!selectedPiece || !canEditSetup || updateSetup.isPending}
                        onClick={unplaceSelectedPiece}
                      >
                        Remove
                      </Button>
                      <Button
                        type="button"
                        size="sm"
                        className="h-8 bg-[#d6a348] text-[#121212] hover:bg-[#e2b45b]"
                        disabled={!canMarkReady || markReady.isPending}
                        onClick={readySetup}
                      >
                        {markReady.isPending ? (
                          <Loader2 className="mr-2 size-3.5 animate-spin" />
                        ) : (
                          <Check className="mr-2 size-3.5" />
                        )}
                        {ownReady ? 'Ready' : 'Ready'}
                      </Button>
                    </div>
                  </div>
                  <div className="grid grid-cols-7 gap-2 sm:grid-cols-11">
                    {(state?.ownPieces ?? []).map((piece) => (
                      <SetupPieceButton
                        key={piece.id}
                        piece={piece}
                        definition={pieceDefinitions.get(piece.type)}
                        selected={selectedPieceId === piece.id}
                        onSelect={() => setSelectedPieceId(piece.id)}
                      />
                    ))}
                  </div>
                </div>
              ) : null}
            </div>
          </section>

          <aside className="flex min-h-[38rem] min-w-0 flex-col gap-3">
            <section className="min-h-0 rounded-lg border border-white/15 bg-white/[0.04]">
              <div className="flex items-center justify-between border-b border-white/10 px-4 py-3">
                <h2 className="font-black">Move History</h2>
                {busy ? <Loader2 className="size-4 animate-spin text-white/60" /> : null}
              </div>
              <div className="max-h-[18rem] overflow-auto">
                {lastMoves.length > 0 ? (
                  lastMoves.map((move) => (
                    <div
                      key={`${move.moveNumber}-${move.pieceId}`}
                      className="grid grid-cols-[2.5rem_1fr] gap-3 px-4 py-3 text-sm odd:bg-white/[0.03]"
                    >
                      <span className="font-semibold text-white/70">{move.moveNumber}</span>
                      <span
                        className={cn(
                          'truncate',
                          move.actingSide === viewerSide
                            ? move.actingSide === 'RED'
                              ? 'text-[#f36b5c]'
                              : 'text-[#9bc8ff]'
                            : 'text-white/55',
                        )}
                      >
                        {moveHistoryLabel(move, viewerSide, pieceDefinitions)}
                      </span>
                    </div>
                  ))
                ) : (
                  <p className="px-4 py-5 text-sm text-white/55">No moves yet.</p>
                )}
              </div>
            </section>

            <section className="flex min-h-0 flex-1 flex-col rounded-lg border border-white/15 bg-white/[0.04]">
              <div className="border-b border-white/10 px-4 py-3">
                <h2 className="font-black">Chat</h2>
              </div>
              <div className="flex-1 space-y-4 overflow-auto p-4 text-sm">
                <ChatBubble
                  name="System"
                  time="now"
                  message={chatStatusText(state, socket.connected)}
                />
              </div>
              <div className="flex gap-2 border-t border-white/10 p-3">
                <div className="relative flex-1">
                  <Input
                    disabled
                    placeholder="Type a message..."
                    className="border-white/15 bg-black/20 pr-9 text-white placeholder:text-white/35"
                  />
                  <Smile className="absolute right-3 top-1/2 size-4 -translate-y-1/2 text-white/45" />
                </div>
                <Button disabled variant="ghost" className="shrink-0 text-white/45">
                  <Send className="size-5" />
                </Button>
              </div>
            </section>
          </aside>
        </div>

        <footer className="flex items-center justify-between border-t border-white/10 px-4 py-3">
          <div className="flex gap-2">
            <Button
              asChild
              variant="outline"
              className="border-white/15 bg-white/[0.03] text-white hover:bg-white/10"
            >
              <Link href="/lobby">
                <ArrowLeft className="mr-2 size-4" />
                Lobby
              </Link>
            </Button>
            <Button
              className="border border-white/15 bg-white/[0.03] text-white hover:bg-white/10"
              disabled={leaveMatch.isPending || !match.data}
              onClick={leave}
              type="button"
            >
              <DoorOpen className="mr-2 size-4" />
              {leaveMatch.isPending ? 'Leaving...' : 'Leave Match'}
            </Button>
          </div>
          <div className="flex overflow-hidden rounded-md border border-white/10">
            <Button variant="ghost" className="rounded-none text-white/60 hover:bg-white/10">
              <Volume2 className="size-5" />
            </Button>
            <Button
              variant="ghost"
              className="rounded-none border-l border-white/10 text-white/60 hover:bg-white/10"
            >
              <Settings className="size-5" />
            </Button>
          </div>
        </footer>
      </section>

      <Dialog open={Boolean(shouldShowInvite && inviteOpen)} onOpenChange={setInviteOpen}>
        <DialogContent className="border-white/15 bg-[#181b15] text-[#f6f0e4] sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-2xl font-black tracking-normal">
              <Lock className="size-5 text-[#d6a348]" />
              Private invite link
            </DialogTitle>
            <DialogDescription className="text-sm leading-6 text-white/65">
              Send this link to the other player so they can join this private match.
            </DialogDescription>
          </DialogHeader>
          <div className="flex gap-2">
            <Input readOnly value={inviteLink} className="border-white/15 bg-white/5 text-xs" />
            <Button
              type="button"
              variant="outline"
              className="shrink-0 border-white/15 bg-white/5"
              onClick={copyInviteLink}
            >
              <Copy className="mr-2 size-4" />
              {copied ? 'Copied' : 'Copy'}
            </Button>
          </div>
          <DialogFooter>
            <Button
              type="button"
              className="bg-[#d6a348] text-[#121212] hover:bg-[#e2b45b]"
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

function PlayerBar({
  seat,
  active,
  viewerSide,
  capturedPieces,
  definitions,
}: {
  seat: MatchSeat;
  active: boolean;
  viewerSide: PlayerSide | undefined;
  capturedPieces: GameState['capturedPieces'];
  definitions: Map<PieceType, PieceDefinition>;
}) {
  const displayName = seat.side === viewerSide ? 'You' : 'Opponent';
  const isViewer = seat.side === viewerSide;
  const summary = summarizeCaptured(capturedPieces, definitions).slice(0, 4);

  return (
    <section className="mb-3 flex min-w-0 items-center gap-3 rounded-md border border-white/10 bg-black/20 px-3 py-2">
      <div className="flex min-w-0 shrink-0 items-center gap-2">
        <div
          className={cn(
            'relative flex size-9 items-center justify-center rounded-md border border-white/10 text-sm font-black shadow-lg',
            seat.side === 'RED' ? 'bg-[#6f241d] text-[#ffd7c8]' : 'bg-[#1e3e65] text-[#d9ecff]',
          )}
        >
          {seat.side[0]}
          <span
            className={cn(
              'absolute -bottom-0.5 -right-0.5 size-2.5 rounded-full border border-[#111315]',
              active ? 'bg-[#79d15f]' : 'bg-white/35',
            )}
          />
        </div>
        <div className="min-w-0">
          <p className="truncate text-sm font-black">{displayName}</p>
          <p className="text-xs text-white/55">
            {seat.side} {seat.ready ? 'ready' : 'not ready'}
          </p>
        </div>
      </div>

      <div className="flex min-w-0 flex-1 items-center gap-2 overflow-hidden">
        {summary.length > 0 ? (
          summary.map((item) => (
            <div key={item.type} className="flex shrink-0 items-center gap-1">
              <div
                className={cn(
                  'flex size-7 items-center justify-center rounded-full border text-[0.65rem] font-black',
                  isViewer
                    ? seat.side === 'RED'
                      ? 'border-[#d46b5d] bg-[#7e2c23] text-[#ffd7c8]'
                      : 'border-[#6ea1d6] bg-[#244b78] text-[#d9ecff]'
                    : 'border-white/15 bg-[#050607] text-transparent',
                )}
                aria-label={isViewer ? pieceAbbreviation(item.type, definitions) : 'Hidden piece'}
              >
                {isViewer ? pieceAbbreviation(item.type, definitions) : ''}
              </div>
              <p className="text-xs text-white/70">{item.count}</p>
            </div>
          ))
        ) : (
          <p className="text-xs text-white/40">0 lost</p>
        )}
      </div>
    </section>
  );
}

function SetupPieceButton({
  piece,
  definition,
  selected,
  onSelect,
}: {
  piece: PieceInstance;
  definition: PieceDefinition | undefined;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      className={cn(
        'flex aspect-square min-w-0 items-center justify-center rounded-full border text-xs font-black transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#f6e09f]',
        piece.status === 'ACTIVE'
          ? 'border-white/10 bg-white/10 text-white/50'
          : 'border-[#d6a348] bg-[#27211a] text-[#f6d38b] hover:bg-[#352917]',
        selected && 'ring-2 ring-[#f6e09f]',
        piece.status === 'CAPTURED' && 'opacity-35',
      )}
      disabled={piece.status === 'CAPTURED'}
      title={definition?.label ?? piece.type}
      onClick={onSelect}
    >
      {definition?.abbreviation ?? pieceAbbreviation(piece.type, new Map())}
    </button>
  );
}

function ChatBubble({ name, time, message }: { name: string; time: string; message: string }) {
  return (
    <div className="flex gap-3">
      <div className="flex size-10 shrink-0 items-center justify-center rounded-md bg-[#d6a348] text-sm font-black text-[#121212]">
        {name[0]}
      </div>
      <div className="min-w-0">
        <p className="font-black">
          {name} <span className="ml-2 text-xs font-medium text-white/45">{time}</span>
        </p>
        <p className="mt-1 text-white/75">{message}</p>
      </div>
    </div>
  );
}

function positionKey(row: number, column: number) {
  return `${row}:${column}`;
}

function toActualPosition(
  displayRow: number,
  displayColumn: number,
  viewerSide: PlayerSide | undefined,
) {
  if (viewerSide === 'RED') {
    return {
      row: BOARD_ROWS - 1 - displayRow,
      column: BOARD_COLUMNS - 1 - displayColumn,
    };
  }

  return {
    row: displayRow,
    column: displayColumn,
  };
}

function isSetupRow(row: number, side: PlayerSide) {
  return side === 'RED' ? row >= 0 && row <= 2 : row >= 5 && row <= 7;
}

function pieceTone(side: PlayerSide | undefined) {
  if (side === 'RED') {
    return 'border-[#7b241b] bg-[#a83228] text-[#ffd7c8]';
  }

  if (side === 'BLUE') {
    return 'border-[#152e4d] bg-[#203f66] text-[#d9ecff]';
  }

  return '';
}

function squareLabel(square: BoardSquare, definitions: Map<PieceType, PieceDefinition>) {
  if (!square.piece) {
    return '';
  }

  if (!square.piece.visible) {
    return '?';
  }

  if (square.piece.abbreviation) {
    return square.piece.abbreviation;
  }

  if (square.piece.type) {
    return pieceAbbreviation(square.piece.type, definitions);
  }

  return '?';
}

function pieceAbbreviation(type: PieceType, definitions: Map<PieceType, PieceDefinition>) {
  return (
    definitions.get(type)?.abbreviation ??
    type
      .split('_')
      .map((part) => part[0])
      .join('')
  );
}

function comparePieces(
  first: PieceInstance,
  second: PieceInstance,
  definitions: Map<PieceType, PieceDefinition>,
) {
  const firstRank = definitions.get(first.type)?.rank ?? 0;
  const secondRank = definitions.get(second.type)?.rank ?? 0;

  if (firstRank !== secondRank) {
    return secondRank - firstRank;
  }

  return first.id.localeCompare(second.id);
}

function summarizeCaptured(
  capturedPieces: GameState['capturedPieces'],
  definitions: Map<PieceType, PieceDefinition>,
) {
  const counts = new Map<PieceType, number>();
  capturedPieces.forEach((piece) => counts.set(piece.type, (counts.get(piece.type) ?? 0) + 1));

  return Array.from(counts.entries())
    .map(([type, count]) => ({ type, count }))
    .sort((first, second) => {
      const firstRank = definitions.get(first.type)?.rank ?? 0;
      const secondRank = definitions.get(second.type)?.rank ?? 0;
      return secondRank - firstRank;
    });
}

function moveHistoryLabel(
  move: MoveHistory,
  viewerSide: PlayerSide | undefined,
  definitions: Map<PieceType, PieceDefinition>,
) {
  if (move.actingSide !== viewerSide) {
    return `Opponent moved to ${move.toRow + 1},${move.toColumn + 1}`;
  }

  return (
    move.notation ??
    `${pieceAbbreviation(move.pieceType ?? 'FLAG', definitions)} to ${move.toRow + 1},${
      move.toColumn + 1
    }`
  );
}

function turnRailTone(
  currentTurn: PlayerSide | null | undefined,
  viewerSide: PlayerSide | undefined,
) {
  if (!currentTurn || !viewerSide) {
    return 'bg-white/20';
  }

  return currentTurn === viewerSide
    ? 'bg-[#79d15f] shadow-[0_0_18px_rgba(121,209,95,0.55)]'
    : 'bg-[#b94f3f] shadow-[0_0_18px_rgba(185,79,63,0.45)]';
}

function turnRailLabel(
  currentTurn: PlayerSide | null | undefined,
  viewerSide: PlayerSide | undefined,
  phase: GameState['phase'] | undefined,
) {
  if (phase === 'SETUP') {
    return 'Setup phase';
  }

  if (!currentTurn || !viewerSide) {
    return 'No active turn';
  }

  return currentTurn === viewerSide ? 'Your turn' : 'Opponent turn';
}

function chatStatusText(state: GameState | undefined, connected: boolean) {
  if (!state) {
    return 'Loading match room...';
  }

  if (state.phase === 'SETUP') {
    return 'Setup phase is active. Arrange your pieces and mark ready.';
  }

  if (state.phase === 'PLAYING') {
    return connected ? 'Realtime connection is active.' : 'Realtime connection is idle.';
  }

  return state.winnerSide
    ? `${state.winnerSide} won by ${state.winReason ?? 'game over'}.`
    : `Game over${state.drawReason ? ` by ${state.drawReason}` : ''}.`;
}

function showMutationError(error: unknown, toast: ReturnType<typeof useToast>['toast']) {
  toast({
    title: 'Action failed',
    description: error instanceof Error ? error.message : 'The server rejected that action.',
  });
}

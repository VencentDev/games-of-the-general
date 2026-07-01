'use client';

import Link from 'next/link';
import Image, { type StaticImageData } from 'next/image';
import { useRouter } from 'next/navigation';
import { type CSSProperties, type DragEvent, useEffect, useMemo, useRef, useState } from 'react';
import { toast } from 'sonner';
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
import captainImage from '@/assets/gg assets/Captain.png';
import colonelImage from '@/assets/gg assets/colonel.png';
import drawImage from '@/assets/gg assets/draw.png';
import firstLieutenantImage from '@/assets/gg assets/1st Lieutenant.png';
import fiveStarImage from '@/assets/gg assets/5star.jpg';
import flagImage from '@/assets/gg assets/flag.png';
import fourStarImage from '@/assets/gg assets/4star.png';
import gameOverLostImage from '@/assets/gg assets/gameover you lost.png';
import gameOverWinImage from '@/assets/gg assets/gameover you win.png';
import lieutenantColonelImage from '@/assets/gg assets/Lieutenant Colonel.png';
import lostImage from '@/assets/gg assets/lost.png';
import majorImage from '@/assets/gg assets/Major.png';
import oneStarImage from '@/assets/gg assets/1star.png';
import privateImage from '@/assets/gg assets/private.png';
import secondLieutenantImage from '@/assets/gg assets/2nd Lieutenant.png';
import sergeantImage from '@/assets/gg assets/Sergeant.png';
import spyImage from '@/assets/gg assets/spy.png';
import threeStarImage from '@/assets/gg assets/3star.png';
import twoStarImage from '@/assets/gg assets/2star.png';
import wonImage from '@/assets/gg assets/won.png';
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
  type SetupPieceInput,
  useCreateMatch,
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
import { ApiError } from '@/lib/api';
import { cn } from '@/lib/utils';

const BOARD_ROWS = 8;
const BOARD_COLUMNS = 9;

const PIECE_IMAGES: Record<PieceType, StaticImageData> = {
  FIVE_STAR_GENERAL: fiveStarImage,
  FOUR_STAR_GENERAL: fourStarImage,
  THREE_STAR_GENERAL: threeStarImage,
  TWO_STAR_GENERAL: twoStarImage,
  ONE_STAR_GENERAL: oneStarImage,
  COLONEL: colonelImage,
  LT_COLONEL: lieutenantColonelImage,
  MAJOR: majorImage,
  CAPTAIN: captainImage,
  FIRST_LIEUTENANT: firstLieutenantImage,
  SECOND_LIEUTENANT: secondLieutenantImage,
  SERGEANT: sergeantImage,
  SPY: spyImage,
  PRIVATE: privateImage,
  FLAG: flagImage,
};

type EmptySquare = BoardSquare & {
  piece: null;
};

export function MatchRoomPageContent({ matchId }: { matchId: string }) {
  const router = useRouter();
  const match = useMatch(matchId);
  const gameModel = useGameModel();
  const gameState = useGameState(matchId);
  const moveHistory = useMoveHistory(matchId);
  const leaveMatch = useLeaveMatch();
  const updateSetup = useUpdateSetup(matchId);
  const markReady = useMarkReady(matchId);
  const movePiece = useMovePiece(matchId);
  const createMatch = useCreateMatch();
  const socket = useMatchSocket(matchId);
  const [inviteOpen, setInviteOpen] = useState(false);
  const [copied, setCopied] = useState(false);
  const [leaveDialogOpen, setLeaveDialogOpen] = useState(false);
  const [selectedPieceId, setSelectedPieceId] = useState<string | null>(null);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [resultOverlay, setResultOverlay] = useState<{
    image: StaticImageData;
    alt: string;
    durationMs: number;
  } | null>(null);
  const [movementOverlay, setMovementOverlay] = useState<{
    key: string;
    move: MoveHistory;
  } | null>(null);
  const [gameOverDialogOpen, setGameOverDialogOpen] = useState(false);
  const [draggingSetupPieceId, setDraggingSetupPieceId] = useState<string | null>(null);
  const moveInFlightRef = useRef(false);
  const lastSeenMoveNumberRef = useRef(0);
  const gameOverShownRef = useRef(false);

  const state = gameState.data;
  const viewerSide = state?.viewerSide;
  const isViewerTurn = state?.phase === 'PLAYING' && state.currentTurn === viewerSide;
  const selectedPiece = state?.ownPieces.find((piece) => piece.id === selectedPieceId) ?? null;
  const legalMoves = useLegalMoves(
    matchId,
    selectedPieceId,
    isViewerTurn && selectedPiece?.status === 'ACTIVE',
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
  const setupEndsAt = state?.setupEndsAt ?? match.data?.setupEndsAt ?? null;
  const setupRemainingMs =
    state?.phase === 'SETUP' && setupEndsAt
      ? Math.max(0, new Date(setupEndsAt).getTime() - nowMs)
      : null;
  const setupLocked = setupRemainingMs !== null && setupRemainingMs <= 0;
  const canMarkReady =
    state?.phase === 'SETUP' && activePlacedCount === 21 && !ownReady && !setupLocked;
  const canEditSetup = state?.phase === 'SETUP' && !ownReady && !setupLocked;
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
    if (state?.phase !== 'SETUP' || !setupEndsAt) {
      return;
    }

    const interval = window.setInterval(() => setNowMs(Date.now()), 250);
    return () => window.clearInterval(interval);
  }, [setupEndsAt, state?.phase]);
  useEffect(() => {
    if (!selectedPieceId || state?.ownPieces.some((piece) => piece.id === selectedPieceId)) {
      return;
    }

    setSelectedPieceId(null);
  }, [selectedPieceId, state?.ownPieces]);

  useEffect(() => {
    if (state?.phase !== 'PLAYING' || isViewerTurn) {
      return;
    }

    setSelectedPieceId(null);
  }, [isViewerTurn, state?.phase]);

  useEffect(() => {
    if (!viewerSide || moveHistory.data === undefined) {
      return;
    }

    const latestMove = moveHistory.data.at(-1);
    if (!latestMove) {
      return;
    }

    if (latestMove.moveNumber <= lastSeenMoveNumberRef.current) {
      return;
    }

    const shouldIgnoreInitialHistory = lastSeenMoveNumberRef.current === 0;
    lastSeenMoveNumberRef.current = latestMove.moveNumber;

    if (shouldIgnoreInitialHistory || state?.phase === 'GAME_OVER') {
      return;
    }

    setMovementOverlay({
      key: `${latestMove.moveNumber}-${latestMove.pieceId}`,
      move: latestMove,
    });

    if (
      latestMove.resultingPhase === 'GAME_OVER' ||
      !latestMove.targetPieceId ||
      !latestMove.battleResult
    ) {
      return;
    }

    const outcome = captureOutcomeForViewer(latestMove, viewerSide);
    if (!outcome) {
      return;
    }

    setResultOverlay({
      image: outcome === 'WON' ? wonImage : outcome === 'LOST' ? lostImage : drawImage,
      alt: outcome === 'WON' ? 'Won capture' : outcome === 'LOST' ? 'Lost capture' : 'Draw capture',
      durationMs: 1_500,
    });
  }, [moveHistory.data, state?.phase, viewerSide]);

  useEffect(() => {
    if (!state || state.phase !== 'GAME_OVER' || gameOverShownRef.current) {
      return;
    }

    gameOverShownRef.current = true;
    const viewerWon = state.winnerSide === state.viewerSide;
    setResultOverlay({
      image: viewerWon ? gameOverWinImage : gameOverLostImage,
      alt: viewerWon ? 'Game over, you win' : 'Game over, you lost',
      durationMs: 5_000,
    });

    const timeout = window.setTimeout(() => {
      setGameOverDialogOpen(true);
    }, 5_000);

    return () => window.clearTimeout(timeout);
  }, [state]);

  useEffect(() => {
    if (!resultOverlay) {
      return;
    }

    const timeout = window.setTimeout(() => setResultOverlay(null), resultOverlay.durationMs);
    return () => window.clearTimeout(timeout);
  }, [resultOverlay]);

  useEffect(() => {
    if (!movementOverlay) {
      return;
    }

    const timeout = window.setTimeout(() => setMovementOverlay(null), 980);
    return () => window.clearTimeout(timeout);
  }, [movementOverlay]);

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
      if (state.phase === 'PLAYING' && !isViewerTurn) {
        toast.error("It's not your turn yet", {
          description: 'Wait for your side color on the turn rail before selecting a move.',
        });
        return;
      }

      if (selectedPieceId === visibleOwnPiece.id) {
        setSelectedPieceId(null);
        return;
      }

      if (state.phase === 'SETUP' && selectedPieceId) {
        swapSelectedPieceWith(visibleOwnPiece.id);
        return;
      }

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
      if (!isViewerTurn) {
        toast.error("It's not your turn yet", {
          description: 'Wait for your side color on the turn rail before moving.',
        });
        return;
      }

      moveSelectedPiece(square.position.row, square.position.column);
    }
  }

  function placeSelectedPiece(currentState: GameState, row: number, column: number) {
    if (!viewerSide || !selectedPieceId || !canEditSetup) {
      return;
    }

    if (!isSetupRow(row, viewerSide)) {
      toast.error('Invalid setup square', {
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
      onSuccess: () => setSelectedPieceId(null),
      onError: showMutationError,
    });
  }

  function swapSelectedPieceWith(targetPieceId: string) {
    if (!state || !selectedPieceId || !canEditSetup || updateSetup.isPending) {
      return;
    }

    const selected = state.ownPieces.find((piece) => piece.id === selectedPieceId);
    const target = state.ownPieces.find((piece) => piece.id === targetPieceId);
    if (!selected || !target || target.status !== 'ACTIVE') {
      return;
    }

    updateSetup.mutate(
      [
        {
          pieceId: selected.id,
          row: target.row,
          column: target.column,
        },
        {
          pieceId: target.id,
          row: selected.row,
          column: selected.column,
        },
      ],
      {
        onSuccess: () => setSelectedPieceId(null),
        onError: showMutationError,
      },
    );
  }

  function moveSetupPieceToSquare(pieceId: string, row: number, column: number) {
    if (!state || !viewerSide || !canEditSetup || updateSetup.isPending) {
      return;
    }

    if (!isSetupRow(row, viewerSide)) {
      toast.error('Invalid setup square', {
        description: `${viewerSide} can only place pieces in its three setup rows.`,
      });
      return;
    }

    const movingPiece = state.ownPieces.find((piece) => piece.id === pieceId);
    if (!movingPiece || movingPiece.status === 'CAPTURED') {
      return;
    }

    const targetPiece = state.ownPieces.find(
      (piece) => piece.status === 'ACTIVE' && piece.row === row && piece.column === column,
    );

    if (targetPiece?.id === movingPiece.id) {
      setSelectedPieceId(null);
      return;
    }

    const pieces: SetupPieceInput[] = [
      {
        pieceId: movingPiece.id,
        row,
        column,
      },
    ];

    if (targetPiece) {
      pieces.push({
        pieceId: targetPiece.id,
        row: movingPiece.row,
        column: movingPiece.column,
      });
    }

    updateSetup.mutate(pieces, {
      onSuccess: () => setSelectedPieceId(null),
      onError: showMutationError,
    });
  }

  function handleSetupPieceDragStart(event: DragEvent<HTMLElement>, pieceId: string) {
    if (!canEditSetup) {
      event.preventDefault();
      return;
    }

    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', pieceId);
    setDraggingSetupPieceId(pieceId);
    setSelectedPieceId(pieceId);
  }

  function handleSetupDragOver(event: DragEvent<HTMLElement>, row: number) {
    if (!viewerSide || !canEditSetup || !isSetupRow(row, viewerSide)) {
      return;
    }

    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }

  function handleSetupDrop(event: DragEvent<HTMLElement>, row: number, column: number) {
    event.preventDefault();
    const pieceId = event.dataTransfer.getData('text/plain') || draggingSetupPieceId;
    setDraggingSetupPieceId(null);

    if (!pieceId) {
      return;
    }

    moveSetupPieceToSquare(pieceId, row, column);
  }

  function moveSelectedPiece(row: number, column: number) {
    if (!selectedPieceId || !isViewerTurn || movePiece.isPending || moveInFlightRef.current) {
      return;
    }

    const legalTarget = legalTargets.get(positionKey(row, column));
    if (!legalTarget) {
      return;
    }

    const movingPieceId = selectedPieceId;
    moveInFlightRef.current = true;
    setSelectedPieceId(null);
    movePiece.mutate(
      { pieceId: movingPieceId, toRow: row, toColumn: column },
      {
        onSettled: () => {
          moveInFlightRef.current = false;
        },
        onError: showMutationError,
      },
    );
  }

  function unplaceSelectedPiece() {
    if (!selectedPieceId || !canEditSetup) {
      return;
    }

    updateSetup.mutate([{ pieceId: selectedPieceId, row: null, column: null }], {
      onSuccess: () => setSelectedPieceId(null),
      onError: showMutationError,
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
      onSuccess: () => setSelectedPieceId(null),
      onError: showMutationError,
    });
  }

  function readySetup() {
    markReady.mutate(undefined, {
      onError: showMutationError,
    });
  }

  function rematch() {
    if (!match.data) {
      router.push('/lobby');
      return;
    }

    createMatch.mutate(
      {
        name: `${match.data.name} rematch`,
        visibility: match.data.visibility,
        mode: match.data.mode,
        preparationSeconds: match.data.preparationSeconds,
      },
      {
        onSuccess: (createdMatch) => router.push(`/matches/${createdMatch.id}`),
        onError: showMutationError,
      },
    );
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
                  setupRemainingMs={setupRemainingMs}
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
                <div className="relative">
                  <div className="grid aspect-[9/8] grid-cols-9 grid-rows-[repeat(8,minmax(0,1fr))] overflow-hidden rounded-md border border-[#6b4d2f] bg-[#e5be84] shadow-inner">
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
                          disabled={
                            busy ||
                            gameState.isLoading ||
                            state?.phase === 'GAME_OVER' ||
                            setupLocked
                          }
                          draggable={
                            canEditSetup &&
                            Boolean(square.piece?.visible && square.piece.side === viewerSide)
                          }
                          onClick={() => handleSquareClick(square)}
                          onDragEnd={() => setDraggingSetupPieceId(null)}
                          onDragOver={(event) => handleSetupDragOver(event, square.position.row)}
                          onDragStart={(event) => {
                            if (!square.piece?.visible || square.piece.side !== viewerSide) {
                              event.preventDefault();
                              return;
                            }

                            handleSetupPieceDragStart(event, square.piece.id);
                          }}
                          onDrop={(event) =>
                            handleSetupDrop(event, square.position.row, square.position.column)
                          }
                        >
                          <span
                            className={cn(
                              'flex h-[68%] w-[78%] items-center justify-center rounded-sm border-2 text-[clamp(0.6rem,1.35vw,1.05rem)] font-black shadow-md',
                              pieceTone(square.piece?.side),
                              !square.piece && 'border-transparent bg-transparent shadow-none',
                              square.piece &&
                                !square.piece.visible &&
                                'h-[58%] w-[58%] rounded-full border-[#111315] bg-[#202326] text-[#f7ecd8]',
                            )}
                          >
                            <PieceFace
                              piece={square.piece}
                              definitions={pieceDefinitions}
                              size="board"
                            />
                          </span>
                        </button>
                      );
                    })}
                  </div>
                  {movementOverlay && viewerSide ? (
                    <MoveAnimationOverlay
                      key={movementOverlay.key}
                      move={movementOverlay.move}
                      viewerSide={viewerSide}
                      definitions={pieceDefinitions}
                    />
                  ) : null}
                  {resultOverlay ? (
                    <div className="pointer-events-none absolute inset-0 z-20 flex items-center justify-center bg-transparent p-6">
                      <Image
                        src={resultOverlay.image}
                        alt={resultOverlay.alt}
                        className="h-auto max-h-[42%] w-[72%] max-w-[44rem] object-contain drop-shadow-2xl"
                        priority
                      />
                    </div>
                  ) : null}
                  {state?.phase === 'GAME_OVER' && !resultOverlay ? (
                    <div className="absolute inset-0 flex items-center justify-center bg-black/45 p-6">
                      <div className="rounded-md border border-white/15 bg-[#111315]/95 px-6 py-4 text-center shadow-2xl">
                        <p className="text-xs font-semibold uppercase tracking-[0.18em] text-white/55">
                          Game Over
                        </p>
                        <p className="mt-2 text-2xl font-black">
                          {state.winnerSide
                            ? `${state.winnerSide} wins`
                            : state.drawReason
                              ? 'Draw'
                              : 'Match ended'}
                        </p>
                        <p className="mt-1 text-sm text-white/65">
                          {state.winReason ?? state.drawReason ?? 'Final position reached'}
                        </p>
                      </div>
                    </div>
                  ) : null}
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
                    setupRemainingMs={setupRemainingMs}
                  />
                </div>
              ) : null}
            </div>
          </section>

          <aside className="flex min-h-[38rem] min-w-0 flex-col gap-3">
            {state?.phase === 'SETUP' ? (
              <section className="flex min-h-0 flex-col rounded-lg border border-white/15 bg-white/[0.04]">
                <div className="border-b border-white/10 px-4 py-3">
                  <h2 className="font-black">Preparation</h2>
                  <p className="mt-1 text-xs text-white/55">{activePlacedCount}/21 placed</p>
                </div>
                <div className="space-y-3 p-3">
                  <div className="grid grid-cols-2 gap-2">
                    <Button
                      type="button"
                      size="sm"
                      variant="outline"
                      className="h-8 border-white/15 bg-white/5 text-white hover:bg-white/10"
                      disabled={!canEditSetup || updateSetup.isPending}
                      onClick={autoFillSetup}
                    >
                      <RotateCcw className="mr-2 size-3.5" />
                      Fill
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
                  </div>
                  <Button
                    type="button"
                    size="sm"
                    className="h-8 w-full bg-[#d6a348] text-[#121212] hover:bg-[#e2b45b]"
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
                <div className="grid max-h-[31rem] grid-cols-4 gap-2 overflow-auto border-t border-white/10 p-3">
                  {(state?.ownPieces ?? []).map((piece) => (
                    <SetupPieceButton
                      key={piece.id}
                      piece={piece}
                      definition={pieceDefinitions.get(piece.type)}
                      selected={selectedPieceId === piece.id}
                      disabled={!canEditSetup}
                      onSelect={() => setSelectedPieceId(piece.id)}
                      onDragEnd={() => setDraggingSetupPieceId(null)}
                      onDragStart={(event) => handleSetupPieceDragStart(event, piece.id)}
                    />
                  ))}
                </div>
              </section>
            ) : (
              <>
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
              </>
            )}
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
              onClick={() => setLeaveDialogOpen(true)}
              type="button"
            >
              <DoorOpen className="mr-2 size-4" />
              Leave Match
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

      <Dialog open={leaveDialogOpen} onOpenChange={setLeaveDialogOpen}>
        <DialogContent className="border-white/15 bg-[#181b15] text-[#f6f0e4] sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2 text-2xl font-black tracking-normal">
              <DoorOpen className="size-5 text-[#d6a348]" />
              Leave match?
            </DialogTitle>
            <DialogDescription className="text-sm leading-6 text-white/65">
              Are you sure you want to leave this match?
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2">
            <Button
              type="button"
              variant="outline"
              className="border-white/15 bg-white/5 text-white hover:bg-white/10"
              disabled={leaveMatch.isPending}
              onClick={() => setLeaveDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              type="button"
              className="bg-[#8f2f24] text-white hover:bg-[#76251c]"
              disabled={leaveMatch.isPending}
              onClick={leave}
            >
              {leaveMatch.isPending ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
              Leave Match
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={gameOverDialogOpen} onOpenChange={setGameOverDialogOpen}>
        <DialogContent className="border-white/15 bg-[#181b15] text-[#f6f0e4] sm:max-w-md">
          <DialogHeader>
            <DialogTitle className="text-2xl font-black tracking-normal">Game over</DialogTitle>
            <DialogDescription className="text-sm leading-6 text-white/65">
              {state?.winnerSide
                ? `${state.winnerSide} won by ${state.winReason ?? 'final result'}.`
                : `Match ended${state?.drawReason ? ` by ${state.drawReason}` : ''}.`}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="gap-2 sm:justify-between">
            <Button
              type="button"
              variant="outline"
              className="border-white/15 bg-white/5 text-white hover:bg-white/10"
              onClick={() => router.push('/lobby')}
            >
              Return to lobby
            </Button>
            <Button
              type="button"
              className="bg-[#d6a348] text-[#121212] hover:bg-[#e2b45b]"
              disabled={createMatch.isPending}
              onClick={rematch}
            >
              {createMatch.isPending ? <Loader2 className="mr-2 size-4 animate-spin" /> : null}
              Rematch
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
  setupRemainingMs,
}: {
  seat: MatchSeat;
  active: boolean;
  viewerSide: PlayerSide | undefined;
  capturedPieces: GameState['capturedPieces'];
  definitions: Map<PieceType, PieceDefinition>;
  setupRemainingMs: number | null;
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
        {setupRemainingMs !== null ? (
          <div className="ml-1 rounded-sm bg-white px-2 py-1 font-mono text-sm font-black tabular-nums text-[#141414] shadow">
            {formatSetupTime(setupRemainingMs)}
          </div>
        ) : null}
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
                      ? 'rounded-sm border-[#d46b5d] bg-[#7e2c23] text-[#ffd7c8]'
                      : 'rounded-sm border-[#6ea1d6] bg-[#244b78] text-[#d9ecff]'
                    : 'border-white/15 bg-[#050607] text-transparent',
                )}
                aria-label={isViewer ? pieceAbbreviation(item.type, definitions) : 'Hidden piece'}
              >
                {isViewer ? (
                  <Image
                    src={PIECE_IMAGES[item.type]}
                    alt={definitions.get(item.type)?.label ?? item.type}
                    className="size-full rounded-sm object-cover"
                  />
                ) : null}
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
  disabled,
  onSelect,
  onDragEnd,
  onDragStart,
}: {
  piece: PieceInstance;
  definition: PieceDefinition | undefined;
  selected: boolean;
  disabled: boolean;
  onSelect: () => void;
  onDragEnd: () => void;
  onDragStart: (event: DragEvent<HTMLButtonElement>) => void;
}) {
  const isDisabled = disabled || piece.status === 'CAPTURED';

  return (
    <button
      type="button"
      className={cn(
        'flex aspect-[4/3] min-w-0 items-center justify-center rounded-sm border text-xs font-black transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[#f6e09f]',
        piece.status === 'ACTIVE'
          ? 'border-white/10 bg-white/10 text-white/50'
          : 'border-[#d6a348] bg-[#27211a] text-[#f6d38b] hover:bg-[#352917]',
        selected && 'ring-2 ring-[#f6e09f]',
        piece.status === 'CAPTURED' && 'opacity-35',
        isDisabled && 'cursor-not-allowed opacity-45',
      )}
      disabled={isDisabled}
      draggable={!isDisabled}
      title={definition?.label ?? piece.type}
      onClick={onSelect}
      onDragEnd={onDragEnd}
      onDragStart={onDragStart}
    >
      <Image
        src={PIECE_IMAGES[piece.type]}
        alt={definition?.label ?? piece.type}
        className="h-[82%] w-[90%] rounded-sm object-cover"
      />
    </button>
  );
}

function MoveAnimationOverlay({
  move,
  viewerSide,
  definitions,
}: {
  move: MoveHistory;
  viewerSide: PlayerSide;
  definitions: Map<PieceType, PieceDefinition>;
}) {
  const from = toDisplayPosition(move.fromRow, move.fromColumn, viewerSide);
  const to = toDisplayPosition(move.toRow, move.toColumn, viewerSide);
  const fromX = ((from.column + 0.5) / BOARD_COLUMNS) * 100;
  const fromY = ((from.row + 0.5) / BOARD_ROWS) * 100;
  const toX = ((to.column + 0.5) / BOARD_COLUMNS) * 100;
  const toY = ((to.row + 0.5) / BOARD_ROWS) * 100;
  const visible = move.actingSide === viewerSide && Boolean(move.pieceType);
  const animatedPiece: BoardSquare['piece'] = {
    id: move.pieceId,
    side: move.actingSide,
    visible,
    type: visible ? move.pieceType : null,
    label: null,
    abbreviation: null,
    rank: null,
  };

  return (
    <div
      className="pointer-events-none absolute inset-0 z-10 overflow-hidden rounded-md"
      aria-hidden="true"
    >
      <svg
        className="got-move-trail absolute inset-0 size-full"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
      >
        <line x1={fromX} y1={fromY} x2={toX} y2={toY} />
      </svg>
      <span
        className="got-move-square got-move-square-from absolute"
        style={
          {
            '--square-x': `${fromX}%`,
            '--square-y': `${fromY}%`,
          } as CSSProperties
        }
      />
      <span
        className="got-move-square got-move-square-to absolute"
        style={
          {
            '--square-x': `${toX}%`,
            '--square-y': `${toY}%`,
          } as CSSProperties
        }
      />
      <span
        className={cn(
          'got-move-ghost absolute flex items-center justify-center rounded-sm border-2 text-[clamp(0.6rem,1.35vw,1.05rem)] font-black shadow-2xl',
          pieceTone(move.actingSide),
          !visible && 'rounded-full border-[#111315] bg-[#202326] text-[#f7ecd8]',
        )}
        style={
          {
            '--from-x': `${fromX}%`,
            '--from-y': `${fromY}%`,
            '--to-x': `${toX}%`,
            '--to-y': `${toY}%`,
          } as CSSProperties
        }
      >
        <PieceFace piece={animatedPiece} definitions={definitions} size="board" />
      </span>
    </div>
  );
}

function PieceFace({
  piece,
  definitions,
  size,
}: {
  piece: BoardSquare['piece'];
  definitions: Map<PieceType, PieceDefinition>;
  size: 'board' | 'small';
}) {
  if (!piece) {
    return null;
  }

  if (!piece.visible || !piece.type) {
    return <span className="text-xs font-black">?</span>;
  }

  return (
    <Image
      src={PIECE_IMAGES[piece.type]}
      alt={definitions.get(piece.type)?.label ?? piece.type}
      className={cn(
        'rounded-full object-cover',
        size === 'board' ? 'h-full w-full rounded-sm' : 'size-7 rounded-sm',
      )}
    />
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

function toDisplayPosition(row: number, column: number, viewerSide: PlayerSide | undefined) {
  if (viewerSide === 'RED') {
    return {
      row: BOARD_ROWS - 1 - row,
      column: BOARD_COLUMNS - 1 - column,
    };
  }

  return { row, column };
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

function captureOutcomeForViewer(move: MoveHistory, viewerSide: PlayerSide) {
  const viewerActed = move.actingSide === viewerSide;

  switch (move.battleResult) {
    case 'ATTACKER_WINS':
    case 'FLAG_CAPTURED':
      return viewerActed ? 'WON' : 'LOST';
    case 'DEFENDER_WINS':
      return viewerActed ? 'LOST' : 'WON';
    case 'BOTH_ELIMINATED':
      return 'DRAW';
    default:
      return null;
  }
}

function turnRailTone(
  currentTurn: PlayerSide | null | undefined,
  viewerSide: PlayerSide | undefined,
) {
  if (!currentTurn || !viewerSide) {
    return 'bg-white/20';
  }

  return currentTurn === 'RED'
    ? 'bg-[#a83228] shadow-[0_0_18px_rgba(168,50,40,0.55)]'
    : 'bg-[#203f66] shadow-[0_0_18px_rgba(32,63,102,0.55)]';
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

function formatSetupTime(milliseconds: number) {
  const totalSeconds = Math.max(0, Math.ceil(milliseconds / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
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

function showMutationError(error: unknown) {
  if (error instanceof ApiError && error.status === 409) {
    const message = error.message.toLowerCase();
    if (message.includes('not your turn')) {
      toast.error("It's not your turn yet", {
        description: 'Wait for your side color on the turn rail before moving.',
      });
      return;
    }

    if (message.includes('existing data') || message.includes('conflicts')) {
      toast.error('Move conflict', {
        description: 'The board changed before that move was applied. Refreshing match state.',
      });
      return;
    }

    toast.error(error.message || 'Action conflicts with the current match state');
    return;
  }

  toast.error('Action failed', {
    description: error instanceof Error ? error.message : 'The server rejected that action.',
  });
}

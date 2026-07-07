'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useSession } from 'next-auth/react';

import { clientApi } from '@/lib/api';

export type MatchVisibility = 'PUBLIC' | 'PRIVATE';

export type MatchSeat = {
  userId: string;
  side: 'RED' | 'BLUE';
  ready: boolean;
  joinedAt: string;
};

export type MatchSummary = {
  id: string;
  name: string;
  visibility: MatchVisibility;
  status: 'WAITING' | 'SETUP' | 'PLAYING' | 'FINISHED' | 'CANCELLED';
  phase: 'SETUP' | 'PLAYING' | 'GAME_OVER';
  currentTurn: PlayerSide | null;
  moveNumber: number;
  mode: string;
  preparationSeconds: number;
  inviteCode: string;
  inviteUrl: string;
  rematchSourceMatchId: string | null;
  rematchRequestedByUserId: string | null;
  pendingRematchMatchId: string | null;
  viewerCanAcceptRematch: boolean;
  hostUserId: string;
  winnerUserId: string | null;
  winnerSide: PlayerSide | null;
  winReason: string | null;
  drawReason: string | null;
  resignedSide: PlayerSide | null;
  createdAt: string | null;
  startedAt: string | null;
  setupStartedAt: string | null;
  setupEndsAt: string | null;
  finishedAt: string | null;
  seats: MatchSeat[];
};

export type MatchmakingStatus = 'ACTIVE' | 'QUEUED' | 'MATCHED';

export type MatchmakingResponse = {
  status: MatchmakingStatus;
  match: MatchSummary | null;
  enqueuedAt: string | null;
  preparationSeconds: number | null;
};

export type MatchChatMessage = {
  id: string | null;
  type: 'CHAT_MESSAGE' | 'CHAT_EVENT';
  matchId: string;
  subject: string;
  displayName: string;
  message: string;
  occurredAt: string;
};

export type GameModel = {
  rows: number;
  columns: number;
  setupRowsPerPlayer: number;
  piecesPerPlayer: number;
  vacantSetupSquares: number;
  phases: string[];
  movement: string[];
  pieces: PieceDefinition[];
};

export type PlayerSide = 'RED' | 'BLUE';
export type PieceStatus = 'UNPLACED' | 'ACTIVE' | 'CAPTURED';
export type PieceType =
  | 'FIVE_STAR_GENERAL'
  | 'FOUR_STAR_GENERAL'
  | 'THREE_STAR_GENERAL'
  | 'TWO_STAR_GENERAL'
  | 'ONE_STAR_GENERAL'
  | 'COLONEL'
  | 'LT_COLONEL'
  | 'MAJOR'
  | 'CAPTAIN'
  | 'FIRST_LIEUTENANT'
  | 'SECOND_LIEUTENANT'
  | 'SERGEANT'
  | 'SPY'
  | 'PRIVATE'
  | 'FLAG';

export type PieceDefinition = {
  type: PieceType;
  label: string;
  abbreviation: string;
  rank: number;
  count: number;
};

export type BoardPosition = {
  row: number;
  column: number;
};

export type VisiblePiece = {
  id: string;
  side: PlayerSide;
  visible: boolean;
  type: PieceType | null;
  label: string | null;
  abbreviation: string | null;
  rank: number | null;
};

export type BoardSquare = {
  position: BoardPosition;
  piece: VisiblePiece | null;
};

export type PieceInstance = {
  id: string;
  side: PlayerSide;
  type: PieceType;
  status: PieceStatus;
  row: number | null;
  column: number | null;
};

export type CapturedPiece = {
  pieceId: string;
  side: PlayerSide;
  type: PieceType;
  capturedBySide: PlayerSide;
  capturedOnMoveNumber: number | null;
};

export type GameState = {
  matchId: string;
  phase: 'SETUP' | 'PLAYING' | 'GAME_OVER';
  status: MatchSummary['status'];
  viewerSide: PlayerSide;
  currentTurn: PlayerSide | null;
  moveNumber: number;
  winnerSide: PlayerSide | null;
  winReason: string | null;
  drawReason: string | null;
  setupStartedAt: string | null;
  setupEndsAt: string | null;
  board: BoardSquare[];
  ownPieces: PieceInstance[];
  capturedPieces: CapturedPiece[];
};

export type SetupPieceInput = {
  pieceId: string;
  row: number | null;
  column: number | null;
};

export type LegalMove = {
  position: BoardPosition;
  attack: boolean;
};

export type MoveHistory = {
  moveNumber: number;
  actingSide: PlayerSide;
  pieceId: string;
  pieceType: PieceType | null;
  fromRow: number;
  fromColumn: number;
  toRow: number;
  toColumn: number;
  targetPieceId: string | null;
  targetPieceType: PieceType | null;
  battleResult: string | null;
  resultingPhase: string | null;
  notation: string | null;
  createdAt: string | null;
};

export type MoveInput = {
  pieceId: string;
  toRow: number;
  toColumn: number;
};

export type MoveResult = {
  state: GameState;
  move: MoveHistory;
};

export type PlayerLobbySettings = {
  challengeReveal: string;
  invitePrivacy: string;
  reconnectSeconds: number;
  soundEnabled: boolean;
};

export type CreateMatchInput = {
  name: string;
  visibility: MatchVisibility;
  mode: string;
  preparationSeconds: number;
};

export type FindMatchInput = {
  preparationSeconds: number;
};

export const lobbyKeys = {
  activeMatch: ['lobby', 'active-match'] as const,
  publicMatches: ['lobby', 'public-matches'] as const,
  matchmaking: ['lobby', 'matchmaking'] as const,
  history: ['lobby', 'history'] as const,
  gameModel: ['lobby', 'game-model'] as const,
  settings: ['lobby', 'settings'] as const,
  match: (matchId: string) => ['lobby', 'match', matchId] as const,
  chat: (matchId: string) => ['lobby', 'match', matchId, 'chat'] as const,
  state: (matchId: string) => ['lobby', 'match', matchId, 'state'] as const,
  moves: (matchId: string) => ['lobby', 'match', matchId, 'moves'] as const,
  legalMoves: (matchId: string, pieceId: string | null) =>
    ['lobby', 'match', matchId, 'legal-moves', pieceId] as const,
};

export function usePublicMatches() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.publicMatches,
    queryFn: () => clientApi<MatchSummary[]>(session?.accessToken, '/api/v1/matches/public'),
    enabled: !!session?.accessToken,
  });
}

export function useActiveMatch() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.activeMatch,
    queryFn: async () =>
      (await clientApi<MatchSummary | undefined>(session?.accessToken, '/api/v1/matches/active')) ??
      null,
    enabled: !!session?.accessToken,
  });
}

export function useMatchHistory() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.history,
    queryFn: () => clientApi<MatchSummary[]>(session?.accessToken, '/api/v1/matches/history'),
    enabled: !!session?.accessToken,
  });
}

export function useMatchByInviteCode(inviteCode: string) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: ['lobby', 'invite', inviteCode] as const,
    queryFn: () =>
      clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/invite/${inviteCode}`),
    enabled: !!session?.accessToken && !!inviteCode,
  });
}

export function useMatch(matchId: string) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.match(matchId),
    queryFn: () => clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/${matchId}`),
    enabled: !!session?.accessToken && !!matchId,
    refetchInterval: (query) => (query.state.data?.phase === 'GAME_OVER' ? false : 2_500),
    refetchIntervalInBackground: true,
  });
}

export function useMatchChat(matchId: string) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.chat(matchId),
    queryFn: () =>
      clientApi<MatchChatMessage[]>(session?.accessToken, `/api/v1/matches/${matchId}/chat`),
    enabled: !!session?.accessToken && !!matchId,
  });
}

export function useClearMatchChat() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) =>
      clientApi<void>(session?.accessToken, `/api/v1/matches/${matchId}/chat`, {
        method: 'DELETE',
      }),
    onSuccess: (_, matchId) => {
      queryClient.setQueryData(lobbyKeys.chat(matchId), []);
    },
  });
}

export function useSendMatchChat(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (message: string) =>
      clientApi<MatchChatMessage>(session?.accessToken, `/api/v1/matches/${matchId}/chat`, {
        method: 'POST',
        body: JSON.stringify({ message }),
      }),
    onSuccess: (message) => {
      queryClient.setQueryData<MatchChatMessage[]>(lobbyKeys.chat(matchId), (current) => [
        ...(current ?? []),
        message,
      ]);
    },
  });
}

export function useGameState(matchId: string) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.state(matchId),
    queryFn: () => clientApi<GameState>(session?.accessToken, `/api/v1/matches/${matchId}/state`),
    enabled: !!session?.accessToken && !!matchId,
    refetchInterval: (query) => (query.state.data?.phase === 'GAME_OVER' ? false : 1_500),
    refetchIntervalInBackground: true,
  });
}

export function useGameModel() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.gameModel,
    queryFn: () => clientApi<GameModel>(session?.accessToken, '/api/v1/game-model'),
    enabled: !!session?.accessToken,
  });
}

export function usePlayerLobbySettings() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.settings,
    queryFn: () =>
      clientApi<PlayerLobbySettings>(session?.accessToken, '/api/v1/player-lobby-settings/me'),
    enabled: !!session?.accessToken,
  });
}

export function useCreateMatch() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: CreateMatchInput) =>
      clientApi<MatchSummary>(session?.accessToken, '/api/v1/matches', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
    },
  });
}

export function useFindMatch() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: FindMatchInput) =>
      clientApi<MatchmakingResponse>(session?.accessToken, '/api/v1/matches/find', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: (response) => {
      if (response.match) {
        queryClient.setQueryData(lobbyKeys.activeMatch, response.match);
        queryClient.setQueryData(lobbyKeys.match(response.match.id), response.match);
      }

      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.matchmaking });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
    },
  });
}

export function useCancelFindMatch() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      clientApi<void>(session?.accessToken, '/api/v1/matches/find/queue', {
        method: 'DELETE',
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.matchmaking });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
    },
  });
}

export function useJoinMatch() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) =>
      clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/${matchId}/join`, {
        method: 'POST',
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
    },
  });
}

export function useRequestRematch(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/${matchId}/rematch`, {
        method: 'POST',
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
    },
  });
}

export function useAcceptRematch(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/${matchId}/rematch/accept`, {
        method: 'POST',
      }),
    onSuccess: (rematch) => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(rematch.id) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
    },
  });
}

export function useLeaveMatch() {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (matchId: string) =>
      clientApi<MatchSummary>(session?.accessToken, `/api/v1/matches/${matchId}/seat`, {
        method: 'DELETE',
      }),
    onSuccess: async (_match, matchId) => {
      await queryClient.cancelQueries({ queryKey: lobbyKeys.match(matchId) });
      await queryClient.cancelQueries({ queryKey: lobbyKeys.state(matchId) });
      await queryClient.cancelQueries({ queryKey: lobbyKeys.moves(matchId) });
      await queryClient.cancelQueries({
        queryKey: ['lobby', 'match', matchId, 'legal-moves'],
      });
      queryClient.setQueryData(lobbyKeys.activeMatch, null);
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.activeMatch });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
      queryClient.removeQueries({ queryKey: lobbyKeys.match(matchId) });
      queryClient.removeQueries({ queryKey: lobbyKeys.state(matchId) });
      queryClient.removeQueries({ queryKey: lobbyKeys.moves(matchId) });
      queryClient.removeQueries({ queryKey: ['lobby', 'match', matchId, 'legal-moves'] });
    },
  });
}

export function useUpdateSetup(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (pieces: SetupPieceInput[]) =>
      clientApi<{ state: GameState }>(session?.accessToken, `/api/v1/matches/${matchId}/setup`, {
        method: 'PUT',
        body: JSON.stringify({ pieces }),
      }),
    onSuccess: (response) => {
      queryClient.setQueryData(lobbyKeys.state(matchId), response.state);
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
    },
    onError: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.state(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
    },
  });
}

export function useMarkReady(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () =>
      clientApi<{ state: GameState }>(
        session?.accessToken,
        `/api/v1/matches/${matchId}/setup/ready`,
        { method: 'POST' },
      ),
    onSuccess: (response) => {
      queryClient.setQueryData(lobbyKeys.state(matchId), response.state);
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
    },
  });
}

export function useLegalMoves(matchId: string, pieceId: string | null, enabled: boolean) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.legalMoves(matchId, pieceId),
    queryFn: () =>
      clientApi<LegalMove[]>(
        session?.accessToken,
        `/api/v1/matches/${matchId}/pieces/${pieceId}/legal-moves`,
      ),
    enabled: !!session?.accessToken && !!matchId && !!pieceId && enabled,
  });
}

export function useMovePiece(matchId: string) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: MoveInput) =>
      clientApi<MoveResult>(session?.accessToken, `/api/v1/matches/${matchId}/moves`, {
        method: 'POST',
        body: JSON.stringify(input),
      }),
    onSuccess: (response) => {
      queryClient.setQueryData(lobbyKeys.state(matchId), response.state);
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.moves(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
      void queryClient.invalidateQueries({ queryKey: ['lobby', 'match', matchId, 'legal-moves'] });
    },
    onError: () => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.state(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.moves(matchId) });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
      void queryClient.invalidateQueries({ queryKey: ['lobby', 'match', matchId, 'legal-moves'] });
    },
  });
}

export function useMoveHistory(matchId: string) {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.moves(matchId),
    queryFn: () =>
      clientApi<MoveHistory[]>(session?.accessToken, `/api/v1/matches/${matchId}/moves`),
    enabled: !!session?.accessToken && !!matchId,
    refetchInterval: 1_500,
    refetchIntervalInBackground: true,
  });
}

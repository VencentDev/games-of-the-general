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
  mode: string;
  preparationSeconds: number;
  inviteCode: string;
  inviteUrl: string;
  hostUserId: string;
  winnerUserId: string | null;
  winReason: string | null;
  createdAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  seats: MatchSeat[];
};

export type GameModel = {
  rows: number;
  columns: number;
  setupRowsPerPlayer: number;
  piecesPerPlayer: number;
  vacantSetupSquares: number;
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

export const lobbyKeys = {
  publicMatches: ['lobby', 'public-matches'] as const,
  history: ['lobby', 'history'] as const,
  gameModel: ['lobby', 'game-model'] as const,
  settings: ['lobby', 'settings'] as const,
  match: (matchId: string) => ['lobby', 'match', matchId] as const,
};

export function usePublicMatches() {
  const { data: session } = useSession();

  return useQuery({
    queryKey: lobbyKeys.publicMatches,
    queryFn: () => clientApi<MatchSummary[]>(session?.accessToken, '/api/v1/matches/public'),
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
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
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
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
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
    onSuccess: (_match, matchId) => {
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
      void queryClient.invalidateQueries({ queryKey: lobbyKeys.match(matchId) });
    },
  });
}

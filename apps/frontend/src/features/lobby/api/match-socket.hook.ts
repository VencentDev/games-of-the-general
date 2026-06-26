'use client';

import { Client, type IMessage } from '@stomp/stompjs';
import { useQueryClient } from '@tanstack/react-query';
import { useSession } from 'next-auth/react';
import { useEffect, useMemo, useState } from 'react';

import { lobbyKeys, type MatchSummary } from '@/features/lobby/api/lobby.hooks';

export type MatchSocketEvent =
  | {
      type: string;
      matchId: string;
      match: MatchSummary;
      occurredAt: string;
    }
  | {
      type: string;
      matchId: string;
      subject: string;
      occurredAt: string;
    };

const apiWsUrl = process.env.NEXT_PUBLIC_API_BASE?.replace(/^http/, 'ws').replace(/\/$/, '');
const WS_URL =
  process.env.NEXT_PUBLIC_WS_URL ?? (apiWsUrl ? `${apiWsUrl}/ws` : 'ws://localhost:8080/ws');

export function useMatchSocket(matchId: string | null) {
  const { data: session } = useSession();
  const queryClient = useQueryClient();
  const [connected, setConnected] = useState(false);
  const [events, setEvents] = useState<MatchSocketEvent[]>([]);

  const token = session?.accessToken;
  const client = useMemo(() => {
    if (!matchId || !token) {
      return null;
    }

    return new Client({
      brokerURL: WS_URL,
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug:
        process.env.NODE_ENV === 'development'
          ? (message) => console.debug(message)
          : () => undefined,
    });
  }, [matchId, token]);

  useEffect(() => {
    if (!client || !matchId) {
      setConnected(false);
      return;
    }

    client.onConnect = () => {
      setConnected(true);
      client.subscribe(`/topic/matches/${matchId}`, (message: IMessage) => {
        const event = JSON.parse(message.body) as MatchSocketEvent;

        if ('match' in event) {
          queryClient.setQueryData(lobbyKeys.match(event.matchId), event.match);
          void queryClient.invalidateQueries({ queryKey: lobbyKeys.publicMatches });
          void queryClient.invalidateQueries({ queryKey: lobbyKeys.history });
        }

        setEvents((current) => [event, ...current].slice(0, 20));
      });
      client.publish({ destination: `/app/matches/${matchId}/presence`, body: '{}' });
    };

    client.onDisconnect = () => setConnected(false);
    client.onStompError = () => setConnected(false);
    client.activate();

    return () => {
      void client.deactivate();
      setConnected(false);
    };
  }, [client, matchId, queryClient]);

  return { connected, events };
}

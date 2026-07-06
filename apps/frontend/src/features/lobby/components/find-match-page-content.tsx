'use client';

import { ArrowLeft, LoaderCircle, Search, X } from 'lucide-react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useRef, useState } from 'react';

import { Button } from '@/components/ui/button';
import { useActiveMatch, useCancelFindMatch, useFindMatch } from '@/features/lobby/api/lobby.hooks';

export function FindMatchPageContent() {
  const router = useRouter();
  const activeMatch = useActiveMatch();
  const findMatch = useFindMatch();
  const cancelFindMatch = useCancelFindMatch();
  const [isQueued, setIsQueued] = useState(false);
  const shouldCancelOnUnmountRef = useRef(false);
  const redirectingRef = useRef(false);
  const cancelQueuedSearchRef = useRef<() => void>(() => undefined);

  const redirectToMatch = useCallback(
    (matchId: string) => {
      redirectingRef.current = true;
      shouldCancelOnUnmountRef.current = false;
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

  function startSearch() {
    findMatch.mutate(undefined, {
      onSuccess: (response) => {
        if (response.match?.id) {
          redirectToMatch(response.match.id);
          return;
        }

        if (response.status === 'QUEUED') {
          setIsQueued(true);
        }
      },
    });
  }

  function cancelSearch() {
    shouldCancelOnUnmountRef.current = false;
    setIsQueued(false);
    cancelFindMatch.mutate();
  }

  const isSearching = findMatch.isPending || isQueued;

  return (
    <main className="-mx-4 -my-6 min-h-[calc(100svh-3.5rem)] bg-[#f5f1e6] px-4 py-6 text-[#201b16] dark:bg-[#10130f] dark:text-[#f6f0e4] md:px-8">
      <section className="mx-auto flex min-h-[calc(100svh-6.5rem)] max-w-3xl flex-col justify-center gap-8 py-8">
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
          <p className="mt-3 max-w-xl text-base text-[#655c51] dark:text-[#c9c1b4]">
            {isQueued ? 'Waiting for another player...' : 'Queue for the next available opponent.'}
          </p>
        </div>

        <div className="rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-6 shadow-sm dark:border-white/10 dark:bg-white/[0.04]">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm font-black uppercase tracking-[0.14em] text-[#7c735f] dark:text-[#b8b09e]">
                Automatic matchmaking
              </p>
              <p className="mt-2 text-sm text-[#655c51] dark:text-[#c9c1b4]">
                {isQueued
                  ? 'Keep this page open while the next opponent is assigned.'
                  : 'Start a search and the first available opponent will be assigned automatically.'}
              </p>
            </div>
            <div className="flex flex-col gap-2 sm:min-w-44">
              <Button
                type="button"
                className="bg-[#8f2f24] text-white hover:bg-[#76261d]"
                disabled={isSearching || cancelFindMatch.isPending}
                onClick={startSearch}
              >
                {isSearching ? (
                  <LoaderCircle className="mr-2 size-4 animate-spin" />
                ) : (
                  <Search className="mr-2 size-4" />
                )}
                Find match
              </Button>
              {isQueued ? (
                <Button
                  type="button"
                  variant="outline"
                  className="border-[#8a7b62]/35 bg-white/70"
                  disabled={cancelFindMatch.isPending}
                  onClick={cancelSearch}
                >
                  <X className="mr-2 size-4" />
                  Cancel search
                </Button>
              ) : null}
            </div>
          </div>
        </div>
      </section>
    </main>
  );
}

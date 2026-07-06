# Frontend Ticket 03: Optional Realtime Redirect And Verification

## Goal

Verify the queue flow end to end and optionally replace polling-only redirect with a STOMP user-queue event.

## Files

- Optional modify: `apps/frontend/src/features/lobby/api/match-socket.hook.ts`
- Optional create: `apps/frontend/src/features/lobby/api/matchmaking-socket.hook.ts`
- Modify as needed: `apps/frontend/src/features/lobby/components/find-match-page-content.tsx`

## Optional Realtime Design

If backend publishes user-specific matchmaking events, subscribe to:

```text
/user/queue/matchmaking
```

Expected event shape:

```ts
{
  type: 'MATCH_FOUND';
  matchId: string;
  match: MatchSummary;
  occurredAt: string;
}
```

When received:

- Set the `lobbyKeys.activeMatch` cache to the event match.
- Redirect to `/matches/{matchId}`.
- Do not send cancel-on-unmount during redirect.

Polling remains acceptable for the first implementation if realtime user events are not added.

## Verification

Run from the workspace root or frontend package:

```bash
pnpm --filter @app/frontend lint
pnpm --filter @app/frontend build
```

Manual browser checks:

- Player A clicks Find Match and sees waiting state.
- Player B clicks Find Match and redirects to the match.
- Player A redirects without manual refresh.
- Player A cancels while waiting and does not get matched afterward.
- Double-clicking Find Match does not create two matches.

## Done

- Frontend build passes.
- Manual two-user flow is verified locally.

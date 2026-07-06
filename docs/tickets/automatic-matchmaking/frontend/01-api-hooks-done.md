# Frontend Ticket 01: Matchmaking API Hooks

## Goal

Expose typed TanStack Query hooks for starting and cancelling matchmaking.

## Files

- Modify: `apps/frontend/src/features/lobby/api/lobby.hooks.ts`

## Requirements

- Add `MatchmakingStatus = 'ACTIVE' | 'QUEUED' | 'MATCHED'`.
- Add `MatchmakingResponse` with `status`, `match`, and `enqueuedAt`.
- Add `lobbyKeys.matchmaking`.
- Add `useFindMatch()`.
- Add `useCancelFindMatch()`.
- On successful find/cancel, invalidate active match and matchmaking-related state.

## API Calls

```ts
POST / api / v1 / matches / find;
DELETE / api / v1 / matches / find / queue;
```

## Done

- Hooks compile.
- Existing match, create, join, rematch, move, setup hooks remain unchanged except for shared invalidation keys.

# Automatic Matchmaking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace manual public-match browsing/joining with a Find Match action that queues the current player and automatically creates one match for each pair of waiting players.

**Architecture:** Add a durable `matchmaking_queue_entries` table keyed by `user_id`, then pair users inside a backend transaction using row locks. The frontend calls a single matchmaking endpoint, shows a queued state for the first player, redirects when a match exists, and cancels the queue entry when the player leaves the search flow.

**Tech Stack:** Spring Boot 4, Java 21, JPA/Flyway, PostgreSQL row locks, MockMvc, Next.js 15, TanStack Query, optional STOMP user queue notification.

---

## Current State

- `apps/frontend/src/features/lobby/components/find-match-page-content.tsx` currently lists public waiting matches and lets a user manually join one.
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchServiceImpl.java` currently creates public/private waiting matches and joins a selected match by id.
- `MatchServiceImpl.findActiveMatchForUpdate(...)` already prevents a player from intentionally creating or joining another active match when a seat exists in a `WAITING`, `SETUP`, or `PLAYING` match.
- `match_seats` has unique constraints for `(match_id, user_id)` and `(match_id, side)`, but it does not prevent a player from appearing in two different active matches if two transactions race.

## Product Behavior

1. Player opens Find Match and clicks one primary button.
2. Backend checks for an existing active match first.
3. If an active match exists, the API returns it and the frontend redirects to `/matches/{matchId}`.
4. If no opponent is waiting, backend upserts the player into the queue and returns `QUEUED`.
5. If another player is waiting, backend locks both queue rows, creates one match, assigns the earlier waiter to `RED` and the new player to `BLUE`, starts setup, marks both queue rows `MATCHED`, and returns `MATCHED` with the match.
6. The waiting player is redirected by polling `GET /api/v1/matches/active` or, if implemented, by a `/user/queue/matchmaking` websocket event.
7. Leaving the Find Match page or pressing Cancel removes the player's `WAITING` queue entry.

## Backend Design

Create a `matchmaking_queue_entries` table:

- `id UUID PRIMARY KEY`
- `user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE`
- `status VARCHAR(24) NOT NULL` with values `WAITING`, `MATCHED`, `CANCELLED`
- `match_id UUID REFERENCES game_matches(id) ON DELETE SET NULL`
- `enqueued_at TIMESTAMPTZ NOT NULL`
- audit columns matching existing entities

Add `MatchmakingQueueEntry`, `MatchmakingQueueStatus`, repository methods with pessimistic locks, request/response DTOs, and a `MatchmakingService`.

Preferred API:

- `POST /api/v1/matches/find`
  - Response: `{ status: "ACTIVE" | "QUEUED" | "MATCHED", match: MatchResponse | null, enqueuedAt: Instant | null }`
- `DELETE /api/v1/matches/find/queue`
  - Cancels only a `WAITING` queue entry for the current user.

Concurrency rules:

- Resolve the internal user id from `@CurrentUser`; never accept user ids from the client.
- Lock the current user's queue row when it exists.
- Call existing active-match lookup under lock before queueing.
- Lock the oldest other `WAITING` queue entry with `FOR UPDATE SKIP LOCKED` before pairing.
- Save both seats in the same transaction as the match creation and queue status updates.
- Return the active match instead of creating a duplicate if the user already has a `WAITING`, `SETUP`, or `PLAYING` seat.

## Frontend Design

Replace the table-driven Find Match screen with a focused queue experience:

- Primary button: `Find match`.
- Pending state after click: disable the button, show waiting status, show a Cancel button.
- On `MATCHED` or `ACTIVE`, redirect to `/matches/{match.id}`.
- While queued, poll `useActiveMatch()` every 1-2 seconds on this page.
- Cancel the queue on explicit Cancel and on unmount if the player is still queued.
- Keep manual invite/private match flows intact. Existing Create Match and Invite Match behavior remains separate from automatic matchmaking.

## Ticket Order

1. Backend schema and entity contract.
2. Backend service and controller behavior.
3. Backend integration and concurrency tests.
4. Frontend API hooks and types.
5. Frontend Find Match page replacement.
6. End-to-end verification.

## Acceptance Criteria

- Two players clicking Find Match are assigned to one new match with exactly two seats.
- A player cannot get two active matches from repeated clicks, double-clicks, refreshes, or concurrent requests.
- A queued player can cancel before being matched.
- A player with an existing active match is redirected to it instead of being queued.
- The first queued player is eventually redirected after a second player arrives.
- Public match browsing is no longer required for normal Find Match use.

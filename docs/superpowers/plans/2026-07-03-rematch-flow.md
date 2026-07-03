# Rematch Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the game-over rematch dialog with inline game-over actions, backed by synchronized rematch request/accept behavior.

**Architecture:** Store pending rematches as a new match linked to the completed source match. The original match response exposes the pending rematch for each viewer, and websocket events redirect both players once the rematch is accepted.

**Tech Stack:** Spring Boot 4, JPA/Flyway, MockMvc, Next.js 15, TanStack Query, STOMP websocket.

---

### Task 1: Backend Rematch Contract

**Files:**

- Modify: `apps/backend/src/test/java/com/vencentdev/backend/match/controller/MatchControllerIntegrationTest.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/entity/GameMatch.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/dto/lobby/MatchResponse.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/dto/lobby/MatchRealtimeEvent.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/repository/lobby/GameMatchRepository.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchRealtimeService.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchService.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/service/MatchServiceImpl.java`
- Modify: `apps/backend/src/main/java/com/vencentdev/backend/match/controller/MatchController.java`
- Create: `apps/backend/src/main/resources/db/migration/V10__match_rematches.sql`

- [ ] Write integration tests for requesting and accepting a rematch.
- [ ] Run the focused integration test and confirm it fails because endpoints/fields do not exist.
- [ ] Add rematch columns, response fields, repository lookup, service methods, and controller endpoints.
- [ ] Run the focused integration test and confirm it passes.

### Task 2: Frontend Match Room Flow

**Files:**

- Modify: `apps/frontend/src/features/lobby/api/lobby.hooks.ts`
- Modify: `apps/frontend/src/features/lobby/api/match-socket.hook.ts`
- Modify: `apps/frontend/src/features/lobby/components/match-room-page-content.tsx`

- [ ] Add rematch response fields and request/accept hooks.
- [ ] Remove the automatic game-over dialog and hide only chat input fields at game over.
- [ ] Add inline New Match, Rematch, Accept Match, and waiting states in the chat section.
- [ ] Redirect both players when a `REMATCH_ACCEPTED` websocket event includes the rematch match id.

### Task 3: Verification

- [ ] Run backend focused tests for the match controller.
- [ ] Run frontend typecheck.
- [ ] Run git diff review for accidental unrelated changes.

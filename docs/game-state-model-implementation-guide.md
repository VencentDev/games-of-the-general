# Game State Model Implementation Guide

This guide explains the missing live game-state pieces for Games of the General and the order to create them manually.

The existing model already describes the static rules: board size, phases, movement directions, piece types, ranks, and counts. What is still needed is the state of one actual match while two players are setting up, moving, attacking, capturing pieces, and finishing the game.

## Goal

Build a server-owned game state model that can answer these questions:

- Where is every piece right now?
- Which side owns each piece?
- Whose turn is it?
- Which pieces are captured?
- Which ranks can this viewer see?
- What moves are legal?
- What happened in the last move?
- Has the game ended, and why?

## Recommended File Creation Order

Create the model in this order so each layer has something stable to depend on.

### 1. Add Board And Position Value Objects

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/enums/PieceStatus.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/BoardPositionResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/BoardSquareResponse.java`

What this covers:

- Piece position: row/column, captured, or off-board.
- BoardState: the actual 8 x 9 grid shape.

Suggested enum:

```java
public enum PieceStatus {
  UNPLACED,
  ACTIVE,
  CAPTURED
}
```

Guide questions:

- Do I have a way to describe a square without also describing a piece?
- Can I represent an empty square?
- Can I represent a piece that exists but is not placed yet?
- Can I represent a captured piece without pretending it is still on the board?
- If a row or column is invalid, where will I reject it?

Why this matters:

The game cannot safely move or capture pieces until position and status are explicit. If position is only implied by the UI, the backend cannot enforce rules.

### 2. Add PieceInstance

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/entity/MatchPiece.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/repository/MatchPieceRepository.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/PieceInstanceResponse.java`

What this covers:

- Individual piece IDs.
- Piece owner/side.
- Piece type and rank.
- Position/status per piece.

Fields to consider:

- `id`
- `match`
- `side`
- `type`
- `status`
- `row`
- `col`
- `capturedByMoveNumber`
- `createdAt`
- `updatedAt`

Guide questions:

- If Red has 6 Privates, how do I tell Private #1 from Private #6?
- If two Spies have the same type, can I still move only the selected Spy?
- Can I query all active pieces for one match?
- Can I query captured pieces separately from active pieces?
- Can two pieces accidentally occupy the same square?

Why this matters:

`PieceType` describes a kind of piece. `MatchPiece` describes one real piece inside one match. Duplicate pieces make this necessary.

### 3. Add Match Game State

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/enums/WinReason.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/enums/DrawReason.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/entity/GameStateSnapshot.java` if using JSON state
- Or migration columns/tables if using relational state

What this covers:

- Current turn.
- Move number.
- Game phase.
- Winner.
- Win reason enum.
- Resign/draw outcomes.

Preferred fields on `GameMatch` or a related `match_game_states` table:

- `phase`
- `currentTurn`
- `moveNumber`
- `winnerSide`
- `winReason`
- `drawReason`
- `resignedSide`

Suggested win reasons:

```java
public enum WinReason {
  FLAG_CAPTURED,
  FLAG_REACHED_OPPOSITE_END,
  RESIGNATION
}
```

Suggested draw reasons:

```java
public enum DrawReason {
  AGREEMENT
}
```

Guide questions:

- Do I know whose turn it is without reading the last move?
- Do I know whether the match is still in setup or already playing?
- Can I distinguish a flag capture win from a resignation win?
- If the game is over, can I explain why?
- If both players agree to a draw, where is that stored?

Why this matters:

Game state should be directly readable. You should not need to reconstruct critical state by replaying every move each time.

### 4. Add Setup Formation State

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/SetupPieceRequest.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/SetupFormationRequest.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/SetupFormationResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/SetupService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/SetupServiceImpl.java`

What this covers:

- Setup formations.
- Placed and unplaced pieces.
- Ready/setup-complete state.
- Transition from `SETUP` to `PLAYING`.

Rules to enforce:

- Each side can place only its own pieces.
- Pieces must be inside that side's first 3 rows.
- A side cannot ready up until all 21 pieces are placed.
- Both sides must be ready before the match starts.
- Opponent ranks must remain hidden during setup.

Guide questions:

- Can a player place a piece outside their setup zone?
- Can a player place the opponent's piece?
- Can a player mark ready with only 20 pieces placed?
- Can two pieces be placed on the same square?
- When both players are ready, who gets the first turn?

Why this matters:

Setup is part of the game, not just a frontend form. If setup is not validated on the server, players can create impossible boards.

### 5. Add Viewer-Specific Piece Projection

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/VisiblePieceResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/GameStateResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/GameStateProjectionService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/GameStateProjectionServiceImpl.java`

What this covers:

- Visibility projection.
- Viewer-specific game-state endpoint response.
- Hidden opponent ranks.

Response behavior:

- Own pieces: include `type`, `label`, `rank`, and abbreviation.
- Opponent active pieces: hide `type`, `label`, `rank`, and abbreviation unless the selected mode allows reveal.
- Empty squares: return no piece.
- Captured pieces: decide whether to reveal based on match mode.

Guide questions:

- If Blue requests the board, can Blue see Red's hidden ranks?
- If Red requests the board, can Red see Red's own ranks?
- Does the server hide data, or is the frontend just choosing not to display it?
- Are captured pieces visible, hidden, or mode-dependent?
- Can a spectator see both sides, neither side, or a special projection?

Why this matters:

Hidden information must be protected by the backend response. If the API sends the true opponent rank, the frontend cannot be trusted to hide it.

### 6. Add Captured Pieces Model

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/CapturedPieceResponse.java`
- Optional: `apps/backend/src/main/java/com/vencentdev/backend/match/entity/CapturedPiece.java`

What this covers:

- Captured pieces list per side.
- Capture metadata.

You can derive captured pieces from `MatchPiece.status = CAPTURED`, or create a separate capture table if you need richer history.

Fields to consider:

- `pieceId`
- `side`
- `type`
- `capturedBySide`
- `capturedOnMoveNumber`
- `capturedAt`

Guide questions:

- Can I show which pieces Red has lost?
- Can I show which pieces Blue has captured?
- Do I need capture order?
- Do I need to hide captured piece ranks from the opponent?
- Can the same piece be captured twice?

Why this matters:

Captured pieces are not just UI decoration. They are part of match state and often needed for history, replay, and game-over summaries.

### 7. Add Legal Move Calculation

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/LegalMoveResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/LegalMoveService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/LegalMoveServiceImpl.java`

What this covers:

- Legal move calculation.
- Orthogonal movement.
- Turn enforcement.
- Own-piece blocking.
- Enemy-square attack targets.

Rules to enforce:

- A player can move only during their turn.
- A piece can move one square only.
- Movement is up, down, left, or right.
- Diagonal movement is illegal.
- Moving outside the board is illegal.
- Moving onto an own piece is illegal.
- Moving onto an enemy piece is a challenge.

Guide questions:

- Can this piece move if it is not this player's turn?
- Is the target square on the board?
- Is the target exactly one orthogonal square away?
- Is the target occupied by my own piece?
- Is the target empty or occupied by an enemy?
- Does the Flag have special movement in the ruleset I selected?

Why this matters:

Legal moves should come from the game engine, not from duplicated frontend logic. The frontend can highlight moves, but the backend must enforce them.

### 8. Add Battle Resolver

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/service/BattleResolver.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/BattleResolution.java`
- Update existing `BattleResult.java` if needed.
- Tests under `apps/backend/src/test/java/com/vencentdev/backend/match/service/BattleResolverTest.java`

What this covers:

- Battle resolver function/table for every matchup.
- Flag capture.
- Equal-rank elimination.
- Spy and Private exceptions.

Suggested battle result values:

- `ATTACKER_WINS`
- `DEFENDER_WINS`
- `BOTH_ELIMINATED`
- `FLAG_CAPTURED`
- `INVALID`

Guide questions:

- What happens when a Five-Star General attacks a Private?
- What happens when a Private attacks a Spy?
- What happens when a Spy attacks a Private?
- What happens when equal ranks challenge each other?
- What happens when a Flag attacks the opposing Flag?
- Does the resolver return a result without mutating the board?

Why this matters:

Battle rules are the most error-prone part of the game. A pure resolver is easier to test than one that directly changes database rows.

### 9. Add Move Application Service

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/MoveRequest.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/MoveResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/MoveService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/MoveServiceImpl.java`

What this covers:

- Applying a legal move.
- Updating board state.
- Resolving challenges.
- Capturing pieces.
- Switching turns.
- Incrementing move number.
- Writing move history.

Move flow:

1. Load match.
2. Confirm phase is `PLAYING`.
3. Confirm acting user controls `currentTurn`.
4. Load the selected piece.
5. Validate the requested move.
6. If target is empty, move the piece.
7. If target has enemy piece, resolve battle.
8. Update active/captured pieces.
9. Check win conditions.
10. Save `MatchMove`.
11. Increment `moveNumber`.
12. Switch turn unless the game is over.

Guide questions:

- Does this service reject moves during setup?
- Does it reject moves from the wrong player?
- Does it call the legal move service before changing state?
- Does it call the battle resolver before removing pieces?
- Does it save enough history to replay what happened?
- Does it avoid revealing hidden ranks in the response?

Why this matters:

This is the command path for the game. It should be the only place that changes live board state during play.

### 10. Expand Move History

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/dto/MoveHistoryResponse.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/repository/MatchMoveRepository.java`
- Optional updates to `MatchMove.java`

What this covers:

- Full move history DTO/API.
- Battle result history.
- Final move reference.

Fields to consider adding to `MatchMove`:

- `pieceId`
- `pieceType`
- `targetPieceId`
- `targetPieceType`
- `attackerOutcome`
- `defenderOutcome`
- `resultingPhase`

Guide questions:

- Can I list every move in order?
- Can I explain whether a move was a plain move or a challenge?
- Can I show the last move after game over?
- Do I need hidden or viewer-specific history?
- If a battle happened, can I show only what the viewer is allowed to know?

Why this matters:

History is needed for replay, debugging, game summaries, reconnects, and player trust.

### 11. Add Win Condition Service

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/service/WinConditionService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/WinConditionServiceImpl.java`

What this covers:

- Flag capture.
- Flag reaching the opposite end.
- Flag end-row safety rule.
- Resignation.
- Draw agreement.

Guide questions:

- Did a Flag get captured?
- Did a Flag reach the opposite end?
- Does the selected ruleset require the end-row safety check?
- Did a player resign?
- Did both players agree to a draw?
- Should the game immediately become `GAME_OVER`?

Why this matters:

Win checks should be centralized. If every service decides game-over differently, rule bugs will appear quickly.

### 12. Add Persistence Migration

Suggested files:

- `apps/backend/src/main/resources/db/migration/V5__game_state.sql`

What this covers:

- Match/game state persistence table, or JSON state column.
- Piece instances.
- Board position constraints.
- Captured/off-board status.

Relational approach:

- Add `phase`, `current_turn`, and `move_number` to `game_matches`, or create `match_game_states`.
- Add `match_pieces` table for all piece instances.
- Keep `match_moves` as history.

JSON approach:

- Add a `state_json` column to `game_matches`.
- Store board, pieces, captures, current turn, and move number inside JSON.
- Keep `match_moves` as history.

Recommended for this app:

Use a relational `match_pieces` table plus explicit match state columns. It is easier to query, validate, and test with JPA.

Guide questions:

- Do I need to query pieces by match and side?
- Do I need database constraints for board bounds?
- Can two active pieces occupy the same square?
- Can I restore the full board after a server restart?
- Is the move history separate from current state?

Why this matters:

The game must survive refreshes, reconnects, and backend restarts. In-memory state is not enough for online play.

### 13. Add Viewer-Specific Game State Endpoint

Suggested files:

- `apps/backend/src/main/java/com/vencentdev/backend/match/controller/GameStateController.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/GameStateService.java`
- `apps/backend/src/main/java/com/vencentdev/backend/match/service/GameStateServiceImpl.java`

Suggested endpoint:

```text
GET /api/v1/matches/{matchId}/state
```

What this covers:

- Viewer-specific game-state endpoint.
- Board state response.
- Current turn.
- Captured pieces.
- Move number.
- Winner and win reason.

Keep this separate from:

```text
GET /api/v1/game-model
```

`/game-model` should describe static rules. `/matches/{matchId}/state` should describe one live match.

Guide questions:

- Is this response for one specific match?
- Does the response depend on who is asking?
- Does it hide opponent ranks?
- Does it include current turn and phase?
- Does it include captured pieces?
- Does it include enough board data for the frontend to render?

Why this matters:

The frontend needs one endpoint that gives the current playable match state without leaking hidden information.

## Suggested Implementation Milestones

### Milestone 1: Read-Only State

Build:

- `MatchPiece`
- `PieceStatus`
- Database migration
- Basic game-state response
- Viewer-specific projection

Done when:

- You can create all 42 pieces for a match.
- You can return an empty or setup board.
- Own ranks are visible.
- Opponent ranks are hidden.

### Milestone 2: Setup Phase

Build:

- Setup placement API.
- Setup ready API.
- Setup validation.
- Transition to `PLAYING`.

Done when:

- Each player can place 21 pieces.
- Invalid setup squares are rejected.
- Both players ready starts the game.

### Milestone 3: Movement

Build:

- Legal move service.
- Move request/response.
- Move application service.

Done when:

- Pieces move one orthogonal square.
- Turns switch.
- Illegal moves are rejected.

### Milestone 4: Battle

Build:

- Battle resolver.
- Battle resolver tests.
- Capture state updates.
- Battle move history.

Done when:

- Every matchup resolves correctly.
- Equal ranks remove both pieces.
- Captures update piece status.

### Milestone 5: Game Over

Build:

- Win condition service.
- Flag capture handling.
- Flag end-row handling.
- Resign/draw endpoints if needed.

Done when:

- Capturing a Flag ends the game.
- A Flag reaching the opposite end can end the game.
- Resignation and draw outcomes are represented.

## Final Checklist

- Do I have a real board state, not just static board dimensions?
- Do I have individual piece instances, not only piece types?
- Do I know where each piece is?
- Do I know which side owns each piece?
- Do I hide opponent ranks at the API level?
- Do I know whose turn it is?
- Do I track move number in current state?
- Do I support setup formations?
- Do I know whether each player is ready?
- Do I track captured pieces?
- Do I expose move history?
- Do I calculate legal moves on the backend?
- Do I resolve battles with a tested pure function?
- Do I store win reason as an enum?
- Do I represent resignation and draw?
- Do I handle the Flag end-row rule intentionally?
- Do I persist game state after refresh or restart?
- Do I have a viewer-specific `/matches/{matchId}/state` endpoint?

## Development Advice

Do not start with final board visuals. Start with plain text pieces and JSON responses. Once the backend can truthfully answer where every piece is, whose turn it is, what a viewer may see, and whether a move is legal, the frontend becomes much easier to build.

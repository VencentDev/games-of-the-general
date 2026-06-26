# Games of the General Web App Build Plan

This document outlines a practical build order for creating Games of the General as a web app. The main recommendation is to build the game engine and playable board first, then polish the visuals and assets after the rules are correct.

## Guiding Principle

Start with the game model, not piece artwork.

Assets are easy to replace later. The rules, board state, hidden information, movement, battle resolution, and win conditions are the foundation. If those are unclear or tightly coupled to the UI, the app will become difficult to extend into online play, saved matches, replays, or ranked modes.

## Recommended Build Order

### 1. Define The Game Model

Create the core data structures before designing final visuals.

The model should represent:

- Board size: 8 rows x 9 columns.
- Players: two sides, such as `red` and `blue`.
- Piece ownership.
- Piece type and rank.
- Whether a piece is visible to the current viewer.
- Current turn.
- Game phase.
- Captured pieces.
- Winner, if the match is over.

Suggested game phases:

- `setup`
- `playing`
- `game_over`

Suggested piece types:

- Five-Star General
- Four-Star General
- Three-Star General
- Two-Star General
- One-Star General
- Colonel
- Lt. Colonel
- Major
- Captain
- First Lieutenant
- Second Lieutenant
- Sergeant
- Spy
- Private
- Flag

Each player has 21 total pieces:

- 1 Five-Star General
- 1 Four-Star General
- 1 Three-Star General
- 1 Two-Star General
- 1 One-Star General
- 1 Colonel
- 1 Lt. Colonel
- 1 Major
- 1 Captain
- 1 First Lieutenant
- 1 Second Lieutenant
- 1 Sergeant
- 2 Spies
- 6 Privates
- 1 Flag

### 2. Build A Plain Playable Board

Use text labels before artwork.

Examples:

- `5G`
- `4G`
- `Col`
- `LC`
- `Maj`
- `Capt`
- `1Lt`
- `2Lt`
- `Sgt`
- `Spy`
- `Pvt`
- `F`

This makes it easier to test the actual game without waiting for visual assets.

The first board milestone should include:

- 8 x 9 board rendering.
- Click/select piece.
- Highlight legal moves.
- Move a piece.
- Attack an occupied square.
- Switch turns.

### 3. Implement Setup Phase

Each player places 21 pieces inside their first 3 rows.

Since the setup zone is 3 rows x 9 columns, each player has 27 setup squares. With 21 pieces, 6 setup squares remain empty.

Setup requirements:

- Players can place, remove, and reorder pieces.
- Pieces cannot be placed outside the setup zone.
- A player cannot start until all 21 pieces are placed.
- The opponent cannot see the player's piece ranks.

Useful setup controls:

- Drag and drop piece placement.
- Click-to-place fallback for mobile.
- Reset formation.
- Random formation.
- Save formation preset.

### 4. Implement Movement Rules

Start with the standard adjacent-square movement.

Each movable piece should be able to move one square:

- Up
- Down
- Left
- Right

Important checks:

- A piece cannot move outside the board.
- A piece cannot move onto a square occupied by its own side.
- A piece may move into an enemy-occupied square to attack.
- A player can only move during their turn.
- The Flag may have special movement handling depending on the exact ruleset you choose to enforce.

### 5. Implement Battle Resolution

This is the most important rule system.

General combat rules:

- Higher-ranking officers eliminate lower-ranking officers.
- Officers eliminate Privates and Flags.
- Sergeant eliminates Privates and Flags.
- Spy eliminates officers from Sergeant up to Five-Star General, and can eliminate the Flag.
- Private eliminates Spy and Flag.
- The Flag can be eliminated by any opposing piece, including the opposing Flag.
- If both pieces have equal rank, both are eliminated.

Recommended implementation approach:

- Create a pure function such as `resolveBattle(attacker, defender)`.
- Return a structured result instead of directly mutating UI state.
- Cover every piece matchup with tests.

Example battle result types:

- `attacker_wins`
- `defender_wins`
- `both_eliminated`
- `flag_captured`
- `invalid`

### 6. Implement Win Conditions

A player wins when:

- They capture or eliminate the opponent's Flag.
- They successfully move their own Flag to the opposite end of the board.

The app should immediately transition to `game_over` when a win condition is met.

Store:

- Winner.
- Win reason.
- Final move.
- Captured pieces.

Example win reasons:

- `captured_flag`
- `flag_reached_opposite_end`

### 7. Add Hidden Information

Games of the General depends on hidden ranks.

Viewer rules:

- A player can see their own piece ranks.
- A player should see opponent pieces as unknown pieces.
- Captured pieces may be shown in a captured list.
- Revealed battle results can be shown in move history if desired.

Opponent display examples:

- `?`
- Back-facing token
- Generic enemy badge

Do not build hidden information only as a visual trick. The underlying game state sent to each player should only include what that player is allowed to know, especially once online multiplayer is added.

### 8. Add Match State And History

Useful match data:

- Match ID.
- Player IDs.
- Board state.
- Current turn.
- Move number.
- Captured pieces.
- Move history.
- Started time.
- Finished time.
- Winner.

Move history should record:

- From square.
- To square.
- Acting player.
- Whether combat occurred.
- Battle result.
- Captured piece, if visible.
- Win condition, if triggered.

### 9. Add Visual Assets

Only create polished assets after the text-label version is playable.

Recommended asset format:

- Use SVG for rank tokens, badges, and board pieces.
- Use PNG or WebP only for illustrated backgrounds, splash art, or textured boards.

Why SVG is better for pieces:

- Scales cleanly on desktop and mobile.
- Easy to recolor for each player.
- Smaller and sharper than many PNGs.
- Works well for rank badges and military-style symbols.

Suggested visual system:

- One token shape for all pieces.
- Player color border or base.
- Rank abbreviation on the token.
- Optional rank icon or stars.
- Separate back-facing token for hidden enemy pieces.

Do not block gameplay development on final art. Start with clean text tokens and replace them later.

### 10. Add User Accounts And Saved Matches

Once the local playable game works, connect it to accounts.

Account features:

- Login.
- Signup.
- Player profile.
- Saved match history.
- Win/loss record.
- Saved formations.

Avoid making authentication the center of early development. It should support the game, not delay the game engine.

### 11. Add Online Multiplayer

After the local version is stable, add real multiplayer.

Recommended multiplayer requirements:

- Create match.
- Join match.
- Private match invite.
- Real-time board updates.
- Server-authoritative move validation.
- Per-player hidden state.
- Reconnect support.
- Surrender/resign.
- Draw or timeout handling, if desired.

For online play, never trust the client to decide legal moves or battle results. The server should validate moves and resolve combat.

### 12. Add Polish Features

Add these after the core loop works:

- Timers.
- Ranked/unranked modes.
- Matchmaking.
- Spectator mode.
- Replay viewer.
- Formation presets.
- Tutorial.
- Rules reference.
- Sound effects.
- Animations.
- Mobile gestures.

## Suggested Technical Milestones

### Milestone 1: Local Rules Engine

Goal: The game can be simulated without UI.

Deliverables:

- Piece definitions.
- Board state.
- Setup validation.
- Move validation.
- Battle resolution.
- Win-condition detection.
- Unit tests for piece matchups.

### Milestone 2: Local Playable UI

Goal: Two players can play on one browser.

Deliverables:

- 8 x 9 board.
- Text-based piece tokens.
- Setup flow.
- Turn flow.
- Legal move highlights.
- Captured piece list.
- Game-over screen.

### Milestone 3: Better UX

Goal: The game feels understandable and comfortable.

Deliverables:

- Drag and drop or tap-to-place setup.
- Move confirmation.
- Battle result display.
- Move history.
- Responsive mobile layout.
- Rules help panel.

### Milestone 4: Accounts And Persistence

Goal: Players can save identity and history.

Deliverables:

- Login/signup.
- Player profile.
- Saved formations.
- Stored match results.
- Basic stats.

### Milestone 5: Online Multiplayer

Goal: Two users can play from different devices.

Deliverables:

- Match creation.
- Match joining.
- Server-side validation.
- Real-time state sync.
- Hidden information per player.
- Reconnect handling.

### Milestone 6: Visual Asset Upgrade

Goal: Replace text tokens with polished assets.

Deliverables:

- SVG piece set.
- Board styling.
- Hidden enemy token.
- Player color themes.
- Responsive animations.

## Testing Priorities

Test the rule engine before testing visuals.

High-priority tests:

- Board dimensions are exactly 8 x 9.
- Setup allows only first 3 rows per player.
- Setup requires exactly 21 pieces.
- Setup leaves 6 empty squares per player.
- Movement rejects out-of-bounds moves.
- Movement rejects moving onto friendly pieces.
- Combat resolves every rank matchup correctly.
- Equal ranks eliminate both pieces.
- Flag capture ends the game.
- Flag reaching the opposite end ends the game.
- Hidden opponent ranks are not exposed to the wrong player.

## First Thing To Build

Build a local, text-token version of the game.

The first target should be:

- One page.
- 8 x 9 board.
- Both players can arrange pieces.
- Turns work.
- Combat works.
- Win conditions work.
- No final art required.

Once that works, the rest of the app becomes much easier to build safely.

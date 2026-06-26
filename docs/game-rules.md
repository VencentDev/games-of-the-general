# Games of the General Rules

## Object Of The Game

The objective of the game is to eliminate or capture the Flag of your opponent.

You may also win by successfully maneuvering your own Flag to the opposite end of the board.

## The Pieces

Each player has a set of 21 pieces, also called soldiers. Each piece has a rank and a function.

| Piece              | Number Of Pieces | Function                                                                                                                                                                                    |
| ------------------ | ---------------: | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Five-Star General  |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Four-Star General  |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Three-Star General |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Two-Star General   |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| One-Star General   |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Colonel            |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Lt. Colonel        |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Major              |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Captain            |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| First Lieutenant   |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Second Lieutenant  |                1 | Eliminates any lower-ranking officer, the Private, and the Flag.                                                                                                                            |
| Sergeant           |                1 | Eliminates the Private and the Flag.                                                                                                                                                        |
| Spy                |                2 | Eliminates all officers from Sergeant up to Five-Star General, and the Flag.                                                                                                                |
| Private            |                6 | Eliminates the Spy and the Flag.                                                                                                                                                            |
| Flag               |                1 | Can be eliminated by any piece, including the opposing Flag. A Flag eliminates the opposing Flag when it takes aggressive action by moving into the same square occupied by the other Flag. |

Note: If both soldiers are of equal rank, both are eliminated.

## Preparing For Battle

Spread out the board as illustrated in the official game reference.

Arrange your respective sets of pieces on the first three rows on your end of the board, with the printed sides facing you.

There is no predetermined place for any piece. You are free to arrange the pieces according to your strategy and style of play.

As you arrange your pieces on the first three rows, there will be six vacant squares. These empty squares allow for maneuvering and freedom of movement when play begins.

## Movement

1. Any player makes the first move. Players move alternately.
2. A player is allowed to move only one piece at a time.
3. A move consists of pushing a piece to an adjacent square, either forward, backward, or sideward.
4. A diagonal move is illegal.
5. A move of more than one square is illegal.

## Challenging

As the game progresses, challenges are made, resulting in the elimination of soldiers.

A challenge is made when a soldier moves into the same square occupied by an opposing soldier.

When a challenge is made, the following rules of elimination apply:

1. A higher-ranking soldier eliminates a lower-ranking soldier from the board.
2. If both soldiers are of equal rank, both are eliminated.
3. A Spy eliminates any officer, starting with the rank of Five-Star General down to Sergeant.
4. The Flag can be eliminated or captured by any piece, including the opponent's Flag.
5. Only a Private can eliminate the Spy.
6. The Flag that moves into the same square occupied by the other Flag wins the game.

## Arbiter Rules

For maximum interest and suspense, a neutral party, called an arbiter, may be present to preside over challenges for both players.

The arbiter is not allowed to reveal to either player the ranks of any piece, whether engaged in a challenge or not.

In case of a challenge, the arbiter quietly removes the outranked piece and gives it back to the player who has lost it. Care must be taken so that the eliminated piece is not shown to the opponent.

Official tournament games are conducted with an arbiter.

## Playing Without An Arbiter

When playing without an arbiter, every time there is a challenge, both players must declare the ranks of the two opposing pieces concerned.

After the ranks are declared, the outranked player removes their piece from the board.

## How The Game Ends

The game ends:

1. When the Flag is eliminated or captured.
2. When a Flag reaches the opposite end of the board.
3. When a player resigns.
4. When both players agree on a drawn position.

## Flag End-Row Rule

A Flag reaching the opposite end of the board may still be eliminated by an opposing piece occupying a square adjacent to the one reached by the Flag.

In order to win, the Flag should be at least two squares, or two moves, ahead of any opposing piece.

## Implementation Notes For The Web App

The official rules above should be treated as the source of truth for the game engine.

Important implementation details:

- The board should have 8 rows and 9 columns.
- Each player places 21 pieces in their first 3 rows.
- Each player has 27 setup squares, leaving 6 vacant squares.
- Movement is orthogonal only: forward, backward, left, or right.
- Diagonal movement is not allowed.
- Moving more than one square is not allowed.
- Challenges occur when a piece moves into a square occupied by an opposing piece.
- Opponent piece ranks should remain hidden unless the selected game mode intentionally reveals them.
- In digital play without a human arbiter, the server should act as the arbiter.
- The server should resolve challenges without exposing hidden ranks unnecessarily.
- Equal-rank challenges remove both pieces.
- Capturing or eliminating the Flag ends the game unless the selected Flag end-row rule requires an additional safety check.

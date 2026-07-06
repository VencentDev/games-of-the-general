# Frontend Ticket 02: Replace Manual Find Match With Queue UX

## Goal

Make Find Match a one-click automatic matchmaking page instead of a public match browser.

## Files

- Modify: `apps/frontend/src/features/lobby/components/find-match-page-content.tsx`

## Requirements

- Remove public match listing, preparation-time filter, refresh button, and manual Join buttons from this page.
- Show a primary `Find match` button.
- After clicking, show a waiting state and a `Cancel` button.
- Disable repeated clicks while the request is pending or queued.
- Redirect immediately when `useFindMatch()` returns a response with `match.id`.
- Poll `useActiveMatch()` every 1-2 seconds while queued and redirect when it returns a match.
- On explicit Cancel, call `useCancelFindMatch()`, clear the queued UI state, and stay on the Find Match page.
- On unmount while still queued, call cancel unless the page is redirecting to a match.

## UX Copy

- Initial heading: `Find match`
- Initial supporting text: `Queue for the next available opponent.`
- Button: `Find match`
- Queued text: `Waiting for another player...`
- Cancel button: `Cancel search`

## Done

- User no longer needs to pick a public match.
- First waiting player sees a stable queued state.
- Second player redirects immediately to the created match.
- First player redirects once active-match polling sees the created match.

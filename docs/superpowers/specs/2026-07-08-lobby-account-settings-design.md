# Lobby Account Settings Design

## Goal

Extend the existing lobby Settings dialog so players can open an Account mini page, edit their display name, review their email, and sign out from that focused account view.

## User Experience

The lobby Settings dialog remains the only entry point. Its main Settings view keeps the current appearance, lobby defaults, and theme control. The Account row is now clickable and no longer contains a Sign out button. Activating the Account row switches the dialog content to a focused account view with a back action.

The Account mini page shows a profile image when Auth.js has one. If no image is available, it shows a simple initials avatar derived from the display name, session name, or email. The email is read-only. The display name field is editable and saved with a button. The Sign out button is shown only on this account page and uses a red destructive treatment.

## Data Flow

The account view reads the current user from the existing `/api/v1/users/me` query and saves changes through the existing `PATCH /api/v1/users/me` backend endpoint. The frontend sends only `{ "displayName": "<trimmed value>" }`.

On save success, the `me` query cache is updated with the response so the dialog reflects the latest value without waiting for another fetch. On failure, the account view shows an inline error message and leaves the typed value intact.

## Constraints

- Do not add a new route.
- Do not add a new backend endpoint.
- Keep email read-only.
- Keep Sign out only inside the account mini page.
- Keep the display name max length aligned with the backend limit of 120 characters.
- Use existing UI primitives and styling conventions.

## Verification

Frontend tests are not configured in this project. Verification should use TypeScript checking and, if possible, a frontend build.

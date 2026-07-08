# Lobby Account Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the lobby Settings dialog with a clickable Account mini page that edits display name and keeps sign out there.

**Architecture:** Reuse the existing lobby dialog state and add a lightweight dialog sub-view. Add a `useUpdateMe` mutation beside the existing `useMe` query so account writes remain in the auth session API layer.

**Tech Stack:** Next.js App Router, React client components, TanStack Query, Auth.js, Spring Boot `/api/v1/users/me`, existing shadcn-style UI primitives.

## Global Constraints

- Do not add a new route.
- Do not add a new backend endpoint.
- Keep email read-only.
- Keep Sign out only inside the account mini page.
- Keep the display name max length aligned with the backend limit of 120 characters.
- Use existing UI primitives and styling conventions.
- Frontend tests are not configured; verify with `pnpm --filter @app/frontend typecheck` and build when practical.

---

### Task 1: Add Account Mutation Hook

**Files:**

- Modify: `apps/frontend/src/features/auth/session/api/me.hooks.ts`

**Interfaces:**

- Consumes: `clientApi`, `qk.me()`, Auth.js session access token.
- Produces: `useUpdateMe()` mutation accepting `{ displayName: string | null }` and returning the `/api/v1/users/me` response type.

- [ ] **Step 1: Define mutation input and hook**

Add `useMutation` and `useQueryClient` imports from TanStack Query. Export `UpdateMeInput` and `useUpdateMe()`. The mutation should call:

```ts
clientApi<Me>(session?.accessToken, '/api/v1/users/me', {
  method: 'PATCH',
  body: JSON.stringify(input),
});
```

On success, write the returned user to `qk.me()`.

- [ ] **Step 2: Verify TypeScript**

Run: `pnpm --filter @app/frontend typecheck`

Expected: either passes or reports only pre-existing issues unrelated to this change.

### Task 2: Add Account Sub-View To Settings Dialog

**Files:**

- Modify: `apps/frontend/src/features/lobby/components/lobby-page-content.tsx`

**Interfaces:**

- Consumes: `useMe()`, `useUpdateMe()`, Auth.js `useSession()`, `SignOutButton`.
- Produces: A Settings dialog with `settings` and `account` views.

- [ ] **Step 1: Add dialog view state**

In `SettingsDialog`, add local view state:

```ts
const [view, setView] = useState<'settings' | 'account'>('settings');
```

Reset `view` to `settings` when the dialog closes.

- [ ] **Step 2: Make Account row clickable**

Replace the current Account row with a button-styled row that calls `setView('account')`. Remove Sign out from the settings overview.

- [ ] **Step 3: Add account mini page**

Render profile avatar, read-only email, editable display name, save button, inline success/error feedback, and a red Sign out button. Use `PATCH /api/v1/users/me` through `useUpdateMe()`.

- [ ] **Step 4: Verify TypeScript**

Run: `pnpm --filter @app/frontend typecheck`

Expected: either passes or reports only pre-existing issues unrelated to this change.

### Task 3: Final Verification

**Files:**

- Check: `apps/frontend/src/features/auth/session/api/me.hooks.ts`
- Check: `apps/frontend/src/features/lobby/components/lobby-page-content.tsx`

- [ ] **Step 1: Run frontend typecheck**

Run: `pnpm --filter @app/frontend typecheck`

Expected: exit code 0.

- [ ] **Step 2: Run frontend build**

Run: `pnpm --filter @app/frontend build`

Expected: exit code 0.

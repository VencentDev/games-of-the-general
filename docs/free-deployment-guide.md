# Free Deployment Guide: Vercel + Render + Neon

This guide deploys the project with a free-friendly split:

```text
Browser
  -> Vercel: apps/frontend Next.js app
  -> Render: apps/backend Spring Boot API
  -> Neon: hosted PostgreSQL database
```

Use this for demos, school projects, and personal testing. It is not a production hosting plan: free backend services can sleep, cold starts can be slow, and free database limits can change.

## What Goes Where

| Project part | Folder          | Host                    |
| ------------ | --------------- | ----------------------- |
| Frontend     | `apps/frontend` | Vercel Hobby            |
| Backend API  | `apps/backend`  | Render free web service |
| Database     | PostgreSQL      | Neon free project       |

Vercel is only for the Next.js frontend here. The Spring Boot API should not be deployed to Vercel as-is because it is a long-running Java service, not a Vercel serverless backend.

## Prerequisites

1. Push this repository to GitHub.
2. Create free accounts for:
   - Vercel: <https://vercel.com>
   - Render: <https://render.com>
   - Neon: <https://neon.com>
3. Have your OAuth providers ready if login is enabled:
   - Google OAuth app
   - GitHub OAuth app

For local reference, the app's OAuth setup is documented in [`docs/google-github-oauth-setup.md`](google-github-oauth-setup.md).

## Step 1: Create the Neon Database

1. Open Neon and create a new project.
2. Create or use the default database.
3. Copy the connection string from Neon.
4. Keep these values ready:

```text
Database host
Database name
Database user
Database password
```

For Spring Boot JDBC, the URL should look like this:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_NEON_USER
SPRING_DATASOURCE_PASSWORD=YOUR_NEON_PASSWORD
```

Neon connection strings often include a host, user, password, database name, and SSL requirement. Keep `sslmode=require`.

## Step 2: Deploy the Backend to Render

1. Open Render.
2. Choose **New** -> **Web Service**.
3. Connect the GitHub repository.
4. Choose the repository for this project.
5. Configure the service:

```text
Name: games-of-the-general-backend
Runtime: Docker
Root Directory: apps/backend
Dockerfile Path: Dockerfile
Instance Type: Free
```

Render should build from [`apps/backend/Dockerfile`](../apps/backend/Dockerfile), which builds the Spring Boot jar using Maven and runs it with Java 21.

Add these Render environment variables:

```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_NEON_USER
SPRING_DATASOURCE_PASSWORD=YOUR_NEON_PASSWORD
APP_CORS_ALLOWED_ORIGINS=https://YOUR_VERCEL_APP.vercel.app
APP_RATE_LIMIT_AUTH_ENABLED=true
APP_RATE_LIMIT_AUTH_CAPACITY=60
APP_RATE_LIMIT_AUTH_REFILL_PERIOD=1m
```

At this point you may not know the final Vercel URL yet. Use a temporary value such as:

```env
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Then come back and replace it after Vercel gives you the frontend URL.

Deploy the service. After it finishes, copy the Render public URL:

```text
https://YOUR_BACKEND.onrender.com
```

## Step 3: Deploy the Frontend to Vercel

1. Open Vercel.
2. Choose **Add New** -> **Project**.
3. Import the GitHub repository.
4. Configure the project:

```text
Framework Preset: Next.js
Root Directory: apps/frontend
Install Command: pnpm install
Build Command: pnpm --filter @app/frontend build
Development Command: pnpm --filter @app/frontend dev
```

The frontend already has [`output: 'standalone'`](../apps/frontend/next.config.mjs), so Vercel can build it as a normal Next.js app.

Add these Vercel environment variables:

```env
AUTH_SECRET=GENERATE_A_LONG_RANDOM_SECRET
AUTH_URL=https://YOUR_VERCEL_APP.vercel.app

AUTH_GOOGLE_ID=YOUR_GOOGLE_CLIENT_ID
AUTH_GOOGLE_SECRET=YOUR_GOOGLE_CLIENT_SECRET

AUTH_GITHUB_ID=YOUR_GITHUB_CLIENT_ID
AUTH_GITHUB_SECRET=YOUR_GITHUB_CLIENT_SECRET

NEXT_PUBLIC_API_BASE=https://YOUR_BACKEND.onrender.com
NEXT_PUBLIC_API_BASE_URL=https://YOUR_BACKEND.onrender.com
NEXT_PUBLIC_WS_URL=wss://YOUR_BACKEND.onrender.com/ws
```

Generate `AUTH_SECRET` locally with:

```bash
openssl rand -base64 32
```

Deploy the Vercel project. After deployment, copy the frontend URL:

```text
https://YOUR_VERCEL_APP.vercel.app
```

## Step 4: Update Render CORS

Go back to the Render backend service and update:

```env
APP_CORS_ALLOWED_ORIGINS=https://YOUR_VERCEL_APP.vercel.app
```

Redeploy the backend after changing the variable.

Do not use:

```env
APP_CORS_ALLOWED_ORIGINS=*
```

The backend is configured for credentialed auth flows, so explicit origins are safer.

## Step 5: Update OAuth Callback URLs

If Google login is enabled, add this authorized redirect URI in Google Cloud Console:

```text
https://YOUR_VERCEL_APP.vercel.app/api/auth/callback/google
```

If GitHub login is enabled, add this authorization callback URL in the GitHub OAuth app:

```text
https://YOUR_VERCEL_APP.vercel.app/api/auth/callback/github
```

Also make sure the OAuth app homepage/application URL points to:

```text
https://YOUR_VERCEL_APP.vercel.app
```

## Step 6: Verify the Deployment

Open the frontend:

```text
https://YOUR_VERCEL_APP.vercel.app
```

Check these flows:

1. The page loads without build/runtime errors.
2. Login redirects to Google or GitHub.
3. After login, protected pages load.
4. API-backed screens can read and write data.
5. Lobby or realtime screens reconnect after the Render backend wakes from sleep.

If the first API request is slow, that is expected on Render free services after inactivity.

## Common Problems

### Frontend Cannot Reach Backend

Check Vercel env vars:

```env
NEXT_PUBLIC_API_BASE=https://YOUR_BACKEND.onrender.com
NEXT_PUBLIC_API_BASE_URL=https://YOUR_BACKEND.onrender.com
```

Then redeploy the Vercel project. Next.js public env vars are embedded at build time.

### CORS Error in Browser

Check Render env vars:

```env
APP_CORS_ALLOWED_ORIGINS=https://YOUR_VERCEL_APP.vercel.app
```

The value must exactly match the frontend origin. No trailing slash.

### OAuth Redirect URI Mismatch

Check provider callback URLs:

```text
https://YOUR_VERCEL_APP.vercel.app/api/auth/callback/google
https://YOUR_VERCEL_APP.vercel.app/api/auth/callback/github
```

Also check Vercel:

```env
AUTH_URL=https://YOUR_VERCEL_APP.vercel.app
```

### Database Connection Fails

Check Render:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_NEON_USER
SPRING_DATASOURCE_PASSWORD=YOUR_NEON_PASSWORD
```

Make sure the Neon project is active and the password is copied exactly.

### Websocket Fails

Check Vercel:

```env
NEXT_PUBLIC_WS_URL=wss://YOUR_BACKEND.onrender.com/ws
```

Use `wss://` in production, not `ws://`.

Render free services can sleep. The frontend should reconnect when the backend wakes up, but the first connection may fail or take time.

### Render Build Fails

Confirm the Render service uses:

```text
Runtime: Docker
Root Directory: apps/backend
Dockerfile Path: Dockerfile
```

The backend Docker build expects to run from `apps/backend`, not from the repository root.

### Vercel Build Fails

Confirm the Vercel project uses:

```text
Root Directory: apps/frontend
Install Command: pnpm install
Build Command: pnpm --filter @app/frontend build
```

If Vercel cannot detect pnpm correctly, make sure the repository root contains [`pnpm-lock.yaml`](../pnpm-lock.yaml) and [`pnpm-workspace.yaml`](../pnpm-workspace.yaml).

## Final Environment Checklist

Render backend:

```env
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
SPRING_DATASOURCE_USERNAME=YOUR_NEON_USER
SPRING_DATASOURCE_PASSWORD=YOUR_NEON_PASSWORD
APP_CORS_ALLOWED_ORIGINS=https://YOUR_VERCEL_APP.vercel.app
APP_RATE_LIMIT_AUTH_ENABLED=true
APP_RATE_LIMIT_AUTH_CAPACITY=60
APP_RATE_LIMIT_AUTH_REFILL_PERIOD=1m
```

Vercel frontend:

```env
AUTH_SECRET=GENERATE_A_LONG_RANDOM_SECRET
AUTH_URL=https://YOUR_VERCEL_APP.vercel.app
AUTH_GOOGLE_ID=YOUR_GOOGLE_CLIENT_ID
AUTH_GOOGLE_SECRET=YOUR_GOOGLE_CLIENT_SECRET
AUTH_GITHUB_ID=YOUR_GITHUB_CLIENT_ID
AUTH_GITHUB_SECRET=YOUR_GITHUB_CLIENT_SECRET
NEXT_PUBLIC_API_BASE=https://YOUR_BACKEND.onrender.com
NEXT_PUBLIC_API_BASE_URL=https://YOUR_BACKEND.onrender.com
NEXT_PUBLIC_WS_URL=wss://YOUR_BACKEND.onrender.com/ws
```

## Useful Official Docs

- Vercel GitHub deployments: <https://vercel.com/docs/git/vercel-for-github>
- Vercel monorepos: <https://vercel.com/docs/monorepos>
- Vercel environment variables: <https://vercel.com/docs/environment-variables>
- Render pricing and Docker support: <https://render.com/pricing>
- Neon application connection guide: <https://neon.com/docs/connect/connect-from-any-app>

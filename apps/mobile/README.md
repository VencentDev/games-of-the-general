# app_mobile

Flutter (Dart) mobile client for the monorepo backend. See
[`docs/adr/0007-flutter-for-mobile.md`](../../docs/adr/0007-flutter-for-mobile.md)
for the architecture decisions.

## Prerequisites

- Flutter SDK (stable channel) and Dart
- A running backend (`make dev` from the repo root → backend on `:8080`)
- A Google OAuth client (iOS/Android type) — see below

## Setup

```bash
cd apps/mobile
flutter pub get
make -C ../.. mobile-gen   # or: dart run swagger_parser && dart run build_runner build -d
```

`mobile-gen` generates the typed API client from `packages/api-types/openapi.json`
(the same spec the web app uses). Generated code under `lib/src/api/generated/`
is git-ignored and regenerated in CI.

## Configuration

Auth uses native Google OAuth; the access token is sent to the backend as a
bearer token (ADR-006). Provide config via `--dart-define`:

```bash
flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8080 \
  --dart-define=GOOGLE_CLIENT_ID=ID.apps.googleusercontent.com \
  --dart-define=GOOGLE_REDIRECT_URL=com.googleusercontent.apps.ID:/oauth2redirect
```

- `10.0.2.2` is the Android emulator's alias for the host's `localhost`.
- The redirect scheme (reversed client ID) must also be set as the platform URL
  scheme: Android `appAuthRedirectScheme` (gradle property
  `android.appAuthRedirectScheme`) and iOS `CFBundleURLSchemes` in
  `ios/Runner/Info.plist`.

> GitHub sign-in is not implemented on mobile yet — it needs a backend
> code-exchange endpoint (ADR-007).

## Common commands

```bash
make -C ../.. mobile-gen    # regenerate the API client
make -C ../.. mobile-run    # flutter run
make -C ../.. mobile-test   # flutter analyze && flutter test
```

## Layout

```text
lib/
  main.dart                     ProviderScope + MaterialApp
  src/
    core/        env.dart, dio_provider.dart (auth interceptor + providers)
    auth/        auth_service.dart (Google OAuth), token_store.dart, auth_controller.dart
    features/    home/home_screen.dart (sign-in + profile)
    api/generated/              generated OpenAPI client (git-ignored)
```

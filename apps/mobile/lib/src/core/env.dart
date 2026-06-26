/// Runtime configuration, supplied via `--dart-define` at build/run time.
///
/// Auth mirrors the backend contract (ADR-006): the app obtains a **Google**
/// OAuth access token natively and sends it to the backend as a bearer token;
/// the backend resolves the user via the Google userinfo API. No client secret
/// is required for native (installed-app) Google OAuth — PKCE is used instead.
///
/// `API_BASE_URL` defaults to the Android emulator's host alias (`10.0.2.2`).
/// Override per environment, e.g.:
///   flutter run \
///     --dart-define=API_BASE_URL=https://api.example.com \
///     --dart-define=GOOGLE_CLIENT_ID=`ID`.apps.googleusercontent.com \
///     --dart-define=GOOGLE_REDIRECT_URL=com.googleusercontent.apps.`ID`:/oauth2redirect
class Env {
  const Env._();

  static const String apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );

  /// Google's OIDC issuer; AppAuth discovers endpoints from
  /// `$issuer/.well-known/openid-configuration`.
  static const String googleIssuer = 'https://accounts.google.com';

  /// OAuth client ID from Google Cloud Console (iOS/Android client type).
  static const String googleClientId = String.fromEnvironment(
    'GOOGLE_CLIENT_ID',
  );

  /// Must equal the reversed client ID scheme registered for the Google client,
  /// and match the Android `appAuthRedirectScheme` / iOS URL scheme.
  static const String googleRedirectUrl = String.fromEnvironment(
    'GOOGLE_REDIRECT_URL',
  );

  /// `openid email profile` yields a token the backend can resolve via userinfo.
  static const List<String> googleScopes = <String>[
    'openid',
    'email',
    'profile',
  ];

  /// True when the Google client has been configured via --dart-define.
  static bool get isGoogleConfigured =>
      googleClientId.isNotEmpty && googleRedirectUrl.isNotEmpty;
}

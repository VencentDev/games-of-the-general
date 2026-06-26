import 'package:flutter_appauth/flutter_appauth.dart';

import '../core/env.dart';
import 'token_store.dart';

/// Drives native Google OAuth (Authorization Code + PKCE) and keeps tokens
/// fresh. The resulting Google **access token** is what the backend accepts as
/// a bearer credential (ADR-006), resolved server-side via the userinfo API.
class AuthService {
  AuthService(this._appAuth, this._store);

  final FlutterAppAuth _appAuth;
  final TokenStore _store;

  Future<bool> get isLoggedIn async => (await _store.refreshToken) != null;

  Future<void> login() async {
    final result = await _appAuth.authorizeAndExchangeCode(
      AuthorizationTokenRequest(
        Env.googleClientId,
        Env.googleRedirectUrl,
        issuer: Env.googleIssuer,
        scopes: Env.googleScopes,
      ),
    );
    await _persist(
      result.accessToken,
      result.refreshToken,
      result.idToken,
      result.accessTokenExpirationDateTime,
    );
  }

  /// Returns a non-expired access token, refreshing if needed. Null if logged out.
  Future<String?> validAccessToken() async {
    final token = await _store.accessToken;
    final expiry = await _store.expiry;
    final stillValid =
        token != null &&
        expiry != null &&
        expiry.isAfter(DateTime.now().add(const Duration(seconds: 30)));
    return stillValid ? token : refresh();
  }

  Future<String?> refresh() async {
    final refreshToken = await _store.refreshToken;
    if (refreshToken == null) return null;
    final result = await _appAuth.token(
      TokenRequest(
        Env.googleClientId,
        Env.googleRedirectUrl,
        issuer: Env.googleIssuer,
        refreshToken: refreshToken,
        scopes: Env.googleScopes,
      ),
    );
    await _persist(
      result.accessToken,
      result.refreshToken,
      result.idToken,
      result.accessTokenExpirationDateTime,
    );
    return result.accessToken;
  }

  /// Clears local tokens. Google has no RP-initiated end-session endpoint, so
  /// sign-out is local; full revocation can be added via the revoke endpoint.
  Future<void> logout() => _store.clear();

  Future<void> _persist(
    String? access,
    String? refresh,
    String? id,
    DateTime? expiry,
  ) => _store.save(
    accessToken: access,
    refreshToken: refresh,
    idToken: id,
    expiry: expiry,
  );
}

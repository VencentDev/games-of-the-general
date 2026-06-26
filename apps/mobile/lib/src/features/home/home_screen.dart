import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../auth/auth_controller.dart';
import '../../core/env.dart';

/// Starter screen: proves the full chain — Keycloak login -> token storage ->
/// authenticated Dio -> generated client -> rendered backend data.
class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final controller = ref.read(authControllerProvider.notifier);

    return Scaffold(
      appBar: AppBar(
        title: const Text('app_mobile'),
        actions: [
          if (auth.asData?.value != null)
            IconButton(
              tooltip: 'Log out',
              icon: const Icon(Icons.logout),
              onPressed: controller.logout,
            ),
        ],
      ),
      body: Center(
        child: switch (auth) {
          AsyncLoading() => const CircularProgressIndicator(),
          AsyncError(:final error) => Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Sign-in failed:\n$error', textAlign: TextAlign.center),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: controller.login,
                  child: const Text('Try again'),
                ),
              ],
            ),
          ),
          AsyncData(value: final user) when user != null => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.account_circle, size: 72),
              const SizedBox(height: 12),
              Text(user.email, style: Theme.of(context).textTheme.titleMedium),
              Text('Role: ${user.role.name}'),
            ],
          ),
          _ => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              FilledButton.icon(
                onPressed: Env.isGoogleConfigured ? controller.login : null,
                icon: const Icon(Icons.login),
                label: const Text('Sign in with Google'),
              ),
              if (!Env.isGoogleConfigured)
                const Padding(
                  padding: EdgeInsets.only(top: 12),
                  child: Text(
                    'Set GOOGLE_CLIENT_ID and GOOGLE_REDIRECT_URL via --dart-define.',
                    textAlign: TextAlign.center,
                  ),
                ),
            ],
          ),
        },
      ),
    );
  }
}

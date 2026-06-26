'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';

import { siteConfig } from '@/config/site';
import { AuroraPanel } from '@/features/auth/shared/components/aurora-panel';
import { OAuthButtons } from '@/features/auth/shared/components/oauth-buttons';

export function LoginPageContent() {
  return (
    <Suspense fallback={<LoginShell />}>
      <LoginContent />
    </Suspense>
  );
}

function LoginContent() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') ?? '/lobby';

  return <LoginShell callbackUrl={callbackUrl} />;
}

function LoginShell({ callbackUrl }: { callbackUrl?: string }) {
  return (
    <main className="relative -mx-4 -my-6 flex min-h-[calc(100svh-3.5rem)] max-w-[100vw] items-center overflow-x-clip bg-[#f5f1e6] px-4 py-6 text-[#201b16] selection:bg-[#d7bd73]/40 dark:bg-[#11130f] dark:text-[#f6f0e4] md:py-8">
      <section className="relative z-10 mx-auto flex min-h-[520px] w-full max-w-[min(58rem,calc(100vw-2rem))] animate-float-in flex-col overflow-hidden rounded-xl border border-[#8a7b62]/25 bg-[#fbf8ef] shadow-2xl shadow-black/10 dark:border-white/10 dark:bg-[#181b15] md:flex-row">
        <AuroraPanel
          variant="login"
          title={siteConfig.name}
          subtitle="Return to your matches and continue the campaign."
        />
        <div className="flex flex-1 items-center p-8 md:p-12">
          <form className="w-full space-y-6" onSubmit={(event) => event.preventDefault()}>
            <div className="space-y-2">
              <p className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8f2f24] dark:text-[#f29a7f]">
                Command access
              </p>
              <h1 className="text-3xl font-bold tracking-tight">Sign in</h1>
              <p className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
                Enter the war room, resume saved games, and keep your battle record attached to your
                account.
              </p>
            </div>

            <div className="space-y-4">
              <div className="space-y-1.5">
                <label
                  htmlFor="email"
                  className="font-mono text-[11px] uppercase tracking-wider text-[#6f674f] dark:text-[#b8b09e]"
                >
                  Email Address
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  placeholder={siteConfig.emailPlaceholder}
                  className="field-input"
                />
              </div>

              <div className="space-y-1.5">
                <div className="flex items-center justify-between gap-4">
                  <label
                    htmlFor="password"
                    className="font-mono text-[11px] uppercase tracking-wider text-[#6f674f] dark:text-[#b8b09e]"
                  >
                    Password
                  </label>
                  <Link
                    href="/login"
                    className="font-mono text-[11px] uppercase tracking-wider text-[#8f2f24] transition-colors hover:text-[#2c3520] dark:text-[#f29a7f] dark:hover:text-[#d7bd73]"
                  >
                    Forgot?
                  </Link>
                </div>
                <input
                  id="password"
                  name="password"
                  type="password"
                  autoComplete="current-password"
                  placeholder="••••••••"
                  className="field-input"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full rounded-lg bg-[#2c3520] py-2.5 font-semibold text-[#fff8ea] shadow-lg shadow-[#2c3520]/10 transition-all duration-300 hover:bg-[#202817] active:scale-[0.98] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
            >
              Enter War Room
            </button>

            <AuthDivider label="Continue with" />
            <OAuthButtons callbackUrl={callbackUrl} />

            <p className="text-center text-sm text-[#655c51] dark:text-[#c9c1b4]">
              New here?{' '}
              <Link
                href="/signup"
                className="text-[#8f2f24] underline-offset-4 transition-colors hover:text-[#2c3520] hover:underline decoration-[#8f2f24]/40 dark:text-[#f29a7f] dark:hover:text-[#d7bd73]"
              >
                Create account
              </Link>
            </p>
          </form>
        </div>
      </section>
    </main>
  );
}

function AuthDivider({ label }: { label: string }) {
  return (
    <div className="relative">
      <div className="absolute inset-0 flex items-center">
        <span className="w-full border-t border-[#8a7b62]/25 dark:border-white/10" />
      </div>
      <div className="relative flex justify-center">
        <span className="bg-[#fbf8ef] px-3 font-mono text-[10px] uppercase tracking-widest text-[#6f674f] dark:bg-[#181b15] dark:text-[#b8b09e]">
          {label}
        </span>
      </div>
    </div>
  );
}

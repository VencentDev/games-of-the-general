'use client';

import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { Suspense } from 'react';

import { siteConfig } from '@/config/site';
import { AuroraPanel } from '@/features/auth/shared/components/aurora-panel';
import { OAuthButtons } from '@/features/auth/shared/components/oauth-buttons';

export function SignupPageContent() {
  return (
    <Suspense fallback={<SignupShell />}>
      <SignupContent />
    </Suspense>
  );
}

function SignupContent() {
  const searchParams = useSearchParams();
  const callbackUrl = searchParams.get('callbackUrl') ?? '/lobby';

  return <SignupShell callbackUrl={callbackUrl} />;
}

function SignupShell({ callbackUrl }: { callbackUrl?: string }) {
  return (
    <main className="relative flex min-h-screen max-w-[100vw] items-center overflow-x-clip bg-[#f5f1e6] px-4 py-6 text-[#201b16] selection:bg-[#d7bd73]/40 dark:bg-[#11130f] dark:text-[#f6f0e4] md:py-8">
      <section className="relative z-10 mx-auto flex min-h-[560px] w-full max-w-[min(58rem,calc(100vw-2rem))] animate-float-in flex-col-reverse overflow-hidden rounded-xl border border-[#8a7b62]/25 bg-[#fbf8ef] shadow-2xl shadow-black/10 dark:border-white/10 dark:bg-[#181b15] md:flex-row">
        <div className="flex flex-1 items-center p-8 md:p-12">
          <form className="w-full space-y-6" onSubmit={(event) => event.preventDefault()}>
            <div className="space-y-2">
              <p className="font-mono text-[10px] uppercase tracking-[0.3em] text-[#8f2f24] dark:text-[#f29a7f]">
                New commander
              </p>
              <h1 className="text-3xl font-bold tracking-tight">Create account</h1>
              <p className="text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
                Save your profile, track future matches, and prepare your first hidden formation.
              </p>
            </div>

            <div className="space-y-4">
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  id="first-name"
                  label="First Name"
                  placeholder="Maria"
                  autoComplete="given-name"
                />
                <Field
                  id="last-name"
                  label="Last Name"
                  placeholder="Santos"
                  autoComplete="family-name"
                />
              </div>
              <Field
                id="signup-email"
                label="Email"
                type="email"
                placeholder={siteConfig.emailPlaceholder}
                autoComplete="email"
              />
              <div className="grid gap-3 sm:grid-cols-2">
                <Field
                  id="signup-password"
                  label="Password"
                  type="password"
                  placeholder="••••••••"
                  autoComplete="new-password"
                />
                <Field
                  id="confirm-password"
                  label="Confirm"
                  type="password"
                  placeholder="••••••••"
                  autoComplete="new-password"
                />
              </div>
            </div>

            <button
              type="submit"
              className="w-full rounded-lg bg-[#8f2f24] py-2.5 font-semibold text-[#fff8ea] transition-all duration-300 hover:bg-[#76251c] active:scale-[0.98] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
            >
              Create Command Profile
            </button>

            <AuthDivider label="Or continue with" />
            <OAuthButtons callbackUrl={callbackUrl} />

            <p className="text-center text-sm text-[#655c51] dark:text-[#c9c1b4]">
              Already enlisted?{' '}
              <Link
                href="/login"
                className="text-[#8f2f24] underline-offset-4 transition-colors hover:text-[#2c3520] hover:underline decoration-[#8f2f24]/40 dark:text-[#f29a7f] dark:hover:text-[#d7bd73]"
              >
                Sign in
              </Link>
            </p>
          </form>
        </div>
        <AuroraPanel
          variant="signup"
          title="Build your command"
          subtitle="Ranks hidden until battle"
        />
      </section>
    </main>
  );
}

function Field({
  id,
  label,
  type = 'text',
  placeholder,
  autoComplete,
}: {
  id: string;
  label: string;
  type?: string;
  placeholder: string;
  autoComplete?: string;
}) {
  return (
    <div className="space-y-1.5">
      <label
        htmlFor={id}
        className="font-mono text-[11px] uppercase tracking-wider text-[#6f674f] dark:text-[#b8b09e]"
      >
        {label}
      </label>
      <input
        id={id}
        name={id}
        type={type}
        placeholder={placeholder}
        autoComplete={autoComplete}
        className="field-input"
      />
    </div>
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

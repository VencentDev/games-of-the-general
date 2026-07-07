import Link from 'next/link';
import { Crosshair } from 'lucide-react';

import { SignOutButton } from '@/components/sign-out-button';
import { ThemeToggle } from '@/components/theme-toggle';
import { Button } from '@/components/ui/button';
import { siteConfig } from '@/config/site';
import { auth } from '@/lib/auth';

export async function Navbar() {
  const session = await auth();

  return (
    <header className="border-b border-[#ded7c8] bg-white text-[#11130f] dark:border-[#2a2418] dark:bg-[#070b05] dark:text-[#fffaf0]">
      <div className="container mx-auto flex h-14 items-center justify-between gap-4">
        <Link
          href="/"
          className="flex items-center gap-3 font-mono text-xs font-black uppercase tracking-[0.18em]"
        >
          <span className="grid size-8 place-items-center rounded-lg bg-[#ee7b51] text-white shadow-[0_0_22px_rgba(238,123,81,0.22)]">
            <Crosshair className="size-4" />
          </span>
          {siteConfig.navName}
        </Link>
        <nav className="flex items-center gap-2">
          {session ? (
            <>
              <Button
                asChild
                variant="outline"
                size="sm"
                className="border-[#11130f] bg-[#11130f] text-white hover:bg-[#2a2d22] hover:text-white dark:border-[#5b5036] dark:bg-[#11130f] dark:hover:bg-[#1b2116]"
              >
                <Link href="/lobby">Lobby</Link>
              </Button>
              <SignOutButton />
            </>
          ) : (
            <Button
              asChild
              variant="outline"
              size="sm"
              className="border-[#11130f] bg-[#11130f] text-white hover:bg-[#2a2d22] hover:text-white dark:border-[#5b5036] dark:bg-[#11130f] dark:hover:bg-[#1b2116]"
            >
              <Link href="/login">Sign in</Link>
            </Button>
          )}
          <ThemeToggle />
        </nav>
      </div>
    </header>
  );
}

'use client';

import { LogOut } from 'lucide-react';
import { signOut } from 'next-auth/react';

import { Button } from '@/components/ui/button';

export function SignOutButton() {
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      className="gap-2 border-[#11130f] bg-[#11130f] text-white hover:bg-[#2a2d22] hover:text-white dark:border-[#5b5036] dark:bg-[#11130f] dark:hover:bg-[#1b2116]"
      onClick={() => void signOut({ redirectTo: '/login' })}
    >
      <LogOut className="size-4" aria-hidden="true" />
      <span>Sign out</span>
    </Button>
  );
}

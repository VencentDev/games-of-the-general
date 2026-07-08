'use client';

import { useTheme } from 'next-themes';
import { useEffect, useId, useState } from 'react';

import { cn } from '@/lib/utils';

export function ThemeToggle({ className = '' }: { className?: string }) {
  const { resolvedTheme, setTheme } = useTheme();
  const clipPathId = useId();
  const [mounted, setMounted] = useState(false);
  const isDark = mounted && resolvedTheme === 'dark';
  const nextTheme = isDark ? 'light' : 'dark';

  useEffect(() => {
    setMounted(true);
  }, []);

  return (
    <button
      type="button"
      aria-label="Toggle theme"
      title="Toggle theme"
      className={cn(
        'relative inline-flex size-10 items-center justify-center rounded-full border border-transparent transition-all duration-300 active:scale-95 disabled:pointer-events-none disabled:opacity-50',
        isDark
          ? 'bg-[#11130f] text-[#fffaf0] hover:bg-[#1b2116]'
          : 'bg-white text-[#11130f] shadow-sm ring-1 ring-[#11130f]/10 hover:bg-[#fff8eb]',
        className,
      )}
      disabled={!mounted}
      onClick={() => setTheme(nextTheme)}
    >
      <svg
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
        fill="currentColor"
        strokeLinecap="round"
        viewBox="0 0 32 32"
        className="size-5"
      >
        <clipPath id={clipPathId}>
          <path
            className="transition-transform duration-300 ease-in-out motion-reduce:transition-none"
            style={{
              transform: isDark ? 'translate(-12px, 10px)' : 'translate(0, 0)',
            }}
            d="M0-5h30a1 1 0 0 0 9 13v24H0Z"
          />
        </clipPath>
        <g clipPath={`url(#${clipPathId})`}>
          <circle
            className="origin-center transition-transform duration-300 ease-in-out motion-reduce:transition-none"
            style={{ transform: isDark ? 'scale(1.25)' : 'scale(1)' }}
            cx="16"
            cy="16"
            r="8"
          />
          <g
            className="origin-center transition-all duration-300 ease-in-out motion-reduce:transition-none"
            style={{
              opacity: isDark ? 0 : 1,
              transform: isDark ? 'rotate(-100deg) scale(0.5)' : 'rotate(0deg) scale(1)',
            }}
            stroke="currentColor"
            strokeWidth="1.5"
          >
            <path d="M16 5.5v-4" />
            <path d="M16 30.5v-4" />
            <path d="M1.5 16h4" />
            <path d="M26.5 16h4" />
            <path d="m23.4 8.6 2.8-2.8" />
            <path d="m5.7 26.3 2.9-2.9" />
            <path d="m5.8 5.8 2.8 2.8" />
            <path d="m23.4 23.4 2.9 2.9" />
          </g>
        </g>
      </svg>
    </button>
  );
}

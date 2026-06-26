import { Flag, Shield, Swords } from 'lucide-react';

import { cn } from '@/lib/utils';

type AuroraPanelProps = {
  title: string;
  subtitle: string;
  variant: 'login' | 'signup';
  className?: string;
};

export function AuroraPanel({ title, subtitle, variant, className }: AuroraPanelProps) {
  if (variant === 'signup') {
    return (
      <aside
        className={cn(
          'relative flex min-h-[280px] w-full items-center justify-center overflow-hidden border-t border-[#8a7b62]/25 bg-[#2c3520] p-8 text-[#fff8ea] md:min-h-0 md:w-[45%] md:border-l md:border-t-0',
          className,
        )}
      >
        <div className="absolute inset-0 bg-[linear-gradient(135deg,rgba(215,189,115,0.18)_25%,transparent_25%,transparent_50%,rgba(215,189,115,0.18)_50%,rgba(215,189,115,0.18)_75%,transparent_75%)] bg-[length:34px_34px] opacity-25" />
        <div className="relative z-10 flex max-w-xs flex-col items-center text-center">
          <div className="mb-7 grid grid-cols-3 gap-2">
            {['5G', 'Spy', 'F'].map((piece) => (
              <span
                key={piece}
                className="flex size-14 items-center justify-center rounded-md border border-[#d7bd73]/40 bg-[#f6f0e4] text-xs font-black text-[#2c3520] shadow-lg"
              >
                {piece}
              </span>
            ))}
          </div>
          <h2 className="text-xl font-bold tracking-tight">{title}</h2>
          <p className="mt-2 font-mono text-[10px] uppercase tracking-[0.2em] text-[#d7bd73]">
            {subtitle}
          </p>
        </div>
      </aside>
    );
  }

  return (
    <aside
      className={cn(
        'relative flex min-h-[280px] w-full items-center justify-center overflow-hidden border-b border-[#8a7b62]/25 bg-[#fbf8ef] p-8 md:min-h-0 md:w-[45%] md:border-b-0 md:border-r dark:bg-[#181b15]',
        className,
      )}
    >
      <div className="absolute inset-5 rounded-lg border border-[#8a7b62]/20" />
      <div className="relative z-10 flex max-w-xs flex-col items-center px-4 text-center">
        <div className="mb-6 flex size-16 items-center justify-center rounded-lg border border-[#2c3520]/15 bg-[#2c3520] text-[#fff8ea] shadow-lg shadow-black/10 dark:border-[#d7bd73]/20">
          {variant === 'login' ? <Swords className="size-7" /> : <Shield className="size-7" />}
        </div>
        <h2 className="text-xl font-bold tracking-tight text-[#201b16] dark:text-[#f6f0e4]">
          {title}
        </h2>
        <p className="mt-2 max-w-[24ch] text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
          {subtitle}
        </p>
        <div className="mt-6 flex items-center gap-2 text-[#8f2f24] dark:text-[#f29a7f]">
          <Flag className="size-4" />
          <span className="font-mono text-[10px] uppercase tracking-[0.2em]">Flag route ready</span>
        </div>
      </div>
    </aside>
  );
}

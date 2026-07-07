const marqueeItems = [
  'Online lobby',
  'Secret deployment',
  '21 pieces',
  'Flag capture',
  'Spy traps',
  'Private counters spy',
  'Ranked matches',
  'Turn-based tactics',
  'Cross-device play',
  'Match history',
];

export function LandingMarquee() {
  const items = [...marqueeItems, ...marqueeItems];

  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-0 z-30 border-y border-[#ded7c8] bg-white/92 py-3 shadow-[0_-18px_48px_rgba(0,0,0,0.12)] backdrop-blur-md dark:border-[#3b321e] dark:bg-[#090c06]/92 dark:shadow-[0_-18px_48px_rgba(0,0,0,0.38)]">
      <div className="got-marquee flex w-max items-center gap-8 font-mono text-xs font-black uppercase tracking-[0.18em] text-[#5d5648] dark:text-[#a9a08a]">
        {items.map((item, index) => (
          <span key={`${item}-${index}`} className="flex items-center gap-8 whitespace-nowrap">
            {item}
            <span className="size-1.5 rounded-full bg-[#ee7b51]" />
          </span>
        ))}
      </div>
    </div>
  );
}

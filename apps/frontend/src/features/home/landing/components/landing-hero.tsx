import Link from 'next/link';
import { ArrowRight, Radio, SatelliteDish, Shield, Swords } from 'lucide-react';

import { TacticalBoardPreview } from '@/features/home/landing/components/tactical-board-preview';

const missionStats = [
  ['21', 'hidden pieces'],
  ['3', 'setup rows'],
  ['2', 'win conditions'],
];

export function LandingHero({ signedIn }: { signedIn: boolean }) {
  return (
    <section className="relative isolate min-h-[calc(100svh-3.5rem)] overflow-hidden border-b border-[#ded7c8] dark:border-[#2a2418]">
      <div className="absolute inset-0 -z-20 bg-[radial-gradient(circle_at_58%_42%,rgba(238,123,81,0.18),transparent_34%),linear-gradient(90deg,#ffffff_0%,#faf7ef_48%,#f1e7d3_100%)] dark:bg-[radial-gradient(circle_at_58%_42%,rgba(162,95,47,0.36),transparent_34%),linear-gradient(90deg,#070b05_0%,#121407_45%,#201309_100%)]" />
      <div className="absolute inset-0 -z-10 bg-[linear-gradient(rgba(112,91,52,0.09)_1px,transparent_1px),linear-gradient(90deg,rgba(112,91,52,0.09)_1px,transparent_1px)] bg-[size:64px_64px] dark:bg-[linear-gradient(rgba(232,190,117,0.055)_1px,transparent_1px),linear-gradient(90deg,rgba(232,190,117,0.055)_1px,transparent_1px)]" />
      <div className="absolute inset-x-0 bottom-0 -z-10 h-48 bg-gradient-to-t from-white to-transparent dark:from-[#070b05]" />

      <div className="mx-auto grid min-h-[calc(100svh-7rem)] w-full max-w-7xl items-center gap-14 px-4 pb-24 pt-20 sm:px-6 lg:grid-cols-[0.92fr_1.08fr] lg:px-8 lg:pt-24">
        <div className="max-w-2xl">
          <div className="mb-7 inline-flex items-center gap-2 rounded-full border border-[#ee7b51]/55 bg-white/80 px-3 py-1.5 font-mono text-[10px] font-extrabold uppercase tracking-[0.18em] text-[#7b301d] shadow-[0_0_28px_rgba(240,123,78,0.14)] dark:bg-[#1b140d]/75 dark:text-[#f4d69c]">
            <span className="size-2 rounded-full bg-[#f07b4e]" />
            Hidden ranks online
          </div>

          <h1 className="max-w-[12ch] text-5xl font-black uppercase leading-[0.9] tracking-normal text-[#14130f] sm:text-6xl lg:text-7xl dark:text-white">
            Command the
            <span className="block text-[#9b5d19] dark:text-[#e8d18b]">secret front.</span>
          </h1>

          <p className="mt-6 max-w-xl text-base leading-8 text-[#5d5648] sm:text-lg dark:text-[#c9c0aa]">
            Deploy your twenty-one-piece army in secret, read every threat behind the fog of rank,
            and win by capturing the opposing flag or carrying yours across enemy lines.
          </p>

          <div className="mt-7 flex flex-col gap-3 sm:flex-row">
            <Link
              href={signedIn ? '/lobby' : '/signup'}
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-lg bg-[#11130f] px-6 font-mono text-xs font-black uppercase tracking-[0.14em] text-white shadow-[0_0_0_7px_rgba(17,19,15,0.1),0_18px_40px_rgba(0,0,0,0.18)] transition hover:bg-[#2a2d22] active:scale-[0.98] dark:bg-[#ee7b51] dark:hover:bg-[#ff8b5e]"
            >
              {signedIn ? 'Enter lobby' : 'Launch command'}
              <ArrowRight className="size-4" />
            </Link>
            <Link
              href={signedIn ? '/lobby/history' : '/login'}
              className="inline-flex min-h-14 items-center justify-center gap-2 rounded-lg border border-[#11130f] bg-[#11130f] px-6 font-mono text-xs font-black uppercase tracking-[0.14em] text-white transition hover:bg-[#2a2d22] active:scale-[0.98] dark:border-[#5b5036] dark:bg-[#0e1209]/80 dark:text-[#fff6df] dark:hover:border-[#e8d18b]/70 dark:hover:bg-[#191d10]"
            >
              <Swords className="size-4" />
              {signedIn ? 'Match history' : 'Sign in'}
            </Link>
          </div>

          <dl className="mt-8 grid max-w-xl grid-cols-3 gap-3">
            {missionStats.map(([value, label]) => (
              <div
                key={label}
                className="border-l border-[#9b5d19]/25 bg-white/70 px-4 py-3 shadow-sm backdrop-blur-sm dark:border-[#e8d18b]/25 dark:bg-[#14140b]/40"
              >
                <dt className="font-mono text-2xl font-black text-[#57391a] dark:text-[#f7e2a3]">
                  {value}
                </dt>
                <dd className="mt-1 text-[10px] font-bold uppercase tracking-[0.16em] text-[#6c6559] dark:text-[#9f987f]">
                  {label}
                </dd>
              </div>
            ))}
          </dl>
        </div>

        <div className="relative mx-auto w-full max-w-[38rem] lg:max-w-[43rem]">
          <div className="absolute -right-3 top-4 z-20 hidden rounded-lg border border-[#d8c8a8] bg-white/95 p-4 shadow-2xl shadow-black/15 sm:block dark:border-[#5b5036]/80 dark:bg-[#11150c]/95 dark:shadow-black/45">
            <div className="flex items-center gap-2 font-mono text-[10px] font-black uppercase tracking-[0.18em] text-[#f07b4e]">
              <SatelliteDish className="size-3.5" />
              Live match
            </div>
            <p className="mt-3 font-mono text-sm font-black uppercase tracking-[0.12em] text-[#17140f] dark:text-white">
              Flag pressure
            </p>
            <div className="mt-3 flex items-center gap-3">
              <span className="font-mono text-xs font-bold text-[#6c6559] dark:text-[#9f987f]">
                Turn 18
              </span>
              <span className="text-xs font-bold text-[#9b5d19] dark:text-[#e8d18b]">72%</span>
            </div>
            <div className="mt-2 h-1.5 w-36 overflow-hidden rounded-full bg-[#e4dccd] dark:bg-[#2c321c]">
              <div className="h-full w-[72%] bg-[#f1b96d]" />
            </div>
          </div>

          <TacticalBoardPreview />

          <div className="got-radar absolute -bottom-8 -left-4 z-20 hidden sm:grid">
            <div className="got-radar-sweep" />
            <span className="got-radar-blip left-[35%] top-[34%]" />
            <span className="got-radar-blip got-radar-blip-delay right-[28%] top-[42%]" />
            <span className="got-radar-blip got-radar-blip-slow bottom-[28%] left-[52%]" />
            <Radio className="relative z-10 size-5 text-[#9b5d19] dark:text-[#e8d18b]" />
          </div>

          <div className="absolute -left-5 top-8 hidden h-24 w-24 rounded-full border border-[#e8d18b]/15 bg-[#e8d18b]/5 blur-2xl lg:block" />
          <Shield className="absolute -bottom-7 right-10 hidden size-16 rotate-12 text-[#e8d18b]/10 lg:block" />
        </div>
      </div>
    </section>
  );
}

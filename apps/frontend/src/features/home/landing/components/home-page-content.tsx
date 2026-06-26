import Link from 'next/link';
import { Flag, Shield, Swords, Trophy } from 'lucide-react';

export function HomePageContent() {
  return (
    <main className="relative -mx-4 -my-6 max-w-[100vw] overflow-x-clip bg-[#f5f1e6] text-[#201b16] dark:bg-[#11130f] dark:text-[#f6f0e4]">
      <section className="mx-auto grid min-h-[calc(100svh-3.5rem)] w-full max-w-7xl items-center gap-10 px-4 py-8 md:grid-cols-[0.95fr_1.05fr] md:px-8 md:py-10">
        <div className="max-w-2xl space-y-7">
          <div className="inline-flex items-center gap-2 rounded-md border border-[#6f7b4a]/30 bg-white/55 px-3 py-1.5 text-[11px] font-semibold uppercase tracking-[0.22em] text-[#566033] shadow-sm dark:border-[#b7c36e]/25 dark:bg-white/5 dark:text-[#cbd585]">
            <Shield className="size-3.5" />
            Hidden-rank battlefield
          </div>
          <div className="space-y-5">
            <h1 className="max-w-[11ch] text-5xl font-black leading-[0.94] tracking-tight sm:text-6xl lg:text-7xl">
              Command the board. Capture the flag.
            </h1>
            <p className="max-w-xl text-base leading-7 text-[#5d544a] dark:text-[#c9c1b4]">
              Arrange twenty-one pieces in secret, read your opponent&apos;s formation, and win by
              eliminating the Flag or escorting yours across enemy lines.
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              href="/signup"
              className="inline-flex items-center justify-center gap-2 rounded-lg bg-[#2c3520] px-5 py-3 text-sm font-bold text-[#fff8ea] shadow-lg shadow-[#2c3520]/20 transition hover:bg-[#202817] active:scale-[0.98] dark:bg-[#d7bd73] dark:text-[#15130d] dark:hover:bg-[#e7ce88]"
            >
              <Flag className="size-4" />
              Create account
            </Link>
            <Link
              href="/login"
              className="inline-flex items-center justify-center gap-2 rounded-lg border border-[#8a7b62]/35 bg-white/55 px-5 py-3 text-sm font-semibold transition hover:border-[#2c3520]/50 hover:bg-white/80 active:scale-[0.98] dark:border-white/15 dark:bg-white/5 dark:hover:bg-white/10"
            >
              <Swords className="size-4" />
              Sign in
            </Link>
          </div>
          <dl className="grid max-w-xl grid-cols-3 gap-3">
            {[
              ['21', 'pieces'],
              ['3', 'setup rows'],
              ['2', 'win paths'],
            ].map(([value, label]) => (
              <div
                key={label}
                className="rounded-lg border border-[#8a7b62]/20 bg-white/45 p-3 dark:border-white/10 dark:bg-white/[0.04]"
              >
                <dt className="text-2xl font-black">{value}</dt>
                <dd className="text-[11px] font-semibold uppercase tracking-[0.16em] text-[#6f674f] dark:text-[#b8b09e]">
                  {label}
                </dd>
              </div>
            ))}
          </dl>
        </div>

        <BattlefieldPreview />
      </section>

      <section className="border-y border-[#8a7b62]/20 bg-[#ebe2d1] px-4 py-8 dark:border-white/10 dark:bg-[#181b15] md:px-8">
        <div className="mx-auto grid max-w-7xl gap-4 md:grid-cols-3">
          {[
            {
              icon: Swords,
              title: 'Officers outrank officers',
              body: 'Generals down to Lieutenants remove lower officers, Privates, and Flags.',
            },
            {
              icon: Shield,
              title: 'Spies reverse the hierarchy',
              body: 'Two Spies can eliminate any officer from Sergeant through Five-Star General.',
            },
            {
              icon: Trophy,
              title: 'Private hunts the Spy',
              body: 'Six Privates are vulnerable to officers but can eliminate Spies and Flags.',
            },
          ].map((item) => (
            <article
              key={item.title}
              className="rounded-lg border border-[#8a7b62]/20 bg-[#fbf8ef] p-5 dark:border-white/10 dark:bg-white/[0.04]"
            >
              <item.icon className="mb-4 size-5 text-[#8f2f24] dark:text-[#f29a7f]" />
              <h2 className="text-base font-bold">{item.title}</h2>
              <p className="mt-2 text-sm leading-6 text-[#655c51] dark:text-[#c9c1b4]">
                {item.body}
              </p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}

function BattlefieldPreview() {
  const rows = [
    ['5G', 'Col', 'Pvt', '', 'Spy', 'Maj', '', 'F', '2G'],
    ['Lt', '', 'Capt', 'Pvt', '', 'Sgt', '2Lt', '', 'Pvt'],
    ['4G', 'Pvt', '', '1G', 'Pvt', '', '3G', 'LC', ''],
    ['', '', '', '', '', '', '', '', ''],
    ['', '', '', '', '', '', '', '', ''],
    ['?', '?', '', '?', '?', '', '?', '?', '?'],
    ['?', '', '?', '?', '', '?', '?', '', '?'],
    ['?', '?', '?', '', '?', '?', 'F', '?', '?'],
  ];

  return (
    <div className="relative mx-auto w-full max-w-[38rem]">
      <div className="rounded-xl border border-[#2c3520]/20 bg-[#2c3520] p-3 shadow-2xl shadow-black/20 dark:border-[#d7bd73]/20">
        <div className="rounded-lg border border-[#d7bd73]/30 bg-[#d1b16a] p-2">
          <div className="grid aspect-[9/8] grid-cols-9 overflow-hidden rounded-md border border-[#201b16]/30 bg-[#53442c]">
            {rows.flatMap((row, rowIndex) =>
              row.map((piece, colIndex) => {
                const isPlayer = rowIndex < 3;
                const isEnemy = rowIndex > 4;
                const key = `${rowIndex}-${colIndex}`;

                return (
                  <div
                    key={key}
                    className="relative flex items-center justify-center border border-[#2b2419]/35 bg-[#b89555] odd:bg-[#caa765]"
                  >
                    {piece ? (
                      <span
                        className={[
                          'flex size-[68%] items-center justify-center rounded-md border text-[10px] font-black shadow-md sm:text-xs',
                          isPlayer
                            ? 'border-[#1f2b16] bg-[#eef0d2] text-[#253017]'
                            : 'border-[#7b261d] bg-[#fbefe1] text-[#7b261d]',
                          isEnemy ? 'font-mono' : '',
                        ].join(' ')}
                      >
                        {piece}
                      </span>
                    ) : null}
                  </div>
                );
              }),
            )}
          </div>
        </div>
      </div>
      <div className="absolute -bottom-4 left-5 right-5 rounded-lg border border-[#8a7b62]/25 bg-[#fbf8ef]/95 px-4 py-3 shadow-xl dark:border-white/10 dark:bg-[#1f211a]/95">
        <p className="text-xs font-bold uppercase tracking-[0.18em] text-[#8f2f24] dark:text-[#f29a7f]">
          Opening formation
        </p>
        <p className="mt-1 text-sm text-[#5d544a] dark:text-[#c9c1b4]">
          First three rows are yours. Six open squares keep the first moves flexible.
        </p>
      </div>
    </div>
  );
}

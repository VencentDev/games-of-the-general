import Image from 'next/image';

import captainSrc from '@/assets/gg assets/Captain.png';
import flagSrc from '@/assets/gg assets/flag.png';
import generalSrc from '@/assets/gg assets/5star.jpg';
import majorSrc from '@/assets/gg assets/Major.png';
import privateSrc from '@/assets/gg assets/private.png';
import spySrc from '@/assets/gg assets/spy.png';

const boardRows = [
  ['5G', 'Maj', '', 'Spy', '', 'Pvt', 'Flag', '', 'Capt'],
  ['', 'Pvt', 'Capt', '', 'Maj', '', '', 'Spy', ''],
  ['Pvt', '', 'Flag', 'Pvt', '', '5G', 'Capt', '', 'Pvt'],
  ['', '', '', '', '', '', '', '', ''],
  ['', '', '', '', '', '', '', '', ''],
  ['?', '?', '', '?', '?', '', '?', '?', '?'],
  ['?', '', '?', '?', '', '?', '?', '', '?'],
  ['?', '?', '?', '', '?', '?', '?', '?', ''],
];

const featuredPieces = [
  { label: '5G', src: generalSrc, alt: 'Five-star general rank piece' },
  { label: 'Spy', src: spySrc, alt: 'Spy rank piece' },
  { label: 'Flag', src: flagSrc, alt: 'Flag piece' },
  { label: 'Maj', src: majorSrc, alt: 'Major rank piece' },
  { label: 'Capt', src: captainSrc, alt: 'Captain rank piece' },
  { label: 'Pvt', src: privateSrc, alt: 'Private rank piece' },
];

export function TacticalBoardPreview() {
  return (
    <div className="relative rotate-0 rounded-2xl border border-[#d7c7a6] bg-white p-3 shadow-[0_32px_90px_rgba(90,68,36,0.18)] lg:-rotate-1 dark:border-[#5b5036]/70 dark:bg-[#171508] dark:shadow-[0_32px_90px_rgba(0,0,0,0.52)]">
      <div className="absolute left-4 top-4 z-10 grid grid-cols-3 gap-1.5">
        {featuredPieces.slice(0, 6).map((piece) => (
          <div
            key={piece.label}
            className="grid size-9 place-items-center overflow-hidden rounded-full border border-[#d7c7a6] bg-[#f7f1e4] shadow-inner dark:border-[#8b743e] dark:bg-[#191408]"
          >
            <Image src={piece.src} alt={piece.alt} className="h-full w-full object-cover" />
          </div>
        ))}
      </div>

      <div className="rounded-xl border border-[#d7c7a6] bg-[radial-gradient(circle_at_50%_40%,rgba(238,186,95,0.16),transparent_34%),linear-gradient(135deg,#fffaf0,#ead7af)] p-4 dark:border-[#c19b55]/25 dark:bg-[radial-gradient(circle_at_50%_40%,rgba(238,186,95,0.2),transparent_34%),linear-gradient(135deg,#2c2515,#0d1009)]">
        <div className="mb-3 flex items-center justify-between border-b border-[#9b5d19]/15 pb-3 pl-28 dark:border-[#e8d18b]/15">
          <div>
            <p className="font-mono text-[9px] font-black uppercase tracking-[0.18em] text-[#7b715f] dark:text-[#9f987f]">
              Operation
            </p>
            <p className="font-mono text-xs font-black uppercase tracking-[0.14em] text-[#9b5d19] dark:text-[#e8d18b]">
              Silent Flag
            </p>
          </div>
          <p className="font-mono text-[10px] font-bold uppercase tracking-[0.14em] text-[#7b715f] dark:text-[#746c54]">
            Rank intel concealed
          </p>
        </div>

        <div className="grid aspect-[9/8] grid-cols-9 overflow-hidden rounded-lg border border-[#c6a46b] bg-[#ead7af] dark:border-[#846b35]/55 dark:bg-[#2f2a18]">
          {boardRows.flatMap((row, rowIndex) =>
            row.map((piece, colIndex) => {
              const isEnemy = rowIndex > 4;
              const isObjective = piece === 'Flag';

              return (
                <div
                  key={`${rowIndex}-${colIndex}`}
                  className="relative flex items-center justify-center border border-[#a47738]/20 bg-[#dec48d]/70 odd:bg-[#efd8a4]/75 dark:border-[#95733a]/20 dark:bg-[#594a26]/65 dark:odd:bg-[#6b572d]/55"
                >
                  <span className="absolute inset-1 rounded-[0.35rem] border border-[#8a5a24]/10 dark:border-[#e8d18b]/5" />
                  {piece ? (
                    <span
                      className={[
                        'relative z-10 flex size-[64%] items-center justify-center rounded-full border font-mono text-[9px] font-black uppercase shadow-lg sm:text-[10px]',
                        isEnemy
                          ? 'border-[#22251c] bg-[#22251c] text-white dark:border-[#1e2418] dark:bg-[#1a2115] dark:text-[#d7d0b8]'
                          : 'border-[#11130f] bg-[#11130f] text-white dark:border-[#d2ac5d] dark:bg-[#2a170e] dark:text-[#f5d584]',
                        isObjective ? 'shadow-[#f07b4e]/20' : '',
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
  );
}

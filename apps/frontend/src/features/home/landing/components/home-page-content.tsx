import { LandingHero } from '@/features/home/landing/components/landing-hero';
import { LandingMarquee } from '@/features/home/landing/components/landing-marquee';

export function HomePageContent({ signedIn }: { signedIn: boolean }) {
  return (
    <main className="relative min-h-[calc(100svh-3.5rem)] overflow-hidden bg-white text-[#16130d] dark:bg-[#070b05] dark:text-[#fffaf0]">
      <LandingHero signedIn={signedIn} />
      <LandingMarquee />
    </main>
  );
}

import type { Metadata } from 'next';

import { Navbar } from '@/components/navbar';
import { siteConfig } from '@/config/site';
import { HomePageContent } from '@/features/home/landing/components/home-page-content';
import { auth } from '@/lib/auth';

export const metadata: Metadata = {
  title: siteConfig.metadataTitle,
  description: siteConfig.metadataDescription,
  openGraph: {
    title: siteConfig.metadataTitle,
    description: siteConfig.metadataDescription,
  },
};

export default async function HomePage() {
  const session = await auth();

  return (
    <>
      <Navbar />
      <HomePageContent signedIn={!!session && !session.error} />
    </>
  );
}

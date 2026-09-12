import type { Metadata } from "next";
import { Suspense } from "react";

import { HeroBanner } from "@/features/media/components/hero-banner";
import { MediaRow } from "@/features/media/components/media-row";
import { GenreChips } from "@/features/media/components/genre-chips";
import { CatalogConfigBanner } from "@/features/media/components/catalog-config-banner";
import { PageLoader } from "@/components/feedback/page-loader";
import { ScrollReveal } from "@/components/motion/scroll-reveal";
import { getActiveCatalogMaturity } from "@/lib/media/catalog-context";
import { safeGetDiscoveryHome } from "@/lib/media/catalog";
import { Skeleton } from "@/components/ui/skeleton";

export const metadata: Metadata = {
  title: "Home",
  description: "Discover movies and TV shows to watch now",
};

export const revalidate = 900;

export default async function BrowsePage() {
  const maturity = await getActiveCatalogMaturity();
  const data = await safeGetDiscoveryHome(maturity);
  const heroItems =
    data.heroItems?.length > 0
      ? data.heroItems
      : data.hero
        ? [data.hero]
        : [];

  return (
    <div className="space-y-10">
      {!data.configured ? <CatalogConfigBanner /> : null}

      {"error" in data && data.error ? (
        <div
          className="animate-fade-up rounded-xl border-0 bg-destructive/12 px-4 py-3 text-sm text-destructive"
          role="alert"
        >
          <p className="font-medium">Catalog temporarily unavailable</p>
          <p className="mt-1 text-muted-foreground">{data.error}</p>
        </div>
      ) : null}

      {heroItems.length > 0 ? (
        <ScrollReveal variant="scale">
          <HeroBanner items={heroItems} intervalMs={3000} />
        </ScrollReveal>
      ) : null}

      <Suspense fallback={<Skeleton className="h-12 w-full rounded-xl" />}>
        <ScrollReveal delay={0.05}>
          <GenreChips genres={data.genres} />
        </ScrollReveal>
      </Suspense>

      <div className="space-y-10">
        {data.sections.map((section, index) => (
          <MediaRow
            key={section.id}
            title={section.title}
            items={section.items}
            href={section.href}
            priorityCount={index === 0 ? 4 : 0}
          />
        ))}
      </div>

      {data.configured && !data.hero && data.sections.length === 0 ? (
        <p className="text-sm text-muted-foreground">
          No catalog content returned. Check your TMDB key and network access.
        </p>
      ) : null}
    </div>
  );
}

export function BrowseLoading() {
  return <PageLoader />;
}

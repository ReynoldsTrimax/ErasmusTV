import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { DetailHero } from "@/features/media/components/detail-hero";
import { StreamButton } from "@/features/streaming/components/stream-button";
import { MediaMeta } from "@/features/media/components/media-meta";
import { CastRow } from "@/features/media/components/cast-row";
import { MediaRow } from "@/features/media/components/media-row";
import { StreamingProviders } from "@/features/media/components/streaming-providers";
import { SeasonEpisodes } from "@/features/media/components/season-episodes";
import { Badge } from "@/components/ui/badge";
import { CatalogConfigBanner } from "@/features/media/components/catalog-config-banner";
import { getActiveCatalogMaturity } from "@/lib/media/catalog-context";
import { getTvShow, isCatalogConfigured } from "@/lib/media/catalog";
import { isTitleAllowedForAge } from "@/lib/media/maturity";
import { formatDate, formatNumber, formatRuntime } from "@/lib/media/format";
import { mediaHref } from "@/lib/media/routes";
import type { MediaIdentity } from "@/types/media";

interface PageProps {
  params: Promise<{ id: string }>;
}

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { id } = await params;
  if (!isCatalogConfigured()) return { title: "TV Show" };
  try {
    const show = await getTvShow(id);
    if (!show) return { title: "Show not found" };
    return {
      title: show.title,
      description: show.overview?.slice(0, 160) ?? `Details for ${show.title}`,
    };
  } catch {
    return { title: "TV Show" };
  }
}

export default async function TvDetailPage({ params }: PageProps) {
  const { id } = await params;

  if (!isCatalogConfigured()) {
    return <CatalogConfigBanner />;
  }

  const show = await getTvShow(id);
  if (!show) notFound();

  const maturity = await getActiveCatalogMaturity();
  if (
    !isTitleAllowedForAge(
      {
        adult: show.adult,
        certification: show.certification,
        mediaType: "tv",
        genres: show.genres,
        genreIds: show.genres.map((g) => g.id),
      },
      maturity,
    )
  ) {
    return (
      <div className="mx-auto max-w-lg py-20 text-center">
        <h1 className="text-xl font-semibold">Not available on this profile</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          This title is outside the age range for the current profile.
        </p>
      </div>
    );
  }

  const runtime =
    show.episodeRunTime?.length
      ? show.episodeRunTime[0]
      : null;

  const identity: MediaIdentity = {
    provider: "tmdb",
    mediaType: "tv",
    externalId: show.id,
    title: show.title,
    originalTitle: show.originalTitle,
    posterPath: show.posterPath,
    backdropPath: show.backdropPath,
    releaseDate: show.firstAirDate,
    overview: show.overview,
    runtimeMinutes: runtime,
    totalEpisodes: show.numberOfEpisodes,
    genres: show.genres.map((g) => g.name),
    originalLanguage: show.originalLanguage,
  };

  return (
    <div className="space-y-10 animate-fade-up">
      <DetailHero
        title={show.title}
        tagline={show.tagline}
        overview={show.overview}
        backdropPath={show.backdropPath}
        posterPath={show.posterPath}
        logoPath={show.logoPath}
        releaseDate={show.firstAirDate}
        runtime={runtime}
        certification={show.certification}
        status={show.status}
        mediaTypeLabel="TV Series"
        genres={show.genres}
        ratings={show.ratings}
        videos={show.videos}
        streaming={show.streaming}
      >
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 pt-2">
          <StreamButton
            title={show.title}
            tmdbId={show.id}
            mediaType="tv"
            identity={identity}
            season={
              show.seasons.find((s) => s.seasonNumber > 0)?.seasonNumber ?? 1
            }
            episode={1}
            variant="hero"
          />
          {show.createdBy.length ? (
            <p className="text-sm text-muted-foreground">
              Created by{" "}
              {show.createdBy.map((c, i) => (
                <span key={c.creditId ?? c.id}>
                  {i > 0 ? ", " : null}
                  <Link
                    href={mediaHref("person", c.id)}
                    className="font-medium text-foreground underline-offset-4 hover:underline"
                  >
                    {c.name}
                  </Link>
                </span>
              ))}
            </p>
          ) : null}
        </div>
      </DetailHero>

      <div className="grid w-full min-w-0 max-w-full gap-8 lg:grid-cols-[minmax(0,1fr)_minmax(16.5rem,20rem)] lg:items-start lg:gap-6">
        <div className="min-w-0 max-w-full space-y-10 overflow-x-hidden">
          {/* Progress is recorded here, on the episode rows themselves. Tracking
              props are omitted for signed-out visitors, who get the same list
              without checkboxes. */}
          <SeasonEpisodes
            showId={show.id}
            seasons={show.seasons}
            title={show.title}
          />

          <CastRow people={show.cast} />

          {show.recommendations.length ? (
            <MediaRow title="Recommendations" items={show.recommendations} />
          ) : null}
          {show.similar.length ? (
            <MediaRow title="Similar shows" items={show.similar} />
          ) : null}
        </div>

        <aside className="min-w-0 w-full space-y-6 lg:sticky lg:top-20 lg:self-start lg:max-h-[calc(100vh-5.5rem)] lg:w-auto lg:overflow-y-auto lg:overflow-x-hidden lg:pr-0.5">
          <div className="rounded-3xl border-0 bg-muted/40 dark:bg-white/[0.05] p-5">
            <h2 className="mb-3 text-sm font-semibold">Details</h2>
            <MediaMeta
              items={[
                { label: "First air date", value: formatDate(show.firstAirDate) },
                { label: "Last air date", value: formatDate(show.lastAirDate) },
                { label: "Status", value: show.status },
                { label: "Type", value: show.type },
                { label: "Seasons", value: show.numberOfSeasons },
                { label: "Episodes", value: show.numberOfEpisodes },
                {
                  label: "Episode runtime",
                  value: runtime ? formatRuntime(runtime) : null,
                },
                {
                  label: "Original language",
                  value: show.originalLanguage?.toUpperCase(),
                },
                {
                  label: "Languages",
                  value: show.spokenLanguages
                    .map((l) => l.englishName ?? l.name)
                    .join(", "),
                },
                { label: "Popularity", value: formatNumber(show.popularity) },
                { label: "Vote count", value: formatNumber(show.voteCount) },
              ]}
            />
          </div>

          {show.networks.length ? (
            <div className="rounded-3xl border-0 bg-muted/40 dark:bg-white/[0.05] p-5">
              <h2 className="mb-3 text-sm font-semibold">Networks</h2>
              <ul className="space-y-1.5 text-sm text-muted-foreground">
                {show.networks.map((n) => (
                  <li key={n.id}>{n.name}</li>
                ))}
              </ul>
            </div>
          ) : null}

          {show.productionCompanies.length ? (
            <div className="rounded-3xl border-0 bg-muted/40 dark:bg-white/[0.05] p-5">
              <h2 className="mb-3 text-sm font-semibold">Studios</h2>
              <ul className="space-y-1.5 text-sm text-muted-foreground">
                {show.productionCompanies.map((c) => (
                  <li key={c.id}>{c.name}</li>
                ))}
              </ul>
            </div>
          ) : null}

          {show.keywords.length ? (
            <div className="rounded-3xl border-0 bg-muted/40 dark:bg-white/[0.05] p-5">
              <h2 className="mb-3 text-sm font-semibold">Keywords</h2>
              <div className="flex flex-wrap gap-1.5">
                {show.keywords.slice(0, 20).map((k) => (
                  <Badge key={k.id} variant="muted">
                    {k.name}
                  </Badge>
                ))}
              </div>
            </div>
          ) : null}

          <StreamingProviders availability={show.streaming} />

          <div className="rounded-3xl border-0 bg-muted/40 dark:bg-white/[0.05] bg-muted/30 dark:bg-white/[0.04] p-5">
            <h2 className="text-sm font-semibold">Awards</h2>
            <p className="mt-1 text-xs text-muted-foreground">
              Awards data will appear when a provider is connected.
            </p>
          </div>
        </aside>
      </div>
    </div>
  );
}

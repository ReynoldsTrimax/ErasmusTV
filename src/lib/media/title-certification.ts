import { TtlCache } from "@/lib/utils/ttl-cache";
import { tmdbFetch } from "@/lib/media/providers/tmdb/client";
import {
  isTitleAllowedForAge,
  type CatalogMaturity,
} from "@/lib/media/maturity";
import type { MediaSummary } from "@/types/media";

const cache = new TtlCache(2_000);
const TTL_MS = 24 * 60 * 60 * 1000;

interface ReleaseDatesPayload {
  results?: {
    iso_3166_1: string;
    release_dates?: { certification?: string }[];
  }[];
}

interface ContentRatingsPayload {
  results?: { iso_3166_1: string; rating?: string }[];
}

function pickUsMovieCert(payload: ReleaseDatesPayload | null): string | null {
  const us = payload?.results?.find((row) => row.iso_3166_1 === "US");
  const rated = us?.release_dates?.find((row) => row.certification?.trim());
  return rated?.certification?.trim() || null;
}

function pickUsTvRating(payload: ContentRatingsPayload | null): string | null {
  const us = payload?.results?.find((row) => row.iso_3166_1 === "US");
  return us?.rating?.trim() || null;
}

export async function lookupCertification(
  mediaType: "movie" | "tv",
  id: string,
): Promise<string | null> {
  return cache.resolve(`cert:${mediaType}:${id}`, TTL_MS, async () => {
    if (mediaType === "tv") {
      const data = await tmdbFetch<ContentRatingsPayload>(
        `/tv/${id}/content_ratings`,
      );
      return pickUsTvRating(data);
    }
    const data = await tmdbFetch<ReleaseDatesPayload>(
      `/movie/${id}/release_dates`,
    );
    return pickUsMovieCert(data);
  });
}

async function mapPool<T, R>(
  items: T[],
  limit: number,
  mapper: (item: T) => Promise<R>,
): Promise<R[]> {
  const out: R[] = [];
  for (let i = 0; i < items.length; i += limit) {
    const slice = items.slice(i, i + limit);
    out.push(...(await Promise.all(slice.map(mapper))));
  }
  return out;
}

/** Look up missing US ratings, then drop anything this profile cannot watch. */
export async function enforceMaturityOnSummaries<T extends MediaSummary>(
  items: T[],
  maturity: CatalogMaturity,
): Promise<T[]> {
  if (maturity.age >= 18) return items;

  const enriched = await mapPool(items, 6, async (item) => {
    if (item.certification) return item;
    const certification = await lookupCertification(item.mediaType, item.id);
    return { ...item, certification };
  });

  return enriched.filter((item) =>
    isTitleAllowedForAge(item, maturity, { requireCertification: maturity.age < 17 }),
  );
}

export async function isMediaAllowedForMaturity(
  mediaType: "movie" | "tv",
  id: string,
  maturity: CatalogMaturity,
  extra?: {
    adult?: boolean | null;
    certification?: string | null;
    genreIds?: string[] | null;
  },
): Promise<boolean> {
  if (maturity.age >= 18) return true;
  const certification =
    extra?.certification ?? (await lookupCertification(mediaType, id));
  return isTitleAllowedForAge(
    {
      mediaType,
      adult: extra?.adult,
      certification,
      genreIds: extra?.genreIds,
    },
    maturity,
    { requireCertification: maturity.age < 17 },
  );
}

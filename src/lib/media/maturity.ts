import type { MediaDiscoverFilters, MediaSummary } from "@/types/media";

export interface CatalogMaturity {
  age: number;
  includeAdult: boolean;
  /** TMDB movie certification.lte when under 17. */
  movieCertLte: "G" | "PG" | "PG-13" | null;
  excludeGenreIds: string[];
  tvRatings: string[] | null;
}

const HORROR = "27";
const THRILLER = "53";
const CRIME = "80";
const WAR = "10752";
const ROMANCE = "10749";

const MOVIE_CERT_MIN_AGE: Record<string, number> = {
  G: 0,
  PG: 8,
  "PG-13": 13,
  R: 17,
  "NC-17": 18,
  NR: 17,
};

const TV_RATING_MIN_AGE: Record<string, number> = {
  "TV-Y": 0,
  "TV-Y7": 7,
  "TV-G": 0,
  "TV-PG": 8,
  "TV-14": 14,
  "TV-MA": 17,
};

export function ageFromBirthYear(
  birthYear: number | null | undefined,
  now = new Date(),
): number {
  if (!birthYear) return 18;
  return Math.min(120, Math.max(1, now.getFullYear() - birthYear));
}

export function maturityFromBirthYear(
  birthYear: number | null | undefined,
): CatalogMaturity {
  const age = ageFromBirthYear(birthYear);
  if (age < 8) {
    return {
      age,
      includeAdult: false,
      movieCertLte: "G",
      excludeGenreIds: [HORROR, THRILLER, CRIME, WAR, ROMANCE],
      tvRatings: ["TV-Y", "TV-Y7", "TV-G"],
    };
  }
  if (age < 13) {
    return {
      age,
      includeAdult: false,
      movieCertLte: "PG",
      excludeGenreIds: [HORROR, CRIME, WAR],
      tvRatings: ["TV-Y", "TV-Y7", "TV-G", "TV-PG"],
    };
  }
  if (age < 17) {
    return {
      age,
      includeAdult: false,
      movieCertLte: "PG-13",
      excludeGenreIds: [],
      tvRatings: ["TV-Y", "TV-Y7", "TV-G", "TV-PG", "TV-14"],
    };
  }
  return {
    age,
    includeAdult: age >= 18,
    movieCertLte: null,
    excludeGenreIds: [],
    tvRatings: null,
  };
}

export function certificationMinimumAge(
  certification: string | null | undefined,
  mediaType: "movie" | "tv",
): number {
  if (!certification) return 0;
  const key = certification.trim().toUpperCase();
  if (mediaType === "tv") return TV_RATING_MIN_AGE[key] ?? 0;
  return MOVIE_CERT_MIN_AGE[key] ?? 0;
}

export function isTitleAllowedForAge(
  item: {
    adult?: boolean | null;
    certification?: string | null;
    mediaType?: "movie" | "tv";
    genreIds?: string[] | null;
    genres?: { id: string }[] | null;
  },
  maturity: CatalogMaturity,
  options: { requireCertification?: boolean } = {},
): boolean {
  if (item.adult && maturity.age < 18) return false;
  const mediaType = item.mediaType === "tv" ? "tv" : "movie";
  const cert = item.certification?.trim() || null;
  if (options.requireCertification && maturity.age < 17 && !cert) {
    return false;
  }
  if (certificationMinimumAge(cert, mediaType) > maturity.age) {
    return false;
  }
  if (maturity.tvRatings && mediaType === "tv" && cert) {
    if (!maturity.tvRatings.includes(cert.toUpperCase())) return false;
  }
  if (maturity.excludeGenreIds.length) {
    const ids = new Set([
      ...(item.genreIds ?? []),
      ...(item.genres ?? []).map((g) => g.id),
    ]);
    if (maturity.excludeGenreIds.some((id) => ids.has(id))) return false;
  }
  return true;
}

export function filterSummariesForAge<T extends MediaSummary>(
  items: T[],
  maturity: CatalogMaturity,
): T[] {
  return items.filter((item) => isTitleAllowedForAge(item, maturity));
}

export function applyMaturityToFilters(
  filters: MediaDiscoverFilters,
  maturity: CatalogMaturity,
  kind: "movie" | "tv",
): MediaDiscoverFilters {
  return {
    ...filters,
    includeAdult: maturity.includeAdult,
    certificationLte:
      kind === "movie" ? (maturity.movieCertLte ?? undefined) : undefined,
    certificationGte: kind === "movie" && maturity.movieCertLte ? "G" : undefined,
    certificationCountry:
      kind === "movie" && maturity.movieCertLte ? "US" : undefined,
    contentRatings: kind === "tv" ? (maturity.tvRatings ?? undefined) : undefined,
    withoutGenreIds: maturity.excludeGenreIds.length
      ? maturity.excludeGenreIds
      : filters.withoutGenreIds,
  };
}

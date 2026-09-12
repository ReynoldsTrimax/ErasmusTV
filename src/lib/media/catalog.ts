/**
 * High-level catalog service.
 * Features and pages call this layer — not providers or HTTP clients directly.
 */

import type {
  CollectionDetails,
  DiscoverySection,
  Genre,
  MediaDiscoverFilters,
  MediaSummary,
  MovieDetails,
  PaginatedResult,
  PersonDetails,
  SearchResponse,
  TvDetails,
  TvSeason,
} from "@/types/media";
import { getMediaProvider } from "@/lib/media/providers";
import { isTmdbConfigured } from "@/lib/media/providers/tmdb/client";
import { enrichRatings } from "@/lib/media/ratings";
import { CREDIBILITY_VOTE_FLOOR } from "@/lib/media/filters";
import {
  applyMaturityToFilters,
  type CatalogMaturity,
} from "@/lib/media/maturity";
import { enforceMaturityOnSummaries } from "@/lib/media/title-certification";

export function isCatalogConfigured(): boolean {
  return isTmdbConfigured();
}

export async function searchCatalog(
  query: string,
  page?: number,
  maturity?: CatalogMaturity,
): Promise<SearchResponse> {
  const result = await getMediaProvider().search(query, { page });
  if (!maturity || maturity.age >= 18) return result;
  const titles = result.results.filter(
    (item) => item.kind === "movie" || item.kind === "tv",
  );
  const allowed = await enforceMaturityOnSummaries(
    titles.map((item) => ({
      id: item.id,
      mediaType: item.kind as "movie" | "tv",
      title: item.title,
      posterPath: item.imagePath,
      backdropPath: null,
      adult: item.adult,
    })),
    maturity,
  );
  const allowedKeys = new Set(allowed.map((item) => `${item.mediaType}:${item.id}`));
  return {
    ...result,
    results: result.results.filter((item) => {
      if (item.kind !== "movie" && item.kind !== "tv") return true;
      return allowedKeys.has(`${item.kind}:${item.id}`);
    }),
  };
}

export async function getMovie(id: string): Promise<MovieDetails | null> {
  const movie = await getMediaProvider().getMovie(id);
  if (!movie) return null;
  const ratings = await enrichRatings({
    imdbId: movie.imdbId,
    title: movie.title,
    releaseDate: movie.releaseDate,
    mediaType: "movie",
    voteAverage: movie.voteAverage,
    voteCount: movie.voteCount,
  });
  return { ...movie, ratings };
}

export async function getTvShow(id: string): Promise<TvDetails | null> {
  const show = await getMediaProvider().getTvShow(id);
  if (!show) return null;
  const ratings = await enrichRatings({
    imdbId: show.imdbId,
    title: show.title,
    releaseDate: show.firstAirDate ?? show.releaseDate,
    mediaType: "tv",
    voteAverage: show.voteAverage,
    voteCount: show.voteCount,
  });
  return { ...show, ratings };
}

export async function getTvSeason(
  showId: string,
  seasonNumber: number,
): Promise<TvSeason | null> {
  return getMediaProvider().getTvSeason(showId, seasonNumber);
}

export async function getPerson(id: string): Promise<PersonDetails | null> {
  return getMediaProvider().getPerson(id);
}

export async function getCollection(id: string): Promise<CollectionDetails | null> {
  return getMediaProvider().getCollection(id);
}

export async function getMovieGenres(): Promise<Genre[]> {
  return getMediaProvider().getMovieGenres();
}

export async function getTvGenres(): Promise<Genre[]> {
  return getMediaProvider().getTvGenres();
}

export async function discoverMovies(
  filters: MediaDiscoverFilters,
  maturity?: CatalogMaturity,
): Promise<PaginatedResult<MediaSummary>> {
  const next = maturity
    ? applyMaturityToFilters(filters, maturity, "movie")
    : filters;
  const page = await getMediaProvider().discoverMovies(next);
  if (!maturity) return page;
  return {
    ...page,
    results: await enforceMaturityOnSummaries(page.results, maturity),
  };
}

export async function discoverTv(
  filters: MediaDiscoverFilters,
  maturity?: CatalogMaturity,
): Promise<PaginatedResult<MediaSummary>> {
  const next = maturity ? applyMaturityToFilters(filters, maturity, "tv") : filters;
  const page = await getMediaProvider().discoverTv(next);
  if (!maturity) return page;
  return {
    ...page,
    results: await enforceMaturityOnSummaries(page.results, maturity),
  };
}

const EMPTY_PAGE: PaginatedResult<MediaSummary> = {
  page: 1,
  totalPages: 0,
  totalResults: 0,
  results: [],
};

async function settledPage(
  promise: Promise<PaginatedResult<MediaSummary>>,
  label: string,
): Promise<PaginatedResult<MediaSummary>> {
  try {
    return await promise;
  } catch (error) {
    console.warn(`[catalog] ${label} failed:`, error instanceof Error ? error.message : error);
    return EMPTY_PAGE;
  }
}

async function settledGenres(
  promise: Promise<Genre[]>,
  label: string,
): Promise<Genre[]> {
  try {
    return await promise;
  } catch (error) {
    console.warn(`[catalog] ${label} failed:`, error instanceof Error ? error.message : error);
    return [];
  }
}

/**
 * Assembles the discovery homepage sections in parallel.
 * Individual rail failures are isolated so one timeout does not blank the page.
 */
export async function getDiscoveryHome(
  maturity?: CatalogMaturity,
): Promise<{
  hero: MediaSummary | null;
  heroItems: MediaSummary[];
  sections: DiscoverySection[];
  genres: Genre[];
}> {
  const provider = getMediaProvider();
  const clip = async (items: MediaSummary[]) =>
    maturity ? enforceMaturityOnSummaries(items, maturity) : items;
  const restricted = Boolean(maturity && maturity.age < 17);

  const [
    trending,
    popularMovies,
    popularTv,
    nowPlaying,
    upcoming,
    topMovies,
    topTv,
    recentMovies,
    movieGenres,
  ] = await Promise.all([
    settledPage(
      restricted && maturity
        ? discoverMovies({ sortBy: "popularity.desc" }, maturity)
        : provider.getTrending("all", "day"),
      "trending",
    ),
    settledPage(
      restricted && maturity
        ? discoverMovies({ sortBy: "popularity.desc" }, maturity)
        : provider.getPopularMovies(),
      "popular-movies",
    ),
    settledPage(
      restricted && maturity
        ? discoverTv({ sortBy: "popularity.desc" }, maturity)
        : provider.getPopularTv(),
      "popular-tv",
    ),
    settledPage(
      restricted && maturity
        ? discoverMovies({ sortBy: "release_date.desc" }, maturity)
        : provider.getNowPlayingMovies(),
      "now-playing",
    ),
    settledPage(
      restricted && maturity
        ? discoverMovies(
            {
              sortBy: "release_date.desc",
              yearGte: new Date().getFullYear(),
            },
            maturity,
          )
        : provider.getUpcomingMovies(),
      "upcoming",
    ),
    settledPage(
      restricted && maturity
        ? discoverMovies({ sortBy: "vote_average.desc", voteCountGte: 1000 }, maturity)
        : provider.getTopRatedMovies(),
      "top-movies",
    ),
    settledPage(
      restricted && maturity
        ? discoverTv({ sortBy: "vote_average.desc", voteCountGte: 500 }, maturity)
        : provider.getTopRatedTv(),
      "top-tv",
    ),
    settledPage(
      discoverMovies(
        {
          sortBy: "release_date.desc",
          yearGte: new Date().getFullYear() - 1,
          voteAverageGte: 6,
        },
        maturity,
      ),
      "recent-movies",
    ),
    settledGenres(provider.getMovieGenres(), "movie-genres"),
  ]);

  const heroPool = [
    ...trending.results,
    ...popularMovies.results,
    ...popularTv.results,
  ];
  const seen = new Set<string>();
  const uniqueHero = heroPool.filter((item) => {
    const key = `${item.mediaType}:${item.id}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return Boolean(item.backdropPath || item.posterPath);
  });

  const [
    heroItems,
    trendingItems,
    popularMovieItems,
    popularTvItems,
    nowPlayingItems,
    upcomingItems,
    topMovieItems,
    topTvItems,
    recentItems,
    editorsPicks,
  ] = await Promise.all([
    clip(uniqueHero).then((items) => items.slice(0, 12)),
    clip(trending.results).then((items) => items.slice(0, 18)),
    clip(popularMovies.results).then((items) => items.slice(0, 18)),
    clip(popularTv.results).then((items) => items.slice(0, 18)),
    clip(nowPlaying.results).then((items) => items.slice(0, 18)),
    clip(upcoming.results).then((items) => items.slice(0, 18)),
    clip(topMovies.results).then((items) => items.slice(0, 18)),
    clip(topTv.results).then((items) => items.slice(0, 18)),
    clip(recentMovies.results).then((items) => items.slice(0, 18)),
    clip([...topMovies.results, ...topTv.results]).then((items) =>
      items
        .sort((a, b) => (b.voteAverage ?? 0) - (a.voteAverage ?? 0))
        .slice(0, 12),
    ),
  ]);
  const hero = heroItems[0] ?? null;

  const sections: DiscoverySection[] = [
    {
      id: "trending",
      title: "Trending Today",
      href: "/discover?section=trending",
      items: trendingItems,
    },
    {
      id: "popular-movies",
      title: "Popular Movies",
      href: "/movies?sort=popularity.desc",
      items: popularMovieItems,
    },
    {
      id: "popular-tv",
      title: "Popular TV Shows",
      href: "/tv?sort=popularity.desc",
      items: popularTvItems,
    },
    {
      id: "now-playing",
      title: "Now Playing",
      href: "/movies?section=now_playing",
      items: nowPlayingItems,
    },
    {
      id: "upcoming",
      title: "Upcoming Movies",
      href: "/movies?section=upcoming",
      items: upcomingItems,
    },
    {
      id: "top-movies",
      title: "Top Rated Movies",
      href: "/movies?sort=vote_average.desc",
      items: topMovieItems,
    },
    {
      id: "top-tv",
      title: "Top Rated Shows",
      href: "/tv?sort=vote_average.desc",
      items: topTvItems,
    },
    {
      id: "recent",
      title: "Recently Released",
      href: "/movies?sort=release_date.desc",
      items: recentItems,
    },
    {
      id: "streaming",
      title: "New on Streaming",
      href: "/discover?section=streaming",
      items: popularMovieItems.slice(4, 16),
    },
    {
      id: "editors",
      title: "Editor's Picks",
      items: editorsPicks,
    },
  ];

  const filled = sections.filter((s) => s.items.length > 0);

  if (filled.length === 0) {
    throw new Error(
      "Could not load any catalog sections from TMDB. Check network/DNS access to api.themoviedb.org.",
    );
  }

  return {
    hero,
    heroItems,
    sections: filled,
    genres: movieGenres,
  };
}

export async function getGenrePage(
  genreId: string,
  mediaType: "movie" | "tv" = "movie",
  filters: MediaDiscoverFilters = {},
  maturity?: CatalogMaturity,
): Promise<{
  genre: Genre | null;
  featured: MediaSummary[];
  popular: PaginatedResult<MediaSummary>;
  topRated: PaginatedResult<MediaSummary>;
  newest: PaginatedResult<MediaSummary>;
}> {
  const provider = getMediaProvider();
  const genres =
    mediaType === "movie" ? await provider.getMovieGenres() : await provider.getTvGenres();
  const genre = genres.find((g) => g.id === genreId) ?? null;

  const base: MediaDiscoverFilters = {
    ...filters,
    genreIds: [genreId],
    page: filters.page ?? 1,
  };

  const discover =
    mediaType === "movie"
      ? (f: MediaDiscoverFilters) => discoverMovies(f, maturity)
      : (f: MediaDiscoverFilters) => discoverTv(f, maturity);

  const [popular, topRated, newest] = await Promise.all([
    discover({ ...base, sortBy: "popularity.desc" }),
    discover({
      ...base,
      sortBy: "vote_average.desc",
      voteAverageGte: 7,
      voteCountGte: CREDIBILITY_VOTE_FLOOR.filtered[mediaType],
    }),
    discover({ ...base, sortBy: "release_date.desc" }),
  ]);

  return {
    genre,
    featured: popular.results.slice(0, 6),
    popular,
    topRated,
    newest,
  };
}

/** Safe wrapper — returns empty discovery when TMDB is missing (dev without keys). */
export async function safeGetDiscoveryHome(maturity?: CatalogMaturity) {
  if (!isCatalogConfigured()) {
    return {
      hero: null as MediaSummary | null,
      heroItems: [] as MediaSummary[],
      sections: [] as DiscoverySection[],
      genres: [] as Genre[],
      configured: false,
    };
  }
  try {
    const data = await getDiscoveryHome(maturity);
    return { ...data, configured: true };
  } catch (error) {
    console.error("[catalog] discovery home failed", error);
    return {
      hero: null as MediaSummary | null,
      heroItems: [] as MediaSummary[],
      sections: [] as DiscoverySection[],
      genres: [] as Genre[],
      configured: true,
      error: error instanceof Error ? error.message : "Failed to load catalog",
    };
  }
}

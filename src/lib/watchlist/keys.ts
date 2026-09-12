export function watchlistKey(mediaType: "movie" | "tv", tmdbId: string): string {
  return `${mediaType}:${tmdbId}`;
}

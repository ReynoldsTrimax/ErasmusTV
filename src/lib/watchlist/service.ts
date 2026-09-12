import { createClient } from "@/lib/supabase/server";
import { requireUser } from "@/lib/services/user-service";
import { getWatchProfile } from "@/lib/watch-profiles/service";
import type { MediaSummary } from "@/types/media";
import { watchlistKey } from "@/lib/watchlist/keys";

export { watchlistKey };

export interface WatchlistItem {
  id: string;
  profile_id: string;
  media_type: "movie" | "tv";
  tmdb_id: string;
  title: string;
  poster_path: string | null;
  backdrop_path: string | null;
  release_date: string | null;
  created_at: string;
}

function table(client: Awaited<ReturnType<typeof createClient>>) {
  return client.from("watchlist_items" as never);
}

export async function listWatchlist(profileId: string): Promise<WatchlistItem[]> {
  const profile = await getWatchProfile(profileId);
  if (!profile) return [];
  const client = await createClient();
  const { data, error } = await table(client)
    .select("*")
    .eq("profile_id", profileId)
    .order("created_at", { ascending: false });
  if (error) throw new Error(error.message);
  return (data ?? []) as WatchlistItem[];
}

export async function listWatchlistKeys(profileId: string): Promise<string[]> {
  const items = await listWatchlist(profileId);
  return items.map((item) => watchlistKey(item.media_type, item.tmdb_id));
}

export async function addToWatchlist(
  profileId: string,
  item: Pick<
    MediaSummary,
    "id" | "mediaType" | "title" | "posterPath" | "backdropPath" | "releaseDate"
  >,
): Promise<void> {
  const user = await requireUser();
  const profile = await getWatchProfile(profileId);
  if (!profile) throw new Error("Profile not found.");
  const client = await createClient();
  const { error } = await table(client).insert({
    user_id: user.id,
    profile_id: profileId,
    media_type: item.mediaType,
    tmdb_id: item.id,
    title: item.title,
    poster_path: item.posterPath,
    backdrop_path: item.backdropPath,
    release_date: item.releaseDate ?? null,
  } as never);
  if (error && !error.message.toLowerCase().includes("duplicate")) {
    throw new Error(error.message);
  }
}

export async function removeFromWatchlist(
  profileId: string,
  mediaType: "movie" | "tv",
  tmdbId: string,
): Promise<void> {
  const profile = await getWatchProfile(profileId);
  if (!profile) throw new Error("Profile not found.");
  const client = await createClient();
  const { error } = await table(client)
    .delete()
    .eq("profile_id", profileId)
    .eq("media_type", mediaType)
    .eq("tmdb_id", tmdbId);
  if (error) throw new Error(error.message);
}

export function watchlistItemToSummary(item: WatchlistItem): MediaSummary {
  return {
    id: item.tmdb_id,
    mediaType: item.media_type,
    title: item.title,
    posterPath: item.poster_path,
    backdropPath: item.backdrop_path,
    releaseDate: item.release_date,
  };
}

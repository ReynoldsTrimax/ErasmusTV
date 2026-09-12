"use server";

import { revalidatePath } from "next/cache";

import { ROUTES } from "@/constants/routes";
import type { ActionResult } from "@/types";
import type { MediaSummary } from "@/types/media";
import { getActiveWatchProfile } from "@/lib/watch-profiles/service";
import {
  addToWatchlist,
  removeFromWatchlist,
} from "@/lib/watchlist/service";

export async function actionToggleWatchlist(item: {
  id: string;
  mediaType: "movie" | "tv";
  title: string;
  posterPath: string | null;
  backdropPath: string | null;
  releaseDate?: string | null;
  saved: boolean;
}): Promise<ActionResult<{ saved: boolean }>> {
  const profile = await getActiveWatchProfile();
  if (!profile) {
    return { success: false, error: "Choose a profile first." };
  }

  try {
    if (item.saved) {
      await removeFromWatchlist(profile.id, item.mediaType, item.id);
      revalidatePath(ROUTES.watchlist);
      return { success: true, data: { saved: false } };
    }
    const summary: Pick<
      MediaSummary,
      "id" | "mediaType" | "title" | "posterPath" | "backdropPath" | "releaseDate"
    > = {
      id: item.id,
      mediaType: item.mediaType,
      title: item.title,
      posterPath: item.posterPath,
      backdropPath: item.backdropPath,
      releaseDate: item.releaseDate,
    };
    await addToWatchlist(profile.id, summary);
    revalidatePath(ROUTES.watchlist);
    return { success: true, data: { saved: true } };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Could not update watchlist.",
    };
  }
}

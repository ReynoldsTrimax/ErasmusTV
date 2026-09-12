import type { Metadata } from "next";
import { redirect } from "next/navigation";

import { MediaGrid } from "@/features/media/components/media-grid";
import { ROUTES } from "@/constants/routes";
import { getActiveWatchProfile } from "@/lib/watch-profiles/service";
import {
  listWatchlist,
  watchlistItemToSummary,
} from "@/lib/watchlist/service";

export const metadata: Metadata = {
  title: "Watchlist",
  description: "Titles you plan to watch",
};

export default async function WatchlistPage() {
  const profile = await getActiveWatchProfile();
  if (!profile) redirect(ROUTES.profiles);

  const items = await listWatchlist(profile.id);
  const summaries = items.map(watchlistItemToSummary);

  return (
    <div className="space-y-6 animate-fade-up">
      <header className="space-y-1">
        <h1 className="font-display text-2xl font-semibold tracking-tight">
          Watchlist
        </h1>
        <p className="text-sm text-muted-foreground">
          Plan to Watch for {profile.name}. Hover a poster to play.
        </p>
      </header>
      <MediaGrid
        items={summaries}
        emptyTitle="Nothing saved yet"
        emptyDescription="Hover a poster and tap Plan to Watch."
      />
    </div>
  );
}

"use client";

import * as React from "react";
import { Bookmark } from "lucide-react";
import { toast } from "sonner";

import { actionToggleWatchlist } from "@/features/watchlist/actions";
import { useWatchlist } from "@/features/watchlist/watchlist-provider";
import { cn } from "@/lib/utils";
import type { MediaSummary } from "@/types/media";

export function PlanToWatchButton({
  item,
  className,
}: {
  item: MediaSummary;
  className?: string;
}) {
  const { has, setSaved } = useWatchlist();
  const saved = has(item.mediaType, item.id);
  const [pending, startTransition] = React.useTransition();

  return (
    <button
      type="button"
      disabled={pending}
      onClick={(event) => {
        event.preventDefault();
        event.stopPropagation();
        startTransition(async () => {
          const result = await actionToggleWatchlist({
            id: item.id,
            mediaType: item.mediaType,
            title: item.title,
            posterPath: item.posterPath,
            backdropPath: item.backdropPath,
            releaseDate: item.releaseDate,
            saved,
          });
          if (!result.success) {
            toast.error(result.error);
            return;
          }
          setSaved(item.mediaType, item.id, result.data.saved);
          toast.success(
            result.data.saved
              ? `Saved “${item.title}” to Plan to Watch`
              : `Removed “${item.title}” from Watchlist`,
          );
        });
      }}
      className={cn(
        "inline-flex h-7 flex-1 items-center justify-center gap-1 rounded-md px-1.5 text-[10px] font-semibold shadow-sm backdrop-blur-md transition",
        "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary",
        saved
          ? "bg-primary text-white"
          : "bg-black/70 text-white hover:bg-black/85",
        className,
      )}
    >
      <Bookmark className={cn("h-3 w-3", saved && "fill-current")} />
      Plan
    </button>
  );
}

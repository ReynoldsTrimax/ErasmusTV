"use client";

import * as React from "react";
import Image from "next/image";
import { useQuery } from "@tanstack/react-query";
import { ChevronRight } from "lucide-react";

import { Skeleton } from "@/components/ui/skeleton";
import { StreamButton } from "@/features/streaming/components/stream-button";
import { stillUrl } from "@/lib/media/image";
import { formatDate, formatRuntime, formatVote } from "@/lib/media/format";
import type { TvSeason } from "@/types/media";
import { cn } from "@/lib/utils";

interface SeasonEpisodesProps {
  showId: string;
  seasons: TvSeason[];
  title: string;
}

async function fetchSeason(showId: string, season: number): Promise<TvSeason> {
  const res = await fetch(`/api/media/tv/${showId}/season/${season}`);
  if (!res.ok) throw new Error("Failed to load season");
  return res.json() as Promise<TvSeason>;
}

export function SeasonEpisodes({ showId, seasons, title }: SeasonEpisodesProps) {
  const [expanded, setExpanded] = React.useState<Set<number>>(() => {
    const first = seasons.find((s) => (s.episodeCount ?? 0) > 0 && s.seasonNumber > 0);
    return new Set(first ? [first.seasonNumber] : []);
  });

  if (!seasons.length) {
    return <p className="text-sm text-muted-foreground">No seasons available.</p>;
  }

  return (
    <section className="min-w-0 w-full max-w-full space-y-3" aria-label="Seasons and episodes">
      <h2 className="text-section-title">Episodes</h2>
      <ul className="min-w-0 w-full max-w-full space-y-2">
        {seasons.map((season) => (
          <SeasonRow
            key={season.id}
            showId={showId}
            title={title}
            season={season}
            open={expanded.has(season.seasonNumber)}
            onOpenChange={(open) =>
              setExpanded((prev) => {
                const next = new Set(prev);
                if (open) next.add(season.seasonNumber);
                else next.delete(season.seasonNumber);
                return next;
              })
            }
          />
        ))}
      </ul>
    </section>
  );
}

function SeasonRow({
  showId,
  title,
  season,
  open,
  onOpenChange,
}: {
  showId: string;
  title: string;
  season: TvSeason;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const query = useQuery({
    queryKey: ["tv-season", showId, season.seasonNumber],
    queryFn: () => fetchSeason(showId, season.seasonNumber),
    enabled: open,
    staleTime: 60_000,
  });

  const episodes = query.data?.episodes ?? season.episodes ?? [];

  return (
    <li className="overflow-hidden rounded-2xl border-0 bg-muted/30 dark:bg-white/[0.04]">
      <button
        type="button"
        className="flex w-full items-center gap-3 px-4 py-3 text-left"
        aria-expanded={open}
        onClick={() => onOpenChange(!open)}
      >
        <ChevronRight
          className={cn(
            "h-4 w-4 shrink-0 text-muted-foreground transition-transform duration-150",
            open && "rotate-90",
          )}
        />
        <span className="min-w-0 flex-1 truncate font-medium">{season.name}</span>
        <span className="text-xs text-muted-foreground tabular-nums">
          {season.episodeCount ?? episodes.length} ep
        </span>
      </button>
      {open ? (
        <div className="space-y-1 border-t border-white/5 px-2 pb-3 pt-2">
          {query.isLoading ? (
            <Skeleton className="h-16 w-full rounded-xl" />
          ) : episodes.length === 0 ? (
            <p className="px-2 py-3 text-sm text-muted-foreground">No episodes listed.</p>
          ) : (
            episodes.map((episode) => {
              const still = stillUrl(episode.stillPath, "w300");
              return (
                <div
                  key={episode.id}
                  className="flex items-center gap-3 rounded-xl px-2 py-2 hover:bg-white/[0.04]"
                >
                  <div className="relative h-14 w-24 shrink-0 overflow-hidden rounded-lg bg-muted">
                    {still ? (
                      <Image
                        src={still}
                        alt=""
                        fill
                        className="object-cover"
                        sizes="96px"
                      />
                    ) : null}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium">
                      {episode.episodeNumber}. {episode.name}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {[
                        formatDate(episode.airDate),
                        formatRuntime(episode.runtime),
                        episode.voteAverage
                          ? formatVote(episode.voteAverage)
                          : null,
                      ]
                        .filter(Boolean)
                        .join(" · ")}
                    </p>
                  </div>
                  <StreamButton
                    title={title}
                    tmdbId={showId}
                    mediaType="tv"
                    season={season.seasonNumber}
                    episode={episode.episodeNumber}
                    variant="icon"
                  />
                </div>
              );
            })
          )}
        </div>
      ) : null}
    </li>
  );
}

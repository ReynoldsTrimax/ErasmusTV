"use client";

import * as React from "react";
import Image from "next/image";
import { ChevronLeft, ChevronRight } from "lucide-react";

import { Button } from "@/components/ui/button";
import { StreamButton } from "@/features/streaming/components/stream-button";
import { posterUrl } from "@/lib/media/image";
import {
  formatTimecode,
  listContinueWatching,
  type ContinueWatchingItem,
} from "@/lib/streaming/playback-progress";
import { cn } from "@/lib/utils";

export function ContinueWatchingRow() {
  const [items, setItems] = React.useState<ContinueWatchingItem[]>([]);
  const scrollerRef = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    const stored = listContinueWatching();
    if (!stored.length) {
      setItems([]);
      return;
    }

    let cancelled = false;
    fetch("/api/media/allowed", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        items: stored.map((item) => ({
          mediaType: item.mediaType,
          id: item.tmdbId,
        })),
      }),
    })
      .then((response) => response.json())
      .then((data: { allowed?: string[] }) => {
        if (cancelled) return;
        const allowed = new Set(data.allowed ?? []);
        setItems(
          stored.filter((item) =>
            allowed.has(`${item.mediaType}:${item.tmdbId}`),
          ),
        );
      })
      .catch(() => {
        if (!cancelled) setItems([]);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const scroll = (dir: -1 | 1) => {
    const el = scrollerRef.current;
    if (!el) return;
    el.scrollBy({
      left: dir * Math.min(el.clientWidth * 0.82, 540),
      behavior: "smooth",
    });
  };

  if (!items.length) return null;

  return (
    <section className="min-w-0 max-w-full space-y-3" aria-label="Continue watching">
      <div className="flex items-end justify-between gap-3 px-1">
        <h2 className="text-section-title">Continue Watching</h2>
        <div className="hidden items-center gap-1.5 sm:flex">
          <Button
            type="button"
            variant="outline"
            size="icon-sm"
            className="rounded-xl"
            onClick={() => scroll(-1)}
            aria-label="Scroll Continue Watching left"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <Button
            type="button"
            variant="outline"
            size="icon-sm"
            className="rounded-xl"
            onClick={() => scroll(1)}
            aria-label="Scroll Continue Watching right"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
      </div>

      <div
        ref={scrollerRef}
        className="scrollbar-thin flex gap-4 overflow-x-auto scroll-smooth py-6 pl-2 pr-2 snap-x"
        role="list"
      >
        {items.map((item) => {
          const src = posterUrl(item.posterPath, "w342");
          const ratio =
            item.duration && item.duration > 0
              ? Math.min(1, item.seconds / item.duration)
              : 0.08;
          return (
            <div
              key={`${item.mediaType}-${item.tmdbId}`}
              className="w-36 shrink-0 snap-start sm:w-40"
              role="listitem"
            >
              <div className="relative aspect-[2/3] overflow-hidden rounded-xl bg-muted shadow-md">
                {src ? (
                  <Image
                    src={src}
                    alt=""
                    fill
                    className="object-cover"
                    sizes="160px"
                  />
                ) : (
                  <div className="flex h-full items-center p-3 text-center text-xs text-muted-foreground">
                    {item.title}
                  </div>
                )}
                <div className="absolute inset-x-0 bottom-0 bg-gradient-to-t from-black/85 to-transparent p-2 pt-8">
                  <div className="mb-2 h-1 overflow-hidden rounded-full bg-white/25">
                    <div
                      className="h-full rounded-full bg-primary"
                      style={{ width: `${Math.round(ratio * 100)}%` }}
                    />
                  </div>
                  <StreamButton
                    title={item.title}
                    tmdbId={item.tmdbId}
                    mediaType={item.mediaType}
                    season={item.season}
                    episode={item.episode}
                    posterPath={item.posterPath}
                    backdropPath={item.backdropPath}
                    variant="compact"
                    className={cn(
                      "h-7 w-full border-0 bg-white text-[10px] font-semibold text-black hover:bg-white/90 hover:text-black",
                    )}
                  />
                </div>
              </div>
              <p className="mt-2 line-clamp-2 text-sm font-medium">{item.title}</p>
              <p className="text-xs text-muted-foreground">
                {item.mediaType === "tv" && item.season && item.episode
                  ? `S${item.season} E${item.episode} · ${formatTimecode(item.seconds)}`
                  : formatTimecode(item.seconds)}
              </p>
            </div>
          );
        })}
      </div>
    </section>
  );
}

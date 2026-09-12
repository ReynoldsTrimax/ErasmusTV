"use client";

import * as React from "react";
import Image from "next/image";
import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import { Star } from "lucide-react";

import { StreamButton } from "@/features/streaming/components/stream-button";
import { PlanToWatchButton } from "@/features/watchlist/plan-button";

import { cn } from "@/lib/utils";
import { posterUrl } from "@/lib/media/image";
import { formatNumber, formatVote, formatYear } from "@/lib/media/format";
import { mediaHref } from "@/lib/media/routes";
import type { MediaSummary } from "@/types/media";

interface PosterCardProps {
  item: MediaSummary;
  className?: string;
  priority?: boolean;
  size?: "sm" | "md" | "lg";
  quickActions?: boolean;
  rank?: number;
  imdbRating?: number | null;
  imdbVotes?: number | null;
}

const sizeClass = {
  sm: "w-[7.5rem] sm:w-32",
  md: "w-36 sm:w-40",
  lg: "w-40 sm:w-48",
};

const popTransition = {
  type: "spring" as const,
  stiffness: 260,
  damping: 30,
  mass: 0.9,
};

export function PosterCard({
  item,
  className,
  priority,
  size = "md",
  rank,
  imdbRating,
  imdbVotes,
}: PosterCardProps) {
  const reduceMotion = useReducedMotion();
  const [loaded, setLoaded] = React.useState(false);
  const [hovered, setHovered] = React.useState(false);

  const href = mediaHref(item.mediaType, item.id);
  const src = posterUrl(item.posterPath, "w342");
  const year = formatYear(item.releaseDate);

  const motionTarget = reduceMotion
    ? { y: 0, scale: 1, zIndex: hovered ? 8 : 0 }
    : hovered
      ? { y: -12, scale: 1.12, zIndex: 40 }
      : { y: 0, scale: 1, zIndex: 0 };

  return (
    <motion.div
      className={cn(
        "group relative shrink-0 will-change-transform",
        sizeClass[size],
        className,
      )}
      style={{ transformOrigin: "50% 80%" }}
      animate={motionTarget}
      transition={reduceMotion ? { duration: 0.01 } : popTransition}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocusCapture={() => setHovered(true)}
      onBlurCapture={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) {
          setHovered(false);
        }
      }}
    >
      <div
        className={cn(
          "bg-muted relative aspect-[2/3] overflow-hidden rounded-xl shadow-md",
          "transition-shadow duration-500 ease-out",
          hovered && "shadow-2xl ring-1 shadow-black/50 ring-white/10",
        )}
      >
        <Link
          href={href}
          prefetch
          className="focus-visible:ring-ring absolute inset-0 z-0 focus-visible:ring-2 focus-visible:outline-none focus-visible:ring-inset"
          aria-label={`${rank != null ? `#${rank} ` : ""}${item.title}${
            year ? `, ${year}` : ""
          }${imdbRating != null ? `, IMDb ${imdbRating.toFixed(1)}` : ""}`}
        >
          {src ? (
            <>
              {!loaded ? (
                <div className="skeleton-shimmer absolute inset-0" aria-hidden />
              ) : null}
              <Image
                src={src}
                alt=""
                fill
                sizes="(max-width: 640px) 40vw, 160px"
                className={cn(
                  "object-cover transition-[transform,opacity] duration-500 ease-out",
                  hovered && "scale-[1.02]",
                  loaded ? "opacity-100" : "opacity-0",
                )}
                priority={priority}
                onLoad={() => setLoaded(true)}
              />
            </>
          ) : (
            <div className="bg-muted text-muted-foreground flex h-full items-center justify-center p-3 text-center text-xs">
              {item.title}
            </div>
          )}
        </Link>

        <div
          className={cn(
            "pointer-events-none absolute inset-x-0 bottom-0 z-[1] h-[55%] bg-gradient-to-t from-black/90 via-black/40 to-transparent transition-opacity duration-400 ease-out",
            hovered ? "opacity-100" : "opacity-0",
          )}
        />

        {rank != null ? (
          <span className="pointer-events-none absolute top-0 left-0 z-[1] rounded-br-lg bg-black/80 px-2 py-1 font-mono text-xs font-semibold text-white tabular-nums backdrop-blur-sm">
            {rank}
          </span>
        ) : item.voteAverage != null && item.voteAverage > 0 ? (
          <div className="pointer-events-none absolute top-2 left-2 z-[1] inline-flex items-center gap-1 rounded-md bg-black/65 px-1.5 py-0.5 text-[10px] font-medium text-white backdrop-blur-sm">
            <Star className="fill-primary text-primary h-3 w-3" aria-hidden="true" />
            {formatVote(item.voteAverage)}
          </div>
        ) : null}

        {imdbRating != null && !hovered ? (
          <span className="pointer-events-none absolute bottom-1.5 left-1.5 z-[1] inline-flex items-center gap-1 rounded bg-[#f5c518] px-1.5 py-0.5 text-[10px] font-bold text-black">
            IMDb {imdbRating.toFixed(1)}
          </span>
        ) : null}

        <div
          className={cn(
            "absolute inset-x-0 bottom-0 z-[3] flex gap-1 p-1.5 transition-opacity duration-200",
            hovered
              ? "pointer-events-auto opacity-100"
              : "pointer-events-none opacity-0 max-sm:pointer-events-auto max-sm:opacity-100",
          )}
        >
          <StreamButton
            title={item.title}
            tmdbId={item.id}
            mediaType={item.mediaType}
            posterPath={item.posterPath}
            backdropPath={item.backdropPath}
            variant="compact"
            className="h-7 flex-1 border-0 bg-white px-1.5 text-[10px] font-semibold text-black hover:bg-white/90 hover:text-black"
          />
          <PlanToWatchButton item={item} />
        </div>
      </div>

      <Link href={href} prefetch className="mt-2 block space-y-0.5 px-0.5">
        <p className="line-clamp-2 text-sm leading-snug font-medium tracking-tight">
          {item.title}
        </p>
        <p className="text-muted-foreground text-xs">
          {year ?? "—"}
          <span className="mx-1 opacity-40">·</span>
          {imdbVotes
            ? `${formatNumber(imdbVotes)} votes`
            : item.mediaType === "tv"
              ? "TV"
              : "Movie"}
        </p>
      </Link>
    </motion.div>
  );
}

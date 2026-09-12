"use client";

import type { ComponentType } from "react";
import { Clapperboard, Layers, Search, Star } from "lucide-react";

import { EdgeTraces } from "./electric-cta";
import { Reveal } from "./reveal";
import { SectionHeader } from "./section-header";

const CAPABILITIES = [
  {
    icon: Clapperboard,
    title: "Cinematic catalog",
    body: "Films and series with trailers, cast, collections, and where they are available to stream.",
  },
  {
    icon: Layers,
    title: "A player that belongs here",
    body: "Resume, subtitles, and a server picker when a source does not load — without leaving the title.",
  },
  {
    icon: Search,
    title: "Command-driven",
    body: "⌘K reaches titles, people, and genres. Keyboard first, always.",
  },
  {
    icon: Star,
    title: "Household profiles",
    body: "Switch who is watching without signing in again. Each profile keeps its own resume point.",
  },
] as const;

export function CapabilitiesSection() {
  return (
    <section
      id="capabilities"
      className="content-container relative scroll-mt-24 py-20 sm:py-28"
    >
      <SectionHeader
        title="Built to watch, not to log."
        lead="Dark, fast, and navigable from the keyboard — a home for discovery and playback."
      />

      <div className="mt-14 grid gap-4 sm:mt-16 sm:grid-cols-2 lg:grid-cols-4">
        {CAPABILITIES.map((item, i) => (
          <Reveal key={item.title} delay={i * 70} amount={0.15}>
            <CapabilityCard {...item} />
          </Reveal>
        ))}
      </div>
    </section>
  );
}

function CapabilityCard({
  icon: Icon,
  title,
  body,
}: {
  icon: ComponentType<{ className?: string }>;
  title: string;
  body: string;
}) {
  return (
    <article className="lx-card lx-corner h-full p-6">
      <div className="lx-card__bloom" aria-hidden="true" />
      <EdgeTraces />

      <Icon className="lx-icon h-5 w-5" aria-hidden="true" />
      <h3 className="font-display mt-6 text-lg font-semibold tracking-[-0.02em] text-white/95">
        {title}
      </h3>
      <p className="mt-3 text-sm leading-relaxed text-white/50">{body}</p>
    </article>
  );
}

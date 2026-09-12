"use client";

import { Hairline, Reveal } from "./reveal";
import { SectionHeader } from "./section-header";

const PRINCIPLES = [
  {
    key: "Discover",
    title: "Find the next title fast",
    body: "Trending films, series, and genres in one browse — then open a title and play.",
  },
  {
    key: "Watch",
    title: "Play without leaving the site",
    body: "A native player with resume, subtitles, and a server switch if a source fails.",
  },
  {
    key: "Profiles",
    title: "One account, several people",
    body: "Household profiles keep resume points and preferences separate, without extra logins.",
  },
] as const;

export function PremiseSection() {
  return (
    <section
      id="premise"
      className="content-container relative scroll-mt-24 py-20 sm:py-28"
    >
      <SectionHeader
        title="A streaming home, not a spreadsheet."
        lead="Browse the catalog, see where a title is available, and pick up exactly where you stopped — under the profile that is watching."
      />

      <div className="mt-14 grid gap-x-10 gap-y-12 sm:mt-16 sm:grid-cols-3">
        {PRINCIPLES.map((item, i) => (
          <div key={item.key}>
            <Hairline delay={i * 90} electric={i === 0} />
            <Reveal delay={i * 90 + 140} className="pt-7">
              <span className="landing-mono">{item.key}</span>
              <h3 className="font-display mt-4 text-xl font-semibold tracking-[-0.02em] text-white/95">
                {item.title}
              </h3>
              <p className="mt-3 text-sm leading-relaxed text-white/50">{item.body}</p>
            </Reveal>
          </div>
        ))}
      </div>
    </section>
  );
}

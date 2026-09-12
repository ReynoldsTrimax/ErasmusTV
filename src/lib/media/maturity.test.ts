import { describe, expect, it } from "vitest";

import {
  ageFromBirthYear,
  filterSummariesForAge,
  isTitleAllowedForAge,
  maturityFromBirthYear,
} from "./maturity";
import type { MediaSummary } from "@/types/media";

function title(partial: Partial<MediaSummary> & { title: string }): MediaSummary {
  return {
    id: partial.id ?? "1",
    mediaType: partial.mediaType ?? "movie",
    title: partial.title,
    posterPath: null,
    backdropPath: null,
    adult: partial.adult ?? false,
    genreIds: partial.genreIds,
  };
}

describe("maturity", () => {
  it("derives age from birth year", () => {
    expect(ageFromBirthYear(2014, new Date("2026-01-01"))).toBe(12);
  });

  it("caps young profiles at G and excludes horror", () => {
    const m = maturityFromBirthYear(2020);
    expect(m.movieCertLte).toBe("G");
    expect(m.excludeGenreIds).toContain("27");
  });

  it("hides adult and R-rated titles from a 12-year-old", () => {
    const m = maturityFromBirthYear(2014);
    expect(
      isTitleAllowedForAge({ adult: true, mediaType: "movie" }, m),
    ).toBe(false);
    expect(
      isTitleAllowedForAge(
        { certification: "R", mediaType: "movie", adult: false },
        m,
      ),
    ).toBe(false);
    expect(
      isTitleAllowedForAge(
        { certification: "PG", mediaType: "movie", adult: false },
        m,
      ),
    ).toBe(true);
  });

  it("filters a list by genre exclusions", () => {
    const m = maturityFromBirthYear(2020);
    const kept = filterSummariesForAge(
      [
        title({ id: "1", title: "Kids", genreIds: ["16"] }),
        title({ id: "2", title: "Scary", genreIds: ["27"] }),
      ],
      m,
    );
    expect(kept.map((t) => t.id)).toEqual(["1"]);
  });
});

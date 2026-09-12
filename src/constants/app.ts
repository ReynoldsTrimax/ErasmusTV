export const APP_NAME = "Erasmus";
export const APP_TAGLINE = "Watch what you want, when you want";
export const APP_DESCRIPTION =
  "Discover movies and TV shows, see where they stream, and play them in one place.";

export const APP_METADATA = {
  name: APP_NAME,
  tagline: APP_TAGLINE,
  description: APP_DESCRIPTION,
  locale: "en_US",
} as const;

export const LAYOUT = {
  headerHeight: 56,
  sidebarWidth: 260,
  sidebarCollapsedWidth: 64,
  contentMaxWidth: 1440,
} as const;

export const STORAGE_KEYS = {
  sidebarCollapsed: "argus:sidebar-collapsed",
  commandRecent: "argus:command-recent",
  animationIntensity: "argus:animation-intensity",
  posterDensity: "argus:poster-density",
  pinnedSearches: "argus:pinned-searches",
} as const;

export const PROFILE_COOKIE = "argus_profile";

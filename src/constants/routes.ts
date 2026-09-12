/**
 * Canonical application routes for the streaming site.
 */
export const ROUTES = {
  home: "/",
  login: "/login",
  signup: "/signup",
  browse: "/browse",
  profiles: "/profiles",
  settings: "/settings",
  offline: "/offline",
  authCallback: "/auth/callback",
  terms: "/terms",
  privacy: "/privacy",
  movies: "/movies",
  tv: "/tv",
  genres: "/genres",
  watchlist: "/watchlist",
  movie: (id: string | number) => `/movie/${id}`,
  show: (id: string | number) => `/tv/${id}`,
  person: (id: string | number) => `/person/${id}`,
  collection: (id: string | number) => `/collection/${id}`,
  genre: (id: string | number) => `/genre/${id}`,
} as const;

export type AppRoute = string;

/** Authenticated product routes. Catalog + player + profiles. */
export const PROTECTED_ROUTES: readonly string[] = [
  ROUTES.browse,
  ROUTES.profiles,
  ROUTES.settings,
  ROUTES.movies,
  ROUTES.tv,
  ROUTES.genres,
  ROUTES.watchlist,
  "/movie",
  "/tv",
  "/person",
  "/collection",
  "/genre",
];

/** Logged-in users with no selected profile are sent here. */
export const PROFILE_GATE_EXEMPT: readonly string[] = [
  ROUTES.profiles,
  ROUTES.login,
  ROUTES.signup,
  ROUTES.authCallback,
];

export const AUTH_ROUTES: readonly string[] = [ROUTES.login, ROUTES.signup];

/** Retired tracker URLs → browse, so old bookmarks do not 404. */
export const LEGACY_TRACKER_REDIRECTS: readonly string[] = [
  "/dashboard",
  "/discover",
  "/library",
  "/favorites",
  "/history",
  "/activity",
  "/collections",
  "/friends",
  "/stats",
  "/insights",
  "/recommendations",
  "/calendar",
  "/timeline",
  "/wrapped",
  "/recap",
  "/profile",
];

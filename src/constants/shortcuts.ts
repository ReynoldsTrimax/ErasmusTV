import { ROUTES } from "@/constants/routes";

export interface ShortcutDefinition {
  id: string;
  keys: string[];
  description: string;
  sequence?: string[];
  href?: string;
  action?: "command" | "search" | "theme" | "sidebar";
}

export const SHORTCUTS: readonly ShortcutDefinition[] = [
  {
    id: "command",
    keys: ["⌘", "K"],
    description: "Search movies and TV",
    action: "command",
  },
  {
    id: "search",
    keys: ["/"],
    description: "Focus search",
    action: "search",
  },
  {
    id: "browse",
    keys: ["G", "H"],
    description: "Go to Home",
    sequence: ["g", "h"],
    href: ROUTES.browse,
  },
  {
    id: "movies",
    keys: ["G", "M"],
    description: "Go to Movies",
    sequence: ["g", "m"],
    href: ROUTES.movies,
  },
  {
    id: "tv",
    keys: ["G", "T"],
    description: "Go to TV Shows",
    sequence: ["g", "t"],
    href: ROUTES.tv,
  },
  {
    id: "watchlist",
    keys: ["G", "W"],
    description: "Go to Watchlist",
    sequence: ["g", "w"],
    href: ROUTES.watchlist,
  },
  {
    id: "profiles",
    keys: ["G", "P"],
    description: "Switch profile",
    sequence: ["g", "p"],
    href: ROUTES.profiles,
  },
  {
    id: "settings",
    keys: ["G", ","],
    description: "Go to Settings",
    sequence: ["g", ","],
    href: ROUTES.settings,
  },
  {
    id: "escape",
    keys: ["Esc"],
    description: "Close modal / search",
  },
] as const;

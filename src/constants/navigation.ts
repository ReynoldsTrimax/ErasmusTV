import {
  Bookmark,
  Clapperboard,
  Compass,
  Film,
  Settings,
  Tv,
  Users,
  type LucideIcon,
} from "lucide-react";

import { ROUTES } from "./routes";

export interface NavItem {
  title: string;
  href: string;
  icon: LucideIcon;
  comingSoon?: boolean;
  description?: string;
}

export const MAIN_NAV: readonly NavItem[] = [
  {
    title: "Home",
    href: ROUTES.browse,
    icon: Compass,
    description: "Discover what to watch",
  },
  {
    title: "Movies",
    href: ROUTES.movies,
    icon: Film,
    description: "Browse movies",
  },
  {
    title: "TV Shows",
    href: ROUTES.tv,
    icon: Tv,
    description: "Browse series",
  },
  {
    title: "Genres",
    href: ROUTES.genres,
    icon: Clapperboard,
    description: "Browse by genre",
  },
  {
    title: "Watchlist",
    href: ROUTES.watchlist,
    icon: Bookmark,
    description: "Plan to watch",
  },
] as const;

export const SECONDARY_NAV: readonly NavItem[] = [
  {
    title: "Profiles",
    href: ROUTES.profiles,
    icon: Users,
    description: "Switch or manage profiles",
  },
  {
    title: "Settings",
    href: ROUTES.settings,
    icon: Settings,
    description: "Account preferences",
  },
] as const;

export const MARKETING_NAV: readonly NavItem[] = [] as const;

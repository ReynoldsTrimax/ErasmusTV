export const WATCH_PROFILE_AVATARS = [
  { key: "slate", label: "Slate", from: "#334155", to: "#0f172a" },
  { key: "ocean", label: "Ocean", from: "#0369a1", to: "#082f49" },
  { key: "violet", label: "Violet", from: "#6d28d9", to: "#2e1065" },
  { key: "rose", label: "Rose", from: "#be123c", to: "#4c0519" },
  { key: "amber", label: "Amber", from: "#d97706", to: "#451a03" },
  { key: "forest", label: "Forest", from: "#047857", to: "#022c22" },
  { key: "crimson", label: "Crimson", from: "#e11d48", to: "#4c0519" },
  { key: "indigo", label: "Indigo", from: "#4338ca", to: "#1e1b4b" },
] as const;

export type WatchProfileAvatarKey = (typeof WATCH_PROFILE_AVATARS)[number]["key"];

export const MAX_WATCH_PROFILES = 5;

export interface WatchProfilePreferences {
  watchRegion?: string;
  language?: string;
}

export interface WatchProfile {
  id: string;
  user_id: string;
  name: string;
  avatar_key: WatchProfileAvatarKey;
  /** Calendar year of birth. Age is derived at read time. */
  birth_year: number | null;
  preferences: WatchProfilePreferences;
  created_at: string;
  updated_at: string;
}

export function avatarGradient(key: string): { from: string; to: string } {
  const found = WATCH_PROFILE_AVATARS.find((item) => item.key === key);
  return found ?? { from: "#334155", to: "#0f172a" };
}

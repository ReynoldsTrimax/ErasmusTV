import { cookies } from "next/headers";

import { PROFILE_COOKIE } from "@/constants/app";

export const PROFILE_COOKIE_MAX_AGE = 60 * 60 * 24 * 365;

export function profileCookieOptions() {
  return {
    httpOnly: false,
    sameSite: "lax" as const,
    path: "/",
    maxAge: PROFILE_COOKIE_MAX_AGE,
    secure: process.env.NODE_ENV === "production",
  };
}

export async function readProfileCookie(): Promise<string | null> {
  const store = await cookies();
  return store.get(PROFILE_COOKIE)?.value ?? null;
}

export async function writeProfileCookie(profileId: string): Promise<void> {
  const store = await cookies();
  store.set(PROFILE_COOKIE, profileId, profileCookieOptions());
}

export async function clearProfileCookie(): Promise<void> {
  const store = await cookies();
  store.delete(PROFILE_COOKIE);
}

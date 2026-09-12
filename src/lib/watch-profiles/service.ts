import { createClient } from "@/lib/supabase/server";
import { requireUser } from "@/lib/services/user-service";
import { readProfileCookie } from "@/lib/watch-profiles/cookie";
import type {
  WatchProfile,
  WatchProfileAvatarKey,
  WatchProfilePreferences,
} from "@/types/watch-profile";
import { MAX_WATCH_PROFILES } from "@/types/watch-profile";

interface WatchProfileRow {
  id: string;
  user_id: string;
  name: string;
  avatar_key: string;
  birth_year: number | null;
  preferences: WatchProfilePreferences | null;
  created_at: string;
  updated_at: string;
}

function mapRow(row: WatchProfileRow): WatchProfile {
  return {
    id: row.id,
    user_id: row.user_id,
    name: row.name,
    avatar_key: (row.avatar_key || "slate") as WatchProfileAvatarKey,
    birth_year: row.birth_year ?? null,
    preferences: row.preferences ?? {},
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
}

export function birthYearFromAge(age: number, now = new Date()): number {
  return now.getFullYear() - age;
}

function profilesTable(client: Awaited<ReturnType<typeof createClient>>) {
  return client.from("watch_profiles" as never);
}

export async function listWatchProfiles(userId?: string): Promise<WatchProfile[]> {
  const user = userId ? { id: userId } : await requireUser();
  const client = await createClient();
  const { data, error } = await profilesTable(client)
    .select("*")
    .eq("user_id", user.id)
    .order("created_at", { ascending: true });

  if (error) throw new Error(error.message);
  return ((data ?? []) as WatchProfileRow[]).map(mapRow);
}

export async function getWatchProfile(
  profileId: string,
): Promise<WatchProfile | null> {
  const user = await requireUser();
  const client = await createClient();
  const { data, error } = await profilesTable(client)
    .select("*")
    .eq("id", profileId)
    .eq("user_id", user.id)
    .maybeSingle();

  if (error) throw new Error(error.message);
  return data ? mapRow(data as WatchProfileRow) : null;
}

export async function createWatchProfile(input: {
  name: string;
  avatarKey: string;
  age: number;
}): Promise<WatchProfile> {
  const user = await requireUser();
  const existing = await listWatchProfiles(user.id);
  if (existing.length >= MAX_WATCH_PROFILES) {
    throw new Error(`You can have up to ${MAX_WATCH_PROFILES} profiles.`);
  }

  const client = await createClient();
  const { data, error } = await profilesTable(client)
    .insert({
      user_id: user.id,
      name: input.name.trim(),
      avatar_key: input.avatarKey,
      birth_year: birthYearFromAge(input.age),
      preferences: {},
    } as never)
    .select("*")
    .single();

  if (error) {
    if (error.message.toLowerCase().includes("unique")) {
      throw new Error("That name is already used on this account.");
    }
    throw new Error(error.message);
  }
  return mapRow(data as WatchProfileRow);
}

export async function updateWatchProfile(
  profileId: string,
  input: { name: string; avatarKey: string; age: number },
): Promise<WatchProfile> {
  const user = await requireUser();
  const client = await createClient();
  const { data, error } = await profilesTable(client)
    .update({
      name: input.name.trim(),
      avatar_key: input.avatarKey,
      birth_year: birthYearFromAge(input.age),
      updated_at: new Date().toISOString(),
    } as never)
    .eq("id", profileId)
    .eq("user_id", user.id)
    .select("*")
    .single();

  if (error) {
    if (error.message.toLowerCase().includes("unique")) {
      throw new Error("That name is already used on this account.");
    }
    throw new Error(error.message);
  }
  return mapRow(data as WatchProfileRow);
}

export async function deleteWatchProfile(profileId: string): Promise<void> {
  const user = await requireUser();
  const remaining = await listWatchProfiles(user.id);
  if (remaining.length <= 1) {
    throw new Error("Keep at least one profile on this account.");
  }

  const client = await createClient();
  const { error } = await profilesTable(client)
    .delete()
    .eq("id", profileId)
    .eq("user_id", user.id);

  if (error) throw new Error(error.message);
}

export async function getActiveWatchProfile(): Promise<WatchProfile | null> {
  const id = await readProfileCookie();
  if (!id) return null;
  return getWatchProfile(id);
}

export async function ensureDefaultWatchProfile(): Promise<WatchProfile> {
  const user = await requireUser();
  const existing = await listWatchProfiles(user.id);
  if (existing[0]) return existing[0];
  return createWatchProfile({ name: "Profile 1", avatarKey: "crimson", age: 18 });
}

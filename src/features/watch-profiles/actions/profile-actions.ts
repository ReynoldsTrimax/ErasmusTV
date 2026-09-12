"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";

import { ROUTES } from "@/constants/routes";
import { watchProfileSchema } from "@/lib/validations/watch-profile";
import {
  clearProfileCookie,
  readProfileCookie,
  writeProfileCookie,
} from "@/lib/watch-profiles/cookie";
import {
  createWatchProfile,
  deleteWatchProfile,
  getWatchProfile,
  listWatchProfiles,
  updateWatchProfile,
} from "@/lib/watch-profiles/service";
import type { ActionResult } from "@/types";

export async function actionSelectProfile(profileId: string): Promise<ActionResult> {
  const profile = await getWatchProfile(profileId);
  if (!profile) {
    return { success: false, error: "That profile is not on this account." };
  }
  await writeProfileCookie(profile.id);
  revalidatePath("/", "layout");
  redirect(ROUTES.browse);
}

export async function actionCreateProfile(formData: FormData): Promise<ActionResult> {
  const parsed = watchProfileSchema.safeParse({
    name: formData.get("name"),
    avatarKey: formData.get("avatarKey") || "slate",
    age: formData.get("age"),
  });
  if (!parsed.success) {
    return {
      success: false,
      error: parsed.error.issues[0]?.message ?? "Check the profile details.",
    };
  }

  try {
    const profile = await createWatchProfile({
      name: parsed.data.name,
      avatarKey: parsed.data.avatarKey,
      age: parsed.data.age,
    });
    await writeProfileCookie(profile.id);
    revalidatePath(ROUTES.profiles);
    revalidatePath("/", "layout");
    return { success: true, data: undefined };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Could not create profile.",
    };
  }
}

export async function actionUpdateProfile(
  profileId: string,
  formData: FormData,
): Promise<ActionResult> {
  const parsed = watchProfileSchema.safeParse({
    name: formData.get("name"),
    avatarKey: formData.get("avatarKey") || "slate",
    age: formData.get("age"),
  });
  if (!parsed.success) {
    return {
      success: false,
      error: parsed.error.issues[0]?.message ?? "Check the profile details.",
    };
  }

  try {
    await updateWatchProfile(profileId, {
      name: parsed.data.name,
      avatarKey: parsed.data.avatarKey,
      age: parsed.data.age,
    });
    revalidatePath(ROUTES.profiles);
    revalidatePath("/", "layout");
    return { success: true, data: undefined };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Could not update profile.",
    };
  }
}

export async function actionDeleteProfile(profileId: string): Promise<ActionResult> {
  try {
    const active = await readProfileCookie();
    await deleteWatchProfile(profileId);
    if (active === profileId) {
      const remaining = await listWatchProfiles();
      if (remaining[0]) await writeProfileCookie(remaining[0].id);
      else await clearProfileCookie();
    }
    revalidatePath(ROUTES.profiles);
    revalidatePath("/", "layout");
    return { success: true, data: undefined };
  } catch (error) {
    return {
      success: false,
      error: error instanceof Error ? error.message : "Could not delete profile.",
    };
  }
}

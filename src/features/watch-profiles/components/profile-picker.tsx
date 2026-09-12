"use client";

import * as React from "react";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  actionCreateProfile,
  actionDeleteProfile,
  actionSelectProfile,
  actionUpdateProfile,
} from "@/features/watch-profiles/actions/profile-actions";
import { ProfileAvatar } from "@/features/watch-profiles/components/profile-avatar";
import { cn } from "@/lib/utils";
import { ageFromBirthYear } from "@/lib/media/maturity";
import {
  MAX_WATCH_PROFILES,
  WATCH_PROFILE_AVATARS,
  type WatchProfile,
} from "@/types/watch-profile";

interface ProfilePickerProps {
  profiles: WatchProfile[];
  activeProfileId?: string | null;
}

export function ProfilePicker({ profiles, activeProfileId }: ProfilePickerProps) {
  const [mode, setMode] = React.useState<"pick" | "create" | "edit">("pick");
  const [editing, setEditing] = React.useState<WatchProfile | null>(null);
  const [pending, startTransition] = React.useTransition();

  const canAdd = profiles.length < MAX_WATCH_PROFILES;

  const select = (id: string) => {
    startTransition(async () => {
      const result = await actionSelectProfile(id);
      if (result && !result.success) toast.error(result.error);
    });
  };

  if (mode === "create" || mode === "edit") {
    return (
      <ProfileForm
        profile={mode === "edit" ? editing : null}
        pending={pending}
        onCancel={() => {
          setMode("pick");
          setEditing(null);
        }}
        onSubmit={(formData) => {
          startTransition(async () => {
            const result =
              mode === "edit" && editing
                ? await actionUpdateProfile(editing.id, formData)
                : await actionCreateProfile(formData);
            if (!result.success) {
              toast.error(result.error);
              return;
            }
            toast.success(mode === "edit" ? "Profile updated" : "Profile created");
            setMode("pick");
            setEditing(null);
          });
        }}
      />
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col items-center px-4 py-16 sm:py-24">
      <h1 className="font-display text-3xl font-semibold tracking-tight text-white sm:text-4xl">
        Who&apos;s watching?
      </h1>
      <p className="mt-2 max-w-md text-center text-sm text-white/60">
        Each profile has its own watchlist, resume point, and age rating.
      </p>

      <ul className="mt-12 flex flex-wrap items-start justify-center gap-8">
        {profiles.map((profile) => (
          <li key={profile.id} className="flex w-28 flex-col items-center gap-3">
            <button
              type="button"
              onClick={() => select(profile.id)}
              disabled={pending}
              className={cn(
                "group rounded-full p-1 transition duration-150",
                "focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none",
                activeProfileId === profile.id
                  ? "ring-2 ring-white"
                  : "hover:ring-2 hover:ring-white/40",
              )}
            >
              <ProfileAvatar
                name={profile.name}
                avatarKey={profile.avatar_key}
                size="lg"
                className="transition duration-150 group-hover:scale-[1.04] group-active:scale-[0.98]"
              />
            </button>
            <p className="w-full truncate text-center text-sm font-medium text-white/90">
              {profile.name}
            </p>
            <div className="flex gap-1">
              <Button
                type="button"
                size="icon-sm"
                variant="ghost"
                className="text-white/50 hover:text-white"
                aria-label={`Edit ${profile.name}`}
                onClick={() => {
                  setEditing(profile);
                  setMode("edit");
                }}
              >
                <Pencil className="h-3.5 w-3.5" />
              </Button>
              {profiles.length > 1 ? (
                <Button
                  type="button"
                  size="icon-sm"
                  variant="ghost"
                  className="text-white/50 hover:text-destructive"
                  aria-label={`Delete ${profile.name}`}
                  disabled={pending}
                  onClick={() => {
                    startTransition(async () => {
                      const result = await actionDeleteProfile(profile.id);
                      if (!result.success) toast.error(result.error);
                      else toast.success("Profile removed");
                    });
                  }}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>
              ) : null}
            </div>
          </li>
        ))}

        {canAdd ? (
          <li className="flex w-28 flex-col items-center gap-3">
            <button
              type="button"
              onClick={() => setMode("create")}
              className="flex h-24 w-24 items-center justify-center rounded-full border border-dashed border-white/25 text-white/60 transition duration-150 hover:border-white/60 hover:text-white focus-visible:ring-2 focus-visible:ring-primary focus-visible:outline-none"
              aria-label="Add profile"
            >
              <Plus className="h-8 w-8" />
            </button>
            <p className="text-sm font-medium text-white/50">Add profile</p>
          </li>
        ) : null}
      </ul>
    </div>
  );
}

function ProfileForm({
  profile,
  pending,
  onCancel,
  onSubmit,
}: {
  profile: WatchProfile | null;
  pending: boolean;
  onCancel: () => void;
  onSubmit: (data: FormData) => void;
}) {
  const [avatarKey, setAvatarKey] = React.useState(profile?.avatar_key ?? "crimson");

  return (
    <form
      className="mx-auto w-full max-w-md space-y-6 px-4 py-16"
      onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        data.set("avatarKey", avatarKey);
        onSubmit(data);
      }}
    >
      <h2 className="font-display text-2xl font-semibold text-white">
        {profile ? "Edit profile" : "Add profile"}
      </h2>
      <div className="flex justify-center">
        <ProfileAvatar
          name={profile?.name || "New"}
          avatarKey={avatarKey}
          size="lg"
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="profile-name">Name</Label>
        <Input
          id="profile-name"
          name="name"
          required
          maxLength={32}
          defaultValue={profile?.name ?? ""}
          placeholder="e.g. Alex"
          className="h-11"
        />
      </div>
      <div className="space-y-2">
        <Label htmlFor="profile-age">Age</Label>
        <Input
          id="profile-age"
          name="age"
          type="number"
          required
          min={1}
          max={120}
          inputMode="numeric"
          defaultValue={
            profile?.birth_year ? ageFromBirthYear(profile.birth_year) : undefined
          }
          placeholder="How old is this person?"
          className="h-11"
        />
        <p className="text-xs text-white/45">
          Used to hide movies and shows that do not fit this profile.
        </p>
      </div>
      <fieldset className="space-y-2">
        <legend className="text-sm font-medium">Avatar</legend>
        <div className="flex flex-wrap gap-2">
          {WATCH_PROFILE_AVATARS.map((avatar) => (
            <button
              key={avatar.key}
              type="button"
              aria-label={avatar.label}
              aria-pressed={avatarKey === avatar.key}
              onClick={() => setAvatarKey(avatar.key)}
              className={cn(
                "rounded-full p-0.5",
                avatarKey === avatar.key ? "ring-2 ring-white" : "ring-1 ring-white/15",
              )}
            >
              <ProfileAvatar name={avatar.label} avatarKey={avatar.key} size="sm" />
            </button>
          ))}
        </div>
      </fieldset>
      <div className="flex gap-3">
        <Button type="submit" disabled={pending} className="flex-1">
          {profile ? "Save" : "Create"}
        </Button>
        <Button type="button" variant="ghost" onClick={onCancel} disabled={pending}>
          Cancel
        </Button>
      </div>
    </form>
  );
}

"use client";

import Link from "next/link";
import { useTransition } from "react";
import { Check, Users } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ROUTES } from "@/constants/routes";
import { actionSelectProfile } from "@/features/watch-profiles/actions/profile-actions";
import { ProfileAvatar } from "@/features/watch-profiles/components/profile-avatar";
import type { WatchProfile } from "@/types/watch-profile";

interface ProfileSwitcherProps {
  profiles: WatchProfile[];
  active: WatchProfile | null;
}

export function ProfileSwitcher({ profiles, active }: ProfileSwitcherProps) {
  const [pending, startTransition] = useTransition();

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="h-9 gap-2 rounded-full px-1.5 pr-2"
          aria-label={
            active ? `Watching as ${active.name}` : "Choose profile"
          }
          disabled={pending}
        >
          {active ? (
            <ProfileAvatar
              name={active.name}
              avatarKey={active.avatar_key}
              size="sm"
            />
          ) : (
            <Users className="h-4 w-4" />
          )}
          <span className="hidden max-w-[7rem] truncate text-sm font-medium sm:inline">
            {active?.name ?? "Profile"}
          </span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>Profiles</DropdownMenuLabel>
        {profiles.map((profile) => (
          <DropdownMenuItem
            key={profile.id}
            onSelect={() => {
              if (profile.id === active?.id) return;
              startTransition(async () => {
                await actionSelectProfile(profile.id);
              });
            }}
          >
            <ProfileAvatar
              name={profile.name}
              avatarKey={profile.avatar_key}
              size="sm"
            />
            <span className="flex-1 truncate">{profile.name}</span>
            {profile.id === active?.id ? (
              <Check className="h-4 w-4 text-primary" />
            ) : null}
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem asChild>
          <Link href={ROUTES.profiles}>Manage profiles</Link>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

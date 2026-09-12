"use client";

import { CommandTrigger } from "@/components/layout/command-trigger";
import { MobileNav } from "@/components/layout/mobile-nav";
import { Logo } from "@/components/layout/logo";
import { UserMenu, type UserMenuUser } from "@/components/layout/user-menu";
import { ProfileSwitcher } from "@/features/watch-profiles/components/profile-switcher";
import { ROUTES } from "@/constants/routes";
import type { WatchProfile } from "@/types/watch-profile";

interface AppHeaderProps {
  user: UserMenuUser;
  profiles: WatchProfile[];
  activeProfile: WatchProfile | null;
}

export function AppHeader({ user, profiles, activeProfile }: AppHeaderProps) {
  return (
    <header className="sticky top-0 z-40 flex h-[var(--header-height)] items-center gap-2.5 border-b border-border bg-background/85 px-3 backdrop-blur-xl sm:gap-3 sm:px-5 dark:border-white/[0.08]">
      <div className="md:hidden">
        <MobileNav />
      </div>

      <div className="hidden min-w-0 md:block">
        <Logo href={ROUTES.browse} />
      </div>

      <div className="min-w-0 flex-1" aria-hidden />

      <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
        <CommandTrigger className="hidden md:inline-flex" />
        <CommandTrigger compact className="md:hidden" />
        <ProfileSwitcher profiles={profiles} active={activeProfile} />
        <UserMenu user={user} />
      </div>
    </header>
  );
}

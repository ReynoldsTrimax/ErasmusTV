import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";

import { ROUTES } from "@/constants/routes";
import { signOut } from "@/features/auth/actions/auth-actions";
import { ProfilePicker } from "@/features/watch-profiles/components/profile-picker";
import { getSessionContext } from "@/lib/services/user-service";
import { readProfileCookie } from "@/lib/watch-profiles/cookie";
import { listWatchProfiles } from "@/lib/watch-profiles/service";

export const metadata: Metadata = {
  title: "Who's watching?",
  description: "Choose a profile to start watching",
};

export default async function ProfilesPage() {
  const { user } = await getSessionContext();
  if (!user) redirect(ROUTES.login);

  const [profiles, activeId] = await Promise.all([
    listWatchProfiles(user.id),
    readProfileCookie(),
  ]);

  return (
    <div className="relative min-h-dvh">
      <header className="flex items-center justify-between px-6 py-5">
        <p className="font-display text-lg tracking-tight">Erasmus</p>
        <form action={signOut}>
          <button
            type="submit"
            className="text-sm text-white/55 transition hover:text-white"
          >
            Sign out
          </button>
        </form>
      </header>
      <ProfilePicker profiles={profiles} activeProfileId={activeId} />
      <p className="pb-10 text-center text-xs text-white/35">
        <Link href={ROUTES.settings} className="underline-offset-4 hover:text-white/70 hover:underline">
          Account settings
        </Link>
      </p>
    </div>
  );
}

import { redirect } from "next/navigation";

import { AppShell } from "@/components/layout/app-shell";
import { ROUTES } from "@/constants/routes";
import { getSessionContext } from "@/lib/services/user-service";
import {
  getActiveWatchProfile,
  listWatchProfiles,
} from "@/lib/watch-profiles/service";
import { listWatchlistKeys } from "@/lib/watchlist/service";

export default async function AppLayout({ children }: { children: React.ReactNode }) {
  let context: Awaited<ReturnType<typeof getSessionContext>>;

  try {
    context = await getSessionContext();
  } catch {
    redirect(ROUTES.login);
  }

  if (!context.user) {
    redirect(ROUTES.login);
  }

  const [profiles, activeProfile] = await Promise.all([
    listWatchProfiles(context.user.id),
    getActiveWatchProfile(),
  ]);
  const watchlistKeys = activeProfile
    ? await listWatchlistKeys(activeProfile.id)
    : [];

  const user = {
    email: context.user.email,
    displayName: context.profile?.display_name ?? context.user.user_metadata?.full_name,
    username: context.profile?.username,
    avatarUrl:
      context.profile?.avatar_url ??
      (context.user.user_metadata?.avatar_url as string | undefined),
  };

  return (
    <AppShell
      user={user}
      profiles={profiles}
      activeProfile={activeProfile}
      watchlistKeys={watchlistKeys}
    >
      {children}
    </AppShell>
  );
}

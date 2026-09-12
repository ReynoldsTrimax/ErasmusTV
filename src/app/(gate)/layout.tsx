import { redirect } from "next/navigation";

import { ROUTES } from "@/constants/routes";
import { getSessionContext } from "@/lib/services/user-service";

export default async function GateLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user } = await getSessionContext();
  if (!user) redirect(ROUTES.login);

  return (
    <div className="min-h-dvh bg-black text-white">
      {children}
    </div>
  );
}

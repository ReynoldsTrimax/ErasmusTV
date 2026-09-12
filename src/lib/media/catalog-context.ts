import { maturityFromBirthYear, type CatalogMaturity } from "@/lib/media/maturity";
import { getActiveWatchProfile } from "@/lib/watch-profiles/service";

export async function getActiveCatalogMaturity(): Promise<CatalogMaturity> {
  const profile = await getActiveWatchProfile();
  return maturityFromBirthYear(profile?.birth_year);
}

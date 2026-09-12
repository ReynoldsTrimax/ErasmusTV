import { NextResponse, type NextRequest } from "next/server";

import { requireApiUser } from "@/lib/api/guard";
import { getActiveCatalogMaturity } from "@/lib/media/catalog-context";
import { isMediaAllowedForMaturity } from "@/lib/media/title-certification";

export const dynamic = "force-dynamic";

export async function GET(request: NextRequest) {
  const auth = await requireApiUser();
  if (!auth.ok) return auth.response;

  const type = request.nextUrl.searchParams.get("type") === "tv" ? "tv" : "movie";
  const id = request.nextUrl.searchParams.get("id") ?? "";
  if (!id) {
    return NextResponse.json({ allowed: false }, { status: 400 });
  }

  const maturity = await getActiveCatalogMaturity();
  const allowed = await isMediaAllowedForMaturity(type, id, maturity);
  return NextResponse.json({ allowed });
}

export async function POST(request: NextRequest) {
  const auth = await requireApiUser();
  if (!auth.ok) return auth.response;

  let body: { items?: { mediaType?: string; id?: string }[] };
  try {
    body = (await request.json()) as { items?: { mediaType?: string; id?: string }[] };
  } catch {
    return NextResponse.json({ allowed: [] }, { status: 400 });
  }

  const maturity = await getActiveCatalogMaturity();
  const items = (body.items ?? []).slice(0, 40);
  const allowed: string[] = [];
  for (const item of items) {
    const mediaType = item.mediaType === "tv" ? "tv" : "movie";
    const id = item.id ?? "";
    if (!id) continue;
    if (await isMediaAllowedForMaturity(mediaType, id, maturity)) {
      allowed.push(`${mediaType}:${id}`);
    }
  }
  return NextResponse.json({ allowed });
}

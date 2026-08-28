import { NextRequest, NextResponse } from "next/server";
import { generateMptStory, MptUnavailableError, type MptStoryRequest } from "@/lib/moneyprinterturbo";

export const runtime = "nodejs";

export async function POST(request: NextRequest) {
  try {
    const body = await request.json() as Record<string, unknown>;
    const characters = Array.isArray(body.characterBible)
      ? body.characterBible.filter((item): item is MptStoryRequest["characterBible"][number] => Boolean(item) && typeof item === "object" && typeof (item as { name?: unknown }).name === "string")
      : [];

    const input: MptStoryRequest = {
      subject: typeof body.subject === "string" ? body.subject : "",
      language: typeof body.language === "string" ? body.language : "tr",
      sceneCount: typeof body.sceneCount === "number" ? body.sceneCount : 4,
      genre: typeof body.genre === "string" ? body.genre : "Hikâye",
      instruction: typeof body.instruction === "string" ? body.instruction : undefined,
      durationSeconds: typeof body.durationSeconds === "number" ? body.durationSeconds : 45,
      characterBible: characters,
    };

    return NextResponse.json(await generateMptStory(input));
  } catch (error) {
    const message = error instanceof Error ? error.message : "Hikâye içeriği oluşturulamadı.";
    const status = error instanceof MptUnavailableError ? error.status : 500;
    return NextResponse.json({ error: message }, { status });
  }
}

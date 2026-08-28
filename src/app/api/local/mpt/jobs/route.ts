import { NextRequest, NextResponse } from "next/server";
import { createMptVideo, MptUnavailableError, type MptVideoRequest } from "@/lib/moneyprinterturbo";

export const runtime = "nodejs";

function isAspect(value: unknown): value is MptVideoRequest["aspect"] {
  return value === "9:16" || value === "16:9" || value === "1:1";
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json() as Record<string, unknown>;
    const input: MptVideoRequest = {
      subject: typeof body.subject === "string" ? body.subject : "",
      script: typeof body.script === "string" ? body.script : "",
      aspect: isAspect(body.aspect) ? body.aspect : "9:16",
      language: typeof body.language === "string" ? body.language : "tr",
      voice: typeof body.voice === "string" ? body.voice : undefined,
      materialSource: body.materialSource === "local" || body.materialSource === "pixabay" || body.materialSource === "pexels" ? body.materialSource : "pexels",
      subtitleEnabled: body.subtitleEnabled !== false,
    };
    return NextResponse.json(await createMptVideo(input), { status: 202 });
  } catch (error) {
    const message = error instanceof Error ? error.message : "MoneyPrinterTurbo görevi başlatılamadı.";
    const status = error instanceof MptUnavailableError ? error.status : 500;
    return NextResponse.json({ error: message }, { status });
  }
}

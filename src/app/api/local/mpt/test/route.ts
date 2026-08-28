import { mkdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { NextResponse } from "next/server";

export const runtime = "nodejs";

const TEST_IMAGE_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAiklEQVR42u3asQ2AIBQFQDDG1gmcwi2s3MU5XMQB3MINHMYJxMJEIN5rabj8Fwogdv0Qak4TKg8AAABA3rR3C9OxlrbXfVxUqKIKpQf3ZdJlViEAAAAAAAAAAAAAAAAAAAAAAACAPwKeL3cLfChQoZISfTUAAAAAeHWMzudmAgAAAAAAAAAAAHlyAYsUCmKg8hxaAAAAAElFTkSuQmCC";

function getLocalMptBaseUrl() {
  if (process.env.VERCEL) throw new Error("Bu test yalnızca yerel PostFlow başlatıcısında çalışır.");
  const configured = process.env.POSTFLOW_MPT_API?.trim();
  const value = configured || (process.env.POSTFLOW_LOCAL_RUNTIME === "1" || process.env.NODE_ENV === "development" ? "http://127.0.0.1:8080" : "");
  if (!value) throw new Error("Yerel MoneyPrinterTurbo adresi bulunamadı. PostFlow Yerel Başlatıcıyı açın.");
  const url = new URL(value);
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "::1"].includes(url.hostname)) {
    throw new Error("Test güvenlik nedeniyle yalnızca bu bilgisayardaki MoneyPrinterTurbo ile çalışır.");
  }
  return url.toString().replace(/\/$/, "");
}

async function ensureTestMaterial() {
  const dir = path.join(process.cwd(), ".postflow-test");
  const file = path.join(dir, "postflow-test-bg.png");
  await mkdir(dir, { recursive: true });
  await writeFile(file, Buffer.from(TEST_IMAGE_BASE64, "base64"));
  return file;
}

export async function POST() {
  try {
    const baseUrl = getLocalMptBaseUrl();
    const materialPath = await ensureTestMaterial();
    const response = await fetch(`${baseUrl}/api/v1/videos`, {
      method: "POST",
      headers: { Accept: "application/json", "Content-Type": "application/json" },
      cache: "no-store",
      signal: AbortSignal.timeout(20_000),
      body: JSON.stringify({
        video_subject: "PostFlow ilk test videosu",
        video_script: "PostFlow ilk test videosu başladı. Yerel seslendirme ve render zincirini kontrol ediyoruz. Bu videoyu görüyorsanız üretim sistemi çalışıyor.",
        video_terms: "",
        video_aspect: "9:16",
        video_concat_mode: "sequential",
        video_transition_mode: "None",
        video_clip_duration: 5,
        video_count: 1,
        video_source: "local",
        video_materials: [{ provider: "local", url: materialPath, duration: 0 }],
        video_language: "tr",
        voice_name: "tr-TR-AhmetNeural",
        voice_volume: 1,
        voice_rate: 1,
        bgm_type: "random",
        bgm_file: "",
        bgm_volume: 0.12,
        subtitle_enabled: true,
        subtitle_position: "bottom",
        n_threads: 2,
        paragraph_number: 1,
      }),
    });

    const raw = await response.text();
    if (!response.ok) {
      return NextResponse.json({ error: raw || `MoneyPrinterTurbo ${response.status} hatası verdi.` }, { status: response.status });
    }

    let parsed: { data?: { task_id?: string }; message?: string };
    try {
      parsed = JSON.parse(raw) as { data?: { task_id?: string }; message?: string };
    } catch {
      return NextResponse.json({ error: "MoneyPrinterTurbo beklenen JSON yanıtını döndürmedi." }, { status: 502 });
    }

    const taskId = parsed.data?.task_id;
    if (!taskId) return NextResponse.json({ error: parsed.message || "Test görevi için task_id dönmedi." }, { status: 502 });
    return NextResponse.json({ taskId }, { status: 202 });
  } catch (error) {
    return NextResponse.json({ error: error instanceof Error ? error.message : "Test videosu başlatılamadı." }, { status: 500 });
  }
}

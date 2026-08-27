const MEDIA_ENGINE_URL = process.env.MEDIA_ENGINE_URL ?? "http://127.0.0.1:8787";

async function mediaFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${MEDIA_ENGINE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(`Media engine ${response.status}: ${message}`);
  }

  return response.json() as Promise<T>;
}

export type RenderPayload = {
  materials: string[];
  output_name?: string;
  aspect?: "9:16" | "16:9" | "1:1";
  fps?: number;
  clip_duration?: number;
  audio_path?: string | null;
  subtitle_path?: string | null;
  background_music_path?: string | null;
  background_music_volume?: number;
};

export async function mediaEngineHealth() {
  return mediaFetch<{ ok: boolean; service: string; version: string }>("/health");
}

export async function createVoice(text: string, voice = "tr-TR-AhmetNeural") {
  return mediaFetch<{ ok: boolean; path: string }>("/v1/voice", {
    method: "POST",
    body: JSON.stringify({ text, voice, output_name: `voice-${Date.now()}.mp3` }),
  });
}

export async function createSubtitles(text: string, duration: number) {
  return mediaFetch<{ ok: boolean; path: string }>("/v1/subtitles", {
    method: "POST",
    body: JSON.stringify({ text, duration, output_name: `subtitles-${Date.now()}.srt` }),
  });
}

export async function renderVideo(payload: RenderPayload) {
  return mediaFetch<{ ok: boolean; path: string }>("/v1/render", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

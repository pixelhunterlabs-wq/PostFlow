import "server-only";

export type VideoAspect = "9:16" | "16:9" | "1:1";
export type MaterialSource = "pexels" | "pixabay" | "local";

export type MptVideoRequest = {
  subject: string;
  script: string;
  aspect: VideoAspect;
  language: string;
  voice?: string;
  materialSource?: MaterialSource;
  subtitleEnabled: boolean;
};

export type MptStoryRequest = {
  subject: string;
  language: string;
  sceneCount: number;
  genre: string;
  instruction?: string;
  durationSeconds?: number;
  characterBible?: Array<{
    name: string;
    face?: string;
    hair?: string;
    ageAppearance?: string;
    bodyType?: string;
    outfit?: string;
    styleNotes?: string;
    negativePrompt?: string;
  }>;
};

export type MptStoryScene = {
  number: number;
  text: string;
  visualPrompt: string;
  characters: string[];
};

export type MptStoryResult = {
  title: string;
  summary: string;
  script: string;
  scenes: MptStoryScene[];
};

export type MptTaskStatus = {
  taskId: string;
  status: "queued" | "processing" | "completed" | "failed";
  progress: number;
  videos: string[];
  error?: string;
  failedStage?: string;
};

export class MptUnavailableError extends Error {
  constructor(message: string, readonly status = 503) {
    super(message);
    this.name = "MptUnavailableError";
  }
}

type MptEnvelope<T> = { status?: number; message?: string; data?: T };
type MptTaskData = {
  task_id?: string;
  state?: number;
  progress?: number;
  videos?: string[];
  combined_videos?: string[];
  error?: string;
  failed_stage?: string;
};

type ScriptResponse = { video_script?: string };

function translateMptError(message?: string) {
  if (!message) return undefined;
  if (message.toLowerCase().includes("failed to generate video search terms")) {
    return "Video arama terimleri oluşturulamadı. Yerel materyal kaynağını seçin veya MPT LLM ayarını kontrol edin.";
  }
  return message;
}

function translateFailedStage(stage?: string) {
  if (stage === "terms") return "Arama terimleri";
  if (stage === "materials") return "Materyaller";
  if (stage === "audio") return "Seslendirme";
  if (stage === "render") return "Render";
  return stage;
}

function getMptBaseUrl() {
  if (process.env.VERCEL) {
    throw new MptUnavailableError(
      "Video üretimi bilgisayarınızdaki yerel motorla çalışır. PostFlow Yerel Başlatıcı ile http://127.0.0.1:3000 adresini açın.",
    );
  }

  const configured = process.env.POSTFLOW_MPT_API?.trim();
  const localFallback = process.env.POSTFLOW_LOCAL_RUNTIME === "1" || process.env.NODE_ENV === "development"
    ? "http://127.0.0.1:8080"
    : "";
  const value = configured || localFallback;
  if (!value) {
    throw new MptUnavailableError("Yerel MoneyPrinterTurbo adresi ayarlı değil. PostFlow yerel başlatıcısını çalıştırın.");
  }

  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new MptUnavailableError("POSTFLOW_MPT_API geçerli bir HTTP adresi olmalıdır.");
  }
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "::1"].includes(url.hostname)) {
    throw new MptUnavailableError("MoneyPrinterTurbo bağlantısı güvenlik nedeniyle yalnızca bu bilgisayardaki loopback adresine açılır.");
  }
  return url.toString().replace(/\/$/, "");
}

async function requestMpt<T>(path: string, init?: RequestInit, timeoutMs = 20_000): Promise<T> {
  const baseUrl = getMptBaseUrl();
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: {
        Accept: "application/json",
        ...(init?.body ? { "Content-Type": "application/json" } : {}),
        ...(init?.headers ?? {}),
      },
      cache: "no-store",
      signal: AbortSignal.timeout(timeoutMs),
    });
  } catch {
    throw new MptUnavailableError("MoneyPrinterTurbo çalışmıyor. PostFlow yerel başlatıcısını açın ve MPT API'nin hazır olmasını bekleyin.");
  }

  const raw = await response.text();
  if (!response.ok) {
    throw new MptUnavailableError(raw || `MoneyPrinterTurbo ${response.status} hatası verdi.`, response.status);
  }
  try {
    return JSON.parse(raw) as T;
  } catch {
    throw new MptUnavailableError("MoneyPrinterTurbo beklenen JSON yanıtını döndürmedi.", 502);
  }
}

function outputUrl(baseUrl: string, value: string) {
  if (/^https?:\/\//i.test(value)) return value;
  const normalized = value.replace(/\\/g, "/");
  const taskStorageMatch = normalized.match(/(?:^|\/)storage\/tasks\/(.+)$/i);
  if (taskStorageMatch) return new URL(`/tasks/${taskStorageMatch[1]}`, baseUrl).toString();
  if (/^[A-Za-z]:\//.test(normalized)) return value;
  return new URL(normalized.startsWith("/") ? normalized : `/${normalized}`, baseUrl).toString();
}

function mapTask(data: MptTaskData): MptTaskStatus {
  const state = Number(data.state ?? 0);
  const status = state === 1 ? "completed" : state === -1 ? "failed" : state === 4 ? "processing" : "queued";
  const baseUrl = getMptBaseUrl();
  const videos = [...(data.videos ?? []), ...(data.combined_videos ?? [])]
    .filter((value, index, array) => Boolean(value) && array.indexOf(value) === index)
    .map((value) => outputUrl(baseUrl, value));
  return {
    taskId: data.task_id ?? "",
    status,
    progress: Math.max(0, Math.min(100, Number(data.progress ?? (status === "completed" ? 100 : 0)))),
    videos,
    error: translateMptError(data.error),
    failedStage: translateFailedStage(data.failed_stage),
  };
}

function resolveVoice(language: string, voice?: string) {
  if (!voice || voice === "Otomatik") {
    if (language === "tr") return "tr-TR-AhmetNeural";
    if (language === "ar") return "ar-SA-HamedNeural";
    return "en-US-GuyNeural";
  }
  if (voice === "Kadın") {
    if (language === "tr") return "tr-TR-EmelNeural";
    if (language === "ar") return "ar-SA-ZariyahNeural";
    return "en-US-JennyNeural";
  }
  if (voice === "Erkek") {
    if (language === "tr") return "tr-TR-AhmetNeural";
    if (language === "ar") return "ar-SA-HamedNeural";
    return "en-US-GuyNeural";
  }
  return voice;
}

function splitScriptIntoScenes(script: string, sceneCount: number) {
  const cleaned = script.replace(/```[a-z]*|```/gi, "").trim();
  let chunks = cleaned
    .split(/\n\s*\n+/)
    .map((part) => part.replace(/^\s*(?:sahne|scene|bölüm|paragraf)?\s*\d+[\s:.)-]*/i, "").trim())
    .filter(Boolean);

  if (chunks.length < sceneCount) {
    chunks = cleaned
      .split(/(?<=[.!?])\s+/)
      .map((part) => part.trim())
      .filter(Boolean);
  }

  if (!chunks.length) chunks = [cleaned];
  const result: string[] = [];
  for (let index = 0; index < sceneCount; index += 1) {
    const start = Math.floor((index * chunks.length) / sceneCount);
    const end = Math.max(start + 1, Math.floor(((index + 1) * chunks.length) / sceneCount));
    result.push(chunks.slice(start, end).join(" ").trim() || chunks[Math.min(index, chunks.length - 1)] || cleaned);
  }
  return result;
}

export async function getMptHealth() {
  const baseUrl = getMptBaseUrl();
  let response: Response;
  try {
    response = await fetch(`${baseUrl}/ping`, { cache: "no-store", signal: AbortSignal.timeout(3_500) });
  } catch {
    throw new MptUnavailableError("MoneyPrinterTurbo kapalı. PostFlow yerel başlatıcısını açın.");
  }
  const body = (await response.text()).trim();
  if (!response.ok || (body !== "pong" && body !== "\"pong\"")) {
    throw new MptUnavailableError("MoneyPrinterTurbo API sağlık kontrolünden geçemedi.");
  }
  return { ok: true, apiUrl: baseUrl, pipeline: "Qwen 2.5 3B + Edge TTS + local media + FFmpeg" };
}

export async function generateMptStory(input: MptStoryRequest): Promise<MptStoryResult> {
  const subject = input.subject.trim();
  if (!subject) throw new MptUnavailableError("Video konusu gerekli.", 400);
  const sceneCount = Math.max(2, Math.min(24, Math.round(input.sceneCount || 4)));
  const durationSeconds = Math.max(20, Math.min(1200, Math.round(input.durationSeconds || 45)));
  const characterText = (input.characterBible ?? [])
    .filter((character) => character.name.trim())
    .map((character) => `${character.name}: yüz ${character.face || "belirtilmedi"}; saç ${character.hair || "belirtilmedi"}; yaş görünümü ${character.ageAppearance || "belirtilmedi"}; vücut ${character.bodyType || "belirtilmedi"}; kıyafet ${character.outfit || "belirtilmedi"}; stil ${character.styleNotes || "belirtilmedi"}; kaçınılacaklar ${character.negativePrompt || "yok"}.`)
    .join("\n");

  const prompt = [
    `Yaklaşık ${durationSeconds} saniyelik bir video için tam ${sceneCount} anlatım paragrafı yaz.`,
    `Tür: ${input.genre}.`,
    input.instruction?.trim() ? `Ek talimat: ${input.instruction.trim()}` : "",
    characterText ? `Karakter sürekliliği için Character Bible:\n${characterText}` : "",
    "Her paragraf tek bir sahneye karşılık gelsin. Sadece anlatım metni üret; başlık, numara, markdown veya açıklama ekleme.",
    "İlk sahne güçlü bir kanca içersin, orta bölüm olay veya bilgi akışını ilerletsin, son sahne net bir kapanış yapsın.",
  ].filter(Boolean).join("\n\n");

  const response = await requestMpt<MptEnvelope<ScriptResponse>>("/api/v1/scripts", {
    method: "POST",
    body: JSON.stringify({
      video_subject: subject,
      video_language: input.language || "tr",
      paragraph_number: sceneCount,
      video_script_prompt: prompt,
      custom_system_prompt: "You are PostFlow's video script planner. Follow the user's requested language and produce clean narration only.",
    }),
  }, 120_000);

  const script = response.data?.video_script?.trim();
  if (!script) throw new MptUnavailableError(response.message || "Yerel Qwen modeli senaryo döndürmedi.", 502);
  const paragraphs = splitScriptIntoScenes(script, sceneCount);
  const names = (input.characterBible ?? []).map((character) => character.name.trim()).filter(Boolean);
  const scenes = paragraphs.map((text, index) => ({
    number: index + 1,
    text,
    visualPrompt: `${input.genre}, ${subject}, sahne ${index + 1}, sinematik kompozisyon, tutarlı ışık, temiz kadraj${names.length ? `, karakterler: ${names.join(", ")}` : ""}`,
    characters: names,
  }));

  return {
    title: subject.slice(0, 80),
    summary: `${subject} için yerel Qwen modeliyle ${sceneCount} sahnelik plan oluşturuldu.`,
    script: scenes.map((scene) => scene.text).join("\n\n"),
    scenes,
  };
}

export async function createMptVideo(input: MptVideoRequest) {
  const subject = input.subject.trim();
  const script = input.script.trim();
  if (!subject || !script) throw new MptUnavailableError("Video konusu ve metni gerekli.", 400);

  const response = await requestMpt<MptEnvelope<{ task_id?: string }>>("/api/v1/videos", {
    method: "POST",
    body: JSON.stringify({
      video_subject: subject,
      video_script: script,
      video_aspect: input.aspect,
      video_concat_mode: "sequential",
      video_clip_duration: 5,
      video_count: 1,
      video_source: input.materialSource ?? "local",
      video_language: input.language,
      voice_name: resolveVoice(input.language, input.voice),
      subtitle_enabled: input.subtitleEnabled,
    }),
  });
  const taskId = response.data?.task_id;
  if (!taskId) throw new MptUnavailableError(response.message || "MoneyPrinterTurbo görev kimliği döndürmedi.", 502);
  return { taskId };
}

export async function getMptTask(taskId: string) {
  if (!/^[a-zA-Z0-9-]{8,128}$/.test(taskId)) throw new MptUnavailableError("Geçersiz MPT görev kimliği.", 400);
  const response = await requestMpt<MptEnvelope<MptTaskData>>(`/api/v1/tasks/${encodeURIComponent(taskId)}`);
  if (!response.data) throw new MptUnavailableError(response.message || "MPT görev durumu bulunamadı.", 404);
  return mapTask({ ...response.data, task_id: response.data.task_id ?? taskId });
}

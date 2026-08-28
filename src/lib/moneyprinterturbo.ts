import "server-only";

export type MptVideoRequest = {
  subject: string;
  script: string;
  aspect: "9:16" | "16:9" | "1:1";
  language: string;
  voice?: string;
  materialSource?: "pexels" | "pixabay" | "local";
  subtitleEnabled: boolean;
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

function translateMptError(message?: string) {
  if (!message) return undefined;
  if (message.toLowerCase().includes("failed to generate video search terms")) return "Video arama terimleri oluşturulamadı. MPT sağlayıcı/LLM yapılandırmasını kontrol edin.";
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
  // A Vercel function runs on Vercel's network, never on the creator's PC.
  if (process.env.VERCEL) {
    throw new MptUnavailableError("MoneyPrinterTurbo yalnızca yerel PostFlow oturumunda kullanılabilir. Vercel üretimi bilgisayarınızdaki motora erişemez.");
  }

  const configured = process.env.POSTFLOW_MPT_API?.trim();
  const localFallback = process.env.POSTFLOW_LOCAL_RUNTIME === "1" || process.env.NODE_ENV === "development"
    ? "http://127.0.0.1:8080"
    : "";
  const value = configured || localFallback;
  if (!value) {
    throw new MptUnavailableError("Yerel MoneyPrinterTurbo adresi ayarlı değil. PostFlow yerel launcher'ını başlatın.");
  }

  let url: URL;
  try {
    url = new URL(value);
  } catch {
    throw new MptUnavailableError("POSTFLOW_MPT_API geçerli bir HTTP adresi olmalıdır.");
  }
  if (url.protocol !== "http:" || !["127.0.0.1", "localhost", "::1"].includes(url.hostname)) {
    throw new MptUnavailableError("MoneyPrinterTurbo adapter'ı yalnızca yerel loopback adresine bağlanır.");
  }
  return url.toString().replace(/\/$/, "");
}

async function requestMpt<T>(path: string, init?: RequestInit): Promise<T> {
  const baseUrl = getMptBaseUrl();
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers: { Accept: "application/json", ...(init?.body ? { "Content-Type": "application/json" } : {}), ...(init?.headers ?? {}) },
      cache: "no-store",
      signal: AbortSignal.timeout(12_000),
    });
  } catch {
    throw new MptUnavailableError("MoneyPrinterTurbo çalışmıyor. PostFlow yerel launcher'ını başlatın ve MPT API'nin hazır olmasını bekleyin.");
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
  return new URL(value.startsWith("/") ? value : `/${value}`, baseUrl).toString();
}

function mapTask(data: MptTaskData): MptTaskStatus {
  const state = data.state;
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

export async function getMptHealth() {
  const baseUrl = getMptBaseUrl();
  let response: Response;
  try {
    response = await fetch(`${baseUrl}/ping`, { cache: "no-store", signal: AbortSignal.timeout(3_500) });
  } catch {
    throw new MptUnavailableError("MoneyPrinterTurbo kapalı. Başlatmak için PostFlow yerel launcher'ını açın.");
  }
  const body = (await response.text()).trim();
  if (!response.ok || (body !== "pong" && body !== "\"pong\"")) {
    throw new MptUnavailableError("MoneyPrinterTurbo API sağlık kontrolünden geçemedi. Launcher logunu kontrol edin.");
  }
  return { ok: true, apiUrl: baseUrl };
}

export async function createMptVideo(input: MptVideoRequest) {
  const subject = input.subject.trim();
  const script = input.script.trim();
  if (!subject || !script) throw new MptUnavailableError("Video konusu ve metni gerekli.", 400);

  // This maps only documented v1.3.5 TaskVideoRequest fields. Character
  // reference images are intentionally not sent: MPT does not document a
  // character-reference contract and must not be presented as identity lock.
  const response = await requestMpt<MptEnvelope<{ task_id?: string }>>("/api/v1/videos", {
    method: "POST",
    body: JSON.stringify({
      video_subject: subject,
      video_script: script,
      video_aspect: input.aspect,
      video_concat_mode: "sequential",
      video_clip_duration: 5,
      video_count: 1,
      video_source: input.materialSource ?? "pexels",
      video_language: input.language,
      voice_name: input.voice || "",
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

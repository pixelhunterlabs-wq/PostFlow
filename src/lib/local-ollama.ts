import "server-only";

import { MptUnavailableError, type MptStoryRequest, type MptStoryResult } from "@/lib/moneyprinterturbo";

const OLLAMA_URL = "http://127.0.0.1:11434";
const OLLAMA_MODEL = "qwen2.5:3b";

function splitIntoScenes(script: string, sceneCount: number) {
  const cleaned = script.replace(/```[a-z]*|```/gi, "").trim();
  let chunks = cleaned.split(/\n\s*\n+/).map((part) => part.replace(/^\s*(?:sahne|scene|bölüm|paragraf)?\s*\d+[\s:.)-]*/i, "").trim()).filter(Boolean);
  if (chunks.length < sceneCount) chunks = cleaned.split(/(?<=[.!?])\s+/).map((part) => part.trim()).filter(Boolean);
  if (!chunks.length) chunks = [cleaned];
  const result: string[] = [];
  for (let index = 0; index < sceneCount; index += 1) {
    const start = Math.floor((index * chunks.length) / sceneCount);
    const end = Math.max(start + 1, Math.floor(((index + 1) * chunks.length) / sceneCount));
    result.push(chunks.slice(start, end).join(" ").trim() || chunks[Math.min(index, chunks.length - 1)] || cleaned);
  }
  return result;
}

export async function generateOllamaStory(input: MptStoryRequest): Promise<MptStoryResult> {
  if (process.env.VERCEL) throw new MptUnavailableError("Yerel Qwen modeli yalnızca PostFlow Yerel Başlatıcı ile kullanılabilir.");
  const subject = input.subject.trim();
  if (!subject) throw new MptUnavailableError("Video konusu gerekli.", 400);

  const sceneCount = Math.max(2, Math.min(24, Math.round(input.sceneCount || 4)));
  const durationSeconds = Math.max(20, Math.min(1200, Math.round(input.durationSeconds || 45)));
  const characterText = (input.characterBible ?? [])
    .filter((character) => character.name.trim())
    .map((character) => `${character.name}: yüz ${character.face || "belirtilmedi"}; saç ${character.hair || "belirtilmedi"}; yaş görünümü ${character.ageAppearance || "belirtilmedi"}; vücut ${character.bodyType || "belirtilmedi"}; kıyafet ${character.outfit || "belirtilmedi"}; stil ${character.styleNotes || "belirtilmedi"}; kaçınılacaklar ${character.negativePrompt || "yok"}.`)
    .join("\n");

  const languageName = input.language === "tr" ? "Türkçe" : input.language === "ar" ? "Arapça" : "İngilizce";
  const prompt = [
    `Konu: ${subject}`,
    `Dil: ${languageName}`,
    `Tür: ${input.genre}`,
    `Yaklaşık süre: ${durationSeconds} saniye`,
    `Tam ${sceneCount} anlatım paragrafı yaz. Her paragraf tek bir sahne olsun.`,
    input.instruction?.trim() ? `Ek talimat: ${input.instruction.trim()}` : "",
    characterText ? `Karakter sürekliliği:\n${characterText}` : "",
    "İlk sahne güçlü bir kanca olsun. Orta sahneler olayı ilerletsin. Son sahne net bir kapanış yapsın.",
    "Yalnızca anlatım metnini yaz. Başlık, numara, markdown, açıklama veya not ekleme.",
  ].filter(Boolean).join("\n\n");

  let response: Response;
  try {
    response = await fetch(`${OLLAMA_URL}/api/generate`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ model: OLLAMA_MODEL, prompt, stream: false, options: { temperature: 0.75 } }),
      cache: "no-store",
      signal: AbortSignal.timeout(120_000),
    });
  } catch {
    throw new MptUnavailableError("Ollama çalışmıyor. PostFlow Yerel Başlatıcıyı yeniden açın.");
  }

  const raw = await response.text();
  if (!response.ok) throw new MptUnavailableError(`Ollama hata verdi: ${raw || response.status}`, 502);
  let payload: { response?: string };
  try { payload = JSON.parse(raw) as { response?: string }; }
  catch { throw new MptUnavailableError("Ollama geçerli JSON döndürmedi.", 502); }

  const script = payload.response?.trim();
  if (!script) throw new MptUnavailableError("Yerel Qwen modeli senaryo döndürmedi.", 502);
  const paragraphs = splitIntoScenes(script, sceneCount);
  const names = (input.characterBible ?? []).map((character) => character.name.trim()).filter(Boolean);
  const scenes = paragraphs.map((text, index) => ({
    number: index + 1,
    text,
    visualPrompt: `${input.genre}, ${subject}, sahne ${index + 1}, sinematik kompozisyon, tutarlı ışık, temiz kadraj${names.length ? `, karakterler: ${names.join(", ")}` : ""}`,
    characters: names,
  }));

  return {
    title: subject.slice(0, 80),
    summary: `${subject} için Ollama ${OLLAMA_MODEL} ile ${sceneCount} sahnelik plan oluşturuldu.`,
    script: scenes.map((scene) => scene.text).join("\n\n"),
    scenes,
  };
}

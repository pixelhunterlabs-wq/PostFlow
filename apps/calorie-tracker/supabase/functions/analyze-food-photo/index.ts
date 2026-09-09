import "jsr:@supabase/functions-js/edge-runtime.d.ts";

type PhotoRequest = {
  imageBase64?: string;
  mimeType?: string;
};

type MealItem = {
  name: string;
  grams: number;
  calories: number;
  protein_g: number;
  carbs_g: number;
  fat_g: number;
  confidence: number;
};

const jsonHeaders = { "Content-Type": "application/json; charset=utf-8" };

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") {
    return new Response(JSON.stringify({ error: "method_not_allowed" }), { status: 405, headers: jsonHeaders });
  }

  const apiKey = Deno.env.get("OPENAI_API_KEY");
  if (!apiKey) {
    return new Response(JSON.stringify({ error: "ai_not_configured" }), { status: 503, headers: jsonHeaders });
  }

  let payload: PhotoRequest;
  try {
    payload = await req.json();
  } catch {
    return new Response(JSON.stringify({ error: "invalid_json" }), { status: 400, headers: jsonHeaders });
  }

  const imageBase64 = payload.imageBase64?.trim();
  const mimeType = payload.mimeType?.trim() || "image/jpeg";
  if (!imageBase64 || imageBase64.length < 100) {
    return new Response(JSON.stringify({ error: "missing_image" }), { status: 400, headers: jsonHeaders });
  }
  if (imageBase64.length > 8_000_000) {
    return new Response(JSON.stringify({ error: "image_too_large" }), { status: 413, headers: jsonHeaders });
  }
  if (!/^image\/(jpeg|jpg|png|webp)$/i.test(mimeType)) {
    return new Response(JSON.stringify({ error: "unsupported_image_type" }), { status: 415, headers: jsonHeaders });
  }

  const prompt = `Bu yemek fotoğrafını beslenme günlüğü için analiz et. Görülebilen yiyecekleri ayrı ayrı tahmin et. Porsiyon gramını ve yaklaşık kalori, protein, karbonhidrat ve yağ miktarını hesapla. Sonuçlar tahmindir. Yalnızca geçerli JSON döndür ve başka metin yazma. Şema: {"items":[{"name":"string","grams":number,"calories":number,"protein_g":number,"carbs_g":number,"fat_g":number,"confidence":0-1}],"total":{"grams":number,"calories":number,"protein_g":number,"carbs_g":number,"fat_g":number},"note":"string"}. Türkçe yiyecek adları kullan.`;

  const aiResponse = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${apiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      model: "gpt-5.6-luna",
      input: [{
        role: "user",
        content: [
          { type: "input_text", text: prompt },
          { type: "input_image", image_url: `data:${mimeType};base64,${imageBase64}` },
        ],
      }],
      max_output_tokens: 1400,
      text: { format: { type: "json_schema", name: "meal_analysis", strict: true, schema: { type: "object", additionalProperties: false, required: ["items", "note"], properties: { items: { type: "array", items: { type: "object", additionalProperties: false, required: ["name", "grams", "calories", "protein_g", "carbs_g", "fat_g", "confidence"], properties: { name: { type: "string" }, grams: { type: "number" }, calories: { type: "number" }, protein_g: { type: "number" }, carbs_g: { type: "number" }, fat_g: { type: "number" }, confidence: { type: "number" } } } }, note: { type: "string" } } } },
    }),
  });

  if (!aiResponse.ok) {
    const requestId = aiResponse.headers.get("x-request-id") ?? "none";
    const kind = aiResponse.status === 401 || aiResponse.status === 403 ? "openai_auth_failed" : aiResponse.status === 429 ? "openai_rate_limited" : aiResponse.status === 400 ? "openai_bad_request" : aiResponse.status >= 500 ? "openai_unavailable" : "ai_request_failed";
    console.error("OpenAI request failed", { status: aiResponse.status, requestId, kind });
    return new Response(JSON.stringify({ error: kind }), { status: aiResponse.status === 429 ? 429 : 502, headers: jsonHeaders });
  }

  const raw = await aiResponse.json();
  const outputText = (raw.output ?? [])
    .flatMap((entry: { content?: Array<{ type?: string; text?: string }> }) => entry.content ?? [])
    .find((part: { type?: string }) => part.type === "output_text")?.text ?? "";

  let analysis: { items?: MealItem[]; total?: Record<string, number>; note?: string };
  try {
    analysis = JSON.parse(outputText);
  } catch {
    console.error("Unable to parse AI JSON", outputText.slice(0, 1500));
    return new Response(JSON.stringify({ error: "invalid_ai_response" }), { status: 502, headers: jsonHeaders });
  }

  const items = (analysis.items ?? []).slice(0, 12).map((item) => ({
    name: String(item.name || "Yiyecek").slice(0, 120),
    grams: Math.max(0, Number(item.grams) || 0),
    calories: Math.max(0, Number(item.calories) || 0),
    protein_g: Math.max(0, Number(item.protein_g) || 0),
    carbs_g: Math.max(0, Number(item.carbs_g) || 0),
    fat_g: Math.max(0, Number(item.fat_g) || 0),
    confidence: Math.min(1, Math.max(0, Number(item.confidence) || 0)),
  }));

  const total = items.reduce((sum, item) => ({
    grams: sum.grams + item.grams,
    calories: sum.calories + item.calories,
    protein_g: sum.protein_g + item.protein_g,
    carbs_g: sum.carbs_g + item.carbs_g,
    fat_g: sum.fat_g + item.fat_g,
  }), { grams: 0, calories: 0, protein_g: 0, carbs_g: 0, fat_g: 0 });

  return new Response(JSON.stringify({
    items,
    total,
    note: String(analysis.note || "Fotoğraftan hesaplanan değerler yaklaşık tahmindir.").slice(0, 500),
  }), { status: 200, headers: jsonHeaders });
});

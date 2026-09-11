import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

Deno.serve(async (req) => {
  const barcode = String((await req.json().catch(() => ({}))).barcode ?? "").replace(/\D/g, "");
  if (![8, 12, 13].includes(barcode.length)) return Response.json({ error: "invalid_barcode" }, { status: 400 });
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);
  const variants = [...new Set([barcode, barcode.length === 12 ? `0${barcode}` : barcode, barcode.length === 13 && barcode.startsWith("0") ? barcode.slice(1) : barcode])];
  const { data: cached } = await admin.from("calorie_barcode_products").select().in("barcode", variants).limit(1).maybeSingle();
  if (cached) return Response.json({ product: cached, source: "cache" });
  try {
    const off = await fetch(`https://world.openfoodfacts.org/api/v2/product/${barcode}.json?fields=product_name,brands,nutriments`);
    const body = await off.json();
    if (body.status === 1 && body.product) {
      const n = body.product.nutriments ?? {}; const product = { barcode, product_name: body.product.product_name || "Barkodlu ürün", brand: body.product.brands || "", calories_100g: n["energy-kcal_100g"] ?? 0, protein_100g: n.proteins_100g ?? 0, carbs_100g: n.carbohydrates_100g ?? 0, fat_100g: n.fat_100g ?? 0, source: "open_food_facts" };
      await admin.from("calorie_barcode_products").upsert(product); return Response.json({ product, source: "open_food_facts" });
    }
  } catch { /* try next provider */ }
  const upcKey = Deno.env.get("UPCITEMDB_API_KEY");
  if (upcKey) try { const r = await fetch(`https://api.upcitemdb.com/prod/trial/lookup?upc=${barcode}`, { headers: { Authorization: `Bearer ${upcKey}` } }); const b = await r.json(); const item = b.items?.[0]; if (item) { const product = { barcode, product_name: item.title || "Barkodlu ürün", brand: item.brand || "", quantity_text: item.size || null, image_url: item.images?.[0] || null, source: "upcitemdb" }; await admin.from("calorie_barcode_products").upsert(product); return Response.json({ product, source: "upcitemdb", nutritionMissing: true }); } } catch { /* not found */ }
  return Response.json({ error: "not_found" }, { status: 404 });
});

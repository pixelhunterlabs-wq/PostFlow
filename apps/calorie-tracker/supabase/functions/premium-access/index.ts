import { authenticate, refreshAccess, json, errorResponse } from "../_shared/access.ts";
import { photoLimit } from "../_shared/policy.ts";
Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  try {
    const { admin, user } = await authenticate(req);
    const body = await req.json().catch(() => ({}));
    const access = await refreshAccess(admin, user.id);
    const month = new Date().toISOString().slice(0,7) + "-01";
    const { data: usage, error } = await admin.from("calorie_ai_monthly_usage").select("photo_analyses").eq("user_id", user.id).eq("month_start", month).maybeSingle();
    if (error) throw error;
    if (body.feature && !["voice", "reports", "weight_chart", "smart_goal"].includes(body.feature)) throw Error("invalid_request");
    if (body.feature && !access.premium) throw Error("premium_required");
    return json({ ...access, photoUsed: usage?.photo_analyses || 0, photoLimit: photoLimit(access.premium), month });
  } catch (e) { return errorResponse(e); }
});

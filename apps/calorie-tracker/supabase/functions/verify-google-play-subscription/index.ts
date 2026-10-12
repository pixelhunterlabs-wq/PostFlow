import { authenticate, verifyPurchase, refreshAccess, json, errorResponse } from "../_shared/access.ts";
Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);
  try {
    const { admin, user } = await authenticate(req);
    const body = await req.json().catch(() => ({}));
    if (typeof body.purchaseToken !== "string" || !body.purchaseToken.trim() || body.purchaseToken.length > 8192) throw Error("invalid_request");
    await verifyPurchase(admin, user.id, body.purchaseToken.trim());
    return json(await refreshAccess(admin, user.id));
  } catch (e) { return errorResponse(e); }
});

import { createClient } from "npm:@supabase/supabase-js@2.95.3";
import { googleAccessToken, sha256 } from "./google-play.ts";
import { subscriptionDecision } from "./policy.ts";
export const json = (value: unknown, status = 200) => new Response(JSON.stringify(value), {
  status, headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
});
export async function authenticate(req: Request) {
  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false, autoRefreshToken: false } });
  const jwt = (req.headers.get("Authorization") || "").replace(/^Bearer /, "");
  const { data: { user }, error } = await admin.auth.getUser(jwt);
  if (error || !user) throw new Error("authentication_required");
  return { admin, user };
}
export async function verifyPurchase(admin: any, userId: string, purchaseToken: string) {
  const account = Deno.env.get("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
  if (!account) throw new Error("billing_not_configured");
  const tokenHash = await sha256(purchaseToken);
  const access = await googleAccessToken(account);
  const base = "https://androidpublisher.googleapis.com/androidpublisher/v3/applications/com.pixelhunter.calorietracker";
  const response = await fetch(base + "/purchases/subscriptionsv2/tokens/" + encodeURIComponent(purchaseToken), { headers: { Authorization: "Bearer " + access }, signal: AbortSignal.timeout(15000) });
  if (response.status === 404 || response.status === 410) {
    const { error } = await admin.from("calorie_subscription_purchases").update({ subscription_state: "SUBSCRIPTION_STATE_EXPIRED", expires_at: new Date(0).toISOString(), verified_at: new Date().toISOString() }).eq("token_hash", tokenHash).eq("user_id", userId);
    if (error) throw Error("verification_unavailable");
    return { premium: false, plan: null, expiresAt: null };
  }
  if (!response.ok) throw Error("verification_unavailable");
  const verified = await response.json();
  if (verified.externalAccountIdentifiers?.obfuscatedExternalAccountId !== await sha256(userId)) throw Error("purchase_account_mismatch");
  const decision = subscriptionDecision(verified, Date.now());
  if (!decision) throw Error("unrecognized_subscription");
  if (decision.premium && verified.acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING") {
    const ack = await fetch(base + "/purchases/subscriptions/kalori_premium/tokens/" + encodeURIComponent(purchaseToken) + ":acknowledge", { method: "POST", headers: { Authorization: "Bearer " + access, "Content-Type": "application/json" }, body: "{}", signal: AbortSignal.timeout(15000) });
    if (!ack.ok) throw Error("purchase_acknowledgement_failed");
  }
  const { error } = await admin.rpc("calorie_record_verified_purchase", {
    p_user: userId, p_hash: tokenHash, p_token: purchaseToken, p_plan: decision.plan,
    p_state: verified.subscriptionState, p_expiry: decision.expiresAt,
    p_linked_hash: verified.linkedPurchaseToken ? await sha256(verified.linkedPurchaseToken) : null,
  });
  if (error) throw Error("purchase_already_linked");
  return decision;
}
export async function refreshAccess(admin: any, userId: string) {
  const { data: purchases, error } = await admin.from("calorie_subscription_purchases").select("purchase_token").eq("user_id", userId);
  if (error) throw Error("verification_unavailable");
  // Verify with Play on every protected operation; no client or stale JWT claims.
  for (const p of purchases || []) await verifyPurchase(admin, userId, p.purchase_token);
  const { data, error: readError } = await admin.from("calorie_subscription_purchases").select("base_plan_id,subscription_state,expires_at,verified_at").eq("user_id", userId);
  if (readError) throw Error("verification_unavailable");
  const active = (data || []).filter((p: any) => ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_CANCELED", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"].includes(p.subscription_state) && Date.parse(p.expires_at) > Date.now()).sort((a: any,b: any) => Date.parse(b.expires_at)-Date.parse(a.expires_at))[0];
  const expiresAt = active?.expires_at || new Date(0).toISOString();
  const { error: writeError } = await admin.from("calorie_subscription_entitlements").upsert({ user_id: userId, product_id: "kalori_premium", base_plan_id: active?.base_plan_id || "monthly", subscription_state: active?.subscription_state || "SUBSCRIPTION_STATE_EXPIRED", expires_at: expiresAt, verified_at: new Date().toISOString() });
  if (writeError) throw Error("verification_unavailable");
  return { premium: !!active, plan: active?.base_plan_id || null, expiresAt: active?.expires_at || null, canceled: active?.subscription_state === "SUBSCRIPTION_STATE_CANCELED" };
}
export function errorResponse(error: unknown) {
  const code = error instanceof Error ? error.message : "verification_unavailable";
  const safe = ["authentication_required", "billing_not_configured", "purchase_account_mismatch", "purchase_already_linked", "unrecognized_subscription", "purchase_acknowledgement_failed", "premium_required", "quota_exceeded", "invalid_request"];
  return json({ error: safe.includes(code) ? code : "verification_unavailable" }, code === "authentication_required" ? 401 : code === "quota_exceeded" ? 429 : code === "premium_required" || code === "purchase_account_mismatch" ? 403 : code === "invalid_request" ? 400 : 503);
}

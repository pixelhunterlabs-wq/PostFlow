export function subscriptionDecision(verified: any, now: number) {
  const item = (verified.lineItems || []).filter((i: any) => i.productId === "kalori_premium" && ["monthly", "yearly"].includes(i.offerDetails?.basePlanId) && Number.isFinite(Date.parse(i.expiryTime))).sort((a: any,b: any) => Date.parse(b.expiryTime)-Date.parse(a.expiryTime))[0];
  if (!item) return null;
  return { premium: ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_CANCELED", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"].includes(verified.subscriptionState) && Date.parse(item.expiryTime) > now, plan: item.offerDetails.basePlanId, expiresAt: item.expiryTime };
}
export function photoLimit(premium: boolean) { return premium ? 100 : 5; }

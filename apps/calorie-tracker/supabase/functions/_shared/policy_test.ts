import {subscriptionDecision,photoLimit} from "./policy.ts";
const now=Date.parse("2026-10-12T00:00:00Z");
function assert(value:unknown){if(!value)throw Error("assertion failed");}
function purchase(state:string,expiry="2026-11-12T00:00:00Z",plan="monthly",product="kalori_premium") {return {subscriptionState:state,lineItems:[{productId:product,offerDetails:{basePlanId:plan},expiryTime:expiry}]};}
Deno.test("active monthly and yearly receive Premium",()=>{for(const plan of ["monthly","yearly"]) assert(subscriptionDecision(purchase("SUBSCRIPTION_STATE_ACTIVE",undefined,plan),now)?.premium);});
Deno.test("cancel retains access only until expiry",()=>{assert(subscriptionDecision(purchase("SUBSCRIPTION_STATE_CANCELED"),now)?.premium);assert(!subscriptionDecision(purchase("SUBSCRIPTION_STATE_CANCELED","2026-10-11T00:00:00Z"),now)?.premium);});
Deno.test("pending paused hold expired and revoked deny",()=>{for(const state of ["SUBSCRIPTION_STATE_PENDING","SUBSCRIPTION_STATE_PAUSED","SUBSCRIPTION_STATE_ON_HOLD","SUBSCRIPTION_STATE_EXPIRED","REVOKED"]) assert(!subscriptionDecision(purchase(state),now)?.premium);});
Deno.test("grace is supported; expired active denies",()=>{assert(subscriptionDecision(purchase("SUBSCRIPTION_STATE_IN_GRACE_PERIOD"),now)?.premium);assert(!subscriptionDecision(purchase("SUBSCRIPTION_STATE_ACTIVE","2026-10-11T00:00:00Z"),now)?.premium);});
Deno.test("unknown products plans invalid expiry deny",()=>{assert(!subscriptionDecision(purchase("SUBSCRIPTION_STATE_ACTIVE",undefined,"other"),now));assert(!subscriptionDecision(purchase("SUBSCRIPTION_STATE_ACTIVE",undefined,undefined,"other"),now));assert(!subscriptionDecision(purchase("SUBSCRIPTION_STATE_ACTIVE","garbage"),now));});
Deno.test("free and premium quota",()=>{assert(photoLimit(false)===5);assert(photoLimit(true)===100);});

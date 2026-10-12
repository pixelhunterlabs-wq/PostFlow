import { authenticate, refreshAccess, json, errorResponse } from "../_shared/access.ts";
Deno.serve(async (req: Request) => {
 if (req.method !== "POST") return json({error:"method_not_allowed"},405);
 try {
  const {admin,user} = await authenticate(req);
  const access = await refreshAccess(admin,user.id);
  if (!access.premium) throw Error("premium_required");
  const body=await req.json().catch(()=>({}));
  const days = ({WEEK:7,MONTH:30,YEAR:365} as Record<string,number>)[body.range || "MONTH"];
  if (!days || ![0,0.25,0.5,0.75].includes(body.targetWeeklyLoss ?? 0.25)) throw Error("invalid_request");
  const today=new Date().toISOString().slice(0,10), start=new Date(Date.parse(today)-86400000*(days-1)).toISOString();
  const [foods,weights,profile] = await Promise.all([
   admin.from("calorie_food_entries").select("calories,protein_g,carbs_g,fat_g,eaten_at").eq("user_id",user.id).gte("eaten_at",start).lt("eaten_at",new Date(Date.parse(today)+86400000).toISOString()).order("eaten_at").limit(10000),
   admin.from("calorie_weight_entries").select("*").eq("user_id",user.id).order("measured_at",{ascending:false}).limit(365),
   admin.from("calorie_profiles").select("daily_calorie_target").eq("user_id",user.id).single()
  ]);
  if (foods.error || weights.error || profile.error) throw Error("verification_unavailable");
  const goal=profile.data.daily_calorie_target || 2000;
  const chart=Array.from({length:days},(_,i)=>({date:new Date(Date.parse(start)+i*86400000).toISOString().slice(0,10),calories:0}));
  let protein=0,carbs=0,fat=0;
  for (const f of foods.data) {
   const day=chart.find(d=>d.date===f.eaten_at.slice(0,10));
   if(day) day.calories+=Number(f.calories);
   protein+=Number(f.protein_g);carbs+=Number(f.carbs_g);fat+=Number(f.fat_g);
  }
  const recorded=chart.filter(d=>d.calories>0);
  const sorted=[...weights.data].reverse();
  let suggestion=null;
  if(sorted.length>=2) {
   const last=sorted[sorted.length-1];
   const first=sorted.find(w=>Date.parse(w.measured_at)>=Date.parse(last.measured_at)-14*86400000) || sorted[0];
   if(first.id!==last.id) {
    const elapsed=Math.max(3,(Date.parse(last.measured_at)-Date.parse(first.measured_at))/86400000);
    const observed=(Number(first.weight_kg)-Number(last.weight_kg))/(elapsed/7);
    const target=body.targetWeeklyLoss ?? 0.25;
    const adjust=Math.abs(target-observed)<0.08?0:Math.max(-250,Math.min(250,Math.round((target-observed)*7700/7)));
    suggestion={currentGoal:goal,suggestedGoal:Math.max(1200,Math.min(4500,goal-adjust)),observedWeeklyChangeKg:observed,targetWeeklyLossKg:target};
   }
  }
  return json({chart,average:recorded.length?Math.round(recorded.reduce((s,d)=>s+d.calories,0)/recorded.length):0,adherence:recorded.length?Math.round(recorded.filter(d=>Math.abs(d.calories-goal)<=goal*0.1).length*100/recorded.length):0,protein,carbs,fat,recordedDays:recorded.length,weights:weights.data,suggestion});
 } catch(e) {return errorResponse(e);}
});

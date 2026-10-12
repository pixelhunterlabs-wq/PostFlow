-- Additive migration: existing meals, profiles, weights and authentication stay intact.
create table if not exists public.calorie_subscription_entitlements (
 user_id uuid primary key references auth.users(id) on delete cascade,
 product_id text not null, base_plan_id text not null, subscription_state text not null,
 expires_at timestamptz not null, verified_at timestamptz not null default now()
);
create table if not exists public.calorie_subscription_purchases (
 token_hash text primary key, purchase_token text not null,
 user_id uuid not null references auth.users(id) on delete cascade,
 product_id text not null, base_plan_id text not null, subscription_state text not null,
 expires_at timestamptz, verified_at timestamptz not null default now()
);
create index if not exists calorie_subscription_purchases_user_idx on public.calorie_subscription_purchases(user_id);
create table if not exists public.calorie_ai_monthly_usage (
 user_id uuid not null references auth.users(id) on delete cascade,
 month_start date not null, photo_analyses integer not null default 0 check(photo_analyses>=0),
 primary key(user_id,month_start)
);
create table if not exists public.calorie_ai_reservations (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
 month_start date not null, refunded boolean not null default false, created_at timestamptz not null default now()
);
alter table public.calorie_subscription_entitlements enable row level security;
alter table public.calorie_subscription_purchases enable row level security;
alter table public.calorie_ai_monthly_usage enable row level security;
alter table public.calorie_ai_reservations enable row level security;
revoke all on public.calorie_subscription_entitlements,public.calorie_subscription_purchases,public.calorie_ai_monthly_usage,public.calorie_ai_reservations from anon,authenticated;
grant select on public.calorie_subscription_entitlements,public.calorie_ai_monthly_usage to authenticated;
grant all on public.calorie_subscription_entitlements,public.calorie_subscription_purchases,public.calorie_ai_monthly_usage,public.calorie_ai_reservations to service_role;
drop policy if exists "premium own entitlement" on public.calorie_subscription_entitlements;
create policy "premium own entitlement" on public.calorie_subscription_entitlements for select to authenticated using ((select auth.uid())=user_id);
drop policy if exists "premium own usage" on public.calorie_ai_monthly_usage;
create policy "premium own usage" on public.calorie_ai_monthly_usage for select to authenticated using ((select auth.uid())=user_id);
create or replace function public.calorie_record_verified_purchase(p_user uuid,p_hash text,p_token text,p_plan text,p_state text,p_expiry timestamptz,p_linked_hash text default null)
returns void language plpgsql security invoker set search_path='' as $$
begin
 if p_plan not in ('monthly','yearly') then raise exception 'invalid_plan'; end if;
 -- Unique token claim is atomic; never overwrite a different owner's purchase.
 insert into public.calorie_subscription_purchases(token_hash,purchase_token,user_id,product_id,base_plan_id,subscription_state,expires_at,verified_at)
 values(p_hash,p_token,p_user,'kalori_premium',p_plan,p_state,p_expiry,now())
 on conflict(token_hash) do update set base_plan_id=excluded.base_plan_id,subscription_state=excluded.subscription_state,expires_at=excluded.expires_at,verified_at=excluded.verified_at
 where public.calorie_subscription_purchases.user_id=excluded.user_id;
 if not found then raise exception 'purchase_already_linked'; end if;
 if p_linked_hash is not null then
  if exists(select 1 from public.calorie_subscription_purchases where token_hash=p_linked_hash and user_id<>p_user) then raise exception 'purchase_account_mismatch'; end if;
  update public.calorie_subscription_purchases set subscription_state='SUBSCRIPTION_STATE_EXPIRED',expires_at=now() where token_hash=p_linked_hash and user_id=p_user;
 end if;
end $$;
create or replace function public.calorie_reserve_photo(p_user uuid)
returns uuid language plpgsql security invoker set search_path='' as $$
declare m date := date_trunc('month',now() at time zone 'UTC')::date; lim int; reservation uuid;
begin
 select case when exists(select 1 from public.calorie_subscription_entitlements where user_id=p_user and product_id='kalori_premium' and base_plan_id in('monthly','yearly') and subscription_state in('SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_CANCELED','SUBSCRIPTION_STATE_IN_GRACE_PERIOD') and expires_at>now() and verified_at>now()-interval '5 minutes') then 100 else 5 end into lim;
 insert into public.calorie_ai_monthly_usage(user_id,month_start) values(p_user,m) on conflict do nothing;
 update public.calorie_ai_monthly_usage set photo_analyses=photo_analyses+1 where user_id=p_user and month_start=m and photo_analyses<lim;
 if not found then raise exception 'quota_exceeded'; end if;
 insert into public.calorie_ai_reservations(user_id,month_start) values(p_user,m) returning id into reservation;
 return reservation;
end $$;
create or replace function public.calorie_refund_photo(p_user uuid,p_reservation uuid)
returns void language plpgsql security invoker set search_path='' as $$
declare m date;
begin
 update public.calorie_ai_reservations set refunded=true where id=p_reservation and user_id=p_user and refunded=false returning month_start into m;
 if found then update public.calorie_ai_monthly_usage set photo_analyses=greatest(0,photo_analyses-1) where user_id=p_user and month_start=m; end if;
end $$;
revoke all on function public.calorie_record_verified_purchase(uuid,text,text,text,text,timestamptz,text),public.calorie_reserve_photo(uuid),public.calorie_refund_photo(uuid,uuid) from public,anon,authenticated;
grant execute on function public.calorie_record_verified_purchase(uuid,text,text,text,text,timestamptz,text),public.calorie_reserve_photo(uuid),public.calorie_refund_photo(uuid,uuid) to service_role;
create schema if not exists calorie_private;
revoke all on schema calorie_private from public,anon,authenticated;
create or replace function calorie_private.check_voice_access() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if new.source='voice' and (TG_OP='INSERT' or new.source is distinct from old.source) then
  if auth.uid() is null or auth.uid()<>new.user_id or not exists(select 1 from public.calorie_subscription_entitlements where user_id=auth.uid() and expires_at>now() and verified_at>now()-interval '5 minutes' and product_id='kalori_premium' and base_plan_id in('monthly','yearly') and subscription_state in('SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_CANCELED','SUBSCRIPTION_STATE_IN_GRACE_PERIOD')) then raise exception 'premium_required'; end if;
 end if;
 return new;
end $$;
revoke all on function calorie_private.check_voice_access() from public,anon,authenticated;
drop trigger if exists calorie_voice_access on public.calorie_food_entries;
create trigger calorie_voice_access before insert or update on public.calorie_food_entries for each row execute function calorie_private.check_voice_access();

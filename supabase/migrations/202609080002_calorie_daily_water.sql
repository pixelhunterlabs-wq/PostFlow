create table if not exists public.calorie_daily_water (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  target_date date not null,
  water_ml integer not null default 0 check (water_ml >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (user_id, target_date)
);

alter table public.calorie_daily_water enable row level security;

drop policy if exists "daily water select own" on public.calorie_daily_water;
drop policy if exists "daily water insert own" on public.calorie_daily_water;
drop policy if exists "daily water update own" on public.calorie_daily_water;
drop policy if exists "daily water delete own" on public.calorie_daily_water;

create policy "daily water select own" on public.calorie_daily_water for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "daily water insert own" on public.calorie_daily_water for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "daily water update own" on public.calorie_daily_water for update to authenticated
  using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "daily water delete own" on public.calorie_daily_water for delete to authenticated
  using ((select auth.uid()) = user_id);

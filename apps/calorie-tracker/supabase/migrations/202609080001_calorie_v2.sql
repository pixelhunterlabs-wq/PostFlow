alter table public.calorie_profiles
  add column if not exists gender text,
  add column if not exists age integer,
  add column if not exists height_cm numeric,
  add column if not exists activity_level text,
  add column if not exists goal_type text,
  add column if not exists weekly_weight_goal numeric,
  add column if not exists water_goal_ml integer not null default 2500,
  add column if not exists protein_goal_g numeric,
  add column if not exists carbs_goal_g numeric,
  add column if not exists fat_goal_g numeric;

create table if not exists public.calorie_favorite_foods (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
  food_name text not null, grams numeric not null default 100, calories numeric not null default 0,
  protein_g numeric not null default 0, carbs_g numeric not null default 0, fat_g numeric not null default 0,
  meal_type text not null default 'Öğün', usage_count integer not null default 0, last_used_at timestamptz, created_at timestamptz not null default now(),
  unique(user_id, food_name)
);
create table if not exists public.calorie_water_entries (
  id uuid primary key default gen_random_uuid(), user_id uuid not null references auth.users(id) on delete cascade,
  amount_ml integer not null check(amount_ml > 0), logged_at timestamptz not null default now(), created_at timestamptz not null default now()
);
alter table public.calorie_favorite_foods enable row level security;
alter table public.calorie_water_entries enable row level security;
create policy "favorite owner access" on public.calorie_favorite_foods for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "water owner access" on public.calorie_water_entries for all to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);

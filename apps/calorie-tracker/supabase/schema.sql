-- Calorie Tracker schema for the SHARED Supabase project.
-- Tables are namespaced with calorie_ so future apps can coexist safely.

create table if not exists public.calorie_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  daily_calorie_goal integer not null default 2000 check (daily_calorie_goal between 500 and 10000),
  protein_goal_g numeric(8,2) not null default 100 check (protein_goal_g >= 0),
  carb_goal_g numeric(8,2) not null default 250 check (carb_goal_g >= 0),
  fat_goal_g numeric(8,2) not null default 65 check (fat_goal_g >= 0),
  updated_at timestamptz not null default now()
);

create table if not exists public.calorie_entries (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (char_length(name) between 1 and 160),
  meal_type text not null default 'Öğün' check (char_length(meal_type) between 1 and 40),
  grams numeric(10,2) not null check (grams >= 0 and grams <= 100000),
  calories numeric(10,2) not null check (calories >= 0 and calories <= 100000),
  protein_g numeric(10,2) not null default 0 check (protein_g >= 0),
  carb_g numeric(10,2) not null default 0 check (carb_g >= 0),
  fat_g numeric(10,2) not null default 0 check (fat_g >= 0),
  entry_date date not null default current_date,
  created_at timestamptz not null default now()
);

create table if not exists public.calorie_weights (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  weight_kg numeric(7,2) not null check (weight_kg > 0 and weight_kg < 1000),
  entry_date date not null default current_date,
  created_at timestamptz not null default now()
);

create index if not exists calorie_entries_user_date_idx
  on public.calorie_entries(user_id, entry_date desc);
create index if not exists calorie_weights_user_date_idx
  on public.calorie_weights(user_id, entry_date desc);

alter table public.calorie_profiles enable row level security;
alter table public.calorie_entries enable row level security;
alter table public.calorie_weights enable row level security;

-- Explicit Data API grants. RLS below remains the authorization layer.
grant select, insert, update, delete on public.calorie_profiles to authenticated;
grant select, insert, update, delete on public.calorie_entries to authenticated;
grant select, insert, update, delete on public.calorie_weights to authenticated;

-- Profiles
create policy "calorie_profiles_select_own"
on public.calorie_profiles for select to authenticated
using ((select auth.uid()) = user_id);

create policy "calorie_profiles_insert_own"
on public.calorie_profiles for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy "calorie_profiles_update_own"
on public.calorie_profiles for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "calorie_profiles_delete_own"
on public.calorie_profiles for delete to authenticated
using ((select auth.uid()) = user_id);

-- Food entries
create policy "calorie_entries_select_own"
on public.calorie_entries for select to authenticated
using ((select auth.uid()) = user_id);

create policy "calorie_entries_insert_own"
on public.calorie_entries for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy "calorie_entries_update_own"
on public.calorie_entries for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "calorie_entries_delete_own"
on public.calorie_entries for delete to authenticated
using ((select auth.uid()) = user_id);

-- Weight entries
create policy "calorie_weights_select_own"
on public.calorie_weights for select to authenticated
using ((select auth.uid()) = user_id);

create policy "calorie_weights_insert_own"
on public.calorie_weights for insert to authenticated
with check ((select auth.uid()) = user_id);

create policy "calorie_weights_update_own"
on public.calorie_weights for update to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy "calorie_weights_delete_own"
on public.calorie_weights for delete to authenticated
using ((select auth.uid()) = user_id);

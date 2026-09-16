create table if not exists public.calorie_barcode_products (
  barcode text primary key, product_name text not null, brand text not null default '', quantity_text text,
  calories_100g double precision, protein_100g double precision, carbs_100g double precision, fat_100g double precision,
  image_url text, source text not null, verified boolean not null default false,
  created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
alter table public.calorie_barcode_products enable row level security;
drop policy if exists "barcode products readable" on public.calorie_barcode_products;
create policy "barcode products readable" on public.calorie_barcode_products for select to authenticated using (true);

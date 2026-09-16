-- Central, read-only catalog plus account-scoped user foods.
-- Curated recipe estimates are explicitly marked unverified and are not clinical nutrition data.
create extension if not exists pg_trgm;

create or replace function public.calorie_normalize_food_text(value text)
returns text
language sql
immutable
security invoker
set search_path = public
as $$
  select regexp_replace(
    replace(replace(replace(replace(replace(replace(replace(lower(coalesce(value, '')), 'ç', 'c'), 'ğ', 'g'), 'ı', 'i'), 'ö', 'o'), 'ş', 's'), 'ü', 'u'), 'â', 'a'),
    '[^a-z0-9]+', ' ', 'g'
  );
$$;

create table if not exists public.calorie_food_catalog (
  id uuid primary key default gen_random_uuid(),
  name_tr text not null check (length(trim(name_tr)) > 0),
  normalized_name text not null unique,
  search_text text not null,
  category text not null,
  brand text,
  barcode text unique,
  calories_100g double precision not null check (calories_100g >= 0 and calories_100g <= 900),
  protein_100g double precision not null default 0 check (protein_100g between 0 and 100),
  carbs_100g double precision not null default 0 check (carbs_100g between 0 and 100),
  fat_100g double precision not null default 0 check (fat_100g between 0 and 100),
  fiber_100g double precision check (fiber_100g between 0 and 100),
  sugar_100g double precision check (sugar_100g between 0 and 100),
  aliases text[] not null default '{}',
  source text not null,
  source_reference text,
  verified boolean not null default false,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.calorie_food_portions (
  id uuid primary key default gen_random_uuid(),
  food_id uuid not null references public.calorie_food_catalog(id) on delete cascade,
  portion_name text not null check (length(trim(portion_name)) > 0),
  grams double precision not null check (grams > 0),
  is_default boolean not null default false,
  sort_order integer not null default 0,
  unique(food_id, portion_name)
);

create table if not exists public.calorie_user_foods (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null check (length(trim(name)) > 0),
  normalized_name text not null,
  category text not null default 'Kişisel',
  calories_100g double precision not null check (calories_100g >= 0 and calories_100g <= 900),
  protein_100g double precision not null default 0 check (protein_100g between 0 and 100),
  carbs_100g double precision not null default 0 check (carbs_100g between 0 and 100),
  fat_100g double precision not null default 0 check (fat_100g between 0 and 100),
  default_portion_name text not null default '100 gram',
  default_portion_grams double precision not null default 100 check (default_portion_grams > 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, normalized_name)
);

create index if not exists calorie_food_catalog_search_trgm_idx on public.calorie_food_catalog using gin (search_text gin_trgm_ops);
create index if not exists calorie_food_catalog_category_idx on public.calorie_food_catalog(category) where is_active;
create index if not exists calorie_user_foods_owner_search_idx on public.calorie_user_foods(user_id, normalized_name);

alter table public.calorie_food_catalog enable row level security;
alter table public.calorie_food_portions enable row level security;
alter table public.calorie_user_foods enable row level security;

drop policy if exists "Authenticated users can read food catalog" on public.calorie_food_catalog;
create policy "Authenticated users can read food catalog" on public.calorie_food_catalog for select to authenticated using (is_active);
drop policy if exists "Authenticated users can read food portions" on public.calorie_food_portions;
create policy "Authenticated users can read food portions" on public.calorie_food_portions for select to authenticated using (true);
drop policy if exists "Users manage only own foods" on public.calorie_user_foods;
create policy "Users read own foods" on public.calorie_user_foods for select to authenticated using ((select auth.uid()) = user_id);
create policy "Users create own foods" on public.calorie_user_foods for insert to authenticated with check ((select auth.uid()) = user_id);
create policy "Users update own foods" on public.calorie_user_foods for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
create policy "Users delete own foods" on public.calorie_user_foods for delete to authenticated using ((select auth.uid()) = user_id);

grant select on public.calorie_food_catalog, public.calorie_food_portions to authenticated;
grant select, insert, update, delete on public.calorie_user_foods to authenticated;

create or replace function public.search_calorie_foods(search_query text, page_size integer default 30, page_offset integer default 0)
returns table (
  id uuid, name_tr text, category text, calories_100g double precision, protein_100g double precision,
  carbs_100g double precision, fat_100g double precision, aliases text[], verified boolean, source text
)
language sql
stable
security invoker
set search_path = public
as $$
  with q as (select public.calorie_normalize_food_text(search_query) term)
  select c.id, c.name_tr, c.category, c.calories_100g, c.protein_100g, c.carbs_100g, c.fat_100g, c.aliases, c.verified, c.source
  from public.calorie_food_catalog c cross join q
  where c.is_active and (q.term = '' or c.search_text % q.term or c.search_text like '%' || q.term || '%')
  order by case when c.normalized_name = q.term then 0 when c.search_text like q.term || '%' then 1 else 2 end,
           similarity(c.search_text, q.term) desc, c.name_tr
  limit greatest(1, least(page_size, 50)) offset greatest(page_offset, 0);
$$;
grant execute on function public.search_calorie_foods(text, integer, integer) to authenticated;

-- Named, non-brand, barcode-free Turkish foods. Values are intentionally category-level
-- curated estimates and are marked verified=false; production imports may replace them.
with source_rows(category, names, kcal, protein, carbs, fat) as (
  values
  ('Tatlılar', 'Baklava|Fıstıklı baklava|Cevizli baklava|Soğuk baklava|Şöbiyet|Künefe|Kadayıf|Ekmek kadayıfı|Revani|Şekerpare|Kalburabastı|Tulumba tatlısı|Lokma tatlısı|Halka tatlısı|Sütlaç|Fırın sütlaç|Kazandibi|Tavukgöğsü tatlısı|Keşkül|Muhallebi|Supangle|Magnolia|Profiterol|Ekler|Tiramisu|Cheesecake|San Sebastian cheesecake|Yaş pasta|Mozaik pasta|Brownie|Islak kek|Havuçlu kek|Elmalı turta|Vişneli turta|Çikolatalı pasta|Meyveli pasta|Pavlova|Panna cotta|Crème brûlée|Trileçe|Aşure|İrmik helvası|Un helvası|Tahin helvası|Kabak tatlısı|Ayva tatlısı|İncir tatlısı|Güllaç|Zerde|Dondurma vanilyalı|Dondurma çikolatalı|Dondurma çilekli|Dondurma sade|Sorbe limon|Çikolata sütlü|Çikolata bitter|Çikolata beyaz|Gofret|Bisküvi sade|Bisküvi kakaolu|Kurabiye tereyağlı|Kurabiye damla çikolatalı|Kurabiye tahinli|Kurabiye tuzlu|Makaron|Donut|Churros|Waffle|Krep çikolatalı|Pankek|Lokum sade|Lokum fıstıklı|Cezerye|Pestil|Köme|Meyve tatlısı|Fırın helva|Saray sarması|Sufle|Mousse çikolatalı|Cupcake|Muffin|Tarçınlı rulo|Baba tatlısı|Paris brest|Milföy tatlısı|Krem şokola|Karamelli puding|Çilekli puding|Pirinç pudingi|Çikolatalı puding|Badem ezmesi|Marzipan|Krokan|Fıstık ezmeli bar|Enerji topu|Çikolatalı kurabiye|Limonlu kek|Portakallı kek|Mermer kek|Pandispanya|Şambali', 360, 5, 55, 14),
  ('Ev Yemekleri', 'Adana kebap|Urfa kebap|İskender kebap|Beyti kebap|Patlıcan kebap|Tepsi kebabı|Çöp şiş|Et şiş|Tavuk şiş|Köfte|İnegöl köfte|Tekirdağ köfte|Kasap köfte|Kadınbudu köfte|İçli köfte|Çiğ köfte|Et döner|Tavuk döner|Yaprak döner|Döner dürüm|Etli pide|Kıymalı pide|Kuşbaşılı pide|Kaşarlı pide|Karışık pide|Lahmacun|Lahmacun dürüm|Pizza margarita|Pizza karışık|Hamburger|Cheeseburger|Tavuk burger|Balık ekmek|Tantuni|Tavuk tantuni|Kokoreç|Midye dolma|Kumpir|Mantı|Kayseri mantısı|Sinop mantısı|Erişte|Makarna sade|Makarna bolonez|Makarna kremalı|Fırın makarna|Spagetti napoliten|Lazanya|Pirinç pilavı|Bulgur pilavı|Şehriyeli pilav|Nohutlu pilav|İç pilav|Keşkek|Kısır|Bamya yemeği|Türlü|İmam bayıldı|Karnıyarık|Patlıcan musakka|Patates oturtma|Etli patates|Tavuk sote|Et sote|Saç kavurma|Tas kebabı|Orman kebabı|Hünkar beğendi|Ali nazik|Çoban kavurma|Ciğer tava|Arnavut ciğeri|Kuzu tandır|Et haşlama|Fırın tavuk|Tavuk kanat|Tavuk baget|Tavuk schnitzel|Tavuk fajita|Balık tava|Fırın somon|Hamsi tava|Hamsi pilavı|Karides güveç|Kalamar tava|Paella|Sebzeli güveç|Etli güveç|Taze fasulye|Kuru fasulye|Nohut yemeği|Mercimek yemeği|Barbunya pilaki|Zeytinyağlı enginar|Zeytinyağlı pırasa|Zeytinyağlı yaprak sarma|Etli yaprak sarma|Biber dolması|Kabak dolması|Patlıcan dolması|Lahana sarması|Su böreği|Sigara böreği|Paçanga böreği|Kol böreği|Gözleme peynirli|Gözleme patatesli|Gözleme kıymalı|Çiğ börek|Mıhlama|Kuymak|Menemen|Sucuklu yumurta|Kavurma|Kısık ateş et|Fırın köfte|Patates kızartması|Fırın patates|Sebzeli omlet|Omlet peynirli', 190, 12, 15, 10),
  ('Çorbalar ve Zeytinyağlılar', 'Mercimek çorbası|Ezogelin çorbası|Tarhana çorbası|Yayla çorbası|Domates çorbası|Düğün çorbası|İşkembe çorbası|Kelle paça çorbası|Tavuk suyu çorba|Şehriye çorbası|Mantar çorbası|Brokoli çorbası|Sebze çorbası|Balık çorbası|Bamya çorbası|Yoğurt çorbası|Un çorbası|Arpa şehriye çorbası|Kremalı mantar çorbası|Kabak çorbası|Karnabahar çorbası|Kırmızı mercimek çorbası|Yeşil mercimek çorbası|Nohut çorbası|Fasulye piyazı|Patates salatası|Rus salatası|Çoban salata|Gavurdağı salata|Mevsim salata|Roka salatası|Akdeniz salata|Kısır salata|Yoğurtlu semizotu|Cacık|Haydari|Acılı ezme|Humus|Muhammara|Babagannuş|Patlıcan salatası|Piyaz|Zeytinyağlı kereviz|Zeytinyağlı taze fasulye|Zeytinyağlı barbunya|Zeytinyağlı kabak|Zeytinyağlı biber dolması|Zeytinyağlı yaprak sarma|Şakşuka|Fava|Mücver|Kabak mücver|Köz patlıcan|Köz biber|Turşu karışık|Lahana turşusu|Pancar turşusu|Purslane salatası|Makarna salatası', 105, 4, 12, 5),
  ('Kahvaltı ve Hamur işi', 'Beyaz ekmek|Tam buğday ekmeği|Çavdar ekmeği|Kepek ekmeği|Mısır ekmeği|Bazlama|Lavaş|Pide ekmek|Simit|Açma|Poğaça peynirli|Poğaça patatesli|Poğaça zeytinli|Kruvasan|Sandviç ekmeği|Baget ekmek|Tost ekmeği|Galeta|Grissini|Yufka|Gül böreği|Tepsi böreği|Peynirli börek|Ispanaklı börek|Patatesli börek|Kıymalı börek|Çikolatalı çörek|Tarçınlı çörek|Kahvaltılık gevrek|Mısır gevreği|Yulaf ezmesi|Granola|Müsli|Yulaf lapası|Menemen|Haşlanmış yumurta|Sahanda yumurta|Omlet sade|Omlet sebzeli|Sucuk|Sosis|Salam|Pastırma|Beyaz peynir|Kaşar peyniri|Tulum peyniri|Lor peyniri|Labne|Krem peynir|Zeytin siyah|Zeytin yeşil|Bal|Reçel çilek|Reçel vişne|Reçel kayısı|Tahin pekmez|Fındık ezmesi|Fıstık ezmesi|Çikolata kreması|Kaymak|Tereyağı|Margarın|Pankek sade|Krep sade|French toast|Avokadolu tost|Kaşarlı tost|Sucuklu tost|Karışık tost|Peynirli tost|Göz yumurta|Kuymak|Mıhlama', 290, 9, 35, 12),
  ('Meyve ve Sebze', 'Elma|Armut|Muz|Portakal|Mandalina|Limon|Greyfurt|Nar|Üzüm|Çilek|Kiraz|Vişne|Şeftali|Nektarin|Kayısı|Erik|İncir|Karpuz|Kavun|Ananas|Kivi|Mango|Avokado|Hurma|Kuru kayısı|Kuru üzüm|Kuru incir|Yaban mersini|Böğürtlen|Ahududu|Frambuaz|Domates|Salatalık|Marul|Roka|Maydanoz|Dereotu|Ispanak|Pazı|Brokoli|Karnabahar|Lahana|Brüksel lahanası|Havuç|Pancar|Patates|Tatlı patates|Kabak|Patlıcan|Biber kırmızı|Biber yeşil|Soğan|Sarımsak|Mantar|Enginar|Kereviz|Pırasa|Taze fasulye|Bezelye|Mısır|Bamya|Turp|Şalgam|Kuşkonmaz|Semizotu|Kara lahana|Kırmızı lahana|Fesleğen|Nane|Kişniş', 55, 1.5, 11, 0.4),
  ('Et Tavuk Balık Bakliyat', 'Tavuk göğsü pişmiş|Tavuk but pişmiş|Tavuk kanat pişmiş|Hindi göğsü|Hindi füme|Dana kıyma|Dana biftek|Dana antrikot|Dana bonfile|Kuzu pirzola|Kuzu incik|Kuzu kuşbaşı|Sığır dili|Dana ciğer|Tavuk ciğer|Yumurta|Yumurta akı|Ton balığı|Somon|Levrek|Çipura|Hamsi|Sardalya|Uskumru|Palamut|Alabalık|Karides|Midye|Kalamar|Ahtapot|Nohut pişmiş|Yeşil mercimek pişmiş|Kırmızı mercimek pişmiş|Kuru fasulye pişmiş|Barbunya pişmiş|Börülce pişmiş|Bakla pişmiş|Maş fasulyesi|Soya fasulyesi|Tofu|Tempeh|Seitan|Fıstık|Badem|Ceviz|Fındık|Kaju|Antep fıstığı|Ay çekirdeği|Kabak çekirdeği|Chia tohumu|Keten tohumu|Susam', 175, 17, 8, 8),
  ('Süt Ürünleri', 'Süt tam yağlı|Süt yarım yağlı|Süt yağsız|Yoğurt tam yağlı|Yoğurt süzme|Yoğurt probiyotik|Kefir|Ayran|Cacık|Beyaz peynir|Kaşar peyniri|Mozzarella|Parmesan|Çedar peyniri|Tulum peyniri|Ezine peyniri|Çökelek|Lor peyniri|Ricotta|Labne|Krem peynir|Krema|Süt tozu|Dondurma sade|Dondurma meyveli|Dondurma çikolatalı|Sütlaç|Muhallebi|Keşkül|Puding vanilyalı|Puding çikolatalı|Yoğurtlu içecek|Ayran köpüklü|Protein yoğurt|Protein süt|Whey protein tozu|Kazein protein tozu|Süzme yoğurt meyveli|Yoğurt meyveli|Kaymak', 120, 6, 10, 6),
  ('İçecekler ve Atıştırmalıklar', 'Su|Maden suyu|Sade soda|Kola|Kola şekersiz|Gazoz|Meyve suyu portakal|Meyve suyu elma|Limonata|Şalgam suyu|Ayran|Kefir içeceği|Soğuk çay|Enerji içeceği|Kahve sade|Türk kahvesi|Latte|Cappuccino|Filtre kahve|Espresso|Çay şekersiz|Çay şekerli|Sıcak çikolata|Salep|Boza|Komposto|Hoşaf|Protein bar|Granola bar|Meyve bar|Cips patates|Cips mısır|Patlamış mısır|Kraker|Tuzlu çubuk|Leblebi|Kavrulmuş nohut|Kuru yemiş karışık|Badem kavrulmuş|Fındık kavrulmuş|Yer fıstığı kavrulmuş|Ceviz içi|Kaju kavrulmuş|Antep fıstığı|Ay çekirdeği|Kabak çekirdeği|Pirinç patlağı|Pirinç keki|Kuru meyve karışık|Dondurulmuş yoğurt', 180, 4, 22, 8)
), expanded as (
  select category, trim(name) as name_tr, kcal, protein, carbs, fat
  from source_rows cross join lateral unnest(string_to_array(names, '|')) as name
), prepared as (
  select name_tr, public.calorie_normalize_food_text(name_tr) normalized_name,
         public.calorie_normalize_food_text(name_tr) search_text, category, kcal, protein, carbs, fat
  from expanded
), deduped as (
  select distinct on (normalized_name) * from prepared order by normalized_name, name_tr
)
insert into public.calorie_food_catalog (name_tr, normalized_name, search_text, category, calories_100g, protein_100g, carbs_100g, fat_100g, aliases, source, source_reference, verified)
select name_tr, normalized_name, search_text, category, kcal, protein, carbs, fat, '{}', 'curated_recipe_estimate', 'Curated Turkish recipe estimate; approximate per 100 g, not medical data.', false
from deduped
on conflict (normalized_name) do update set
  name_tr = excluded.name_tr, search_text = excluded.search_text, category = excluded.category,
  calories_100g = excluded.calories_100g, protein_100g = excluded.protein_100g,
  carbs_100g = excluded.carbs_100g, fat_100g = excluded.fat_100g,
  source = excluded.source, source_reference = excluded.source_reference, verified = false, updated_at = now();

insert into public.calorie_food_portions(food_id, portion_name, grams, is_default, sort_order)
select id, '100 gram', 100, true, 0 from public.calorie_food_catalog
on conflict (food_id, portion_name) do nothing;

-- Search aliases cover transliterations and common compact spellings without duplicating foods.
update public.calorie_food_catalog
set aliases = case normalized_name
  when 'baklava' then array['baklawa']
  when 'sutlac' then array['sutlac']
  when 'kazandibi' then array['kazan dibi']
  when 'tavuk gogsu pismis' then array['tavuk gogsu', 'tavuk gogsu']
  when 'kuru fasulye' then array['kurufasulye']
  when 'cig kofte' then array['cigkofte']
  else aliases end,
search_text = public.calorie_normalize_food_text(name_tr || ' ' || array_to_string(
  case normalized_name
    when 'baklava' then array['baklawa']
    when 'sutlac' then array['sutlac']
    when 'kazandibi' then array['kazan dibi']
    when 'tavuk gogsu pismis' then array['tavuk gogsu', 'tavuk gogsu']
    when 'kuru fasulye' then array['kurufasulye']
    when 'cig kofte' then array['cigkofte']
    else aliases end, ' '
));


-- Keep category minima after de-duplicating cross-category entries.
insert into public.calorie_food_catalog (name_tr, normalized_name, search_text, category, calories_100g, protein_100g, carbs_100g, fat_100g, aliases, source, source_reference, verified)
select name_tr, public.calorie_normalize_food_text(name_tr), public.calorie_normalize_food_text(name_tr), category, kcal, protein, carbs, fat, '{}', 'curated_recipe_estimate', 'Curated Turkish recipe estimate; approximate per 100 g, not medical data.', false
from (values
  ('Bülbül yuvası', 'Tatlılar', 390::double precision, 6::double precision, 55::double precision, 17::double precision),
  ('Dilber dudağı', 'Tatlılar', 400::double precision, 6::double precision, 57::double precision, 18::double precision),
  ('Vezir parmağı', 'Tatlılar', 365::double precision, 5::double precision, 58::double precision, 14::double precision),
  ('Zülbiye tatlısı', 'Tatlılar', 370::double precision, 5::double precision, 60::double precision, 14::double precision),
  ('Paluze', 'Tatlılar', 145::double precision, 2::double precision, 31::double precision, 1::double precision),
  ('Kiremitte köfte', 'Ev Yemekleri', 220::double precision, 16::double precision, 10::double precision, 13::double precision),
  ('Çömlek kebabı', 'Ev Yemekleri', 185::double precision, 15::double precision, 12::double precision, 9::double precision),
  ('Çerkez tavuğu', 'Ev Yemekleri', 210::double precision, 17::double precision, 7::double precision, 14::double precision),
  ('Elbasan tava', 'Ev Yemekleri', 205::double precision, 16::double precision, 8::double precision, 13::double precision),
  ('Söğürme kebabı', 'Ev Yemekleri', 195::double precision, 14::double precision, 10::double precision, 11::double precision),
  ('Hellim peyniri', 'Süt Ürünleri', 320::double precision, 22::double precision, 2::double precision, 25::double precision),
  ('Gouda peyniri', 'Süt Ürünleri', 356::double precision, 25::double precision, 2::double precision, 27::double precision),
  ('Brie peyniri', 'Süt Ürünleri', 334::double precision, 21::double precision, 0::double precision, 28::double precision),
  ('Mavi peynir', 'Süt Ürünleri', 353::double precision, 21::double precision, 2::double precision, 29::double precision),
  ('Mascarpone', 'Süt Ürünleri', 429::double precision, 4::double precision, 4::double precision, 44::double precision)
) as supplement(name_tr, category, kcal, protein, carbs, fat)
on conflict (normalized_name) do nothing;
do $$
declare total_count integer; dessert_count integer;
begin
  select count(*) into total_count from public.calorie_food_catalog;
  select count(*) into dessert_count from public.calorie_food_catalog where category = 'Tatlılar';
  if total_count < 500 then raise exception 'calorie catalog seed must contain at least 500 foods, got %', total_count; end if;
  if dessert_count < 100 then raise exception 'calorie catalog seed must contain at least 100 desserts, got %', dessert_count; end if;
  if exists (select 1 from public.calorie_food_catalog where name_tr = '' or calories_100g < 0 or calories_100g > 900 or protein_100g < 0 or carbs_100g < 0 or fat_100g < 0) then
    raise exception 'calorie catalog seed validation failed';
  end if;
end $$;

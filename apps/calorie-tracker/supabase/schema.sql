-- Reference/verification script for the existing PostFlow_DB calorie schema.
-- This file is intentionally non-destructive. It does not create duplicate tables.

select table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name in (
    'calorie_profiles',
    'calorie_food_entries',
    'calorie_weight_entries',
    'calorie_daily_targets'
  )
order by table_name, ordinal_position;

select schemaname, tablename, policyname, cmd, roles, qual, with_check
from pg_policies
where schemaname = 'public'
  and tablename in (
    'calorie_profiles',
    'calorie_food_entries',
    'calorie_weight_entries',
    'calorie_daily_targets'
  )
order by tablename, policyname;

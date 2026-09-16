import { createClient } from 'npm:@supabase/supabase-js@2'

Deno.serve(async (req: Request) => {
  if (req.method !== 'POST') {
    return Response.json({ error: 'Method not allowed' }, { status: 405 })
  }

  const authHeader = req.headers.get('Authorization')
  if (!authHeader?.startsWith('Bearer ')) {
    return Response.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const supabaseUrl = Deno.env.get('SUPABASE_URL')
  const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
  if (!supabaseUrl || !serviceRoleKey) {
    return Response.json({ error: 'Server configuration error' }, { status: 500 })
  }

  const admin = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false }
  })

  const token = authHeader.slice('Bearer '.length)
  const { data: userData, error: userError } = await admin.auth.getUser(token)
  const user = userData.user
  if (userError || !user) {
    return Response.json({ error: 'Invalid session' }, { status: 401 })
  }

  const tables = [
    'calorie_saved_meal_items',
    'calorie_saved_meals',
    'calorie_daily_water',
    'calorie_favorite_foods',
    'calorie_food_entries',
    'calorie_weight_entries',
    'calorie_daily_targets',
    'calorie_profiles'
  ]

  for (const table of tables) {
    const { error } = await admin.from(table).delete().eq('user_id', user.id)
    if (error) {
      console.error(`Failed deleting ${table}`, error)
      return Response.json({ error: 'Account data could not be deleted' }, { status: 500 })
    }
  }

  const { error: deleteUserError } = await admin.auth.admin.deleteUser(user.id)
  if (deleteUserError) {
    console.error('Failed deleting auth user', deleteUserError)
    return Response.json({ error: 'Account could not be deleted' }, { status: 500 })
  }

  return Response.json({ ok: true })
})

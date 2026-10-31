-- FocusForge Phase 3: private student profiles and study preferences.
-- Apply only after the dedicated FocusForge Supabase project is created.

alter table public.profiles
  add column if not exists email text,
  add column if not exists avatar_url text,
  add column if not exists onboarding_completed_at timestamptz;

-- Stop exposing every user's profile to every authenticated account.
drop policy if exists "profiles are readable by authenticated users" on public.profiles;
drop policy if exists "users read their own profile" on public.profiles;
create policy "users read their own profile"
  on public.profiles for select to authenticated
  using ((select auth.uid()) = id);

create table if not exists public.education_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  education_level text,
  institution_name text,
  course_name text,
  stream text,
  academic_year text,
  semester text,
  expected_graduation_year integer,
  updated_at timestamptz not null default now()
);

create table if not exists public.study_goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null check (char_length(title) between 1 and 120),
  subject text,
  daily_target_minutes integer check (daily_target_minutes between 1 and 1440),
  weekly_target_minutes integer check (weekly_target_minutes between 1 and 10080),
  target_date date,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.exam_preferences (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  exam_name text not null check (char_length(exam_name) between 1 and 100),
  target_date date,
  preparation_level text,
  notes text,
  created_at timestamptz not null default now()
);

create index if not exists study_goals_user_active_idx
  on public.study_goals(user_id, is_active);
create index if not exists exam_preferences_user_idx
  on public.exam_preferences(user_id);

alter table public.education_profiles enable row level security;
alter table public.study_goals enable row level security;
alter table public.exam_preferences enable row level security;

drop policy if exists "users manage their education profile" on public.education_profiles;
create policy "users read their education profile"
  on public.education_profiles for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "users create their education profile"
  on public.education_profiles for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "users update their education profile"
  on public.education_profiles for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users delete their education profile"
  on public.education_profiles for delete to authenticated
  using ((select auth.uid()) = user_id);

create policy "users read their study goals"
  on public.study_goals for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "users create their study goals"
  on public.study_goals for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "users update their study goals"
  on public.study_goals for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users delete their study goals"
  on public.study_goals for delete to authenticated
  using ((select auth.uid()) = user_id);

create policy "users read their exam preferences"
  on public.exam_preferences for select to authenticated
  using ((select auth.uid()) = user_id);
create policy "users create their exam preferences"
  on public.exam_preferences for insert to authenticated
  with check ((select auth.uid()) = user_id);
create policy "users update their exam preferences"
  on public.exam_preferences for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
create policy "users delete their exam preferences"
  on public.exam_preferences for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select, insert, update, delete on public.profiles to authenticated;
grant select, insert, update, delete on public.education_profiles to authenticated;
grant select, insert, update, delete on public.study_goals to authenticated;
grant select, insert, update, delete on public.exam_preferences to authenticated;

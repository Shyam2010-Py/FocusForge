create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  username text unique,
  display_name text not null default 'Student',
  avatar_path text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.study_rooms (
  id uuid primary key default gen_random_uuid(),
  room_code text not null unique,
  name text not null check (char_length(name) between 2 and 60),
  host_id uuid not null references auth.users(id) on delete cascade,
  is_private boolean not null default true,
  status text not null default 'waiting' check (status in ('waiting','running','paused','completed')),
  duration_seconds integer not null default 1500 check (duration_seconds between 60 and 14400),
  started_at timestamptz,
  created_at timestamptz not null default now()
);

create table if not exists public.room_members (
  room_id uuid not null references public.study_rooms(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member' check (role in ('host','member')),
  joined_at timestamptz not null default now(),
  last_seen_at timestamptz not null default now(),
  primary key (room_id, user_id)
);

create table if not exists public.focus_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  room_id uuid references public.study_rooms(id) on delete set null,
  planned_seconds integer not null check (planned_seconds between 60 and 86400),
  actual_seconds integer not null default 0 check (actual_seconds >= 0),
  completed boolean not null default false,
  started_at timestamptz not null default now(),
  ended_at timestamptz
);

create index if not exists room_members_user_idx on public.room_members(user_id);
create index if not exists focus_sessions_user_started_idx on public.focus_sessions(user_id, started_at desc);
create index if not exists study_rooms_host_idx on public.study_rooms(host_id);

alter table public.profiles enable row level security;
alter table public.study_rooms enable row level security;
alter table public.room_members enable row level security;
alter table public.focus_sessions enable row level security;

create policy "profiles are readable by authenticated users" on public.profiles for select to authenticated using (true);
create policy "users insert their own profile" on public.profiles for insert to authenticated with check ((select auth.uid()) = id);
create policy "users update their own profile" on public.profiles for update to authenticated using ((select auth.uid()) = id) with check ((select auth.uid()) = id);

create policy "room members and hosts can view rooms" on public.study_rooms for select to authenticated using (
  host_id = (select auth.uid()) or exists (select 1 from public.room_members m where m.room_id = id and m.user_id = (select auth.uid()))
);
create policy "users create rooms as host" on public.study_rooms for insert to authenticated with check (host_id = (select auth.uid()));
create policy "hosts update their rooms" on public.study_rooms for update to authenticated using (host_id = (select auth.uid())) with check (host_id = (select auth.uid()));
create policy "hosts delete their rooms" on public.study_rooms for delete to authenticated using (host_id = (select auth.uid()));

create policy "users view their own memberships" on public.room_members for select to authenticated using (user_id = (select auth.uid()));
create policy "users join public rooms or their own hosted room" on public.room_members for insert to authenticated with check (
  user_id = (select auth.uid()) and exists (select 1 from public.study_rooms r where r.id = room_id and (r.is_private = false or r.host_id = (select auth.uid())))
);
create policy "users leave their own rooms" on public.room_members for delete to authenticated using (user_id = (select auth.uid()));

create policy "users view their own focus sessions" on public.focus_sessions for select to authenticated using (user_id = (select auth.uid()));
create policy "users create their own focus sessions" on public.focus_sessions for insert to authenticated with check (user_id = (select auth.uid()));
create policy "users update their own focus sessions" on public.focus_sessions for update to authenticated using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));

create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data ->> 'display_name', 'Student'))
  on conflict (id) do nothing;
  return new;
end;
$$;
revoke all on function public.handle_new_user() from public, anon, authenticated;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users for each row execute procedure public.handle_new_user();

do $$ begin alter publication supabase_realtime add table public.study_rooms;
exception when duplicate_object then null; when undefined_object then null; end $$;
do $$ begin alter publication supabase_realtime add table public.room_members;
exception when duplicate_object then null; when undefined_object then null; end $$;

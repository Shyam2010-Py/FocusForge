-- Phase 4: Groups and independent member focus status
-- Applied to Supabase project hyfnjltsnhvclrnlajfr on 2026-10-31.

create table if not exists public.groups (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(btrim(name)) between 2 and 60),
  description text check (description is null or char_length(description) <= 240),
  created_by uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists public.group_members (
  group_id uuid not null references public.groups(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'member' check (role in ('owner','admin','member')),
  joined_at timestamptz not null default now(),
  primary key (group_id, user_id)
);
create table if not exists public.group_invites (
  id uuid primary key default gen_random_uuid(),
  group_id uuid not null references public.groups(id) on delete cascade,
  invite_code text not null unique check (invite_code ~ '^[A-Z0-9]{8}$'),
  created_by uuid not null references auth.users(id) on delete cascade,
  expires_at timestamptz,
  max_uses integer check (max_uses is null or max_uses > 0),
  uses integer not null default 0 check (uses >= 0),
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);
create table if not exists public.group_focus_status (
  group_id uuid not null,
  user_id uuid not null,
  status text not null default 'not_focusing' check (status in ('focusing','paused','not_focusing','completed')),
  started_at timestamptz,
  ends_at timestamptz,
  planned_seconds integer check (planned_seconds is null or planned_seconds between 60 and 86400),
  updated_at timestamptz not null default now(),
  primary key (group_id,user_id),
  foreign key (group_id,user_id) references public.group_members(group_id,user_id) on delete cascade,
  check ((status='focusing' and started_at is not null and ends_at is not null and planned_seconds is not null) or status <> 'focusing')
);
create index if not exists group_members_user_id_idx on public.group_members(user_id);
create index if not exists group_invites_group_id_idx on public.group_invites(group_id);
create index if not exists group_focus_status_user_id_idx on public.group_focus_status(user_id);
create index if not exists group_focus_status_ends_at_idx on public.group_focus_status(ends_at) where status='focusing';

alter table public.groups enable row level security;
alter table public.group_members enable row level security;
alter table public.group_invites enable row level security;
alter table public.group_focus_status enable row level security;

create or replace function public.is_group_member(p_group_id uuid)
returns boolean language sql stable security definer set search_path=''
as $$ select exists(select 1 from public.group_members gm where gm.group_id=p_group_id and gm.user_id=auth.uid()); $$;
revoke all on function public.is_group_member(uuid) from public,anon;
grant execute on function public.is_group_member(uuid) to authenticated;

create policy "Members can view their groups" on public.groups for select to authenticated using (public.is_group_member(id));
create policy "Authenticated users can create groups" on public.groups for insert to authenticated with check (created_by=auth.uid());
create policy "Owners and admins can update groups" on public.groups for update to authenticated
using (exists(select 1 from public.group_members gm where gm.group_id=id and gm.user_id=auth.uid() and gm.role in ('owner','admin')))
with check (exists(select 1 from public.group_members gm where gm.group_id=id and gm.user_id=auth.uid() and gm.role in ('owner','admin')));
create policy "Owners can delete groups" on public.groups for delete to authenticated using (created_by=auth.uid());

create policy "Members can view group membership" on public.group_members for select to authenticated using (public.is_group_member(group_id));
create policy "Users can join themselves through controlled flow" on public.group_members for insert to authenticated with check (user_id=auth.uid() and role='member');
create policy "Users can leave their own groups" on public.group_members for delete to authenticated using (user_id=auth.uid() and role<>'owner');

create policy "Members can view group invites" on public.group_invites for select to authenticated using (public.is_group_member(group_id));
create policy "Admins can create group invites" on public.group_invites for insert to authenticated with check (created_by=auth.uid() and exists(select 1 from public.group_members gm where gm.group_id=group_id and gm.user_id=auth.uid() and gm.role in ('owner','admin')));
create policy "Admins can revoke group invites" on public.group_invites for update to authenticated
using (exists(select 1 from public.group_members gm where gm.group_id=group_id and gm.user_id=auth.uid() and gm.role in ('owner','admin')))
with check (exists(select 1 from public.group_members gm where gm.group_id=group_id and gm.user_id=auth.uid() and gm.role in ('owner','admin')));

create policy "Members can view group focus status" on public.group_focus_status for select to authenticated using (public.is_group_member(group_id));
create policy "Users can manage their own focus status" on public.group_focus_status for all to authenticated
using (user_id=auth.uid() and public.is_group_member(group_id))
with check (user_id=auth.uid() and public.is_group_member(group_id));

create or replace function public.create_group(p_name text,p_description text default null)
returns table(group_id uuid,invite_code text) language plpgsql security definer set search_path=''
as $$
declare v_group_id uuid; v_code text;
begin
 if auth.uid() is null then raise exception 'Authentication required'; end if;
 if char_length(btrim(p_name))<2 or char_length(btrim(p_name))>60 then raise exception 'Group name must be between 2 and 60 characters'; end if;
 if p_description is not null and char_length(p_description)>240 then raise exception 'Description must be 240 characters or fewer'; end if;
 insert into public.groups(name,description,created_by) values(btrim(p_name),nullif(btrim(p_description),''),auth.uid()) returning id into v_group_id;
 insert into public.group_members(group_id,user_id,role) values(v_group_id,auth.uid(),'owner');
 loop
  v_code:=upper(substr(encode(extensions.gen_random_bytes(8),'hex'),1,8));
  exit when not exists(select 1 from public.group_invites gi where gi.invite_code=v_code);
 end loop;
 insert into public.group_invites(group_id,invite_code,created_by) values(v_group_id,v_code,auth.uid());
 return query select v_group_id,v_code;
end $$;
revoke all on function public.create_group(text,text) from public,anon;
grant execute on function public.create_group(text,text) to authenticated;

create or replace function public.join_group_by_code(p_invite_code text)
returns table(group_id uuid) language plpgsql security definer set search_path=''
as $$
declare v_group_id uuid;
begin
 if auth.uid() is null then raise exception 'Authentication required'; end if;
 select gi.group_id into v_group_id from public.group_invites gi
 where gi.invite_code=upper(btrim(p_invite_code)) and gi.revoked_at is null
 and (gi.expires_at is null or gi.expires_at>now()) and (gi.max_uses is null or gi.uses<gi.max_uses)
 for update;
 if v_group_id is null then raise exception 'Invitation code is invalid, expired, or full'; end if;
 insert into public.group_members(group_id,user_id,role) values(v_group_id,auth.uid(),'member')
 on conflict(group_id,user_id) do nothing;
 update public.group_invites set uses=uses+1 where invite_code=upper(btrim(p_invite_code));
 return query select v_group_id;
end $$;
revoke all on function public.join_group_by_code(text) from public,anon;
grant execute on function public.join_group_by_code(text) to authenticated;

grant select,insert,update,delete on public.groups to authenticated;
grant select,insert,delete on public.group_members to authenticated;
grant select,insert,update on public.group_invites to authenticated;
grant select,insert,update,delete on public.group_focus_status to authenticated;

alter publication supabase_realtime add table public.group_focus_status;
alter publication supabase_realtime add table public.group_members;

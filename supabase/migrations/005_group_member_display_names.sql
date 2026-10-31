-- Phase 4 follow-up: display names in groups and safe invitation joining.
alter table public.group_members
  add column if not exists display_name text not null default 'Student';

update public.group_members gm
set display_name = coalesce(nullif(p.display_name, ''), 'Student')
from public.profiles p
where p.id = gm.user_id;

create or replace function public.sync_group_member_display_name()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  update public.group_members
  set display_name = coalesce(nullif(new.display_name, ''), 'Student')
  where user_id = new.id;
  return new;
end;
$$;
revoke all on function public.sync_group_member_display_name() from public, anon, authenticated;
drop trigger if exists sync_group_member_display_name_trigger on public.profiles;
create trigger sync_group_member_display_name_trigger
after update of display_name on public.profiles
for each row execute function public.sync_group_member_display_name();

create or replace function public.create_group(p_name text, p_description text default null)
returns table(group_id uuid, invite_code text)
language plpgsql security definer set search_path = ''
as $$
declare
  v_group_id uuid;
  v_code text;
  v_display_name text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if char_length(btrim(p_name)) < 2 or char_length(btrim(p_name)) > 60 then
    raise exception 'Group name must be between 2 and 60 characters';
  end if;
  if p_description is not null and char_length(p_description) > 240 then
    raise exception 'Description must be 240 characters or fewer';
  end if;

  select coalesce(nullif(p.display_name, ''), 'Student') into v_display_name
  from public.profiles p where p.id = auth.uid();

  insert into public.groups(name, description, created_by)
  values (btrim(p_name), nullif(btrim(p_description), ''), auth.uid())
  returning id into v_group_id;

  insert into public.group_members(group_id, user_id, role, display_name)
  values (v_group_id, auth.uid(), 'owner', coalesce(v_display_name, 'Student'));

  loop
    v_code := upper(substr(encode(extensions.gen_random_bytes(8), 'hex'), 1, 8));
    exit when not exists (select 1 from public.group_invites gi where gi.invite_code = v_code);
  end loop;

  insert into public.group_invites(group_id, invite_code, created_by)
  values (v_group_id, v_code, auth.uid());

  return query select v_group_id, v_code;
end;
$$;
revoke all on function public.create_group(text, text) from public, anon;
grant execute on function public.create_group(text, text) to authenticated;

drop function if exists public.join_group_by_code(text);
create function public.join_group_by_code(p_invite_code text)
returns table(group_id uuid)
language plpgsql security definer set search_path = ''
as $$
declare
  v_group_id uuid;
  v_display_name text;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select coalesce(nullif(p.display_name, ''), 'Student') into v_display_name
  from public.profiles p where p.id = auth.uid();

  select gi.group_id into v_group_id
  from public.group_invites gi
  where gi.invite_code = upper(btrim(p_invite_code))
    and gi.revoked_at is null
    and (gi.expires_at is null or gi.expires_at > now())
    and (gi.max_uses is null or gi.uses < gi.max_uses)
  for update;

  if v_group_id is null then raise exception 'Invitation code is invalid, expired, or full'; end if;

  insert into public.group_members(group_id, user_id, role, display_name)
  values (v_group_id, auth.uid(), 'member', coalesce(v_display_name, 'Student'))
  on conflict (group_id, user_id) do nothing;

  update public.group_invites set uses = uses + 1
  where invite_code = upper(btrim(p_invite_code));

  return query select v_group_id;
end;
$$;
revoke all on function public.join_group_by_code(text) from public, anon;
grant execute on function public.join_group_by_code(text) to authenticated;

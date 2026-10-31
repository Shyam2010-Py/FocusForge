-- Add an index for lookups and joins by study room.
create index if not exists focus_sessions_room_id_idx
  on public.focus_sessions(room_id)
  where room_id is not null;

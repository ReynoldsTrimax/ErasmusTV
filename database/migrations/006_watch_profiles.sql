-- Additive: household watch profiles for the streaming site.
-- Does not alter or drop tracker tables.

create table if not exists public.watch_profiles (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  name text not null,
  avatar_key text not null default 'slate',
  preferences jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint watch_profiles_name_len check (char_length(btrim(name)) between 1 and 32)
);

create unique index if not exists watch_profiles_user_name_idx
  on public.watch_profiles (user_id, lower(name));

create index if not exists watch_profiles_user_id_idx
  on public.watch_profiles (user_id);

alter table public.watch_profiles enable row level security;

drop policy if exists watch_profiles_select_own on public.watch_profiles;
create policy watch_profiles_select_own
  on public.watch_profiles for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists watch_profiles_insert_own on public.watch_profiles;
create policy watch_profiles_insert_own
  on public.watch_profiles for insert to authenticated
  with check ((select auth.uid()) = user_id);

drop policy if exists watch_profiles_update_own on public.watch_profiles;
create policy watch_profiles_update_own
  on public.watch_profiles for update to authenticated
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

drop policy if exists watch_profiles_delete_own on public.watch_profiles;
create policy watch_profiles_delete_own
  on public.watch_profiles for delete to authenticated
  using ((select auth.uid()) = user_id);

create or replace function public.watch_profiles_enforce_limit()
returns trigger
language plpgsql
as $$
begin
  if (select count(*) from public.watch_profiles where user_id = new.user_id) >= 5 then
    raise exception 'Maximum of 5 profiles per account';
  end if;
  return new;
end;
$$;

drop trigger if exists watch_profiles_limit_trg on public.watch_profiles;
create trigger watch_profiles_limit_trg
  before insert on public.watch_profiles
  for each row execute function public.watch_profiles_enforce_limit();

grant select, insert, update, delete on table public.watch_profiles to authenticated;
revoke all on table public.watch_profiles from anon;

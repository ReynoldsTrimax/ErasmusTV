alter table public.watch_profiles
  add column if not exists birth_year integer;

alter table public.watch_profiles
  drop constraint if exists watch_profiles_birth_year_range;

alter table public.watch_profiles
  add constraint watch_profiles_birth_year_range
  check (birth_year is null or (birth_year >= 1905 and birth_year <= extract(year from now())::integer));

create table if not exists public.watchlist_items (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users (id) on delete cascade,
  profile_id uuid not null references public.watch_profiles (id) on delete cascade,
  media_type text not null check (media_type in ('movie', 'tv')),
  tmdb_id text not null,
  title text not null,
  poster_path text,
  backdrop_path text,
  release_date text,
  created_at timestamptz not null default now(),
  unique (profile_id, media_type, tmdb_id)
);

create index if not exists watchlist_items_profile_idx
  on public.watchlist_items (profile_id, created_at desc);

alter table public.watchlist_items enable row level security;

drop policy if exists watchlist_items_select_own on public.watchlist_items;
create policy watchlist_items_select_own
  on public.watchlist_items for select to authenticated
  using ((select auth.uid()) = user_id);

drop policy if exists watchlist_items_insert_own on public.watchlist_items;
create policy watchlist_items_insert_own
  on public.watchlist_items for insert to authenticated
  with check (
    (select auth.uid()) = user_id
    and exists (
      select 1 from public.watch_profiles p
      where p.id = profile_id and p.user_id = (select auth.uid())
    )
  );

drop policy if exists watchlist_items_delete_own on public.watchlist_items;
create policy watchlist_items_delete_own
  on public.watchlist_items for delete to authenticated
  using ((select auth.uid()) = user_id);

grant select, insert, delete on table public.watchlist_items to authenticated;
revoke all on table public.watchlist_items from anon;

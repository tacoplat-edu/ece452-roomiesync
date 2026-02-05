-- RoomieSync schema for Supabase
-- Run in Supabase: SQL Editor → New query → paste → Run
--
-- Table definitions:
--
--   auth.users (Supabase built-in)
--     Auth'd users. We don't create this; sign-up adds rows here.
--
--   profiles
--     One row per user; id = auth.users.id. Extra info: email, display_name.
--
--   houses
--     A "house" / group (e.g. one apartment). name, created_by (user who created it).
--
--   house_members
--     Links users to houses (many-to-many). role = 'admin' | 'member'.
--     Creator is added as admin via trigger when a house is created.

-- =============================================================================
-- PROFILES (one per auth user)
-- =============================================================================
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  display_name text,
  created_at timestamptz default now()
);

alter table public.profiles enable row level security;

create policy "Users can view own profile"
  on public.profiles for select using (auth.uid() = id);
create policy "Users can update own profile"
  on public.profiles for update using (auth.uid() = id);
create policy "Users can insert own profile"
  on public.profiles for insert with check (auth.uid() = id);

-- Auto-create profile on sign-up
create or replace function public.handle_new_user()
returns trigger as $$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    new.email,
    coalesce(new.raw_user_meta_data->>'display_name', new.email)
  );
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- =============================================================================
-- HOUSES (groups that link users together)
-- =============================================================================
create table if not exists public.houses (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  created_by uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz default now()
);

alter table public.houses enable row level security;

-- See houses you're a member of
create policy "Members can view house"
  on public.houses for select
  using (
    exists (
      select 1 from public.house_members hm
      where hm.house_id = id and hm.user_id = auth.uid()
    )
  );

-- Authenticated users can create a house (they become creator)
create policy "Authenticated users can create house"
  on public.houses for insert
  with check (auth.uid() = created_by);

-- Only creator can update or delete the house
create policy "Creator can update house"
  on public.houses for update
  using (created_by = auth.uid());
create policy "Creator can delete house"
  on public.houses for delete
  using (created_by = auth.uid());

-- =============================================================================
-- HOUSE_MEMBERS (links users to houses; many-to-many)
-- =============================================================================
create table if not exists public.house_members (
  house_id uuid not null references public.houses(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('admin', 'member')),
  joined_at timestamptz default now(),
  primary key (house_id, user_id)
);

alter table public.house_members enable row level security;

-- Members can see other members of the same house
create policy "Members can view house_members"
  on public.house_members for select
  using (
    exists (
      select 1 from public.house_members hm
      where hm.house_id = house_members.house_id and hm.user_id = auth.uid()
    )
  );

-- You can add yourself to a house (e.g. join by code later); creator is added by trigger
create policy "Users can add themselves to house"
  on public.house_members for insert
  with check (auth.uid() = user_id);

-- Admins and house creator can remove members; users can remove themselves (leave)
create policy "Users can delete own membership"
  on public.house_members for delete
  using (user_id = auth.uid());

create policy "House creator can delete any member"
  on public.house_members for delete
  using (
    exists (
      select 1 from public.houses h
      where h.id = house_members.house_id and h.created_by = auth.uid()
    )
  );

-- Only house creator can update roles (e.g. promote to admin)
create policy "House creator can update members"
  on public.house_members for update
  using (
    exists (
      select 1 from public.houses h
      where h.id = house_members.house_id and h.created_by = auth.uid()
    )
  );

-- When a house is created, add the creator as an admin member
create or replace function public.handle_new_house()
returns trigger as $$
begin
  insert into public.house_members (house_id, user_id, role)
  values (new.id, new.created_by, 'admin');
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_house_created on public.houses;
create trigger on_house_created
  after insert on public.houses
  for each row execute function public.handle_new_house();

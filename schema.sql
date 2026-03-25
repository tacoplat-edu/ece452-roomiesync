-- 1. NUKE EVERYTHING: Start from a true zero state
-- Drop triggers first (depend on tables + functions)
drop trigger if exists on_house_created on public.houses;
drop trigger if exists set_house_join_code_trigger on public.houses;
drop trigger if exists on_auth_user_created on auth.users;

-- Drop tables first (cascade drops RLS policies that depend on is_member_of_house)
drop table if exists public.expense_splits cascade;
drop table if exists public.expenses cascade;
drop table if exists public.chore_assignments cascade;
drop table if exists public.chores cascade;
drop table if exists public.house_members cascade;
drop table if exists public.houses cascade;
drop table if exists public.profiles cascade;

-- Now safe to drop functions (no policies depend on them)
drop function if exists public.join_house_by_code(text);
drop function if exists public.is_member_of_house(uuid);
drop function if exists public.handle_new_house();
drop function if exists public.handle_new_user();
drop function if exists public.set_house_join_code();
drop function if exists public.generate_house_join_code();

-- 2. CORE IDENTITY: Profiles, Houses, and Members
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  display_name text,
  created_at timestamptz default now()
);

create table public.houses (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  address text,
  join_code text unique,
  created_by uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz default now()
);

create table public.house_members (
  house_id uuid not null references public.houses(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  role text not null default 'member' check (role in ('admin', 'member')),
  joined_at timestamptz default now(),
  primary key (house_id, user_id)
);

-- Security Helper to prevent recursive RLS errors
create function public.is_member_of_house(p_house_id uuid)
returns boolean language sql security definer stable as $$
  select exists (select 1 from public.house_members where house_id = p_house_id and user_id = auth.uid());
$$;

-- 3. CHORES & ASSIGNMENTS (The Review Queue)
create table public.chores (
  id uuid primary key default gen_random_uuid(),
  house_id uuid not null references public.houses(id) on delete cascade,
  title text not null,
  description text,
  recurrence_type text default 'none',
  created_at timestamptz default now()
);

create table public.chore_assignments (
  id uuid primary key default gen_random_uuid(),
  chore_id uuid not null references public.chores(id) on delete cascade,
  assigned_to_id uuid not null references public.profiles(id) on delete cascade,
  status text default 'todo' check (status in ('todo', 'pending_approval', 'completed')),
  proof_photo_url text,
  verified_by uuid references public.profiles(id),
  due_date timestamptz,
  completed_at timestamptz
);

-- 4. FINANCES (Expenses & Splits)
create table public.expenses (
  id uuid primary key default gen_random_uuid(),
  house_id uuid not null references public.houses(id) on delete cascade,
  paid_by_id uuid not null references public.profiles(id) on delete cascade,
  amount decimal(10,2) not null,
  description text,
  created_at timestamptz default now()
);

create table public.expense_splits (
  id uuid primary key default gen_random_uuid(),
  expense_id uuid not null references public.expenses(id) on delete cascade,
  user_id uuid not null references public.profiles(id) on delete cascade,
  amount_owed decimal(10,2) not null,
  is_paid boolean default false
);

-- 5. AUTOMATION FUNCTIONS & TRIGGERS
-- Create profile on signup
create function public.handle_new_user() returns trigger as $$
begin
  insert into public.profiles (id, email, display_name)
  values (new.id, new.email, coalesce(new.raw_user_meta_data->>'display_name', new.email));
  return new;
end; $$ language plpgsql security definer;
create trigger on_auth_user_created after insert on auth.users for each row execute function public.handle_new_user();

-- Auto-generate unique 8-char join code
create function public.set_house_join_code() returns trigger as $$
begin
  new.join_code := upper(substr(md5(random()::text || clock_timestamp()::text), 1, 8));
  return new;
end; $$ language plpgsql;
create trigger set_house_join_code_trigger before insert on public.houses for each row execute function public.set_house_join_code();

-- Add creator as Admin immediately
create function public.handle_new_house() returns trigger as $$
begin
  insert into public.house_members (house_id, user_id, role)
  values (new.id, new.created_by, 'admin');
  return new;
end; $$ language plpgsql security definer;
create trigger on_house_created after insert on public.houses for each row execute function public.handle_new_house();

-- 6. SECURITY: RLS POLICIES
alter table public.profiles enable row level security;
alter table public.houses enable row level security;
alter table public.house_members enable row level security;
alter table public.chores enable row level security;
alter table public.chore_assignments enable row level security;
alter table public.expenses enable row level security;
alter table public.expense_splits enable row level security;

-- Profiles: view/update own; insert own (for signup trigger)
create policy "Profiles: view allowed" on public.profiles for select using (
    auth.uid() = id OR
    exists (
        select 1 from public.house_members hm1
        join public.house_members hm2 on hm1.house_id = hm2.house_id
        where hm1.user_id = auth.uid() and hm2.user_id = profiles.id
    )
);
-- create policy "Profiles: view own" on public.profiles for select using (auth.uid() = id);
create policy "Profiles: insert own" on public.profiles for insert with check (auth.uid() = id);
create policy "Profiles: update own" on public.profiles for update using (auth.uid() = id);

-- Houses: view if member, insert as creator (so you can create a new household)
create policy "Houses: view if member" on public.houses for select using (public.is_member_of_house(id));
create policy "Houses: insert as creator" on public.houses for insert with check (auth.uid() = created_by);

-- House members: view if member of that house; insert self (creator is added by trigger; join by code uses RPC)
create policy "House members: view if member" on public.house_members for select using (public.is_member_of_house(house_id));
create policy "House members: insert self" on public.house_members for insert with check (auth.uid() = user_id);

-- Chores: house members can view and insert; only creator or admin can update
create policy "Chores: view if member" on public.chores for select using (public.is_member_of_house(house_id));
create policy "Chores: insert if member" on public.chores for insert with check (public.is_member_of_house(house_id));

-- Chore assignments: house members can view and insert; assignee can update (submit proof); any member can update (approve)
create policy "Chore assignments: view if member" on public.chore_assignments for select
  using (exists (select 1 from public.chores c where c.id = chore_assignments.chore_id and public.is_member_of_house(c.house_id)));
create policy "Chore assignments: insert if member" on public.chore_assignments for insert
  with check (exists (select 1 from public.chores c where c.id = chore_id and public.is_member_of_house(c.house_id)));
create policy "Chore assignments: update if member" on public.chore_assignments for update
  using (exists (select 1 from public.chores c where c.id = chore_assignments.chore_id and public.is_member_of_house(c.house_id)));

-- Also let house members view each other's profiles (needed for assignee lists)
-- create policy "Profiles: view housemates" on public.profiles for select
--  using (exists (
--    select 1 from public.house_members hm1
--    join public.house_members hm2 on hm1.house_id = hm2.house_id
--    where hm1.user_id = auth.uid() and hm2.user_id = profiles.id
--  ));

create policy "Expenses: view if member" on public.expenses for select using (public.is_member_of_house(house_id));
create policy "Expenses: insert if member" on public.expenses for insert with check (public.is_member_of_house(house_id));

-- expense_splits: house members can view/insert; any house member can update (e.g. mark paid).
-- To restrict "mark as paid" to only the debtor, replace the update policy with:
--   using (user_id = auth.uid()) so only the user who owes can mark their own split.
create policy "Expense splits: view if member" on public.expense_splits for select
  using (exists (select 1 from public.expenses e where e.id = expense_splits.expense_id and public.is_member_of_house(e.house_id)));
create policy "Expense splits: insert if member" on public.expense_splits for insert
  with check (exists (select 1 from public.expenses e where e.id = expense_id and public.is_member_of_house(e.house_id)));
create policy "Expense splits: update if member" on public.expense_splits for update
  using (exists (select 1 from public.expenses e where e.id = expense_splits.expense_id and public.is_member_of_house(e.house_id)));

-- 7. THE JOIN FUNCTION (RPC)
create function public.join_house_by_code(p_join_code text)
returns jsonb language plpgsql security definer as $$
declare v_house_id uuid;
begin
  select id into v_house_id from public.houses where join_code = upper(trim(p_join_code)) limit 1;
  if v_house_id is null then return jsonb_build_object('ok', false, 'error', 'invalid_code'); end if;
  insert into public.house_members (house_id, user_id, role) values (v_house_id, auth.uid(), 'member') on conflict do nothing;
  return jsonb_build_object('ok', true);
end; $$;
grant execute on function public.join_house_by_code(text) to authenticated;
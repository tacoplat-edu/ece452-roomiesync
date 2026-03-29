# RoomieSync

A Kotlin app to organize shared tasks among roommates. (Description WIP)

- Members
  - Jack Pearson (j4pearson)
  - Jerome Lu (jerome-lu-uw)
  - Andy Zhen (a3zhen)
  - Ethan Hsu (eh128)
  - Tiffany Zhang (karatecarrot905)
  - Eric Bettinson (bericlol)
  - Rayan Ahmad (rayanthefirst)
- Links:
  - [Team contract](https://humane-amaryllis-819.notion.site/Team-Contract-2ef91847e7e380759e9ed376a5e25f47?source=copy_link) (WIP)
  - [Meeting minutes](https://humane-amaryllis-819.notion.site/2ef91847e7e38061bca0ed8e7fcc32a6?v=2ef91847e7e38050bd08000c6a395b5f&source=copy_link) (WIP)

## Backend notes (expenses & avatars)

- **Expenses backend**: the Bills tab is wired to Supabase (`expenses` and `expense_splits` tables). Creating an expense inserts one `expenses` row and equal `expense_splits` rows for the selected roommates. Marking a split as paid updates `expense_splits.is_paid` and recomputes the current user’s net balance. RLS on `expenses`/`expense_splits` is set up so any house member can view and update expenses for their house.

- **Avatars (profile pictures)**: we originally implemented avatar upload to Supabase Storage (`avatars` bucket) and `profiles.avatar_url` stored the public Storage URL. Due to persistent Storage RLS issues in our project (uploads blocked even with reasonable policies and no ability to disable RLS on `storage.objects`), we reverted to a simpler approach for the final milestone: the client stores the local image URI (`content://...`) in `profiles.avatar_url` and displays it while the app has access. This keeps the UI working for demos without relying on Storage configuration. In a production deployment we would:
  - keep `avatar_url` as a remote URL,
  - upload avatars to a Storage bucket behind server/Edge Function code using a service role key, and
  - tighten Storage RLS so users can only modify their own avatar objects.

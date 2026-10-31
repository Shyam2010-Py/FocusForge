# Phase 4 — Groups and Individual Focus Status

## Product rules
- Groups are member-invitation based; there is no public group directory.
- A group creator is its owner. The owner can share the generated 8-character invitation code.
- Members join with the code. The database validates the code and creates membership atomically.
- Every member's focus timer remains independent. No group-wide or synchronized countdown exists.
- A member's own device publishes their status for each group they belong to: focusing, paused, not focusing, or completed.
- A running status includes the member's own start time, end time, and planned duration.
- Expired running timestamps are rendered as not focusing by the client.

## Database
- `groups`: group metadata and owner.
- `group_members`: membership, role, and display name.
- `group_invites`: invitation codes, expiry/revocation and use limits.
- `group_focus_status`: one independent status row per group/member.
- RLS restricts group data to members. Members can read other members' names and statuses, but can only write their own focus status.
- `create_group` creates a group, owner membership and invitation code in one database transaction.
- `join_group_by_code` validates and consumes an invitation code in a transaction.

## Android
- A Groups tab is available in bottom navigation.
- Users can create groups, join using a code, view their groups, copy owner invitation codes, and view member names and focus states.
- The Focus screen publishes status transitions to all of the current user's groups.
- Group member lists refresh periodically; a dedicated Realtime subscription and deep-link invitation URLs remain follow-up work.

## Verification required
1. Build the Android debug APK through GitHub Actions.
2. Create a group with account A and copy its code.
3. Join with account B.
4. Start/pause/stop timers independently on both devices.
5. Confirm each account sees the other member's correct status.
6. Test expired/invalid invitation codes and access isolation.

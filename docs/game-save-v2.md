# GameSave V2 backend integration

## Current scope

This branch introduces the persistence and content-object foundation only. It does not expose an unauthenticated GameSave HTTP API yet.

Implemented:

- GameSave account/device/library/object/snapshot/snapshot-file/sync-head schema.
- Internal file caller identity bridge for `OWNER_ONLY` access.
- Authorized hash lookup inside `module.file`.
- User-level content object missing check and upload/dedup flow.
- Server-side SHA-256 and size verification after file upload.

## Security boundary

The desktop client must never receive `X-File-Api-Key`. A later GameSave device-token interceptor authenticates the user/device, builds `GameCallerContext`, and calls `GameObjectService` or snapshot services inside the same Spring Boot process.

## Next transaction

Snapshot commit will validate all referenced `game_object` rows, insert an immutable snapshot and its manifest, increment object references, and advance `game_sync_head` with a compare-and-set update. A failed CAS returns `409 SYNC_CONFLICT`.

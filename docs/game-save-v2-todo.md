# GameSave V2 TODO

- Device token authentication and `GameCallerContext` interceptor.
- Object check/upload REST endpoints behind device authentication.
- Immutable snapshot commit transaction.
- `game_sync_head` compare-and-set update and 409 conflict response.
- Snapshot delete/reference decrement and file lifecycle release.
- Integration tests for duplicate object upload and concurrent HEAD advance.

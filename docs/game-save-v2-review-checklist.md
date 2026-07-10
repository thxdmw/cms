# Review checklist

- [ ] Run `mvn test` / package on JDK 8 compatible toolchain.
- [ ] Apply `docs/db/file_system.sql` before `docs/db/game_save.sql` on a disposable database.
- [ ] Verify `game-save-private` bucket exists or storage bootstrap creates it.
- [ ] Add GameSave HTTP authentication before exposing object endpoints.
- [ ] Add concurrency tests for unique object identity and sync HEAD CAS.

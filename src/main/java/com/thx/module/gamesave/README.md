# GameSave Module

GameSave manages game metadata, snapshots, device synchronization and delegates file storage lifecycle to com.thx.module.file.

Architecture:

GameSave -> FileSystemService -> ObjectStorageClient -> MinIO

The module must not access MinIO directly.

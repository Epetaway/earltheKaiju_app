# Contracts

Shared data shapes between the Android app and the planned desktop companion.

| File | What it describes | Status |
|---|---|---|
| `kaiju-coach-backup.v1.schema.json` | The JSON backup the app exports today (Settings › Export backup), one array per Room table, tombstones included. The desktop companion will read and write records in this shape. | Matches Room schema v1 |

Rules

- Ids are UUID strings created on the device that made the record. Never reuse or regenerate.
- `createdAt` / `updatedAt` / `deletedAt` are epoch milliseconds (UTC). `deletedAt` set = tombstone.
- Class `date` and `startTime` are **local wall-clock** values (`2026-10-20`, `18:30`).
- Enums are stored by name. Adding a value is fine; renaming one is a breaking change.
- A breaking change bumps `formatVersion` and gets a new schema file; old files stay readable.

The sync protocol (planned) is specified in [`docs/06-sync-architecture.md`](../docs/06-sync-architecture.md).
These files are **contracts, not data**: real exports contain student names and must never be
committed (`.gitignore` blocks `kaiju-coach-backup-*.json`).

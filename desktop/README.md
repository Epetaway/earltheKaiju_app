# Desktop companion (planned)

A React + TypeScript app for planning on the computer: curriculum, lesson plans, roster and media
organization, with a small local service that syncs with the phone over Wi-Fi.

Not started. Sprint 01 prepared for it:

- Record shapes are defined in [`contracts/`](../contracts/) (the same JSON the phone exports).
- Every record has a stable UUID, `createdAt`, `updatedAt` and a `deletedAt` tombstone.
- Discovery, pairing, the sync exchange and conflict rules are specified in
  [`docs/06-sync-architecture.md`](../docs/06-sync-architecture.md).

Constraints carried forward: no cloud service, nothing exposed beyond the local network, the phone
never depends on the desktop to teach.

# 4. Key decisions

[← Architecture](03-architecture.md) · Next: [Quality →](05-quality.md)

Six of the fifteen records in [`decisions.md`](../decisions.md), in plain language. Each one shows what I chose, why, and what it cost.

---

## 1. Native Kotlin, not React Native (ADR-001)

**Context:** My professional stack is React and TypeScript, so React Native was the obvious choice.

**Decision:** Native Android with Jetpack Compose.

**Why:** The app depends on platform features: keeping the screen on, audio routing, haptics, a foreground service and, later, local-network discovery. All of these are first-class in native Android. A native project also shows range next to my React work.

**Cost:** A second language to maintain, and the planned React desktop companion can't share UI code with the app.

---

## 2. The phone is the source of truth (ADR-002)

**Decision:** All data lives in Room on the phone, and there's no server.

**Why:** Classes happen without reliable signal. The phone must never wait on a network.

**Cost:** Sync has to be designed carefully. That's why UUIDs, timestamps and tombstones went into schema v1, before any sync code exists.

---

## 3. Derive the timer from a monotonic clock (ADR-005)

**Decision:** Store start and pause anchors, derive everything from `elapsedRealtime()`, redraw on second boundaries, and drop cues that would fire late.

**Why:** Accuracy can't depend on frame rate, and the timer has to survive the app being backgrounded or killed.

**Cost:** A reboot mid-round loses the run. That case is detected and handled.

---

## 4. Copy the lesson plan when a class starts (ADR-006)

**Decision:** Copy the plan's segments and technique names into the session at Start Class.

**Why:** A past class has to keep what was actually taught, even after the template changes.

**Cost:** Some duplicated text. It's intentional.

---

## 5. Store as little about students as possible (ADR-011)

**Decision:** Name, belt and stripes, an under-18 flag, programs and one teaching note. No birth dates, contacts, photos or medical data.

**Why:** Some of these students are children. Store only what serves teaching.

**Cost:** I look up parent contacts outside the app for now.

---

## 6. Ship updates without risking real data (ADR-013)

**Context:** Every CI build signed with a fresh key. Android only lets an update install over an app signed by the same key, so the first update would have meant uninstalling, and uninstalling deletes everything.

**Decision:**
- A stable personal signing key.
- The key lives only in a repository secret and my password manager.
- CI checks the certificate fingerprint before publishing.
- The version code is the CI run number, so every build installs over the last.
- The test build ("Kaiju Test") is a separate app with its own data.

**Why:** The app is only useful once real classes live in it, and that requires updates that never strand data.

**Cost:** Losing both copies of the key would force an uninstall on every phone, so the backup is mandatory. Google's developer verification arrives in the US in 2027 and will need a free registration, which the stable key makes possible.

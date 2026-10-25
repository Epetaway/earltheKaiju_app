# 07 · Timer: Android behavior and what Sprint 01 supports

## How the timer keeps time

The countdown is never "subtract one each second." A run stores three anchors, all on
`SystemClock.elapsedRealtime()` (monotonic, keeps counting in deep sleep, unaffected by the user
or the network changing the wall clock):

```
startedAt, pausedAccumulated, pausedAt?
active   = (pausedAt ?: now) - startedAt - pausedAccumulated
interval = first interval whose end > active
remaining = interval.end - active          // displayed rounded up to whole seconds
```

The screen redraws exactly when the displayed second changes (`TimerEngine.millisUntilNextSecond`),
but accuracy never depends on redraws: a frame dropped, a slow device, or the app being frozen for
a minute changes nothing. Tested in `TimerEngineTest` (12 cases) and `TimerControllerTest`.

## Behavior matrix

| Situation | What happens in Sprint 01 | Verified by |
|---|---|---|
| Timer on screen | Exact countdown; bells, 3-2-1 count-in, warning clicks and vibration at transitions. | Unit tests; **device check needed** |
| Screen would time out | Screen is kept on while a timer runs (Settings › Keep screen on, default on). | Device check |
| Moving to Class Mode, Attendance, other tabs | Timer keeps running (one app-wide controller); cues keep playing; Class Mode shows a live mini timer. | Code path; device check |
| App in background, process alive | Countdown stays correct. Cues may play while Android lets the process run; Android may freeze cached apps. | Device check |
| App frozen then resumed | Display jumps to the correct time. Cues whose moment passed more than 1.5 s ago are **dropped** so a bell never rings late. | `TimerEngineTest` |
| Process killed (swiped away, memory pressure) | Run anchors are saved in DataStore; on reopen the timer resumes at the right time. | `TimerControllerTest` |
| Phone rebooted | `elapsedRealtime` restarts at boot, so a saved run is discarded (detected via `Settings.Global.BOOT_COUNT`); the chosen preset is kept. | `TimerControllerTest` |
| Phone locked mid-round | **Not supported in Sprint 01.** The count stays correct, but transition sounds are not guaranteed while locked. | Documented limitation |
| Notification / lock-screen controls | **Not in Sprint 01.** | Planned |
| Bluetooth speaker | Cues use the media stream (`USAGE_MEDIA`), so they follow the connected speaker and the media volume. | Device check |
| Do Not Disturb | Media stream is not silenced by DND by default; vibration may be. | Device check |

## Why locked-screen cues are deferred

Reliable sound while the phone is locked needs work Android restricts deliberately:

- **Foreground service.** Since Android 14 every foreground service needs a declared type. A plain
  interval timer fits none of the specific types: `health` requires body-sensor permissions,
  `mediaPlayback` is for continuous media, and `shortService` stops after about 3 minutes. The
  remaining option is `specialUse` with a written justification (reviewed by Google Play if the app
  is ever published; this app is sideloaded).
- **Exact alarms.** Since Android 14, `SCHEDULE_EXACT_ALARM` is denied by default for new installs;
  the user must grant "Alarms & reminders" in Settings. `USE_EXACT_ALARM` is reserved for alarm and
  calendar apps under Play policy.
- **Notifications.** Android 13+ requires the `POST_NOTIFICATIONS` runtime permission for the
  ongoing timer notification.

Doing this half-way would produce a timer that *sometimes* rings when locked — worse on the mat
than one that is honest about needing the screen on. In class the phone is propped up with the
screen on, which Sprint 01 handles fully.

## Plan (Sprint 02 candidate)

1. `TimerService`: foreground service, type `specialUse`
   (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE = "Interval timer for martial-arts training rounds"`),
   started only by the user pressing Start, stopped on finish/reset.
2. Ongoing notification with round, phase, a `Chronometer` counting to the next transition, and
   Pause/Resume/Reset actions; lock-screen visibility public (no student data in it).
3. The same `TimerEngine` drives it; cues come from the service, the UI only observes.
4. Request `POST_NOTIFICATIONS` at first Start, with a one-line rationale.
5. Test on the instructor's phone: locked screen for a full 5 × 5:00 program, Bluetooth speaker,
   DND on/off, battery saver on.

## Device checklist for Earl

Run these on your phone with a debug build (Settings › Load sample data gives you presets):

- [ ] Start "Sparring rounds"; confirm the count-in beeps at 3-2-1 and the double bell at round start.
- [ ] Let a round reach the 30-second warning: three clicks + gold border.
- [ ] Pause for 20 seconds, resume: the remaining time continues from where it paused.
- [ ] Switch to Class Mode and back while it runs: no reset, cues still play.
- [ ] Swipe the app away mid-round, reopen: the timer resumes at the right time.
- [ ] Connect a Bluetooth speaker: cues play through it.
- [ ] Check readability from across the mat in work (deep emerald) and rest (shadow + gold).

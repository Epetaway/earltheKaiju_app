# 07 · Timer: Android behavior and what Sprints 01–02 support

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

| Situation | What happens | Verified by |
|---|---|---|
| Timer on screen | Exact countdown; bells, 3-2-1 count-in, warning clicks and vibration at transitions. | Unit tests; device run |
| Screen would time out | Screen is kept on while a timer runs (Settings › Keep screen on, default on). | Device check |
| Moving to Class Mode, Attendance, other tabs | Timer keeps running (one app-wide controller); cues keep playing; Class Mode shows a live mini timer. | Device run |
| **Phone locked mid-round** (Sprint 02) | `TimerService` keeps the process in the foreground and holds a partial wake lock while a round runs, so cues ring on time with the screen off. | `TimerServiceTest`; **Earl's checklist below** |
| **Notification / lock-screen controls** (Sprint 02) | Ongoing notification: preset name, round and phase, a live countdown to the next change, **Pause/Resume** and **Reset**. Visible on the lock screen; it holds no student data. | `TimerServiceTest`; device check |
| Notifications not allowed | The timer and the locked-screen cues still work; only the notification is hidden. Start asks once on Android 13+. | Code path |
| App in background, process alive | Same as locked: the service keeps it running. | Device check |
| App frozen then resumed | Display jumps to the correct time. Cues whose moment passed more than 1.5 s ago are **dropped** so a bell never rings late. | `TimerEngineTest` |
| Process killed (swiped away, memory pressure) | Run anchors are saved in DataStore; on reopen the timer resumes at the right time and the service starts again. | `TimerControllerTest` |
| Phone rebooted | `elapsedRealtime` restarts at boot, so a saved run is discarded (detected via `Settings.Global.BOOT_COUNT`); the chosen preset is kept. | `TimerControllerTest` |
| **Music playing** (Sprint 02) | Each burst of cues takes transient may-duck audio focus: the music app dips under the bell and comes back 1.5 s after the last cue (a 3-2-1 count-in dips it once). | Device check |
| **Music turned down** (Sprint 02) | By default cues play on the **alarm** stream, so lowering the media volume never quiets the bell. Settings › "Bells stay loud over music" off puts cues back on the media stream. | Device check |
| Bluetooth speaker | Alarm stream (default): Android plays the bell on the phone speaker and usually on the connected speaker too. Media stream (setting off): the bell follows the speaker exactly like the music. | Device check |
| Do Not Disturb | Alarm stream: plays unless DND is set to block alarms. Media stream: not silenced by default. Vibration may be. | Device check |

## How the lock-screen timer works (Sprint 02, ADR-014)

1. `TimerService`: foreground service, type `specialUse`
   (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE = "Interval timer for martial-arts training rounds"`).
   `TimerService.follow` (called from the Application) starts it whenever a run becomes active,
   including one restored after the process was killed. It stops itself on Reset, and 5 s after
   the program finishes so the final bells play out.
2. It owns no timer logic. `TimerController` still counts and fires cues; the service only keeps
   the process alive, holds the wake lock, and shows the notification.
3. **Wake lock**: partial, held only while a round is *running* (released when paused), with a
   timeout of the program's remaining time plus one minute, capped at 4 hours.
4. **Notification**: channel "Training timer", low importance (silent), public on the lock screen.
   It's rebuilt only when the status or interval changes; the per-second countdown is the
   notification's own chronometer, so nothing posts every second.
5. `POST_NOTIFICATIONS` is requested when Start is pressed on Android 13+. Declining it doesn't
   stop the timer.
6. Exact alarms aren't used: the wake lock covers a class-length run without asking for the
   "Alarms & reminders" permission.

## Device checklist for Earl

Run these on your phone with a debug build (Settings › Load sample data gives you presets):

- [ ] Start "Sparring rounds"; confirm the count-in beeps at 3-2-1 and the double bell at round start.
- [ ] Let a round reach the 30-second warning: three clicks + gold border.
- [ ] Pause for 20 seconds, resume: the remaining time continues from where it paused.
- [ ] Switch to Class Mode and back while it runs: no reset, cues still play.
- [ ] Swipe the app away mid-round, reopen: the timer resumes at the right time.
- [ ] Connect a Bluetooth speaker: cues play through it.
- [ ] Check readability from across the mat in work (deep emerald) and rest (shadow + gold).

**Sprint 02, lock-screen timer** (personal build, Pixel 9):

- [ ] Press Start; allow notifications when asked. A "Training timer" notification appears.
- [ ] Lock the phone for a full 5 × 5:00 program (or a short custom one). Every bell, the
      warning clicks and the 3-2-1 count-in play on time with the screen off.
- [ ] From the lock screen, Pause, wait, Resume, then Reset. The notification goes away on Reset.
- [ ] Play Spotify, start a round: the music dips under each bell and comes back.
- [ ] Turn the media volume down low: the bell stays loud. Then turn "Bells stay loud over music"
      off in Settings and confirm the bell follows the media volume.
- [ ] Repeat with the gym Bluetooth speaker, with the setting on and off; note which sounds better.
- [ ] Repeat once with Battery Saver on.

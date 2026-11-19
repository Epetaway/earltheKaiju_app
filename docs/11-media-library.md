# 11 · Media library (Sprint 03)

Earl asked for this on 2026-11-18: upload his own technique videos into the app, keep them
private, label them by move, pull up clips of a move while planning or teaching, and share them
out. The plan is in the project doc *Kaiju Coach: media library, Class Hub, and Class Director
plan*.

## What it does

| Feature | How |
|---|---|
| **Import** | Library › Media › Import. Android's photo picker, videos only, up to 20 at a time; no storage permission needed. Each video is **copied** into the app's private folder. |
| **Private folder** | `filesDir/media`. Other apps, Google Photos and the file manager can't see it. Cloud backup and device transfer are off for the whole app (`data_extraction_rules.xml`). |
| **Labels** | Type (Instruction, Drilling, Rolling, Competition, Other), position (one-tap suggestions or typed), techniques from the technique library, notes, and two flags: *Shows students* and *Shows minors*. |
| **Clips** | Bookmarks inside a video: start and end time, name, techniques, notes. No copy, no re-encoding, no extra space. Made with *Start here* / *End here* while the video plays. |
| **Player** | Play, pause, seek; **loop**; **½× and ¼×**; **mirror** (flip left and right to show the other side); **sound off**, which also stops the video from pausing the class music. A clip plays on its own and loops. |
| **Find** | Search covers titles, positions, notes, clip names and technique names (video or clip). Type chips filter. A technique's page lists its videos and clips. |
| **Share** | The share sheet sends the whole video to any app (YouTube, Drive, Messages…). If it's flagged as showing students or minors, the app asks first. |
| **Save all to a folder** | Copies every video plus `kaiju-media-labels.json` (titles, labels, clips) to a folder the instructor picks. This is the backup for media. |
| **Delete** | Deleting a video deletes its file right away and frees the space. The database keeps a tombstone, like every other record. |

## Limits, stated plainly

- **Uninstalling the app deletes the private folder.** Updates (the normal build-N flow) keep it.
  Use *Save all to a folder* now and then. The empty state and the Media tab both say this.
- An import runs while the app is open. Leaving the Media screen is fine; closing the app during
  a big import can stop it. Anything half-copied is cleaned up, and nothing half-imported shows.
- Sharing a single clip (cutting a new file) isn't built yet; sharing sends the whole video.
- Restoring from *Save all to a folder* isn't built. The labels file is readable JSON so it can
  be later.
- TV playback (Cast) isn't built. Android's own screen cast works as a stopgap.
- Storage: phone video is roughly 100–200 MB a minute at 1080p. The Media tab shows the total.

## How it's built

- **Data** (Room v2, migration `1 → 2`):
  - `media_item`: title, file name, size, duration, size in pixels, kind, position, notes,
    flags, recorded time.
  - `media_clip`: video, name, start and end (ms), notes.
  - `media_technique` and `clip_technique`: links to `technique`.
  - Soft deletes, like every table. A deleted technique stops labelling anything.
- **`MediaRepository`** combines videos, clips and technique links into `MediaVideo` objects for
  the screens.
- **`MediaTransfers`** runs imports and exports one at a time on the app scope, with progress.
  It reads duration, size and recording date from the file, and saves a 480 px JPEG thumbnail.
- **Player**: Media3 ExoPlayer 1.11.1 with a `PlayerView` inflated from
  `kaiju_player_view.xml`. It uses a TextureView so Compose can mirror it, and the app's own
  Compose controls. A clip is played with ExoPlayer clipping, so the bar, time and loop cover
  just the clip.
- **Share** uses a `FileProvider` (`${applicationId}.media`) limited to the media folder.

## Tests

- `MediaLibraryTest`:
  - migrates a real version-1 database built from the committed `1.json`, keeping a technique,
    then uses the new tables;
  - labels, clip ordering and clamping, search, a technique's videos, deleted techniques, and
    deleting clips and videos (including the file).
- Screenshots:
  - `08b-library-media`: the list.
  - `08c-library-media-empty`: empty state with an import in progress.
  - `08d-video`: a video with clips and a mark set.
  - `08e-video-clip`: playing a clip, mirrored, sound off.

## Earl's checklist on the Pixel

- [ ] Import two or three real videos, including a long one; watch the progress.
- [ ] Confirm they don't appear in Google Photos as new items (the originals are still there).
- [ ] Label one, mark two clips with techniques, then open the technique's page and jump to a clip.
- [ ] Try ½×, ¼×, mirror and loop on a clip. Play with Spotify running, sound off, then on.
- [ ] Share a short video to yourself in Messages or Drive.
- [ ] Save all to a folder, and check the files and `kaiju-media-labels.json` arrived.

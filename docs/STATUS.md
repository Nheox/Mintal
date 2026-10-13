# Status

Last updated: 2026-10-13, after the first round of on-device feedback.

## Where the project stands

The v1 feature set from the specification is implemented and builds: countdown and
count-up sessions on a foreground service, layered distraction detection with a warned
grace period, the Grove, twelve *Mentha* species on cumulative-time milestones, tags,
ratings and notes, statistics with hand-drawn charts, CSV export and JSON backup/restore,
the radial-hub home.

Verified: 43 unit tests (86 executions across both variants) pass, both APKs build, lint
vital is clean. The app has been installed and used on a real device once, which found
eleven issues; all of them are addressed in the branch.

Not verified: everything behavioural. Nobody has yet confirmed on a device that the
foreground service survives a long session, that the notification updates, that the grace
period fails a session correctly, or that the result screen now appears — the last of
which was the most serious bug found so far.

## From the first device session

| Reported | Status |
|---|---|
| No rating/note prompt after finishing a session | Fixed — the controller cleared the session before writing the row, so the UI read it as abandoned. Regression test added. |
| Session bar fill overflows the track | Fixed — the fill is clipped to the bar's shape. |
| Light mode has no visible boxes | Fixed — page, card and edge are now distinct tones, cards have a border. |
| Mints are just green lines | Fixed — a NaN collapsed every leaf path. Confirmed by rendering. |
| Buttons disappear in a small multi-window pane | Fixed — chip and button rows wrap. |
| Numbers overlap when the window is small | Fixed — the session clock sizes itself to the available width. |
| Summary shows weekdays in Spanish | Fixed — forced to English while the copy is English. |
| Chart gives no sense of scale | Fixed — labelled value axis and a named x axis. |
| Tags can only be made in the tag manager | Fixed — the picker creates them, and links to the manager. |
| "Day starts at" reads like a weekday question | Fixed — renamed, and the missing "Week starts on" control added. |
| Minimum window size to avoid layout breakage | Not doing — Android has no polite way to refuse a small window; the adaptive layout is the fix. |
| Typography | Keep as is, per the owner. |

## Open decisions

- **Artwork.** The mints are drawn procedurally from botanical traits and are now legible
  and distinct. The owner is considering generated illustrations. Agreed shape if it
  happens: whole-plant raster (~512px WebP) for the catalogue and Herbarium cards, one
  vector leaf per species for the Grove, and a loader that prefers a bundled asset and
  falls back to drawing — so art can arrive one file at a time. Photographs of real plants
  are reference only; shipping them has a licensing problem the project cannot absorb.
- **Merging to `main`.** Deliberately deferred until the app has been exercised properly.
- **Signing.** Only debug APKs exist. A release key would make builds installable as real
  updates; the keystore must be generated locally and never committed.
- **Spanish.** Wanted. Blocked on moving UI strings into `strings.xml`.

## Next, in the order worth doing it

1. Install the current build and exercise it: finishing a session, giving up, leaving the
   app mid-session, a long session with the screen off.
2. Screenshot the remaining screens (Home, running session, Grove, Statistics) and fix
   what they show — the renderer exists, the screens just need splitting into pure
   content functions that take state.
3. UI strings into `strings.xml`, then the Spanish translation.
4. Accessibility pass: TalkBack labels on hub tiles and chips, contrast in both palettes,
   touch targets. The specification calls this a release gate.
5. Release signing, then a tagged release, then the `fdroiddata` recipe.

## Known gaps

- No instrumented tests. The service, the database and the Compose screens have no
  automated coverage; unit tests cover the state machine, statistics, Grove layout,
  milestones, CSV and time bucketing.
- A session is held in memory by the controller. If the process is killed mid-session the
  session is lost; the foreground service makes this unlikely but not impossible.
- UI strings are hardcoded, so the app cannot be translated yet.

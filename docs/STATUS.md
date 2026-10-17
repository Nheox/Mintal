# Status

Last updated: 2026-10-17, after publishing 0.2.0 and checking it reproduces.

## Where the project stands

The v1 feature set from the specification is implemented and builds: countdown and
count-up sessions on a foreground service, layered distraction detection with a warned
grace period, the Grove, twelve *Mentha* species on cumulative-time milestones, tags,
ratings and notes, statistics with hand-drawn charts, CSV export and JSON backup/restore,
the radial-hub home.

Verified: the unit suite passes (92 tests, both variants), both APKs build, and the
screenshot renders were used to check every change to the artwork and the charts.

Verified on hardware: the app has been installed and used on the owner's phone across
three rounds of feedback, which is what produced every change below. The behavioural
doubts that stood here in earlier revisions — whether the foreground service survives a
long session, whether the notification updates, whether the grace period fails a session
correctly — are resolved by use.

Release signing is verified end to end: the Release workflow was run manually on `main`
on 15 Oct, the keystore and all four secrets were picked up, and `apksigner` confirmed the
signature. **0.2.0 is published**: the owner made the release on GitHub's website on 17 Oct,
with tag `v0.2.0` on `e8e40bf`, and the workflow attached the signed APK. Publishing from
the website fired *both* triggers, the release and the tag push; the second run re-uploaded
the APK over the first, as designed. The release is marked as a pre-release.

The build is reproducible: `v0.2.0` rebuilt from a fresh clone matches the published APK
byte for byte once its signature is copied over. How it was checked, and what could break
it, is in `docs/FDROID.md`.

The one thing with neither automated coverage nor a deliberate check is whether the
fifteen-second warning **makes a sound and pops over another app**. Posting it needs a real
notification manager, which the unit suite has none of. Re-confirm it on a device after any
change to the notification code.

## From the second device session

| Reported | Status |
|---|---|
| A mistaken START costs a plant | Fixed — START now arms a 5 second planting window. Cancelling, changing a setting, or leaving the hub writes nothing at all. |
| Chart axis labels read "0 0 0 0 1 1 1 2" | Fixed — labels are laid out by measurement, and thin out instead of being clipped. |
| Focus axis in seconds when it should be minutes | Fixed — `AxisTicks` picks round steps and one unit per axis. Tested over every plausible axis top. |
| "1 / 0 / 0" on the rating breakdown | Fixed — the same change; no axis can repeat a label. |
| No way to donate | Added — a Support card at the foot of Settings, above About. **The URL is still empty** (see below). |
| Permissions requested with no explanation | Fixed — each is explained in Mintal's words first, with "don't show again", and all of them stay readable in Settings. The notification permission was never actually requested before; now it is. |
| A new day should begin at 00:00 local | Confirmed and pinned by a test. The hour stays configurable, with 00:00 the default. |
| "Study tip" wording | Kept, per the owner. Citations checked; two lines were wrong and were rewritten. |
| Unlock order is fixed | Changed — a milestone now earns a *slot*, and the player picks which mint fills it. |
| One plant grows forever | Fixed — a plant matures at twelve nodes and the next session starts another beside it. |
| Grove art wrong against the reference | Rewritten — see `docs/SPEC.md` §8.3 for the corrected table. |
| Leaves grow singly | Fixed — one session is one node: a stem segment and an opposite pair, decussate, with branches from leaf axils. |
| "Don't show again" belongs on the principles, not the permissions | Fixed — the principles now stand in front of the hub at every launch until dismissed, and stay readable at Settings → Mintal's principles. The permission notes keep their own, separate tick. |
| Mid-session kill listed twice in Settings | Fixed — the *Device* card is gone; battery optimisation lives with the other permissions, once. |
| The Grove could be dragged into nowhere | Fixed — panning keeps 30% of each axis on content, and a **Reset view** button appears once the view has been moved. Invariant covered by a test. |
| A young Grove is drawn too small | Fixed — the band is framed against what is actually in it, between one mature plant and one seedling. |
| No slogan | Added — "Stay focused, stay mintalized", under the name on the principles screen and in About. |
| No tutorial | Added — a looping three-beat animation (set the timer, keep off the phone, plant a mint), drawn with the app's own plant code rather than shipped as a video. Falls back to plain numbered text where animations are turned off. |
| First run was one long page | Split into four swipeable slides: name and slogan, the tutorial, the promises, the permission. One idea each, centred, with the action pinned to the bottom. |
| The phone's nav buttons covered the Next button | Fixed — the first-run footer pads for the navigation bar. The renderer can fake those buttons, and the clearance is measured: exactly one bar's worth, applied once. |
| The principles returned at every launch | Removed — they are read once in the first-run deck and kept in Settings. No gate, no tick-box, no stored state. |
| A dot at the end of the progress bar | Removed — Material 3's stop indicator, switched off on all three progress bars. |
| Three buttons on the result screen, two alike | Cut to **Save** and **Discard**. "Save and start another" navigated exactly where "Save" did. |
| The notification note had a tick-box | Removed, and with it the whole permission-note dismissal: the note is an advert for the permission with the grant button under it, shown once before Android asks. |

## From the third device session

Five changes. Three other things were asked for, discussed and dropped — see
"Decided against", below.

| Reported | Status |
|---|---|
| The Tag and Change-mint boxes have no margins | Fixed — both were hand-rolled `Card`s with 14dp and no border beside everyone else's `SectionCard`. `SectionCard` now takes an `onClick`, and `SectionCardScreenshotTest` stacks a plain and a tappable box so the next one cannot drift. |
| Leaving the app kills the plant | Softened, in two steps. Failed sessions no longer stand in the Grove at all by default — they stay in the statistics, where the honest record belongs. Turning them back on is a switch in Settings, and a stub then fades out over a fortnight instead of standing there for good. What a failure costs is unchanged: no node, no time toward a milestone. |
| The 15 second warning arrives silently | Fixed — it is now its own notification on its own high-importance channel, so it pops over whatever app is in front and makes a sound. The running-session notification stays quiet, which is what it is for, and Android will not let a channel's importance be raised after it is made, so the two have to be separate. |
| "Save" and "Discard" describe something neither button does | Renamed to "Save rating and note" and "Skip", with a line above them saying the session is kept either way. The row is written when the session ends; nothing on that screen can create or delete it. |
| The species list wants a single-entry card view | Added — a List/Cards toggle in the top bar, and tapping a row opens that mint's card. One mint per page, swiped horizontally: the plant at 220dp, the catalogue note, the field marks read back out of the same drawing parameters the art uses, and its lock or selection state. |

## Decided against

Recorded because each was built or half-built and then removed on purpose, and the next
session should not quietly re-propose them.

- **A floating overlay over other apps.** It wanted `SYSTEM_ALERT_WINDOW` — the permission
  overlay malware uses — in an app whose README promises minimum permissions, and it only
  earned that during the fifteen seconds of the grace period. Built, reviewed, removed —
  and then superseded, because the high-importance warning channel carries the one thing
  the overlay was actually for and costs no permission at all.
- **Slowing the first-run tutorial.** A beat went from 2.6 s to 5.2 s and back. More
  seconds treats the symptom; if it still reads too fast on a device, the fix is letting
  the reader advance or pause the beats.
- **F-Droid submission material.** Store metadata, a build recipe and release signing were
  prepared and then removed: the owner is not submitting yet, and scaffolding that sits
  unused for months goes stale and gets submitted stale. Worth redoing when the app is
  actually ready to go out, against whatever F-Droid's requirements are then.
- **Usage Access on by default, or nudging toward it.** Offered after the home-screen
  report, along with making leaving harmless without it. The owner chose to keep it as
  it is: Usage Access sees every app you open, which for this app is a tracker, and the
  default build stays at the permissions nobody can avoid. The home screen therefore
  counts as leaving by default, knowingly.
- **Returning to the hub during a pause to change settings.** Asked as an either/or
  against changing the tag during a session; the tag was built instead (spec §5.2). The
  hub would have to understand a half-finished session to offer the one change that is
  safe to make, and the planting window already covers correcting the rest before a
  session starts.

## Needs the owner before release

- **The F-Droid merge request.** The recipe in `docs/FDROID.md` is ready to submit as it
  stands, reproducible build included. It is the owner's to open, from a GitLab account.
- **0.1.0 was never published.** A signed 0.1.0 (code 1) was built on 15 October by a
  manual workflow run and only ever existed as an artifact. The first public release is
  0.2.0 so that nothing can mistake one for the other.
- **The donation address.** `ui/settings/Donation.URL` is an empty string. The owner has
  deferred this until there are testers to ask. Until it is filled in, the Settings card
  renders with its button disabled and says so. One line to change; no other code knows
  the URL.

## Done since the third device session

- **Release paperwork for 0.2.0.** Version bumped; `changelogs/2.txt` written (F-Droid's
  "What's new", 424 of 500 characters), and the release workflow now puts the same file at
  the top of the GitHub release, so the two cannot disagree. `dependenciesInfo` is off in
  `app/build.gradle.kts`: AGP otherwise writes the dependency list into the signing block
  encrypted with a Google key, which F-Droid's scanner flags. The built release APK was
  checked: version 0.2.0 (2), no `INTERNET`, the permissions the spec lists and no others.
  `docs/FDROID.md` holds the `fdroiddata` recipe and both ways to submit it. The README
  says how to move from a CI debug build to a release without losing the Grove; CREDITS
  lists the two test dependencies it was missing.

- **The spec is in the repository and the amendments are folded into it.** The owner
  supplied spec v0.3, and `docs/SPEC.md` is now v0.4: every amendment is merged into the
  section it belongs to, §13.1 records why each change was made, and
  `docs/SPEC-AMENDMENTS.md` is deleted. Two of its entries were themselves out of date and
  were folded in as the code actually behaves — §5.6's permanent wilted stub (failures are
  `Hidden` by default now) and §5.4's "Save / Discard" buttons ("Save rating and note" and
  "Skip"). Four §8 rows that had never matched the build were corrected at the same time:
  Coil, the SVG build step and the Apache CSV dependency are gone, and Robolectric is
  recorded as the way the UI is seen without a device.
- **Merged to `main`.** `main` is at `ff1b43c` and carries everything.
- **The lock screen no longer fails a session.** Reported from a device: leaving the app
  while counting up, then looking at the lock screen, failed the session. The spec has
  said "screen-off never fails a session" since v0.3 and the code checked
  `PowerManager.isInteractive`, which is false only while the device is *asleep* — the
  keyguard is interactive, so a woken lock screen, an always-on display and lift-to-wake
  all read as leaving the app. A locked device is now checked explicitly.
- **The implicit allowlist covers the system surfaces.** SystemUI (lock screen, shade,
  recents), every installed home screen, telecom, settings, the keyboard in use,
  permission and installer dialogs, and the clock app. The camera, browser and gallery are
  deliberately excluded — opening one is using something else. The home screen was
  excluded too at first and was added after a device report; the manifest gained a
  `<queries>` entry for `CATEGORY_HOME`, a visibility declaration and not a permission.
  This list only takes effect under detection layer B, which is off by default.
- **Layer B was only catching app-hopping, and now catches staying.** It read the last ten
  seconds of the usage log and treated an empty window as "nothing in front", but the log
  records switches, not states, and ten seconds is shorter than the grace period — so
  opening one app and staying in it cleared the countdown before it ran out. The last app
  seen in front is now sticky, and reset to Mintal whenever Mintal comes back.
- **The away decision is a pure function**, `DistractionPolicy`, in `domain/session/`,
  with every exemption pinned by name in `DistractionPolicyTest` — including the
  limitation, that without Usage Access the home screen cannot be told from an app. The
  service only gathers the signals.
- **A two-minute pause, with thirty seconds between pauses.** Clock stopped, leaving the
  app harmless while it runs, auto-resuming when it expires. When a pause ends, the button
  stays on screen disabled and counts down "Pause again in 30s". It first shipped as once
  per session; the owner asked for the count after using it. Paused time is subtracted
  from the session, so a paused countdown ends later rather than early, and a pause
  expiring while the user is still away restarts the grace period from zero rather than
  failing instantly.
- **The tag can be changed during a session.** The running screen's tag chip is always
  shown now (as "Add a tag" when there is none) and opens the same picker as the hub,
  extracted into `ui/components/TagPickerDialog.kt` so the two cannot drift. Only this
  session's tag changes; the hub's choice for the next one is left alone. The chip used to
  have an empty click handler. Two tests in `SessionControllerTest`.
- **The Usage Access switch is disabled while "Fail a session if I leave the app" is
  off**, and says why. Layer B only refines layer A, so with layer A off it did nothing,
  while looking switchable — which is how it was tested on a device and reported broken.
- **The Give Up dialog stopped describing a wilted sprig in the Grove**, which failures
  have not left by default since the third device round.
- **The species catalogue remembers its layout.** Choosing Cards used to last until the
  screen left the back stack, because the layout was a `rememberSaveable` inside it. It is
  a DataStore preference now, travels in backups, and has the first unit coverage any
  preference in this project has had (`MintalPreferencesTest`). The enum moved from
  `ui/species/` to `data/prefs/`, next to `ThemeChoice`, since it is persisted.

## Open decisions

- **Artwork.** The mints are drawn procedurally from the photo reference: outline,
  attachment, margin, surface, hairs, stem colour, flower form. They are legible at Grove
  size and correct against the reference sheet. The offer of commissioned illustration
  still stands, and the loader shape agreed earlier is unchanged: whole-plant raster for
  the catalogue, one vector leaf per species for the Grove, falling back to drawing. The
  owner is going to ask about illustrators at UdeC. If one is commissioned, the licence
  has to be free and recorded in `docs/CREDITS.md` or F-Droid cannot ship the art.
- **Translation.** Deferred by the owner — no Spanish and no other language for now. The
  prerequisite is unchanged if that changes: UI strings into `strings.xml` (only 9 are
  there; roughly 68 literals are in Compose across 70 files). Not worth doing before it is
  wanted, since its only consumer is translation.

## Next, in the order worth doing it

1. **Accessibility pass.** The spec calls it a release gate (§8.2). It is in better shape
   than earlier revisions of this file implied: there are no `contentDescription = null`
   anywhere, every icon is labelled, the canvas-drawn parts (Grove, charts, growth track,
   plant art) all carry descriptions, and a clickable `Surface` merges its text children,
   so the hub tiles already announce as "Grove, 14 plants". What is left is verification
   that needs a device: contrast at 4.5:1 in **both** palettes (light is the likelier to
   have a thin spot, having been retuned once for the card-separation bug), 48dp touch
   targets, and one TalkBack pass over hub → session → result → mint chooser.
   0.2.0 goes out ahead of it, at the owner's call; the gate is still the gate.
2. **F-Droid**, per `docs/FDROID.md`: the merge request to `fdroiddata` with the recipe
   there. Separately, screenshots from a phone into
   `fastlane/metadata/android/en-US/images/phoneScreenshots/` and a 512×512
   `images/icon.png`, which reach the listing with the next tagged release.
3. Fill in the donation URL, once there are testers to ask.
4. Translation, if and when it is wanted.
5. An illustrator, if the UdeC enquiry turns one up.

## Known gaps

- The grace warning notification has no automated coverage: posting it needs a real
  notification manager, and the unit suite has none. Whether it actually pops and makes a
  sound has to be checked on a device.
- No instrumented tests. The service, the database and the Compose screens have no
  automated coverage; unit tests cover the state machine, statistics, chart axes,
  milestones, plant growth, Grove layout, CSV and time bucketing. For a solo project this
  is a deliberate gap rather than an oversight: use on a real phone has caught more than
  instrumented tests would have, and the screenshot renders cover the drawing code.
- Gradle 8.14.3 warns that the build uses features incompatible with Gradle 9.0. Nothing
  is broken; it is a chore waiting for whenever AGP forces the upgrade.
- A session is held in memory by the controller. If the process is killed mid-session the
  session is lost; the foreground service makes this unlikely but not impossible.
- The planting window lives in the hub's ViewModel, so it is deliberately cancelled by
  leaving the hub. That is the intended behaviour, not a limitation, but it does mean the
  window cannot survive a process death — which costs nothing, since nothing was written.
- UI strings are hardcoded, so the app cannot be translated yet.
- **Without Usage Access, the home screen fails a session like any app does.** Android
  gives layer A no way to tell them apart. Accepted by the owner rather than worked
  around — see "Decided against" — and stated in the Settings copy so it is not a surprise.
- The tag picker dialog has no screenshot coverage either: an `AlertDialog` draws in its
  own window, and `ScreenshotRenderer` draws the activity's decor view only.
- The running screen, and so the pause button and its countdown, has no screenshot
  coverage: `SessionRunningScreen` calls `hiltViewModel()` itself. Extracting a
  `SessionRunningContent` the way `SpeciesContent` is would fix that.
- Twice now, on the first full test run after a cloud session started, every Robolectric
  test failed with `UnsatisfiedLinkError` from `org.robolectric.nativeruntime` — the
  native graphics library never loaded. Both times a re-run passed with no change, and CI
  has never shown it in 37 runs. It is this container, not the tests; `CLAUDE.md` says so
  where the next session will look.

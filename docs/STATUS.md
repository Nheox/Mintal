# Status

Last updated: 2026-10-13, after the second round of on-device feedback.

## Where the project stands

The v1 feature set from the specification is implemented and builds: countdown and
count-up sessions on a foreground service, layered distraction detection with a warned
grace period, the Grove, twelve *Mentha* species on cumulative-time milestones, tags,
ratings and notes, statistics with hand-drawn charts, CSV export and JSON backup/restore,
the radial-hub home.

Verified: the unit suite passes (82 tests, both variants), both APKs build, and the
screenshot renders were used to check every change to the artwork and the charts.

Not verified: everything behavioural, and now rather more of it than last time. Nobody has
yet confirmed on a device that the foreground service survives a long session, that the
notification updates, or that the grace period fails a session correctly — and the three
new flows (the planting window, the permission explanations, the mint chooser) have only
been exercised by reading the code.

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
| Grove art wrong against the reference | Rewritten — see `docs/SPEC-AMENDMENTS.md` §8.3 for the corrected table. |
| Leaves grow singly | Fixed — one session is one node: a stem segment and an opposite pair, decussate, with branches from leaf axils. |
| "Don't show again" belongs on the principles, not the permissions | Fixed — the principles now stand in front of the hub at every launch until dismissed, and stay readable at Settings → Mintal's principles. The permission notes keep their own, separate tick. |
| Mid-session kill listed twice in Settings | Fixed — the *Device* card is gone; battery optimisation lives with the other permissions, once. |
| The Grove could be dragged into nowhere | Fixed — panning keeps 30% of each axis on content, and a **Reset view** button appears once the view has been moved. Invariant covered by a test. |
| A young Grove is drawn too small | Fixed — the band is framed against what is actually in it, between one mature plant and one seedling. |
| No slogan | Added — "Stay focused, stay mintalized", under the name on the principles screen and in About. |
| No tutorial | Added — a looping three-beat animation (set the timer, keep off the phone, plant a mint), drawn with the app's own plant code rather than shipped as a video. Falls back to plain numbered text where animations are turned off. |
| First run was one long page | Split into four swipeable slides: name and slogan, the tutorial, the promises, the permission. One idea each, centred, with the action pinned to the bottom. |
| The notification note had a tick-box | Removed, and with it the whole permission-note dismissal: the note is an advert for the permission with the grant button under it, shown once before Android asks. |

## Needs the owner before release

- **The donation address.** `ui/settings/Donation.URL` is an empty string. Until it is
  filled in, the Settings card renders with its button disabled and says so. One line to
  change; no other code knows the URL.
- **The spec document.** `docs/SPEC-AMENDMENTS.md` holds the rewritten §5.1, §5.6, §6,
  §6.1, §8.3, §9, §4, §5.7 and §3.2 text, ready to fold into the spec in `reference/`.
  That file is not in the repository, so a cloud session cannot edit it directly.

## Open decisions

- **Artwork.** The mints are drawn procedurally from the photo reference: outline,
  attachment, margin, surface, hairs, stem colour, flower form. They are legible at Grove
  size and correct against the reference sheet. The offer of commissioned illustration
  still stands, and the loader shape agreed earlier is unchanged: whole-plant raster for
  the catalogue, one vector leaf per species for the Grove, falling back to drawing.
- **Merging to `main`.** Still deliberately deferred until the app has been exercised.
- **Signing.** Only debug APKs exist.
- **Spanish.** Wanted. Still blocked on moving UI strings into `strings.xml`.

## Next, in the order worth doing it

1. Install this build and exercise the new flows: arm a session and cancel it; arm one
   and let it run; refuse the notification permission and check a session still runs;
   reach a milestone and pick a mint; fill a plant to twelve nodes and watch the next
   session start a new one; relaunch twice to see the principles return, then tick the
   box and confirm they stop and are still in Settings.
2. Fill in the donation URL.
3. UI strings into `strings.xml`, then the Spanish translation.
4. Accessibility pass: TalkBack labels on hub tiles, chips and the mint chooser, contrast
   in both palettes, touch targets. The specification calls this a release gate.
5. Release signing, then a tagged release, then the `fdroiddata` recipe.

## Known gaps

- No instrumented tests. The service, the database and the Compose screens have no
  automated coverage; unit tests cover the state machine, statistics, chart axes,
  milestones, plant growth, Grove layout, CSV and time bucketing.
- A session is held in memory by the controller. If the process is killed mid-session the
  session is lost; the foreground service makes this unlikely but not impossible.
- The planting window lives in the hub's ViewModel, so it is deliberately cancelled by
  leaving the hub. That is the intended behaviour, not a limitation, but it does mean the
  window cannot survive a process death — which costs nothing, since nothing was written.
- UI strings are hardcoded, so the app cannot be translated yet.

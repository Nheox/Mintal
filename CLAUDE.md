# Working on Mintal

Context for any future session. Read this first; it is kept current deliberately, because
the chat that built this project will not survive.

## What this is

An Android focus timer built around real *Mentha* species, written from a product
specification (v0.3). Local-first, GPL-3.0, aimed at F-Droid. Finish a focus session and
it is grafted onto **the Grove**, one continuously growing mint network.

## Branch workflow

- Develop on `ccr-d6d58d3f-dpo66a`. Push there.
- `main` is deliberately untouched — the owner chose to keep the work on the branch until
  the app has been exercised on a device. Do not merge to `main` without being asked.
- Never force-push; never rewrite pushed history.

## Setting up a fresh cloud container

The container starts without an Android SDK, and two network facts matter:

1. `dl.google.com` must be allowed by the environment's network policy (it is, at the
   environment level — if a build suddenly cannot resolve AGP, check that first).
2. **Maven Central rate-limits this container**: the same artifact URL returns 200, then
   429, then 200. A build resolves hundreds of artifacts, so roughly a third fail and the
   build dies somewhere arbitrary. Google's mirror of Maven Central is reliable.

```sh
# 1. Android SDK (~490 MB, about two minutes)
SDK=/opt/android-sdk; mkdir -p "$SDK/cmdline-tools"
curl -sS -O https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
unzip -q commandlinetools-linux-16111833_latest.zip -d /tmp/clt
mv /tmp/clt/cmdline-tools "$SDK/cmdline-tools/latest"
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null 2>&1 || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
echo "sdk.dir=$SDK" > local.properties    # gitignored

# 2. Maven Central mirror, environment-only — NOT in the project's build files
mkdir -p /root/.gradle/init.d
cat > /root/.gradle/init.d/mintal-maven-mirror.gradle <<'INIT'
beforeSettings { settings ->
    settings.pluginManagement { repositories {
        maven { url 'https://maven-central.storage-download.googleapis.com/maven2/' } } }
    settings.dependencyResolutionManagement { repositories {
        maven { url 'https://maven-central.storage-download.googleapis.com/maven2/' } } }
}
allprojects {
    tasks.withType(Test).configureEach {
        systemProperty 'robolectric.dependency.repo.url',
            'https://maven-central.storage-download.googleapis.com/maven2'
    }
}
INIT
```

Keep that mirror out of `settings.gradle.kts`: CI, F-Droid and other contributors resolve
Maven Central directly and must keep doing so.

## Commands

```sh
export ANDROID_HOME=/opt/android-sdk
./gradlew test assembleDebug          # ~1m20s warm. Do this before every push.
./gradlew testDebugUnitTest --tests "*ScreenshotTest*"   # PNGs → app/build/screenshots/
```

The screenshot tests are the only way to see the UI here, and they are worth running on
any art or chart change: `species-sheet-*` (all twelve mints), `species-card-*` and
`species-list-light` (the two layouts of the catalogue), `grove-bed-*` (a mixed bed),
`grove-stages` (one mint at 1, 3, 6, 9 and 12 nodes), `section-cards-*` (plain and
tappable boxes on the same edges), and `charts-focus-*` and `charts-rating-*` (the
statistics axes at the sizes that broke them).

CI (`.github/workflows/build.yml`) runs the same thing plus `assembleRelease` and uploads
both APKs. It takes ~6 minutes, so it is a safety net, not a gate — verify locally first.

## Looking at the UI without a device

There is no emulator here and there cannot be: the container has no `/dev/kvm`. Instead
Robolectric renders real Compose to PNG on the JVM in ~20 seconds, and those images can be
opened directly. `ScreenshotRenderer` draws the decor view straight to a bitmap; the
Compose test rule's idling machinery times out under Robolectric, so do not reach for
`captureToImage()`.

This matters more than it sounds. Three visual bugs were shipped to the owner's phone
while guessing blind, and the first render found the real cause of all of them.

## Architecture

```
core/      model + time (java.time; minSdk 26 so no desugaring)
data/      Room, DataStore, repositories, species catalogue asset, CSV/JSON transfer
domain/    session state machine, milestones, statistics, Grove layout — NO Android imports
service/   foreground service, distraction detection
ui/        Compose: radial-hub home, session flow, grove, stats, tags, species, settings
```

- `SessionController` is the heart: a state machine over a `SessionStore` interface and an
  injected clock, so it is fully unit-testable. Keep it that way.
- `SessionOverlay` is the floating bubble, and it is plain Android views on purpose: a
  `ComposeView` in a `WindowManager` window needs a lifecycle owner, a saved-state registry
  and a view-model store supplied by hand before it draws anything, and the screenshot
  renderer cannot reach it either way. The service drives it from the same `appInForeground`
  signal the distraction detector uses.
- The Grove is a *rendering*, not a table: `GroveLayoutBuilder` turns the session list into
  a bed of plants deterministically, seeded by session id, so it never reshuffles between
  launches. One successful session is one **node** (stem segment + opposite leaf pair) on
  that species' youngest unfinished plant; at twelve nodes a plant is mature and the next
  session starts a new one beside it. `PlantBuilder` owns that structure and is pure.
- Unlocks have **two** sources of truth, and the split is the thing to keep hold of:
  *how many* mints are unlocked is derived from one `SUM` over successful sessions and
  never stored; *which* ones is the player's choice at each milestone, and that is the one
  fact the `species` table holds. Never auto-stamp a species — `MintalApplication` and the
  backup restore used to, and must not again.

## Hard constraints

- **No `INTERNET` permission.** It is a stated, verifiable privacy claim. Anything needing
  the network goes in a separate build flavour, or does not happen.
- **Nothing downloaded at runtime** — no remote fonts, no remote config. F-Droid builds
  offline from source and it would break.
- No Google Play Services, no vendor SDKs, no analytics, no crash reporting, no IAP.
- Every dependency OSI-licensed, resolvable from Maven Central or Google's Maven.
- Permissions are a design constraint, not an implementation detail: five unavoidable ones,
  three optional special accesses (Usage Access, display over other apps, ignoring battery
  optimisation). Adding one needs a conversation — the overlay had one, in the third round
  of device feedback, and is off by default like the others.

## Gotchas already paid for

- `sin(PI)` in `Float` is `-8.7e-8`. Raising it to a fractional power gives NaN, and one
  NaN point silently collapses a whole `Path` — every mint drew as a bare stem for days.
  `LeafGeometryTest` guards this.
- Offset a blade along the **unit** perpendicular, never the raw axis vector.
- `SessionController.finish()` must set the result **before** clearing the running session.
  Clearing first leaves a window where the UI sees neither and skips the result screen.
- Point the debug and release KSP tasks at **different** Room schema directories (the Room
  Gradle plugin does this) or they race and read each other's half-written files.
- Light mode needs real separation between page, card and edge; a 4% tonal difference
  renders as no boxes at all.
- Rows of chips/buttons must be `FlowRow`, or they run off the edge in a multi-window pane.
- Dates are forced to `Locale.ENGLISH` while the UI copy is English-only. Revisit when
  strings are translated.
- Material 3's `Surface` propagates min constraints to its child, so `Modifier.height(x)`
  on that child does nothing. A screenshot test that renders a band inside a `Surface`
  silently gets a full-screen band and flatters the layout; wrap it in a `Column`.
- Mint is **decussate**: each leaf pair is turned ~90° from the one below, so every second
  pair projects edge-on. Drawing them all flat is what made the plants read as a generic
  herb. Branches come out of leaf axils, never mid-internode.
- Leaf size is relative to the plant's own internode (`Plant.segmentUnit`), not to the
  canvas unit. Scaling leaves off the canvas makes Corsican mint — whose whole point is
  being tiny — the same size as spearmint.
- Chart axis labels are laid out by measuring them, not by dividing the width into equal
  slots: twenty-four equal slivers clip "03" to "0", which is how an hour axis came to
  read "0 0 0 0 1 1 1 2" on a real phone.
- The app draws edge to edge, and Material's `Scaffold` pads its own `BottomAppBar` for
  the system bars but **not** a hand-written `bottomBar`. One has to apply
  `windowInsetsPadding(WindowInsets.safeDrawing.only(Horizontal + Bottom))` itself, or
  on a phone with on-screen back/home/recents the button sits under them and cannot be
  pressed. Screens whose content applies the Scaffold's own padding are already fine.
  `ScreenshotRenderer.render(..., navigationBarPx = 132)` fakes those buttons, because a
  render on a gesture-navigation device will never show the bug.
- A screenshot test that rebuilds a component instead of calling it verifies the rebuild.
  The first-run footer was duplicated in `PrinciplesScreenshotTest`, so a render "proving"
  the inset fix was a render of a copy that never had it. Make the real composable
  `internal` and call it.
- A tappable box hand-rolled beside `SectionCard` comes out with its own padding and no
  border, and 2dp of difference is invisible in review and obvious on a phone — that is how
  the hub's Mint and Tag boxes ended up visibly tighter than their neighbours. `SectionCard`
  takes an `onClick`; use it rather than a bare `Card`. `SectionCardScreenshotTest` stacks
  both kinds so the next one cannot drift.

## Conventions

- Kotlin official style, four spaces, trailing commas.
- Comments explain *why*, never *what*. No comment is better than a restated line.
- `domain/` stays free of Android framework imports.
- UI strings are currently hardcoded in Compose. Moving them to `strings.xml` is the
  prerequisite for the Spanish translation the owner wants. The exceptions are the few the
  framework needs as resources: the notification channel and the overlay bubble.
- F-Droid submission material lives in `fastlane/metadata/` (what the store page shows) and
  `fdroid/dev.nheox.mintal.yml` (the build recipe, destined for a merge request against
  fdroiddata). `docs/FDROID.md` is the checklist and says what is still blocked on the
  owner.

## Current state and next steps

See `docs/STATUS.md`.

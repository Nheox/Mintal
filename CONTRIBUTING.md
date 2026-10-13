# Contributing to Mintal

Honest about the stage: Mintal is early, single-maintainer, and the architecture is still
settling. That makes some contributions much easier to accept than others.

## Easy to accept

- Bug reports with a device model, Android version and what you did.
- Botanical corrections to `app/src/main/assets/species.json`.
- Accessibility fixes: TalkBack labels, contrast, touch targets.
- Translations, once the UI strings have been moved into `strings.xml` (see below).
- Tests for anything in `core/`, `domain/` or `data/`.

## Please open an issue first

- New features. The specification is deliberately small; v1 scope is closed.
- Anything that adds a dependency. Every dependency must be OSI-licensed and must
  resolve from Maven Central or Google's Maven, because F-Droid builds this app with no
  network access.
- Anything that adds a permission. The permission list is a design constraint, not an
  implementation detail.

## Hard no

- Analytics, crash reporting, advertising, in-app purchases, or any network call in the
  default build. The app has no `INTERNET` permission and will keep it that way.
- Code, assets or copy taken from another app. See `docs/PRIOR_ART.md`.

## Known gaps, if you want something to do

1. **UI strings are hardcoded in Compose** rather than in `strings.xml`. Moving them is
   mechanical, unlocks translation, and is genuinely useful.
2. **No instrumented tests yet.** The session state machine, statistics, Grove layout,
   milestones, CSV export and time bucketing are unit-tested; the foreground service, the
   database and the Compose screens are not covered at all.
3. **Procedural artwork.** The mints are drawn from botanical parameters. An illustrator's
   set would replace `ui/components/MintArt.kt`.

## Looking at the UI without a device

Compose screens can be rendered to PNG on the JVM — no emulator, no phone:

```sh
./gradlew testDebugUnitTest --tests "*ScreenshotTest*"
# PNGs land in app/build/screenshots/
```

They are rendered by Robolectric drawing the view straight to a bitmap. Use them for
layout, contrast and shape problems; they cannot tell you anything about the foreground
service, the notification, or how the app feels in the hand.

`LeafGeometryTest` is the companion to that: it asserts every species' leaf has actual
area in every direction. It exists because a NaN from `sin(PI)` once collapsed every leaf
path silently, and the app shipped drawing bare stems.

## House rules

- Kotlin official code style, four-space indent, trailing commas.
- Keep `domain/` free of Android framework imports so it stays unit-testable.
- Comments explain *why*, not *what*.
- `./gradlew test assembleDebug` must pass before you open a pull request.

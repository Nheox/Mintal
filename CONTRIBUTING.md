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
2. **No instrumented tests yet.** The session engine is unit-testable; the foreground
   service and the Compose screens are not covered.
3. **Procedural artwork.** The mints are drawn from botanical parameters. An illustrator's
   set would replace `ui/components/MintArt.kt`.

## House rules

- Kotlin official code style, four-space indent, trailing commas.
- Keep `domain/` free of Android framework imports so it stays unit-testable.
- Comments explain *why*, not *what*.
- `./gradlew test assembleDebug` must pass before you open a pull request.

# Getting Mintal onto F-Droid

F-Droid does not take an APK. It takes a short **recipe** telling its own build servers
how to compile Mintal from source at a tagged commit, then publishes the result. The
recipe lives in F-Droid's repository on GitLab, not here, so submitting is something done
there, from a GitLab account (spec §10.2).

Mintal's build is **reproducible**, so the recipe asks F-Droid to publish the APK under
the author's own signature rather than F-Droid's. That makes the F-Droid install and the
GitHub release the same app to Android, and either can update the other.

## What F-Droid needs from this repository

| Requirement | State |
|---|---|
| A free licence | GPL-3.0, verbatim in `LICENSE` |
| No proprietary dependencies, no Play Services | AndroidX, Dagger/Hilt and kotlinx only — `docs/CREDITS.md` |
| A build that works with no network access | Only `mavenCentral()` and `google()` are declared; nothing is fetched at runtime |
| No encrypted dependency metadata in the APK | `dependenciesInfo` is switched off in `app/build.gradle.kts` |
| `assembleRelease` working with no signing key | Yes — it produces an unsigned APK, which F-Droid builds and compares |
| A tagged release to point at | **`v0.2.0`**, commit `e8e40bf33974bd80ab90cd4b4b6b60325f30458d`, published with its signed APK |
| A reproducible build | Verified for `v0.2.0` — see below |
| Listing text | `fastlane/metadata/android/en-US/` — title, short and full description |
| "What's new" | `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` |
| An icon | `images/icon.png`, 512×512, rendered from the launcher icon (below). Needed because the launcher icon is adaptive XML only, which F-Droid cannot render |
| Screenshots | **Not yet.** See below |

There are no anti-features to declare: no network access, no tracking, no ads, no
non-free dependencies.

## Screenshots and icon

Take the screenshots on a phone — hub, a running session, the Grove, statistics, a
species card — and add them as

```
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
fastlane/metadata/android/en-US/images/phoneScreenshots/2.png
…
```

The icon, `fastlane/metadata/android/en-US/images/icon.png`, is not drawn by hand.
`LauncherIconScreenshotTest` renders it from `R.mipmap.ic_launcher`, framed on the leaf
and masked to a circle; after any change to the launcher icon, run
`./gradlew testDebugUnitTest --tests "*LauncherIconScreenshotTest*"` and copy
`app/build/screenshots/store-icon.png` over it. Without it the listing would show a
generic placeholder, because the APK holds no PNG launcher icon to fall back on (minSdk
26 lets AGP ship the adaptive XML alone).

F-Droid picks all of these up from this repository at the tagged commit, so they arrive
with the next release after they are committed. The listing works without screenshots;
it is only plainer.

## Submitting: two ways

**The easy way — ask F-Droid to package it.** Open an issue at
<https://gitlab.com/fdroid/rfp/-/issues> ("Request For Packaging") with the name, the
source link <https://github.com/Nheox/Mintal>, the licence (GPL-3.0-only) and a sentence
about what the app does. A volunteer writes the recipe. No git needed; the queue can take
weeks.

**The direct way — submit the recipe yourself.** Fork
<https://gitlab.com/fdroid/fdroiddata>, add the file below as
`metadata/dev.nheox.mintal.yml`, and open a merge request. GitLab's CI builds the app from
the recipe, and a reviewer takes it from there. Faster, but it is a git workflow.

```yaml
Categories:
  - Time Tracker
  - Timer
License: GPL-3.0-only
AuthorName: Nheox
SourceCode: https://github.com/Nheox/Mintal
IssueTracker: https://github.com/Nheox/Mintal/issues
Changelog: https://github.com/Nheox/Mintal/releases

AutoName: Mintal

RepoType: git
Repo: https://github.com/Nheox/Mintal.git
Binaries: https://github.com/Nheox/Mintal/releases/download/v%v/mintal-v%v.apk

Builds:
  - versionName: 0.2.0
    versionCode: 2
    commit: e8e40bf33974bd80ab90cd4b4b6b60325f30458d
    subdir: app
    gradle:
      - yes

AllowedAPKSigningKeys: 8c2f78132ceb70b7728b120f01267dd3a174999e7109a22c60ffad3df10d14c2

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.2.0
CurrentVersionCode: 2
```

Notes on the recipe:

- **`commit` is the full hash, never the tag.** F-Droid's checklist requires it, so a tag
  moved later cannot change what was built.
- **`Binaries` and `AllowedAPKSigningKeys` are what make it reproducible.** F-Droid
  builds the APK itself, downloads the one at `Binaries` (`%v` is `versionName`), copies
  its signature onto its own build, and publishes only if that verifies. The key is the
  SHA-256 of the release certificate, as `apksigner verify --print-certs` prints it in
  the Release workflow's log. The URL depends on the asset name the workflow gives the
  APK, `mintal-v<version>.apk`; renaming it breaks every later F-Droid update.
- **`Time` is no longer a category.** F-Droid replaced its category list;
  `Timer` and `Time Tracker` are from the current one (`config/categories.yml` in
  `fdroiddata`, checked 17 Oct 2026).
- **The merge request's first CI job is `fdroid rewritemeta`**, and it fails on
  formatting alone. The first submission failed on two things: categories must be in
  sorted order (`Time Tracker` before `Timer`, because a space sorts before a letter), and
  the file must end with a newline.
- **`GPL-3.0-only`** because that is what the repository says: `GPL-3.0` with no "or any
  later version". If the owner would rather allow later versions of the GPL, the README
  and a licence notice have to say so first, and then this becomes `GPL-3.0-or-later`.
- **Do not add `test` to the build steps.** Robolectric downloads artifacts the first time
  it runs, which breaks a build with no network. CI runs the tests; F-Droid only needs
  `assembleRelease`.
- Reviewers often adjust `Categories`; that is normal.

## After it is accepted

Every later release is the same as now: bump `versionCode` and `versionName`, write
`changelogs/<versionCode>.txt`, commit, push a `v*` tag. With `UpdateCheckMode: Tags`
F-Droid notices the tag on its own, usually within a few days. Nothing has to be submitted
again.

## Reproducibility

Checked on 17 Oct 2026: `v0.2.0` was rebuilt from a fresh clone with `assembleRelease`,
no signing key, no Gradle build cache, OpenJDK 21 (the Release workflow uses Temurin 17)
and a different checkout path. `apksigcopier compare` against the GitHub release APK
passed, and the rebuilt APK with the release signature copied onto it is byte-identical to
the published file. That is the check F-Droid's server runs.

What could break it:

- **A checkout without a readable `.git`.** AGP writes the commit hash into
  `META-INF/version-control-info.textproto`. A git *worktree* or a source tarball makes
  that file say `NO_VALID_GIT_FOUND` instead, and it is the only thing that differed in
  a first attempt built from a worktree. F-Droid and `actions/checkout` both clone, so
  both write the hash.
- **Anything that varies between machines**: a timestamp, a build-time random value,
  a dependency resolved to a different version. None exists today. A new Gradle plugin
  or code generator is the likeliest place one would come from.

If a release is not reproducible, F-Droid does not quietly fall back to its own key: that
version simply fails to build there until it is fixed. Once F-Droid publishes the author's
signature, the README and the comment at the top of `release.yml`, which both say F-Droid
signs with its own key, need correcting.

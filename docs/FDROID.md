# Getting Mintal onto F-Droid

F-Droid does not take an APK. It takes a short **recipe** telling its own build servers
how to compile Mintal from source at a tagged commit, then signs the result with its own
key and publishes it. The recipe lives in F-Droid's repository on GitLab, not here, so
submitting is something done there, from a GitLab account (spec §10.2).

## What F-Droid needs from this repository

| Requirement | State |
|---|---|
| A free licence | GPL-3.0, verbatim in `LICENSE` |
| No proprietary dependencies, no Play Services | AndroidX, Dagger/Hilt and kotlinx only — `docs/CREDITS.md` |
| A build that works with no network access | Only `mavenCentral()` and `google()` are declared; nothing is fetched at runtime |
| No encrypted dependency metadata in the APK | `dependenciesInfo` is switched off in `app/build.gradle.kts` |
| `assembleRelease` working with no signing key | Yes — it produces an unsigned APK, which F-Droid then signs |
| A tagged release to point at | **`v0.2.0`** |
| Listing text | `fastlane/metadata/android/en-US/` — title, short and full description |
| "What's new" | `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` |
| Screenshots | **Not yet.** See below |

There are no anti-features to declare: no network access, no tracking, no ads, no
non-free dependencies.

## Screenshots

Take them on a phone — hub, a running session, the Grove, statistics, a species card —
and add them as

```
fastlane/metadata/android/en-US/images/phoneScreenshots/1.png
fastlane/metadata/android/en-US/images/phoneScreenshots/2.png
…
```

F-Droid picks them up from this repository at the tagged commit, so they arrive with the
next release after they are committed. The listing works without them; it is only
plainer.

## Submitting: two ways

Both are done from the owner's GitLab account, **`mintal`**. The group `mintal-group` is
not needed for this, and the code stays on GitHub: the recipe points F-Droid there.

**Submit only once the GitHub release is a full release, not a pre-release.** F-Droid
watches tags and ignores GitHub's pre-release label, so it would publish a version the
owner had not yet called finished.

**The easy way — ask F-Droid to package it.** Open an issue at
<https://gitlab.com/fdroid/rfp/-/issues> ("Request For Packaging"). A volunteer writes the
recipe. No git needed; the queue can take weeks or months. Text to paste, adjusted to
whatever the issue template asks:

> **Mintal** — a focus timer that grows real mint plants.
>
> - Source code: <https://github.com/Nheox/Mintal>
> - Licence: GPL-3.0-only
> - Suggested category: Time
> - Build: `gradle assembleRelease` in `app/`; tagged releases (`v0.2.0` onward);
>   listing text and changelogs in `fastlane/metadata/android/en-US/`
> - No `INTERNET` permission, no proprietary dependencies, no Play Services, no tracking
> - I am the author, submitting it myself.

**The direct way — submit the recipe yourself**, which is faster and can be done entirely
in the browser:

1. Signed in as `mintal`, open <https://gitlab.com/fdroid/fdroiddata> and press **Fork**
   (into the personal namespace).
2. In the fork, open the `metadata` folder, choose **+ → New file**, name it
   `dev.nheox.mintal.yml`, and paste the recipe below.
3. **Commit changes** to a *new branch*, with "Start a new merge request" ticked.
4. Make the merge request target `fdroid/fdroiddata`, fill in its checklist, and submit.
   GitLab's CI builds the app from the recipe; a reviewer takes it from there and may ask
   for changes in the merge request's comments.

```yaml
Categories:
  - Time
License: GPL-3.0-only
AuthorName: Nheox
SourceCode: https://github.com/Nheox/Mintal
IssueTracker: https://github.com/Nheox/Mintal/issues
Changelog: https://github.com/Nheox/Mintal/releases

AutoName: Mintal

RepoType: git
Repo: https://github.com/Nheox/Mintal.git

Builds:
  - versionName: 0.2.0
    versionCode: 2
    commit: v0.2.0
    subdir: app
    gradle:
      - yes

AutoUpdateMode: Version
UpdateCheckMode: Tags
CurrentVersion: 0.2.0
CurrentVersionCode: 2
```

Notes on the recipe:

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

The F-Droid build is signed with **F-Droid's** key, so to Android it is a different app
from the GitHub release: one cannot update the other, and moving between them means a
backup, an uninstall and a restore. F-Droid can publish the developer's own signature
instead if the build is reproducible bit for bit — worth looking at once the app is
settled, not before.

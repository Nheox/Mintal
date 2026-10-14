# Getting Mintal into F-Droid

Everything in this file that is already done is done. What is left is listed first,
because all of it needs the owner, not the code.

## What is still needed from the owner

### 1. The repository has to be public

F-Droid's build server clones the source and builds it on their machine. `Nheox/Mintal`
is currently **private**, so nothing else on this list matters until it is public. This
is the one hard blocker.

### 2. A donation address — deliberately not yet

`Donation.URL` is an empty string, and the Support card in Settings renders with its
button disabled and a line saying no address is set in this build. That is the intended
state for the first release: an app with no users earns no donations, and registering a
payment identity in your own name before anyone has installed it is a commitment with
nothing on the other side of it.

When that changes it is one line, in
`app/src/main/kotlin/dev/nheox/mintal/ui/settings/Donation.kt`, plus the matching line in
`fdroid/dev.nheox.mintal.yml`. What the string looks like depends on the platform:

| Platform | What to put in `URL` | F-Droid metadata field |
|---|---|---|
| PayPal.me | `https://www.paypal.com/paypalme/<handle>` | `Donate:` |
| Ko-fi | `https://ko-fi.com/<handle>` | `Donate:` |
| Liberapay | `https://liberapay.com/<handle>` | `Liberapay: <handle>` |
| GitHub Sponsors | `https://github.com/sponsors/<handle>` | `Donate:` |
| Buy Me a Coffee | `https://buymeacoffee.com/<handle>` | `Donate:` |

Two things worth knowing before picking one. The platform has to be able to **pay out to
a Chilean account** — that is the part that rules options in or out, not the link format,
and it is worth confirming on the platform's own site before creating the account.
Liberapay is the one the F-Droid audience is most used to and takes no cut; PayPal.me is
the one most people already have an account for.

None of this changes the no-`INTERNET`-permission claim. The button hands the URL to
whichever browser is already installed; Mintal never opens a socket.

### 3. Identity for the recipe

`fdroid/dev.nheox.mintal.yml` is valid as it stands; two optional fields are commented
out in it and worth a decision before the merge request:

- `AuthorName` — the name that should appear as the app's author.
- `AuthorEmail` — an address to put in public metadata. A forwarding alias is fine, and
  probably wiser than a personal address.

Both are public and permanent in a way a commit is not: they end up on the app's F-Droid
page, in their index, and in every mirror of it.

### 4. Confirm the licence wording

`LICENSE` is the plain GPL-3.0 text and the README says "GPL-3.0" with no "or any later
version" notice, so the recipe declares `GPL-3.0-only`. If "or later" was meant, say so
and both the README and the recipe change to `GPL-3.0-or-later`. It matters: it is the
difference between code that can and cannot be reused under GPL-4, if there ever is one.

### 5. Screenshots, from a real phone

F-Droid shows up to eight. They go in
`fastlane/metadata/android/en-US/images/phoneScreenshots/`, named `1.png`, `2.png` and so
on, PNG or JPG, no particular size. The renders the screenshot tests produce are for
checking layout, not for a store page — a store page wants the real thing, with the
status bar, on a real device.

Worth capturing, in this order: the hub with a session set up, a session running, the
Grove with a few mature plants, a mint's card in the species view, the statistics page.
A Grove with one seedling in it sells nothing, so it is worth doing after a week of real
use.

### 6. A tagged release

F-Droid builds a **tag**, not a branch. When the app is ready:

```sh
git checkout main
git merge <the working branch>
git tag -a v0.1.0 -m "Mintal 0.1.0"
git push origin main --tags
```

The tag name, `versionName` and `versionCode` in `app/build.gradle.kts`, and the
`Builds:` entry in the recipe all have to agree.

## What is already prepared

- **`fastlane/metadata/android/en-US/`** — title, short description (within F-Droid's
  80-character limit), full description and the changelog for version code 1. F-Droid
  reads these straight out of the repository at build time; there is no separate form to
  fill in.
- **`fdroid/dev.nheox.mintal.yml`** — the build recipe, ready to be copied into a merge
  request against [fdroiddata](https://gitlab.com/fdroid/fdroiddata) as
  `metadata/dev.nheox.mintal.yml`.
- **Release signing** — `app/build.gradle.kts` reads a keystore from a gitignored
  `keystore.properties` at the repository root, or from `MINTAL_KEYSTORE_FILE`,
  `MINTAL_KEYSTORE_PASSWORD`, `MINTAL_KEY_ALIAS` and `MINTAL_KEY_PASSWORD`. With neither
  present, `assembleRelease` still succeeds and leaves the APK unsigned, so CI and a
  fresh clone keep working.

  This signing is only for APKs published from here — a GitHub release, a build handed
  to a tester. **F-Droid does not use it.** F-Droid builds from source and signs with its
  own key, so an F-Droid install and a sideloaded release build are, to Android, two
  different apps that cannot upgrade each other.

- **The things F-Droid checks that Mintal already passes**: an OSI licence, no
  proprietary dependencies, no Google Play Services, nothing downloaded at build or run
  time, no analytics or crash reporting, a committed Gradle wrapper, and a manifest with
  no `INTERNET` permission. There is no anti-feature to declare.

## The submission itself

1. Make the repository public, tag a release, confirm CI is green on the tag.
2. Fork <https://gitlab.com/fdroid/fdroiddata>.
3. Copy `fdroid/dev.nheox.mintal.yml` to `metadata/dev.nheox.mintal.yml` in the fork,
   with the `TODO`s filled in.
4. Open a merge request. Their CI lints the recipe and tries the build; expect review
   comments and a wait measured in weeks, not days.
5. After it merges, every later release is picked up automatically from a new tag —
   `UpdateCheckMode: Tags` — as long as `versionCode` goes up.

Two things to decide before step 1, because they are awkward to change afterwards:

- **The application ID is permanent.** `dev.nheox.mintal` is the app's identity in
  F-Droid forever; it cannot be renamed without publishing a second, unrelated app.
- **Spanish.** F-Droid will show translated metadata from
  `fastlane/metadata/android/es-ES/` the moment it exists, but the app's own UI strings
  are still hardcoded in Compose and cannot be translated yet. Submitting first and
  translating later is fine and costs nothing.

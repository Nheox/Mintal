# Mintal

An open focus timer built around real mint plants, for Android.

Choose how long you want to concentrate. While the session runs a single chunky progress
bar fills and nothing else asks for your attention. Finish the block without leaving the
app and the session is planted in **the Grove**: a bed of mints that is the record of your
attention, not a reward animation bolted on top of it.

One session is one node — a stem segment and the pair of leaves on top of it — so a plant
grows the way mint actually grows, pair by opposite pair, branching from its leaf axils.
When a plant is full grown it stops, and your next session of that species starts another
beside it.

Every plant is a real member of the genus *Mentha* — spearmint, peppermint, water mint,
Corsican mint — drawn from a photo reference, leaf shape, margin, hairs and all. Cumulative
focused time earns new species, and which one you unlock at each milestone is your choice:
twelve to pick from at the first, one at the last.

## Principles

- **Local-first.** Everything lives in a database on your device and works in airplane
  mode. No account, no server of ours.
- **No surveillance.** No analytics, no crash reporting, no advertising IDs, no network
  calls of any kind.
- **No paywall.** Every feature is available to everyone. Nothing is for sale, ever.
- **Minimum permissions.** The app asks for the least it can and still work, explains
  anything optional before requesting it, and stays fully functional when refused.
- **Honest blocking.** Leaving mid-session gets a visible 15-second warning before the
  session is failed. Mintal never seizes control of your device.

### No INTERNET permission

Mintal's manifest contains no `INTERNET` permission, so the app cannot send your data
anywhere even by accident. You can verify that in
[`app/src/main/AndroidManifest.xml`](app/src/main/AndroidManifest.xml).

A later, opt-in cloud *backup* is planned, and it does not change this: it writes the same
JSON backup file to a folder **you** pick with Android's own document picker (Drive,
Nextcloud, a local folder — whatever is already installed). Mintal holds no credentials,
talks to no sync API, and runs no backend. One-way backup to storage you already own is
not two-way sync, which stays out of scope.

### The permissions it does ask for

Five install-time permissions, all of them implied by "a timer that keeps running": two
for the foreground service, a wake lock, a notification and vibration.

Three further things are **special access**, off by default, each explained in Mintal's
own words before the system is ever asked, and none of them a precondition for a session:

- **Usage Access**, so an allowlisted app — a dictionary, a calculator — can be told from
  a distraction instead of failing your session. Mintal reads only which app is in front.
- **Display over other apps**, for the optional floating bubble that carries the time left
  and the 15-second warning over whatever you are using. The bubble shows the clock; it
  cannot read what is underneath it.
- **Ignoring battery optimisation**, requested by you, from Settings, on phones that stop
  background timers regardless of the notification.

Refuse all three and every other part of the app behaves exactly as before.

## State of the project

Early. The v1 feature set is implemented — countdown and count-up sessions, the Grove,
tags, ratings and notes, statistics, the species catalogue with milestone unlocks,
distraction detection with an allowlist, CSV export and full JSON backup/restore — and the
app builds and runs. Two things are deliberately placeholders:

- **Artwork.** The twelve mints and the Grove are drawn procedurally from real botanical
  traits (leaf shape, leaf margin, growth habit, relative size) rather than from
  commissioned illustrations, which do not exist yet. See `docs/CREDITS.md`.
- **Typography.** Both faces are system families. A bundled typeface will replace them;
  nothing is ever downloaded at runtime, because that would break a reproducible build.

## Building

Requirements: JDK 17 or newer and the Android SDK (platform 36). Nothing else — no
Google Play Services, no vendor SDKs.

```sh
git clone https://github.com/Nheox/mintal.git
cd mintal
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # unsigned release APK
./gradlew test                # unit tests
```

If Gradle cannot find the SDK, point it at one:

```sh
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

CI builds both APKs and runs the tests on every push; the debug APK is attached to each
run as a downloadable artifact.

## Distribution

F-Droid builds its own APK from this source — it does not accept a pre-built one — so the
only thing this repository needs to guarantee is a clean, reproducible
`assembleRelease`. There are no vendor SDKs, no artifacts fetched outside Maven Central
and Google's Maven, and nothing downloaded at runtime. The F-Droid recipe itself lives in
F-Droid's own `fdroiddata` repository and is submitted once there is a stable tagged
release.

## Layout

```
app/src/main/kotlin/dev/nheox/mintal/
├── core/        model, time
├── data/        room, datastore, repositories, species catalogue, export/backup
├── domain/      session engine, milestones, statistics, Grove layout
├── service/     foreground service, distraction detection
└── ui/          radial-hub home, session flow, grove, stats, tags, species, settings
```

## How this was built

Mintal was written with Claude, Anthropic's coding assistant, working from the product
specification in this repository. The specification, the product decisions, the testing on
real hardware and the judgement about what was wrong came from the author; Claude wrote
the code under that direction.

The history records it rather than just claiming it: every commit carries a
`Co-Authored-By: Claude` trailer, so you can see exactly which changes were written that
way — `git log` will tell you, not this paragraph.

It is written down because the app asks you to believe things about itself: that it holds
no network permission, that nothing is sent anywhere, that no code or art was taken from
another app. Those claims are checkable in the source, and how the source came to exist
belongs to the same accounting.

## Licence

GPL-3.0. See [LICENSE](LICENSE).

## Prior art

A focus timer with a visual reward loop is a well-established category. Mintal is an
independent implementation written from a behavioural specification; no code, assets, text
or branding from any existing app is used. See [docs/PRIOR_ART.md](docs/PRIOR_ART.md).

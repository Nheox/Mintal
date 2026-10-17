# Mintal — Open Focus Timer
## Product specification v0.4

**Status:** draft · **Date:** 2026-10-17 · **Platform:** Android (Kotlin / Jetpack Compose)

**Changelog from v0.3:** v1 is implemented, and three rounds of use on a real device
changed nine behaviours, each of which is folded into the section it belongs to rather
than kept in a separate amendments file. In brief: START now arms a five-second
**planting window** (§5.1.1) so a mistyped duration costs nothing; the Grove is **a bed
of discrete mints** grown a node at a time, not one continuous runner network (§5.6);
**failed sessions are no longer drawn** in the Grove by default (§5.4, §5.6); a milestone
now earns a **slot the player spends** on whichever mint they want, not a fixed next
species (§6.1); every permission is **explained in Mintal's own words before** the system
asks (§9); first launch is **four slides** rather than one page (§4.1); the app has a
slogan and an animated tutorial (§4.2); the principles are read once and then live in
Settings, with no launch gate (§4.3); chart axes are **measured and stepped** rather than
"peak, half, zero" (§5.7); the illustration brief is replaced by a **procedural**
trait-driven one (§8.3); and §8's stack table is corrected to what was actually built.
See §13.1 for the reasoning behind each.

**Changelog from v0.2:** renamed app; principal palette set (light green + light blue);
cloud backup added as an opt-in, server-less v2 feature; session-setup dial and
in-session radial arc replaced (too close to *Forest*); Garden re-imagined as a single
growing mint network ("the Grove") instead of a multi-plant plot; rating gets a line
chart; species catalogue gets a v2 "Herbarium" collectible-card presentation; Home's
visual language moves toward a PS Vita–style radial hub; several v2 decisions
resolved; added §10.2 explaining how F-Droid actually builds and distributes the app.
See §13 for the full list of what changed and why.

---

## 1. What Mintal is

A focus timer built around real mint plants. You choose how long you want to
concentrate; while the session runs, a progress indicator moves toward completion —
nothing else to watch, nothing to feel guilty about mid-session. If you stay off the
phone for the whole block, the session is written into **the Grove**: a single,
ever-growing mint network that is the visual record of your attention, not a reward
animation bolted on top of it.

Every plant belongs to the genus ***Mentha***. New species unlock at cumulative
focused-time milestones, and each one is a real mint — spearmint, peppermint, water
mint, Corsican mint — with a one-line botanical note. The constraint is the product's
identity, not decoration borrowed from elsewhere.

Mintal is **local-first and offline by default**: no account, no Mintal-run server, no
analytics, no ads, no in-app purchases, nothing for sale at any point. Everything
lives in a database on the device, and the user can export, back up, or delete it at
will. v2 adds an *optional* backup to the user's own cloud storage (§3.1, §6.2) —
Mintal still runs nothing of its own on a server; see §13 for why this doesn't break
principle 1.

### 1.1 Relationship to prior art

The concept category (a focus timer with a visual reward loop) is well-established,
*Forest* chief among them. Mintal is an independent implementation written from a
behavioural specification — this document — and this revision deliberately moves two
of its most recognisable visual beats (the radial countdown dial, the single-plant
reward) away from that app's specific presentation; see §5.1, §5.2, §5.6. No source
code, assets, text, layout, or branding from any existing app is used. All UI copy,
illustrations, plant designs, and names are original to Mintal.

Working material used to compile this spec lives in `~/mintal/reference/` and is
**gitignored**; it never enters the build. This is backed by an honest, public
statement to the same effect in the repository itself — see §10.1.

**This document, however, is in the repository**, at `docs/SPEC.md`. It is the
clean-room behavioural specification — the output of that analysis, not the analysis —
and publishing it is what makes the independent-implementation claim in
`docs/PRIOR_ART.md` checkable by anyone who cares to. The `reference/` material it was
compiled from stays out, as §10.1 requires.

---

## 2. Design principles

1. **Local-first.** Device-only data by default. Everything works in airplane mode.
   A v2 cloud backup is opt-in and uses storage the user already owns — see §3.1.
2. **No surveillance.** No analytics, crash reporting, advertising IDs, or network
   calls of any kind in the default build.
3. **No paywall, nothing for sale.** Every feature is available to every user. No
   premium tier, no cosmetic purchase, no locked species behind a price.
4. **FOSS-clean.** Only dependencies with OSI-approved licences; no Google Play
   Services, so the app is F-Droid eligible. See §10.2 for what that eligibility
   actually requires and how F-Droid builds and ships the app.
5. **Minimum permissions.** Mintal asks for the least it can and still work. Anything
   beyond the minimum is opt-in, explained before it's requested, and the app is fully
   functional when it's refused.
6. **Honest blocking.** The app discourages distraction, with a clear warning before a
   session is failed; it does not seize control of the device, and the user can always
   exit.
7. **Gentle, not punitive.** Failure states are plain and non-shaming.

---

## 3. Scope

### 3.1 v1 — must have

| # | Feature |
|---|---|
| 1 | Countdown focus sessions, duration selectable up to 3 hours |
| 2 | Count-up (stopwatch) sessions, up to 3 hours |
| 3 | Mint species, unlocked by cumulative focus milestones |
| 4 | Tags (category labels) with colours |
| 5 | Optional per-session note |
| 6 | Session rating, 1–5 |
| 7 | Distraction detection — leaving the app starts a warned grace period, then fails the session |
| 8 | App allowlist — permitted apps don't fail the session |
| 9 | Persistent foreground notification with live countdown |
| 10 | **The Grove** — a single, continuously growing mint network recording every session |
| 11 | Day detail — list of a day's sessions |
| 12 | Statistics — totals, per-day/week/month charts, tag and rating breakdown |
| 13 | CSV export of all session data |
| 14 | Settings |
| 15 | Full local backup (export/import JSON) |

Ambient sound, session presets, cloud backup, and scheduled reminders are demoted to
v2 — they add real value but nothing in the core loop depends on them, and keeping v1
small keeps the first ship honest.

### 3.2 v2 — nice to have

- **Widgets, kept simple** — a single quick-view widget showing the Grove's current
  state and a one-line stats summary (today's focus time, current streak). No
  quick-start timer widget for now; adds complexity for little payoff.
- ~~Ambient sound player~~ — **cut.** Out of scope even for v2 (see §13 for the note
  on the musician question this raises).
- **Session presets** — species + tag + duration saved as a favourite, **with the
  last-used preset persisted** as the default pre-fill on Session setup, so a student
  who always does 25-minute tagged Study blocks never re-configures it.
- ~~Local reminders~~ — cut.
- **Breaks, with optional auto-start** (Pomodoro-style cycling).
- **Study guidance** — short, cited notes on spaced repetition, retrieval practice,
  and interleaving, aimed at academic/university students. Ships together with
  breaks (above) so the Pomodoro technique is explained only once the app can
  actually run it. Claims are cited; this is positioned as evidence-based, not
  motivational filler. The same pool already feeds the in-session legend line in v1
  (§5.2), and every citation in it has been checked against the published work — two
  lines said something their source did not and were rewritten.
- ~~Achievements / daily goals~~ — cut.
- **The Herbarium** — a collectible-card presentation of the species catalogue,
  Steam-trading-card style: each unlocked mint is a card (art + botanical note), with
  a shareable image-card export of a day's or a session's result. Replaces the plainer
  "species catalogue + share card" items from v0.2 with one coherent system. See §13.
- **Dark theme** — cheap because the token layer (§8.2) is built with semantic roles
  from day one; ships whenever the palette is ready, v1 or shortly after.
- ~~Notification blocking during sessions~~ — **cut, permanently.** Judged too
  invasive even as an opt-in; principle 5 wins here even against real utility.
- **Cloud backup (opt-in, no Mintal server)** — see §6.2. Writes the same JSON backup
  (§7.1) to a destination the user picks via Android's Storage Access Framework
  (Google Drive, Dropbox, Nextcloud, a folder they control — whatever app they already
  have installed that exposes a SAF provider). Mintal never talks to a sync API
  directly and runs no backend; it only writes a file to a location the OS hands it.
- **Accessibility-service-based active blocking** (see §11.6) — status downgraded from
  "deferred, revisit only if users ask" to "maybe": still the highest-friction,
  highest-review-risk permission in the app, so it stays a v2+ experiment behind a
  very clear opt-in, not a v1 or default-v2 feature.

### 3.3 Out of scope — and why

| Dropped | Reason |
|---|---|
| Accounts, login, proprietary cloud *sync* (two-way, server-mediated) | Would require a Mintal-run backend; conflicts with local-first. (One-way backup to the *user's own* storage is different — see §3.1, §6.2.) |
| Friends, leaderboards, profiles | Server-dependent |
| Shared/group sessions | Server-dependent (may return in v3 over local network) |
| Real tree planting | Requires a payment processor and a partner organisation |
| Referral programme | Server-dependent, and growth-hacking isn't the point |
| Premium tier, subscriptions, IAP | Principle 3 — nothing is for sale |
| Ads, analytics, crash reporting | Principle 2 |
| In-app support chat | GitHub issues + an email link instead |
| Virtual currency | **Resolved: no coins.** Species unlock at cumulative focused-time milestones (§6.1). |
| Notification blocking during sessions | **Resolved: cut.** Too invasive for the access it buys (§3.2). |

---

## 4. Screen map

```
Splash
└── Onboarding (first launch only — four slides, §4.1)
    └── Home  (PS Vita–style radial hub — see §8.2)
        ├── Session setup (default view, center of the hub)
        ├── Session running
        ├── Session result  (rating, optional note)
        ├── The Grove                              ← was "Garden"
        │   └── Day detail
        │       └── Session detail (edit note/tag/rating, delete)
        ├── Statistics
        │   ├── Summary (totals, best day, best hour, average rating)
        │   ├── Chart (day / week / month / year)
        │   ├── Rating trend (line chart — §5.5)
        │   ├── Tag breakdown
        │   └── Rating breakdown
        ├── Species catalogue (list or card view; unlocked / next milestone)
        │   └── Mint chooser  (spend an earned slot — §6.1)
        ├── Tag manager
        │   └── Tag editor (name, colour)
        └── Settings
            ├── Timer settings
            ├── Blocking & allowlist
            │   └── App picker
            ├── Appearance  (including whether failed sessions are drawn — §5.6)
            ├── Permissions  (each one explained, listed exactly once — §9)
            ├── Data (export CSV, backup, restore, clear, v2: cloud backup destination)
            ├── Mintal's principles  (§4.3)
            ├── Support Mintal  (§4.4)
            └── About
```

Structurally this is unchanged from v0.2 — one Activity, Compose destinations,
Session setup/running/result all still exist as distinct states of one flow (a live
timer genuinely needs its own screen; it isn't being cut, just re-skinned — see
§5.2). What's changing is **how Home is laid out and how it moves**: instead of a
bottom nav bar, Home reads as a radial hub in the spirit of the PS Vita's
LiveArea — Session setup sits at the center, and Garden/Grove, Species, Tags, and
Statistics sit as large circular tiles arranged around it, reachable with a flick in
any direction rather than a row of small icons. This is a navigation and motion
treatment, not a change to what screens exist. v2 adds Presets, The Herbarium's card
view, Cloud backup, and Breaks/Study guidance under their respective existing
sections, plus the quick-view widget.

### 4.1 First launch, as four slides

One idea per slide, swipeable, with a thin progress bar and a Next button:

1. **Mintal**, the slogan, and a flowering sprig of the catalogue's first mint — drawn
   with the app's own plant code, so the first thing seen is the thing itself rather
   than a logo of it.
2. **How it works** — the tutorial (§4.2).
3. **What Mintal promises** — the three principles.
4. **One permission** — the notification note, and the button that asks for it. Absent
   below Android 13, where the permission is granted at install time; the deck is three
   slides there.

The order is the order the questions arrive in: what is this, what does it do, what will
it do to me, what does it want. System back steps back a slide rather than leaving.

This replaces the single long first-run page of v0.3, which asked someone to read
everything Mintal is before it would do anything.

Anything anchored to the bottom of a slide pads for the navigation bar — see §8.2.

At the end of the deck the player chooses their **starting mint** (§6.1), because the
first milestone is at zero hours.

### 4.2 The slogan and the tutorial

Mintal's line is **"Stay focused, stay mintalized"**. It sits under the name on the first
slide and in About.

A **three-beat animated tutorial**, looping, each beat an instruction with a line of
explanation under it:

| Beat | Instruction | Detail | Drawn as |
|---|---|---|---|
| 1 | Set the timer | Choose how long you want to concentrate. | the growth track, bud marker sliding out |
| 2 | Keep off the phone | Leave the app while it runs and the session fails. | a phone tipping face-down, session bar still running |
| 3 | Plant a mint | Finish it and a real species of *Mentha* grows in your Grove. | a real mint gaining nodes |

Between them the three captions have one job: that someone who has never opened Mintal
understands it is **a timer**, that the point is to **leave the phone alone** while it
runs, and that doing so **grows a real species of mint**. "Focus time" was jargon for a
thing everyone already has a word for.

It is **drawn, not played**: the same Compose canvas and the same plant code the Grove
uses, so there is no video or GIF asset to ship, nothing is fetched at runtime, and the
tutorial cannot drift from what the app actually does. Where the device has animations
turned off system-wide, the three captions are listed as plain numbered text instead —
someone who turned animations off has asked not to be shown one.

### 4.3 Where the principles live

The three claims Mintal makes about itself are read once, on the third slide of the
first-run deck, and kept at Settings → *Mintal's principles* for any time after that.

They were briefly shown before the hub at **every** launch until dismissed, with a
"don't show this again" tick. In use that was an obstacle between the launcher and the
timer: a focus app that asks to be read before it will start one is arguing with its own
purpose. There is now no gate, no tick-box anywhere in the app, and no stored
dismissal state for them.

### 4.4 Settings: Support Mintal

A "Support Mintal" card sits at the bottom of Settings, directly above About. It states
that Mintal is free software with nothing for sale, and offers a donation link that opens
in the reader's browser via `ACTION_VIEW`.

**No permission is added for this and none is needed.** Mintal still declares no
`INTERNET` permission and still opens no socket; handing a URL to a browser is not
network access by the app. The address lives in one constant. While it is empty the card
still renders with the button disabled and a line saying no address is set, rather than
shipping a link that goes nowhere.

This does not touch principle 3. Nothing is for sale, nothing is withheld from anyone who
does not pay, and no feature anywhere in the app is behind the card.

---

## 5. Core flows

### 5.1 Start a session

1. User lands on **Session setup**, the center tile of the radial hub: the chosen
   duration, selected species, current tag, and a Start control.
2. **Duration picker — not a radial dial** (too close to *Forest*'s signature UI).
   Instead: a horizontal **growth track** — a stylised mint runner laid left to
   right, marked at 5-minute intervals, with small leaf glyphs the user drags a
   marker across (or taps a numeric field to set directly).
   - Range: 5 minutes to 3 hours, in 5-minute steps.
   - Switching to count-up mode replaces the track with a stopwatch face.
3. Tapping the species chip opens the **Species catalogue** (The Herbarium in v2).
   Locked mints are visible but greyed, each showing the milestone that unlocks it.
   The catalogue offers a list or a one-mint-per-page card view, and an unspent
   milestone slot can be spent from either (§6.1).
4. Tapping the tag chip opens the **Tag selector**: a searchable list of existing tags
   plus a "create tag" row. A session may have exactly one tag, or none.
   v2: if a preset is active, species/tag/duration pre-fill from it.
5. **Start** *arms* the session rather than beginning it — see §5.1.1.

#### 5.1.1 The planting window

Pressing START arms a session for **5 seconds**. The hub shows a countdown in place of
the START button and a card offering **Cancel**. During the window:

- no session row is written, no foreground service runs, no notification appears;
- changing the duration, the mode or the tag cancels the window outright;
- leaving the hub — to Settings, to the catalogue, or out of the app — cancels it;
- cancelling leaves **nothing** behind: no record, no plant, no wilted stub.

When the window elapses the session starts exactly as before. The rationale is that the
cost of a mistyped duration or a wrong tag used to be a dead plant in the Grove, and a
timer app should not punish a slip of the thumb.

This is a *different mechanism* from the distraction grace period of §5.3, which keeps
its own name and its 15 seconds. The two never overlap: the planting window ends before
a session exists, the grace period only applies once one does.

Because nothing is written during the window, it does not need to survive process death,
and it is deliberately scoped to the hub.

### 5.2 During a session

- **Not a radial arc** (§13) — instead, a horizontal **progress bar in the spirit of
  a Hearthstone mana/health bar**: a solid, chunky, game-like bar that fills as the
  session progresses, with the remaining time (or elapsed, in count-up) shown on it.
- Below the bar, a rotating line of **legend text** — alternating between a short,
  in-world line about the mint being grown ("*the roots are settling in*") and a
  genuine, cited study tip (spaced repetition, active recall, etc. — same pool as the
  v2 Study guidance content, §3.2). This is the one place flavour text and real
  advice sit side by side; copy must make clear which is which, which is what the
  literal **"Study tip ·"** label does — it is kept precisely because it is plain.
- Every citation in that pool has been checked against the published work, and two
  lines were rewritten because they did not say what their paper found.
  **Interleaving** was credited with "improves later discrimination between them"; the
  discrimination account is later work (Rohrer, 2012), whereas Rohrer & Taylor (2007)
  measured mixed practice beating blocked practice on a test a week later, and that is
  what the line now says. **Summarising** was cited to Dunlosky et al. (2013) — a
  review that rates summarisation *low* utility, so the line said the opposite of its
  source; it now reports what that review actually concluded, that of ten common
  techniques highlighting and re-reading rated lowest and self-testing highest. Chi et
  al. (1994) is about *understanding* rather than retention, and the line now says so.
  A cited claim that misreports its source is worse than no claim.
- The tag is shown; a **Give Up** button is present, with a brief confirmation (this
  is the "warning before failure" principle 6 calls for) rather than an instant exit.
- A foreground service keeps the session alive. Its notification shows remaining time
  and a Give Up action, and is non-dismissible while a session runs.
- If "keep screen on" is enabled, a wake lock holds the screen awake.

### 5.3 Distraction handling

Detection is layered, each layer independently optional, and the **default build asks
for no special access at all**.

| Layer | Mechanism | Permission | Default |
|---|---|---|---|
| A. App lifecycle | Activity `onStop` — the app lost foreground | none | **On** |
| B. Foreground app polling | `UsageStatsManager` queried every ~1s | `PACKAGE_USAGE_STATS` | Off, opt-in |

A third, stricter layer (active blocking via an accessibility service) stays a v2+
experiment, not a v1 feature — see §11.6.

**Rules:**

- Layer A alone: leaving Mintal starts a grace period with a visible countdown/warning
  in the persistent notification. If the user doesn't return within **15 seconds**,
  the session fails. This needs no permission and is enough for an honest user, which
  is the user Mintal is built for.
- Layer B adds the allowlist: the grace period is skipped when the foreground app is
  permitted, so checking a dictionary doesn't kill a session.
- Incoming phone calls never fail a session.
- Screen-off never fails a session.
- The allowlist is user-editable and starts empty; the system dialler and system
  settings are always implicitly permitted.

**The warning is a second notification on a second channel, and has to be.** The
running-session notification is deliberately `IMPORTANCE_LOW` and silent, which is right
for a timer that sits there for twenty-five minutes. Android fixes a channel's importance
when the channel is created and lets an app only ever *lower* it afterwards, so the
fifteen-second warning could not be made audible on that channel — it arrived silently,
with no heads-up, in the one place the user had by definition already stopped looking.
The warning is therefore posted to its own `IMPORTANCE_HIGH` channel so it pops over
whatever app is in front and makes a sound, with `setOnlyAlertOnce(true)` so the first
post alerts and the twice-a-second updates only change the number.

Mintal explains in plain language what each permission does *before* requesting it,
and never requests one as a precondition for starting a session — see §9.1.

### 5.4 Session end

**Success** (countdown reached zero, or count-up stopped after ≥ the minimum):
- The session is written into **the Grove** (§5.6) as exactly **one node** on the
  chosen species' youngest unfinished plant. One session is one node regardless of its
  length — a three-hour session is not three nodes — because the Grove records
  *completed intentions*, and duration is already measured everywhere in §5.7.
- Any milestone crossed by this session is announced once, plainly, and leaves a slot
  to spend (§6.1).
- Result screen: elapsed time, species, tag, a **1–5 rating selector**, and an optional
  note field (200 characters).

**Failure** (user gave up after confirming, or left too long):
- The session is recorded with its partial elapsed time, and by default **nothing is
  drawn for it in the Grove** — see §5.6 for the setting that turns stubs back on.
- Result screen states what happened without moralising.
- Rating and note are still offered — a session that failed is often the one worth
  annotating.
- Partial time counts toward statistics, marked failed, and **does not** count toward
  unlock milestones. What a failure costs is unchanged: no node, no time toward a
  milestone.

**The result screen offers two outcomes, not three, and neither of them is the
session's.** The row was written when the session ended, two screens earlier; nothing on
this screen can create or delete it. The buttons therefore decide the fate of the rating
and the note and nothing else, and they are named for that: **Save rating and note** and
**Skip**, with a line above them saying the session is kept either way.

They were briefly "Save" and "Discard", and before that there were three, including a
"Save and start another" that went exactly where "Save" went — the hub is where another
session starts from — so it promised a difference it did not make. "Discard" was worse
than redundant: it read as "throw this session away", which is the one thing that button
has never done.

**Count-up minimum:** a count-up session under **1 minute** is discarded entirely.

### 5.5 Rating

- A 1–5 scale captured on the result screen: "how well did that go?" — subjective
  quality, not duration, which is already measured.
- Always skippable; `null` is a first-class value, never shown as zero.
- Editable afterwards from Session detail.
- **Visualised as a line chart** in Statistics — rating over time, so trend is
  visible at a glance — alongside the existing distribution and tag cross-reference.

### 5.6 The Grove

The Grove is **a bed of mints** — not a grid of potted plants, and not the single
continuous runner network v0.3 described. The runner model was botanically defensible and
turned out to be the wrong *unit*: a network that only ever lengthens has no way to say
"this one is finished", so one plant grew forever and a long history became a single
unreadable sprawl. A bed of discrete plants, each grown node by node and each eventually
complete, says the same thing about accumulation while staying legible at any size — and
a species used constantly becomes a clump, which is also what mint actually does.

**It remains a rendering, not a table** (§7): the bed is computed from the session list,
deterministically and seeded by session id, so it never reshuffles between launches.

**How a plant grows**

- **One session is one node.** A node is a stem segment plus the opposite pair of leaves
  on top of it. Not a leaf on its own — *Mentha*, like the rest of Lamiaceae, cannot grow
  one — and not a stage jump.
- **Pairs are decussate.** Each pair is rotated ~90° from the pair below it, so in side
  view every second pair is edge-on: short, narrow, close to the stem. Drawing every pair
  flat is what makes a plant read as a generic herb.
- **Stems are square**, and shown so by a lit angle down one face.
- **Branches start at leaf axils**, never partway up an internode, and the two branches of
  a plant leave the two opposite axils of one node — as a real pair of axillary buds does.
- **Growth is balanced.** After the main stem reaches six nodes, nodes alternate strictly
  left, right, left, right onto the two branches, so a plant never leans because of the
  order its sessions happened to arrive in.
- **A plant stops at maturity** (12 nodes: six on the main stem, three on each branch).
  The next session for that species starts a **new plant** beside it.
- **Only a mature plant flowers.** Flower form is a field mark, so it appears when the
  plant is grown, not before. (The catalogue emblem always shows the flower: it is an
  identification illustration, not a record of growth.)
- **Growth stages**: seedling (1–2 nodes), young (3–5), growing (6–11), mature (12).
- **Leaf size is relative to the plant's own internode**, never to the canvas — or
  Corsican mint, whose whole point is being tiny, comes out the size of spearmint.

**Failed sessions**

A failure is always recorded in the database and always counted in the statistics. What a
setting decides is only whether it is **drawn**:

| Setting | Behaviour |
|---|---|
| **Hidden** (default) | No stub is drawn and none takes up width. The bed is identical to one the failure never happened in. |
| **Fading** | A wilted stub — a short bare stem with two collapsed leaves — stands in time order and thins out over a fortnight, then goes. |

v0.3 drew a wilted sprig permanently, on the reasoning that a Grove hiding the bad days
flatters. On a real phone that reasoning was wrong, because the Grove is not a ledger —
§5.7 is — and the scar outlived its usefulness by weeks while the one thing it taught
("leaving the app ends the session") was already learned in fifteen seconds. Principle 7
decides it: gentle, not punitive. **The setting decides what is drawn, never what is
kept.** A hidden failure still records a position, so its day keeps its Grove anchor and
stays openable.

**Reading the bed**

- **Position is stable.** A plant's place is fixed when it is first planted and never
  changes as it grows; the bed reads left to right as a history.
- **The view is height-driven**: one plant always occupies about the same fraction of the
  band, and the reader pans along the bed. Fitting the whole Grove into the canvas would
  mean every plant shrinks as the Grove succeeds.
- **The band is framed against what is in it**, between one mature plant and one young
  one. Three seedlings framed for the plant they have not grown into yet are three specks
  on an empty field; framed for themselves they are three seedlings.
- **Panning is bounded.** However far the bed is dragged, at least 30% of each axis still
  shows something; it cannot be flung into empty space with nothing left to drag back.
  Once the view has been moved at all, a **Reset view** button appears in the corner and
  returns the pan and zoom to where they started. It is not shown before then: it is an
  undo for a gesture, not a permanent control sitting on the bed.
- **Navigation**: **Day detail** is reached by tapping the growth laid down on a given day
  (grouped by the configurable day start time, default 00:00); it lists that day's
  sessions with time, duration, tag, rating, note, and outcome. **Session detail** (tap
  one session within a day) is unchanged: note, tag and rating are editable, and the
  session can be deleted. This resolves the open question v0.3 carried into §11 — the tap
  lands on the rendered bed, not on a separate calendar.

### 5.7 Statistics

Date range selector: day / week / month / year.

- **Summary:** total focus time in range, session count, success rate, average session
  length, longest session, average rating.
- **Best focus day:** the weekday with the highest average.
- **Best focus hour:** the hour-of-day bucket with the most accumulated time.
- **Chart:** bar chart of focus time per bucket across the range.
- **Rating trend:** line chart of rating over time (§5.5).
- **Tag breakdown:** time per tag, with average rating per tag beside it.
- **Rating breakdown:** distribution of 1–5 across the range.
- **Species summary:** most-planted mint in range.
- Empty states explain what to do rather than showing a blank chart.

**Chart axes.** Ticks are chosen from a ladder of round steps, with the axis top the
first multiple of that step at or above the data. This replaces v0.3's implicit "peak,
half the peak, zero", which produced labels that were wrong rather than merely ugly. Two
properties are guaranteed and tested over every plausible axis:

- **No label on an axis repeats another.** The old scheme, on a one-session rating tally,
  read "1 / 0 / 0".
- **A duration axis is in one unit throughout**, chosen from the step — seconds, whole
  minutes, or whole hours. A fifteen-second day reads "0 / 5 s / 10 s / 15 s"; a real day
  reads "0 / 30 min / 60 min / 90 min"; a long week reads in hours. An axis of focus time
  measured in seconds was reported from a device as unreadable, correctly.

Category labels are laid out by **measuring** them: a label is drawn only if it fits
beside the one before it, so twenty-four hour labels thin out to every second or third
hour instead of being clipped to one character each — which is how an hour axis came to
read "0 0 0 0 1 1 1 2" on a real phone. Dividing the width into equal slots is what
causes that, and is specifically not done. Bars are drawn against the axis top rather
than the tallest bar, and the rating trend carries a labelled 1–5 star axis and a named
x axis.

### 5.8 Tags

- Create, rename, recolour, delete. Deleting a tag leaves its sessions untagged rather
  than deleting them.
- Palette of ~12 preset colours drawn from the Mintal token layer (§8.2); no free
  colour picker in v1.
- Tag names are unique, case-insensitive, max 30 characters.
- A small set of starter tags is created on first launch (e.g. Study, Work, Reading,
  Exercise, Rest) — all editable and deletable.

### 5.9 Allowlist

- Settings → Blocking → Allowlist shows installed launchable apps with checkboxes.
- Visible but inert until layer B is enabled, with an inline explanation of why.
- System apps are in a separate collapsed section.
- v1 supports a single allowlist.

---

## 6. Rules and defaults

| Rule | Value |
|---|---|
| Minimum session | 5 min (countdown), 1 min (count-up, below which it's discarded) |
| Maximum session | 3 h |
| Duration step | 5 min |
| Default duration | 25 min |
| Planting window after START | 5 s, cancellable, nothing written (§5.1.1) |
| Grace period after leaving | 15 s, with a visible warning |
| Nodes earned per successful session | 1, whatever its length (§5.6) |
| Plant maturity | 12 nodes, then a new plant beside it (§5.6) |
| Session rating | 1–5, optional, editable later |
| Note length | 200 characters |
| Tag name length | 30 characters |
| Day start time | 00:00 device-local, configurable |
| First day of week | Locale default, configurable |
| Milestone counting | Successful sessions only |
| Failed sessions drawn in the Grove | No, by default (§5.6) |

**The day boundary.** A new day, for streaks and for the daily statistics, begins at
**00:00 device-local time**. The day-start hour remains configurable for people who study
past midnight, and the Settings copy says plainly that the default is local midnight. Day
bucketing uses the device's own zone, not the stored UTC instant — a session is filed
under the day the person experienced, not the day in Greenwich.

### 6.1 Species and milestones

Twelve mints ship in v1, and the twelve cumulative-time thresholds are unchanged:

| Milestone | Unlocks at |
|---|---|
| 1st | from the start (0 h) |
| 2nd | 2 h |
| 3rd | 5 h |
| 4th | 10 h |
| 5th | 20 h |
| 6th | 35 h |
| 7th | 50 h |
| 8th | 75 h |
| 9th | 100 h |
| 10th | 150 h |
| 11th | 200 h |
| 12th | 300 h |

**What changed is what a milestone gives you.** In v0.3 each threshold unlocked one
*named* species in a fixed order, so the first twenty hours of anyone's use of Mintal
were identical and the mint someone actually wanted could be three hundred hours away.
A milestone now earns a **slot**, and the player chooses which mint fills it:

- Crossing a threshold entitles the player to choose **one** more mint, from everything
  still locked — twelve candidates at the first milestone, eleven at the second, down to
  one at the last.
- The first milestone is at zero, so the player chooses their **starting** mint at the end
  of first-run onboarding rather than being handed spearmint.
- Choosing is **deferrable**. An unspent slot is offered on the hub, in the catalogue and
  on the result screen until it is used.
- A mint chosen while no unlocked mint is selected becomes the selected one, so the
  obvious next move — planting it — is one tap away.

The twelve species themselves, with their binomial names and botanical notes:

| Species | Common name |
|---|---|
| *Mentha spicata* | Spearmint |
| *Mentha × piperita* | Peppermint |
| *Mentha suaveolens* | Apple mint |
| *Mentha arvensis* | Field mint |
| *Mentha aquatica* | Water mint |
| *Mentha × gracilis* | Ginger mint |
| *Mentha longifolia* | Horse mint |
| *Mentha pulegium* | Pennyroyal |
| *Mentha requienii* | Corsican mint |
| *Mentha × piperita* f. *citrata* 'Chocolate' | Chocolate mint |
| *Mentha × piperita* f. *citrata* 'Lavender' | Lavender mint |
| *Mentha cervina* | Hart's pennyroyal |

Each species carries a one-line botanical note in the catalogue — habitat, scent, whether
it's a true species or a hybrid. v2's Herbarium (§3.2) presents these as individual cards;
the underlying data and thresholds don't change.

### 6.2 Cloud backup (v2, opt-in)

Resolving the tension with principle 1 directly: Mintal still runs **no server of its
own**, has **no accounts**, and does **no two-way sync**. What v2 adds is a one-way
*export* of the same local JSON backup (§7.1) to a destination the user already
controls, chosen through Android's Storage Access Framework — their Google Drive,
Dropbox, a Nextcloud folder, anything that exposes a SAF document provider. Mintal
never holds credentials for any of these services and never calls a sync API directly;
as far as the OS and Mintal are concerned, it's just writing a file where the user
pointed. Off by default; the destination is picked explicitly in Settings → Data.

---

## 7. Data model

Room database, version 1.

```kotlin
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,            // epoch millis, UTC
    val endedAt: Long,              // epoch millis, UTC
    val plannedSeconds: Int,        // 0 for count-up
    val actualSeconds: Int,
    val mode: SessionMode,          // COUNTDOWN | COUNT_UP
    val outcome: SessionOutcome,    // SUCCESS | GAVE_UP | LEFT_APP
    val speciesId: String,
    val tagId: Long?,               // null = untagged
    val rating: Int?,               // 1..5, null = not rated
    val note: String?,
    val timezoneId: String          // IANA id at time of recording
)

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorIndex: Int,            // index into the fixed palette
    val createdAt: Long,
    val sortOrder: Int
)

@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,     // stable slug, e.g. "spicata"
    val unlockedAt: Long?           // null = still locked
)

@Entity(tableName = "allowed_apps")
data class AllowedAppEntity(
    @PrimaryKey val packageName: String,
    val addedAt: Long
)
```

`speciesId` is retained on a failed session even though the Grove draws a wilted
sprig for it — the user's intent is still part of the record, and it keeps the column
non-null.

**The Grove is a rendering, not a new table.** It's computed from `sessions` ordered by
time, same as the old Garden grid was — grouping by calendar day becomes "which plants
this day's nodes sit on" instead of "which tile." No schema change was required to move
from Garden to Grove, nor from the runner network to the bed of §5.6; all of it is a
UI-layer change. The layout is deterministic and seeded by session id, so the bed never
reshuffles between launches, and whether a failure is drawn (§5.6) is a display setting
read at render time — never a property of the stored row.

**Unlock state has two sources of truth, and the split is the thing to keep hold of.**

- ***How many* mints are unlocked is derived and never stored.** It is a single `SUM` of
  successful seconds over `sessions`, compared against the §6.1 thresholds.
- ***Which* ones they are is the player's choice**, and a choice cannot be recomputed
  from anything. That is the one fact the `species` table holds: `unlockedAt` is stamped
  when, and only when, the player spends a slot on that mint.

Consequences, each of which is tested:

- A restored backup keeps the choices recorded in it and recomputes the entitlement from
  the restored sessions. A backup from a longer history simply leaves a choice waiting.
- Deleting sessions never revokes a mint. Pending choices are clamped at zero.
- Clearing all data returns the player to one unspent slot and no mints — not to
  spearmint.
- **No species is ever auto-stamped.** Application startup and backup restore both used
  to do this, which silently spent the player's choice for them; neither may again.

**Not in Room** (DataStore Preferences instead): selected species, theme, day start
time, first weekday, detection-layer toggles, keep-screen-on, onboarding-completed
flag, **whether failed sessions are drawn in the Grove** (§5.6), the set of
**dismissed permission notes** (§9, and it travels in backups), **active preset id**
(v2, persisted so it survives app restarts), **cloud backup destination URI** (v2, a SAF
persistable permission, not a credential).

There is deliberately no stored state for the principles screen (§4.3): it is not a gate,
so there is nothing to remember about it.

**Species catalogue** ships as a bundled JSON asset: id, binomial name, common name,
milestone threshold, botanical note, illustration asset. The `species` table stores
only unlock state, so the catalogue can grow across app versions without a migration.

A `presets` table (species + tag + duration, saved as a favourite) arrives in v2 as an
additive migration — no changes needed now to make room for it. The *active* preset is
a DataStore pointer into that table, not a Room column, so switching or clearing the
active preset never touches session history.

### 7.1 Export formats

**CSV** (`mintal_sessions_YYYY-MM-DD.csv`), one row per session:

```
started_at,ended_at,planned_seconds,actual_seconds,mode,outcome,species,tag,rating,note
```

Timestamps ISO-8601 local with offset. Note field RFC 4180 quoted. An unrated session
is an empty field, never `0`.

**JSON backup** contains every table plus preferences and a schema version, so a
restore is lossless. Restore replaces the database wholesale after an explicit
confirmation. This is the same file v2's cloud backup (§6.2) writes to the user's
chosen cloud folder — one format, two destinations (local Downloads, or SAF-picked
cloud folder).

---

## 8. Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 with a small custom token layer (§8.2) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 36 |
| Architecture | MVVM; unidirectional state flow; repository layer over Room |
| DI | Hilt |
| Persistence | Room + DataStore Preferences |
| Background | Foreground Service for the session; WorkManager for maintenance |
| Charts | Hand-drawn Compose canvas — no chart library |
| Illustration | **Procedural Compose canvas**, drawn from per-species traits (§8.3) |
| Images | **None** — there is no species art to load, so no image library is needed |
| CSV | **Hand-rolled** — no dependency at all |
| Cloud backup (v2) | Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) — no vendor SDKs |
| Animation | Compose animation APIs |
| Audio | None — ambient sound cut (§3.2) |
| Reminders | None — cut (§3.2) |
| Widgets (v2) | Glance |
| Testing | JUnit5, Turbine, Room in-memory, and **Robolectric** rendering real Compose to PNG on the JVM for screenshot review |
| Build | Gradle KTS, version catalog |
| Licence | **GPL-3.0** |

Min SDK 26 rather than 29 because nothing in the v1 feature set requires anything
newer, and it widens the device base meaningfully. `java.time` is used directly, without
desugaring, which API 26 permits.

Three rows above changed against v0.3 because the artwork did (§8.3). With the mints
drawn rather than shipped, there is no SVG build step, nothing to decode, and no image
loader — which also removes three dependencies from an F-Droid build (§10.2). The
Robolectric row is not a testing nicety: it is the only way to *see* the UI without a
device, since a CI container has no `/dev/kvm` and therefore no emulator, and it exists
because three visual bugs were shipped while guessing blind.

### 8.1 Dependency swaps

Everything proprietary in the reference material is replaced or dropped:

| Original category | Mintal |
|---|---|
| Firebase Analytics / Measurement | **Dropped** |
| Firebase Cloud Messaging | **Dropped** — all notifications local |
| Firebase Crashlytics | **Dropped** — optional ACRA with user-initiated reports, v2 |
| Firebase Remote Config | **Dropped** — config is compiled in |
| Google Play Billing | **Dropped** |
| AdMob / AppLovin / InMobi / Vungle / Meta Audience | **Dropped** |
| Play Services Auth / Location / FIDO / Places | **Dropped** |
| AppsFlyer attribution | **Dropped** |
| Intercom support chat | **Dropped** — GitHub issues + email |
| Picasso / Fresco | **Dropped** — no image loading at all (§8.3) |
| Evernote android-job | WorkManager |
| Tray preferences provider | DataStore |
| Koin | Hilt |
| Any proprietary cloud SDK (Drive/Dropbox API libs) | **Dropped** — plain SAF intents instead (§6.2, §8) |

No Google Play Services dependency remains, which is what makes F-Droid distribution
possible — see §10.2.

### 8.2 Design notes

**Palette:** light green and light blue as the two principal colours, defined as
semantic roles Mintal owns and maps *onto* Material 3, not inherited from it — built
with dark mode in mind from day one, since adding the roles later is the expensive
part, not filling them in.

1. Two typefaces, chosen once: one with real character for the timer numerals and
   headings (read for twenty-five minutes at a stretch, so it's picked first), one
   quiet face for body text.
2. **Home's navigation is a radial hub**, in the spirit of the PS Vita's LiveArea:
   Session setup at the center, large circular tiles for the Grove, Species/Herbarium,
   Tags, and Statistics arranged around it. This is the single biggest visual
   differentiator in the app and the first thing to prototype (§13) — it changes
   touch-target layout and motion design, not just a skin.
3. **In-session UI is a horizontal, game-style progress bar** (§5.2), not a radial
   arc — both to avoid echoing *Forest* and because it reads legends/tips better than
   a dial does.
4. **The Grove** (§5.6) and the statistics charts are both Compose canvas, built to
   the same tokens — a borrowed chart library reads as borrowed, and the Grove
   renderer in particular is bespoke, not a reskin of any existing plant-growth widget.
5. An accessibility pass (contrast, touch targets, TalkBack labels on the growth
   track, the session bar, and the Grove) is a release gate, not a nice-to-have —
   this matters more, not less, with a radial hub and a freeform Grove, since both
   are harder to make screen-reader-legible than a list or a grid.
6. **Mintal draws edge to edge, and anything anchored to the bottom of a screen keeps
   itself clear of the system bars.** Material's `Scaffold` pads its own `BottomAppBar`
   for them but *not* a hand-written one, so a footer must apply the horizontal and
   bottom safe-drawing insets itself — or, on a phone with on-screen
   back/home/recents, the button sits underneath them and cannot be pressed. Screens
   whose content applies the Scaffold's own padding already have this. The clearance is
   measured against a simulated navigation bar in a screenshot test, because a render on
   a gesture-navigation device will never show the bug.
7. **Progress bars draw no stop indicator.** Material 3 puts a dot at the end of the
   track by default, which on the four-slide first-run deck read as a fifth step that
   never filled.
8. **Light mode needs real separation between page, card and edge.** A 4% tonal
   difference between them renders as no boxes at all. A tappable box is the same
   component as a plain one with an `onClick`, never a second one hand-rolled beside it,
   because 2dp of drift is invisible in review and obvious on a phone.

### 8.3 Illustration brief

**This replaces v0.3's "warm flat vector, 14 commissioned assets" brief.** The mints are
**drawn procedurally** by the app, from the traits visible in the author's photo reference
sheet — one slide per species. Nothing is shipped as art and nothing is fetched, which
means the catalogue, the Grove and the first-run tutorial all draw from one description
of each plant and cannot drift apart.

The brief is now explicit that **distinctness is not the goal — correctness is**. Two
species that genuinely resemble each other should be drawn resembling each other, and
told apart by the trait that actually separates them. v0.3 had this backwards: it asked
for species "differentiated by real botany, not hue", but counted the set a success when
the twelve looked different, which is how one mint ended up wearing another's leaf.

Every species declares:

| Trait | Values | What it is for |
|---|---|---|
| `leafShape` | lanceolate, ovate-lanceolate, ovate, rounded-ovate, oblong, oval, round, linear | The outline, which is most of the identification |
| `attachment` | sessile, stalked | A real field mark: spearmint against peppermint turns on it |
| `margin` | serrate, crenate, entire | Teeth, scallops, or smooth — tapered to entire at both ends, as a real leaf is |
| `surface` | smooth, glossy, veined, puckered | What the blade does with light |
| `pubescence` | none, downy, woolly, hairy | Why horse mint is silver and apple mint white |
| `flush` | none, purple, bronze | A wash under the margin that is not the leaf's own green |
| `variegated` | true/false | Gold blotches out near the margin |
| `stemHue` | green, purple, red-purple, purple-brown, red-brown, grey, brown | Often the strongest cue of all |
| `stemHairy` | true/false | Hairs down the angles of the square stem |
| `habit` | upright, tall, spreading, mat | How the plant holds itself |
| `leafScale` | float | Leaf length **relative to the plant's own internode**, not to the canvas |
| `flower` | slender spike, blunt spike, round head, axillary whorls, spaced whorls, globe, none | Separates the mints faster than leaf shape does |

Drawing rules the brief fixes:

- A leaf is about as long as the internode below it. Half that draws a plant with specks
  on it rather than foliage.
- Veins are drawn: a midrib always, three lateral pairs at full detail, heavier when the
  surface is `veined`.
- Detail is dropped by **size, not zoom level**: full detail above 22 px of leaf, outline
  and midrib above 9 px, flat fill below. A mature Grove is hundreds of leaves.
- Hairs are drawn over the fill and under the outline, because that is where they are.
- Puckering is quilting between the veins, not a texture over the whole blade.
- A blade is offset along the **unit** perpendicular to its axis, never the raw axis
  vector.

#### The twelve, as corrected

| Mint | Leaf | Margin | Surface / hairs | Stem | Habit | Flower |
|---|---|---|---|---|---|---|
| Spearmint *(M. spicata)* | sessile, lanceolate, pointed | sharply serrate | deeply impressed veins | green | upright | slender spike |
| Peppermint *(M. × piperita)* | short-stalked, ovate-lanceolate, dark | serrate | glossy | **red-purple** | upright | blunt spike |
| Apple mint *(M. suaveolens)* | sessile, rounded-oblong | crenate | deeply puckered, white-woolly | green, hairy | upright | slender spike |
| Field mint *(M. arvensis)* | stalked, ovate | serrate | hairy | purple-brown, hairy | upright | **axillary whorls, stem leafy above** |
| Water mint *(M. aquatica)* | stalked, ovate, purple-flushed | serrate | hairy | dark purple-brown, hairy | upright | **round head, one whorl below** |
| Ginger mint *(M. × gracilis 'Variegata')* | short-stalked, ovate | mildly crenate | glossy, gold-cream blotches | green | upright | axillary whorls |
| Horse mint *(M. longifolia)* | sessile, long and narrow lanceolate | serrate | silver-grey from dense hairs | grey, hairy | tall | slender spike |
| Pennyroyal *(M. pulegium)* | stalked, very small oval | near-entire | downy | green, hairy | low and spreading | whorls spaced up erect shoots, bare stem between |
| Corsican mint *(M. requienii)* | sessile, tiny, round | entire | glossy | threadlike green | **flat creeping mat** | none drawn |
| Chocolate mint *(citrata 'Chocolate')* | stalked, ovate, deep green | serrate | bronze-purple flush | **dark red-brown** | upright | blunt spike |
| Lavender mint *(citrata 'Lavender')* | short-stalked, rounded-ovate, blunt tip | shallowly crenate | softly downy, **not** puckered | purple | upright (as chocolate mint) | blunt spike |
| Hart's pennyroyal *(M. cervina)* | sessile, narrow linear, swept up | entire | smooth | wiry green | low, slender | **globes with protruding stamens** |

The entry that was simply wrong before: **lavender mint** was drawn with apple mint's
sessile, puckered, crenate leaf. It is short-stalked, rounded-ovate, blunt-tipped, softly
hairy and smoother, and it stands the way chocolate mint does — they are the same
*f. citrata*.

**Ginger mint** is drawn as the variegated form, which is what the reference photograph
shows; the plain form is solid green and is noted as such in the catalogue entry.

#### If an illustrator is commissioned

The procedural art ships, is legible at Grove size and is correct against the reference
sheet, so commissioned illustration is now an **enhancement rather than a dependency** —
which is the opposite of v0.3, where it was the critical path. The loader shape is fixed
in advance so that it can be adopted one species at a time, with the drawing as the
fallback for every mint not yet delivered:

- a **whole-plant raster** per species for the catalogue and the card view;
- **one vector leaf** per species for the Grove, which composes it into nodes itself;
- the procedural drawing wherever an asset is absent.

Two constraints on such a commission, neither negotiable: the artwork must carry a
**free licence** compatible with GPL-3.0 distribution and be recorded in
`docs/CREDITS.md`, or F-Droid cannot ship it (§10.2); and no species may introduce a hue
outside the token layer (§8.2).

---

## 9. Permissions

| Permission | Purpose | Required? |
|---|---|---|
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the session running | Yes |
| `POST_NOTIFICATIONS` | Session notification | Yes (runtime, SDK 33+) |
| `WAKE_LOCK` | Optional keep-screen-on | Yes |
| `VIBRATE` | End-of-session feedback | Yes |
| `PACKAGE_USAGE_STATS` | Detection layer B | Optional, special access, off by default |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent OEM session kills | Optional, user-initiated only |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | — | **Removed** — reminders cut (§3.2) |

Dropped entirely, in line with principle 5: `BIND_ACCESSIBILITY_SERVICE` (unless the
v2 active-blocking experiment is actually built — §11.6), `SYSTEM_ALERT_WINDOW`, and
`QUERY_ALL_PACKAGES` in favour of a `<queries>` block, which is sufficient for listing
launchable apps.

**Cloud backup (v2)** needs no new dangerous permission: `ACTION_OPEN_DOCUMENT_TREE`
is a system picker, not a runtime permission grant, and Mintal only retains a
persistable URI permission to the one folder the user picked.

The v1 manifest therefore contains **four** permissions a user can't avoid, all
obviously implied by "a timer that keeps running," and one optional special access.

**No** `INTERNET` permission in the default build — a strong, verifiable privacy claim
worth defending. Cloud backup (v2) is implemented without it, via SAF against a
content provider another already-installed app exposes; if that ever stops being
possible for a given provider, that provider is simply unsupported rather than the
claim being quietly dropped. If a future feature needs raw network access, it goes in
a separate build flavour.

### 9.1 Every permission is explained before it is requested

Principle 5 says permissions are "explained before they're requested". v0.3 stated the
principle; this section states the mechanism, because on a device the principle was being
honoured in the wrong order — Android's prompt arrived first, and Mintal's explanation,
if it came at all, came after the answer.

Each permission is introduced **in Mintal's own words immediately before** the system
prompt, never after it, and each explanation says what still works if the answer is no:

| Permission | Explained |
|---|---|
| Notifications | On the fourth slide of the first-run deck (§4.1), then requested. Also explained from the hub if onboarding was skipped. |
| Usage Access | Before Settings opens the system screen. |
| Battery optimisation | Likewise. |

The notification permission **was never actually requested** before this revision, which
on Android 13 and above meant the running-session notification — the thing keeping a
foreground session visible — simply never appeared.

Each explanation carries its own "don't show this again" box. It suppresses **Mintal's
note** only: Android's own prompt belongs to the system and always appears. Every
explanation stays readable at Settings → Permissions, which also states that Mintal has
no internet permission. The suppressed set travels in backups (§7).

**Settings lists each permission exactly once.** Battery optimisation used to appear both
under *Device* ("Ask Android not to kill running sessions") and under *Permissions* ("Not
being killed mid-session") — two names for one system screen — so the *Device* card is
gone and its action lives with the other permissions.

**`SYSTEM_ALERT_WINDOW` was reconsidered and refused again.** A floating overlay to carry
the grace-period warning over another app was built, reviewed and removed: it wanted the
one permission most associated with Android malware, in an app whose README promises the
minimum, and it earned it only during fifteen seconds of a session. The
high-importance notification channel of §5.3 carries exactly what the overlay was for and
costs no permission at all. Future sessions should not re-propose it.

---

## 10. Project layout

```
mintal/
├── app/
│   ├── src/main/kotlin/dev/nheox/mintal/
│   │   ├── core/           # model, time (java.time; no desugaring at minSdk 26)
│   │   ├── data/           # room, datastore, repositories, species asset, CSV/JSON transfer
│   │   ├── domain/         # session state machine, milestones, statistics, Grove layout
│   │   │                   #   — NO Android framework imports
│   │   ├── service/        # foreground service, distraction detection
│   │   ├── ui/
│   │   │   ├── onboarding/ # the four-slide deck, tutorial, principles
│   │   │   ├── home/      # the radial hub
│   │   │   ├── session/   # setup, running, result
│   │   │   ├── grove/     # the bed renderer + day/session detail
│   │   │   ├── stats/
│   │   │   ├── tags/
│   │   │   ├── species/   # list and card views; v2: herbarium
│   │   │   ├── settings/
│   │   │   ├── components/ # growth track, session bar, chart canvas, plant art
│   │   │   └── theme/      # tokens, type
│   │   └── widget/         # v2, Glance
│   └── src/test/           # unit tests + Robolectric screenshot renders (§8)
├── reference/              # GITIGNORED — analysis material only
├── docs/
│   ├── SPEC.md             # this document
│   ├── STATUS.md           # where the project stands, and what is next
│   ├── CREDITS.md
│   └── PRIOR_ART.md
├── CLAUDE.md               # working notes for whoever picks this up next
├── LICENSE                 # GPL-3.0
├── README.md
└── CONTRIBUTING.md
```

`domain/` staying free of Android framework imports is what makes the session state
machine testable on the JVM against an injected clock, and it is worth keeping.

### 10.1 Repository setup

In order, before the first commit that could contain anything from `reference/`:

1. `.gitignore` — Android template, plus `reference/` and `local.properties`.
2. `LICENSE` — the full GPL-3.0 text, verbatim.
3. `README.md` — what Mintal is, build instructions, the no-`INTERNET`-by-default
   claim stated plainly, and a one-line note on what cloud backup does and doesn't do.
4. `docs/PRIOR_ART.md` — a short, honest statement: the category is well-established,
   this is an independent implementation from a behavioural spec, no code or assets
   from any existing app were used or reused, and naming the specific UI choices
   (growth track, session bar, Grove) made deliberately to diverge from *Forest*'s.
5. `docs/CREDITS.md` — every third-party asset with its licence, started from the
   first asset added.
6. `CONTRIBUTING.md` — can be short; honest about the project's stage.
7. GitHub Actions: assemble + unit tests on push.

### 10.2 F-Droid: how it actually builds and ships Mintal

Worth spelling out, because it changes who does what and in what order.

**F-Droid does not accept a pre-built APK.** Submitting an app means submitting a
small **recipe** — a YAML metadata file — that tells F-Droid's own build servers how
to fetch Mintal's source and compile it themselves, from a tagged commit, in a clean
sandboxed environment with no network access during the build. F-Droid then signs the
result with *its own* signing key before distributing it. Mintal's GitHub repo is
never the thing a user downloads; it's the thing F-Droid's server reads.

**What that means for the project, concretely:**

1. **Mintal's own repo doesn't need an F-Droid folder at all.** The recipe lives in a
   *separate*, F-Droid-maintained repository called `fdroiddata`, as
   `metadata/dev.<you>.mintal.yml`. Getting Mintal onto F-Droid is a pull request
   against `fdroiddata`, not a release step inside Mintal's own repo.
2. **The recipe is short** — roughly: the Git URL, which tag/commit to build at
   (`CurrentVersion` / `CurrentVersionCode`), the build steps (for a vanilla Compose
   project, almost always just `gradle:assembleRelease`), and `AllowedAPKSigningKeys`
   if Mintal is ever also distributed elsewhere and the two signatures need
   reconciling. GitHub Releases (§13, CI) are for people installing directly from
   GitHub, Releases/Obtainium, etc.; the recipe points at raw source, not at those
   release artefacts.
3. **Reproducibility is the real constraint, and it's already designed in.** F-Droid's
   build has no network access, so *any* dependency fetched at build time (a gradle
   plugin from a non-mirrored repo, a font downloaded on first run, etc.) breaks the
   build outright. §8's dependency list is already vendor-SDK-free for exactly this
   reason — Hilt, Room, and the rest all resolve from Maven Central/Google's Maven,
   which F-Droid's build environment mirrors. Drawing the mints rather than shipping
   them (§8.3) removed three more dependencies, and the project's own build files
   declare only `mavenCentral()` and `google()`.
4. **No `INTERNET` permission (§9) is a second, independent reason F-Droid likes this
   app**, not a requirement of F-Droid itself — F-Droid does host apps that use the
   network — but it does drive the "Anti-Features" flags down to none, which is what
   shows up as a clean, trustworthy listing.
5. **Order of operations:** get the GitHub repo building cleanly and tagged with a
   real release *first* (§10.1, §12). Only once there's a stable tagged commit is
   there anything for an `fdroiddata` recipe to point at — submitting the metadata
   file is close to the last step, not an early one, and it's a PR to F-Droid's repo,
   separate from any of Mintal's own commits.

In short: building the APK and getting it onto F-Droid are two different jobs done by
two different parties — Mintal ships source, F-Droid's own infrastructure compiles,
signs, and distributes it. Nothing in this spec changes because of that; it just means
the "build the APK" step on the project's side stops at a clean, reproducible,
`assembleRelease`-able Gradle project, not at an actual `.apk` file.

---

## 11. Open decisions

1. ~~Coins or milestones?~~ **Resolved: milestones.** §3.3, §6.1.
2. ~~Licence.~~ **Resolved: GPL-3.0.**
3. **Plant + Grove art.** **Resolved differently: the app draws the mints itself**
   (§8.3), procedurally, from the traits on the reference sheet. No asset is shipped
   and none is fetched. An illustrator is therefore **no longer on the critical path** —
   the offer stands, the loader shape is agreed, and it can be adopted one species at a
   time with the drawing as the fallback. What remains open is only whether to commission
   at all, which is a question about money and taste rather than about whether the app
   can ship.
4. **Ambient audio sourcing.** ~~Deferred to v2.~~ **Cut entirely (§3.2).** This
   leaves an open contradiction to flag, not silently resolve: the brief also says "I
   need a musician," which only makes sense if sound is wanted *somewhere* — UI sound
   effects, a trailer/store-listing piece, an app jingle — even with in-session
   ambient audio cut. Needs a decision on what the musician would actually be scoring
   before that search starts.
5. ~~Min SDK 26 vs 29.~~ **Resolved: 26**, revisit if it causes real pain.
6. **Detection layer C (accessibility service).** Still not v1. Downgraded from
   "deferred, revisit only if users ask" to an explicit v2+ maybe (§3.2): highest-
   friction permission, biggest store-review risk, in direct tension with principle 5.
7. ~~Breaks vs. study guidance mismatch.~~ **Resolved:** ship break-cycling and study
   guidance together in v2, confirmed, targeted explicitly at university/academic
   students, claims cited.
8. ~~Dark theme timing.~~ **Resolved as guidance:** build the token layer with
   semantic roles from day one (near-free); ship dark whenever the palette (light
   green / light blue core) is actually done, v1 or just after.
9. ~~How to document prior art.~~ **Resolved:** both — `reference/` stays gitignored
   *and* `docs/PRIOR_ART.md` states plainly what was and wasn't used, now also naming
   the specific UI divergences from *Forest* (§10.1).
10. ~~Grove navigation model (§5.6).~~ **Resolved: tapping the rendered bed.** The
    renderer was built and the harder, more distinctive option turned out to be
    affordable, so Day detail is reached by tapping the day's growth, not from a
    separate calendar. Bounded panning and the Reset view button (§5.6) are what make
    that safe to navigate.
11. **New: cloud backup provider coverage (§6.2).** SAF covers "any app that exposes a
    document provider," which in practice means Google Drive and Nextcloud-style apps
    work well and some proprietary clients (plain Dropbox, in particular) vary by
    version. Needs a real-device check against the 2–3 providers Chilean/academic
    users are most likely to have before v2 ships.
12. **F-Droid recipe timing (§10.2).** Partly resolved. Release signing is built and
    verified — a signed APK has been produced by CI and installed — but no `v*` tag has
    been pushed, so no GitHub release exists yet and there is still nothing stable for an
    `fdroiddata` recipe to point at. The remaining step is a tag, not code.

---

## 12. Suggested build order

1. Project skeleton, repo hygiene (§10.1), token layer + type (palette: light green /
   light blue core), navigation shell for the radial hub, Room schema, DI
2. Session engine + foreground service + notification (countdown only)
3. Session setup (growth-track duration picker) → running (Hearthstone-style bar) →
   result, with one placeholder species
4. Rating + note capture on the result screen, and editing them from Session detail
5. Grove v0: start with a simple chronological list behind the scenes, so every other
   layer has real data to work against while the renderer (item 13) is built in
   parallel
6. Tags
7. Detection layer A, then B; allowlist, with the pre-failure warning (§5.3)
8. Statistics + hand-drawn charts (time, tag, rating-as-line-chart, §5.5/5.7)
9. Species catalogue + milestone unlock logic
10. Settings
11. CSV export + JSON backup/restore (same format cloud backup will reuse)
12. Count-up mode
13. **Grove renderer** — the bespoke growth visualisation (§5.6, §8.2); this is the
    highest-risk UI item in the app and should start as soon as item 1's token layer
    exists, running in parallel with 2–12, not after them
14. Illustration brief (§8.3) written after step 1, commissioned immediately once a
    rough Grove layout exists (step 13), assets integrated as they arrive
15. Accessibility pass, tests, README/CONTRIBUTING/LICENSE/CREDITS/PRIOR_ART final,
    first tagged GitHub release
16. v2: presets (persisted active preset), breaks + study guidance, Herbarium cards,
    dark theme, quick-view widget, cloud backup (SAF)
17. Once 15 is stable: write and submit the `fdroiddata` metadata recipe (§10.2) —
    this is the actual "get it onto F-Droid" step, done against F-Droid's own repo

**Where this actually stands.** Items 1–14 are implemented, tested and have been run on a
real phone across three rounds of feedback, which is what produced this revision. Item 15
is in progress: the licence, README, CREDITS and PRIOR_ART are written, release signing is
built and verified, and what remains of it is the accessibility pass and a pushed tag.
Items 16 and 17 are untouched by design — 17 cannot start until 15 produces a tag.
`docs/STATUS.md` tracks this in detail and is the file to read first.

---

## 13. Notes on this revision

### 13.1 v0.4 — what three sessions on a real device changed

Every change in this revision came from using the app, not from reviewing it, and the
pattern is consistent enough to be worth naming: **nearly all of them are cases where the
spec was defensible in prose and wrong in the hand.** They are recorded here so that the
reasoning survives, and so none of them is quietly reverted by someone reading only the
section it changed.

- **The planting window (§5.1.1)** exists because the spec had no answer for a mistake. A
  mistyped duration or a wrong tag cost a plant, and the only way to undo it was to fail
  the session deliberately. Arming for five seconds costs an honest user nothing and makes
  the irreversible step reversible, which is the cheaper half of principle 7.
- **The Grove became a bed (§5.6).** The runner network was the headline idea of v0.3 and
  it was botanically justified, but it had no concept of *finished*, so one plant grew
  forever and the Grove got less readable the better you did. One session is one node,
  twelve nodes is a mature plant, and the next session starts another beside it — which
  keeps the "accumulation of real growth" quality v0.3 was reaching for while giving it a
  unit that terminates.
- **Failures stopped being drawn (§5.4, §5.6).** v0.3 reasoned that a Grove hiding the bad
  days flatters. True of a *ledger*, and §5.7 is the ledger; the Grove is not. The scar
  outlived its teaching value by weeks, and principle 7 decides it. The setting that turns
  stubs back on, fading over a fortnight, is there for people who want the honest picture
  — and the data is kept either way, which is the part that matters.
- **Milestones earn a slot, not a species (§6.1).** A fixed unlock order made everyone's
  first twenty hours identical and put the mint someone actually wanted three hundred
  hours away. Letting the player choose costs nothing and is the one place in the app
  where a decision is genuinely theirs. It also forced the useful clarification in §7:
  *how many* mints are unlocked is derived, *which* ones is a choice, and a choice cannot
  be recomputed.
- **Permissions are explained before Android asks (§9.1).** Principle 5 already said this.
  The app was not doing it, and the notification permission was never requested at all —
  so on Android 13+ the foreground notification, the one thing making a running session
  visible, never appeared. A principle stated but not mechanised is not a principle.
- **First launch became four slides (§4.1), and the principles stopped gating it
  (§4.3).** One page carrying the name, the tutorial, the promises and a permission
  request is four questions answered at once. Separately, showing the principles before the
  hub at every launch meant a focus app arguing with its own purpose: it would not start a
  timer until you had read about it.
- **Chart axes are measured, not divided (§5.7).** Twenty-four equal slots clip "03" to
  "0", which is how an hour axis came to read "0 0 0 0 1 1 1 2" on a real phone, and
  "peak, half, zero" rendered a one-session rating tally as "1 / 0 / 0". Both were visible
  the first time anyone looked at a chart with real data in it.
- **The illustration brief inverted (§8.3).** v0.3 made commissioned art the critical path
  and asked for species "differentiated by real botany, not hue" while treating the set as
  successful when the twelve looked *different*. Drawing them from declared traits makes
  correctness the test instead, and takes the illustrator off the critical path without
  closing the door on one.
- **§8's stack table was corrected to what was built.** Coil, the SVG build step and the
  Apache CSV dependency are all gone, because procedural art needs no image loading and
  the CSV writer is twenty lines. Robolectric was added for a reason worth recording: a
  container has no `/dev/kvm`, so rendering real Compose to PNG on the JVM is the only way
  to see the UI before a phone does, and three visual bugs reached the owner's phone while
  that was being guessed at instead.

One thing is recorded as **deliberately rejected**, so it is not re-proposed: a floating
overlay for the grace-period warning, which wanted `SYSTEM_ALERT_WINDOW` (§9.1).

### 13.2 v0.3 — Garden to Grove, and away from *Forest*

- **Cloud sync vs. principle 1.** Resolved by distinguishing *sync* (two-way,
  server-mediated — still out of scope, §3.3) from *backup* (one-way, to storage the
  user already owns, no Mintal server, §6.2). This is the only way to add the
  requested capability without contradicting "no server" and "no accounts."
- **Radial dial (§5.1) and radial arc (§5.2)** were both named directly as too close
  to *Forest*'s signature visuals and have been replaced with a growth-track picker
  and a Hearthstone-style horizontal bar, respectively.
- **Garden → Grove (§5.6)** is the largest conceptual change: one spreading mint
  network instead of a grid of potted plants, justified botanically (mint spreads via
  runners) rather than purely aesthetically, which is what keeps the "epic, colossal"
  quality requested from turning into generic fantasy-tree art unconnected to mint.
  Flagged as higher implementation risk and moved earlier in the build order (§12)
  accordingly.
- **Ambient sound (§3.2) vs. "I need a musician" (§11.4)** — flagged, not resolved,
  since resolving it silently would mean guessing what the musician is actually for.
- **Screen map (§4):** read your note under section 4 as approving the existing
  screen list while asking for the *visual* treatment to become a PS Vita–style radial
  hub rather than a bottom nav bar — that's what changed. Session running was kept as
  a distinct state (a live timer needs one), re-skinned rather than removed; flag me
  if "not necessary" meant something more specific and I've misread it.
- **F-Droid (§10.2), new in this revision:** clarifies that "it's an F-Droid app"
  means F-Droid's own build servers compile Mintal from source against a separate
  `fdroiddata` recipe — it doesn't mean this project needs to produce a `.apk` file
  itself at any point, which is the point that prompted this addition.

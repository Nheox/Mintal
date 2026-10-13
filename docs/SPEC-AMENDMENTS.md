# Spec amendments

Changes to the behavioural specification (v0.3) made in this round, written out in full
so they can be folded into the spec document itself — which lives outside this repository,
in the author's `reference/` material, and is not available to the build or to a cloud
session.

Everything below is implemented and tested in `ccr-d6d58d3f-dpo66a`. Where an amendment
replaces existing spec text, the section it replaces is named.

---

## §5.1 — The planting window (new)

Pressing START **arms** a session rather than beginning one. For
`SessionRules.PLANTING_WINDOW_SECONDS` (5 s) the hub shows a countdown in place of the
START button and a card offering **Cancel**. During the window:

- no session row is written, no foreground service runs, no notification appears;
- changing the duration, the mode or the tag cancels the window outright;
- leaving the hub — to Settings, to the catalogue, or out of the app — cancels it;
- cancelling leaves **nothing** behind: no record, no plant, no wilted stub.

When the window elapses the session starts exactly as before. The rationale is that the
cost of a mistyped duration or a wrong tag used to be a dead plant in the Grove, and a
timer app should not punish a slip of the thumb.

This is a *different mechanism* from the distraction grace period of §5.3, which keeps its
own name and its 15 seconds. The two never overlap: the planting window ends before a
session exists, the grace period only applies once one does.

## §5.6 — How the Grove grows (replaces the runner-network model)

The Grove is **a bed of mints**, not one continuous runner network and not a grid of pots.

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
- **A plant stops at maturity** (`PlantBuilder.MATURE_NODES` = 12: six on the main stem,
  three on each branch). The next session for that species starts a **new plant** beside
  it. A species used constantly becomes a clump, which is both what mint does and what
  stops one plant scaling forever.
- **Only a mature plant flowers.** Flower form is a field mark, so it appears when the
  plant is grown, not before. (The catalogue emblem always shows the flower: it is an
  identification illustration, not a record of growth.)
- **Failed sessions leave a wilted stub** — a short bare stem with two collapsed leaves,
  standing in the bed in time order. They are kept because a Grove that hides the bad days
  flatters.
- **Growth stages**: seedling (1–2 nodes), young (3–5), growing (6–11), mature (12).
- **Position is stable.** A plant's place in the bed is fixed when it is first planted and
  never changes as it grows; the bed reads left to right as a history.
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

## §6 — The day boundary (clarification)

A new day, for streaks and for the daily statistics, begins at **00:00 device-local
time**. The day-start hour remains configurable in Settings for people who study past
midnight; 00:00 is the default and the Settings copy now says plainly that this is local
midnight. Day bucketing uses the device's own zone, not the stored UTC instant.

## §6.1 — Species unlocking (replaces the fixed-order unlock)

Milestones earn a **slot**, not a particular mint.

- The twelve thresholds are unchanged: 0 h, 2, 5, 10, 20, 35, 50, 75, 100, 150, 200,
  300 hours of cumulative successful focus.
- Crossing a threshold entitles the player to choose **one** more mint, from everything
  still locked: twelve candidates at the first milestone, eleven at the second, down to
  one at the last.
- The first milestone is at zero, so the player chooses their starting mint at the end of
  first-run onboarding rather than being given spearmint.
- Choosing is **deferrable**. An unspent slot is offered on the hub, in the catalogue, and
  on the result screen until it is used.
- A mint chosen while no unlocked mint is selected becomes the selected one, so the
  obvious next move — planting it — is one tap away.

**Two sources of truth, and the distinction matters.** *How many* mints are unlocked is
still derived from one `SUM` over successful sessions and never stored. *Which* ones they
are is a decision only the player can make, so it is stored in the `species` table, and
that table is now the source of truth for that one fact. Consequences:

- A restored backup keeps the choices recorded in it, and recomputes the entitlement from
  the restored sessions. A backup from a longer history simply leaves a choice waiting.
- Deleting sessions never revokes a mint. `pendingChoices` is clamped at zero.
- Clearing all data returns the player to one unspent slot and no mints, not to spearmint.

## §8.3 — Illustration brief (replaces the previous brief)

Each mint is drawn procedurally from the traits visible in the author's photo reference
sheet, one slide per species. The brief is now explicit that **distinctness is not the
goal — correctness is**; two species that genuinely resemble each other should be drawn
resembling each other, and told apart by the trait that actually separates them.

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

Drawing rules the brief now fixes:

- A leaf is about as long as the internode below it. Half that draws a plant with specks
  on it rather than foliage.
- Veins are drawn: a midrib always, three lateral pairs at full detail, heavier when the
  surface is `veined`.
- Detail is dropped by size, not by zoom level: full detail above 22 px of leaf, outline
  and midrib above 9 px, flat fill below. A mature Grove is hundreds of leaves.
- Hairs are drawn over the fill and under the outline, because that is where they are.
- Puckering is quilting between the veins, not a texture over the whole blade.

### The twelve, as corrected

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

## §9 — Permissions are explained before they are requested (new)

Every OS permission Mintal can ask for is introduced in Mintal's own words immediately
before the system prompt, never after it, and each explanation says what still works if
the answer is no.

- **Notifications** — explained on the second page of first-run onboarding, then
  requested. (It was never actually requested before, which on Android 13+ meant no live
  notification at all.) Also explained from the hub if onboarding was skipped.
- **Usage Access** — explained before Settings opens the system screen.
- **Battery optimisation** — likewise.

Each explanation carries its own "don't show this again" box — separate from the
principles one above, and about that permission's note alone. It suppresses **Mintal's
note** only: Android's own prompt is the system's and always appears, and every explanation stays
readable in Settings → Permissions, which also states that Mintal has no internet
permission. The suppressed set is stored as `dismissedPermissionNotes` and travels in
backups.

Settings lists each permission exactly once. Battery optimisation used to appear both
under *Device* ("Ask Android not to kill running sessions") and under *Permissions*
("Not being killed mid-session") — two names for one system screen — so the *Device* card
is gone and its action lives with the other permissions.

## §4 — The slogan and the tutorial (new)

Mintal's line is **"Stay focused, stay mintalized"**. It sits under the name on the
principles screen and in About.

Under it, a **three-beat animated tutorial**, looping: set how long you want to focus
(the growth track, with the bud marker sliding out), put the phone down and leave it
alone (a phone tipping face-down with the session bar still running on it), the session
is planted in your Grove (a real mint gaining nodes). Three dots mark the beat.

It is **drawn, not played**: the same Compose canvas and the same `drawPlant` the Grove
uses, so there is no video or GIF asset to ship, nothing is fetched at runtime, and the
tutorial cannot drift from what the app actually does. Where the device has animations
turned off system-wide, the three captions are listed as plain numbered text instead —
someone who turned animations off has asked not to be shown one.

## §4 — The principles stand at every launch (new)

The three claims Mintal makes about itself — it stays on this device, this is what it
will ask for, this is what happens if you leave mid-session — are shown **before the hub
at every launch**, not once at first run. They carry a "Don't show this again" tick-box,
and only that tick retires them.

They remain readable afterwards from Settings → *Mintal's principles*, because a promise
you can no longer find is not much of a promise. The state is one preference,
`principlesDismissed`, and it travels in backups like the rest.

Three doors, decided at startup: no onboarding yet → first run; onboarding done but the
principles not dismissed → the principles, then the hub; otherwise → the hub.

## §4 — Settings: Support Mintal (new)

A "Support Mintal" card sits at the bottom of Settings, directly above About. It states
that Mintal is free software with nothing for sale, and offers a donation link that opens
in the reader's browser via `ACTION_VIEW`.

**No permission is added for this and none is needed.** Mintal still declares no
`INTERNET` permission and still opens no socket; handing a URL to a browser is not network
access by the app. The address lives in one constant, `ui/settings/Donation.URL`. While it
is empty the card still renders with the button disabled and a line saying no address is
set, rather than shipping a link that goes nowhere.

## §5.7 — Chart axes (replaces "peak, half the peak, zero")

Axis ticks are chosen by `AxisTicks`, from a ladder of round steps, with the axis top the
first multiple of that step at or above the data. Two properties are guaranteed and tested
over every plausible axis:

- **No label on an axis repeats another.** The old scheme labelled the peak, half the peak
  and zero, which on a one-session rating tally read "1 / 0 / 0".
- **A duration axis is in one unit throughout**, chosen from the step — seconds, whole
  minutes, or whole hours. A fifteen-second day reads "0 / 5 s / 10 s / 15 s"; a real day
  reads "0 / 30 min / 60 min / 90 min"; a long week reads in hours.

Category labels are laid out by measuring them: a label is drawn only if it fits beside
the one before it, so twenty-four hour labels thin out to every second or third hour
instead of being clipped to one character each — which is what produced the reported
"0 0 0 0 1 1 1 2". Bars are drawn against the axis top rather than the tallest bar, and
the rating trend now carries a labelled 1–5 star axis and a named x axis.

## §3.2 — Study tips (clarification)

The legend line keeps the "Study tip ·" label. Every citation was checked against the
published work, and two lines were rewritten because they did not say what their paper
found:

- **Interleaving** was credited with "improves later discrimination between them". The
  discrimination account is later work (Rohrer, 2012); Rohrer & Taylor (2007) measured
  mixed practice beating blocked practice on a test a week later, and that is what the
  line now says.
- **Summarising** was cited to Dunlosky et al. (2013) — a review that rates summarisation
  **low** utility. The line said the opposite of its source. It is replaced with what that
  review actually concluded: of ten common techniques, highlighting and re-reading rated
  lowest and self-testing highest.

Chi et al. (1994) is about *understanding*, not retention, and the line now says so. The
other three were accurate and are unchanged in substance.

# Prior art

## The honest version

A focus timer that grows a plant while you concentrate is a well-established product
category, and *Forest* is its best-known example. Mintal is in that category on purpose.

Mintal is an **independent implementation written from a behavioural specification** —
what the app should do, screen by screen and rule by rule. No source code, assets,
illustrations, text, layouts, icons or branding from any existing app were used, copied,
decompiled or adapted. Every line of code, every string of UI copy, every plant design and
the name itself are original to this project.

Working material used while writing that specification lives in a `reference/` directory
that is listed in `.gitignore` and has never been committed. It is analysis material only;
it does not enter the build, the repository, or the APK.

## The specific divergences

Where the category has signature visuals, Mintal deliberately does something else — not to
be contrary, but because an independent implementation should look independent:

| The familiar pattern | What Mintal does instead |
|---|---|
| A radial dial to set the duration | A horizontal **growth track**: a stylised mint runner, marked at five-minute steps, with a bud marker you drag (§5.1) |
| A radial arc filling while the session runs | A chunky horizontal **session bar** in the spirit of a game's mana bar, with the time on it and a legend line beneath (§5.2) |
| One plant per session, in a grid or plot | **The Grove**: a bed where a session adds one node — a stem segment and an opposite leaf pair — to a living plant, and a plant that reaches maturity stops while the next session starts another beside it (§5.6) |
| A generic tree or shrub | Only real species of *Mentha*, told apart by leaf shape, attachment, margin, surface, hairs, stem colour and flower form rather than by tint (§6.1, §8.3) |
| Species unlocked in a fixed order | A milestone earns a slot and the player chooses which mint fills it — twelve candidates at the first, one at the last (§6.1) |

The Grove in particular is not a reskin. It is built on how *Mentha* actually grows: square
stems, opposite leaf pairs turned ninety degrees from the pair below them, branches out of
leaf axils, and a plant that finishes rather than scaling forever. A session buys one node
of that structure, so the picture is a record of attention in the plant's own units — and
that is what makes the visualisation Mintal's own.

## What is genuinely shared with the category

The idea of a focus timer, the idea of failing a session when the user leaves the app, the
idea of a visual record of accumulated attention, Pomodoro-style timing. These are ideas
and conventions, not expression, and they are not anyone's to own.

## If you think something here is too close

Open an issue. A specific, concrete report — this screen, this asset, this string — will
be taken seriously and acted on.

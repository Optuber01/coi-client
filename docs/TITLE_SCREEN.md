# The Title Screen Takeover

The Pathway Wheel main menu, and the haunting that predates it.

## The Title Screen Takeover

`screen/title/` replaces the vanilla main menu with **the Pathway Wheel**: a dark void, a slowly
turning ring of the mod's 24 pathway emblems, a gold bloom, the Lord of the Mysteries wordmark at
the centre, and the vanilla buttons re-chromed in `MenuTheme`'s dark/gold language. It is the first
thing a player sees, and before this it was somebody else's game with a mod installed.

The scene is state-aware without asking the server anything: `ClientStateStore` persists
`lastPathway` beside the madness values at disconnect, so **the player's own emblem is lit** on the
wheel and named under the wordmark, and **their persisted madness corrupts the scene**.

`TitleTakeover` is the shell and owns exactly four things — the gate, the wall-clock time base, the
`Geometry` record (`cx`, `cy = 0.46h`, `radius`) and **`accent()`**. That last one is the reason the
class exists: corruption drags a single colour from `CoiStyle.ACCENT` toward a desaturated crimson
and every collaborator reads it, so the void, the ring, the wordmark and the button rails sicken
together instead of four classes each holding an opinion about how red things have got.

| Class          | Draws                                                                                                                                                                                                                                                                                                   |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TitleScene`   | two gradient halves split at `cy`, a radial bloom, ~140 motes seeded from a fixed `long` — the same trick `CracksEffect` uses, to the opposite end: a crack pattern is seeded by its trigger time so every shatter differs, the menu's dust by a constant so the menu looks like itself on every launch |
| `PathwayWheel` | the stroked outer ring, the counter-rotating dashed inner one, the emblems, the travelling crest and the cursor's repulsion                                                                                                                                                                             |
| `TitleLogo`    | `lom_logo.png` (512×339, ratio preserved at every size), the pathway caption, the splash                                                                                                                                                                                                                |
| `TitleButtons` | the button plate: near-black fill, accent border, and the **2px accent rail** the character sheet's destination cards carry — the rhyme that ties the menu to the rest of the mod                                                                                                                       |

Four things a change here must keep:

- **The ring turns; the emblems do not.** Rotating each emblem with its own angle is the obvious
  reading of "a turning wheel" and the wrong one — half the wheel ends up upside down, and these are
  symbols the player is meant to recognise. One revolution per 240 s, off `System.currentTimeMillis()`.
- **The emblems are blitted, not glyphed.** `Pathways.emblemTexture` gives the 64×64 art, drawn at
  the whole-pixel 32 (40 for the lit one). `CoiIcons.drawPathwayEmblem` renders a *9px bitmap font*
  glyph and is mush at this size — it stays right for the sheet and the plate and wrong for here.
  `emblemTexture` is also the one place the `aeon` / `eternalaeon.png` spelling fold is undone.
- **The logo is laid out against vanilla's first button row**, `height / 4 + 48` in
  `TitleScreen.init`, never against a fraction of the screen height: the mod's wordmark is far
  larger than vanilla's, so a percentage that clears the buttons at 16:9 puts them through the
  middle of it in a short window. The band runs from just under the wheel's apex (so the emblem
  riding that point does not land on the lettering) down to that row, and the logo is fitted by *both* its width cap and
  the band's height. On a window too short for both, clearing the buttons
  wins and the apex emblem is allowed to graze.
- **Off is a real path.** `coiTitleScreen` false makes every hook fall through to vanilla —
  panorama, logo, splash placement, button sprites — and `TitleScreenHaunt`, which predates the
  takeover, keeps drawing over the vanilla menu exactly as it did. All three shared hooks (`LogoRenderer`,
  `SplashRenderer`, `AbstractButton`) also check the screen, since the rest of the
  game uses them too.

**The wheel is alive in two ways**, and they share one number. A **travelling crest** circulates the
ring once per 18 s, counter to the wheel and thirteen times its speed — the same sense the dashed
inner ring turns, so the ring reads as the mechanism and the light as driven by it. It is a *pure
function of the clock and the emblem's ring index*, falling off over `CREST_SPREAD` (1.2) emblem
spacings, so one emblem is at full strength and its neighbours are warmed: no state, nothing to pop
when it changes target, and nothing to decide if `Pathways.RING` ever changes size. It raises an
emblem from its resting `REST_ALPHA` (77) toward full and adds two faint rings at the peak, and it **skips the lit
pathway entirely** — that emblem is already at 255 with a halo of its own, so a
travelling light could only make a permanent mark blink. Second, the cursor **pushes emblems away**:
64 px of influence, 16 px of displacement, smoothstepped to zero at the radius (a linear ramp has a
visible crease where it ends), eased toward its target over 120 ms through `TitleTakeover.approach`
— the same contract as `SheetContext.approach`, so `epilepsyMode` returns the target outright at one
chokepoint. Two invariants:

- **The push is measured from the emblem's undisturbed position**, never from its displaced one. A
  displacement fed back into its own input oscillates — the pushed emblem is further away, so it is
  pushed less, so it returns — and an emblem the cursor follows runs away for as long as it is
  followed. Only the easing carries state; the target is a pure function of the cursor.
- **One proximity value drives both the push and the brightening** (and the 32 → 36 px growth).
  Deciding brightness by hit-testing where the emblem *is* would dim it as it slides out from under
  the cursor it is fleeing, which reads as a bug. That shared value is why the two effects are one
  feature.

`TitleTakeover` owns the frame delta as well as the clock, advanced **only** in `drawScene` —
`drawSplash` also runs once a frame, and counting the gap twice would halve every tween — and
clamped to 100 ms, since the menu can sit unrendered for minutes and an unclamped delta finishes
every tween in one step. `PathwayWheel`'s two `float[]` offset arrays are static, in the same shape
as `TitleScene`'s mote field, and are **render thread only**.

**Corruption** is `TitleScreenHaunt.intensity()` — so the hallucinations toggle gates the takeover's
horror for free and a clean player never sees any of it. As it rises: the accent turns, the ring
picks up brief judders, one to three emblems drift toward `error`'s grey on their own slow cycles,
and above 0.6 hairline cracks strike **across** the ring as chords. The haunt's own vignette and eye
apparitions still draw last, above everything. `epilepsyMode` freezes the mote twinkle, the splash
pulse, the judder and the emblem flicker **at their steady state** rather than removing them.

The splash is drawn from `SplashRendererMixin` rather than alongside the logo, so it keeps vanilla's
place in the layer order and only its position moves. The line itself comes from
`TitleScreenHaunt.splashText()`, which decides it once per launch — re-rolling it here would give a
different line every frame, and a haunted line the haunt never chose.

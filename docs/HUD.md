# The HUD

Everything the mod draws over the world: the bars, the plate, the health element, the layout editor that positions them,
and the settings that size them.

## HUD Bars

`MadnessOverlay` and `SpiritualityOverlay` both draw a 182×6 bar and share two helpers:

- `hud/HudAnchor` — `parse(String)` + `resolve(screenW, screenH, barW, xOffset, topY, bottomYOffset)`
  returns `{x, y}`. Six anchors: TOP/BOTTOM × LEFT/CENTER/RIGHT, where LEFT means `x = 10 + xOffset`
  and RIGHT its mirror, `x = screenW - barW - 10 + xOffset` (`HudAnchor.MARGIN`); `isTop()`,
  `isCenter()` and `isRight()` answer which. Every bar now passes its own Y offset for both arguments — the madness bar
  included, since `layoutVersion` 2 (`MadnessOverlay.DEFAULT_TOP_Y = 20` is only the default) —
  so any bar can be dragged whichever edge it is anchored to. `TourScreen` spotlights call the same method,
  which is why they can't drift from the overlays.
- `hud/render/CoiBar` — stateless layers: `frame`, `fill` (gradient + bevel), `shimmer`, `notches`,
  `label` (centred 10px above the bar), `lerpWidth`, `withAlpha`. Anything madness-specific (glitch
  slices, cracks, static, permanent-madness marker, the extras line) stays in its own overlay.

The spirituality bar only draws once a protocol-2 server has actually sent `spirituality` on
`conditions` (`BeyonderState.hasSpiritualityData()`); it goes red below 30% of max, its label
goes red below 25%, and with `spiritualityHideWhenFull` it eases out at full instead of sitting
there. Dev testing: F8 → the *Spirit* buttons.

The spirituality bar is no longer a `CoiBar` composite: it blits three first-party sprites from
`textures/gui/hud/` (`spirituality_fill.png` 182×5 clipped to progress, `spirituality_frame.png`
184×25 drawn at `(fillX-1, fillY-9)`, and `spirituality_frame_critical.png` crossfaded in below
30%), with the luster — edge bloom, motes, drain trail — drawn as plain `fill`s in
`SpiritualityOverlay` and the geometry in `hud/render/SpiritSprites`. Numbers only, right-aligned above
the fill; the flask ornament is the label. `BeyonderState.predictedSpirituality()` extrapolates
between the server's once-a-second regen steps from a rate measured off the last two increases (discarded on any
decrease, capped at max/2 per second and 1.5 s ahead), and hide-when-full now
appears instantly and only retreats after a 1.5 s hold plus a 900 ms fade. The
`hud.coi.spirituality_label` lang key is now unused — leave it in place.

`ActingOverlay` is a third, thinner (182×4) bar using the same two helpers, coloured by
`Pathways.pathwayRgb`. It draws only while `ActingState.hasData()` and the pathway is not
an `OuterPathway`, shows the method cooldown as `mm:ss` on the label, and floats a pathway-coloured
`+N` above the label for 1.2s after a grant. Dev testing: F8 → the *Acting* buttons.

`ResourceOverlay` draws the server's ability resource meters as a stack of 182×4 bars in the
colour each packet names, 18 px apart, growing downwards from a `TOP_*` anchor and upwards from a
`BOTTOM_*` one (`HudAnchor.isTop()`), capped at `resourceMaxBars`. Bars are keyed by id so a
refresh never reorders the stack, and each one expires on its own `ttlMs` unless the server keeps
re-sending it. Dev testing: F8 → the *Resource* / *Res clear* buttons.

## Character Plate

`hud/overlay/CharacterPlateOverlay` replaces three of the four look-alike bars with **one card**. Madness,
acting and the reserve stack were the same 182px `CoiBar` recipe stacked down the top-left corner,
which read as clutter; the plate gives them a shared home and each gauge its own *shape*. The
overlay is the gate and the data; `hud/render/PlateCard` is the card, the header and the rows.

| Row     | Source                        | Notes                                                                                                                                                    |
|---------|-------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| header  | player skin + `BeyonderState` | 16×16 head (face `u=8,v=8`, hat `u=40,v=8`), name, the pathway emblem through `CoiIcons.drawPathwayEmblem` tinted `Pathways.pathwayRgb` + `· SEQUENCE n` |
| sanity  | `100 - madness`               | brain symbol; permanent madness is a **ceiling** the fill can't reach, drawn as a dark capped band on both symbol and bar                                |
| acting  | `ActingState`                 | mask symbol, `mm:ss` method cooldown, the `+N` grant popup                                                                                               |
| reserve | `ResourceState`               | under a divider, one compact row each, capped by `resourceMaxBars`                                                                                       |

**`characterPlateOpacity`** (Elements tab, default `1.0`, clamped
`HudConfig.MIN_ELEMENT_OPACITY`…`MAX_ELEMENT_OPACITY` = 0.15–1.0) fades the whole card so the world
shows through it. The floor is **not zero**: below roughly `alpha < 4/255` the font renderer stops
honouring the alpha channel, so a slider reaching zero would make the numbers behave differently
from the card they sit on — and an element faded to nothing is indistinguishable from one the
show/hide checkbox turned off. It needs no migration and no `layoutVersion` bump, since 1.0 is
exactly the old behaviour.

**Every colour the plate draws goes through `HudOpacity.apply`** — fills, outlines, text colours and
the ARGB tints handed to `ctx.blit` (the player head, the brain/mask sprites). One colour that skips
it is a piece of the card that stays solid while the rest fades, which reads as a bug rather than as
a setting. Two consequences worth knowing: `CoiBar` owns colours the caller cannot reach by dimming
what it passes in — the frame's background gradient and the fill's two bevel hairlines — so `fill`
gained the faded overload `frame` already had, both fed `HudOpacity.current()`; and the card chrome
moved from `CoiStyle.drawCard` to `PlateCard.drawChrome`, which restates the three draw calls but
still reads its three colours **from `CoiStyle`**, because `ui/` seeing the ambient alpha would mean
a `ui → hud` import. The one known gap is `CoiIcons.drawCrest`'s centre pip, on the fallback emblem
for a pathway `Pathways` has no art for — it lives in `ui/` for the same reason.

**Spirituality is deliberately not on the plate** — it keeps its own sprite bar and its own position;
that bar is the one the user is happy with. Rows with no data are **omitted, not blanked**, so the
card's height follows the server, and the whole plate hides when there is no pathway and no gauge
has data (a vanilla server never shows a card containing just a head).

**Symbols** (`hud/render/PlateSymbols`) are `textures/gui/hud/symbol_brain.png` and `symbol_mask.png`, each
a single **32×32 full-colour** sprite — ordinary artwork, not a mask/ink frame pair. `PlateSymbols.SIZE`
is both the sheet edge and the drawn size, so every blit is 1:1 and the art never resamples; 32 is
chosen because the source brain is natively 32×32 and the source clown 64×64, both whole-pixel
ratios. Three draws per symbol:

1. the whole sprite under `EMPTY_RGB`, a **multiply** that dims the art toward its own shadow rather
   than flattening it to grey;
2. its bottom `fill` rows again under the caller's **wash** — `PlateSymbols.NO_WASH` keeps the art's
   own colours, and sanity only bleeds toward the stage colour from stage 2, so the brain shouting
   red means something;
3. the rows a ceiling puts out of reach, under `CAPPED_RGB`.

The partial draws are **sub-rect blits, not scissors** (`v = SIZE - fillH`): a scissor resolves in
window pixels and would cut the wrong rows once the plate sits inside a `HudScale` push. A missing
texture degrades to a filled rounded rect, so a pack that drops the sprites still gets a gauge.
Swapping in better art is a PNG swap with no code change — which is exactly how the shipped pair got
there, replacing a generated placeholder set.

**`showCharacterPlate` (default true) is an either/or.** While it is on, `MadnessOverlay`,
`ActingOverlay` and `ResourceOverlay` draw no bar, `HudElements.all()` drops those three
outright (a superseded element is not the same as one the player switched off, so it is not left in
the editor as a hidden ghost), and the settings screen hides their rows. The madness **screen
effects** are not a bar and keep running either way — that is the easy mistake here. Turning the
plate off restores all three exactly as before.

## Beyonder Health Bar

`hud/overlay/BeyonderHealthOverlay` is the **only element in the mod that replaces a vanilla HUD element**
rather than attaching beside one: it takes `VanillaHudElements.HEALTH_BAR` through
`HudElementRegistry.replaceElement`, keeps the `original`, and calls it whenever our bar is not
drawing — so a vanilla server, a non-Beyonder, creative/spectator, `hud.isHidden()`,
`HudLayout.editing()` or `showBeyonderHealth = false` all get the real hearts back, untouched.

The reason it exists: a Beyonder's HP pool runs 50 (S9) → 1750 (S0) and past 2500 under True Form,
but vanilla `max_health` stays **20** and ten hearts cannot say that. The bar draws `1,234 / 1,750`
inside its own fill, in the hearts' exact footprint.

**The client derives current HP; the server only sends the ceiling.** Vanilla health is a
proportional mirror of the pool, and the plugin recomputes the pool *from* vanilla health after
every hit, so:

```
poolCurrent = maxHealth * (player.getHealth() / player.getMaxHealth())
```

This is not just cheaper than a pushed value — it is **more accurate**, because the plugin's own
`beyonder.getHealth()` is only re-derived every 200 ticks and is stale after non-ability damage. Use
`player.getMaxHealth()`, never a hardcoded 20: some abilities apply a negative max-health modifier,
and the mirror is a percentage either way. `maxHealth` still has to come over the wire (on
`conditions`, see the protocol doc) because it is volatile — True Form doubles it, Strata and Death
marks cut it.

**Four styles, and `HEARTS` is the default.** Replacing the hearts outright took vanilla's *absorption* hearts with it,
and players said so; `hud/render/HealthStyle` is the answer. The
overlay stays the shell — the render gate, the vanilla replacement, the derivation, the flash/pulse
state and the anchor — and `hud/render/HealthBarPaint` owns the pixels of all four:

| Style                | Draws                                                                                                                                                                                 |
|----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `HEARTS` *(default)* | nothing but the readout. Vanilla keeps its heart row, absorption hearts included, and the mod adds the `1,234 / 1,750` those ten hearts cannot carry, plus a gold `+175` for a shield |
| `BAR`                | the 1.2.0 bar, unchanged                                                                                                                                                              |
| `ORNATE`             | the same geometry under carved chrome: near-black border, a bevel cut into the box, gold corner brackets                                                                              |
| `PIPS`               | ten notched segments draining the way a heart row does, absorption as gold pips growing inward from the right                                                                         |

Three things a change here must keep:

- **All four occupy the same `BAR_WIDTH × BAR_HEIGHT` box**, so switching style never moves the
  element and `BeyonderHealthElement`'s `bounds`/`moveTo` stay exact inverses without ever asking
  which style is on. For `HEARTS` the box is where the *numbers* go — vanilla owns the hearts'
  position and that style deliberately does not move them.
- **The replacement hook has three answers, not two.** It used to be
  `if (!render(ctx)) original.extractRenderState(…)`. `HEARTS` needs *both*, and the hearts have to
  go down **first** or they land on the readout, so the displaced element is handed in as a
  `Runnable` the gate calls at the moment it decides. A boolean could express neither the third case
  nor the ordering.
- **Absorption is never invisible.** The gold continues past the main fill while the box has room
  and is drawn *over* the fill's right-hand end when it has not. The original clamped it to
  `BAR_WIDTH - fillW`, which is zero at full pool — so the one state a shield most needs to announce
  itself in was the one state that showed nothing.

`HealthStyle.defaultYOffset()` replaces the old single `DEFAULT_Y_OFFSET`: 39 for the three
replacing styles (the hearts' own row, free precisely because they took the hearts off it) and **59** for `HEARTS`,
which has to clear both the hearts *and* vanilla's armour bar at `h - 49`. The
obvious "one line up" at 49 drops the readout onto the armour of every armoured player. Switching
style **carries the placement across only if the player never moved it**
(`BeyonderHealthOverlay.applyStyle` / `atDefaults`): a bar somebody dragged somewhere must not be
snatched back, and one nobody touched must not end up printed over the hearts it just restored.

`BAR_WIDTH` is **96** — the widest the box can be on the hearts' row. It starts at the hotbar's
left edge (`screenW / 2 - 91`) and ends at `screenW / 2 + 5`; the hearts only reached
`screenW / 2 - 10`, so replacing them freed the 20px gap in the middle, and the food bar at
`screenW / 2 + 10` is the hard stop. Staying on this row is what lets the armour bar keep sitting
directly above it, as it did above the hearts.

`DEFAULT_X_OFFSET` is **derived**, `BAR_WIDTH / 2 - 91`, never typed. `HudAnchor.resolve` centres on
the element's width, so a stored X offset is only meaningful against the width it was calibrated
for — hard-coding it once already went wrong: widening the bar from 82 to 100 left the old `-50`
behind and pushed every existing config 9px to the left. `layoutVersion` **4** resets the bar's
placement for that reason, and **5** does it again for the styles. The identity only holds for an **even** `BAR_WIDTH`,
which is also what keeps the centring exact at odd window widths, so the width
must stay even — 95 shipped in 1.2.0 and broke exactly that, which is the other reason it is now 96.

`BAR_HEIGHT` is **9**, and the constraint is easy to miss: `CoiBar.frame` draws its border *outside*
the box, so the drawn footprint is `BAR_HEIGHT + 2`. The heart row only has `h-40 … h-30` before the
experience bar at `h-29`, and the XP bar draws *after* us — at 11 the bar was 13 rows tall and had
its bottom silently clipped.

The fill colour is **driven by how much pool is left**, not fixed: a deep crimson down to 60%, then
blending to the alarm scarlet, reaching it exactly at `LOW_FRACTION` (30%) where the pulse also
begins — so the colour shift and the pulse are one cue, not two thresholds. A bar that was bright
scarlet at full health spent its loudest colour on the state that needs no attention and had nothing
left to escalate to.

Absorption is drawn as a gold segment past the main fill, converted into pool units — vanilla's
absorption hearts disappear along with the element we replaced, so not drawing it would lose real
information. Damage flashes the bar and low HP pulses it, both suppressed by `epilepsyMode`.

**Known gap:** vanilla's `HEALTH_BAR` height provider still reports heart rows, so with absorption
active the armour/air bars are pushed up and leave a gap above our one-row bar. Fixing it means
replacing the left-side height provider too, which would have to duplicate the whole render gate.

## Server-driven Overlays (batch 3)

Four overlays render structured UI events the plugin used to push through vanilla surfaces. All
four attach before `VanillaHudElements.CHAT` and are gated on `settings.enabled` plus their own
`show*` toggle (DISPLAY tab → *Overlays*):

| Overlay               | Position                                                                                                                                       | Source                  |
|-----------------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------|
| `ActionBarOverlay`    | centred, stacked upward from `h - actionBarYOffset` (default 72), 10 px apart, 2 px channel-coloured tick left of each line                    | `coi-client:actionbar`  |
| `TargetHealthOverlay` | 100×5 bar at `(w/2 - 50, h/2 + 18)`, name above, `hp / max (pct%)` below                                                                       | `coi-client:target`     |
| `CogitationOverlay`   | 220×54 `CoiStyle.drawCard` at `(w/2 - 110, h/2 - 70)`; label at 1.5× scale, streak, draining 200×3 timer                                       | `coi-client:cogitation` |
| `NotificationOverlay` | top-right toasts, `x = w - 12 - 180`, 180 px cards with a 2 px accent bar; slide in 250 ms, hold, fade 300 ms (`epilepsyMode` drops the slide) | `coi-client:notify`     |

Action-bar text arrives as a **vanilla text-component object** and is deserialized through the
registry-free `ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, element)`; anything the codec
rejects degrades to its plain string form rather than dropping the line. Entries expire locally at
`receivedAt + ttlMs`, and `actionBarLines` (0 = follow the server's `maxVisible`) caps how many
draw. Dev testing: F8 → the *ActionBar / Target / Cogitate / Toast* row.

## Ability State on the HUD

`AbilityOverlay.setActive/setCategoryLabel/setCooldown` fan out to **every** slot whose
`AbilityInfo.extractId(stored)` equals the id — exact equality, not `contains`, and no early
`break`. An active (toggled) ability gets a pulsing cyan outline and an `ON` tag; a locked or
blocked one gets a red icon tint and a struck-through red name. State arrives either inline on the
v2 ability list (`active`, `cooldownRemainingTicks`) or live on `coi-client:state`.

## HUD Layout Editor

`screen/settings/HudLayoutScreen` (HUD Settings → *Arrange on screen…*, or *Align* on any element's row)
drags every positionable element into place over the live world. It is **the only position UI**:
the settings tabs carry no anchor cycles and no X/Y offset rows any more — only the show toggles,
sizes and an *Align* button on each element's header. The fields themselves are untouched, still
written to `config/coi_hud.json`, and the presets still assign them. The screen is a thin shell over
`hud/layout/` — it owns selection, the drag and the keys, `LayoutPainter` owns what is drawn over
the world and `LayoutSnap` owns where a dragged element lands:

- `HudElement` — `id` / `label` / `group` / `visible` / `bounds` / `moveTo` / `renderPreview` /
  `resetPosition` / `resetGroup`. `bounds` and `moveTo` are inverses in gui-scaled pixels: after
  `moveTo(x, y, …)`, `bounds` reports `(x, y)` again. Previews draw **sample data** and must never
  touch a class under `domain/beyonder/model/`. One implementation per element lives in `hud/layout/`
  (`SlotElement`, and `BarElements`' `BarElement` plus the descriptors over it), with the
  ids in `ElementIds` —
  never localized, since they go into `coi_hud.json` and into an *Align* button's argument.
- `HudElements.all(settings)` — the descriptors in draw order: `slot_1` … `slot_N` (only the first
  `activeAbilitySlots` of the ten), then `character_plate`, `beyonder_health`, `madness`,
  `spirituality`, `acting`, `resources`, `action_bar`, `target_health`, `cogitation`,
  `notifications`. The list depends on the settings, so the screen builds it once in its
  constructor, and renders forwards / hit-tests backwards so you grab what you see.
- `LayoutGeometry.applyAnchoredMove(...)` — the one rule for anchored bars: the anchor follows the half
  of the screen the bar landed in (`TOP_*` above the middle, `BOTTOM_*` below), and horizontally
  `CENTER` within 12 px of the screen's centre line (and then a near-zero X offset snaps to a true
  0), else `LEFT` (`x = 10 + xOffset`) or `RIGHT` (`x = screenW - barW - 10 + xOffset`) depending on
  which side of the centre line the element's own centre lands. Stacks taller than one bar pass
  their own top/bottom decision.
- **Ability slots are one element each.** `slot_1` … `slot_10` all return `group() ==
  "ability_slots"`; `HudElements.soloSet(id, s)` resolves an *Align* id to a whole group or to a
  single element, and `HudElements.moveGroupBy(…)` is the Ctrl-drag (both delegate to
  `hud/layout/LayoutGroups`, which is where everything group-shaped lives): slots still in the shared row
  ride `hudX`/`hudYOffset` (shifted once, through `AbilityOverlay.shiftRow`), the rest take the
  same delta through their own `moveTo`. `resetGroup` puts the row origin back; `resetPosition` on a
  slot only drops that slot's own placement.
- `HudLayout.editing()` — **every overlay's render gate must include it**, right after the
  `client.gui.hud.isHidden()` check, or the editor will draw its preview on top of the real thing.
  A new overlay that forgets this is the one way to break the editor — which is exactly why the
  whole chain now lives in `hud/HudGate.blocked`, and why an overlay calls that rather than
  retyping it.

**Solo mode** — opened with a preselected id (an *Align* button), the screen resolves it in its
constructor to a one-element list, or — for `ability_slots` — to the whole slot group, and every
loop iterates that `elements` list: nothing else is drawn, outlined, chipped or hit-tested. The
title line above the toolbar reads `screen.coi.layout_title_one` ("Align: <element>", carrying the
group's own `layout_el_ability_slots` label for the slots), `Reset all` becomes
`screen.coi.layout_reset_one` acting on that set alone (plus one `resetGroup` per group), `Tab` is
inert only when the set holds a single element, and a click on empty space keeps the selection.
Cancel / Done / Esc are unchanged. Opened with no id ("Arrange on screen…") the screen lists
everything.

**Group move** — hold **Ctrl** while dragging (or nudging with the arrows) any element that has a
`group()` and the whole group moves by the same delta, each member clamped on screen. The hint
`screen.coi.layout_hint_group` shows above the toolbar only while such an element is selected.
Modifier state comes off the `MouseButtonEvent` / `KeyEvent` (`InputWithModifiers.hasControlDown()`).

Drag snaps to a 4 px grid (Shift for free placement), to the screen centre and to a 10 px margin
within 6 px; arrows nudge 1 px (Shift 8), `R` resets the selection, right-click resets the element
under the cursor, `Tab` cycles (all-elements mode only), `T` flips the toolbar to the other edge,
`Esc` is Done.

**The toolbar gets out of the way.** It is opaque chrome over a live HUD and `inToolbar` turns every
click inside it into a no-op, so an element parked underneath is not merely hard to see — it cannot
be selected, dragged or right-click reset at all. Parked at the bottom it lands squarely on anything
bottom-centre, which is what the Beyonder health element is by definition, and that is what players
reported as the bar they could not move. `chooseToolbarSide` counts how many of the screen's own
elements each candidate band would cover and takes the emptier edge; ties keep the bottom, where the
toolbar has always been. The count is taken **once per `init`**, never per frame — re-deciding while
the player drags would pull the toolbar out from under the cursor mid-gesture — and `T` overrides it
for good, so a resize cannot park it back on what was just uncovered. The hint lines outside the
card are **stacked** rather than placed at fixed offsets (`outsideHintY`), since two of the three are
conditional and the card can be on either edge. Opened from HUD Settings it edits **that screen's working
copy**; opened otherwise it edits the live settings.

**Done commits, whichever settings object the screen was handed.** `persist()` writes a working copy
through to the live settings before saving. Leaving the commit to the settings screen's own Done
meant a player could arrange the whole HUD, press Done here, then press Cancel or Esc on the screen *behind* this one
and lose every drag — which is not something "Done" can mean, and is what players
reported as the layout resetting itself. The write-through carries whatever rows the settings screen
had already changed in that copy too: the two screens edit one object, and a rule that committed
half of it would be harder to predict than one that commits it. Cancel is unaffected — it restores
its snapshot into `settings` and never reaches `persist`.

**`slotSize` is the ability HUD's only size knob.** The slot is drawn at
`HudConfig.BASE_SLOT_SIZE` (40) under a pose scale of `slotSize / 40`, so the keybind chip, the
ability name and the cooldown readout grow with the box instead of staying stuck at one font size —
which is what a plain "draw the box bigger" setting could never do. `MIN_SLOT_SIZE`/`MAX_SLOT_SIZE`
are exactly `MIN/MAX_ELEMENT_SCALE × 40`, so the slider's range maps onto the supported scale band
with nothing to clamp away. Three helpers keep the relationship in one place:
`AbilityOverlay.slotScale(s)`, `boxSize(s)` (`== slotSize`) and `rowStep(s)`
(`boxSize + 10×scale`).

**Per-slot placement** — `AbilityOverlay.slotOrigin(index, w, h, settings)` is *the* answer to
"where does slot N go": the shared row (`hudX + index * rowStep(s)`, `h - hudYOffset` — `rowOrigin`
is plain screen pixels, since the scale grows the slots rather than displacing the row) while
`settings.slotPlacements[index]` is null, otherwise that placement's own `HudAnchor.resolve`, fed
`boxSize(s)`. Either way the box is **clamped** into `[0, w - boxSize(s)]²`, so no slot can be drawn
off screen. `slotBounds` adds the scaled name line under the box, `rowBounds` unions every active
slot (the tour spotlight reads it), and the render loop, the editor previews and `HudGaslight`'s
slot swap (two indices trade origins) all go through `slotOrigin`. Slots sit at their index, not at
their rank among the bound ones.

Positions the editor writes are the same fields the old rows write, so only two things migrate. The
madness bar used to ignore `madnessYOffset` while top-anchored (`layoutVersion` 2: a pre-v2 file gets
`madnessYOffset = 20` back if its anchor is a TOP one). `layoutVersion` **3** retired `hudScale` and
`slotSpacing`: `hudScale` *divided* `hudX`/`hudYOffset` instead of scaling anything, and
`slotSpacing` was a second size knob free to disagree with `slotSize`. Neither has a
position-preserving conversion (the old scale formula depended on the screen height), so a pre-v3
file keeps its offsets verbatim and the two keys are simply dropped on the next save — slots 2..N in
the shared row shift to the derived step. `layoutVersion` **5** sets `beyonderHealthStyle` to `HEARTS` and re-places the
health element: a pre-v5 file predates the setting, so its stored placement was calibrated for a bar
sitting *on* the hearts' row, where the new default's readout would land on the hearts it has just
handed back. `migrate` guards each step by the version that introduced it, so a file at version 0
gets all of them.

## HUD Settings & per-element scale

`screen/settings/HudSettingsScreen` has three tabs, split by what a setting *is* rather than by which overlay
draws it:

| Tab             | Holds                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
|-----------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Ability HUD** | `showAbilityHud` first — off, and the rest of the tab is **not built**, since there is nothing left for a size, a count or a decoration to apply to. Then `slotSize` (the only size knob), key + wheel slot counts, the three slot-decoration toggles, one *Align* for the whole slot group                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| **Elements**    | one uniform block per bar/overlay: a show/hide checkbox **labelled with the element's own name** (`screen.coi.layout_el_<id>`) and an *Align* button, then — **only while the element is switched on** — its *Scale* slider and its own extras (the health element's four-way `Style` cycle, spirituality hide-when-full, resource max bars, action-bar lines). `SettingsRows.elementRow` returns the checked state and re-runs the screen's `init()` on toggle, so switching an element off collapses its block instead of leaving dead controls behind. The Character Plate leads the tab; while it is on, the madness / acting / resources blocks are absent entirely and only `resourceMaxBars` survives, since the plate still draws those rows |
| **General**     | the master `enabled` switch, `useServerMenus` (open the plugin's chest GUIs instead of the mod's menus), `coiTitleScreen` (the Pathway Wheel main menu), accessibility (epilepsy mode, hallucinations, effect volume), the ascension ceremony's three comfort settings (camera shake, cinematic camera, fog density), Discord presence, "Show Tour Again"                                                                                                                                                                                                                                                                                                                                                                                            |

The four presets are gone — the layout editor plus per-element scale cover what they used to
approximate, so the bottom row is just *Reset · Cancel · Done*. Removing them orphaned
`screen.coi.preset*`, `settings_tab_bars`/`_display`, the four `*_section` headers and the eight
`show_*` keys; the element checkboxes now reuse the `layout_el_*` labels the editor already owns.

**Per-element scale** — eight `float` fields (`madnessScale`, `spiritualityScale`, `actingScale`,
`resourceScale`, `actionBarScale`, `targetHealthScale`, `cogitationScale`, `notificationScale`),
plus `characterPlateScale`, default `1.0`, clamped to `HudConfig.MIN_ELEMENT_SCALE`…`MAX_ELEMENT_SCALE` (0.5–2.0). The
ability
slots have no scale field of their own — theirs is derived from `slotSize` (see above) and is the
only one that also scales a *step*, so scaled-up slots never overlap.

**Per-element opacity** is so far the character plate's alone (`characterPlateOpacity`), through the
`hud/HudOpacity` seam. It is deliberately shaped as `HudScale`'s twin — ambient render state pushed
around one element's draw, popped after, render-thread only, so the draw code inside needs no new
argument. The asymmetry is forced rather than chosen: `ctx.pose()` is a 2D matrix stack with no
colour channel, so there is no pose-level alpha to push and opacity has to be applied per colour.
Nested pushes multiply, so a nested piece can never come out more solid than what encloses it.

The one rule: **every element scales about its own fill origin** — the point its `anchor()` returns.
`hud/HudScale` implements it as translate (origin) → scale → translate (−origin) on the pose, so the
draw code inside keeps using absolute screen coordinates unchanged, and a scale of 1 skips the matrix
work entirely. Two consequences anything new must honour:

- anchor resolution passes the **scaled** width (`HudScale.size(BAR_WIDTH, scale)`) to
  `HudAnchor.resolve`, or `*_RIGHT`/`*_CENTER` anchors and the on-screen clamp drift;
- `HudElements`' `bounds` and `moveTo` stay exact inverses only if both scale the label/frame padding
  (`HudScale.size(BAR_LABEL_PAD, scale)`, …) — an unscaled pad there makes elements jump when dragged.

Full-screen effects drawn by an overlay (the madness stage vignette) stay **outside** the push.

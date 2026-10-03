# The mod's own screens

The character sheet, the ability picker, and the first-party icon set all three draw with.

## The Character Sheet

`screen/sheet/CharacterSheetScreen` (**M**, on a `character_sheet` server) is the plate's full-size
sibling: a **single scrolling column of sections**, not a grid of look-alike bars.

**It is drawn as a menu document, and that is the point.** The sheet opens every server-authored
menu the player will ever see, so the two have to be the same object: `CoiStyle.cardWidth` for the
card, `CoiStyle.BACKDROP` behind it, a header carrying the pathway emblem and a close cross, the
faint pathway watermark, `MenuTheme.headingCaption` small-caps sections on the card's own surface, a
draggable accent scrollbar *inside* the card, and `MenuTheme.button` in the footer. Every primitive —
chip, gauge, panel, toggle, badge, hairline — comes from `MenuTheme`, so the sheet cannot drift from
the menus again without somebody changing `MenuTheme` itself. The screen owns **no paint of its
own**. Before this it floated separately bordered cards over a differently dim backdrop with
vanilla-ish buttons in the corner, drew HUD `CoiBar` bars inside a GUI, and used a 12px chip where
the menus use 16 — stepping from it into a menu read as stepping into another program.

| Section     | What it is                                                                                                                                                                                      |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| hero        | 32px player head, the pathway emblem + name in `SheetState.pathwayArgb()`, `Sequence N — <title>`, and the sequence again as a 2×-scaled numeral in a badge                                     |
| vitals      | four 32px rows — health, spirituality, madness, tiredness — each with its **own symbol**, its own colour, the number first and the bar second, and a plain-language line for the stage          |
| acting      | the mask gauge + `mm:ss` method cooldown, then the **whole** source ledger (label · bar · `n / cap`, `∞` when unlimited, red when capped), with chips for limited / overflow / foreign throttle |
| conditions  | Line of Life and Death, Frenzied Mage's Presence, the anomaly — chips, **only when present**                                                                                                    |
| where to go | the nine sub-menus as cards: glyph, name, one line of what is on the other side                                                                                                                 |
| preferences | terrain damage as a switch row, so a preference cannot be mistaken for a doorway                                                                                                                |
| footer      | *Server menu* · *Done*, inside the card under a hairline, in `MenuTheme.button` chrome                                                                                                          |

Three things a change here must keep:

- **The lifecycle is unchanged.** `sheet_open` on init, `sheet_close` exactly once on the way out (both `onClose` and
  `removed`), the `send` guard that never talks into the void, and every value
  re-read from `SheetState` each frame so the 60-tick pushes land live.
- **The draw *is* the layout.** Each section is its own class — `SheetHero`, `SheetVitals`,
  `SheetActing`, `SheetConditions`, `SheetDestinations`, `SheetFooter` over the shared `SheetRows` /
  `SheetChips` — and each is handed a `SheetContext` and returns `int`. Section heights follow the
  data, so a section's `draw` returns the y it reached rather than being measured first — measure and draw are the same
  pass rather than two
  that can drift. Two consequences: mouse events reuse the last frame's hit boxes (`SheetContext.Hit`), the same
  bargain `AbilityPickerOverlay` makes; and the **card's height trails the content by one frame**, so
  `contentHeight` is seeded full (`Integer.MAX_VALUE / 4`) — the card opens at full size and settles
  down onto a short document instead of opening as a sliver and snapping out.
- **A locked destination explains itself.** The card stays legible, dims, gains a lock glyph and
  answers the hover with `screen.coi.sheet_lock_<target>` — a dead grey rectangle that never says
  why is what the old button row did.

Cards send **`ActionPayload.ofOpen(target)`** (which carries the player's `useServerMenus`
preference) and close the sheet; the server answers with its own GUI or a `coi-client:menu` document.

Symbols split by who owns the art: madness and acting reuse `PlateSymbols`' 32×32 brain and mask, and
everything else is drawn from `screen/sheet/SheetGlyphs`' 8×8 grid — blown up by whole pixels, so a mark is
exactly as sharp at 16px as at 32 and needs no PNG. Section headings and condition chips carry the
bundled 16px `CoiIcons` glyphs instead, by the same names a server-authored document uses. The one
deliberate disagreement with the plate is
that **the sheet's brain fills with madness, not sanity**: the row is named Madness and the bar beside
it splits into permanent / godhood / temporary, so a symbol filling the other way would contradict it.

## The Ability Picker

`screen/ability/AbilityPickerOverlay` is the modal shell — the search box, the keyboard and the
scroll; `PickerModel` is the list itself, `PickerPainter` draws one row at a time, `PickerMetrics`
holds the heights and `PickerLabels` every word. The model is a flat `List<Row>` of `UNBIND` /
`HEADER` / `ABILITY` / `MESSAGE` records, so scrolling, hit-testing and keyboard arithmetic all walk
one structure. `Row.clickable()` is what makes headers and the no-results line inert. **Rows are not
uniform height** (`PickerMetrics.ROW_H 26`, `HEADER_H 13`, `UNBIND_H 18`, `MESSAGE_H 18`), so every
scroll calculation is in *pixels*: `PickerModel.contentHeight()`, `maxScroll()` (walks backwards for
the first index whose tail still fits `listH`) and the scrollbar handle. `filtered` stays a plain
ability list, so Enter-picks-first-match is unchanged.

- **Grouping** — sorted pathway → sequence → name, then one header per **pathway *and* sequence**
  (`FOOL · SEQ 5`, then `FOOL · SEQ 4`, …): the sequence is the bracket players actually think in.
  Since the list is already sorted, a group break is just "this row's key differs from the last
  one's". Pathway/sequence prefer `AbilityInfo.pathway()`/`sequence()` and fall back to the id
  segments; a sequence of `-1` prints the pathway alone.
- **Two-line rows** — line 1 is icon + clipped name + right-aligned kind tag (`ACTIVE` / `TOGGLE` /
  `PASSIVE`, or red `LOCKED` / `BLOCKED`); line 2 is cost, cooldown and category. **The cost and
  cooldown badges always draw**, as `◈ <n>` / `◈ <n>/s` and `⏱ <n>s`, with an em-dash for genuinely
  free/instant abilities — a blank where a number belongs is what made the old list read as
  interchangeable. The two glyphs live in the lang strings, so a font that lacks them is a
  translation fix, not a code change.
- **`metaAvailable`**, computed once in `open()` via `PickerModel.detectMeta()`, is
  `ServerCapabilities.has("ability_meta")` *or*
  any listed ability carrying a richer field. False (a protocol-1 server) degrades line 2 to the
  category alone rather than a column of em-dashes. The OR keeps badges alive in the dev
  environment, where nothing answers the hello.
- Categories translate through `screen.coi.ability_cat_*` (14 keys), resolved from the raw
  `info.category()` so a server-invented category falls back to the raw word instead of
  `uncategorized`. Pathway captions stay English and upper-cased — no pathway-name lang keys exist.
- Search matches display name, pathway key **and** category.
- The tooltip keeps only what the row can't show: description, sequence, block reason.

## First-party GUI icons

`ui/CoiIcons` is the one way to draw the mod's small icons, in
`textures/gui/icons/`. **Each icon ships at exactly the size it is drawn at**, so every blit is 1:1
and the artwork never resamples — that is why the sizes are constants in `CoiIcons` rather than a
caller's argument, and why the cog exists twice. Anything that wants a new size gets a new file, not
a scaled blit.

| Icon            | Size | Used by                                     |
|-----------------|------|---------------------------------------------|
| `cog`           | 16   | the badge beside the HUD Settings title     |
| 25 named glyphs | 16   | the menu system's `glyph` icon kind (below) |

**The 25 glyphs** (`alliance`, `authority`, `cooldown`, `cost`, `damage`, `defense`, `divination`,
`flame`, `growth`, `health`, `magic`, `power`, `regen`, `resist`, `restore`, `rites`, `saturation`,
`sequence`, `slot_empty`, `slot_filled`, `soul`, `spirit`, `spirituality`, `uniqueness`, `ward`) are
what a server-authored menu names through `MenuIcon.Kind.GLYPH`. They ship **authored at 16×16**, so
every blit is 1:1 with no reduction at all — unlike the cog, which is a 64→16 whole-pixel reduction.
`CoiIcons.GLYPHS` is the list; `CoiIcons.glyph(name)` resolves one.

`glyph` is the only icon kind whose art lives in this jar, which makes it the only one a server can
name without knowing the player's resource pack — so it is the right kind for anything *conceptual*
(a cost, a cooldown, a sequence) and `item` stays for things that really are an item. **`CoiIcons.glyph` sanitises the
name to `[a-z0-9_]`**, so a document cannot walk out of the icon
folder or reach another namespace; a name that survives sanitising but matches no file just fails
`draw` like any other absent icon.

**16px is the floor for this artwork, and that is a measured fact, not a preference.** The sources
are detailed 64×64 images, not pixel art upscaled from a small grid — only ~34% of the cog's 4×4
blocks are a flat colour. Any reduction below 16px averages the detail away and reads as a blur, so
the slots narrower than that (tab marks, button gutters, the picker's meta-line badges, the plate's
reserve rows) keep their **drawn glyphs and primitives**, which stay sharp at any size. An 8px set
was tried and reverted. Before adding an icon to a new slot, check the slot is at least 16px.

`draw` returns **false** when no loaded pack defines the icon, and every caller falls back to what it
drew before. Presence is cached per identifier and cleared on resource reload by `ClientDataLoader`,
alongside `IconModels` and `PlateSymbols`.

`CoiIcons.drawPathwayEmblem` is the one way to draw a pathway's symbol and returns the width it
drew. The mod ships **real art for all 25 pathways** — 9px bitmaps behind the
`coi-client:pathway_icons` font (`textures/pathways/*.png`), keyed by PUA codepoints in
`Pathways`' own emblem map — so the character sheet and the character plate both use
it and can never show two different symbols for the same pathway. The diamond crest is only a
fallback for a pathway the map does not know; the plate used to draw that crest unconditionally,
which was simply wrong about what art existed.

Source artwork lives in **`art-sources/gui-icons/`** at the repo root — deliberately *outside*
`src/main/resources/`, so the full-size originals are kept for re-cropping without shipping in the
jar. The shipped icon is a whole-pixel reduction (64→16 is 4:1); the plate's symbols are the same (32→32 and 64→32).

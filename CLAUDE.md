# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project Overview

COI Client is a client-only Minecraft Fabric mod implementing a customizable ability system with HUD overlay. Players bind up to **10 abilities** to keybindings (slots 1–6 default Z/X/C/V/B/N, slots 7–10 default unbound), use them in-game, and can customize the HUD visually. The player-facing slot count is the `activeAbilitySlots` HUD setting (1–10, default 6); `AbilityBindings.MAX_ABILITIES = 10` is a hard ceiling because keymappings can only be registered once at init. Lowering the count hides bindings without deleting them. The mod communicates with a server-side Paper plugin via **Fabric custom payloads** (plugin messaging).

**Environment:** Client-only
**Java:** 25 | **MC:** 26.2 | **Fabric Loader:** 0.19.5 | **Fabric API:** 0.159.0+26.2

## Build Commands

```bash
./gradlew build          # → build/libs/coi-client-<version>.jar
./gradlew clean build
./gradlew runClient      # dev client
./gradlew genSources
```

## Documentation

`CLAUDE.md` is the map. The detail lives in `docs/`, one file per topic — read the one you are
about to touch, not all of them.

| Doc                                                                                                             | Read it when                                                                                     |
|-----------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| [docs/NETWORK_PROTOCOL.md](docs/NETWORK_PROTOCOL.md)                                                            | adding or changing anything on the wire                                                          |
| [docs/VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)                                                                | adding an effect, or writing the server call that triggers one                                   |
| [docs/HUD.md](docs/HUD.md)                                                                                      | touching any bar, the character plate, the health element, the layout editor or the HUD settings |
| [docs/SCREENS.md](docs/SCREENS.md)                                                                              | the character sheet, the ability picker, or the first-party icon set                             |
| [docs/MENU_SYSTEM.md](docs/MENU_SYSTEM.md)                                                                      | the server-authored menu documents, both ends                                                    |
| [docs/ASCENSION_CEREMONY.md](docs/ASCENSION_CEREMONY.md)                                                        | the six Sequence 0 set-piece effects and their mixins                                            |
| [docs/TITLE_SCREEN.md](docs/TITLE_SCREEN.md)                                                                    | the Pathway Wheel main menu or the haunting                                                      |
| [docs/MYTHICAL_FORMS.md](docs/MYTHICAL_FORMS.md)                                                                | pathway forms, especially a partial one                                                          |
| [docs/ARCHIVE_PRESENTATION.md](docs/ARCHIVE_PRESENTATION.md) · [docs/ABILITY_MANUAL.md](docs/ABILITY_MANUAL.md) | the archive menu templates                                                                       |

Two handoffs sit at the repo root rather than in `docs/`, because they are contracts with the
plugin repo rather than descriptions of this one: `CLIENT_CEREMONY_HANDOFF.md` (what the server
asked this client to build for batch 7) and `SERVER_IMPACT_FRAMES_HANDOFF.md` (what this client
is asking the server to do with `impact_frame`).

## Architecture

```
dev.ua.ikeepcalm.coi
  ├── CoiClient                 — the client entrypoint, and **only** that: load what is on disk,
  │                               register what draws and what listens, wire the two connection
  │                               events. It holds no ability state any more (see domain/ability/)
  ├── DataGenerator             — datagen entrypoint; creates the pack and generates nothing,
  │                               since every asset this client ships is hand-authored
  ├── config/
  │   ├── AbilityConfig     — persists slot bindings → config/coi_abilities.json
  │   ├── HudConfig         — every HUD setting → config/coi_hud.json
  │   ├── HudConfigIo       — the on-disk key names three ways: `read` ("the value, else
  │   │                       the default"), `write` (the same keys back), and `copy` (the
  │   │                       field-by-field working copy the settings screens edit)
  │   ├── HudConfigMigrations — brings an older file up to `HudConfig.LAYOUT_VERSION`
  │   ├── ClientStateStore  — persistent state, not preferences → coi_client_state.json
  │   └── ClientDataLoader  — the mod's own resource-pack data (the pathway archive), and
  │                           the reload hook clearing IconModels / PlateSymbols / CoiIcons;
  │                           holds `IngredientInfo`, one archive entry
  ├── network/
  │   ├── CoiNetworking     — the wire: payload registration, every S→C receiver, `sendHello`
  │   ├── ClientFeatures    — feature ids + protocol version this client speaks
  │   ├── ServerCapabilities — what the connected server said it can feed; reset on disconnect
  │   └── payload/          — one record per channel, all built from `CoiPayloads`
  │       ├── CoiPayloads   — the shared type/codec shapes: the `coi-client` namespace, the
  │       │                   1 MiB / 32 KiB caps, the read/write pair — spelled out once
  │       ├── ServerboundPayloads — the six C→S records, nested inside it:
  │       ├── AbilityUsePayload      C→S  coi-client:use
  │       ├── AbilityCategoryUsePayload C→S coi-client:use_category
  │       ├── AbilityRequestPayload  C→S  coi-client:request
  │       ├── HelloPayload           C→S  coi-client:hello   (capability handshake)
  │       ├── ActionPayload          C→S  coi-client:action  (open_menu, sheet lifecycle)
  │       ├── MenuActionPayload      C→S  coi-client:menu_action (a click in a menu document)
  │       ├── ClientboundPayloads — the S→C records, nested inside it:
  │       ├── AbilitiesPayload       S→C  coi-client:abilities
  │       ├── AbilitiesV2Payload     S→C  coi-client:abilities_v2 (JSON, 1 MiB cap)
  │       ├── AbilityStatePayload    S→C  coi-client:state   (toggle / category)
  │       ├── ActingPayload          S→C  coi-client:acting
  │       ├── ResourcePayload        S→C  coi-client:resource (JSON, one meter per packet)
  │       ├── ActionBarPayload       S→C  coi-client:actionbar (JSON, text components)
  │       ├── TargetHealthPayload    S→C  coi-client:target
  │       ├── CogitationPayload      S→C  coi-client:cogitation
  │       ├── NotifyPayload          S→C  coi-client:notify
  │       ├── CooldownPayload        S→C  coi-client:cooldown
  │       ├── ConditionsPayload      S→C  coi-client:conditions
  │       ├── AppearancePayload      S→C  coi-client:appearance
  │       ├── MythicalFormPayload    S→C  coi-client:mythical
  │       ├── SheetPayload           S→C  coi-client:sheet   (JSON, 1 MiB cap)
  │       ├── ServerInfoPayload      S→C  coi-client:server
  │       ├── MenuPayload            S→C  coi-client:menu    (JSON document, 1 MiB cap)
  │       └── VisualEffectPayload    S→C  coi-client:effect
  ├── domain/               — what the mod knows, as opposed to what it draws
  │   ├── ability/
  │   │   ├── model/
  │   │   │   ├── AbilityInfo       — ability metadata record + the `id - englishName` encoding
  │   │   │   ├── AbilityCategories — the per-ability category choice and its lock, as the
  │   │   │   │                       server last reported it
  │   │   │   └── Pathways          — **the** single home for a pathway as a pathway:
  │   │   │                           normalisation, the 25-colour `pathwayRgb` table, the
  │   │   │                           emblem glyph, the spelling
  │   │   └── service/
  │   │       ├── AbilityRegistry   — the server's ability catalogue: both list formats (v1
  │   │       │                       delimited, v2 JSON) land in the same two collections, so
  │   │       │                       nothing downstream has to know which protocol answered
  │   │       └── AbilityBindings   — which ability sits in which key slot / wheel slot /
  │   │                               gesture, and the persistence behind all three; owns
  │   │                               MAX_ABILITIES
  │   ├── beyonder/
  │   │   └── model/        — one holder per S→C channel; all parse through util/JsonRead
  │   │       ├── BeyonderState     — madness, spirituality, health ceiling, pathway + sequence
  │   │       ├── ConditionsParser  — the `key=value;…` body of coi-client:conditions
  │   │       ├── SpiritualityTracker — spirituality as the HUD needs it: the server's last
  │   │       │                       figure plus the regen rate `predictedSpirituality()`
  │   │       │                       extrapolates from
  │   │       ├── ActingState       — acting progress, method cooldown, last grant
  │   │       ├── ResourceState     — ability resource meters keyed by id, TTL-expired
  │   │       ├── ActionBarState    — action-bar entries + client-side TTL expiry
  │   │       ├── TargetState       — last ability hit (name, before/after, HP)
  │   │       ├── CogitationState   — cogitation session: prompt, streak, timeout, fail
  │   │       ├── NotificationState — toast queue (3 visible, rest promoted in turn)
  │   │       ├── SheetState        — character sheet snapshot (identity, vitals, mind, acting
  │   │       │                       ledger, sub-menu gates)
  │   │       ├── SheetParser       — coi-client:sheet JSON → SheetState.Snapshot, defensively
  │   │       └── AppearanceState   — appearance traits per player UUID
  │   ├── menu/             — the declarative menu document (server-authored screens)
  │   │   ├── model/
  │   │   │   ├── MenuDocument      — session / version / screen id, header, sections, footer
  │   │   │   ├── MenuComponent     — sealed: text, note, stat, kv, checklist, button(s),
  │   │   │   │                       toggle, list, grid, input, divider, spacer, plus the v2
  │   │   │   │                       five (hero, details, steps, chips, panels)
  │   │   │   ├── MenuIcon          — pathway emblem / item model / glyph / ability / player
  │   │   │   │                       head / none
  │   │   │   ├── MenuParts         — the structures that appear in more than one component
  │   │   │   ├── MenuStyles        — the wire's enum words, folded onto this client's enums
  │   │   │   └── MenuLimits        — how much of a document this client will read
  │   │   └── service/
  │   │       ├── MenuParser        — defensive Gson; unknown `type`s skipped, sizes clamped
  │   │       ├── MenuJson          — the defensive reads a document is parsed with
  │   │       ├── MenuState         — the current menu document + a revision the screen watches
  │   │       └── MenuSample        — the hand-written document behind F8 → *Menu*
  │   ├── ceremony/         — protocol batch 7: the Sequence 0 ascension's set pieces. Six
  │   │   │                   more ids on the **existing** `coi-client:effect` channel,
  │   │   │                   announced by the `ceremony` feature (docs/ASCENSION_CEREMONY.md)
  │   │   ├── CeremonyEffect    — all six, one `Kind` per id: parse, push to a state, report
  │   │   │                       finished. The handle holds nothing itself
  │   │   ├── CeremonyEffects   — the hard reset every exit path shares, and the one tick this
  │   │   │                       batch needs: a new world, and the player taking the camera
  │   │   │                       back. Holds `CeremonyParams`, the defensive `key=value` read
  │   │   │                       plus the two shapes a stop arrives in (`stop`, `stop,fade=600`)
  │   │   ├── PostFx            — which pass is over the world, with the ramp either side, and
  │   │   │                       the five pass names with the graded chain each resolves to
  │   │   ├── SkyTintState      — the sky and fog an aftermath walks the world toward
  │   │   ├── LetterboxState    — the bars, and the slide at both ends
  │   │   ├── CameraCinematics  — the two things a rite does to the camera, both **view only**
  │   │   │                       and both read by `CameraMixin`: `ScreenShakeState`
  │   │   │                       (amplitude / hz / decay) and `CameraOrbitState` (the arc at
  │   │   │                       a rite's peak, as one `Placement` per frame)
  │   │   └── AudioBedState     — the crossfaded score bed and the `CeremonyBedSound`
  │   │                           instance behind it; specified, and
  │   │                           unused until the 22 `mysterria:ascension.*` tracks exist
  │   ├── effect/
  │   │   ├── EffectManager     — registry + active list, renders via HudRenderCallback
  │   │   ├── VisualEffect      — interface (start/render/isFinished/stop)
  │   │   ├── EffectSounds      — the audio companions (loops and one-shots)
  │   │   ├── HallucinationManager — client-side madness hallucinations, on the client tick
  │   │   └── visual/           — the effects themselves
  │   │       ├── EffectParams  — the `key=value,key=value` param splitter
  │   │       ├── EffectPaint   — the 2D paint helpers vanilla does not expose
  │   │       ├── CracksEffect, EyesEffect, VignetteEffect, HeartbeatEffect, GlitchEffect,
  │   │       │                   BloodRainEffect, FrostEffect, WhispersEffect, TunnelEffect,
  │   │       │                   FlashEffect, HallucinationEffect
  │   │       ├── ImpactFrameEffect — the sakuga impact frame (`impact_frame`): a 24 fps plan of
  │   │       │                   negative → held two-tone plate → release, composed around a
  │   │       │                   screen point or a projected world one; ImpactFrameDrawing is the
  │   │       │                   seeded drawing itself (splash, spikes, ring, lines, debris)
  │   │       ├── SpellImpactEffect — the world-space spell impact (`impact`); a shell over the
  │   │       │                   four collaborators below
  │   │       └── ImpactStyle (the presets), ImpactGeometry (the randomised shapes),
  │   │                           ImpactRenderer (every mark it puts on the world or the
  │   │                           screen), WorldImpact (one impact playing out at a position)
  │   ├── form/             — mythical creature forms (docs/MYTHICAL_FORMS.md)
  │   │   ├── MythicalFormManager  — uuid → pathway map, fed by coi-client:mythical
  │   │   ├── MythicalCreatureForm — per-pathway form; pathway/ holds all 20
  │   │   ├── FormPrimitives    — the hand-authored cuboid geometry the full forms are drawn from
  │   │   ├── PartialFormSpec   — placement/scale of a baked lower-body model
  │   │   ├── PartialForms      — shared resolve + carrier-transform helpers
  │   │   ├── PartialFormLayer  — draws the baked model as a player render layer
  │   │   ├── FormModel         — the `carrierDelta()` contract a baked model can implement
  │   │   ├── FormModelLayers   — registration + placement of the baked models
  │   │   ├── pathway/          — the 20 pathway forms (FoolForm, SunForm, VisionaryForm, …)
  │   │   └── model/            — Blockbench exports (VisionaryLowerModel/Animations);
  │   │                           **generated — regenerate from Blockbench, never hand-edit**
  │   ├── appearance/
  │   │   ├── AppearanceTraitLayer    — render layer for the traits the server granted
  │   │   ├── AppearanceTraitRenderer — one additive trait
  │   │   ├── TraitGeometry           — smooth tubes/quads/triangles in block units
  │   │   └── trait/                  — HornsTraitRenderer, MushroomTraitRenderer,
  │   │                                 FemaleTraitsRenderer
  │   └── gesture/
  │       ├── GestureType       — 5 shapes (circle, V, Z, line down, triangle):
  │       │                       direction templates + preview polylines
  │       ├── DirectionCodes    — resampled stroke → 8-way direction string
  │       └── GestureRecognizer — resample → DirectionCodes → Levenshtein match
  ├── hud/
  │   ├── HudGate           — `blocked(client, settings)`: the four refusals every overlay
  │   │                       opens with. Calling it is how an overlay cannot forget the
  │   │                       `HudLayout.editing()` term
  │   ├── HudAnchor         — TOP/BOTTOM × LEFT/CENTER/RIGHT corner math for bars
  │   ├── HudScale          — per-element scaling about the element's own fill origin
  │   ├── HudOpacity        — HudScale's other half: ambient per-element alpha, pushed
  │   │                       around one element's draw and read by `apply(argb)`
  │   ├── HudGaslight       — the HUD's madness lies (wrong cooldowns, swapped slots)
  │   ├── overlay/          — everything that draws on the HUD; each gates on HudGate
  │   │   ├── AbilityOverlay        — all ability slots; owns `slotOrigin` / `boxSize` /
  │   │   │                           `rowStep` / `shiftRow`, the answer to "where is slot N"
  │   │   ├── BeyonderHealthOverlay — replaces the vanilla hearts with the real HP pool,
  │   │   │                           numbers inside the bar
  │   │   ├── CharacterPlateOverlay — one card: head + pathway crest, sanity (brain) and
  │   │   │                           acting (mask) symbol gauges, reserve rows; supersedes
  │   │   │                           the madness / acting / resource bars
  │   │   ├── MadnessOverlay        — madness bar + the stage screen effects
  │   │   ├── SpiritualityOverlay   — spirituality bar (protocol 2 only)
  │   │   ├── ActingOverlay         — acting bar + `+N` gain popup (protocol 2 only)
  │   │   ├── ResourceOverlay       — stack of server-pushed ability resource meters
  │   │   ├── ActionBarOverlay      — COI's own action-bar channel lines above the hotbar
  │   │   ├── TargetHealthOverlay   — hit target's HP bar under the crosshair
  │   │   ├── CogitationOverlay     — centred cogitation prompt card + streak + timer
  │   │   └── NotificationOverlay   — top-right toast stack for `coi-client:notify`
  │   ├── render/           — the paint the overlays share: no state, no render gates
  │   │   ├── CoiBar            — stateless bar layers (frame/fill/shimmer/notches/label)
  │   │   ├── HealthStyle       — which shape the HP pool takes (hearts / bar / ornate / pips),
  │   │   │                       and the per-style default Y offset
  │   │   ├── HealthBarPaint    — every pixel of all four; the absorption rule lives here
  │   │   ├── MadnessPalette    — the bar's colours and status word, one set per stage
  │   │   ├── MadnessCorruption — the full-screen stage effects: vignette, VHS, static
  │   │   ├── PlateCard         — the character plate's card, header and gauge rows
  │   │   ├── PlateSymbols      — the 32×32 fillable brain / mask sprites
  │   │   ├── SpiritSprites     — the three first-party spirituality sprites + the luster
  │   │   ├── AbilitySlotWidget — single slot: icon, cooldown, keybind, glow, toggle
  │   │   │                       outline + "ON" tag, red strike when locked/blocked
  │   │   └── SlotAnimations    — the three moments a slot animates: the cast punch, the
  │   │                           ready flash, the cooldown sweep
  │   └── layout/           — the HUD layout editor's model
  │       ├── HudLayout     — `editing()` flag every overlay's render gate honours
  │       ├── HudElement    — id / label / group / visible / bounds / moveTo / preview / reset
  │       ├── HudElements   — the descriptor list in draw order, `soloSet`, `byId`
  │       ├── LayoutGeometry — the arithmetic every element shares, incl. the anchored-move rule
  │       ├── LayoutGroups  — everything the editor does to a group rather than an element
  │       ├── ElementIds    — the stable, never-localized ids
  │       ├── SlotElement   — one ability slot (slot_1 … slot_10, one group)
  │       └── BarElements   — every bar-and-overlay descriptor, nested: the two bases
  │                           (`AbstractElement`, and `BarElement` — a 182-wide bar with its
  │                           label 10px above it) and the ten over them,
  │                           CharacterPlateElement, BeyonderHealthElement, MadnessElement,
  │                           SpiritualityElement, ActingElement, ResourceElement,
  │                           ActionBarElement, TargetHealthElement, CogitationElement,
  │                           NotificationElement
  ├── screen/               — the chrome these draw with lives in ui/CoiStyle
  │   ├── ScreenInput       — the keyboard concerns the mod's own screens share
  │   ├── ScrollbarPainter  — the 3px track-and-thumb bar drawn *beside* a card
  │   ├── CoiTabButton      — hand-drawn tab widget used by binding + settings screens
  │   ├── GestureScreen     — hold Left Alt, draw with mouse, release to cast;
  │   │                       inert until a gesture has an ability bound
  │   ├── TourScreen        — first-join walkthrough: spotlight cutouts + text cards,
  │   │                       movement stays enabled; re-run via "Show Tour Again"
  │   ├── TitleScreenHaunt  — the main menu remembers the madness you left with
  │   ├── title/            — the Lord of the Mysteries main menu (docs/TITLE_SCREEN.md)
  │   │   ├── TitleTakeover — the gate, the time base, the geometry and the one accent
  │   │   ├── TitleScene    — the void: gradient, central bloom, drifting motes
  │   │   ├── PathwayWheel  — the two rings, the 24 emblems, the lit one, the corruption
  │   │   ├── TitleLogo     — the wordmark, the pathway caption, the relocated splash
  │   │   └── TitleButtons  — the plate drawn in place of a vanilla button sprite
  │   ├── InventoryHint     — a strip on the vanilla inventory saying the Mystery Arts
  │   │                       item is gone and naming the live "open menu" keybind;
  │   │                       dismissed once, remembered in coi_client_state.json
  │   ├── sheet/            — the Beyonder character sheet (opened with M)
  │   │   ├── CharacterSheetScreen — one `MenuTheme` card, drawn as a menu document would be
  │   │   ├── SheetContext / SheetMetrics / SheetPalette — what a section is handed to draw
  │   │   │                       itself with, the measurements + the one hit test its cards
  │   │   │                       use, and every colour the sheet decides for itself
  │   │   ├── SheetHero, SheetVitals, SheetActing, SheetConditions, SheetDestinations,
  │   │   │   SheetFooter, SheetRows, SheetChips — one section each, in draw order
  │   │   └── SheetGlyphs   — the sheet's 8×8 drawn marks (heart/flask/hourglass, the
  │   │                       nine destination glyphs, lock)
  │   ├── ability/
  │   │   ├── AbilityBindingScreen — bind abilities to slots (opened with K); tabbed
  │   │   │                       (hotkeys/wheel/gestures) with a per-tab how-to banner
  │   │   ├── BindingRowPainter — one row of that screen's slot list
  │   │   ├── AbilityWheelScreen — the radial picker, held open on G
  │   │   ├── AbilityPickerOverlay — the modal chooser: the screen, its input and its scroll
  │   │   ├── PickerModel   — the flat Row list: the search, the grouping, the row heights
  │   │   ├── PickerPainter — one row at a time, plus the tooltip after the scissor
  │   │   ├── PickerMetrics — ROW_H 26 / HEADER_H 13 / UNBIND_H 18 / MESSAGE_H 18
  │   │   └── PickerLabels  — every word a row or its tooltip puts on screen
  │   ├── settings/
  │   │   ├── HudSettingsScreen — HUD customization: 3 tabs (Ability HUD/Elements/General),
  │   │   │                       scrollable rows so it fits any gui scale. Holds **no**
  │   │   │                       position rows: "Arrange on screen…" plus the per-element
  │   │   │                       *Align* buttons are the only position UI
  │   │   ├── SettingsTabs  — the contents of those three tabs, one method each
  │   │   ├── SettingsRows  — the row builders the tabs compose
  │   │   ├── HudLayoutScreen — drag-to-position editor: sample previews of every
  │   │   │                       element over the live world — or one element / one group,
  │   │   │                       in solo mode — snapping, Ctrl group move, nudge keys
  │   │   ├── LayoutPainter — what that editor paints over the world
  │   │   └── LayoutSnap    — where a dragged element actually lands
  │   ├── menu/             — the server-authored menu renderer
  │   │   ├── MenuScreen    — one scrolling card: header + sections + footer,
  │   │   │                   hand-drawn buttons, live list search, confirm modal,
  │   │   │                   collapsible sections + details, draggable scrollbar
  │   │   ├── MenuTheme     — every colour and primitive the screen draws with
  │   │   ├── MenuContext   — everything a collaborator may ask the screen for, including
  │   │   │                   `approach` (the one easing chokepoint)
  │   │   ├── MenuPart / MenuPartFactory — one laid-out piece in content space, and the
  │   │   │                   document → flat part list the card scrolls
  │   │   ├── MenuTextParts / MenuValueParts / MenuControlParts / MenuCollectionParts —
  │   │   │                   the words, the values, the things the player operates, and
  │   │   │                   the parts that repeat a cell
  │   │   ├── MenuChrome, MenuGauges, MenuIcons, MenuScrollbar, MenuConfirmModal
  │   │   └── MenuMetrics   — the geometry a document is drawn to, and its one hit test
  │   └── debug/            — dev-only (F8)
  │       ├── EffectDebugScreen — test effects, forms, bars and menus without a server
  │       ├── DebugStates       — the state pokes behind its buttons
  │       └── AppearanceDebugScreen — preview the appearance traits
  ├── input/
  │   └── CoiKeyBindings    — every keymapping and what pressing one does; presses are
  │                           edge-triggered by hand, because the wheel and gesture screens
  │                           need to know the key is still *held*
  ├── mixin/                — every mixin and accessor
  │   ├── LivingEntityRendererMixin — the mythical form: cancels the vanilla player render
  │   │                       for a full form, pushes hipRaise + the carrier transform
  │   │                       for a partial one
  │   ├── AvatarRendererMixin / AvatarRenderStateMixin (with `util/duck/AvatarRenderStateAccessor`) —
  │   │                       attach the mod's layers, and carry a player UUID on a render
  │   │                       state vanilla gives no identity to
  │   ├── PlayerModelMixin / HumanoidArmorLayerMixin — hide legs, then leggings + boots,
  │   │                       under a partial form
  │   ├── TitleScreenMixin  — the title-screen haunting hook, and the takeover's scene
  │   │                       (`extractBackground`, in place of the panorama)
  │   ├── LogoRendererMixin / SplashRendererMixin — suppress the vanilla wordmark, and
  │   │                       redraw the splash off ours, while the takeover is on
  │   ├── AbstractButtonMixin — the title screen's button chrome (`extractDefaultSprite`)
  │   ├── GameRendererMixin — runs the ceremony's post chain over the finished world,
  │   │                       in the same gap vanilla uses for the creeper view
  │   ├── CameraMixin       — the ceremony's orbit and shake, at the end of
  │   │                       `alignWithEntity` so the cull frustum follows them
  │   ├── FogRendererMixin / SkyRendererMixin — the two halves of `coi-client:sky_tint`:
  │   │                       the fog's colour and planes, and the sky disc
  │   ├── LoadingOverlayMixin / JoinMultiplayerScreenMixin / OnlineServerEntryMixin — the
  │   │                       startup logo and the Mysterria server-list entry
  │   └── EntityRendererAccessor / LivingEntityRendererAccessor / SelectionListEntryInvoker
  ├── ui/
  │   ├── CoiStyle          — shared dark/gold palette + card chrome; the BACKDROP / SCRIM /
  │   │                       VEIL dimming ladder, `cardWidth` and `formWidth`
  │   ├── CoiIcons          — the mod's first-party GUI icons, and the one way to draw them
  │   ├── AbilityIcons      — shared icon renderer: pack item model, else category/tier
  │   ├── IconModels        — "is that item model loaded?", cached per reload
  │   └── IngredientTooltips — names the pathway an item belongs to, in its tooltip
  └── util/
      ├── CoiLog            — the mod's one logger ("COI Client"); no System.out.println remains
      ├── JsonRead          — the defensive scalar reads every state class shares
      ├── duck/             — duck interfaces the mixins implement. **Must live outside
      │   │                   `mixin/`**: the mixins.json `package` owns that whole subtree,
      │   │                   and Mixin refuses to let ordinary code load a class from it
      │   └── AvatarRenderStateAccessor — the player UUID `AvatarRenderStateMixin` adds
      └── hooks/
          └── DiscordPresenceManager — Discord Rich Presence via discord-game-sdk4j
                                       (pure-Java IPC, bundled jar-in-jar); lazy connect
                                       on first join, APP_ID = 0 disables it entirely
```

## Shared seams

Five small classes exist only so the same decision cannot be made twice. Reach for them before
retyping what they hold.

- **`hud/HudGate.blocked(client, settings)`** — the four refusals every overlay's render gate opens
  with: no player, `hud.isHidden()`, `HudLayout.editing()`, master switch off. The editing term is
  the one that matters and the easy one to leave out — an overlay that forgets it draws its live
  self underneath the editor's preview, which is the one way to break the layout editor. A new
  overlay calls `HudGate.blocked` rather than retyping the chain; whatever else it needs (its own
  `show*` toggle, whether the server has sent data, the plate superseding it) stays in the overlay,
  because no two of those agree.

  Two overlays deliberately do more with the gate than return early:
  `overlay/BeyonderHealthOverlay` turns it into a **fall-through** — it replaced a vanilla element,
  so `blocked` means "draw the hearts", not "draw nothing", and its `render` returns a boolean the
  wrapper uses to call the original (it also hands the frame back in creative and spectator rather
  than re-deriving vanilla's rule). `overlay/MadnessOverlay` passes the gate and *then* draws
  `MadnessCorruption.screenEffects` **before** the `showCharacterPlate` check, because the stage
  vignette is the world reacting to the player, not a readout — the plate supersedes the bar, never
  the effects.

- **`dev.ua.ikeepcalm.coi.CoiLog`** — the mod's one logger (`CoiLog.LOG`, named "COI Client"). Every
  hand-written `System.out.println("COI Client: …")` is gone; the prefix is now the logger's name,
  so it cannot drift between call sites, and the lines land with a level and a timestamp like every
  other mod's.

- **`network/payload/CoiPayloads`** — the shape every payload record is built from: the `coi-client`
  namespace, the `MAX_DOCUMENT` (1 MiB) and `MAX_ACTION` (32 KiB) caps, and the read/write pair.
  Both ends have to agree on a cap or the packet is rejected mid-flight, so it is stated once here
  rather than 22 times. A payload class is then its record components, its id and its codec — which
  is what makes a mismatch with the plugin visible at a glance.

- **`util/JsonRead`** — the defensive scalar reads (`string`, `intOf`, `dbl`, `bool`, `object`, …)
  that every `domain/beyonder/model/` class shares. Each answers "absent" with the caller's default instead of
  throwing, so a `handle` method only guards the two things that genuinely are fatal: a body that is
  not an object, and a value of the wrong Java type. `domain/menu/service/MenuJson` is the same idea for menu
  documents, with the length clamps a document needs.

- **`ui/CoiStyle`** — besides the dark/gold palette and the card chrome, it now names the screen
  dimming ladder: **`BACKDROP`** for a modal that owns the screen, **`SCRIM`** for one the player is
  expected to glance past, **`VEIL`** for the layout editor, where the world has to stay readable
  while it is dragged on. Choosing between them is a judgement about the screen behind, not a taste
  in alpha, which is why they are named rather than typed as hex at the call site. It also carries
  **two** width rules: `cardWidth` for a scrolling content card (the sheet and the menus, which are
  the same object to a player) and `formWidth` for a settings-style column of labelled rows (the
  binding screen, HUD settings). `formWidth` is deliberately **not** `cardWidth` — a form stops
  being readable long before a document does, so it takes a wider gutter and caps far lower, and
  keeping the rules apart is what stops a later widening of the reading card from stretching the
  forms with it.

**The big screens are shells over collaborators.** `MenuScreen`, `CharacterSheetScreen`,
`AbilityPickerOverlay`, `HudSettingsScreen`, `HudLayoutScreen`, `SpellImpactEffect` and
`HudElements` each kept their public surface and their behaviour, and handed the drawing out:

| Shell                                    | Keeps                                                                             | Hands out                                                                                                                                                                     |
|------------------------------------------|-----------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `screen/menu/MenuScreen`                 | the vanilla `Screen` lifecycle, the session's `__close` guard, scroll, `approach` | `MenuPartFactory` → `MenuPart`s; `Menu{Text,Value,Control,Collection}Parts` draw them; `MenuChrome`/`MenuGauges`/`MenuIcons`/`MenuScrollbar`/`MenuConfirmModal`/`MenuMetrics` |
| `screen/sheet/CharacterSheetScreen`      | the `sheet_open`/`sheet_close` lifecycle, the card, the scroll                    | one `Sheet*` class per section, all handed a `SheetContext`; `SheetMetrics`/`SheetPalette`                                                                                    |
| `screen/ability/AbilityPickerOverlay`    | the modal, the search box, the keyboard                                           | `PickerModel` (rows), `PickerPainter` (paint), `PickerMetrics`, `PickerLabels`                                                                                                |
| `screen/settings/HudSettingsScreen`      | the tabs, the working copy, Done/Cancel                                           | `SettingsTabs` (which rows), `SettingsRows` (how a row is built)                                                                                                              |
| `screen/settings/HudLayoutScreen`        | selection, drag, keys                                                             | `LayoutPainter` (what is drawn over the world), `LayoutSnap` (where it lands)                                                                                                 |
| `domain/effect/visual/SpellImpactEffect` | the `VisualEffect` contract and the params                                        | `ImpactStyle`, `ImpactGeometry`, `ImpactRenderer`, `WorldImpact`                                                                                                              |
| `hud/layout/HudElements`                 | `all` / `byId` / `soloSet` / `moveGroupBy`                                        | one `BarElements.*Element` / `SlotElement` descriptor per element; `LayoutGeometry`, `LayoutGroups`                                                                           |

The seam is always the same: the shell owns *state and lifecycle*, the collaborators own *pixels and
arithmetic* and are handed everything they need. So a rendering change is a change in one
collaborator, and none of the invariants recorded below moved with the code.

## Network Protocol

Full reference: **[docs/NETWORK_PROTOCOL.md](docs/NETWORK_PROTOCOL.md)**

Summary (protocol 2):

- C→S `coi-client:use` — activate ability by id + action (`execute`/`left_click`)
- C→S `coi-client:request` — request available abilities list
- C→S `coi-client:hello` — capability handshake, sent on JOIN **before** `request`
- C→S `coi-client:action` — `{"action":"open_menu"}` (gated on `menu_action`), plus the character
  sheet's `sheet_open`, `sheet_close`, `{"action":"open","target":…,"ui":…}` and `toggle_terrain`
- C→S `coi-client:menu_action` — a click inside a menu document: `{session, version, action, value?}`
- S→C `coi-client:abilities` — pipe/semicolon delimited ability data
- S→C `coi-client:cooldown` — ability id + ticks
- S→C `coi-client:effect` — trigger/stop a visual effect; batch 7 adds `postfx`, `sky_tint`,
  `shake`, `letterbox`, `orbit` and `bed` on the same channel (docs/ASCENSION_CEREMONY.md)
- S→C `coi-client:mythical` — transform a player into a pathway form (see Mythical Creature Forms)
- S→C `coi-client:conditions` — beyonder state (madness, spirituality, …) for the local player
- S→C `coi-client:appearance` — appearance traits per player UUID
- S→C `coi-client:server` — plugin version + server feature list (reply to `hello`)
- S→C `coi-client:abilities_v2` — rich ability list as JSON (description, cost, cooldown, lock
  state, icon model, toggle state, passives included); replaces `abilities` for clients that
  advertise `ability_meta`
- S→C `coi-client:state` — one ability's toggle or category change
- S→C `coi-client:acting` — acting progress, method cooldown, overflow, last grant
- S→C `coi-client:resource` — one ability resource meter (id, label, current/max, colour, ttl,
  percent/value format); `{"id":…,"remove":true}` takes it off the HUD
- S→C `coi-client:actionbar` — COI's sorted action-bar entries (channel, key, priority, ttl,
  text component); replaces the vanilla action bar for clients that advertise `action_bar`
- S→C `coi-client:target` — one packet per ability hit (name, before/after fraction, HP, damage)
- S→C `coi-client:cogitation` — start / prompt / fail / stop for a cogitation session
- S→C `coi-client:notify` — a toast (kind, title, body, colour, duration)
- S→C `coi-client:sheet` — the whole character sheet as JSON; pushed on `sheet_open`, after
  `toggle_terrain`, and every 60 ticks while the sheet is open
- S→C `coi-client:menu` — a whole server-authored screen as a JSON document; `{"closed":true}`
  takes it away again

Ability wire format (v1): `id|localizedName|englishName|category|hasLeftClick` per entry, `;`
separated. In-memory format: `"id - englishName"`.

Client feature ids (`ClientFeatures.SUPPORTED`): `ability_hud`, `hotkeys`, `effects`, `appearance`,
`mythical`, `conditions`, `spirituality_hud`, `menu_action`, `ability_meta`, `ability_state`,
`acting_hud`, `action_bar`, `target_health`, `cogitation`, `notify`, `character_sheet`,
`resource_bar`, `menu_ui`, `menu_specimen`, `menu_archive`, `ability_categories`,
`ability_manual`, `ceremony`.

**Ability icons** — `ui/AbilityIcons.draw` prefers the resource pack's per-ability item model (`AbilityInfo.icon()`,
rendered as a glowstone-dust stack with `DataComponents.ITEM_MODEL` set, posed
to the box size) and falls back to the bundled `textures/icons/<category>/<tier>.png`, with the
category whitelisted against the 14 shipped folders. `ui/IconModels` decides which, by looking for
`<ns>:items/<path>.json` in the resource manager — cached per icon id, cleared on resource reload
and on disconnect.

**Capability gating** — `ClientFeatures` lists what this client renders; `ServerCapabilities` holds
the server's reply and is reset on disconnect. A server that never replies leaves every
`ServerCapabilities.has(...)` false, so client-only UI stays hidden and the legacy server paths (boss bars, slot-9
shortcut item) keep working unchanged.

## Visual Effects System

Full reference + server integration guide: **[docs/VISUAL_EFFECTS.md](docs/VISUAL_EFFECTS.md)**

Effects are triggered server-side via `VisualEffectPayload(effectId, params)`.
`EffectManager` maintains the active list and renders all effects via `HudRenderCallback`.
Effects support `params = "stop"` to remove, `effectId = "all"` to clear all.

Available effects: `vignette`, `heartbeat`, `cracks`, `eyes`, `glitch`, `bloodrain`, `frost`, `whispers`, `tunnel`,
`flash`, `impact`, `impact_frame`, `hallucination`, plus the six ceremony ids below.

A stop arrives in **two** shapes and both are honoured: the bare `stop`, and `stop,fade=1500` —
which is why `VisualEffect.stop(String)` exists beside `stop()`. `CeremonyParams.isStop` is the
one test for both.

**Sound layer** — `EffectSounds` plays audio companions for effects (loops for `heartbeat`/`whispers`/`tunnel`, one-shots for `cracks`/`frost`/`glitch`); assets in `assets/coi-client/sounds/` + `sounds.json`. Volume via `effectSoundVolume` HUD setting.

**Madness hallucinations** — `HallucinationManager` (client tick) fires phantom positional sounds and visual flickers once `BeyonderState` madness ≥ 25, scaling with stages 25/50/75; darkness/night makes events up to ~2.5x more frequent. Server can force one via the `hallucination` pseudo-effect (`event=footsteps|whisper|cave|block|flicker|random`). Toggle: `enableHallucinations` HUD setting, which also gates:
- **HUD gaslighting** (`hud/HudGaslight`) — at madness ≥ 75 the HUD briefly lies: wrong cooldown numbers, glitched keybind glyphs, two slots trading places.
- **Title screen haunting** (`screen/TitleScreenHaunt` + `TitleScreenMixin`) — corruption (max of madness at disconnect
  and permanent madness, incl. debug-screen values) is persisted to `config/coi_client_state.json` (`ClientStateStore`);
  the main menu shows a scaled vignette, occasional eye apparitions, and whisper splash lines
  (`title.coi.haunt_splash.*`). The same figure drives the title screen takeover's corruption (docs/TITLE_SCREEN.md),
  and the haunt still draws last, over whichever menu is underneath. Clean players always get LOTM flavor splashes
  (`title.coi.splash.*`) — not gated by the hallucinations toggle.

**Debug screen** (dev environment only, F8): lists all registered effects with Test/Stop buttons and a params input
field. `shouldPause()` returns false so effects are visible while the screen is open. The list is **paged**,
`EFFECTS_PER_PAGE` at a time — every protocol batch has added effects to it, and the ones past the window's bottom edge
could not be clicked at all; paging makes the screen a constant height however long the registry grows.

## Mythical Creature Forms (`domain/form/`)

S→C `coi-client:mythical` (`MythicalFormPayload`, `targetUuid` + `pathway:<unused>:start|stop`) marks
a player's UUID as transformed into a pathway's form. Two kinds:

- **Full forms** — the vanilla player render is cancelled outright (`LivingEntityRendererMixin` at HEAD)
  and replaced with procedural geometry drawn from `FormPrimitives`. 19 of the 20 pathways.
- **Partial forms** — a baked Blockbench model stands in for the *lower body* while the player's own
  head/torso/arms keep rendering. Currently Visionary only (`FormModelLayers.VISIONARY_LOWER_SPEC`).

Partial forms are assembled from four pieces that all have to agree:

| Piece | Job |
|-------|-----|
| `PlayerModelMixin` (`setupAnim` TAIL) | hides leg parts; must run in `setupAnim`, since submission is deferred |
| `HumanoidArmorLayerMixin` | hides leggings/boots, which draw from their own model set |
| `LivingEntityRendererMixin` | `hipRaise` push (world space, at HEAD) + **carrier transform** push (model space) |
| `PartialFormLayer` | draws the baked model, undoing the carrier transform it inherits |

**The carrier transform** is what makes the halves read as one body. The rig's torso bone (its
"carrier") both rotates and translates during the walk cycle, around a pivot that is over a block
away from the player's waist. `FormModel#carrierDelta` hands out that bone's full rigid motion,
`PartialForms#carrierTransform` converts it into player space, and the renderer mixin pushes it onto
the pose stack just before the model is submitted — so the player *and* every layer above it (armor,
held items, cape, appearance traits) ride the torso exactly. Copying the rotation angle alone is not
enough and looks like shearing: same tilt, wrong pivot, no translation.

Placement knobs live in `FormModelLayers`; read the comment there before touching one. `hipRaise` and
the carrier push sit in different coordinate spaces on purpose — see `LivingEntityRendererMixin`.

`domain/form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are **Blockbench exports**. They are
generated files: change the rig in the modelling tool and re-export, never hand-edit the Java. A
hand edit is invisible until the next export silently reverts it.

**Dev testing** (no server needed): F8 → *Form: None (Click to cycle)* applies a form to yourself.

## Server-authored Menus (batch 6)

Full reference, both repos in one place: **[docs/MENU_SYSTEM.md](docs/MENU_SYSTEM.md)** — the wire
contract, the server's builder DSL, every ported screen, everything that still opens a chest GUI,
how to add a menu, and the constraints the visual redesign kept.

**Vocabulary v2** added five component types — `hero`, `details`, `steps`, `chips`, `panels` — plus
an `icon` field on `kv` rows / `checklist` items / `stat` / `note` / `toggle` / section headings, a
tri-state checklist (`ok` / `no` / **`pending`**), `stat` `style`/`cap`/`delta`, section
`badge`/`collapsed`, list-row `fraction`/`meta`, `grid` `size`, and a labelled `divider`. Purely
additive: **the protocol stays 2 and the feature id stays `menu_ui`** — the capability list
negotiates this, not a version bump.

The reason it exists: the renderer could attach an icon to exactly three things (document header,
button, list/grid row), so an adapter that wanted to *explain* something had one tool — a muted grey
paragraph. Five of the seven menu families therefore carried **zero** component-level icons and
~28,000 characters of prose, 52 blocks of it over 140 characters. After the rewrite there are **zero standalone `text`
components in any adapter**; every paragraph lives inside a `details`
disclosure, collapsed by default. The information was never the problem — showing it unasked was.

Two client-side rules the renderer must keep: **disclosure state (`details` open/closed, collapsed
sections) is keyed `screenId + "/" + id` and survives a rebuild**, resetting only when the screen id
changes — a 60-tick server refresh slamming shut what the player just opened is the bug to avoid;
and **all easing funnels through `MenuContext.approach`**, which returns the target outright under
`epilepsyMode`, so one chokepoint honours the setting.

**The back arrow returns to the character sheet.** Every root document sets `back(true)` but is
opened with a reset stack, so `__back` at the root used to reach `session.pop() == null` and close
the menu outright. The server now answers that case with `{"closed":true,"back":true}`; the client
reopens `CharacterSheetScreen` when the flag is set **and** the session came from the sheet **and**
the server advertises `character_sheet`. Esc and the X still close outright — only the back arrow
goes back. The flag is read off the raw JSON in `handleMenu`, deliberately not through `MenuParser`,
because it describes the *transition* and a closed document has no screen to describe.

The plugin's ~217 InvUI chest GUIs cannot each become a Java class here — `ChurchGUI` alone is
2833 lines and ~30 screens, and every server-side menu change would need a client release. So the
server ships a **document** describing one screen and the client renders it: `domain/menu/` parses
it, `screen/menu/MenuScreen` draws it, and every click goes back on `coi-client:menu_action` for the
server to answer with the next document. The plugin keeps owning navigation, gating and side
effects — exactly where that logic already lives. A document is rendered, never interpreted: the
`screen` id is only used to tell "the same screen refreshed" (scroll and typed text survive) from a
new one.

- **Forward compatibility is the parser's whole job.** `MenuParser` never throws: an unknown
  component `type` is skipped, a wrong JSON type reads as absent, sizes are clamped. A newer plugin
  must degrade to a screen missing one row, never to no screen — which is why `MenuComponent` is
  sealed on *this* side but the wire is not.
- **`enabled` defaults to true, and a disabled control is drawn with its `disabledReason` as a
  tooltip.** A dead button that will not say why is the thing the chest GUIs did worst.
- **`confirm` is client-side.** A button carrying one raises a modal and sends nothing until the
  player agrees, replacing the plugin's two-step chest confirms.
- **One card, one scrollbar.** A `list` flattens its rows (and its search box) into the outer scroll
  rather than nesting one, so `maxVisible` is advisory. Rows are laid out in pixels because no two
  component types are the same height — the same reason `AbilityPickerOverlay` works that way.
- `__close` is sent **exactly once**, however the screen goes away, and never in answer to the
  server's own `{"closed":true}`. Every send is guarded by `canSend`, like the character sheet's.
- Text fields outlive a rebuild (`fields` is keyed by component id): the search box re-lays out the
  whole card on every keystroke, and recreating the `EditBox` would drop the caret mid-word.
- **`useServerMenus`** is always offered and always sent. It used to be suppressed — checkbox hidden,
  `ui` forced to `client` — whenever the server advertised `menu_archive`, i.e. always, so the
  control did nothing. Now the ask reaches the server, which resolves it against its own per-player
  preference (`/coi menu native`). The sheet's Pathways button opens the native multi-pathway
  chooser; unsupported flows still retain their server fallback.
- **Archive templates** (`ledger`, `relic`, `inscription`, `atlas`, `challenge`) keep all document
  sections and actions, adding a section index and adaptive folio layout. `ability_manual` moves
  represented ability rows into its details; Other actions preserves every remaining control.
  See `docs/ARCHIVE_PRESENTATION.md` and `docs/ABILITY_MANUAL.md` for the current presentation contract.
- **Portrait scenes** use the authoritative sheet pathway and sequence. `PortraitPose` is attached
  to an isolated avatar render state and reset during ordinary extraction; never mutate the live
  player to pose a menu model. Reduced-effects mode freezes scene motion.
- Dev testing: F8 → *Menu* feeds a sample document covering every component type through the real
  parser, so the renderer can be judged with no server attached.

## Key Patterns

- **Static singletons, one per concern** — `CoiClient` is the initializer and nothing else. The
  ability catalogue is `domain/ability/service/AbilityRegistry`, the slot bindings
  `domain/ability/service/AbilityBindings`, the
  pathway identity `domain/ability/model/Pathways`, the keymappings `input/CoiKeyBindings`, the wire
  `network/CoiNetworking`, and each S→C channel's data one class under `domain/beyonder/model/`. Screens and widgets
  still reach all of them through static methods — what changed is which class answers, not how.
- **Real-time cooldowns** — tracked via `System.currentTimeMillis()`, not ticks, for smooth animation.
- **Lazy effect geometry** — `CracksEffect` generates crack segments on first render (needs screen dimensions); seeded by `startTime` for consistent patterns.
- **Dev-only keybindings** — `effectDebugMenu` (F8) is only registered when `FabricLoader.isDevelopmentEnvironment()`.

## Keybindings

| Key             | Action                                                                                                                                                                                                                                                                                                                                  |
|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Z–N (6 keys)    | Ability slots 1–6 (each slot is placed on its own in the layout editor)                                                                                                                                                                                                                                                                 |
| *(unbound)*     | Ability slots 7–10 — assign in vanilla Controls, activate via `activeAbilitySlots`                                                                                                                                                                                                                                                      |
| G (hold)        | Ability wheel — radial picker, open while the key is held, `wheelSlots` slots                                                                                                                                                                                                                                                           |
| K               | Open Ability Binding screen                                                                                                                                                                                                                                                                                                             |
| M               | With `useServerMenus`, the plugin's chest GUI straight away (`menu_action`, no longer excluded on `menu_archive` servers); else the character sheet (`character_sheet`), else the server Beyonder menu, else an unsupported message. The preference only *reorders* the first two — a server without `menu_action` still gets the sheet |
| *(unbound × 9)* | Open one of the sheet's destinations directly (`church`, `abilities`, `mythical`, `uniqueness`, `honorific`, `map`, `seat`, `throne`, `pantheon`) — `ActionPayload.ofOpen`, no sheet behind it, so the menu's back arrow means "close"                                                                                                  |
| Left Alt (hold) | Gesture casting — draw a shape, release to cast (only when a gesture is bound)                                                                                                                                                                                                                                                          |
| F8 *(dev only)* | Open Effect Debug screen                                                                                                                                                                                                                                                                                                                |

## Ability Pathway Colors

Extracted from first segment of ability ID (before first `-`):
`fool`=purple, `door`=blue, `sun`=yellow, `tyrant`=cyan, `demoness`=red, `priest`=orange

The table itself is `domain/ability/model/Pathways.pathwayRgb` — **one** map for all 25 pathways, shared by the
HUD slots, the picker, the acting bar, the plate, the sheet and `MythicalFormManager`.
`AbilityInfo.pathwayColor`/`pathwayColorByName` are thin ARGB wrappers over it, kept because callers
that only hold an ability id read better that way.

## Config Files

`config/coi_abilities.json` — bound ability ids per key slot (`abilityN`), wheel slot (`wheelN`), and gesture (`gesture_<id>`)
`config/coi_hud.json` — HUD settings (position/size/scale, display toggles, epilepsy mode, madness bar, spirituality bar
(`showSpiritualityBar`, `spiritualityAnchor`, `spiritualityXOffset`, `spiritualityYOffset`, `spiritualityHideWhenFull`),
acting bar (`showActingBar`, `actingAnchor`, `actingXOffset`, `actingYOffset`), resource bars (`showResourceBars`,
`resourceAnchor`, `resourceXOffset`, `resourceYOffset`, `resourceMaxBars`), overlays (`showActionBar`,
`actionBarXOffset`, `actionBarYOffset`, `actionBarLines`, `showTargetHealth`, `targetHealthXOffset`,
`targetHealthYOffset`, `showCogitationOverlay`, `cogitationXOffset`, `cogitationYOffset`, `showNotifications`,
`notificationXOffset`, `notificationYOffset`), `slotPlacements` (a 10-entry array, each entry `null` for "stay in the
row" or
`{"anchor":"TOP_LEFT","x":0,"y":0}`), the character plate (`showCharacterPlate`, `characterPlateAnchor`,
`characterPlateXOffset`, `characterPlateYOffset`, `characterPlateScale`, `characterPlateOpacity`), the health element (`showBeyonderHealth`,
`beyonderHealthStyle` — `HEARTS` | `BAR` | `ORNATE` | `PIPS`, default `HEARTS` —
`beyonderHealthAnchor`, `beyonderHealthXOffset`, `beyonderHealthYOffset`, `beyonderHealthScale`),
`showAbilityHud`, `slotSize` (the
ability slots' only size knob, 20–80) plus the nine per-element scales (`madnessScale`, `spiritualityScale`,
`actingScale`, `resourceScale`, `actionBarScale`, `targetHealthScale`, `cogitationScale`, `notificationScale` — all
`1.0`, clamped 0.5–2.0), `layoutVersion`, `effectSoundVolume`, `enableHallucinations`, `activeAbilitySlots`,
`wheelSlots`, `enableDiscordPresence`, `presenceShowMadness`, `useServerMenus`, `coiTitleScreen`,
the ascension ceremony's three comfort settings (`enableCameraShake`, `enableCinematicCamera`,
`ceremonyFogDensity` — see docs/ASCENSION_CEREMONY.md))
`config/coi_client_state.json` — persistent state, not preferences (`ClientStateStore`): last madness values for title
haunting, `lastPathway` for the emblem the title wheel lights, `tourCompleted` for the first-join tour,
`inventoryHintDismissed` for the shortcut-item hint (all survive HUD config resets)

## Localization

`src/main/resources/assets/coi-client/lang/en_us.json` + `uk_ua.json`
Key format: `key.coi.*`, `screen.coi.*`, `notification.coi.*`

## Known gaps

Things that are wrong on purpose, or wrong and known. Written down so the next reader does not have
to rediscover them.

- **`HudConfig.load()` still NPEs on a truncated or empty `coi_hud.json`.** `GSON.fromJson` returns
  `null` for an empty or whitespace-only document, `HudConfigIo.read` calls `json.has(...)`
  straight away, and the `try` only catches `IOException` — so a config file cut short by a crash
  during a save takes the mod's init down instead of falling back to defaults. Pre-existing;
  deliberately **not** fixed during a no-behaviour-change refactor, because the fix is a behaviour
  change (silently resetting a corrupt config) that wants its own decision.
- **`domain/form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are generated.** They are
  Blockbench exports and must be regenerated from the modelling tool, never hand-edited — see
  *Mythical Creature Forms*.
- **The vanilla `HEALTH_BAR` height provider still reports heart rows**, so absorption pushes the
  armour/air bars up and leaves a gap above our one-row bar — see docs/HUD.md.
- **`postfx` intensity is quantised to four steps, and `wobble` does not move.** Both follow from
  the same fact: a post pass is handed no clock and no settable uniform, so a continuous intensity
  or an animated distortion would each mean one compiled chain per value. Wrong on purpose — see
  docs/ASCENSION_CEREMONY.md.
- **`coi-client:bed` is implemented and unused.** The twenty-two `mysterria:ascension.*` tracks it
  names do not exist in the resource pack yet and no scene sends it; a `track` that resolves to
  nothing plays silence. The server also still exempts only the exact string `stop` from its
  visual-immunity suppression, so `stop,fade=N` would be swallowed — that is a plugin-side fix,
  and it is why this is listed here rather than called done.

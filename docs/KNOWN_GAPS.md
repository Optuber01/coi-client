# Known gaps

Things that are wrong on purpose, or wrong and known. Written down so the next reader does not have
to rediscover them.

- **`HudConfig.load()` still NPEs on a truncated or empty `coi_hud.json`.** `GSON.fromJson` returns
  `null` for an empty or whitespace-only document, `HudConfigIo.read` calls `json.has(...)`
  straight away, and the `try` only catches `IOException` — so a config file cut short by a crash
  during a save takes the mod's init down instead of falling back to defaults. Pre-existing;
  deliberately **not** fixed during a no-behaviour-change refactor, because the fix is a behaviour
  change (silently resetting a corrupt config) that wants its own decision.
- **`domain/form/model/VisionaryLowerModel` and `VisionaryLowerAnimations` are generated.** They are
  Blockbench exports and must be regenerated from the modelling tool, never hand-edited — see *Mythical Creature Forms*.
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

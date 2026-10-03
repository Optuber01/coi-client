# The Ascension Ceremony (protocol batch 7)

The six set-piece effects a Sequence 0 ascension plays over a witness.

## The Ascension Ceremony (batch 7)

A Sequence 0 ascension is a per-pathway **scene** at the rite and a per-pathway **aftermath** over
the whole server afterwards. Nearly all of it is block overlays and display entities the mod never
hears about. Six things are not drawable that way, and `domain/ceremony/` is those six — **one new feature
id, six new effect ids on the existing `coi-client:effect` channel, no new channel and no protocol
bump**. A client that implements none of it still sees the rite, just a plainer one.

| Effect      | What it does                                                                                                                              | Comfort switch                      |
|-------------|-------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|
| `postfx`    | `name` + `intensity` + `duration`: a real post-processing pass over the finished world. The one effect no amount of particles can imitate | —                                   |
| `sky_tint`  | `sky` / `fog` hex, `density`, `duration`: the colour and the thickness of the world's air                                                 | `ceremonyFogDensity`                |
| `letterbox` | `intensity` + `duration`: cinema bars, so a witness can tell a rite from a fight                                                          | —                                   |
| `shake`     | `amplitude` + `hz` + `duration`: the ground moving                                                                                        | `enableCameraShake`, `epilepsyMode` |
| `orbit`     | `x`/`y`/`z` + `radius` + `height` + `duration`: the camera swinging around the peak                                                       | `enableCinematicCamera`             |
| `bed`       | `track` + `volume` + `fade`: a crossfaded score bed. Specified; nothing sends it yet                                                      | music volume                        |

**An effect is a handle; the state outlives it.** The server re-sends an effect to replace one
already running — aftermaths re-send their tint every five to ten seconds, both to outlive their
own duration and to catch players who joined mid-aftermath — and `EffectManager.trigger` answers a
re-send by throwing the old instance away. So everything that has to survive one (a fade in flight,
the tint being walked toward, the arc already half travelled) lives in a `*State` class, and the
`CeremonyEffect` is a parser whose `isFinished()` asks the state. Two consequences worth keeping:
**re-sending the tint already on screen must move nothing** — the states hold a *target* and walk
toward it rather than starting a fade — and an effect stays registered until its fade-out has
actually finished, not until its duration has.

**Every state is a pure function of the wall clock.** Nothing here is ticked: the renderer reads
them from a mixin, often several times a frame, and a second read in the same millisecond moves
nothing. That is what lets `FogRendererMixin` and `SkyRendererMixin` both advance `SkyTintState`
without either owning it.

**Four clears, and they are not the same clear.**

- `postfx name=none`, `sky_tint density=0` and `letterbox intensity=0` are the server's own
  teardown, sent on every exit path including an abort. Each **ramps out** — `density=0` in
  particular has to put the vanilla sky back exactly, not leave very thin fog behind.
- `stopOrbit` is sent for every witness in teardown and hands the camera back over 300 ms.
- `effectId = "all"` reaches `CeremonyEffects.reset()` through `EffectManager.stopAll`, because
  clearing the active list is not enough for effects the *renderer* reads rather than draws.
- A **dimension change or a disconnect** resets everything outright. Every clear above travels the
  ordinary suppressible path, so a player who becomes visually immune between an effect and its
  clear would otherwise keep it with nothing able to reach them; walking through a portal can.

### `postfx`: why the intensity is quantised

A post chain bakes its uniform values when it loads. Post passes are handed only `SamplerInfo` and
their own config block — no clock, no settable float — so an intensity cannot be dialled into one
chain at runtime. Each of the five names therefore ships at **four graded strengths** under
`assets/coi-client/post_effect/ceremony/<name>_25|50|75|100.json`, and `PostFx.resolve` picks
the nearest; the ramp either side of a pass steps down through them, which is three visible steps
and still far better than a cut.

The five fragment shaders are the mod's own, in `assets/coi-client/shaders/post/ceremony_*.fsh`,
because **vanilla 26.2 ships only `invert`, `blur`, `creeper`, `spider`, `transparency` and
`entity_outline`** — the old `desaturate` / `wobble` / `sobel` / `bits` chains are long gone, and
`entity_sobel.fsh` differentiates *alpha*, which is 1 everywhere on the main target. Each of ours
is a straight `mix(original, processed, Amount)`, so a name behaves the same way at every step.

`ceremony_wobble.fsh` is **deliberately static**, and that is the one place this falls short of the
brief: with no clock in a post pass, an animated wobble would mean one compiled chain per phase. A
frozen distortion at the right amplitude still reads as "the air is wrong", and the scene around it
is moving.

`name=none` is the clear, not a pass called none — and an unknown name folds onto it, because
leaving the previous pass stuck is the one failure a witness cannot get out of.

`GameRendererMixin` runs the chain **after `doEntityOutline`, before the GUI** — vanilla's own slot
for the creeper and spider views. It deliberately does not write `GameRenderer.postEffectId`: that
field is a single slot vanilla owns and re-asserts from `checkEntityPostEffect` whenever the camera
entity changes, and running our chain beside it costs one `process` call and leaves both free.

### The camera moves are view-only

`CameraMixin` injects at the **end of `alignWithEntity`**, not at the end of `update`. The camera
has taken the player's eye position and angles by then, and `update` goes on to build the cull
frustum and the projection *from them* — so a change made later would leave the frustum describing
a camera that is no longer there. A three-degree shake survives that; a seven-block orbit does not,
and would cull the chunks out of a shot pointed straight at them.

Neither move touches the player entity. The camera is a render-side object; the player's real
position and aim stay where the server last agreed they were, which is the only reason a cinematic
can play at all without tripping a movement check. The orbit is **offered, never imposed** in three
ways: it blends out of the player's own eyes and back into them so there is no cut at either end,
any movement key ends it through `CeremonyEffects.tick`, and `enableCinematicCamera` turns it off.

One thing the orbit must keep: the arc's **start angle is captured on its first frame**, from where
the player actually stands, so the camera opens from behind them rather than from an arbitrary
compass point.

### `sky_tint` is two mixins

Recolouring the fog alone turns the horizon and leaves the dome overhead the colour it always was,
which reads as a rendering fault rather than as a sky — so `FogRendererMixin` (the colour **and**
the four fog planes, off the mutable `FogData` that `setupFog` returns) and `SkyRendererMixin` (the
sky disc) are a pair, and a change to one wants the other.

Only the **distances** answer to `ceremonyFogDensity`; the colour rides the full tint. Fog is the
one parameter here with a gameplay cost — it genuinely shortens sight lines, which is why the
Darkness aftermath caps itself at 0.45 server-side — and a player who scales it down should still
get a rite that looks like a rite.

### Testing

F8 → the six `Rite: …` rows, on the second page of the effect list. `orbit` defaults its centre to
the player, so the shot can be watched with no server attached. Server-side:
`/coi ascension preview <pathway>` and `/coi ascension aftermath <pathway>` — both make real
vicinity noise, so run them away from spawn on a test server.

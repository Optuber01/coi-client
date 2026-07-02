# Visual Effects — Server Integration Reference

The client renders screen-space visual effects triggered by the server via the `coi-client:effect` S→C payload.

## Payload Structure

```
String effectId   — effect name (see table below), or "all"
String params     — comma-separated key=value overrides, or "stop"
```

### Stopping effects
```
effectId = "<id>",  params = "stop"   → stop one specific effect
effectId = "all",   params = "stop"   → stop every active effect immediately
```

Triggering an effect that is already active replaces it (restarts from scratch).

---

## Effect Reference

### `vignette`
Darkens screen edges. Good for: cursed zones, low sanity, death proximity.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Darkness and width of the vignette |
| `duration` | long ms | `-1` | How long to show. `-1` = persistent until stopped |

**Examples:**
```
effect("vignette", "intensity=0.5,duration=10000")   // mild, 10 seconds
effect("vignette", "intensity=1.0")                  // maximum, persistent
effect("vignette", "stop")                           // remove
```

---

### `heartbeat`
Vignette that pulses with a realistic lub-DUB rhythm. Good for: high tension, near-death, possessed states.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.85` | Peak darkness per beat |
| `bpm` | float | `75` | Beats per minute |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("heartbeat", "intensity=0.8,bpm=90,duration=15000")   // fast, anxious
effect("heartbeat", "intensity=1.0,bpm=50")                  // slow, ominous
```

---

### `cracks`
Branching fracture lines grow from screen corners toward center. At high intensity the cracks pulse red. Good for: reality breaking, max madness, catastrophic events.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | How far cracks extend toward center |
| `pulse` | boolean | `false` | If true, cracks glow red rhythmically |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("cracks", "intensity=0.5")                          // subtle corner cracks
effect("cracks", "intensity=1.0,pulse=true")               // max — red pulsing cracks
effect("cracks", "intensity=0.8,pulse=true,duration=8000") // timed
```

**Note:** Cracks are generated once on first render, seeded by trigger time — each trigger produces a unique pattern.

---

### `eyes`
Creepy eyes open from the darkness of the screen, stare, then close. Eyes are distributed evenly across the screen in a grid (up to 3 columns). Good for: being watched, high madness, cursed locations.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `count` | int | `2` | Number of eyes (1–6 recommended) |
| `duration` | long ms | `8000` | Total lifetime including open/close animation |

**Timing breakdown** (per eye, slightly staggered):
- 0–500ms: ambient dark glow fades in
- 500–2000ms: eyelids open
- 2000–(duration−2500)ms: staring
- last 2000ms: eyelids close + fade out

**Examples:**
```
effect("eyes", "count=1,duration=6000")    // single eye, quick
effect("eyes", "count=4,duration=12000")   // four eyes across the screen
effect("eyes", "count=6")                  // six eyes, default 8s
```

---

### `glitch`
VHS-style horizontal distortion lines strobing in bursts. Lines flicker ~12×/second (not every frame). Good for: teleportation, reality corruption, spell side-effects, wrong-place warnings.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Number of lines, alpha, burst frequency |
| `duration` | long ms | `3000` | Auto-expires by default |

Line types (randomly mixed each burst):
- Dark horizontal scan band
- White flash band
- RGB chromatic fringe (red above / cyan below)
- Horizontal split block

**Examples:**
```
effect("glitch", "intensity=0.5,duration=2000")   // brief mild glitch on teleport
effect("glitch", "intensity=1.0,duration=5000")   // heavy sustained corruption
effect("glitch", "intensity=0.3,duration=500")    // quick flicker
```

---

---

### `bloodrain`
Dark red streaks fall down the screen. Each drop has a random speed and alpha. Good for: combat, rituals, curses, death proximity.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Number and density of drops |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("bloodrain", "intensity=0.5,duration=8000")   // light drizzle
effect("bloodrain", "intensity=1.0")                 // heavy, persistent
```

---

### `frost`
Ice crystals grow inward from screen edges with a blue-white tint. Combines icy gradient overlays with branching crystal lines. Good for: frozen spells, cold zones, ice pathway abilities.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Reach and brightness of frost |
| `duration` | long ms | `-1` | Persistent by default |

**Examples:**
```
effect("frost", "intensity=0.6,duration=10000")
effect("frost", "intensity=1.0")                    // heavy frost, persistent
```

---

### `whispers`
Cryptic text phrases fade in and out at random positions across the screen. Spawn rate scales with intensity. Good for: high madness, haunted locations, Fool/Door pathways, forbidden knowledge.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | Spawn frequency |
| `duration` | long ms | `-1` | Persistent by default |
| `text` | pipe-separated strings | *(built-in pool)* | Custom phrase pool |

Default text pool: "they can see you", "don't look back", "it knows your name", "run", "the door is open", "help me", "it's behind you", and several others.

**Examples:**
```
effect("whispers", "intensity=0.5")
effect("whispers", "intensity=0.9,text=run|almost|listen|wrong place")
effect("whispers", "duration=15000,text=the rite begins|do not resist")
```

---

### `tunnel`
Circular vignette that closes inward, leaving only a shrinking oval of visibility. Rendered via scanline fill (~80 draw calls per frame). Good for: exhaustion, confusion, extreme madness, near-death.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `intensity` | float 0–1 | `0.7` | How far the tunnel closes (1.0 = near-blackout) |
| `duration` | long ms | `6000` | Total lifetime |
| `closeDuration` | long ms | `2000` | Time to reach full intensity; also used for fade-out |

**Examples:**
```
effect("tunnel", "intensity=0.6,duration=8000")              // moderate, 8 seconds
effect("tunnel", "intensity=0.95,duration=5000,closeDuration=3000")  // dramatic slow close
effect("tunnel", "intensity=0.4,duration=3000,closeDuration=500")    // quick flutter
```

---

### `flash`
Instant full-screen color wash that rises quickly and fades out. 1 fill call per frame. Good for: ability activation feedback, pathway-colored casts, heals, explosions.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `color` | hex RGB (no `#`) | `FFFFFF` | Flash color |
| `intensity` | float 0–1 | `0.6` | Peak opacity |
| `duration` | long ms | `500` | Total fade time |

Pathway color reference: `8B00FF` (Fool/purple), `0055FF` (Door/blue), `FFDD00` (Sun/yellow), `00CCCC` (Tyrant/cyan), `FF2200` (Demoness/red), `FF8800` (Priest/orange).

**Examples:**
```
effect("flash", "color=FF2200,intensity=0.7,duration=400")   // Demoness red burst
effect("flash", "color=FFFFFF,intensity=0.9,duration=600")   // Priest holy light
effect("flash", "color=00CCCC,intensity=0.5,duration=300")   // Tyrant ability hit
effect("flash", "color=000000,intensity=0.8,duration=800")   // darkness flash
```

---

### `impact`
Client-rendered impact frame with configurable cinematic styles. It can draw a receiving-player screen impact frame, an optional in-world anchor, or both. Good for: spell hits, ultimates, parries, executions, divine reveals, void effects, and boss attacks.

This effect is blocked when the player's epilepsy mode setting is enabled.

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `style` | see presets below | `burst` | Visual preset |
| `scope` | `screen`, `world`, `both` | `both` | Whether to render the cinematic screen frame, world anchor, or both |
| `x` | double | crosshair/front of camera | World X coordinate |
| `y` | double | crosshair/front of camera | World Y coordinate |
| `z` | double | crosshair/front of camera | World Z coordinate |
| `color` | hex RGB (no `#`) | `FFFFFF` | Primary impact-frame geometry color |
| `accent` | hex RGB (no `#`) | `FF2200` | Ring/slash/accent geometry color |
| `intensity` | float 0-1 | `0.85` | Screen opacity, geometry opacity, and speed-line count |
| `radius` | float | `3.0` | Approximate world radius in blocks |
| `duration` | long ms | `320` | Total lifetime of the impact frame |
| `frames` | int 1-8 | `3` | Number of alternating color frames |

**Examples:**
```
effect("impact", "style=burst,scope=both,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=FF2200,intensity=0.9,radius=2.5,duration=260,frames=3") // heavy hit
effect("impact", "style=slash,scope=screen,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=FF2200,intensity=0.95,duration=220,frames=3") // sword/cut/parry
effect("impact", "style=holy,scope=both,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=FFD966,intensity=0.85,radius=3.0,duration=360,frames=3") // sun/priest reveal
effect("impact", "style=void,scope=both,x=120.5,y=64.0,z=-33.5,color=111111,accent=8B00FF,intensity=1.0,radius=4.0,duration=420,frames=4") // dark/fool finisher
effect("impact", "style=pierce,scope=screen,color=FFFFFF,accent=66CCFF,intensity=0.9,duration=180,frames=2") // beam/lance hit
effect("impact", "style=crush,scope=screen,color=FFFFFF,accent=555555,intensity=0.95,duration=280,frames=3") // gravity/blunt hit
effect("impact", "style=ripple,scope=both,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=00CCCC,intensity=0.75,radius=4.0,duration=460,frames=3") // spatial ripple
effect("impact", "style=fracture,scope=screen,color=FFFFFF,accent=FF2200,intensity=0.85,duration=360,frames=3") // reality crack
effect("impact", "style=blood,scope=screen,color=FFFFFF,accent=AA0000,intensity=0.9,duration=300,frames=3") // brutal/demoness hit
effect("impact", "style=frost,scope=screen,color=FFFFFF,accent=AADDFF,intensity=0.8,duration=340,frames=3") // ice/freeze hit
```

Presets:
- `burst`: general explosive hit, good default.
- `slash`: cutting/parry/execution frame.
- `void`: dark implosion or Fool/Door corruption.
- `holy`: Sun/Priest reveal or purification.
- `pierce`: beam, lance, bullet, or precise single-target hit.
- `crush`: gravity, blunt force, suppression, or stun.
- `ripple`: space-time distortion, teleport impact, barrier shock.
- `fracture`: reality crack, glass break, catastrophic backlash.
- `blood`: brutal hit, curse, sacrifice, Demoness-style impact.
- `frost`: freeze, ice magic, time-stop cold snap.

When tested from the debug screen without params, the client uses a longer `style=burst,scope=both` preview, closes the debug screen, and spawns the world anchor at the current crosshair hit position or about 4 blocks in front of the camera.

---

## Combining Effects

Effects are independent layers — multiple can be active simultaneously:

```java
// Madness level 3: vignette + heartbeat + cracks
sendEffect(player, "vignette",   "intensity=0.6");
sendEffect(player, "heartbeat",  "intensity=0.9,bpm=95");
sendEffect(player, "cracks",     "intensity=0.6,pulse=true");

// Teleportation: brief glitch
sendEffect(player, "glitch", "intensity=0.8,duration=1500");

// Big spell hit: impact frame plus a short pathway flash
sendEffect(player, "impact", "style=burst,scope=both,x=120.5,y=64.0,z=-33.5,color=FFFFFF,accent=00CCCC,intensity=0.9,radius=2.5,duration=260,frames=3");
sendEffect(player, "flash", "color=00CCCC,intensity=0.35,duration=450");

// Clear everything on respawn / sanity restore
sendEffect(player, "all", "stop");
```

---

## Paper Plugin Integration

Send effects using the standard plugin messaging API:

```java
// Helper — call from your Paper plugin
void sendEffect(Player player, String effectId, String params) {
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF(effectId);
    out.writeUTF(params);
    player.sendPluginMessage(plugin, "coi-client:effect", out.toByteArray());
}
```

Register the outgoing channel in `onEnable`:
```java
getServer().getMessenger().registerOutgoingPluginChannel(this, "coi-client:effect");
```

The client only processes this payload while connected (receiver is registered globally via `ClientPlayNetworking.registerGlobalReceiver`), so it is safe to send at any time after the player joins.

---

## Adding New Effects (client-side)

1. Create `effects/impl/YourEffect.java` implementing `VisualEffect`
2. Register in `EffectManager.initialize()`: `register(YourEffect.ID, YourEffect::new)`
3. Update this file with the new effect's params and examples

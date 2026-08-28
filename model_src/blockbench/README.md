# Blockbench model sources

Home of the hand-authored accessory models for the appearance trait system (hair, wings,
and any future fixed 3D cosmetics) and the offline preview tooling.

## Coordinate space

All models use the mod's **part-local pixel space** — the same units as vanilla skin
dimensions and the trait renderers (`BakedAccessoryModel`, `LayeredHairModel`):

- **16 px = 1 block** (1 px = 1/16 block)
- **+Y points down**: the head's crown is `y = -8`, the chin `y = 0`; the torso runs
  `y = 0` (neck) to `y = 12` (waist)
- Faces look toward `-Z` (the head's face plane is `z = -4`)
- `x` runs left(-4) to right(+4) on the head/torso

## Workflow

1. **Author** a model in [Blockbench](https://www.blockbench.net/) (or by hand in JSON).
   Keep the cubes in the JSON-friendly format described below.
2. **Preview** it without launching Minecraft:

   ```bash
   node generate-coi-preview.mjs my_model.json
   ```

   This writes `my_model.svg` with front/back/side isometric renders.

3. **Transcribe** the finished cubes into the matching Java builder calls
   (`BakedAccessoryModel.Builder#box` / `#quad` for single-off geometry, or
   `LayeredHairModel#build` parameters for hair styles). Both work in the same pixel
   space, so coordinates copy over 1:1.

## JSON format

An array of cubes, or an object with a `"cubes"` array:

```json
[
  { "name": "cap",     "origin": [-4.2, -9.4, -4.2], "size": [8.4, 2.3, 8.4], "color": "#12121e" },
  { "name": "streak",  "origin": [-1.2, -9.6, -4.0], "size": [2.4, 0.5, 4.6], "color": "#3d3a4d" },
  { "name": "membrane","origin": [1.5, 1.0, 2.05],   "size": [9.7, 5.2, 0.2], "color": "#240511", "alpha": 0.85 }
]
```

| field    | meaning                                                        |
|----------|----------------------------------------------------------------|
| `origin` | minimal corner `[x, y, z]` in pixel space                       |
| `size`   | extent `[w, h, d]` in pixels                                    |
| `color`  | CSS hex fill; the preview applies Lambert shading per face      |
| `alpha`  | optional 0–1 transparency (default 1)                           |
| `name`   | optional label, shown in errors and useful for the transcription step |

## Shipped examples

- `example_short_hair.json` — mirrors the short-hair style built by `LayeredHairModel`
  (cap, shadow rim, highlight streak, sides, back, fringe) so you can see the format and
  the projection before authoring your own.

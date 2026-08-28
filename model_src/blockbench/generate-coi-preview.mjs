#!/usr/bin/env node
/**
 * generate-coi-preview.mjs — offline isometric previews for COI accessory models.
 *
 * Reads a simple JSON cuboid description (Blockbench-style) and renders three isometric
 * SVG views (front-quarter, back-quarter, side) next to the input file, so hair/wing/
 * accessory models can be iterated in Blockbench or a text editor without launching
 * Minecraft. Coordinates use the mod's part-local pixel space (16 px = 1 block,
 * +Y points DOWN, matching vanilla model-part space — the crown of the head is y=-8).
 *
 * Usage:
 *     node generate-coi-preview.mjs <model.json> [more.json ...]
 *
 * JSON format (array or { "cubes": [...] }):
 *     [
 *       { "origin": [-4.2, -9.4, -4.2], "size": [8.4, 2.3, 8.4],
 *         "color": "#12121e", "alpha": 1.0, "name": "cap" },
 *       ...
 *     ]
 * origin is the minimal corner in pixel space; size is the extent in pixels.
 * color accepts any CSS hex form; alpha is optional (default 1).
 */

import { readFileSync, writeFileSync } from "node:fs";
import { dirname, join, basename } from "node:path";

function parseCubes(json) {
  const data = JSON.parse(json);
  const cubes = Array.isArray(data) ? data : data.cubes;
  if (!Array.isArray(cubes)) {
    throw new Error("expected a JSON array of cubes or an object with a \"cubes\" array");
  }
  return cubes.map((cube, index) => {
    const [x, y, z] = cube.origin;
    const [w, h, d] = cube.size;
    if ([x, y, z, w, h, d].some((n) => typeof n !== "number")) {
      throw new Error(`cube ${index} (${cube.name ?? "unnamed"}): origin/size must be 3 numbers each`);
    }
    return {
      name: cube.name ?? `cube_${index}`,
      minX: x, minY: y, minZ: z,
      maxX: x + w, maxY: y + h, maxZ: z + d,
      color: cube.color ?? "#888888",
      alpha: cube.alpha ?? 1.0,
    };
  });
}

/** Convert part-local pixel space to block units (divided by 16) with +Y up. */
function toWorld(cube) {
  const s = 1 / 16;
  return {
    ...cube,
    x0: cube.minX * s, x1: cube.maxX * s,
    y0: -cube.maxY * s, y1: -cube.minY * s, // flip Y: pixel space grows down
    z0: cube.minZ * s, z1: cube.maxZ * s,
  };
}

const ISO_VIEWS = [
  { name: "front", yaw: Math.PI * 0.25, pitch: Math.PI * 0.2 },
  { name: "back", yaw: Math.PI * 1.25, pitch: Math.PI * 0.2 },
  { name: "side", yaw: Math.PI * 0.5, pitch: Math.PI * 0.2 },
];

function projectPoint(x, y, z, yaw, pitch) {
  // Rotate around Y, then tilt around X — classic isometric projection.
  const cosY = Math.cos(yaw), sinY = Math.sin(yaw);
  const x1 = x * cosY + z * sinY;
  const z1 = -x * sinY + z * cosY;
  const cosP = Math.cos(pitch), sinP = Math.sin(pitch);
  const y1 = y * cosP - z1 * sinP;
  const depth = z1 * cosP + y * sinP;
  return { x: x1, y: y1, depth };
}

function faceNormalToCamera(nx, ny, nz, yaw, pitch) {
  const cosY = Math.cos(yaw), sinY = Math.sin(yaw);
  const nx1 = nx * cosY + nz * sinY;
  const nz1 = -nx * sinY + nz * cosY;
  return nz1; // camera looks down +Z after rotation; faces with nz1 < 0 face us
}

function hexToRgb(hex) {
  const value = hex.replace("#", "");
  const full = value.length === 3 ? value.split("").map((c) => c + c).join("") : value;
  const n = parseInt(full, 16);
  return [((n >> 16) & 255) / 255, ((n >> 8) & 255) / 255, (n & 255) / 255];
}

/** Lambert shading multiplier per face, keyed by the outward normal in model space. */
const FACES = [
  { n: [0, 0, -1], shade: 1.0 },   // front
  { n: [0, 0, 1], shade: 0.55 },   // back
  { n: [0, -1, 0], shade: 1.18 },  // top (model -Y is up)
  { n: [0, 1, 0], shade: 0.4 },    // bottom
  { n: [1, 0, 0], shade: 0.82 },   // right
  { n: [-1, 0, 0], shade: 0.68 },  // left
];

function faceCorners(cube, face) {
  const [nx, ny, nz] = face.n;
  const x = nx > 0 ? cube.x1 : nx < 0 ? cube.x0 : null;
  const y = ny > 0 ? cube.y1 : ny < 0 ? cube.y0 : null;
  const z = nz > 0 ? cube.z1 : nz < 0 ? cube.z0 : null;
  const pick = (a, b) => (a === null ? b : a);
  const xs = x === null ? [cube.x0, cube.x1] : [x, x];
  const ys = y === null ? [cube.y0, cube.y1] : [y, y];
  const zs = z === null ? [cube.z0, cube.z1] : [z, z];
  // Corners wound consistently per face
  return [
    [pick(nx, xs[0]), pick(ny, ys[1]), pick(nz, zs[0])],
    [pick(nx, xs[1]), pick(ny, ys[1]), pick(nz, zs[0])],
    [pick(nx, xs[1]), pick(ny, ys[0]), pick(nz, zs[1])],
    [pick(nx, xs[0]), pick(ny, ys[0]), pick(nz, zs[1])],
  ];
}

function renderView(cubes, view, svgSize = 360) {
  const projected = [];
  for (const raw of cubes) {
    const cube = toWorld(raw);
    for (const face of FACES) {
      if (faceNormalToCamera(face.n[0], face.n[1], face.n[2], view.yaw, view.pitch) >= 0.02) {
        continue; // back-facing
      }
      const [r, g, b] = hexToRgb(cube.color);
      const shade = face.shade;
      const corners = faceCorners(cube, face)
        .map(([x, y, z]) => projectPoint(x, y, z, view.yaw, view.pitch))
        .map((p) => ({ ...p, screenX: svgSize / 2 + p.x * svgSize * 0.42, screenY: svgSize / 2 - p.y * svgSize * 0.42 }));
      const depth = corners.reduce((sum, c) => sum + c.depth, 0) / corners.length;
      projected.push({
        depth,
        points: corners.map((c) => `${c.screenX.toFixed(1)},${c.screenY.toFixed(1)}`).join(" "),
        fill: `rgb(${Math.round(r * 255 * shade)},${Math.round(g * 255 * shade)},${Math.round(b * 255 * shade)})`,
        fillOpacity: cube.alpha,
      });
    }
  }
  // Painter's algorithm: far faces first
  projected.sort((a, b) => b.depth - a.depth);
  const polygons = projected
    .map((p) => `  <polygon points="${p.points}" fill="${p.fill}" fill-opacity="${p.fillOpacity}" stroke="#000" stroke-opacity="0.15"/>`)
    .join("\n");
  return `  <g id="${view.name}">\n${polygons}\n  </g>`;
}

function renderSvg(cubes, title) {
  const groups = ISO_VIEWS.map((view, index) => {
    const offsetX = index * 380;
    const body = renderView(cubes, view)
      .split("\n")
      .map((line) => "  " + line.trimStart())
      .join("\n");
    return `<g transform="translate(${offsetX},40)">\n${body}\n  <text x="180" y="360" text-anchor="middle" font-family="monospace" font-size="14" fill="#888">${view.name}</text>\n</g>`;
  }).join("\n");
  return `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1140" height="440" viewBox="0 0 1140 440">
  <rect width="1140" height="440" fill="#16161c"/>
  <text x="20" y="26" font-family="monospace" font-size="16" fill="#ffd870">${title}</text>
${groups}
</svg>
`;
}

function main() {
  const args = process.argv.slice(2);
  if (args.length === 0) {
    console.error("usage: node generate-coi-preview.mjs <model.json> [more.json ...]");
    process.exit(1);
  }
  for (const path of args) {
    const cubes = parseCubes(readFileSync(path, "utf8"));
    const svg = renderSvg(cubes, basename(path));
    const outPath = join(dirname(path), basename(path, ".json") + ".svg");
    writeFileSync(outPath, svg);
    console.log(`${path} -> ${outPath} (${cubes.length} cubes)`);
  }
}

main();

package dev.ua.ikeepcalm.coi.client.appearance;

import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import dev.ua.ikeepcalm.coi.client.mcf.MythicalFormManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Emits each pathway's uniqueness signature around players: a colored aura (orbiting
 * dust, halos, rising motes), a procedural dot-matrix glyph of the pathway's symbol
 * rising every few seconds, and a movement trail. The Twilight Giant instead gets a
 * full-bright setting-sun backdrop hanging behind the shoulders.
 *
 * <p>Runs on the client tick but emits at half tick rate. Players farther than 48 blocks
 * are ignored, the local player is suppressed while the camera is first-person, and the
 * whole system respects the self/other toggles in {@link AppearanceConfig}.</p>
 *
 * <p>Pathway resolution order per player: debug assignment (F8 screen) → active mythical
 * form → unambiguous appearance traits. When the COI server eventually broadcasts
 * high-sequence state, the trait map grows to cover it with no client changes.</p>
 */
public final class UniquenessParticleManager {

    /** The 22 pathways with a uniqueness signature, matching the COI server roster. */
    public static final List<String> PATHWAYS = List.of(
            "abyss", "chained", "darkness", "death", "demoness", "door", "emperor", "error",
            "fool", "fortune", "giant", "hanged", "hermit", "justiciar", "moon", "mother",
            "paragon", "priest", "sun", "tower", "tyrant", "visionary");

    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;
    private static final int GLYPH_PERIOD_TICKS = 90;

    /** Trait ids whose home pathway is unambiguous — used to seed effects from server data. */
    private static final Map<String, String> TRAIT_PATHWAY_HINTS = Map.ofEntries(
            Map.entry("female_traits", "demoness"),
            Map.entry("demoness_ears", "demoness"),
            Map.entry("mother_traits", "mother"),
            Map.entry("moon_traits", "moon"),
            Map.entry("black_hair", "moon"),
            Map.entry("blue_hair", "tyrant"),
            Map.entry("red_hair", "priest"),
            Map.entry("silver_hair", "fortune"),
            Map.entry("long_brown_hair", "mother"),
            Map.entry("long_black_hair", "darkness"),
            Map.entry("glowing_eyes", "visionary"),
            Map.entry("darkness_wings", "darkness"),
            Map.entry("horns", "abyss"),
            Map.entry("mushroom", "mother"));

    private static final Map<String, String> debugPathwayByUuid = new ConcurrentHashMap<>();
    private static final Map<String, double[]> lastPositions = new HashMap<>();
    private static int tickCounter = 0;

    private UniquenessParticleManager() {
    }

    // ------------------------------------------------------------------
    // Pathway resolution (debug assignment wins, then form, then traits)
    // ------------------------------------------------------------------

    public static void setDebugPathway(String playerUuid, String pathway) {
        if (pathway == null) {
            debugPathwayByUuid.remove(playerUuid);
        } else {
            debugPathwayByUuid.put(playerUuid, pathway);
        }
    }

    public static String getDebugPathway(String playerUuid) {
        return playerUuid == null ? null : debugPathwayByUuid.get(playerUuid);
    }

    public static void reset() {
        debugPathwayByUuid.clear();
        lastPositions.clear();
    }

    public static String resolvePathway(AbstractClientPlayer player) {
        String uuid = player.getUUID().toString();
        String debug = debugPathwayByUuid.get(uuid);
        if (debug != null) {
            return debug;
        }
        String form = MythicalFormManager.getForm(uuid);
        if (form != null && PATHWAYS.contains(form.toLowerCase(Locale.ROOT))) {
            return form.toLowerCase(Locale.ROOT);
        }
        for (String traitId : ClientAppearanceState.getTraits(uuid)) {
            String pathway = TRAIT_PATHWAY_HINTS.get(traitId);
            if (pathway != null) {
                return pathway;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Minecraft client) {
        tickCounter++;
        if ((tickCounter & 1) == 1) {
            return; // half tick rate
        }
        AppearanceConfig.AppearanceSettings settings = AppearanceConfig.getSettings();
        if (!settings.enableUniquenessEffects) {
            return;
        }
        ClientLevel level = client.level;
        if (level == null) {
            return;
        }

        var camera = client.getCameraEntity() != null ? client.getCameraEntity() : client.player;
        if (camera == null) {
            return;
        }
        boolean firstPerson = client.options.getCameraType().isFirstPerson();

        List<String> seen = new ArrayList<>();
        for (AbstractClientPlayer player : level.players()) {
            String uuid = player.getUUID().toString();
            seen.add(uuid);
            if (player.isInvisible() || player.isSpectator()) {
                continue;
            }
            boolean self = player == client.player;
            if (self && (firstPerson || !settings.uniquenessShowSelf)) {
                continue; // suppress for the local first-person camera
            }
            if (!self && !settings.uniquenessShowOthers) {
                continue;
            }
            if (player.distanceToSqr(camera) > MAX_DISTANCE_SQ) {
                continue;
            }
            String pathway = resolvePathway(player);
            if (pathway == null) {
                continue;
            }

            emitTrail(level, player, uuid);
            emit(level, player, pathway, tickCounter / 2);
        }

        lastPositions.keySet().retainAll(seen);
    }

    // ------------------------------------------------------------------
    // Emission
    // ------------------------------------------------------------------

    private static void emitTrail(ClientLevel level, AbstractClientPlayer player, String uuid) {
        double[] last = lastPositions.get(uuid);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (last == null) {
            lastPositions.put(uuid, new double[]{x, y, z});
            return;
        }
        double dx = x - last[0];
        double dz = z - last[2];
        last[0] = x;
        last[1] = y;
        last[2] = z;
        if (dx * dx + dz * dz < 0.0004) {
            return; // trails require movement
        }

        String pathway = resolvePathway(player);
        if (pathway == null) {
            return;
        }
        int rgb = accent(pathway);
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double backX = x - Math.sin(yawRad) * 0.35;
        double backZ = z + Math.cos(yawRad) * 0.35;
        level.addParticle(new DustParticleOptions(rgb, 0.7f),
                backX, y + 0.12, backZ, -dx * 0.4, 0.015, -dz * 0.4);
    }

    private static void emit(ClientLevel level, AbstractClientPlayer player, String pathway, long emissionTick) {
        int rgb = accent(pathway);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        var random = player.getRandom();
        UUID seed = player.getUUID();
        double phase = (seed.getLeastSignificantBits() & 0xFFFF) / (double) 0xFFFF;

        // Procedural symbol glyph, rising every few seconds, phase-offset per player
        long glyphTick = emissionTick + (long) (phase * GLYPH_PERIOD_TICKS);
        if (glyphTick % GLYPH_PERIOD_TICKS == 0 && !"giant".equals(pathway)) {
            emitGlyph(level, player, pathway, rgb);
        }

        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double lookX = -Math.sin(yawRad);
        double lookZ = Math.cos(yawRad);

        switch (pathway) {
            case "fool" -> {
                // Gray fog above gray fog: a slowly turning haze disc over the head
                double angle = emissionTick * 0.14 + phase * Math.PI * 2;
                for (int i = 0; i < 2; i++) {
                    double a = angle + i * Math.PI;
                    level.addParticle(new DustParticleOptions(rgb, 1.7f),
                            x + Math.cos(a) * 0.55, y + 2.55 + Math.sin(emissionTick * 0.31 + i) * 0.08,
                            z + Math.sin(a) * 0.55, 0.0, 0.004, 0.0);
                }
                if (random.nextFloat() < 0.25f) {
                    level.addParticle(ParticleTypes.WHITE_ASH, x, y + 2.3, z, 0.0, 0.01, 0.0);
                }
            }
            case "door" -> {
                // A starry constellation wheeling overhead
                double angle = emissionTick * 0.21 + phase * Math.PI * 2;
                for (int i = 0; i < 2; i++) {
                    double a = angle + i * Math.PI * 0.9;
                    double r = i == 0 ? 0.75 : 0.45;
                    level.addParticle(ParticleTypes.END_ROD,
                            x + Math.cos(a) * r, y + 2.45 + Math.sin(a * 2.0) * 0.18,
                            z + Math.sin(a) * r, 0.0, 0.0, 0.0);
                }
            }
            case "error" -> {
                // Corrupted static: jittery red sparks that snap between positions
                double jitterX = (random.nextDouble() - 0.5) * 1.3;
                double jitterZ = (random.nextDouble() - 0.5) * 1.3;
                level.addParticle(new DustParticleOptions(rgb, 1.0f),
                        x + jitterX, y + 1.0 + random.nextDouble() * 1.3, z + jitterZ, 0.0, 0.0, 0.0);
                if (random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + jitterX, y + 1.6, z + jitterZ, 0.0, 0.0, 0.0);
                }
            }
            case "visionary" -> {
                // Thought-forms spiraling the brow
                double angle = emissionTick * 0.26 + phase * Math.PI * 2;
                level.addParticle(ParticleTypes.ENCHANT,
                        x + Math.cos(angle) * 0.5, y + 2.1 + Math.sin(angle * 1.5) * 0.22,
                        z + Math.sin(angle) * 0.5, 0.0, 0.012, 0.0);
                if (random.nextFloat() < 0.12f) {
                    level.addParticle(ParticleTypes.WITCH, x, y + 2.3, z, 0.0, 0.01, 0.0);
                }
            }
            case "sun" -> {
                // Solar radiance: a warm ring with rising motes
                double angle = emissionTick * 0.18 + phase * Math.PI * 2;
                level.addParticle(new DustParticleOptions(rgb, 1.25f),
                        x + Math.cos(angle) * 0.9, y + 1.35, z + Math.sin(angle) * 0.9, 0.0, 0.01, 0.0);
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.GLOW, x, y + 0.6 + random.nextDouble() * 1.4, z, 0.0, 0.02, 0.0);
                }
            }
            case "hanged" -> {
                // Inverted whispers: pale motes sinking around the body
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.SOUL,
                            x + (random.nextDouble() - 0.5) * 1.1, y + 2.4, z + (random.nextDouble() - 0.5) * 1.1,
                            0.0, -0.03, 0.0);
                }
                level.addParticle(new DustParticleOptions(rgb, 0.9f),
                        x, y + 1.6, z, 0.0, -0.01, 0.0);
            }
            case "tyrant" -> {
                // Storm-lord charge: electric ring with cloud wisps
                double angle = emissionTick * 0.34 + phase * Math.PI * 2;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        x + Math.cos(angle) * 0.8, y + 0.9 + random.nextDouble() * 1.4, z + Math.sin(angle) * 0.8,
                        0.0, 0.0, 0.0);
                if (emissionTick % 3 == 0) {
                    level.addParticle(ParticleTypes.CLOUD, x, y + 2.2, z, 0.0, 0.005, 0.0);
                }
            }
            case "demoness" -> {
                // Crimson allure: an orbiting charm + rare heart
                double angle = -emissionTick * 0.22 + phase * Math.PI * 2;
                level.addParticle(new DustParticleOptions(rgb, 1.05f),
                        x + Math.cos(angle) * 0.7, y + 1.5 + Math.sin(angle * 2.0) * 0.3,
                        z + Math.sin(angle) * 0.7, 0.0, 0.0, 0.0);
                if (random.nextFloat() < 0.05f) {
                    level.addParticle(ParticleTypes.HEART, x, y + 2.2, z, 0.0, 0.0, 0.0);
                }
            }
            case "abyss" -> {
                // Dark flames licking upward
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            x + (random.nextDouble() - 0.5) * 0.9, y + 0.2 + random.nextDouble() * 1.6,
                            z + (random.nextDouble() - 0.5) * 0.9, 0.0, 0.015, 0.0);
                }
                level.addParticle(ParticleTypes.ASH, x, y + 1.8, z, 0.0, -0.01, 0.0);
            }
            case "chained" -> {
                // Bound spirits: heavy gray motes and sculk whispers
                level.addParticle(new DustParticleOptions(rgb, 1.0f),
                        x + (random.nextDouble() - 0.5) * 0.9, y + 0.5 + random.nextDouble() * 1.2,
                        z + (random.nextDouble() - 0.5) * 0.9, 0.0, -0.005, 0.0);
                if (random.nextFloat() < 0.15f) {
                    level.addParticle(ParticleTypes.SCULK_SOUL, x, y + 1.4, z, 0.0, 0.01, 0.0);
                }
            }
            case "mother" -> {
                // Life spores drifting off the body
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                            x + (random.nextDouble() - 0.5) * 1.2, y + 0.4 + random.nextDouble() * 1.8,
                            z + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.005, 0.0);
                }
                level.addParticle(new DustParticleOptions(rgb, 0.8f), x, y + 1.1, z, 0.0, 0.008, 0.0);
            }
            case "moon" -> {
                // Silver moonlight wheeling around the body
                double angle = emissionTick * 0.16 + phase * Math.PI * 2;
                level.addParticle(new DustParticleOptions(rgb, 1.0f),
                        x + Math.cos(angle) * 0.85, y + 1.2 + Math.sin(angle) * 0.5,
                        z + Math.sin(angle) * 0.85, 0.0, 0.005, 0.0);
                if (random.nextFloat() < 0.1f) {
                    level.addParticle(ParticleTypes.SNOWFLAKE, x, y + 2.4, z, 0.0, -0.01, 0.0);
                }
            }
            case "priest" -> {
                // Ritual flame rising along the body
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.SMALL_FLAME,
                            x + (random.nextDouble() - 0.5) * 0.8, y + 0.3 + random.nextDouble() * 1.5,
                            z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.012, 0.0);
                }
                level.addParticle(new DustParticleOptions(rgb, 0.85f), x, y + 1.7, z, 0.0, 0.006, 0.0);
            }
            case "justiciar" -> {
                // Golden scales of order: a measured orbit + enchant glints
                double angle = emissionTick * 0.15 + phase * Math.PI * 2;
                level.addParticle(new DustParticleOptions(rgb, 1.0f),
                        x + Math.cos(angle) * 0.8, y + 1.0, z + Math.sin(angle) * 0.8, 0.0, 0.012, 0.0);
                if (random.nextFloat() < 0.15f) {
                    level.addParticle(ParticleTypes.ENCHANT, x, y + 2.0, z, 0.0, 0.01, 0.0);
                }
            }
            case "giant" -> emitSettingSun(level, player, rgb, emissionTick);
            case "darkness" -> {
                // Shadow made visible: a dim halo with falling ash
                level.addParticle(new DustParticleOptions(rgb, 1.3f),
                        x + (random.nextDouble() - 0.5) * 1.1, y + 1.9, z + (random.nextDouble() - 0.5) * 1.1,
                        0.0, -0.004, 0.0);
                if (random.nextFloat() < 0.3f) {
                    level.addParticle(ParticleTypes.ASH, x, y + 2.5, z, 0.0, -0.02, 0.0);
                }
            }
            case "death" -> {
                // Soul wisps peeling off the body
                if (emissionTick % 2 == 0) {
                    level.addParticle(ParticleTypes.SOUL,
                            x + (random.nextDouble() - 0.5) * 0.8, y + 0.6 + random.nextDouble() * 1.4,
                            z + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.02, 0.0);
                }
            }
            case "hermit" -> {
                // Deep isolation: a slow indigo spiral, far from the body
                double angle = emissionTick * 0.11 + phase * Math.PI * 2;
                level.addParticle(new DustParticleOptions(rgb, 0.95f),
                        x + Math.cos(angle) * 1.1, y + 0.5 + (emissionTick % 20) * 0.09,
                        z + Math.sin(angle) * 1.1, 0.0, 0.004, 0.0);
            }
            case "fortune" -> {
                // Lucky sparks: gold glitter orbiting upward
                double angle = -emissionTick * 0.28 + phase * Math.PI * 2;
                level.addParticle(ParticleTypes.GLOW,
                        x + Math.cos(angle) * 0.7, y + 0.8 + (emissionTick % 24) * 0.07,
                        z + Math.sin(angle) * 0.7, 0.0, 0.0, 0.0);
                if (random.nextFloat() < 0.2f) {
                    level.addParticle(new DustParticleOptions(rgb, 0.8f), x, y + 1.6, z, 0.0, 0.01, 0.0);
                }
            }
            case "emperor" -> {
                // Royal columns: purple-gold motes rising in a square pattern
                double angle = emissionTick * 0.2 + phase * Math.PI * 2;
                double a = Math.round(angle / (Math.PI / 2)) * (Math.PI / 2);
                level.addParticle(new DustParticleOptions(rgb, 1.0f),
                        x + Math.cos(a) * 0.7, y + 0.3 + (emissionTick % 26) * 0.075,
                        z + Math.sin(a) * 0.7, 0.0, 0.01, 0.0);
                if (random.nextFloat() < 0.12f) {
                    level.addParticle(ParticleTypes.ENCHANT, x, y + 2.1, z, 0.0, 0.0, 0.0);
                }
            }
            case "paragon" -> {
                // Forge sparks of the supreme craftsman
                level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                        x + (random.nextDouble() - 0.5) * 0.7, y + 0.8 + random.nextDouble() * 1.2,
                        z + (random.nextDouble() - 0.5) * 0.7, 0.0, 0.02, 0.0);
                if (random.nextFloat() < 0.3f) {
                    level.addParticle(new DustParticleOptions(rgb, 0.85f), x, y + 1.3, z, 0.0, 0.015, 0.0);
                }
            }
            case "tower" -> {
                // Ascension: weightless motes streaming up the spine
                if (emissionTick % 2 == 0) {
                    level.addParticle(new DustParticleOptions(rgb, 0.95f),
                            x + (random.nextDouble() - 0.5) * 0.5, y + 0.2 + (emissionTick % 30) * 0.06,
                            z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.03, 0.0);
                }
                if (random.nextFloat() < 0.08f) {
                    level.addParticle(ParticleTypes.CLOUD, x, y + 2.6, z, 0.0, 0.01, 0.0);
                }
            }
            default -> {
            }
        }
    }

    /**
     * Twilight Giant's uniqueness: a full-bright setting-sun backdrop hanging behind the
     * shoulders — a dense ember disc with end-rod rim light, plus an amber ground ring.
     * Composed from the brightest vanilla particles (end rods render near-emissive), so
     * the disc reads as glowing even without a custom particle type.
     */
    private static void emitSettingSun(ClientLevel level, AbstractClientPlayer player,
                                       int rgb, long emissionTick) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double backX = x + Math.sin(yawRad) * 1.5; // behind the player
        double backZ = z - Math.cos(yawRad) * 1.5;
        double sunY = y + 1.35;

        var random = player.getRandom();
        double angle = emissionTick * 0.5 + random.nextDouble() * Math.PI * 2;
        double radius = 0.55 + random.nextDouble() * 0.5;
        // Ember disc body
        level.addParticle(new DustParticleOptions(0xFF9E2E, 2.6f),
                backX + Math.cos(angle) * radius, sunY + Math.sin(angle) * radius * 0.85,
                backZ + Math.sin(angle) * radius * 0.35, 0.0, 0.0, 0.0);
        // Rim light
        if (emissionTick % 2 == 0) {
            level.addParticle(ParticleTypes.END_ROD,
                    backX + Math.cos(angle * 1.7) * 0.95, sunY + Math.sin(angle * 1.7) * 0.8,
                    backZ + Math.sin(angle * 1.7) * 0.5, 0.0, 0.0, 0.0);
        }
        // Amber ground ring in front of the feet
        double ringAngle = emissionTick * 0.22;
        level.addParticle(new DustParticleOptions(rgb, 0.9f),
                x + Math.cos(ringAngle) * 1.0, y + 0.1, z + Math.sin(ringAngle) * 1.0, 0.0, 0.005, 0.0);
    }

    /**
     * A procedural symbol: the pathway's glyph as a dot-matrix drawn with dust particles,
     * floating in front of the chest aligned to the body's yaw, then fading out.
     */
    private static void emitGlyph(ClientLevel level, AbstractClientPlayer player, String pathway, int rgb) {
        long mask = GLYPH_MASKS.getOrDefault(pathway, 0L);
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        // Right vector of the body's facing
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        double baseX = player.getX() - Math.sin(yawRad) * 0.85;
        double baseZ = player.getZ() + Math.cos(yawRad) * 0.85;
        double baseY = player.getY() + 1.55;

        int grid = 5;
        double cell = 0.13;
        for (int row = 0; row < grid; row++) {
            for (int col = 0; col < grid; col++) {
                int bit = row * grid + col;
                if (((mask >> bit) & 1L) == 0L) {
                    continue;
                }
                double offsetX = (col - 2) * cell;
                double offsetY = (2 - row) * cell;
                level.addParticle(new DustParticleOptions(rgb, 0.85f),
                        baseX + rightX * offsetX,
                        baseY + offsetY,
                        baseZ + rightZ * offsetX,
                        0.0, 0.012, 0.0);
            }
        }
    }

    // 5x5 bit patterns (bit = row*5+col, row 0 = top) — rough procedural sigils per pathway
    private static final Map<String, Long> GLYPH_MASKS = Map.ofEntries(
            Map.entry("fool", glyph(".....", ".###.", "#.##.", "#.#.#", ".###.")),      // wheel of fate
            Map.entry("door", glyph(".###.", "#...#", "#..##", "#.##.", "#...#")),      // archway
            Map.entry("error", glyph("#.#.#", ".#.#.", "#.##.", ".#.#.", "#.#.#")),     // shattered grid
            Map.entry("visionary", glyph(".....", ".###.", "#.#.#", "#...#", ".###.")), // open eye
            Map.entry("sun", glyph(".###.", "##.##", "#.#.#", "##.##", ".###.")),       // radiant disc
            Map.entry("tyrant", glyph("..#..", ".#.#.", "##.##", ".#.#.", "#####")),    // trident
            Map.entry("demoness", glyph("#...#", "#.#.#", ".###.", ".#.#.", "#...#")),  // allure cross
            Map.entry("abyss", glyph(".###.", "#...#", "#.##.", "#.##.", "#####")),     // devouring maw
            Map.entry("chained", glyph("#####", "..#..", "#####", "..#..", "#####")),   // chain links
            Map.entry("mother", glyph("..#..", ".#.#.", ".#.#.", ".#.#.", "#####")),    // sprout
            Map.entry("moon", glyph(".###.", "#..#.", "#.#..", "#.#..", "#..#.")),      // crescent
            Map.entry("priest", glyph("...#.", "..##.", ".###.", "#####", "..#..")),    // flame sigil
            Map.entry("darkness", glyph("#...#", ".#.#.", "..#..", ".#.#.", "#...#")),  // closing shadow
            Map.entry("death", glyph("#...#", ".#.#.", "..#..", ".#.#.", "#...#")),     // bone cross
            Map.entry("hermit", glyph("#####", "..#..", ".###.", "..#..", "#####")),    // lantern
            Map.entry("fortune", glyph("#...#", ".###.", "..#..", ".###.", "..#..")),   // dice pips
            Map.entry("emperor", glyph("#...#", "#####", ".###.", ".###.", "#####")),   // crown
            Map.entry("paragon", glyph("##.##", ".###.", "..#..", "..#..", "..#..")),   // hammer
            Map.entry("giant", glyph("##.##", "##.##", "#####", ".###.", "..#..")),     // mountain sun
            Map.entry("hanged", glyph("#####", "..#..", ".###.", "..#..", ".#.#.")),    // inverted figure
            Map.entry("justiciar", glyph("#...#", ".#.#.", ".###.", ".#.#.", "#...#")), // scales
            Map.entry("tower", glyph("#####", "..#..", "..#..", "..#..", "..#..")));    // spire

    private static long glyph(String... rows) {
        long mask = 0;
        for (int row = 0; row < rows.length && row < 5; row++) {
            for (int col = 0; col < rows[row].length() && col < 5; col++) {
                if (rows[row].charAt(col) == '#') {
                    mask |= 1L << (row * 5 + col);
                }
            }
        }
        return mask;
    }

    private static int accent(String pathway) {
        return ACCENTS.getOrDefault(pathway, 0xCCCCCC);
    }

    private static final Map<String, Integer> ACCENTS = Map.ofEntries(
            Map.entry("fool", 0xB347CC),
            Map.entry("door", 0x5B7FE6),
            Map.entry("sun", 0xFFE55C),
            Map.entry("tyrant", 0x4AA3FF),
            Map.entry("demoness", 0xB22222),
            Map.entry("priest", 0xFF6B35),
            Map.entry("error", 0xE04848),
            Map.entry("tower", 0x99AABB),
            Map.entry("visionary", 0x44CCBB),
            Map.entry("hanged", 0x3A6E4F),
            Map.entry("darkness", 0x4A1A6E),
            Map.entry("death", 0xC8D0E8),
            Map.entry("giant", 0xE88B2A),
            Map.entry("paragon", 0xE8E8FF),
            Map.entry("hermit", 0x8855CC),
            Map.entry("fortune", 0xFFD700),
            Map.entry("chained", 0x888899),
            Map.entry("abyss", 0x8A2B5A),
            Map.entry("justiciar", 0xEEDD88),
            Map.entry("emperor", 0xDD9922),
            Map.entry("moon", 0xC9D4E8),
            Map.entry("mother", 0x7FC96B));
}

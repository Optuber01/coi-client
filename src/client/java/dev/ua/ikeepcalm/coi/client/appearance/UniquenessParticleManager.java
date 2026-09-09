package dev.ua.ikeepcalm.coi.client.appearance;

import dev.ua.ikeepcalm.coi.client.ClientAppearanceState;
import dev.ua.ikeepcalm.coi.client.config.AppearanceConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded world-space accents and the preserved Death/Door particle compositions. */
public final class UniquenessParticleManager {

    /** The 22 pathways with a uniqueness signature, matching the COI server roster. */
    public static final List<String> PATHWAYS = List.of(
            "abyss", "chained", "darkness", "death", "demoness", "door", "emperor", "error",
            "fool", "fortune", "giant", "hanged", "hermit", "justiciar", "moon", "mother",
            "paragon", "priest", "sun", "tower", "tyrant", "visionary");

    private static final double MAX_DISTANCE_SQ = 48.0 * 48.0;
    private static final int GLYPH_PERIOD_TICKS = 90;
    private static final int STATIONARY_SIGIL_TICKS = 30; // 30 half-rate updates = three seconds
    private static final String UNIQUENESS_MARKER_PREFIX = "uniqueness:";

    private static final Map<String, String> debugPathwayByUuid = new ConcurrentHashMap<>();
    private static final Map<String, double[]> lastPositions = new HashMap<>();
    private static final Map<String, Integer> stationaryTicks = new HashMap<>();
    private static int tickCounter = 0;
    private static ClientLevel lastLevel;
    private static long lastGameTick = Long.MIN_VALUE;

    private UniquenessParticleManager() {
    }

    // ------------------------------------------------------------------
    // Pathway resolution (debug assignment wins, then form, then traits)
    // ------------------------------------------------------------------

    public static void setDebugPathway(String playerUuid, String pathway) {
        if (playerUuid == null) {
            return;
        }
        if (pathway == null || !PATHWAYS.contains(pathway)) {
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
        stationaryTicks.clear();
        tickCounter = 0;
        lastLevel = null;
        lastGameTick = Long.MIN_VALUE;
    }

    public static String resolvePathway(AbstractClientPlayer player) {
        String uuid = player.getUUID().toString();
        return resolvePathway(uuid, ClientAppearanceState.getTraits(uuid));
    }

    public static String resolvePathway(String uuid, Iterable<String> traits) {
        String debug = debugPathwayByUuid.get(uuid);
        if (debug != null) {
            return debug;
        }
        for (String traitId : traits) {
            if (!traitId.startsWith(UNIQUENESS_MARKER_PREFIX)) {
                continue;
            }
            String pathway = traitId.substring(UNIQUENESS_MARKER_PREFIX.length());
            if (PATHWAYS.contains(pathway)) {
                return pathway;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------

    public static void tick(Minecraft client) {
        ClientLevel current = client.level;
        if (current == null) return;
        long gameTick = current.getGameTime();
        // GUI pauses and replay render samples must not emit particles without advancing the world.
        if (current == lastLevel && gameTick == lastGameTick) return;
        if (current != lastLevel || gameTick < lastGameTick) {
            lastPositions.clear();
            stationaryTicks.clear();
            tickCounter = 0;
        }
        lastLevel = current;
        lastGameTick = gameTick;
        tickCounter++;
        if ((tickCounter & 1) == 1) {
            return; // half tick rate
        }
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        if (!settings.enabled || !settings.enableUniquenessEffects) {
            lastPositions.clear();
            stationaryTicks.clear();
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

        Set<String> tracked = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            String uuid = player.getUUID().toString();
            if (player.isInvisible() || player.isSpectator()) {
                continue;
            }
            boolean self = player == client.player;
            if (self && firstPerson && camera == player) {
                continue; // suppress for the local first-person camera
            }
            if (!AppearanceConfig.shouldRenderUniqueness(uuid)) {
                continue;
            }
            if (player.distanceToSqr(camera) > MAX_DISTANCE_SQ) {
                continue;
            }
            String pathway = resolvePathway(player);
            if (pathway == null) {
                continue;
            }

            tracked.add(uuid);
            long emissionTick = tickCounter / 2;
            boolean moving = emitTrail(level, player, uuid, pathway, settings.uniquenessParticleIntensity, emissionTick);
            int stillFor = moving ? 0 : stationaryTicks.merge(uuid, 1, Integer::sum);
            if (legacyParticles(pathway) && stillFor >= STATIONARY_SIGIL_TICKS && stillFor % 2 == 0) {
                emitStationarySigil(level, player, pathway, stillFor - STATIONARY_SIGIL_TICKS);
            }
            emit(level, player, pathway, emissionTick);
        }

        lastPositions.keySet().retainAll(tracked);
        stationaryTicks.keySet().retainAll(tracked);
    }

    // ------------------------------------------------------------------
    // Emission
    // ------------------------------------------------------------------

    private static boolean emitTrail(ClientLevel level, AbstractClientPlayer player, String uuid, String pathway,
                                     float intensity, long emissionTick) {
        double[] last = lastPositions.get(uuid);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        if (last == null) {
            lastPositions.put(uuid, new double[]{x, y, z});
            return false;
        }
        double dx = x - last[0];
        double dz = z - last[2];
        last[0] = x;
        last[1] = y;
        last[2] = z;
        if (dx * dx + dz * dz < 0.0004) {
            return false; // trails require movement
        }

        int rgb = accent(pathway);
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double backX = x + Math.sin(yawRad) * 0.35;
        double backZ = z - Math.cos(yawRad) * 0.35;
        if (shouldEmit(uuid, emissionTick, intensity)) {
            level.addParticle(new DustParticleOptions(rgb, legacyParticles(pathway) ? 0.7f : 0.45f),
                    backX, y + 0.12, backZ, -dx * 0.4, 0.015, -dz * 0.4);
            emitPathwayTrail(level, player, pathway, backX, backZ, dx, dz);
        }
        return true;
    }

    /** Community-inspired movement signatures: footsteps and wakes, not generic body orbits. */
    private static void emitPathwayTrail(ClientLevel level, AbstractClientPlayer player, String pathway,
                                         double backX, double backZ, double dx, double dz) {
        double y = player.getY();
        switch (pathway) {
            case "door" -> level.addParticle(ParticleTypes.GLOW_SQUID_INK, backX, y + 1.15, backZ,
                    -dx * 0.3, 0.012, -dz * 0.3);
            case "death" -> {
                level.addParticle(ParticleTypes.SOUL, backX, y + 0.25, backZ, -dx * 0.2, 0.02, -dz * 0.2);
                level.addParticle(ParticleTypes.SCULK_SOUL, backX, y + 0.55, backZ, 0.0, 0.015, 0.0);
            }
            default -> { }
        }
    }

    /** Deterministic sampling makes the intensity control reduce particle count without flicker bursts. */
    private static boolean shouldEmit(String uuid, long emissionTick, float intensity) {
        if (intensity >= 0.999f) return true;
        long sample = emissionTick * 31L + uuid.hashCode() * 17L;
        return Math.floorMod(sample, 100L) < Math.round(intensity * 100.0f);
    }

    private static void emit(ClientLevel level, AbstractClientPlayer player, String pathway, long emissionTick) {
        int rgb = accent(pathway);
        UUID seed = player.getUUID();
        double phase = (seed.getLeastSignificantBits() & 0xFFFF) / (double) 0xFFFF;

        long glyphTick = emissionTick + (long) (phase * GLYPH_PERIOD_TICKS);
        if (legacyParticles(pathway) && glyphTick % GLYPH_PERIOD_TICKS == 0) {
            emitGlyph(level, player, pathway, rgb);
        }

        Emission emission = new Emission(
                level, player, emissionTick, rgb, phase,
                player.getX(), player.getY(), player.getZ());

        if (!shouldEmit(player.getUUID().toString(), emissionTick, AppearanceConfig.get().uniquenessParticleIntensity)) return;
        switch (pathway) {
            case "door" -> emitDoor(emission);
            case "death" -> emitDeath(emission);
            default -> emitAccent(emission);
        }
    }

    private static void emitDoor(Emission e) {
        double angle = e.tick() * 0.21 + e.phase() * Math.PI * 2;
        for (int index = 0; index < 2; index++) {
            double a = angle + index * Math.PI * 0.9;
            double radius = index == 0 ? 0.75 : 0.45;
            e.level().addParticle(ParticleTypes.END_ROD,
                    e.x() + Math.cos(a) * radius, e.y() + 2.45 + Math.sin(a * 2.0) * 0.18,
                    e.z() + Math.sin(a) * radius, 0.0, 0.0, 0.0);
        }
    }

    private static void emitDeath(Emission e) {
        if (e.tick() % 2 != 0) {
            return;
        }
        var random = e.player().getRandom();
        e.level().addParticle(ParticleTypes.SOUL,
                e.x() + (random.nextDouble() - 0.5) * 0.8, e.y() + 0.6 + random.nextDouble() * 1.4,
                e.z() + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.02, 0.0);
        e.level().addParticle(ParticleTypes.SCULK_SOUL,
                e.x() + (random.nextDouble() - 0.5) * 0.65, e.y() + 0.35,
                e.z() + (random.nextDouble() - 0.5) * 0.65, 0.0, 0.012, 0.0);
    }

    private static boolean legacyParticles(String pathway) {
        return "death".equals(pathway) || "door".equals(pathway);
    }

    private static void emitAccent(Emission e) {
        if (e.tick() % 3 != 0) return;
        double angle = e.tick() * .08 + e.phase() * Math.PI * 2;
        double yaw = Math.toRadians(e.player().getYRot());
        double horizontal = Math.cos(angle) * .65;
        double x = e.x() + Math.sin(yaw) * .5 + Math.cos(yaw) * horizontal;
        double z = e.z() - Math.cos(yaw) * .5 + Math.sin(yaw) * horizontal;
        e.level().addParticle(new DustParticleOptions(e.rgb(), .35f), x,
                e.y() + 1.25 + Math.sin(angle) * .6, z, 0, .008, 0);
        if ("priest".equals(resolvePathway(e.player()))) {
            e.level().addParticle(ParticleTypes.SMALL_FLAME, x, e.y() + .6, z, 0, .035, 0);
        }
    }

    private record Emission(ClientLevel level, AbstractClientPlayer player, long tick, int rgb,
                            double phase, double x, double y, double z) {
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
                if (!shouldEmit(player.getUUID().toString(), bit, AppearanceConfig.get().uniquenessParticleIntensity)) continue;
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

    /**
     * A held uniqueness settles into its pathway sigil after three seconds without walking.
     * Dust particles naturally fade; when movement resumes this method stops feeding them, so
     * the symbol dissolves instead of popping off. The masks are original low-resolution
     * interpretations of the official pathway-symbol vocabulary, not copied wiki artwork.
     */
    private static void emitStationarySigil(ClientLevel level, AbstractClientPlayer player, String pathway,
                                            int settledTicks) {
        long mask = GLYPH_MASKS.getOrDefault(pathway, 0L);
        int rgb = accent(pathway);
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);
        // Behind the shoulders, so the sigil reads as a quiet aura rather than a face overlay.
        double baseX = player.getX() + Math.sin(yawRad) * 0.92;
        double baseZ = player.getZ() - Math.cos(yawRad) * 0.92;
        double baseY = player.getY() + 1.7;
        float size = Math.min(0.58f, 0.22f + settledTicks * 0.018f);
        double cell = 0.19;

        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int bit = row * 5 + col;
                if (((mask >> bit) & 1L) == 0L) continue;
                if (!shouldEmit(player.getUUID().toString(), bit, AppearanceConfig.get().uniquenessParticleIntensity)) continue;
                double horizontal = (col - 2) * cell;
                double vertical = (2 - row) * cell;
                level.addParticle(new DustParticleOptions(rgb, size),
                        baseX + rightX * horizontal, baseY + vertical,
                        baseZ + rightZ * horizontal, 0.0, 0.0, 0.0);
            }
        }
        // Keep the centre empty. Billboard particles such as END_ROD become an opaque white
        // sprite from the front camera and can cover the holder's face even when placed behind
        // the model; the dust matrix remains readable in darkness without that hotspot.
    }

    // 5x5 bit patterns (bit = row*5+col, row 0 = top) — rough procedural sigils per pathway
    private static final Map<String, Long> GLYPH_MASKS = Map.of(
            "door", glyph(".###.", "#...#", "#..##", "#.##.", "#...#"),
            "death", glyph("#...#", ".#.#.", "..#..", ".#.#.", "#...#"));

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
            Map.entry("fool", 0xD6D8CC),
            Map.entry("door", 0x5B7FE6),
            Map.entry("sun", 0xFFE55C),
            Map.entry("tyrant", 0x4AA3FF),
            Map.entry("demoness", 0x9EBEC5),
            Map.entry("priest", 0xFF6B35),
            Map.entry("error", 0xDBC385),
            Map.entry("tower", 0x99AABB),
            Map.entry("visionary", 0xD2D4CE),
            Map.entry("hanged", 0xA34448),
            Map.entry("darkness", 0x7887AB),
            Map.entry("death", 0xC8D0E8),
            Map.entry("giant", 0xE88B2A),
            Map.entry("paragon", 0x79A7B6),
            Map.entry("hermit", 0x8855CC),
            Map.entry("fortune", 0xC3D2D3),
            Map.entry("chained", 0x888899),
            Map.entry("abyss", 0x678C91),
            Map.entry("justiciar", 0xEEDD88),
            Map.entry("emperor", 0xDD9922),
            Map.entry("moon", 0xCF4961),
            Map.entry("mother", 0x7FC96B));
}

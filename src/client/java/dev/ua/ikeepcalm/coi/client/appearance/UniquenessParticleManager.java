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
 * <p>Production pathway state comes from an authoritative {@code uniqueness:<pathway>}
 * marker in the appearance payload. Development builds may override it from the F8 screen.</p>
 */
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
    }

    public static String resolvePathway(AbstractClientPlayer player) {
        String uuid = player.getUUID().toString();
        String debug = debugPathwayByUuid.get(uuid);
        if (debug != null) {
            return debug;
        }
        for (String traitId : ClientAppearanceState.getTraits(uuid)) {
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
        tickCounter++;
        if ((tickCounter & 1) == 1) {
            return; // half tick rate
        }
        AppearanceConfig.Settings settings = AppearanceConfig.get();
        if (!settings.enableUniquenessEffects) {
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

            tracked.add(uuid);
            long emissionTick = tickCounter / 2;
            boolean moving = emitTrail(level, player, uuid, pathway, settings.uniquenessParticleIntensity, emissionTick);
            int stillFor = moving ? 0 : stationaryTicks.merge(uuid, 1, Integer::sum);
            if (stillFor >= STATIONARY_SIGIL_TICKS && stillFor % 2 == 0) {
                emitStationarySigil(level, player, pathway, stillFor - STATIONARY_SIGIL_TICKS);
            }
            if (shouldEmit(uuid, emissionTick, settings.uniquenessParticleIntensity)) {
                emit(level, player, pathway, emissionTick);
            }
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
        double backX = x - Math.sin(yawRad) * 0.35;
        double backZ = z + Math.cos(yawRad) * 0.35;
        if (shouldEmit(uuid, emissionTick, intensity)) {
            level.addParticle(new DustParticleOptions(rgb, 0.7f),
                    backX, y + 0.12, backZ, -dx * 0.4, 0.015, -dz * 0.4);
        }
        return true;
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
        if (glyphTick % GLYPH_PERIOD_TICKS == 0 && !"giant".equals(pathway)) {
            emitGlyph(level, player, pathway, rgb);
        }

        Emission emission = new Emission(
                level, player, emissionTick, rgb, phase,
                player.getX(), player.getY(), player.getZ());

        switch (pathway) {
            case "fool" -> emitFool(emission);
            case "door" -> emitDoor(emission);
            case "error" -> emitError(emission);
            case "visionary" -> emitVisionary(emission);
            case "sun" -> emitSun(emission);
            case "hanged" -> emitHanged(emission);
            case "tyrant" -> emitTyrant(emission);
            case "demoness" -> emitDemoness(emission);
            case "abyss" -> emitAbyss(emission);
            case "chained" -> emitChained(emission);
            case "mother" -> emitMother(emission);
            case "moon" -> emitMoon(emission);
            case "priest" -> emitPriest(emission);
            case "justiciar" -> emitJusticiar(emission);
            case "giant" -> emitSettingSun(level, player, rgb, emissionTick);
            case "darkness" -> emitDarkness(emission);
            case "death" -> emitDeath(emission);
            case "hermit" -> emitHermit(emission);
            case "fortune" -> emitFortune(emission);
            case "emperor" -> emitEmperor(emission);
            case "paragon" -> emitParagon(emission);
            case "tower" -> emitTower(emission);
            default -> throw new IllegalArgumentException("Unknown uniqueness pathway: " + pathway);
        }
    }

    private static void emitFool(Emission e) {
        double angle = e.tick() * 0.14 + e.phase() * Math.PI * 2;
        for (int index = 0; index < 2; index++) {
            double a = angle + index * Math.PI;
            e.level().addParticle(new DustParticleOptions(e.rgb(), 1.7f),
                    e.x() + Math.cos(a) * 0.55, e.y() + 2.55 + Math.sin(e.tick() * 0.31 + index) * 0.08,
                    e.z() + Math.sin(a) * 0.55, 0.0, 0.004, 0.0);
        }
        if (e.player().getRandom().nextFloat() < 0.25f) {
            e.level().addParticle(ParticleTypes.WHITE_ASH, e.x(), e.y() + 2.3, e.z(), 0.0, 0.01, 0.0);
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

    private static void emitError(Emission e) {
        var random = e.player().getRandom();
        double jitterX = (random.nextDouble() - 0.5) * 1.3;
        double jitterZ = (random.nextDouble() - 0.5) * 1.3;
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.0f),
                e.x() + jitterX, e.y() + 1.0 + random.nextDouble() * 1.3, e.z() + jitterZ, 0.0, 0.0, 0.0);
        if (random.nextFloat() < 0.3f) {
            e.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    e.x() + jitterX, e.y() + 1.6, e.z() + jitterZ, 0.0, 0.0, 0.0);
        }
    }

    private static void emitVisionary(Emission e) {
        double angle = e.tick() * 0.26 + e.phase() * Math.PI * 2;
        e.level().addParticle(ParticleTypes.ENCHANT,
                e.x() + Math.cos(angle) * 0.5, e.y() + 2.1 + Math.sin(angle * 1.5) * 0.22,
                e.z() + Math.sin(angle) * 0.5, 0.0, 0.012, 0.0);
        if (e.player().getRandom().nextFloat() < 0.12f) {
            e.level().addParticle(ParticleTypes.WITCH, e.x(), e.y() + 2.3, e.z(), 0.0, 0.01, 0.0);
        }
    }

    private static void emitSun(Emission e) {
        double angle = e.tick() * 0.18 + e.phase() * Math.PI * 2;
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.25f),
                e.x() + Math.cos(angle) * 0.9, e.y() + 1.35, e.z() + Math.sin(angle) * 0.9,
                0.0, 0.01, 0.0);
        if (e.tick() % 2 == 0) {
            e.level().addParticle(ParticleTypes.GLOW, e.x(),
                    e.y() + 0.6 + e.player().getRandom().nextDouble() * 1.4, e.z(), 0.0, 0.02, 0.0);
        }
    }

    private static void emitHanged(Emission e) {
        var random = e.player().getRandom();
        if (e.tick() % 2 == 0) {
            e.level().addParticle(ParticleTypes.SOUL,
                    e.x() + (random.nextDouble() - 0.5) * 1.1, e.y() + 2.4,
                    e.z() + (random.nextDouble() - 0.5) * 1.1, 0.0, -0.03, 0.0);
        }
        e.level().addParticle(new DustParticleOptions(e.rgb(), 0.9f),
                e.x(), e.y() + 1.6, e.z(), 0.0, -0.01, 0.0);
    }

    private static void emitTyrant(Emission e) {
        double angle = e.tick() * 0.34 + e.phase() * Math.PI * 2;
        var random = e.player().getRandom();
        e.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                e.x() + Math.cos(angle) * 0.8, e.y() + 0.9 + random.nextDouble() * 1.4,
                e.z() + Math.sin(angle) * 0.8, 0.0, 0.0, 0.0);
        if (e.tick() % 3 == 0) {
            e.level().addParticle(ParticleTypes.CLOUD, e.x(), e.y() + 2.2, e.z(), 0.0, 0.005, 0.0);
        }
    }

    private static void emitDemoness(Emission e) {
        double angle = -e.tick() * 0.22 + e.phase() * Math.PI * 2;
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.05f),
                e.x() + Math.cos(angle) * 0.7, e.y() + 1.5 + Math.sin(angle * 2.0) * 0.3,
                e.z() + Math.sin(angle) * 0.7, 0.0, 0.0, 0.0);
        if (e.player().getRandom().nextFloat() < 0.05f) {
            e.level().addParticle(ParticleTypes.HEART, e.x(), e.y() + 2.2, e.z(), 0.0, 0.0, 0.0);
        }
    }

    private static void emitAbyss(Emission e) {
        var random = e.player().getRandom();
        if (e.tick() % 2 == 0) {
            e.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                    e.x() + (random.nextDouble() - 0.5) * 0.9, e.y() + 0.2 + random.nextDouble() * 1.6,
                    e.z() + (random.nextDouble() - 0.5) * 0.9, 0.0, 0.015, 0.0);
        }
        e.level().addParticle(ParticleTypes.ASH, e.x(), e.y() + 1.8, e.z(), 0.0, -0.01, 0.0);
    }

    private static void emitChained(Emission e) {
        double angle = e.tick() * 0.32 + e.phase() * Math.PI * 2;
        for (int side = 0; side < 2; side++) {
            double linkAngle = angle + side * Math.PI;
            e.level().addParticle(new DustParticleOptions(e.rgb(), 1.35f),
                    e.x() + Math.cos(linkAngle) * 0.78,
                    e.y() + 1.15 + Math.sin(angle * 0.7 + side * Math.PI) * 0.45,
                    e.z() + Math.sin(linkAngle) * 0.78, 0.0, -0.008, 0.0);
        }
        if (e.player().getRandom().nextFloat() < 0.3f) {
            e.level().addParticle(ParticleTypes.SCULK_SOUL, e.x(), e.y() + 1.4, e.z(), 0.0, 0.01, 0.0);
        }
    }

    private static void emitMother(Emission e) {
        var random = e.player().getRandom();
        if (e.tick() % 2 == 0) {
            e.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR,
                    e.x() + (random.nextDouble() - 0.5) * 1.2, e.y() + 0.4 + random.nextDouble() * 1.8,
                    e.z() + (random.nextDouble() - 0.5) * 1.2, 0.0, 0.005, 0.0);
        }
        e.level().addParticle(new DustParticleOptions(e.rgb(), 0.8f),
                e.x(), e.y() + 1.1, e.z(), 0.0, 0.008, 0.0);
    }

    private static void emitMoon(Emission e) {
        double angle = e.tick() * 0.16 + e.phase() * Math.PI * 2;
        for (int side = 0; side < 2; side++) {
            double orbit = angle + side * Math.PI;
            e.level().addParticle(new DustParticleOptions(e.rgb(), 1.35f),
                    e.x() + Math.cos(orbit) * 1.0, e.y() + 1.3 + Math.sin(orbit) * 0.62,
                    e.z() + Math.sin(orbit) * 1.0, 0.0, 0.008, 0.0);
        }
        e.level().addParticle(new DustParticleOptions(0xE8F2FF, 1.1f),
                e.x() + Math.cos(angle * 0.5) * 0.32, e.y() + 2.45,
                e.z() + Math.sin(angle * 0.5) * 0.32, 0.0, -0.004, 0.0);
        if (e.player().getRandom().nextFloat() < 0.25f) {
            e.level().addParticle(ParticleTypes.SNOWFLAKE, e.x(), e.y() + 2.4, e.z(), 0.0, -0.01, 0.0);
        }
    }

    private static void emitPriest(Emission e) {
        var random = e.player().getRandom();
        if (e.tick() % 2 == 0) {
            e.level().addParticle(ParticleTypes.SMALL_FLAME,
                    e.x() + (random.nextDouble() - 0.5) * 0.8, e.y() + 0.3 + random.nextDouble() * 1.5,
                    e.z() + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.012, 0.0);
        }
        e.level().addParticle(new DustParticleOptions(e.rgb(), 0.85f),
                e.x(), e.y() + 1.7, e.z(), 0.0, 0.006, 0.0);
    }

    private static void emitJusticiar(Emission e) {
        double angle = e.tick() * 0.15 + e.phase() * Math.PI * 2;
        for (int side = -1; side <= 1; side += 2) {
            e.level().addParticle(new DustParticleOptions(e.rgb(), 1.35f),
                    e.x() + Math.cos(angle) * side * 0.95, e.y() + 1.35,
                    e.z() + Math.sin(angle) * side * 0.95, 0.0, 0.012, 0.0);
        }
        e.level().addParticle(new DustParticleOptions(0xFFF2A8, 1.15f),
                e.x(), e.y() + 2.15, e.z(), 0.0, 0.008, 0.0);
        if (e.player().getRandom().nextFloat() < 0.3f) {
            e.level().addParticle(ParticleTypes.ENCHANT, e.x(), e.y() + 2.0, e.z(), 0.0, 0.01, 0.0);
        }
    }

    private static void emitDarkness(Emission e) {
        var random = e.player().getRandom();
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.3f),
                e.x() + (random.nextDouble() - 0.5) * 1.1, e.y() + 1.9,
                e.z() + (random.nextDouble() - 0.5) * 1.1, 0.0, -0.004, 0.0);
        if (random.nextFloat() < 0.3f) {
            e.level().addParticle(ParticleTypes.ASH, e.x(), e.y() + 2.5, e.z(), 0.0, -0.02, 0.0);
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
    }

    private static void emitHermit(Emission e) {
        double angle = e.tick() * 0.11 + e.phase() * Math.PI * 2;
        e.level().addParticle(new DustParticleOptions(e.rgb(), 0.95f),
                e.x() + Math.cos(angle) * 1.1, e.y() + 0.5 + (e.tick() % 20) * 0.09,
                e.z() + Math.sin(angle) * 1.1, 0.0, 0.004, 0.0);
    }

    private static void emitFortune(Emission e) {
        double angle = -e.tick() * 0.28 + e.phase() * Math.PI * 2;
        e.level().addParticle(ParticleTypes.GLOW,
                e.x() + Math.cos(angle) * 0.7, e.y() + 0.8 + (e.tick() % 24) * 0.07,
                e.z() + Math.sin(angle) * 0.7, 0.0, 0.0, 0.0);
        if (e.player().getRandom().nextFloat() < 0.2f) {
            e.level().addParticle(new DustParticleOptions(e.rgb(), 0.8f),
                    e.x(), e.y() + 1.6, e.z(), 0.0, 0.01, 0.0);
        }
    }

    private static void emitEmperor(Emission e) {
        double angle = e.tick() * 0.2 + e.phase() * Math.PI * 2;
        double snappedAngle = Math.round(angle / (Math.PI / 2)) * (Math.PI / 2);
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.0f),
                e.x() + Math.cos(snappedAngle) * 0.7, e.y() + 0.3 + (e.tick() % 26) * 0.075,
                e.z() + Math.sin(snappedAngle) * 0.7, 0.0, 0.01, 0.0);
        if (e.player().getRandom().nextFloat() < 0.12f) {
            e.level().addParticle(ParticleTypes.ENCHANT, e.x(), e.y() + 2.1, e.z(), 0.0, 0.0, 0.0);
        }
    }

    private static void emitParagon(Emission e) {
        var random = e.player().getRandom();
        double angle = e.tick() * 0.38 + e.phase() * Math.PI * 2;
        e.level().addParticle(new DustParticleOptions(e.rgb(), 1.4f),
                e.x() + Math.cos(angle) * 0.85, e.y() + 1.35,
                e.z() + Math.sin(angle) * 0.85, 0.0, 0.018, 0.0);
        for (int spark = 0; spark < 2; spark++) {
            e.level().addParticle(ParticleTypes.ELECTRIC_SPARK,
                    e.x() + (random.nextDouble() - 0.5) * 1.15, e.y() + 0.55 + random.nextDouble() * 1.75,
                    e.z() + (random.nextDouble() - 0.5) * 1.15, 0.0, 0.025, 0.0);
        }
        if (e.tick() % 3 == 0) {
            e.level().addParticle(ParticleTypes.END_ROD, e.x(), e.y() + 2.25, e.z(), 0.0, 0.015, 0.0);
        }
    }

    private static void emitTower(Emission e) {
        var random = e.player().getRandom();
        double rise = (e.tick() % 24) / 24.0;
        for (int column = 0; column < 3; column++) {
            double angle = column * Math.PI * 2 / 3 + e.tick() * 0.08;
            e.level().addParticle(new DustParticleOptions(e.rgb(), 1.25f),
                    e.x() + Math.cos(angle) * 0.42, e.y() + 0.2 + ((rise + column / 3.0) % 1.0) * 2.5,
                    e.z() + Math.sin(angle) * 0.42, 0.0, 0.035, 0.0);
        }
        if (random.nextFloat() < 0.2f) {
            e.level().addParticle(ParticleTypes.CLOUD, e.x(), e.y() + 2.6, e.z(), 0.0, 0.01, 0.0);
        }
    }

    private record Emission(ClientLevel level, AbstractClientPlayer player, long tick, int rgb,
                            double phase, double x, double y, double z) {
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
                double horizontal = (col - 2) * cell;
                double vertical = (2 - row) * cell;
                level.addParticle(new DustParticleOptions(rgb, size),
                        baseX + rightX * horizontal, baseY + vertical,
                        baseZ + rightZ * horizontal, 0.0, 0.0, 0.0);
            }
        }
        // A restrained centre light makes the symbol legible in darkness without becoming a beacon.
        if (settledTicks % 6 == 0) {
            level.addParticle(ParticleTypes.END_ROD, baseX, baseY, baseZ, 0.0, 0.002, 0.0);
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

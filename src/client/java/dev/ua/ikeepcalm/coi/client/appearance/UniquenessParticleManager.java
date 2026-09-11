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
    private static final Map<String, Motion> motions = new HashMap<>();
    private static final Map<String, Integer> stationaryTicks = new HashMap<>();
    private static int tickCounter = 0;
    private static ClientLevel lastLevel;
    private static long lastGameTick = Long.MIN_VALUE;

    private UniquenessParticleManager() {
    }

    public static int idleTicks(String uuid) {
        return stationaryTicks.getOrDefault(uuid, 0) * 2;
    }

    private static final Set<String> ABILITY_PATHWAYS = Set.of("tyrant", "sun", "giant", "priest", "darkness", "emperor");

    public static Set<String> visualPathways(String uuid) {
        Set<String> paths = new java.util.LinkedHashSet<>();
        var traits = ClientAppearanceState.getTraits(uuid);
        String unique = resolvePathway(uuid, traits);
        if (unique != null && AppearanceConfig.shouldRenderUniqueness(uuid)) paths.add(unique);
        for (String trait : traits) {
            if (trait.startsWith("authority:")) paths.add("justiciar");
            if (trait.startsWith("ability:") && ABILITY_PATHWAYS.contains(trait.substring(8))) paths.add(trait.substring(8));
        }
        return paths;
    }

    public record Movement(float sway, float lag, float activity, double spin, float proximity, boolean water, float bloom, float wilt, float groundGap) {
        public static final Movement STILL = new Movement(0, 0, 0, 0, 0, false, .15f, 0, 5);
    }

    public static Movement movement(String uuid, float partialTick) {
        Motion m = motions.get(uuid);
        if (m == null) return Movement.STILL;
        float a = Math.clamp(((tickCounter & 1) + partialTick) / 2, 0, 1);
        return new Movement(m.oldSway+(m.sway-m.oldSway)*a, m.oldLag+(m.lag-m.oldLag)*a,
                m.activity, m.oldSpin+(m.spin-m.oldSpin)*a, m.proximity, m.water, m.bloom, m.wilt, m.groundGap);
    }

    /** Tick-owned damped motion, bounded to visible players and cleared with ownership/world changes. */
    private static final class Motion {
        double x, y, z, dx, dz, spin, oldSpin, boltX, boltY, boltZ;
        long strike = Long.MIN_VALUE;
        int strikePeriod;
        net.minecraft.world.phys.Vec3 waterTarget;
        float yaw, sway, lag, swayVelocity, lagVelocity, oldSway, oldLag, activity, proximity, bloom=.15f, wilt, groundGap=5;
        boolean initialized, water, nearby;
        int scans;
        void update(AbstractClientPlayer player, Set<String> pathways) {
            dx = player.getX()-x; dz = player.getZ()-z;
            double dy = player.getY()-y;
            boolean reset = !initialized || dx*dx+dy*dy+dz*dz>16;
            float turn = reset ? 0 : net.minecraft.util.Mth.wrapDegrees(player.yBodyRot-yaw)/2;
            if (reset) {
                dx=dz=0; dy=0; sway=lag=swayVelocity=lagVelocity=activity=0;
                initialized=true;
            }
            int flower=0;
            for(String trait:ClientAppearanceState.getTraits(player.getUUID().toString()))
                if(trait.startsWith("flower-phase:") && trait.length()==14 && trait.charAt(13)>='0' && trait.charAt(13)<='7')
                    flower=trait.charAt(13)-'0';
            float targetBloom=flower<2?.15f+flower/7f:flower<5?1:Math.max(.12f,1-(flower-4)*.28f);
            float targetWilt=Math.max(0,(flower-4)/3f);
            bloom=reset?targetBloom:bloom+(targetBloom-bloom)*.03f;
            wilt=reset?targetWilt:wilt+(targetWilt-wilt)*.03f;
            oldSway=sway; oldLag=lag; oldSpin=spin;
            float target = (float)Math.clamp(Math.hypot(dx,dz)/.56,0,1);
            activity += (target-activity)*.25f;
            float targetSway=Math.clamp(-turn*.12f,-3,3);
            float targetLag=(float)Math.clamp(activity*2+dy*3,-2,4);
            for(int step=0;step<2;step++) {
                swayVelocity+=(targetSway-sway)*.055f-swayVelocity*.36f;
                lagVelocity+=(targetLag-lag)*.05f-lagVelocity*.34f;
                sway=Math.clamp(sway+swayVelocity,-4,4);
                lag=Math.clamp(lag+lagVelocity,-3,5);
            }
            spin+=.003+activity*.018;
            if(spin>Math.PI*2) {spin-=Math.PI*2;oldSpin-=Math.PI*2;}
            if(scans++%10==0) {
                water=false;
                if(pathways.contains("tyrant")) {
                    int wet=0;double closest=Double.MAX_VALUE;
                    var base=player.blockPosition();
                    for(int xx=-3;xx<=3;xx++) for(int zz=-3;zz<=3;zz++) for(int yy=-1;yy<=1;yy++) {
                        var pos=base.offset(xx,yy,zz);var fluid=player.level().getFluidState(pos);
                        if(!fluid.is(net.minecraft.tags.FluidTags.WATER))continue;
                        wet++;
                        var surface=new net.minecraft.world.phys.Vec3(pos.getX()+.5,pos.getY()+fluid.getHeight(player.level(),pos),pos.getZ()+.5);
                        double distance=surface.distanceToSqr(player.position());
                        if(distance<closest){closest=distance;waterTarget=surface;}
                    }
                    water=wet>=3;
                }
                nearby=pathways.contains("abyss") && player.level().players().stream().anyMatch(other ->
                        other!=player && !other.isSpectator() && !other.isInvisible() && !other.isCrouching()
                                && other.distanceToSqr(player)<64 && player.hasLineOfSight(other));
            }
            groundGap=5;
            if(pathways.contains("abyss") && !player.isSwimming() && !player.isFallFlying()) {
                var hit=player.level().clip(new net.minecraft.world.level.ClipContext(
                        player.position().add(0,.1,0),player.position().add(0,-4,0),
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                if(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK)
                    groundGap=(float)Math.max(0,player.getY()-hit.getLocation().y);
            }
            proximity+=(nearby?1-proximity:-proximity)*.12f;
            x=player.getX();y=player.getY();z=player.getZ();yaw=player.yBodyRot;
        }
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
        motions.clear();
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
            motions.clear();
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
        if (!settings.enabled) {
            motions.clear();
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
            if (!AppearanceConfig.shouldRender(uuid)) {
                continue;
            }
            if (player.distanceToSqr(camera) > MAX_DISTANCE_SQ) {
                continue;
            }
            Set<String> pathways = visualPathways(uuid);
            if (pathways.isEmpty()) continue;
            tracked.add(uuid);
            Motion motion = motions.computeIfAbsent(uuid, ignored -> new Motion());
            motion.update(player, pathways);
            long emissionTick = tickCounter / 2;
            boolean moving = motion.dx * motion.dx + motion.dz * motion.dz >= .0004;
            int stillFor = moving ? 0 : stationaryTicks.merge(uuid, 1, Integer::sum);
            if (moving) stationaryTicks.put(uuid, 0);
            for (String pathway : pathways) {
            emitTrail(level, player, uuid, pathway, settings.uniquenessParticleIntensity, emissionTick);
            if (legacyParticles(pathway) && stillFor >= STATIONARY_SIGIL_TICKS && stillFor % 2 == 0) {
                emitStationarySigil(level, player, pathway, stillFor - STATIONARY_SIGIL_TICKS);
            }
            emit(level, player, pathway, emissionTick);
            }
        }

        motions.keySet().retainAll(tracked);
        stationaryTicks.keySet().retainAll(tracked);
    }

    // ------------------------------------------------------------------
    // Emission
    // ------------------------------------------------------------------

    private static boolean emitTrail(ClientLevel level, AbstractClientPlayer player, String uuid, String pathway,
                                     float intensity, long emissionTick) {
        Motion motion = motions.get(uuid);
        double x = player.getX(), y = player.getY(), z = player.getZ();
        double dx = motion.dx, dz = motion.dz;
        if (dx * dx + dz * dz > 16) return false; // A teleport is not a sprint or a footstep.
        if (dx * dx + dz * dz < 0.0004) {
            return false; // trails require movement
        }

        int rgb = accent(pathway);
        float yawRad = player.getYRot() * ((float) Math.PI / 180.0f);
        double backX = x + Math.sin(yawRad) * 0.35;
        double backZ = z - Math.cos(yawRad) * 0.35;
        if (shouldEmit(uuid, emissionTick, intensity)) {
            if (!pathway.equals("tyrant")) level.addParticle(new DustParticleOptions(rgb, legacyParticles(pathway) ? 0.7f : 0.45f),
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
            case "door" -> level.addParticle(new DustParticleOptions(0xDCDDE5, .3f), backX, y + .12, backZ,
                    -dx * 0.3, 0.012, -dz * 0.3);
            case "tyrant" -> {
                level.addParticle(ParticleTypes.SPLASH, backX, y + .06, backZ, 0, .025, 0);
                if (player.isSprinting()) {
                    // Small outward crest perpendicular to travel, never a wall around the body.
                    double length = Math.max(.001, Math.hypot(dx, dz));
                    for (int i = -3; i <= 3; i++) {
                        double side = i * .13;
                        level.addParticle(ParticleTypes.SPLASH, backX - dz / length * side,
                                y + .08 + (1 - Math.abs(i) / 3.0) * .12, backZ + dx / length * side,
                                -dz / length * side * .05, .025, dx / length * side * .05);
                    }
                }
            }
            case "tower", "hermit" -> level.addParticle(ParticleTypes.ENCHANT,
                    backX, y + .1, backZ, (player.getRandom().nextDouble() - .5) * .15, .12,
                    (player.getRandom().nextDouble() - .5) * .15);
            case "priest" -> level.addParticle(ParticleTypes.SMALL_FLAME, backX, y + .08, backZ, 0, .018, 0);
            case "mother" -> level.addParticle(ParticleTypes.HAPPY_VILLAGER, backX, y + .08, backZ, 0, .01, 0);
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
                player.getX(), player.getY(), player.getZ(), pathway);

        if (!shouldEmit(player.getUUID().toString(), emissionTick, AppearanceConfig.get().uniquenessParticleIntensity)) return;
        switch (pathway) {
            case "door" -> { }
            case "death" -> emitDeath(emission);
            case "tyrant" -> { emitAccent(emission); emitLightning(emission); }
            default -> emitAccent(emission);
        }
    }

    private static void emitLightning(Emission e) {
        Motion motion=motions.get(e.player().getUUID().toString());
        boolean active=ClientAppearanceState.hasTrait(e.player().getUUID().toString(),"ability:tyrant");
        int period=active?30:motion.water?65:130;
        if(e.tick()%period>1)return;
        if(motion.strike!=e.tick()/period || motion.strikePeriod!=period) {
            motion.strike=e.tick()/period;motion.strikePeriod=period;
            var target=motion.water && motion.waterTarget!=null?motion.waterTarget:e.player().position();
            motion.boltX=target.x;motion.boltY=target.y;motion.boltZ=target.z;
        }
        long seed=e.player().getUUID().getLeastSignificantBits()+e.tick()/period;
        var random=new java.util.Random(seed);
        double angle=random.nextDouble()*Math.PI*2,radius=.5+random.nextDouble()*.3;
        double bx=motion.water?0:Math.cos(angle)*radius,bz=motion.water?0:Math.sin(angle)*radius;
        double height=e.level().canSeeSky(e.player().blockPosition().above())?3.5:1.2;
        double x=motion.boltX+bx,y=motion.boltY+height,z=motion.boltZ+bz;
        for(int segment=0;segment<9;segment++) {
            double nx=motion.boltX+bx+(random.nextDouble()-.5)*.55;
            double ny=motion.boltY+height*(1-(segment+1)/9.0);
            double nz=motion.boltZ+bz+(random.nextDouble()-.5)*.55;
            lightningStroke(e,x,y,z,nx,ny,nz);
            if(segment==3 || segment==6)
                lightningStroke(e,nx,ny,nz,nx+(random.nextDouble()-.5)*1.1,ny-.35,nz+(random.nextDouble()-.5));
            x=nx;y=ny;z=nz;
        }
        if(motion.water)for(int i=0;i<8;i++) {
            double a=i*Math.PI/4;
            e.level().addParticle(ParticleTypes.SPLASH,x+Math.cos(a)*.65,y+.08,
                    z+Math.sin(a)*.65,Math.cos(a)*.04,.03,Math.sin(a)*.04);
        }
    }

    private static void lightningStroke(Emission e,double x,double y,double z,double xx,double yy,double zz) {
        int count=Math.max(2,(int)(Math.sqrt((xx-x)*(xx-x)+(yy-y)*(yy-y)+(zz-z)*(zz-z))*14));
        for(int i=0;i<=count;i++) {
            double t=i/(double)count;
            e.level().addParticle(ParticleTypes.ELECTRIC_SPARK,x+(xx-x)*t,y+(yy-y)*t,z+(zz-z)*t,0,0,0);
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
        return "death".equals(pathway);
    }

    private static void emitAccent(Emission e) {
        String pathway = e.pathway();
        boolean moving = idleTicks(e.player().getUUID().toString()) < 4;
        if (e.tick() % (moving ? (e.player().isSprinting() ? 1 : 2) : 9) != 0) return;
        double angle = e.tick() * .7 + e.phase() * Math.PI * 2;
        double x = e.x() + Math.cos(angle) * .23, z = e.z() + Math.sin(angle) * .23;
        if ("tyrant".equals(pathway)) return; // Water comes only from actual footsteps.
        if ("priest".equals(pathway)) {
            e.level().addParticle(ParticleTypes.SMALL_FLAME, x, e.y() + .06, z, 0, .018, 0);
        }
        int points = switch (pathway) {
            case "hanged", "fool", "darkness", "emperor" -> 4;
            default -> 1;
        };
        for (int i = 0; i < points; i++) {
            double u = i / 4.0;
            e.level().addParticle(new DustParticleOptions(pathway.equals("demoness") && e.tick()%2==0 ? 0xB84160 : e.rgb(), .28f),
                    x + Math.sin(angle + u * 2) * u * .18, e.y() + .06 + u * .14,
                    z + Math.cos(angle + u * 2) * u * .18, 0, .009, 0);
        }
    }

    private record Emission(ClientLevel level, AbstractClientPlayer player, long tick, int rgb,
                            double phase, double x, double y, double z, String pathway) {
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
            Map.entry("door", 0xE3E1EC),
            Map.entry("sun", 0xFFE55C),
            Map.entry("tyrant", 0x4AA3FF),
            Map.entry("demoness", 0xD97196),
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

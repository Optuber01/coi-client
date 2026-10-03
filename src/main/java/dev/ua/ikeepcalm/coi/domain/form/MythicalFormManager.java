package dev.ua.ikeepcalm.coi.domain.form;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ua.ikeepcalm.coi.domain.ability.model.Pathways;
import dev.ua.ikeepcalm.coi.domain.effect.EffectManager;
import dev.ua.ikeepcalm.coi.domain.effect.visual.SpellImpactEffect;
import dev.ua.ikeepcalm.coi.domain.form.pathway.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players the server has transformed, and the registry of the twenty pathway forms.
 * {@code REGISTERED_FORMS} is fixed at class-init; {@code TRANSFORMED} follows the
 * {@code coi-client:mythical} packets and is cleared on disconnect.
 */
public class MythicalFormManager {

    /** Player UUID to the pathway they are currently transformed into. */
    private static final Map<String, String> TRANSFORMED = new ConcurrentHashMap<>();
    private static final Map<String, MythicalCreatureForm> REGISTERED_FORMS = new ConcurrentHashMap<>();

    static {
        register(new FoolForm());
        register(new DoorForm());
        register(new ErrorForm());
        register(new TowerForm());
        register(new VisionaryForm());
        register(new SunForm());
        register(new TyrantForm());
        register(new HangedForm());
        register(new DarknessForm());
        register(new DeathForm());
        register(new GiantForm());
        register(new PriestForm());
        register(new DemonessForm());
        register(new ParagonForm());
        register(new HermitForm());
        register(new FortuneForm());
        register(new ChainedForm());
        register(new AbyssForm());
        register(new JusticiarForm());
        register(new EmperorForm());
    }

    /**
     * Every spacing variant, so {@code "sea god"}, {@code "sea_god"} and {@code "seagod"} all hit.
     */
    public static void register(MythicalCreatureForm form) {
        String name = form.getPathwayName().toLowerCase();
        REGISTERED_FORMS.put(name, form);
        REGISTERED_FORMS.put(name.replace(" ", ""), form);
        REGISTERED_FORMS.put(name.replace("_", ""), form);
        REGISTERED_FORMS.put(name.replace(" ", "_"), form);
        REGISTERED_FORMS.put(name.replace("_", " "), form);
    }

    /**
     * {@code params} is {@code pathway:<unused>:start|stop}; anything malformed or empty clears
     * the player's form rather than leaving them stuck mid-transformation.
     */
    public static void handlePacket(String targetUuid, String params) {
        if (params == null || params.isBlank()) {
            TRANSFORMED.remove(targetUuid);
            return;
        }
        String[] parts = params.split(":");
        if (parts.length < 3) {
            TRANSFORMED.remove(targetUuid);
            return;
        }
        String pathwayName = parts[0].toLowerCase();
        String action = parts[2].toLowerCase();
        String previous = TRANSFORMED.get(targetUuid);
        if ("stop".equals(action)) {
            TRANSFORMED.remove(targetUuid);
            if (previous != null) {
                triggerTransformVfx(targetUuid, previous, false);
            }
        } else {
            TRANSFORMED.put(targetUuid, pathwayName);
            if (!pathwayName.equals(previous)) {
                triggerTransformVfx(targetUuid, pathwayName, true);
            }
        }
    }

    private static void triggerTransformVfx(String targetUuid, String pathway, boolean starting) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        for (AbstractClientPlayer player : client.level.players()) {
            if (!player.getUUID().toString().equals(targetUuid)) continue;

            Vec3 pos = player.position();
            String hex = String.format("%06X", Pathways.pathwayRgb(pathway));
            String params = String.format(Locale.ROOT,
                    "style=%s,scope=world,x=%.2f,y=%.2f,z=%.2f,color=FFFFFF,accent=%s,intensity=0.9,radius=1.7,duration=850",
                    starting ? "burst" : "ripple", pos.x, pos.y + 0.1, pos.z, hex);
            EffectManager.trigger(SpellImpactEffect.ID, params);

            if (player == client.player) {
                EffectManager.trigger("flash", "color=" + hex + ",intensity=0.3,duration=350");
            }
            return;
        }
    }

    public static boolean isTransformed(AbstractClientPlayer player) {
        if (player == null) return false;
        return TRANSFORMED.containsKey(player.getUUID().toString());
    }

    public static String getForm(String playerUuid) {
        return TRANSFORMED.get(playerUuid);
    }

    public static MythicalCreatureForm getRegisteredForm(String pathway) {
        return pathway == null ? null : REGISTERED_FORMS.get(pathway.toLowerCase());
    }

    /**
     * Deduped by {@link PartialFormSpec#layer()} identity: {@link #register(MythicalCreatureForm)}
     * stores each form under several normalized key variants.
     */
    public static Map<ModelLayerLocation, PartialFormSpec> getPartialForms() {
        Map<ModelLayerLocation, PartialFormSpec> result = new HashMap<>();
        for (MythicalCreatureForm form : new HashSet<>(REGISTERED_FORMS.values())) {
            PartialFormSpec spec = form.partialForm();
            if (spec != null) {
                result.put(spec.layer(), spec);
            }
        }
        return result;
    }

    public static List<String> getRegisteredPathwayNames() {
        return REGISTERED_FORMS.values().stream()
                .map(MythicalCreatureForm::getPathwayName)
                .distinct()
                .sorted()
                .toList();
    }

    /** Drops every tracked transformation; called on disconnect. */
    public static void clearAll() {
        TRANSFORMED.clear();
    }

    private MythicalFormManager() {
    }

    public static void renderFormSubmit(String pathway, AvatarRenderState state, PoseStack.Pose pose, VertexConsumer consumer) {
        MythicalCreatureForm form = REGISTERED_FORMS.get(pathway.toLowerCase());
        if (form != null) {
            form.render(state, pose, consumer);
        }
    }
}

package dev.ua.ikeepcalm.coi.client.appearance;

import dev.ua.ikeepcalm.coi.client.appearance.renderers.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry of every passive appearance trait this client can render, plus the
 * mutually-exclusive families that stop overlapping cosmetics from z-fighting:
 * for each family only the highest-priority active trait actually draws.
 *
 * <p>Priority chains (first entry wins):</p>
 * <ul>
 *     <li>Body: mother → moon → chaos primogenitor → demoness</li>
 *     <li>Hair: long brown → long black → silver → red → blue → black</li>
 *     <li>Eyes: dragon → glowing → red → iron-black → black</li>
 *     <li>Wings: devil → darkness → natural bat; Scales: dragon → water</li>
 *     <li>Physique: giant → masculine; Chained: werewolf → wraith → zombie;
 *         Claws: corrosive → predator; Skin: devil → chitin → stone → wood → pale</li>
 * </ul>
 *
 * <p>Trait ids are the opaque strings the COI server sends via {@code coi-client:appearance};
 * unknown ids are simply never rendered.</p>
 */
public final class AppearanceTraits {

    public record Family(String id, List<String> priorityOrder) {
    }

    public record TraitInfo(String id, String displayName, Family family) {
    }

    public static final Family BODY = new Family("body",
            List.of("mother_traits", "moon_traits", "chaos_traits", "female_traits"));
    public static final Family HAIR = new Family("hair",
            List.of("long_brown_hair", "long_black_hair", "silver_hair", "red_hair", "blue_hair", "black_hair"));
    public static final Family EYES = new Family("eyes",
            List.of("dragon_irises", "glowing_eyes", "red_eyes", "iron_black_eyes", "black_eyes"));
    public static final Family WINGS = new Family("wings",
            List.of("devil_wings", "darkness_wings", "bat_mutation"));
    public static final Family SCALES = new Family("scales",
            List.of("dragon_scales", "water_scales"));
    public static final Family PHYSIQUE = new Family("physique",
            List.of("giant_physique", "masculine_traits"));
    public static final Family CHAINED = new Family("chained",
            List.of("werewolf_traits", "wraith_traits", "zombie_traits"));
    public static final Family CLAWS = new Family("claws",
            List.of("corrosive_claws", "predator_mutation"));
    public static final Family SKIN = new Family("skin",
            List.of("devil_skin", "chitin_mutation", "stone_skin", "wood_skin", "pale_skin"));

    /** Families gated by the "Body enhancements" visibility switch. */
    public static final Set<String> BODY_ALTERING_FAMILIES = Set.of(
            BODY.id(), SKIN.id(), SCALES.id(), PHYSIQUE.id(), CHAINED.id());

    public static final List<Family> FAMILIES = List.of(
            BODY, HAIR, EYES, WINGS, SCALES, PHYSIQUE, CHAINED, CLAWS, SKIN);

    private static final Map<String, Family> FAMILY_BY_TRAIT = new HashMap<>();

    static {
        for (Family family : FAMILIES) {
            for (String traitId : family.priorityOrder()) {
                FAMILY_BY_TRAIT.put(traitId, family);
            }
        }
    }

    /** Debug UI listing — display names are not localized, matching the other debug screens. */
    public static final List<TraitInfo> TRAITS = List.of(
            new TraitInfo("female_traits", "Demoness Figure", BODY),
            new TraitInfo("mother_traits", "Mother Figure", BODY),
            new TraitInfo("moon_traits", "Moon Figure", BODY),
            new TraitInfo("chaos_traits", "Chaos Figure", BODY),
            new TraitInfo("demoness_ears", "Cat Ears", null),
            new TraitInfo("long_brown_hair", "Long Brown Hair", HAIR),
            new TraitInfo("long_black_hair", "Long Black Hair", HAIR),
            new TraitInfo("silver_hair", "Silver Hair", HAIR),
            new TraitInfo("red_hair", "Red Hair", HAIR),
            new TraitInfo("blue_hair", "Blue Hair", HAIR),
            new TraitInfo("black_hair", "Black Hair", HAIR),
            new TraitInfo("dragon_irises", "Dragon Irises", EYES),
            new TraitInfo("glowing_eyes", "Glowing Eyes", EYES),
            new TraitInfo("red_eyes", "Red Eyes", EYES),
            new TraitInfo("iron_black_eyes", "Iron-Black Eyes", EYES),
            new TraitInfo("black_eyes", "Black Eyes", EYES),
            new TraitInfo("fangs", "Fangs", null),
            new TraitInfo("dragon_scales", "Dragon Scales", SCALES),
            new TraitInfo("water_scales", "Water Scales", SCALES),
            new TraitInfo("pale_skin", "Pale Skin", SKIN),
            new TraitInfo("devil_skin", "Devil Armor Skin", SKIN),
            new TraitInfo("wood_skin", "Wood Skin", SKIN),
            new TraitInfo("stone_skin", "Stone Skin", SKIN),
            new TraitInfo("chitin_mutation", "Chitin Mutation", SKIN),
            new TraitInfo("regrowth_mutation", "Regrowth Vines", null),
            new TraitInfo("werewolf_traits", "Werewolf Traits", CHAINED),
            new TraitInfo("wraith_traits", "Wraith Traits", CHAINED),
            new TraitInfo("zombie_traits", "Zombie Traits", CHAINED),
            new TraitInfo("devil_wings", "Devil Wings", WINGS),
            new TraitInfo("darkness_wings", "Darkness Wings", WINGS),
            new TraitInfo("bat_mutation", "Bat Wings", WINGS),
            new TraitInfo("corrosive_claws", "Corrosive Claws", CLAWS),
            new TraitInfo("predator_mutation", "Predator Claws", CLAWS),
            new TraitInfo("giant_physique", "Giant Physique", PHYSIQUE),
            new TraitInfo("masculine_traits", "Masculine Frame", PHYSIQUE),
            new TraitInfo("lifeless_aura", "Lifeless Aura", null),
            new TraitInfo("horns", "Abyss Horns", null),
            new TraitInfo("mushroom", "Head Mushroom", null)
    );

    private static final List<AppearanceTraitRenderer> RENDERERS = List.of(
            new FemaleTraitsRenderer("mother_traits", 1.38f),
            new FemaleTraitsRenderer("moon_traits", 1.28f),
            new FemaleTraitsRenderer("chaos_traits", 1.18f),
            new FemaleTraitsRenderer("female_traits", 1.08f),
            new DemonessEarsRenderer(),
            new HairTraitRenderer("long_brown_hair", HairTraitRenderer.Style.LONG, 0.25f, 0.11f, 0.045f),
            new HairTraitRenderer("long_black_hair", HairTraitRenderer.Style.LONG, 0.025f, 0.018f, 0.03f),
            new HairTraitRenderer("silver_hair", HairTraitRenderer.Style.SHORT, 0.72f, 0.76f, 0.82f),
            new HairTraitRenderer("red_hair", HairTraitRenderer.Style.SHORT, 0.58f, 0.025f, 0.025f),
            new HairTraitRenderer("blue_hair", HairTraitRenderer.Style.SHORT, 0.035f, 0.23f, 0.62f),
            new HairTraitRenderer("black_hair", HairTraitRenderer.Style.SHORT, 0.018f, 0.014f, 0.025f),
            new EyeTraitRenderer("dragon_irises", EyeTraitRenderer.Style.SLIT, 0.88f, 0.67f, 0.12f, true),
            new EyeTraitRenderer("glowing_eyes", EyeTraitRenderer.Style.NORMAL, 0.66f, 0.16f, 0.92f, true),
            new EyeTraitRenderer("red_eyes", EyeTraitRenderer.Style.NORMAL, 0.92f, 0.025f, 0.035f, true),
            new EyeTraitRenderer("iron_black_eyes", EyeTraitRenderer.Style.NORMAL, 0.025f, 0.018f, 0.018f, false),
            new EyeTraitRenderer("black_eyes", EyeTraitRenderer.Style.NORMAL, 0.005f, 0.005f, 0.008f, false),
            new FangsRenderer(),
            new ScaleTraitRenderer("dragon_scales", ScaleTraitRenderer.Style.DRAGON),
            new ScaleTraitRenderer("water_scales", ScaleTraitRenderer.Style.WATER),
            new SkinTraitRenderer("pale_skin", SkinTraitRenderer.Style.PALE),
            new SkinTraitRenderer("devil_skin", SkinTraitRenderer.Style.DEVIL_ARMOR),
            new SkinTraitRenderer("wood_skin", SkinTraitRenderer.Style.WOOD),
            new SkinTraitRenderer("stone_skin", SkinTraitRenderer.Style.STONE),
            new SkinTraitRenderer("chitin_mutation", SkinTraitRenderer.Style.CHITIN),
            new SkinTraitRenderer("regrowth_mutation", SkinTraitRenderer.Style.REGROWTH),
            new BeastTraitRenderer("werewolf_traits"),
            new SkinTraitRenderer("wraith_traits", SkinTraitRenderer.Style.WRAITH),
            new SkinTraitRenderer("zombie_traits", SkinTraitRenderer.Style.ZOMBIE),
            new WingTraitRenderer("devil_wings", WingTraitRenderer.Style.DEVIL),
            new WingTraitRenderer("darkness_wings", WingTraitRenderer.Style.ILLUSORY),
            new WingTraitRenderer("bat_mutation", WingTraitRenderer.Style.NATURAL),
            new ClawTraitRenderer("corrosive_claws", ClawTraitRenderer.Style.CORROSIVE),
            new ClawTraitRenderer("predator_mutation", ClawTraitRenderer.Style.PREDATOR),
            new PhysiqueTraitRenderer("giant_physique", PhysiqueTraitRenderer.Style.GIANT),
            new PhysiqueTraitRenderer("masculine_traits", PhysiqueTraitRenderer.Style.MASCULINE),
            new AuraTraitRenderer("lifeless_aura"),
            new HornsTraitRenderer(),
            new MushroomTraitRenderer()
    );

    private static final Map<String, AppearanceTraitRenderer> RENDERER_BY_TRAIT = new HashMap<>();

    static {
        for (AppearanceTraitRenderer renderer : RENDERERS) {
            RENDERER_BY_TRAIT.put(renderer.traitId(), renderer);
        }
    }

    private AppearanceTraits() {
    }

    public static Family familyOf(String traitId) {
        return FAMILY_BY_TRAIT.get(traitId);
    }

    public static int traitCount() {
        return TRAITS.size();
    }

    /**
     * Resolves which renderers draw for a player carrying {@code activeTraits}: every
     * independent trait passes through, and within each family only the highest-priority
     * active trait is kept. Order follows {@link #TRAITS} for stable debug layout.
     */
    public static List<AppearanceTraitRenderer> resolve(Set<String> activeTraits) {
        List<AppearanceTraitRenderer> result = new ArrayList<>();
        if (activeTraits.isEmpty()) {
            return result;
        }

        Map<String, String> winnerByFamily = new HashMap<>();
        for (Family family : FAMILIES) {
            for (String candidate : family.priorityOrder()) {
                if (activeTraits.contains(candidate)) {
                    winnerByFamily.put(family.id(), candidate);
                    break;
                }
            }
        }

        for (AppearanceTraitRenderer renderer : RENDERERS) {
            String traitId = renderer.traitId();
            if (!activeTraits.contains(traitId)) {
                continue;
            }
            Family family = FAMILY_BY_TRAIT.get(traitId);
            if (family != null && !traitId.equals(winnerByFamily.get(family.id()))) {
                continue;
            }
            result.add(renderer);
        }
        return result;
    }

    public static AppearanceTraitRenderer rendererFor(String traitId) {
        return RENDERER_BY_TRAIT.get(traitId);
    }
}

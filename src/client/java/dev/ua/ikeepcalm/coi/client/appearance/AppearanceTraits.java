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
 *         Claws: corrosive → predator</li>
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

    public static final List<Family> FAMILIES = List.of(
            BODY, HAIR, EYES, WINGS, SCALES, PHYSIQUE, CHAINED, CLAWS);

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
            new TraitInfo("pale_skin", "Pale Skin", null),
            new TraitInfo("devil_skin", "Devil Armor Skin", null),
            new TraitInfo("wood_skin", "Wood Skin", null),
            new TraitInfo("stone_skin", "Stone Skin", null),
            new TraitInfo("chitin_mutation", "Chitin Mutation", null),
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
            new MotherTraitsRenderer(),
            new MoonTraitsRenderer(),
            new ChaosTraitsRenderer(),
            new FemaleTraitsRenderer(),
            new DemonessEarsRenderer(),
            new LongBrownHairRenderer(),
            new LongBlackHairRenderer(),
            new SilverHairRenderer(),
            new RedHairRenderer(),
            new BlueHairRenderer(),
            new BlackHairRenderer(),
            new DragonIrisesRenderer(),
            new GlowingEyesRenderer(),
            new RedEyesRenderer(),
            new IronBlackEyesRenderer(),
            new BlackEyesRenderer(),
            new FangsRenderer(),
            new DragonScalesRenderer(),
            new WaterScalesRenderer(),
            new PaleSkinRenderer(),
            new DevilSkinRenderer(),
            new WoodSkinRenderer(),
            new StoneSkinRenderer(),
            new ChitinMutationRenderer(),
            new RegrowthMutationRenderer(),
            new WerewolfTraitsRenderer(),
            new WraithTraitsRenderer(),
            new ZombieTraitsRenderer(),
            new DevilWingsRenderer(),
            new DarknessWingsRenderer(),
            new BatMutationRenderer(),
            new CorrosiveClawsRenderer(),
            new PredatorMutationRenderer(),
            new GiantPhysiqueRenderer(),
            new MasculineTraitsRenderer(),
            new LifelessAuraRenderer(),
            new HornsTraitRenderer(),
            new MushroomTraitRenderer()
    );

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

        for (int index = 0; index < RENDERERS.size(); index++) {
            AppearanceTraitRenderer renderer = RENDERERS.get(index);
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
}

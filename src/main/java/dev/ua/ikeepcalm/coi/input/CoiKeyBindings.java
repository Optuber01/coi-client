package dev.ua.ikeepcalm.coi.input;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import dev.ua.ikeepcalm.coi.config.HudConfig;
import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityCategories;
import dev.ua.ikeepcalm.coi.domain.ability.model.AbilityInfo;
import dev.ua.ikeepcalm.coi.domain.ability.service.AbilityBindings;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.NotificationState;
import dev.ua.ikeepcalm.coi.domain.beyonder.model.SheetState;
import dev.ua.ikeepcalm.coi.domain.menu.service.MenuState;
import dev.ua.ikeepcalm.coi.hud.overlay.AbilityOverlay;
import dev.ua.ikeepcalm.coi.network.ServerCapabilities;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.AbilityCategoryUsePayload;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.AbilityUsePayload;
import dev.ua.ikeepcalm.coi.network.payload.ServerboundPayloads.ActionPayload;
import dev.ua.ikeepcalm.coi.screen.GestureScreen;
import dev.ua.ikeepcalm.coi.screen.ability.AbilityBindingScreen;
import dev.ua.ikeepcalm.coi.screen.ability.AbilityWheelScreen;
import dev.ua.ikeepcalm.coi.screen.debug.EffectDebugScreen;
import dev.ua.ikeepcalm.coi.screen.sheet.CharacterSheetScreen;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's keymappings and what pressing one does.
 * <p>
 * All {@link AbilityBindings#MAX_ABILITIES} ability keys are registered up front even though
 * only {@code activeAbilitySlots} of them are read: a keymapping can only be registered at
 * init, so lowering the slot count hides a binding rather than removing it. Presses are
 * edge-triggered by hand rather than through {@code consumeClick}, because the wheel and the
 * gesture screen need to know the key is still <em>held</em>.
 */
public class CoiKeyBindings {

    /**
     * The wire targets, also the stem of every lang key describing one.
     */
    private static final String[] MENU_TARGETS = {
            "church", "abilities", "mythical", "uniqueness", "honorific", "map", "seat",
            "throne", "pantheon"
    };

    // Indices into the edge-trigger array past the ability keys, not ability slots
    private static final int PRESS_BINDING_SCREEN = AbilityBindings.MAX_ABILITIES;
    private static final int PRESS_DEBUG_SCREEN = AbilityBindings.MAX_ABILITIES + 1;
    private static final int PRESS_OPEN_MENU = AbilityBindings.MAX_ABILITIES + 2;
    private static final int PRESS_MENU_TARGET = AbilityBindings.MAX_ABILITIES + 3;
    private static final int TRACKED_KEYS = PRESS_MENU_TARGET + MENU_TARGETS.length;

    /** Slots 7+ default unbound: the player assigns keys in vanilla Controls. */
    private static final int[] DEFAULT_ABILITY_KEYS = {
            GLFW.GLFW_KEY_Z,
            GLFW.GLFW_KEY_X,
            GLFW.GLFW_KEY_C,
            GLFW.GLFW_KEY_V,
            GLFW.GLFW_KEY_B,
            GLFW.GLFW_KEY_N
    };

    private static final boolean[] keyPressed = new boolean[TRACKED_KEYS];
    private static final KeyMapping[] abilityKeys = new KeyMapping[AbilityBindings.MAX_ABILITIES];
    private static final KeyMapping[] menuTargetKeys = new KeyMapping[MENU_TARGETS.length];

    public static KeyMapping abilityMenu;
    public static KeyMapping abilityWheel;
    public static KeyMapping openMenu;
    public static KeyMapping gestureCast;
    /** Null outside the development environment. */
    public static KeyMapping effectDebugMenu;

    private CoiKeyBindings() {
    }

    public static void registerKeybindings() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("category.coi.abilities"));

        for (int i = 0; i < AbilityBindings.MAX_ABILITIES; i++) {
            int defaultKey = i < DEFAULT_ABILITY_KEYS.length
                    ? DEFAULT_ABILITY_KEYS[i] : GLFW.GLFW_KEY_UNKNOWN;
            abilityKeys[i] = register("key.coi.ability" + (i + 1), defaultKey, category);
        }

        abilityMenu = register("screen.coi.ability_binding", GLFW.GLFW_KEY_K, category);
        abilityWheel = register("key.coi.ability_wheel", GLFW.GLFW_KEY_G, category);
        openMenu = register("key.coi.open_menu", GLFW.GLFW_KEY_M, category);
        gestureCast = register("key.coi.gesture", GLFW.GLFW_KEY_LEFT_ALT, category);

        // All unbound by default: nine more claimed keys would collide with the player's own
        for (int i = 0; i < MENU_TARGETS.length; i++) {
            menuTargetKeys[i] = register("key.coi.open_" + MENU_TARGETS[i], GLFW.GLFW_KEY_UNKNOWN, category);
        }

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            effectDebugMenu = register("screen.coi.effect_debug", GLFW.GLFW_KEY_F8, category);
        }
    }

    private static KeyMapping register(String translationKey, int defaultKey, KeyMapping.Category category) {
        return KeyMappingHelper.registerKeyMapping(
                new KeyMapping(translationKey, InputConstants.Type.KEYSYM, defaultKey, category));
    }

    public static void tickKeys(Minecraft client) {
        for (int i = 0; i < AbilityBindings.getActiveAbilitySlots(); i++) {
            int slot = i;
            onPress(i, abilityKeys[i], () -> useAbility(AbilityBindings.getBoundAbility(slot)));
        }

        onPress(PRESS_BINDING_SCREEN, abilityMenu, () -> client.gui.setScreen(new AbilityBindingScreen(null)));
        onPress(PRESS_OPEN_MENU, openMenu, () -> openServerMenu(client));

        for (int i = 0; i < MENU_TARGETS.length; i++) {
            String target = MENU_TARGETS[i];
            onPress(PRESS_MENU_TARGET + i, menuTargetKeys[i], () -> openTarget(client, target));
        }

        if (effectDebugMenu != null) {
            onPress(PRESS_DEBUG_SCREEN, effectDebugMenu, () -> client.gui.setScreen(new EffectDebugScreen(null)));
        }

        if (abilityWheel.isDown() && client.gui.screen() == null) {
            client.gui.setScreen(new AbilityWheelScreen());
        }

        if (gestureCast.isDown() && client.gui.screen() == null && AbilityBindings.hasAnyGestureBound()) {
            client.gui.setScreen(new GestureScreen());
        }
    }

    /** Runs {@code action} on the frame the key goes down, not again until it is released. */
    private static void onPress(int index, KeyMapping key, Runnable action) {
        if (!key.isDown()) {
            keyPressed[index] = false;
            return;
        }
        if (keyPressed[index]) return;
        keyPressed[index] = true;
        action.run();
    }

    public static void useAbility(String abilityIdWithName) {
        if (abilityIdWithName == null) return;

        String abilityId = AbilityInfo.extractId(abilityIdWithName);
        String action = AbilityInfo.extractAction(abilityIdWithName);
        String category = AbilityInfo.extractCategory(abilityIdWithName);
        if (!category.isEmpty()) {
            if (!ServerCapabilities.has("ability_categories")
                    || AbilityCategories.find(abilityId, category) == null
                    || !ClientPlayNetworking.canSend(AbilityCategoryUsePayload.ID)) {
                JsonObject message = new JsonObject();
                message.addProperty("title", Component.translatable("screen.coi.category_unavailable").getString());
                NotificationState.handle(message.toString());
                return;
            }
            ClientPlayNetworking.send(new AbilityCategoryUsePayload(abilityId, category));
        } else {
            if (!ClientPlayNetworking.canSend(AbilityUsePayload.ID)) return;
            ClientPlayNetworking.send(new AbilityUsePayload(abilityId, action));
        }
        AbilityOverlay.onAbilityCast(abilityIdWithName);
    }

    /**
     * {@code useServerMenus} only <em>reorders</em> the first two: a server without
     * {@code menu_action} still gets the sheet, since the preference says "prefer the server's
     * own UI", not "show nothing".
     */
    private static void openServerMenu(Minecraft client) {
        if (client.player == null) return;
        if (HudConfig.getSettings().useServerMenus && ServerCapabilities.has("menu_action")) {
            ClientPlayNetworking.send(ActionPayload.of("open_menu"));
        } else if (ServerCapabilities.has("character_sheet")) {
            client.gui.setScreen(new CharacterSheetScreen(null));
        } else if (ServerCapabilities.has("menu_action")) {
            ClientPlayNetworking.send(ActionPayload.of("open_menu"));
        } else {
            client.player.sendOverlayMessage(Component.translatable("notification.coi.menu_unsupported"));
        }
    }

    /**
     * Deliberately <em>not</em> marked as coming from the sheet: there is no page behind a
     * keybind for the menu's back arrow to return to.
     */
    private static void openTarget(Minecraft client, String target) {
        if (client.player == null) return;
        if (!ServerCapabilities.has("menu_action")) {
            client.player.sendOverlayMessage(Component.translatable("notification.coi.menu_unsupported"));
            return;
        }
        // The gates only exist once the server has pushed a sheet; before that, let it refuse
        if (SheetState.hasData() && !SheetState.actions().unlocked(target)) {
            client.player.sendOverlayMessage(Component.translatable("screen.coi.sheet_lock_" + target));
            return;
        }
        MenuState.markDirect();
        ClientPlayNetworking.send(ActionPayload.ofOpen(target));
    }

    public static KeyMapping abilityKey(int index) {
        return abilityKeys[index];
    }

    /**
     * Asked of GLFW rather than of the keymapping: the wheel and the gesture screen stay open
     * <em>while</em> a key is held, and a screen's own input handling swallows the mapping.
     */
    public static boolean isKeyDown(KeyMapping keyBinding) {
        if (keyBinding == null || keyBinding.isUnbound()) return false;

        long window = Minecraft.getInstance().getWindow().handle();
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(keyBinding);

        return switch (key.getType()) {
            case KEYSYM -> GLFW.glfwGetKey(window, key.getValue()) != GLFW.GLFW_RELEASE;
            case MOUSE -> GLFW.glfwGetMouseButton(window, key.getValue()) != GLFW.GLFW_RELEASE;
            default -> false;
        };
    }
}

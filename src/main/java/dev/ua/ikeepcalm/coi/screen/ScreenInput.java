package dev.ua.ikeepcalm.coi.screen;

import dev.ua.ikeepcalm.coi.input.CoiKeyBindings;

import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ScreenInput {

    private ScreenInput() {
    }

    public static Component keyName(KeyMapping key) {
        return KeyMappingHelper.getBoundKeyOf(key).getDisplayName();
    }

    /**
     * Screens normally swallow keyboard input. Feeding the raw key state back into the movement
     * bindings is what lets the player keep moving while one is open.
     */
    public static void keepMovementKeysAlive(Minecraft client) {
        if (client.player == null) return;
        var options = client.options;
        KeyMapping[] movementKeys = {
                options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyShift, options.keySprint
        };
        for (KeyMapping key : movementKeys) {
            key.setDown(CoiKeyBindings.isKeyDown(key));
        }
    }
}

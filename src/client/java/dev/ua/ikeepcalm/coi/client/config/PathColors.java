package dev.ua.ikeepcalm.coi.client.config;

import net.minecraft.ChatFormatting;

public enum PathColors {
    Fool(ChatFormatting.DARK_PURPLE),
    Door(ChatFormatting.AQUA),
    Error(ChatFormatting.WHITE),

    Sun(ChatFormatting.YELLOW),
    Tyrant(ChatFormatting.BLUE),
    White_Tower(ChatFormatting.AQUA),
    Visionary(ChatFormatting.GOLD),
    Hanged_Man(ChatFormatting.DARK_GRAY),

    Abyss(ChatFormatting.DARK_RED),
    Chained(ChatFormatting.DARK_RED),

    Hermit(ChatFormatting.LIGHT_PURPLE),
    Paragon(ChatFormatting.GRAY),

    Red_Priest(ChatFormatting.RED),
    Demoness(ChatFormatting.LIGHT_PURPLE),

    Wheel_of_Fortune(ChatFormatting.WHITE),

    Moon(ChatFormatting.DARK_RED),
    Mother(ChatFormatting.GREEN),

    Death(ChatFormatting.DARK_GRAY),
    Darkness(ChatFormatting.DARK_GRAY),
    Twilight_Giant(ChatFormatting.GOLD),

    Black_Emperor(ChatFormatting.DARK_GRAY),
    Justiciar(ChatFormatting.GOLD);

    private final ChatFormatting format;

    PathColors(ChatFormatting chatFormatting) {
        this.format = chatFormatting;
    }

    public ChatFormatting toFormat() {
        return this.format;
    }
}

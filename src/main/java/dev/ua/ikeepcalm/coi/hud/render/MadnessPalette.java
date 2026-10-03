package dev.ua.ikeepcalm.coi.hud.render;

/**
 * A pure function of the stage and the clock, so the live bar and the preview cannot disagree.
 */
public class MadnessPalette {

    private MadnessPalette() {
    }

    public record Style(int top, int bottom, int text, int border, String status) {
    }

    public static Style of(int stage, long time) {
        switch (stage) {
            case 1 -> {
                // Stage 1: Warning (Yellow/Orange) - gentle pulse
                float pulse = (float) Math.sin(time * 0.003) * 0.15f + 0.85f;
                int r = (int) (255 * pulse);
                int g = (int) (170 * pulse);
                return new Style((255 << 24) | (r << 16) | (g << 8),
                        (255 << 24) | ((int) (204 * pulse) << 16) | ((int) (119 * pulse) << 8),
                        0xFFFFAA00, 0xCC000000, "Sane");
            }
            case 2 -> {
                // Stage 2: Partial Loss (Red) - static dark red
                return new Style(0xFFDD2222, 0xFF991111, 0xFFDD2222, 0xCC1A0000, "Unstable");
            }
            case 3 -> {
                // Stage 3: Critical (Deep Crimson) - fast pulse
                float pulse = (float) Math.sin(time * 0.01) * 0.2f + 0.8f;
                return new Style((255 << 24) | ((int) (255 * pulse) << 16),
                        (255 << 24) | ((int) (139 * pulse) << 16),
                        0xFFFF0055, 0xCC330000, "Unhinged");
            }
            case 4 -> {
                // MAX: Rampager (Dark Purple/Black)
                float pulse = (float) Math.sin(time * 0.015) * 0.15f + 0.85f;
                int r = (int) (153 * pulse);
                int b = (int) (153 * pulse);
                return new Style((255 << 24) | (r << 16) | (51 << 8) | b,
                        (255 << 24) | ((int) (58 * pulse) << 16) | ((int) (58 * pulse)),
                        0xFF993399, 0xCC1A0520, "Gone Mad");
            }
            default -> {
                // Stage 0: Stable (Green/Cyan)
                return new Style(0xFF00FFCC, 0xFF00AA88, 0xFF00FFCC, 0xCC000000, "Stable");
            }
        }
    }
}

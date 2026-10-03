package dev.ua.ikeepcalm.coi.screen.settings;

import dev.ua.ikeepcalm.coi.config.HudConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

/**
 * Builds the rows of {@link HudSettingsScreen}'s scrollable card down a running cursor. Each row
 * is remembered with its Y offset from the viewport top, so the screen can reposition and hide
 * the lot on a scroll without knowing what any of them are.
 */
final class SettingsRows {

    static final int ROW_STRIDE = 26;
    private static final int FIELD_WIDTH = 52;
    private static final int ALIGN_W = 60;
    static final int INDENT = 10;
    static final int SUB_INDENT = 22;

    /** A tighter stride than a real row, so a hint reads as a caption. */
    private static final int HINT_STRIDE = 18;

    record ContentWidget(AbstractWidget widget, int baseY) {
    }

    private final Font font;
    private final int contentX;
    private final int contentW;
    private final Consumer<AbstractWidget> register;
    /** Re-runs the screen's {@code init()}, for rows that change which rows exist. */
    private final Runnable rebuild;
    private final Consumer<String> openLayout;

    private final List<ContentWidget> widgets = new ArrayList<>();
    private int cursor;

    SettingsRows(Font font, int contentX, int contentW,
                 Consumer<AbstractWidget> register, Runnable rebuild, Consumer<String> openLayout) {
        this.font = font;
        this.contentX = contentX;
        this.contentW = contentW;
        this.register = register;
        this.rebuild = rebuild;
        this.openLayout = openLayout;
    }

    List<ContentWidget> widgets() {
        return widgets;
    }

    /** Total height of everything built so far. */
    int cursor() {
        return cursor;
    }

    void contentRow(AbstractWidget widget) {
        widgets.add(new ContentWidget(widget, cursor));
        register.accept(widget);
        cursor += ROW_STRIDE;
    }

    void headerRow(Component label) {
        contentRow(new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 20, label, font));
    }

    void headerRow(Component label, String elementId) {
        StringWidget title = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2 - ALIGN_W - 4, 20, label, font);
        widgets.add(new ContentWidget(title, cursor));
        register.accept(title);
        contentRow(alignButton(elementId));
    }

    void hintRow(Component label) {
        StringWidget hint = new StringWidget(contentX + INDENT, 0, contentW - INDENT * 2, 14,
                label.copy().withStyle(ChatFormatting.GRAY), font);
        widgets.add(new ContentWidget(hint, cursor));
        register.accept(hint);
        cursor += HINT_STRIDE;
    }

    /**
     * The show/hide checkbox is labelled with the element's own name. Toggling rebuilds the tab,
     * since the rows below appear and disappear with it.
     *
     * @return whether the element is on, i.e. whether its own rows follow
     */
    boolean elementRow(String elementId, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(Component.translatable("screen.coi.layout_el_" + elementId), font)
                .pos(contentX + INDENT, 0)
                .maxWidth(contentW - INDENT * 2 - ALIGN_W - 4)
                .onValueChange((box, checked) -> {
                    setter.accept(checked);
                    rebuild.run();
                })
                .selected(selected)
                .build();
        widgets.add(new ContentWidget(checkbox, cursor));
        register.accept(checkbox);
        contentRow(alignButton(elementId));
        return selected;
    }

    private Button alignButton(String elementId) {
        return Button.builder(Component.translatable("screen.coi.layout_align"), b -> openLayout.accept(elementId))
                .bounds(contentX + contentW - INDENT - ALIGN_W, 0, ALIGN_W, 20).build();
    }

    void scaleRow(float current, Consumer<Float> setter) {
        decimalRow(SUB_INDENT, Component.translatable("screen.coi.element_scale"),
                Component.translatable("screen.coi.element_scale_field"),
                HudConfig.MIN_ELEMENT_SCALE, HudConfig.MAX_ELEMENT_SCALE, current,
                value -> setter.accept((float) value));
    }

    void opacityRow(float current, Consumer<Float> setter) {
        percentRow(SUB_INDENT, Component.translatable("screen.coi.plate_opacity"),
                Component.translatable("screen.coi.plate_opacity_field"),
                HudConfig.MIN_ELEMENT_OPACITY, HudConfig.MAX_ELEMENT_OPACITY, current,
                value -> setter.accept((float) value));
    }

    void cycleRow(int indent, Component label, Component value, Runnable onCycle) {
        contentRow(Button.builder(label.copy().append(": ").append(value), b -> {
            onCycle.run();
            rebuild.run();
        }).bounds(contentX + indent, 0, contentW - indent - INDENT, 20).build());
    }

    /**
     * @return whether the feature is on, i.e. whether its own rows are built at all
     */
    boolean collapsingRow(int indent, Component label, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(label, font)
                .pos(contentX + indent, 0)
                .maxWidth(contentW - indent - INDENT)
                .onValueChange((box, checked) -> {
                    setter.accept(checked);
                    rebuild.run();
                })
                .selected(selected)
                .build();
        contentRow(checkbox);
        return selected;
    }

    void checkboxRow(int indent, Component label, boolean selected, Consumer<Boolean> setter) {
        Checkbox checkbox = Checkbox.builder(label, font)
                .pos(contentX + indent, 0)
                .maxWidth(contentW - indent - INDENT)
                .onValueChange((box, checked) -> setter.accept(checked))
                .selected(selected)
                .build();
        contentRow(checkbox);
    }

    void intRow(int indent, Component label, Component fieldLabel, int min, int max, int initialValue, IntConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        int clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (double) (max - min);
        int sliderW = sliderWidth(indent);

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
                label.copy().append(": " + clampedInitial), sliderValue) {
            @Override
            protected void updateMessage() {
                int value = min + (int) Math.round(this.value * (max - min));
                setter.accept(value);
                this.setMessage(label.copy().append(": " + value));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.valueOf(value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.valueOf(clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Integer.parseInt(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        widgets.add(new ContentWidget(slider, cursor));
        register.accept(slider);
        contentRow(field); // advances the cursor for the pair
    }

    void decimalRow(int indent, Component label, Component fieldLabel, double min, double max,
                    double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);
        int sliderW = sliderWidth(indent);

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
                label.copy().append(": " + String.format("%.1f", clampedInitial)), sliderValue) {
            @Override
            protected void updateMessage() {
                double value = min + this.value * (max - min);
                value = Math.round(value * 10.0) / 10.0;
                setter.accept(value);
                this.setMessage(label.copy().append(": " + String.format("%.1f", value)));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.format("%.1f", value));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.format("%.1f", clampedInitial));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Double.parseDouble(text), min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        widgets.add(new ContentWidget(slider, cursor));
        register.accept(slider);
        contentRow(field);
    }

    /** A 0..1 setting read and typed as a whole percentage: {@code 0.85} is stored, {@code 85%}
     * is what the player means, so the conversion lives here and not in every caller. */
    void percentRow(int indent, Component label, Component fieldLabel, double min, double max,
                    double initialValue, DoubleConsumer setter) {
        final EditBox[] fieldRef = new EditBox[1];
        double clampedInitial = Math.clamp(initialValue, min, max);
        double sliderValue = (clampedInitial - min) / (max - min);
        int sliderW = sliderWidth(indent);

        AbstractSliderButton slider = new AbstractSliderButton(contentX + indent, 0, sliderW, 20,
                label.copy().append(": " + percent(clampedInitial) + "%"), sliderValue) {
            @Override
            protected void updateMessage() {
                int shown = percent(min + this.value * (max - min));
                setter.accept(Math.clamp(shown / 100.0, min, max));
                this.setMessage(label.copy().append(": " + shown + "%"));
                if (fieldRef[0] != null) {
                    fieldRef[0].setValue(String.valueOf(shown));
                }
            }

            @Override
            protected void applyValue() {
                updateMessage();
            }
        };

        EditBox field = new EditBox(font, contentX + indent + sliderW + 6, 0, FIELD_WIDTH, 20, fieldLabel);
        field.setValue(String.valueOf(percent(clampedInitial)));
        field.setResponder(text -> {
            try {
                setter.accept(Math.clamp(Integer.parseInt(text) / 100.0, min, max));
            } catch (NumberFormatException ignored) {
            }
        });
        fieldRef[0] = field;

        widgets.add(new ContentWidget(slider, cursor));
        register.accept(slider);
        contentRow(field);
    }

    private static int percent(double fraction) {
        return (int) Math.round(fraction * 100);
    }

    /** A slider and its number field share a row; the field takes the right edge. */
    private int sliderWidth(int indent) {
        return contentW - indent - INDENT - FIELD_WIDTH - 6;
    }

    int contentX() {
        return contentX;
    }

    int contentW() {
        return contentW;
    }
}

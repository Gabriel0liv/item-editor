package com.gabri.itemeditor.client.screen;

import com.gabri.babel.core.client.ui.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.Consumer;

public class PotionColorModal extends BabelPanel {
    private final Consumer<Integer> onSave;
    private final Runnable onCancel;
    private final BabelTextField hexField;
    private final BabelSlider redSlider;
    private final BabelSlider greenSlider;
    private final BabelSlider blueSlider;
    private Integer selectedColor;

    public PotionColorModal(Font font, Integer initialColor, Consumer<Integer> onSave, Runnable onCancel) {
        this.onSave = onSave == null ? color -> {} : onSave;
        this.onCancel = onCancel == null ? () -> {} : onCancel;
        this.selectedColor = initialColor == null ? null : (initialColor & 0xFFFFFF);

        int startingColor = this.selectedColor == null ? 0xFFFFFF : this.selectedColor;
        int[] rgb = colorToRgb(startingColor);

        style().width(Math.max(300, Math.min(420, 360)));
        style().autoHeight();
        style().padding(10);
        style().gap(8);
        style().background(BabelTheme.PANEL);
        style().border(BabelTheme.BORDER, 1);

        BabelLabel title = new BabelLabel(Component.translatable("itemeditor.ui.potion_color"));
        title.style().textColor(BabelTheme.TEXT);
        add(title);

        this.hexField = new BabelTextField(String.format(Locale.ROOT, "#%06X", startingColor), value -> {
            Integer parsed = parseHexColor(value);
            if (parsed != null) {
                setColorValue(parsed, true);
            }
        });
        this.hexField.maxLength(16).fillWidth();
        this.hexField.inputFilter(value -> value == null || value.isEmpty() || value.matches("(?i)^#?[0-9a-f]{0,6}$"));
        add(this.hexField);

        this.redSlider = new BabelSlider(rgb[0], 0, 255, value -> updateColorFromSliders());
        this.greenSlider = new BabelSlider(rgb[1], 0, 255, value -> updateColorFromSliders());
        this.blueSlider = new BabelSlider(rgb[2], 0, 255, value -> updateColorFromSliders());
        this.redSlider.fillWidth();
        this.greenSlider.fillWidth();
        this.blueSlider.fillWidth();

        add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.red"), this.redSlider));
        add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.green"), this.greenSlider));
        add(BabelTemplates.settingsRow(Component.translatable("itemeditor.ui.blue"), this.blueSlider));

        add(colorPreview());

        BabelRow actions = new BabelRow().fillWidth().gap(6).justify(BabelJustify.END);
        actions.add(
                new BabelButton(Component.translatable("itemeditor.ui.apply"), this::apply).colors(BabelTheme.SUCCESS, BabelTheme.ACCENT_HOVER, BabelTheme.TEXT),
                new BabelButton(Component.translatable("itemeditor.ui.clear"), this::clearSelection).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT),
                new BabelButton(Component.translatable("itemeditor.ui.cancel"), this::cancel).colors(BabelTheme.PANEL_ALT, BabelTheme.ACCENT, BabelTheme.TEXT)
        );
        add(actions);
    }

    private BabelWidget colorPreview() {
        return new BabelWidget() {
            {
                style().fillWidth();
                style().height(26);
                style().background(BabelTheme.PANEL_ALT);
                style().border(BabelTheme.BORDER, 1);
            }

            @Override
            public BabelSize measure(Font font, int availableWidth, int availableHeight) {
                return resolveOuterSize(0, 26, availableWidth, availableHeight);
            }

            @Override
            public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float partialTick) {
                drawBackground(graphics);
                BabelRect content = contentBounds();
                int color = selectedColor == null ? 0xFFFFFFFF : (0xFF000000 | (selectedColor & 0xFFFFFF));
                graphics.fill(content.x(), content.y(), content.right(), content.bottom(), color);
            }
        };
    }

    private void apply() {
        this.onSave.accept(this.selectedColor);
        this.onCancel.run();
    }

    private void clearSelection() {
        this.selectedColor = null;
        this.hexField.setValueSilently("#FFFFFF");
        this.redSlider.setValueSilently(255);
        this.greenSlider.setValueSilently(255);
        this.blueSlider.setValueSilently(255);
        this.onSave.accept(null);
        this.onCancel.run();
    }

    private void cancel() {
        this.onCancel.run();
    }

    private void updateColorFromSliders() {
        setColorValue(rgbToColor(this.redSlider.value(), this.greenSlider.value(), this.blueSlider.value()), false);
    }

    private void setColorValue(int color, boolean updateControls) {
        this.selectedColor = color & 0xFFFFFF;
        if (updateControls) {
            int[] rgb = colorToRgb(this.selectedColor);
            this.redSlider.setValueSilently(rgb[0]);
            this.greenSlider.setValueSilently(rgb[1]);
            this.blueSlider.setValueSilently(rgb[2]);
        }
        this.hexField.setValueSilently(String.format(Locale.ROOT, "#%06X", this.selectedColor));
    }

    private static String colorLabel(Integer color) {
        if (color == null) {
            return Component.translatable("itemeditor.ui.pick_color").getString();
        }
        return Component.translatable("itemeditor.ui.color_with_value", String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF)).getString();
    }

    private static int rgbToColor(int red, int green, int blue) {
        return ((Math.max(0, Math.min(255, red)) & 0xFF) << 16)
                | ((Math.max(0, Math.min(255, green)) & 0xFF) << 8)
                | (Math.max(0, Math.min(255, blue)) & 0xFF);
    }

    private static int[] colorToRgb(int color) {
        return new int[] {
                (color >> 16) & 0xFF,
                (color >> 8) & 0xFF,
                color & 0xFF
        };
    }

    private static Integer parseHexColor(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(normalized, 16);
        } catch (Exception ignored) {
            return null;
        }
    }
}

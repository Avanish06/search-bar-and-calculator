package com.invsearch;

import com.invsearch.config.ConfigManager;
import com.invsearch.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A dedicated, SkyHanni-style HUD editor for the search/calculator bar.
 * Opened via "/sac editbar" — shows a preview of the bar on a dimmed
 * background that you can drag to move and drag-by-corner to resize
 * (both width AND height/line-count now), with no modifier keys needed
 * since the whole screen IS the edit mode. Press ESC to save and exit.
 */
public class HudEditorScreen extends Screen {

    private static final int HANDLE_SIZE = 6;
    private static final int MAX_LINES = 5;

    private boolean isDragging = false;
    private boolean isResizing = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    private int boxX;
    private int boxY;
    private int boxWidth;
    private int boxHeight;

    public HudEditorScreen() {
        super(Component.literal("Search & Calc - HUD Editor"));
    }

    @Override
    protected void init() {
        ModConfig config = ConfigManager.getConfig();
        this.boxWidth = config.barWidth > 0 ? config.barWidth : 120;
        this.boxHeight = Math.max(1, config.barLines) * ModConfig.LINE_HEIGHT;
        this.boxX = config.barX >= 0 ? config.barX : (this.width / 2 - this.boxWidth / 2);
        this.boxY = config.barY >= 0 ? config.barY : (this.height - 22);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dim the whole screen so the preview box stands out clearly.
        graphics.fill(0, 0, this.width, this.height, 0xC0101010);

        // Box preview + border — all sized off the current (resizable) boxHeight.
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xFF2B2B2B);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFFFFFFF);
        graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFFFFFFF);
        graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFFFFFFF);
        graphics.centeredText(this.font, "Search & Calc", boxX + boxWidth / 2, boxY + boxHeight / 2 - 4, 0xFFFFFF);

        int lines = boxHeight / ModConfig.LINE_HEIGHT;
        graphics.centeredText(this.font, lines + " line" + (lines == 1 ? "" : "s"),
            boxX + boxWidth / 2, boxY + boxHeight + 4, 0xAAAAAA);

        // Resize grip, bottom-right corner — tracks boxHeight, not a fixed constant.
        int hx = boxX + boxWidth - HANDLE_SIZE / 2;
        int hy = boxY + boxHeight - HANDLE_SIZE / 2;
        graphics.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, 0xFFFFFFFF);
        graphics.fill(hx + 1, hy + 1, hx + HANDLE_SIZE - 1, hy + HANDLE_SIZE - 1, 0xFF55FF55);

        graphics.centeredText(
            this.font,
            "Drag the box to move it - drag the green corner to resize - ESC to save and exit",
            this.width / 2, this.height - 14, 0xAAAAAA
        );
    }

    private boolean isOverHandle(double mouseX, double mouseY) {
        int hx = boxX + boxWidth - HANDLE_SIZE / 2;
        int hy = boxY + boxHeight - HANDLE_SIZE / 2;
        return mouseX >= hx - 3 && mouseX <= hx + HANDLE_SIZE + 3
            && mouseY >= hy - 3 && mouseY <= hy + HANDLE_SIZE + 3;
    }

    private boolean isOverBox(double mouseX, double mouseY) {
        return mouseX >= boxX && mouseX <= boxX + boxWidth
            && mouseY >= boxY && mouseY <= boxY + boxHeight;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
        if (isOverHandle(event.x(), event.y())) {
            isResizing = true;
            return true;
        } else if (isOverBox(event.x(), event.y())) {
            isDragging = true;
            dragOffsetX = event.x() - boxX;
            dragOffsetY = event.y() - boxY;
            return true;
        }
        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (isDragging) {
            boxX = (int) (event.x() - dragOffsetX);
            boxY = (int) (event.y() - dragOffsetY);
            return true;
        } else if (isResizing) {
            boxWidth = Math.max(30, (int) (event.x() - boxX));

            int newHeight = (int) (event.y() - boxY);
            int lines = Math.max(1, Math.min(MAX_LINES, newHeight / ModConfig.LINE_HEIGHT));
            boxHeight = lines * ModConfig.LINE_HEIGHT;

            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDragging = false;
        isResizing = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) { // ESC
            this.onClose();
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ModConfig config = ConfigManager.getConfig();
        config.barX = boxX;
        config.barY = boxY;
        config.barWidth = boxWidth;
        config.barLines = Math.max(1, boxHeight / ModConfig.LINE_HEIGHT);
        ConfigManager.saveConfig();
    }
}

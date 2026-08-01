package com.invsearch.mixin;

import com.invsearch.CalculatorEngine;
import com.invsearch.InventorySearch;
import com.invsearch.config.ConfigManager;
import com.invsearch.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {

    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected int imageHeight;
    @Shadow @Final protected T menu;

    // NOTE: MultiLineEditBox is used unconditionally now (even for 1 line)
    // instead of swapping between EditBox/MultiLineEditBox at runtime. That
    // swap would drop focus/cursor position every time you crossed the
    // 1<->2 line boundary while resizing, which felt broken. One widget type
    // avoids that entirely at the cost of never getting single-line's native
    // ghost-suggestion support — which is why we draw it manually below anyway.
    private MultiLineEditBox searchBox;
    private String currentSuggestion = "";
    private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);
    private static final int HANDLE_SIZE = 6;

    private boolean isDraggingBox = false;
    private boolean isResizingBox = false;
    private double dragOffsetX = 0;
    private double dragOffsetY = 0;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSearchBox(CallbackInfo ci) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled) return;

        int boxWidth = config.barWidth > 0 ? config.barWidth : 120;
        int lines = Math.max(1, config.barLines);
        int boxHeight = lines * ModConfig.LINE_HEIGHT;
        int boxX = config.barX >= 0 ? config.barX : (this.width / 2 - boxWidth / 2);
        int boxY = config.barY >= 0 ? config.barY : (this.height - 22);

        this.searchBox = MultiLineEditBox.builder()
            .setX(boxX)
            .setY(boxY)
            .build(this.font, boxWidth, boxHeight, Component.literal("Search"));

        // Vanilla text fields default to a short max length (32 chars for
        // EditBox); if MultiLineEditBox has the same constructor-time default,
        // apply the same generous cap here for chained "&&"/multi-line queries
        // and longer calculator expressions.
        this.searchBox.setCharacterLimit(256);

        if (config.rememberLastQuery) {
            this.searchBox.setValue(InventorySearch.draftText);
        } else {
            InventorySearch.draftText = "";
            InventorySearch.currentQuery = "";
        }

        this.searchBox.setValueListener(text -> {
            if (config.rememberLastQuery) {
                InventorySearch.draftText = text;
            }
            if (text.startsWith("=")) {
                Optional<Double> res = CalculatorEngine.evaluate(text.substring(1));
                this.currentSuggestion = res.isPresent()
                    ? (" \u2192 " + FORMATTER.format(res.get()))
                    : " \u2192 Error";
            } else {
                this.currentSuggestion = "";
                // Search is intentionally NOT auto-applied here anymore.
                // The user double-clicks the box to activate/refresh the
                // highlighted search instead of it live-searching every
                // keystroke.
            }
        });

        String initial = this.searchBox.getValue();
        if (initial.startsWith("=")) {
            Optional<Double> res = CalculatorEngine.evaluate(initial.substring(1));
            this.currentSuggestion = res.isPresent()
                ? (" \u2192 " + FORMATTER.format(res.get()))
                : " \u2192 Error";
        }

        this.addRenderableWidget(this.searchBox);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchBox == null) return;

        // Double-click on the box (without ALT) activates the search using
        // whatever text is currently typed, instead of highlighting live as
        // you type. Doesn't cancel the event so the box's own double-click
        // word-select behavior still happens too.
        if (isDouble && !event.hasAltDown() && this.searchBox.isMouseOver(event.x(), event.y())) {
            String text = this.searchBox.getValue();
            if (!text.startsWith("=")) {
                InventorySearch.currentQuery = text;
            }
        }

        if (event.hasAltDown()) {
            if (isOverHandle(event.x(), event.y())) {
                // Grabbed the little corner grip -> resize.
                this.isResizingBox = true;
                cir.setReturnValue(true);
            } else if (this.searchBox.isMouseOver(event.x(), event.y())) {
                // Grabbed the body of the box -> move.
                this.isDraggingBox = true;
                this.dragOffsetX = event.x() - this.searchBox.getX();
                this.dragOffsetY = event.y() - this.searchBox.getY();
                cir.setReturnValue(true);
            }
        }
    }

    /** Small SkyHanni-style resize grip at the bottom-right corner of the box. */
    private boolean isOverHandle(double mouseX, double mouseY) {
        int hx = this.searchBox.getX() + this.searchBox.getWidth() - HANDLE_SIZE / 2;
        int hy = this.searchBox.getY() + this.searchBox.getHeight() - HANDLE_SIZE / 2;
        // Slightly larger hit box than the drawn grip so it's easy to grab.
        return mouseX >= hx - 2 && mouseX <= hx + HANDLE_SIZE + 2
            && mouseY >= hy - 2 && mouseY <= hy + HANDLE_SIZE + 2;
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void onMouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDraggingBox && this.searchBox != null) {
            ModConfig config = ConfigManager.getConfig();
            config.barX = (int) (event.x() - this.dragOffsetX);
            config.barY = (int) (event.y() - this.dragOffsetY);
            this.searchBox.setX(config.barX);
            this.searchBox.setY(config.barY);
            cir.setReturnValue(true);
        } else if (this.isResizingBox && this.searchBox != null) {
            ModConfig config = ConfigManager.getConfig();
            int newWidth = (int) (event.x() - this.searchBox.getX());
            int newHeight = (int) (event.y() - this.searchBox.getY());
            config.barWidth = Math.max(30, newWidth);
            this.searchBox.setWidth(config.barWidth);

            // Vertical drag maps to line count: floor(availableHeight / lineHeight),
            // never round up — a partially-visible extra line is worse than
            // clamping at the lower count. Capped so a wild drag can't produce
            // an absurdly tall box.
            int lines = Math.max(1, Math.min(5, newHeight / ModConfig.LINE_HEIGHT));
            config.barLines = lines;
            this.searchBox.setHeight(lines * ModConfig.LINE_HEIGHT);

            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void onMouseReleased(net.minecraft.client.input.MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.isDraggingBox || this.isResizingBox) {
            this.isDraggingBox = false;
            this.isResizingBox = false;
            ConfigManager.saveConfig();
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            int keyCode = event.key();
            if (keyCode == 257 || keyCode == 335) {
                if (event.hasShiftDown()) {
                    // Shift+Enter: insert a literal newline instead of
                    // submitting, so users can stack "&&" terms one per line.
                    this.searchBox.setValue(this.searchBox.getValue() + "\n");
                    cir.setReturnValue(true);
                    return;
                }

                String val = this.searchBox.getValue();
                if (val.startsWith("=")) {
                    Optional<Double> result = CalculatorEngine.evaluate(val.substring(1));
                    if (result.isPresent()) {
                        this.searchBox.setValue(FORMATTER.format(result.get()));
                        this.currentSuggestion = "";
                        if (ConfigManager.getConfig().rememberLastQuery) {
                            InventorySearch.draftText = this.searchBox.getValue();
                        }
                    }
                }
                cir.setReturnValue(true);
                return;
            } else if (keyCode == 256) {
                this.searchBox.setFocused(false);
                cir.setReturnValue(true);
                return;
            }

            this.searchBox.keyPressed(event);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void onExtractSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, CallbackInfo ci) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled || this.searchBox == null) return;

        String query = InventorySearch.currentQuery;
        if (query.isBlank() || query.startsWith("=") || !slot.hasItem()) return;

        if (!config.includePlayerInventory && slot.container instanceof Inventory) {
            return;
        }

        if (InventorySearch.matches(slot.getItem(), query)) {
            int color = config.highlightColor;
            int x0 = slot.x - 1;
            int y0 = slot.y - 1;
            int x1 = slot.x + 17;
            int y1 = slot.y + 17;
            graphics.fill(x0, y0, x1, y0 + 1, color);
            graphics.fill(x0, y1 - 1, x1, y1, color);
            graphics.fill(x0, y0, x0 + 1, y1, color);
            graphics.fill(x1 - 1, y0, x1, y1, color);
        } else {
            int opacity = Math.max(0, Math.min(255, config.dimOpacity));
            int color = (opacity << 24) | 0x000000;
            graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, color);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled || this.searchBox == null) return;

        String query = InventorySearch.currentQuery;
        if (!query.startsWith("=")) {
            int total = this.menu.slots.stream()
                .filter(slot -> config.includePlayerInventory || !(slot.container instanceof Inventory))
                .filter(Slot::hasItem)
                .map(Slot::getItem)
                .filter(s -> query.isBlank() || InventorySearch.matches(s, query))
                .mapToInt(ItemStack::getCount)
                .sum();

            String countText = "Total: " + FORMATTER.format(total);
            graphics.centeredText(this.font, countText, this.width / 2, this.height - 34, 0xFFFFFF);
        }

        // Draw inline next to the text.
        if (!this.currentSuggestion.isEmpty()) {
            String text = this.searchBox.getValue();
            int lastLineLength = text.isEmpty() ? 0 : text.substring(text.lastIndexOf('\n') + 1).length();
            int usedLines = (int) text.chars().filter(c -> c == '\n').count() + 1;
            int suggestionX = this.searchBox.getX() + 2 + this.font.width(text.substring(Math.max(0, text.length() - lastLineLength)));
            int suggestionY = this.searchBox.getY() + ((usedLines - 1) * ModConfig.LINE_HEIGHT) + 2;

            graphics.fill(suggestionX, suggestionY, suggestionX + this.font.width(this.currentSuggestion) + 2, suggestionY + ModConfig.LINE_HEIGHT, 0x80000000);
            graphics.centeredText(this.font, this.currentSuggestion,
                suggestionX + this.font.width(this.currentSuggestion) / 2, suggestionY + 1, 0xFFAAAAAA);
        }

        // Draw a small grip square in the bottom-right corner so the resize
        // handle is actually discoverable instead of a hidden click zone.
        int hx = this.searchBox.getX() + this.searchBox.getWidth() - HANDLE_SIZE / 2;
        int hy = this.searchBox.getY() + this.searchBox.getHeight() - HANDLE_SIZE / 2;
        graphics.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, 0xFFFFFFFF);
        graphics.fill(hx + 1, hy + 1, hx + HANDLE_SIZE - 1, hy + HANDLE_SIZE - 1, 0xFF55FF55);
    }
}

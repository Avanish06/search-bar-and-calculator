package com.invsearch.mixin;

import com.invsearch.CalculatorEngine;
import com.invsearch.InventorySearch;
import com.invsearch.config.ConfigManager;
import com.invsearch.config.ModConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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

    private EditBox searchBox;
    private static final NumberFormat FORMATTER = NumberFormat.getInstance(Locale.US);

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
        int boxX = config.barX >= 0 ? config.barX : (this.width / 2 - boxWidth / 2);
        int boxY = config.barY >= 0 ? config.barY : (this.height - 22);
        
        this.searchBox = new EditBox(this.font, boxX, boxY, boxWidth, 12, Component.literal("Search"));
        
        if (config.rememberLastQuery) {
            this.searchBox.setValue(InventorySearch.currentQuery);
        } else {
            InventorySearch.currentQuery = "";
        }
        
        this.searchBox.setResponder(text -> {
            if (config.rememberLastQuery) {
                InventorySearch.currentQuery = text;
            }
            if (text.startsWith("=")) {
                Optional<Double> res = CalculatorEngine.evaluate(text.substring(1));
                if (res.isPresent()) {
                    this.searchBox.setSuggestion(" \u2192 " + FORMATTER.format(res.get()));
                } else {
                    this.searchBox.setSuggestion(" \u2192 Error");
                }
            } else {
                this.searchBox.setSuggestion("");
            }
        });
        
        if (this.searchBox.getValue().startsWith("=")) {
            Optional<Double> res = CalculatorEngine.evaluate(this.searchBox.getValue().substring(1));
            if (res.isPresent()) {
                this.searchBox.setSuggestion(" \u2192 " + FORMATTER.format(res.get()));
            } else {
                this.searchBox.setSuggestion(" \u2192 Error");
            }
        }
        
        this.addRenderableWidget(this.searchBox);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble, CallbackInfoReturnable<Boolean> cir) {
        if (event.hasAltDown() && this.searchBox != null) {
            if (this.searchBox.isMouseOver(event.x(), event.y())) {
                if (event.hasControlDown()) {
                    this.isResizingBox = true;
                } else {
                    this.isDraggingBox = true;
                    this.dragOffsetX = event.x() - this.searchBox.getX();
                    this.dragOffsetY = event.y() - this.searchBox.getY();
                }
                cir.setReturnValue(true);
            }
        }
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
            config.barWidth = Math.max(30, newWidth);
            this.searchBox.setWidth(config.barWidth);
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
                String val = this.searchBox.getValue();
                if (val.startsWith("=")) {
                    Optional<Double> result = CalculatorEngine.evaluate(val.substring(1));
                    if (result.isPresent()) {
                        this.searchBox.setValue(FORMATTER.format(result.get()));
                        this.searchBox.setSuggestion("");
                        if (ConfigManager.getConfig().rememberLastQuery) {
                            InventorySearch.currentQuery = this.searchBox.getValue();
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
        
        String query = config.rememberLastQuery ? InventorySearch.currentQuery : this.searchBox.getValue();
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

        String query = config.rememberLastQuery ? InventorySearch.currentQuery : this.searchBox.getValue();
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
    }
}

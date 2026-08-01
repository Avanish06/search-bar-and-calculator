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

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addSearchBox(CallbackInfo ci) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled) return;

        // Docked to the bottom center of the screen
        int boxWidth = 120;
        int boxX = this.width / 2 - boxWidth / 2;
        int boxY = this.height - 22;
        
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
            // Update calculator suggestion
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
        
        // Initial trigger for suggestion if pre-populated
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

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(net.minecraft.client.input.KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.searchBox != null && this.searchBox.isFocused()) {
            int keyCode = event.key();
            if (keyCode == 257 || keyCode == 335) { // ENTER or KP_ENTER
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
            } else if (keyCode == 256) { // ESCAPE
                this.searchBox.setFocused(false);
                cir.setReturnValue(true);
                return;
            }

            // Any other key (letters, backspace, arrows, ctrl+c/v, etc.) while the box is
            // focused: let the EditBox itself handle it, then ALWAYS consume the event here.
            // This is required because AbstractContainerScreen's own keyPressed checks the
            // vanilla "open/close inventory" keybind (default E) before it ever looks at
            // whether a widget has focus - without this, typing "e" closes the screen instead
            // of reaching the text field.
            this.searchBox.keyPressed(event);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractSlot", at = @At("TAIL"))
    private void onExtractSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, CallbackInfo ci) {
        ModConfig config = ConfigManager.getConfig();
        if (!config.enabled || this.searchBox == null) return;
        
        String query = config.rememberLastQuery ? InventorySearch.currentQuery : this.searchBox.getValue();
        if (query.isBlank() || query.startsWith("=") || !slot.hasItem()) return; // No dimming in calc mode

        // Check if we should ignore player inventory
        if (!config.includePlayerInventory && slot.container instanceof Inventory) {
            return;
        }

        if (InventorySearch.matches(slot.getItem(), query)) {
            // Draw a 1px green border around the matching slot's icon area.
            // Four thin fills (top/bottom/left/right) rather than a single hollow-rect
            // call, since GuiGraphicsExtractor only exposes solid fill() here.
            int color = config.highlightColor;
            int x0 = slot.x - 1;
            int y0 = slot.y - 1;
            int x1 = slot.x + 17;
            int y1 = slot.y + 17;
            graphics.fill(x0, y0, x1, y0 + 1, color);       // top
            graphics.fill(x0, y1 - 1, x1, y1, color);        // bottom
            graphics.fill(x0, y0, x0 + 1, y1, color);        // left
            graphics.fill(x1 - 1, y0, x1, y1, color);        // right
        } else {
            // Draw a dark overlay over the slot using fill
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

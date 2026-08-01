package com.invsearch.mixin;

import com.invsearch.InventorySearch;
import com.invsearch.config.ConfigManager;
import com.invsearch.config.ModConfig;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.MultiLineEditBox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiLineEditBox.class)
public abstract class MultiLineEditBoxMixin {

    @Shadow @Final private Font font;
    @Shadow public abstract String getValue();

    private static final int HANDLE_SIZE = 6;

    @Inject(method = "extractContents", at = @At("TAIL"))
    private void onExtractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        MultiLineEditBox self = (MultiLineEditBox) (Object) this;
        if (InventorySearch.currentSearchBox != self) return;
        if (InventorySearch.currentSuggestion.isEmpty()) return;

        ModConfig config = ConfigManager.getConfig();
        String text = self.getValue();
        int usedLines = (int) text.chars().filter(c -> c == '\n').count() + 1;
        int totalLines = Math.max(1, config.barLines);
        
        int suggestionX;
        int suggestionY;

        // MultiLineEditBox line height is strictly 9
        int lineHeight = 9;

        if (usedLines < totalLines) {
            // Room below the typed text -> next line.
            suggestionX = self.getX() + 4; // 4 is standard inner padding
            suggestionY = self.getY() + 4 + (usedLines * lineHeight);
        } else {
            // No room -> fall back to inline at the end of the last line.
            int lastLineLength = text.isEmpty() ? 0 : text.substring(text.lastIndexOf('\n') + 1).length();
            suggestionX = self.getX() + 4 + this.font.width(text.substring(Math.max(0, text.length() - lastLineLength)));
            suggestionY = self.getY() + 4 + ((usedLines - 1) * lineHeight);
        }

        // Draw background and text
        graphics.fill(suggestionX, suggestionY, suggestionX + this.font.width(InventorySearch.currentSuggestion) + 2, suggestionY + lineHeight, 0x80000000);
        graphics.centeredText(this.font, InventorySearch.currentSuggestion,
            suggestionX + this.font.width(InventorySearch.currentSuggestion) / 2, suggestionY + 1, 0xFFAAAAAA);
    }

    @Inject(method = "extractDecorations", at = @At("TAIL"))
    private void onExtractDecorations(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        MultiLineEditBox self = (MultiLineEditBox) (Object) this;
        if (InventorySearch.currentSearchBox != self) return;

        // Draw a small grip square in the bottom-right corner
        int hx = self.getX() + self.getWidth() - HANDLE_SIZE / 2;
        int hy = self.getY() + self.getHeight() - HANDLE_SIZE / 2;
        graphics.fill(hx, hy, hx + HANDLE_SIZE, hy + HANDLE_SIZE, 0xFFFFFFFF);
        graphics.fill(hx + 1, hy + 1, hx + HANDLE_SIZE - 1, hy + HANDLE_SIZE - 1, 0xFF55FF55);
    }
}

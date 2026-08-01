package com.invsearch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

public final class InventorySearch {
    public static String currentQuery = "";

    public static boolean matches(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) return false;
        if (query == null || query.isBlank()) return true;

        String[] rawTerms = query.split("&&");
        List<String> terms = new ArrayList<>();
        for (String raw : rawTerms) {
            String trimmed = raw.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                terms.add(trimmed);
            }
        }
        if (terms.isEmpty()) return true; // e.g. query was just "&&"

        // Build a list of all searchable text from the item
        List<String> searchableText = new ArrayList<>();

        // 1. Display name
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        if (name != null) {
            searchableText.add(name.toLowerCase());
        }

        // 2. Registry ID (includes namespace/modid e.g. "minecraft:diamond_sword")
        if (com.invsearch.config.ConfigManager.getConfig().matchVanillaIds) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            searchableText.add(id.toLowerCase());
        }

        // 3. Lore lines
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component lineComp : lore.lines()) {
                String lineText = ChatFormatting.stripFormatting(lineComp.getString());
                if (lineText != null) {
                    searchableText.add(lineText.toLowerCase());
                }
            }
        }

        // Check that EVERY term matches AT LEAST ONE piece of searchable text
        for (String term : terms) {
            boolean termMatched = false;
            for (String text : searchableText) {
                if (text.contains(term)) {
                    termMatched = true;
                    break;
                }
            }
            if (!termMatched) {
                return false; // This term didn't match anything, so the whole item fails
            }
        }

        return true;
    }

    public static void main(String[] args) {
        System.out.println("InventorySearch class logic loaded.");
    }
}

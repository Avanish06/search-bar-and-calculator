package com.invsearch;

import com.invsearch.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

        // 2. Identity: prefer the item's real (custom) ID over the vanilla one.
        // NEU-style detection: Hypixel Skyblock (and similar servers) stamp custom
        // items with an "ExtraAttributes" NBT tag containing an "id" string, e.g.
        // "ASPECT_OF_THE_END". When that's present, the vanilla item underneath
        // (diamond sword, skull, etc.) is just a placeholder texture and should
        // never be searchable — that was the bug: matchVanillaIds was an
        // all-or-nothing switch instead of checking per-item whether the vanilla
        // ID is even meaningful.
        String skyblockId = getSkyblockId(stack);
        ModConfig cfg = com.invsearch.config.ConfigManager.getConfig();
        if (skyblockId != null) {
            if (cfg.matchVanillaIds) {
                // Toggle now controls the *custom* id instead, since that id is
                // the only "identity" that's actually meaningful here.
                searchableText.add(skyblockId.toLowerCase().replace('_', ' '));
            }
        } else if (cfg.matchVanillaIds) {
            // No ExtraAttributes tag -> this really is a vanilla item, so the
            // registry ID is meaningful and safe to search.
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

    /**
     * Reads the custom "id" string Hypixel Skyblock (and similar servers) stamp
     * onto items inside an "ExtraAttributes" NBT compound. Returns null for
     * genuinely vanilla items that carry no such tag.
     *
     * NOTE: CompoundTag.getCompound()/contains() signatures have shifted across
     * recent Minecraft versions (some return Optional<CompoundTag> instead of
     * CompoundTag directly). Double check this against your NBT reference dump
     * for 26.1 the same way you did for EditBox/Screen — this is the most likely
     * spot for a compile error if the signature moved again.
     */
    private static String getSkyblockId(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        return tag.getCompound("ExtraAttributes")
                .flatMap(extra -> extra.getString("id"))
                .filter(id -> !id.isBlank())
                .orElse(null);
    }

    public static void main(String[] args) {
        System.out.println("InventorySearch class logic loaded.");
    }
}

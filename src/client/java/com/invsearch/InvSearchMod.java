package com.invsearch;

import com.invsearch.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class InvSearchMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("sac")
                .then(ClientCommandManager.literal("togglevanilla")
                    .executes(context -> {
                        boolean newVal = !ConfigManager.getConfig().matchVanillaIds;
                        ConfigManager.getConfig().matchVanillaIds = newVal;
                        ConfigManager.saveConfig();
                        context.getSource().sendFeedback(Component.literal("§a[Search&Calc] Vanilla ID matching is now: " + (newVal ? "ON" : "OFF")));
                        return 1;
                    })
                )
            );
        });
    }
}

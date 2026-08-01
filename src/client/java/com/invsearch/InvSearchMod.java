package com.invsearch;

import com.invsearch.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class InvSearchMod implements ClientModInitializer {
    private static boolean openHudEditor = false;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (openHudEditor) {
                openHudEditor = false;
                client.setScreen(new HudEditorScreen());
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("sac")
                .then(ClientCommands.literal("togglevanilla")
                    .executes(context -> {
                        boolean newVal = !ConfigManager.getConfig().matchVanillaIds;
                        ConfigManager.getConfig().matchVanillaIds = newVal;
                        ConfigManager.saveConfig();
                        context.getSource().sendFeedback(Component.literal("§a[Search&Calc] Vanilla ID matching is now: " + (newVal ? "ON" : "OFF")));
                        return 1;
                    })
                )
                .then(ClientCommands.literal("editbar")
                    .executes(context -> {
                        openHudEditor = true;
                        return 1;
                    })
                )
            );
        });
    }
}

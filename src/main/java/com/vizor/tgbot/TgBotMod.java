package com.vizor.tgbot;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TgBotMod implements ClientModInitializer {

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        BotSettings.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tgbot.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.tgbot"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.openScreen(new BotSettingsScreen());
            }
            MovementController.tick(client);
        });

        if (BotSettings.isConfigured()) {
            BotTelegram.start();
        }

        System.out.println("[tgbot] Загружен. ПКМ для настроек — клавиша P");
    }
}

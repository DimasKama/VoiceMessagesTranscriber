package net.dimaskama.voicemessagestranscriber;

import net.dimaskama.voicemessagestranscriber.config.JsonConfig;
import net.dimaskama.voicemessagestranscriber.config.TranscriberConfig;
import net.dimaskama.voicemessagestranscriber.screen.TranscriberSettingsScreen;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperModels;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VoiceMessagesTranscriber implements ClientModInitializer {

    public static final String MOD_ID = "voicemessagestranscriber";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final JsonConfig<TranscriberConfig> CONFIG = new JsonConfig<>(
            FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json").toString(),
            TranscriberConfig.CODEC,
            () -> TranscriberConfig.DEFAULT
    );

    private static boolean openSettingsNextTick;

    @Override
    public void onInitializeClient() {
        CONFIG.loadOrCreate();
        WhisperModels.init(FabricLoader.getInstance().getGameDir().resolve(MOD_ID).resolve("models"));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) ->
                dispatcher.register(ClientCommands.literal("vmtranscriber").executes(_ -> {
                    openSettingsNextTick = true;
                    return 1;
                }))
        );
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (openSettingsNextTick) {
                openSettingsNextTick = false;
                openSettings(minecraft.gui.screen());
            }
        });
    }

    public static void openSettings(@Nullable Screen parent) {
        Minecraft.getInstance().gui.setScreen(new TranscriberSettingsScreen(parent));
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

}

package net.dimaskama.voicemessagestranscriber.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class Notifications {

    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

    private Notifications() {
    }

    public static void show(Component title, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> SystemToast.addOrUpdate(minecraft.gui.toastManager(), TOAST_ID, title, message));
    }

    public static void error(Component title, String error) {
        show(title, Component.literal(error));
    }

}

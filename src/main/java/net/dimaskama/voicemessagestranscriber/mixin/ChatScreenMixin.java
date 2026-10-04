package net.dimaskama.voicemessagestranscriber.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;
import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {

    @Shadow
    private ChatComponent.DisplayMode displayMode;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void openSettingsOnRightClick(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        Minecraft minecraft = Minecraft.getInstance();
        ActiveTextCollector.ClickableStyleFinder finder = new ActiveTextCollector.ClickableStyleFinder(screen.getFont(), (int) event.x(), (int) event.y());
        minecraft.gui.hud.getChat().captureClickableText(finder, minecraft.getWindow().getGuiScaledHeight(), minecraft.gui.hud.getGuiTicks(), displayMode);
        Style clicked = finder.result();
        if (clicked != null && TranscriptionManager.isTranscriberClick(clicked)) {
            VoiceMessagesTranscriber.openSettings(screen);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void handleTranscriberClick(Style clicked, boolean allowInsertions, CallbackInfoReturnable<Boolean> cir) {
        if (!allowInsertions
                && clicked.getClickEvent() instanceof ClickEvent.Custom custom
                && TranscriptionManager.handleClick(custom, (Screen) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

}

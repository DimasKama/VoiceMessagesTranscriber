package net.dimaskama.voicemessagestranscriber.mixin;

import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
        "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess",
        "net.minecraft.client.gui.components.ChatComponent$DrawingFocusedGraphicsAccess"
}, remap = false)
abstract class DrawingChatGraphicsAccessMixin {

    @Shadow(remap = false)
    @Final
    private ActiveTextCollector textRenderer;

    @Shadow(remap = false)
    private ActiveTextCollector.Parameters parameters;

    @Unique
    private int voicemessagestranscriber_textTop;

    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void captureTextTop(int textTop, float opacity, FormattedCharSequence message, CallbackInfoReturnable<Boolean> cir) {
        voicemessagestranscriber_textTop = textTop;
        TranscriptionManager.clearPlayerReservation();
    }

    @Inject(method = "handleTag", at = @At("HEAD"))
    private void extractTranscriberButton(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag, CallbackInfo ci) {
        TranscriptionManager.extractButton(tag, textRenderer, parameters.withOpacity(opacity), voicemessagestranscriber_textTop, true);
    }

}

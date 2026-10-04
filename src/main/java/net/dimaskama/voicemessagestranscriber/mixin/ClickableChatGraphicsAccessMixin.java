package net.dimaskama.voicemessagestranscriber.mixin;

import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$ClickableTextOnlyGraphicsAccess", remap = false)
abstract class ClickableChatGraphicsAccessMixin {

    @Shadow(remap = false)
    @Final
    private ActiveTextCollector output;

    @Unique
    private int voicemessagestranscriber_textTop;

    @Unique
    private @Nullable GuiMessageTag voicemessagestranscriber_lastTag;

    @Inject(method = "handleMessage", at = @At("HEAD"))
    private void captureTextTop(int textTop, float opacity, FormattedCharSequence message, CallbackInfoReturnable<Boolean> cir) {
        voicemessagestranscriber_textTop = textTop;
    }

    @Inject(method = "handleTag", at = @At("HEAD"))
    private void captureTranscriberButton(int x0, int y0, int x1, int y1, float opacity, GuiMessageTag tag, CallbackInfo ci) {
        // Lines are visited top to bottom, so only the first line of each entry gets the button
        if (tag != voicemessagestranscriber_lastTag) {
            voicemessagestranscriber_lastTag = tag;
            TranscriptionManager.extractButton(tag, output, output.defaultParameters(), voicemessagestranscriber_textTop, false);
        }
    }

}

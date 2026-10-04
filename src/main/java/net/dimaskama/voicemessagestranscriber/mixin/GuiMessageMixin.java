package net.dimaskama.voicemessagestranscriber.mixin;

import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(GuiMessage.class)
abstract class GuiMessageMixin {

    @Shadow
    @Final
    private @Nullable GuiMessageTag tag;

    @ModifyVariable(method = "splitLines", at = @At("HEAD"), argsOnly = true)
    private int reserveTranscriberButtonSpace(int maxWidth) {
        return TranscriptionManager.modifySplitWidth(tag, maxWidth);
    }

}

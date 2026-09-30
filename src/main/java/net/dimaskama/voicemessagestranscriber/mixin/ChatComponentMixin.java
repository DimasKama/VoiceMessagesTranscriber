package net.dimaskama.voicemessagestranscriber.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {

    @ModifyVariable(method = "addPlayerMessage", at = @At("HEAD"), argsOnly = true)
    private Component modifyVoiceMessage(Component message, @Local(argsOnly = true) GuiMessageTag tag) {
        return TranscriptionManager.onPlayerMessageAdded(message, tag);
    }

    @ModifyExpressionValue(method = "addMessageToDisplayQueue", at = @At(value = "INVOKE", target = "Ljava/util/List;removeLast()Ljava/lang/Object;"))
    private Object onLineRemoved(Object original) {
        TranscriptionManager.onLineRemoved((GuiMessage.Line) original);
        return original;
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void onClearMessages(CallbackInfo ci) {
        TranscriptionManager.onChatCleared();
    }

}

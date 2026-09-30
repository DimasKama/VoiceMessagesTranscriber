package net.dimaskama.voicemessagestranscriber.mixin;

import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void handleTranscriberClick(Style clicked, boolean allowInsertions, CallbackInfoReturnable<Boolean> cir) {
        if (!allowInsertions
                && clicked.getClickEvent() instanceof ClickEvent.Custom custom
                && TranscriptionManager.handleClick(custom, (Screen) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

}

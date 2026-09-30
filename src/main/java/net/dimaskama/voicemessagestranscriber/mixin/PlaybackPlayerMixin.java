package net.dimaskama.voicemessagestranscriber.mixin;

import net.dimaskama.voicemessagestranscriber.client.TranscriptionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackPlayer;

@Mixin(value = PlaybackPlayer.class, remap = false)
abstract class PlaybackPlayerMixin {

    @Shadow(remap = false)
    @Final
    private Playback playback;

    @ModifyVariable(method = "setRectangle", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int reserveTranscriberButtonSpace(int width) {
        return TranscriptionManager.modifyPlayerWidth(playback, width);
    }

}

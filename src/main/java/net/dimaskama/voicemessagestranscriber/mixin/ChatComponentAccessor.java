package net.dimaskama.voicemessagestranscriber.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {

    @Accessor("allMessages")
    List<GuiMessage> voicemessagestranscriber_getAllMessages();

    @Invoker("refreshTrimmedMessages")
    void voicemessagestranscriber_refreshTrimmedMessages();

}

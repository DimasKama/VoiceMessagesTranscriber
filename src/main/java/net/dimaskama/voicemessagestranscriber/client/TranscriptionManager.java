package net.dimaskama.voicemessagestranscriber.client;

import com.mojang.util.UndashedUuid;
import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;
import net.dimaskama.voicemessagestranscriber.mixin.ChatComponentAccessor;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperModels;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperTranscriber;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.jspecify.annotations.Nullable;
import ru.dimaskama.voicemessages.client.Playback;
import ru.dimaskama.voicemessages.client.PlaybackManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TranscriptionManager {

    private static final String VOICE_TAG_PREFIX = "VoiceMessage#";
    private static final String TRANSCRIPT_TAG_PREFIX = "VoiceTranscript#";

    private static final Identifier ACTION_TRANSCRIBE = VoiceMessagesTranscriber.id("transcribe");
    private static final Identifier ACTION_SHOW_TEXT = VoiceMessagesTranscriber.id("show_text");
    private static final Identifier ACTION_SHOW_VOICE = VoiceMessagesTranscriber.id("show_voice");

    private static final Identifier SPRITE_TRANSCRIBE = VoiceMessagesTranscriber.id("transcribe");
    private static final Identifier SPRITE_TRANSCRIBING = VoiceMessagesTranscriber.id("transcribing");
    private static final Identifier SPRITE_SHOW_VOICE = VoiceMessagesTranscriber.id("show_voice");

    private static final int BUTTON_GAP = 6;
    private static final Pattern WHITESPACE_OR_CONTROL = Pattern.compile("[\\s\\p{Cntrl}]+");

    private static final Map<UUID, Entry> ENTRIES = new HashMap<>();

    private static @Nullable Playback reservedPlayback;
    private static int reservedWidth;

    private TranscriptionManager() {
    }

    public static Component onPlayerMessageAdded(Component content, @Nullable GuiMessageTag tag) {
        UUID id = parseId(tag, VOICE_TAG_PREFIX);
        if (id != null) {
            ENTRIES.put(id, new Entry(id, content, tag));
        }
        return content;
    }

    public static void onLineRemoved(GuiMessage.Line line) {
        GuiMessageTag tag = line.tag();
        UUID id = parseId(tag, VOICE_TAG_PREFIX);
        if (id == null) {
            id = parseId(tag, TRANSCRIPT_TAG_PREFIX);
            if (id != null) {
                Playback playback = PlaybackManager.MAIN.get(id);
                if (playback != null) {
                    PlaybackManager.MAIN.remove(playback);
                }
            }
        }
        if (id != null) {
            ENTRIES.remove(id);
        }
    }

    public static void onChatCleared() {
        ENTRIES.clear();
        reservedPlayback = null;
    }

    public static void extractButton(GuiMessageTag tag, ActiveTextCollector output, ActiveTextCollector.Parameters parameters, int textTop, boolean reservePlayerSpace) {
        Entry entry = getEntry(tag);
        if (entry == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean transcript = tag == entry.transcriptTag;
        FormattedCharSequence button = transcript ? entry.getShowVoiceButton() : entry.getButton();
        int right = (int) (ChatComponent.getWidth(minecraft.options.chatWidth().get()) / minecraft.options.chatScale().get());
        output.accept(TextAlignment.RIGHT, right, textTop, parameters, button);
        if (reservePlayerSpace && !transcript) {
            reservedPlayback = PlaybackManager.MAIN.get(entry.id);
            reservedWidth = minecraft.font.width(button) + BUTTON_GAP;
        }
    }

    public static int modifySplitWidth(@Nullable GuiMessageTag tag, int maxWidth) {
        Entry entry = getEntry(tag);
        if (entry == null || tag != entry.transcriptTag) {
            return maxWidth;
        }
        return Math.max(maxWidth - Minecraft.getInstance().font.width(entry.getShowVoiceButton()) - BUTTON_GAP, 1);
    }

    public static void clearPlayerReservation() {
        reservedPlayback = null;
    }

    public static int modifyPlayerWidth(Playback playback, int width) {
        if (reservedPlayback == null || playback != reservedPlayback) {
            return width;
        }
        reservedPlayback = null;
        return Math.max(width - reservedWidth, 0);
    }

    public static boolean isTranscriberClick(Style style) {
        return style.getClickEvent() instanceof ClickEvent.Custom custom
                && VoiceMessagesTranscriber.MOD_ID.equals(custom.id().getNamespace());
    }

    public static boolean handleClick(ClickEvent.Custom event, Screen screen) {
        Identifier action = event.id();
        if (!VoiceMessagesTranscriber.MOD_ID.equals(action.getNamespace())) {
            return false;
        }
        Entry entry = event.payload()
                .flatMap(Tag::asString)
                .map(TranscriptionManager::findByToken)
                .orElse(null);
        if (entry != null) {
            if (ACTION_TRANSCRIBE.equals(action)) {
                startTranscription(entry, screen);
            } else if (ACTION_SHOW_TEXT.equals(action)) {
                if (entry.needsRetranscription()) {
                    startTranscription(entry, screen);
                } else {
                    entry.showingText = true;
                    entry.updateChat();
                }
            } else if (ACTION_SHOW_VOICE.equals(action)) {
                entry.showingText = false;
                entry.updateChat();
            }
        }
        return true;
    }

    private static void startTranscription(Entry entry, Screen screen) {
        if (entry.state == State.TRANSCRIBING) {
            return;
        }
        String model = WhisperModels.getSelectedInstalled();
        if (model == null) {
            Notifications.show(
                    Component.translatable("voicemessagestranscriber.no_model"),
                    Component.translatable("voicemessagestranscriber.no_model.description")
            );
            VoiceMessagesTranscriber.openSettings(screen);
            return;
        }
        Playback playback = PlaybackManager.MAIN.get(entry.id);
        if (playback == null) {
            Notifications.show(
                    Component.translatable("voicemessagestranscriber.transcription_failed"),
                    Component.translatable("voicemessagestranscriber.voice_message_unavailable")
            );
            return;
        }

        State previousState = entry.state;
        entry.setState(State.TRANSCRIBING);
        Minecraft minecraft = Minecraft.getInstance();
        WhisperTranscriber.transcribe(List.copyOf(playback.getAudio()), model, VoiceMessagesTranscriber.CONFIG.getData().language())
                .whenComplete((text, error) -> minecraft.execute(() -> {
                    if (ENTRIES.get(entry.id) != entry) {
                        return;
                    }
                    if (error != null) {
                        VoiceMessagesTranscriber.LOGGER.error("Failed to transcribe voice message", error);
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        Notifications.error(
                                Component.translatable("voicemessagestranscriber.transcription_failed"),
                                String.valueOf(cause.getMessage())
                        );
                        entry.setState(previousState);
                    } else {
                        entry.text = sanitize(text);
                        entry.model = model;
                        entry.showingText = true;
                        entry.setState(State.DONE);
                    }
                    entry.updateChat();
                }));
    }

    private static String sanitize(@Nullable String text) {
        if (text == null) {
            return "";
        }
        String stripped = ChatFormatting.stripFormatting(text);
        return WHITESPACE_OR_CONTROL.matcher(stripped).replaceAll(" ").trim();
    }

    @Nullable
    private static Entry getEntry(@Nullable GuiMessageTag tag) {
        UUID id = parseId(tag, VOICE_TAG_PREFIX);
        if (id == null) {
            id = parseId(tag, TRANSCRIPT_TAG_PREFIX);
            if (id == null) {
                return null;
            }
        }
        Entry entry = ENTRIES.get(id);
        return entry != null && (entry.voiceTag == tag || entry.transcriptTag == tag) ? entry : null;
    }

    @Nullable
    private static Entry findByToken(String token) {
        for (Entry entry : ENTRIES.values()) {
            if (entry.token.equals(token)) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    private static UUID parseId(@Nullable GuiMessageTag tag, String prefix) {
        if (tag == null) {
            return null;
        }
        String logTag = tag.logTag();
        if (logTag == null || !logTag.startsWith(prefix)) {
            return null;
        }
        try {
            return UndashedUuid.fromStringLenient(logTag.substring(prefix.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static MutableComponent createButton(Identifier sprite, String fallback, Component tooltip, Identifier action, String token) {
        Style style = Style.EMPTY
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withClickEvent(new ClickEvent.Custom(action, Optional.of(StringTag.valueOf(token))));
        return Component.object(new AtlasSprite(AtlasIds.GUI, sprite), Component.literal(fallback)).setStyle(style);
    }

    private enum State {
        IDLE,
        TRANSCRIBING,
        DONE
    }

    private static final class Entry {

        private final UUID id;
        private final String token = UUID.randomUUID().toString();
        private final Component sender;
        private final GuiMessageTag voiceTag;
        private final GuiMessageTag transcriptTag;
        private State state = State.IDLE;
        private @Nullable FormattedCharSequence button;
        private @Nullable FormattedCharSequence showVoiceButton;
        private boolean buttonRetranscribes;
        private String text = "";
        private @Nullable String model;
        private boolean showingText;

        private Entry(UUID id, Component sender, GuiMessageTag voiceTag) {
            this.id = id;
            this.sender = sender;
            this.voiceTag = voiceTag;
            this.transcriptTag = new GuiMessageTag(
                    voiceTag.indicatorColor(),
                    null,
                    Component.translatable("voicemessagestranscriber.tag.transcribed"),
                    TRANSCRIPT_TAG_PREFIX + UndashedUuid.toString(id)
            );
        }

        private void setState(State state) {
            this.state = state;
            button = null;
        }

        private boolean needsRetranscription() {
            return state == State.DONE && !Objects.equals(model, VoiceMessagesTranscriber.CONFIG.getData().model());
        }

        private FormattedCharSequence getButton() {
            boolean retranscribes = needsRetranscription();
            if (button == null || buttonRetranscribes != retranscribes) {
                buttonRetranscribes = retranscribes;
                button = (switch (state) {
                    case IDLE -> createButton(SPRITE_TRANSCRIBE, "[T]",
                            Component.translatable("voicemessagestranscriber.button.transcribe"), ACTION_TRANSCRIBE, token);
                    case TRANSCRIBING -> createButton(SPRITE_TRANSCRIBING, "[...]",
                            Component.translatable("voicemessagestranscriber.button.transcribing"), ACTION_TRANSCRIBE, token);
                    case DONE -> createButton(SPRITE_TRANSCRIBE, "[T]",
                            Component.translatable(buttonRetranscribes
                                    ? "voicemessagestranscriber.button.transcribe"
                                    : "voicemessagestranscriber.button.show_text"), ACTION_SHOW_TEXT, token);
                }).getVisualOrderText();
            }
            return button;
        }

        private FormattedCharSequence getShowVoiceButton() {
            if (showVoiceButton == null) {
                showVoiceButton = createButton(SPRITE_SHOW_VOICE, "[V]",
                        Component.translatable("voicemessagestranscriber.button.show_voice"), ACTION_SHOW_VOICE, token).getVisualOrderText();
            }
            return showVoiceButton;
        }

        private Component createTextContent() {
            MutableComponent result = Component.empty().append(sender);
            if (!sender.getString().isEmpty()) {
                result.append(" ");
            }
            if (text.isEmpty()) {
                result.append(Component.translatable("voicemessagestranscriber.no_speech").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            } else {
                result.append(Component.literal(text));
            }
            return result;
        }

        private void updateChat() {
            boolean asText = state == State.DONE && showingText;
            Component content = asText ? createTextContent() : sender;
            GuiMessageTag tag = asText ? transcriptTag : voiceTag;
            ChatComponentAccessor chat = (ChatComponentAccessor) Minecraft.getInstance().gui.hud.getChat();
            List<GuiMessage> messages = chat.voicemessagestranscriber_getAllMessages();
            for (int i = 0; i < messages.size(); i++) {
                GuiMessage message = messages.get(i);
                if (message.tag() == voiceTag || message.tag() == transcriptTag) {
                    messages.set(i, new GuiMessage(message.addedTime(), content, message.signature(), message.source(), tag));
                    chat.voicemessagestranscriber_refreshTrimmedMessages();
                    return;
                }
            }
        }

    }

}

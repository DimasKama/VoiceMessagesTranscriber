package net.dimaskama.voicemessagestranscriber.screen;

import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperLanguages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WhisperLanguageScreen extends Screen {

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 36, 33);
    private @Nullable LanguageList languageList;
    private @Nullable EditBox search;

    public WhisperLanguageScreen(Screen parent) {
        super(Component.translatable("voicemessagestranscriber.language.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        LinearLayout header = layout.addToHeader(LinearLayout.vertical().spacing(4));
        header.defaultCellSetting().alignHorizontallyCenter();
        header.addChild(new StringWidget(title, font));
        search = header.addChild(new EditBox(font, 0, 0, 200, 15, Component.empty()));
        search.setHint(Component.translatable("gui.language.search").withStyle(EditBox.SEARCH_HINT_STYLE));
        search.setResponder(filter -> {
            if (languageList != null) {
                languageList.filter(filter);
            }
        });

        languageList = layout.addToContents(new LanguageList(minecraft));
        layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, b -> onDone()).width(200).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    @Override
    protected void setInitialFocus() {
        if (search != null) {
            setInitialFocus(search);
        } else {
            super.setInitialFocus();
        }
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (languageList != null) {
            languageList.updateSize(width, layout);
        }
    }

    private void onDone() {
        LanguageList.Entry selected = languageList != null ? languageList.getSelected() : null;
        if (selected != null) {
            VoiceMessagesTranscriber.CONFIG.setData(VoiceMessagesTranscriber.CONFIG.getData().withLanguage(selected.code));
            VoiceMessagesTranscriber.CONFIG.save();
        }
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private class LanguageList extends ObjectSelectionList<LanguageList.Entry> {

        public LanguageList(Minecraft minecraft) {
            super(minecraft, WhisperLanguageScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 18);
            filter("");
            if (getSelected() != null) {
                centerScrollOn(getSelected());
            }
        }

        void filter(String filter) {
            String query = filter.toLowerCase(Locale.ROOT);
            String current = VoiceMessagesTranscriber.CONFIG.getData().language();
            String selected = getSelected() != null ? getSelected().code : current;
            List<String> codes = new ArrayList<>();
            codes.add(WhisperLanguages.AUTO);
            codes.addAll(WhisperLanguages.LANGUAGES.keySet());
            List<Entry> entries = new ArrayList<>();
            for (String code : codes) {
                Entry entry = new Entry(code);
                if (query.isEmpty()
                        || code.equals(query)
                        || entry.name.getString().toLowerCase(Locale.ROOT).contains(query)) {
                    entries.add(entry);
                }
            }
            replaceEntries(entries);
            for (Entry entry : entries) {
                if (entry.code.equals(selected)) {
                    setSelected(entry);
                }
            }
            refreshScrollAmount();
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {

            private final String code;
            private final Component name;

            private Entry(String code) {
                this.code = code;
                this.name = WhisperLanguages.getDisplayName(code);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                graphics.centeredText(font, name, LanguageList.this.width / 2, getContentYMiddle() - 9 / 2, -1);
            }

            @Override
            public boolean keyPressed(KeyEvent event) {
                if (event.isSelection()) {
                    LanguageList.this.setSelected(this);
                    onDone();
                    return true;
                }
                return super.keyPressed(event);
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                LanguageList.this.setSelected(this);
                if (doubleClick) {
                    onDone();
                }
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", name);
            }

        }

    }

}

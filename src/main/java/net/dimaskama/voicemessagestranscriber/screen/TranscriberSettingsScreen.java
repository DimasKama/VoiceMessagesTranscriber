package net.dimaskama.voicemessagestranscriber.screen;

import com.mojang.blaze3d.Blaze3D;
import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;
import net.dimaskama.voicemessagestranscriber.whisper.ModelDownload;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperLanguages;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperModel;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperModels;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TranscriberSettingsScreen extends Screen {

    private static final int BUTTON_WIDTH = 150;

    private final @Nullable Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 56);
    private @Nullable ModelList modelList;
    private @Nullable Button downloadButton;
    private @Nullable Button selectButton;

    public TranscriberSettingsScreen(@Nullable Screen parent) {
        super(Component.translatable("voicemessagestranscriber.settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        layout.addTitleHeader(title, font);
        modelList = layout.addToContents(new ModelList(minecraft));

        LinearLayout footer = layout.addToFooter(LinearLayout.vertical().spacing(4));
        LinearLayout modelButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        downloadButton = modelButtons.addChild(Button.builder(Component.empty(), b -> onDownloadButton()).width(BUTTON_WIDTH).build());
        selectButton = modelButtons.addChild(Button.builder(Component.translatable("voicemessagestranscriber.settings.select"), b -> onSelectButton()).width(BUTTON_WIDTH).build());
        LinearLayout bottomButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        bottomButtons.addChild(Button.builder(
                Component.translatable("voicemessagestranscriber.settings.language", WhisperLanguages.getDisplayName(VoiceMessagesTranscriber.CONFIG.getData().language())),
                b -> minecraft.gui.setScreen(new WhisperLanguageScreen(this))
        ).width(BUTTON_WIDTH).build());
        bottomButtons.addChild(Button.builder(Component.translatable("voicemessagestranscriber.settings.open_folder"), b -> Blaze3D.openPath(WhisperModels.getModelsDir())).width(74).build());
        bottomButtons.addChild(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).width(68).build());

        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
        updateButtons();
    }

    @Override
    protected void repositionElements() {
        layout.arrangeElements();
        if (modelList != null) {
            modelList.updateSize(width, layout);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (modelList != null) {
            modelList.refreshEntries();
        }
        updateButtons();
    }

    private void updateButtons() {
        if (modelList == null || downloadButton == null || selectButton == null) {
            return;
        }
        ModelList.Entry entry = modelList.getSelected();
        if (entry == null) {
            downloadButton.setMessage(Component.translatable("voicemessagestranscriber.settings.download"));
            downloadButton.active = false;
            selectButton.active = false;
            return;
        }
        ModelDownload download = WhisperModels.getDownload(entry.fileName);
        boolean installed = WhisperModels.isInstalled(entry.fileName);
        if (download != null) {
            downloadButton.setMessage(Component.translatable("voicemessagestranscriber.settings.cancel_download"));
            downloadButton.active = true;
        } else if (installed) {
            downloadButton.setMessage(Component.translatable("voicemessagestranscriber.settings.delete"));
            downloadButton.active = true;
        } else {
            downloadButton.setMessage(Component.translatable("voicemessagestranscriber.settings.download"));
            downloadButton.active = entry.model != null;
        }
        selectButton.active = installed && !entry.fileName.equals(VoiceMessagesTranscriber.CONFIG.getData().model());
    }

    private void onDownloadButton() {
        ModelList.Entry entry = modelList != null ? modelList.getSelected() : null;
        if (entry == null) {
            return;
        }
        ModelDownload download = WhisperModels.getDownload(entry.fileName);
        if (download != null) {
            download.cancel();
        } else if (WhisperModels.isInstalled(entry.fileName)) {
            minecraft.gui.setScreen(new ConfirmScreen(
                    confirmed -> {
                        if (confirmed) {
                            delete(entry.fileName);
                        }
                        minecraft.gui.setScreen(this);
                    },
                    Component.translatable("voicemessagestranscriber.settings.delete.title"),
                    Component.translatable("voicemessagestranscriber.settings.delete.message", entry.fileName)
            ));
        } else if (entry.model != null) {
            WhisperModels.startDownload(entry.model);
        }
        updateButtons();
    }

    private void delete(String fileName) {
        try {
            WhisperModels.delete(fileName);
            if (fileName.equals(VoiceMessagesTranscriber.CONFIG.getData().model())) {
                VoiceMessagesTranscriber.CONFIG.setData(VoiceMessagesTranscriber.CONFIG.getData().withModel(null));
                VoiceMessagesTranscriber.CONFIG.save();
            }
        } catch (IOException e) {
            VoiceMessagesTranscriber.LOGGER.error("Failed to delete model {}", fileName, e);
        }
    }

    private void onSelectButton() {
        ModelList.Entry entry = modelList != null ? modelList.getSelected() : null;
        if (entry != null && WhisperModels.isInstalled(entry.fileName)) {
            VoiceMessagesTranscriber.CONFIG.setData(VoiceMessagesTranscriber.CONFIG.getData().withModel(entry.fileName));
            VoiceMessagesTranscriber.CONFIG.save();
            updateButtons();
        }
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }

    private static String formatSize(int sizeMb) {
        return sizeMb >= 1000 ? String.format("%.1f GB", sizeMb / 1000.0F) : sizeMb + " MB";
    }

    private class ModelList extends ObjectSelectionList<ModelList.Entry> {

        private Set<String> shownCustomModels = Set.of();

        public ModelList(Minecraft minecraft) {
            super(minecraft, TranscriberSettingsScreen.this.width, layout.getContentHeight(), layout.getHeaderHeight(), 24);
            refreshEntries();
            String selectedModel = VoiceMessagesTranscriber.CONFIG.getData().model();
            for (Entry entry : children()) {
                if (entry.fileName.equals(selectedModel)) {
                    setSelected(entry);
                    centerScrollOn(entry);
                    break;
                }
            }
        }

        void refreshEntries() {
            Set<String> catalog = new HashSet<>();
            WhisperModels.CATALOG.forEach(m -> catalog.add(m.fileName()));
            Set<String> custom = new HashSet<>();
            for (String installed : WhisperModels.getInstalled()) {
                if (!catalog.contains(installed)) {
                    custom.add(installed);
                }
            }
            if (!children().isEmpty() && custom.equals(shownCustomModels)) {
                return;
            }
            shownCustomModels = custom;
            String selected = getSelected() != null ? getSelected().fileName : null;
            List<Entry> entries = new ArrayList<>();
            for (WhisperModel model : WhisperModels.CATALOG) {
                entries.add(new Entry(model.fileName(), model));
            }
            custom.stream().sorted().forEach(fileName -> entries.add(new Entry(fileName, null)));
            replaceEntries(entries);
            for (Entry entry : entries) {
                if (entry.fileName.equals(selected)) {
                    setSelected(entry);
                }
            }
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        private class Entry extends ObjectSelectionList.Entry<Entry> {

            private final String fileName;
            private final @Nullable WhisperModel model;
            private final Component name;

            private Entry(String fileName, @Nullable WhisperModel model) {
                this.fileName = fileName;
                this.model = model;
                this.name = Component.literal(model != null ? model.name() : fileName);
            }

            @Override
            public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
                boolean active = fileName.equals(VoiceMessagesTranscriber.CONFIG.getData().model()) && WhisperModels.isInstalled(fileName);
                int x = getContentX() + 2;
                int y = getContentY() + 1;
                MutableComponent title = name.copy();
                if (active) {
                    title.append(" ").append(Component.translatable("voicemessagestranscriber.settings.active").withStyle(ChatFormatting.GREEN));
                }
                graphics.text(font, title, x, y, -1);
                graphics.text(font, getStatus(), x, y + 11, 0xFF909090);
            }

            private Component getStatus() {
                MutableComponent status = Component.empty();
                if (model != null) {
                    status.append("~" + formatSize(model.sizeMb())).append(" · ");
                }
                if (WhisperModel.isEnglishOnly(fileName)) {
                    status.append(Component.translatable("voicemessagestranscriber.settings.english_only")).append(" · ");
                }
                ModelDownload download = WhisperModels.getDownload(fileName);
                if (download != null) {
                    float progress = download.getProgress();
                    status.append(progress >= 0.0F
                            ? Component.translatable("voicemessagestranscriber.settings.downloading", (int) (progress * 100.0F) + "%")
                            : Component.translatable("voicemessagestranscriber.settings.downloading", download.getDownloadedBytes() / (1024 * 1024) + " MB"))
                            .withStyle(ChatFormatting.YELLOW);
                } else if (WhisperModels.isInstalled(fileName)) {
                    status.append(Component.translatable("voicemessagestranscriber.settings.installed").withStyle(ChatFormatting.AQUA));
                } else {
                    status.append(Component.translatable("voicemessagestranscriber.settings.not_installed"));
                }
                return status;
            }

            @Override
            public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
                ModelList.this.setSelected(this);
                if (doubleClick) {
                    if (WhisperModels.isInstalled(fileName)) {
                        onSelectButton();
                    } else if (model != null && WhisperModels.getDownload(fileName) == null) {
                        WhisperModels.startDownload(model);
                    }
                }
                updateButtons();
                return super.mouseClicked(event, doubleClick);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", name);
            }

        }

    }

}

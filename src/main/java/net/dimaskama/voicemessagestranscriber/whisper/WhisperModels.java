package net.dimaskama.voicemessagestranscriber.whisper;

import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class WhisperModels {

    public static final List<WhisperModel> CATALOG = List.of(
            new WhisperModel("tiny", 75),
            new WhisperModel("tiny.en", 75),
            new WhisperModel("tiny-q5_1", 31),
            new WhisperModel("base", 142),
            new WhisperModel("base.en", 142),
            new WhisperModel("base-q5_1", 57),
            new WhisperModel("small", 466),
            new WhisperModel("small.en", 466),
            new WhisperModel("small-q5_1", 181),
            new WhisperModel("medium", 1500),
            new WhisperModel("medium.en", 1500),
            new WhisperModel("medium-q5_0", 514),
            new WhisperModel("large-v3-turbo", 1620),
            new WhisperModel("large-v3-turbo-q5_0", 547),
            new WhisperModel("large-v3", 3100),
            new WhisperModel("large-v3-q5_0", 1080)
    );

    private static final String MODEL_EXTENSION = ".bin";
    private static final Map<String, ModelDownload> DOWNLOADS = new ConcurrentHashMap<>();
    private static Path modelsDir;

    private WhisperModels() {
    }

    public static void init(Path modelsDir) {
        WhisperModels.modelsDir = modelsDir.toAbsolutePath().normalize();
        try {
            Files.createDirectories(WhisperModels.modelsDir);
        } catch (IOException e) {
            VoiceMessagesTranscriber.LOGGER.error("Failed to create models directory {}", WhisperModels.modelsDir, e);
        }
    }

    public static Path getModelsDir() {
        return modelsDir;
    }

    public static boolean isValidFileName(String fileName) {
        if (fileName.length() <= MODEL_EXTENSION.length()
                || !fileName.endsWith(MODEL_EXTENSION)
                || fileName.startsWith(".")
                || fileName.indexOf('/') >= 0
                || fileName.indexOf('\\') >= 0) {
            return false;
        }
        try {
            Path path = modelsDir.resolve(fileName).normalize();
            return modelsDir.equals(path.getParent()) && path.getFileName().toString().equals(fileName);
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static Path getModelPath(String fileName) {
        if (!isValidFileName(fileName)) {
            throw new IllegalArgumentException("Invalid model file name: " + fileName);
        }
        return modelsDir.resolve(fileName);
    }

    public static boolean isInstalled(String fileName) {
        return isValidFileName(fileName) && Files.isRegularFile(getModelPath(fileName)) && !DOWNLOADS.containsKey(fileName);
    }

    public static List<String> getInstalled() {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(modelsDir)) {
            return result;
        }
        try (Stream<Path> files = Files.list(modelsDir)) {
            files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(WhisperModels::isValidFileName)
                    .filter(name -> !DOWNLOADS.containsKey(name))
                    .sorted()
                    .forEach(result::add);
        } catch (IOException e) {
            VoiceMessagesTranscriber.LOGGER.error("Failed to list models in {}", modelsDir, e);
        }
        return result;
    }

    public static String getSelectedInstalled() {
        String selected = VoiceMessagesTranscriber.CONFIG.getData().model();
        return selected != null && isInstalled(selected) ? selected : null;
    }

    public static ModelDownload getDownload(String fileName) {
        return DOWNLOADS.get(fileName);
    }

    public static ModelDownload startDownload(WhisperModel model) {
        return DOWNLOADS.computeIfAbsent(model.fileName(), n -> {
            ModelDownload download = new ModelDownload(model, getModelPath(n), () -> DOWNLOADS.remove(n));
            download.start();
            return download;
        });
    }

    public static void delete(String fileName) throws IOException {
        if (!isValidFileName(fileName)) {
            throw new IOException("Invalid model file name: " + fileName);
        }
        ModelDownload download = DOWNLOADS.get(fileName);
        if (download != null) {
            download.cancel();
        }
        WhisperTranscriber.unloadIfUsing(fileName);
        Files.deleteIfExists(getModelPath(fileName));
    }

}

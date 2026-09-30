package net.dimaskama.voicemessagestranscriber.whisper;

import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperSamplingStrategy;
import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WhisperTranscriber {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "VoiceMessagesTranscriber");
        thread.setDaemon(true);
        return thread;
    });

    private static WhisperJNI whisper;
    private static WhisperContext context;
    private static String contextModel;

    private WhisperTranscriber() {
    }

    public static CompletableFuture<String> transcribe(List<short[]> audio, String modelFile, String language) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return transcribeBlocking(audio, modelFile, language);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }, EXECUTOR);
    }

    public static void unloadIfUsing(String modelFile) {
        EXECUTOR.execute(() -> {
            if (modelFile.equals(contextModel)) {
                unload();
            }
        });
    }

    private static String transcribeBlocking(List<short[]> audio, String modelFile, String language) throws IOException {
        WhisperContext ctx = getContext(modelFile);
        float[] samples = AudioResampler.toWhisperInput(audio);

        WhisperFullParams params = new WhisperFullParams(WhisperSamplingStrategy.GREEDY);
        params.nThreads = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        params.language = language;
        params.translate = false;
        params.noTimestamps = true;
        params.printProgress = false;
        params.printRealtime = false;
        params.printTimestamps = false;
        params.printSpecial = false;
        params.suppressNonSpeechTokens = true;

        long start = System.currentTimeMillis();
        int result = whisper.full(ctx, params, samples, samples.length);
        if (result != 0) {
            throw new IOException("whisper_full failed with code " + result);
        }
        StringBuilder text = new StringBuilder();
        int segments = whisper.fullNSegments(ctx);
        for (int i = 0; i < segments; i++) {
            text.append(whisper.fullGetSegmentText(ctx, i));
        }
        VoiceMessagesTranscriber.LOGGER.info("Transcribed {} ms of audio in {} ms", audio.size() * 20, System.currentTimeMillis() - start);
        return text.toString().trim();
    }

    private static WhisperContext getContext(String modelFile) throws IOException {
        if (whisper == null) {
            WhisperJNI.loadLibrary(VoiceMessagesTranscriber.LOGGER::debug);
            WhisperJNI.setLibraryLogger(VoiceMessagesTranscriber.LOGGER::debug);
            whisper = new WhisperJNI();
        }
        if (context != null && modelFile.equals(contextModel)) {
            return context;
        }
        unload();
        Path path = WhisperModels.getModelPath(modelFile);
        VoiceMessagesTranscriber.LOGGER.info("Loading whisper model {}", path);
        context = whisper.init(path);
        if (context == null) {
            throw new IOException("Failed to load model " + modelFile);
        }
        contextModel = modelFile;
        return context;
    }

    private static void unload() {
        if (context != null) {
            context.close();
            context = null;
            contextModel = null;
        }
    }

}

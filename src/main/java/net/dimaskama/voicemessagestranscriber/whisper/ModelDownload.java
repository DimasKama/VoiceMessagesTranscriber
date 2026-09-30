package net.dimaskama.voicemessagestranscriber.whisper;

import net.dimaskama.voicemessagestranscriber.VoiceMessagesTranscriber;
import net.dimaskama.voicemessagestranscriber.client.Notifications;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

public class ModelDownload {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    private final WhisperModel model;
    private final Path target;
    private final Path tempFile;
    private final Runnable onEnd;
    private volatile long downloadedBytes;
    private volatile long totalBytes = -1L;
    private volatile boolean cancelled;
    private volatile String error;

    ModelDownload(WhisperModel model, Path target, Runnable onEnd) {
        this.model = model;
        this.target = target;
        this.tempFile = target.resolveSibling(target.getFileName() + ".part");
        this.onEnd = onEnd;
    }

    void start() {
        Thread thread = new Thread(this::run, "Whisper model download (" + model.name() + ")");
        thread.setDaemon(true);
        thread.start();
    }

    private void run() {
        VoiceMessagesTranscriber.LOGGER.info("Downloading whisper model {} from {}", model.name(), model.downloadUri());
        try {
            HttpRequest request = HttpRequest.newBuilder(model.downloadUri())
                    .header("User-Agent", VoiceMessagesTranscriber.MOD_ID)
                    .timeout(RESPONSE_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                if (!"https".equalsIgnoreCase(response.uri().getScheme())) {
                    throw new IOException("Insecure download location: " + response.uri());
                }
                long maxBytes = model.maxDownloadBytes();
                totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
                if (totalBytes > maxBytes) {
                    throw new IOException("Model file is too large: " + totalBytes + " bytes");
                }
                Files.createDirectories(target.getParent());
                try (OutputStream out = Files.newOutputStream(tempFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (cancelled) {
                            break;
                        }
                        if (downloadedBytes + read > maxBytes) {
                            throw new IOException("Model file is too large: more than " + maxBytes + " bytes");
                        }
                        out.write(buffer, 0, read);
                        downloadedBytes += read;
                    }
                }
            }
            if (cancelled) {
                Files.deleteIfExists(tempFile);
                VoiceMessagesTranscriber.LOGGER.info("Download of whisper model {} cancelled", model.name());
            } else {
                if (totalBytes > 0L && downloadedBytes != totalBytes) {
                    throw new IOException("Incomplete download: " + downloadedBytes + "/" + totalBytes + " bytes");
                }
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                VoiceMessagesTranscriber.LOGGER.info("Whisper model {} downloaded to {}", model.name(), target);
            }
        } catch (Exception e) {
            error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            VoiceMessagesTranscriber.LOGGER.error("Failed to download whisper model {}", model.name(), e);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {
            }
            Notifications.error(Component.translatable("voicemessagestranscriber.download.failed", model.name()), error);
        } finally {
            onEnd.run();
        }
    }

    public void cancel() {
        cancelled = true;
    }

    public WhisperModel getModel() {
        return model;
    }

    public float getProgress() {
        long total = totalBytes;
        return total > 0L ? (float) ((double) downloadedBytes / total) : -1.0F;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public String getError() {
        return error;
    }

}

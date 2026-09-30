package net.dimaskama.voicemessagestranscriber.whisper;

import java.net.URI;

public record WhisperModel(String name, int sizeMb) {

    private static final String DOWNLOAD_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/";

    public String fileName() {
        return "ggml-" + name + ".bin";
    }

    public URI downloadUri() {
        return URI.create(DOWNLOAD_URL + fileName());
    }

    public long maxDownloadBytes() {
        return sizeMb * 1024L * 1024L * 3L / 2L;
    }

    public boolean isEnglishOnly() {
        return isEnglishOnly(fileName());
    }

    public static boolean isEnglishOnly(String fileName) {
        return fileName.contains(".en.") || fileName.contains(".en-");
    }

}

package net.dimaskama.voicemessagestranscriber.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dimaskama.voicemessagestranscriber.whisper.WhisperLanguages;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record TranscriberConfig(@Nullable String model, String language) {

    public static final TranscriberConfig DEFAULT = new TranscriberConfig(null, WhisperLanguages.AUTO);

    public static final Codec<TranscriberConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("model").forGetter(config -> Optional.ofNullable(config.model())),
            JsonConfig.defaultedField(Codec.STRING, "language", () -> WhisperLanguages.AUTO).forGetter(TranscriberConfig::language)
    ).apply(instance, (model, language) -> new TranscriberConfig(model.orElse(null), language)));

    public TranscriberConfig {
        if (!WhisperLanguages.isKnown(language)) {
            language = WhisperLanguages.AUTO;
        }
    }

    public TranscriberConfig withModel(@Nullable String model) {
        return new TranscriberConfig(model, language);
    }

    public TranscriberConfig withLanguage(String language) {
        return new TranscriberConfig(model, language);
    }

}

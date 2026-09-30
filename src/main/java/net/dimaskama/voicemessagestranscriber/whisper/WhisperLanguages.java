package net.dimaskama.voicemessagestranscriber.whisper;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class WhisperLanguages {

    public static final String AUTO = "auto";
    public static final Map<String, String> LANGUAGES = new LinkedHashMap<>();

    static {
        String[] pairs = {
                "en", "English", "zh", "Chinese", "de", "German", "es", "Spanish", "ru", "Russian",
                "ko", "Korean", "fr", "French", "ja", "Japanese", "pt", "Portuguese", "tr", "Turkish",
                "pl", "Polish", "ca", "Catalan", "nl", "Dutch", "ar", "Arabic", "sv", "Swedish",
                "it", "Italian", "id", "Indonesian", "hi", "Hindi", "fi", "Finnish", "vi", "Vietnamese",
                "he", "Hebrew", "uk", "Ukrainian", "el", "Greek", "ms", "Malay", "cs", "Czech",
                "ro", "Romanian", "da", "Danish", "hu", "Hungarian", "ta", "Tamil", "no", "Norwegian",
                "th", "Thai", "ur", "Urdu", "hr", "Croatian", "bg", "Bulgarian", "lt", "Lithuanian",
                "la", "Latin", "mi", "Maori", "ml", "Malayalam", "cy", "Welsh", "sk", "Slovak",
                "te", "Telugu", "fa", "Persian", "lv", "Latvian", "bn", "Bengali", "sr", "Serbian",
                "az", "Azerbaijani", "sl", "Slovenian", "kn", "Kannada", "et", "Estonian", "mk", "Macedonian",
                "br", "Breton", "eu", "Basque", "is", "Icelandic", "hy", "Armenian", "ne", "Nepali",
                "mn", "Mongolian", "bs", "Bosnian", "kk", "Kazakh", "sq", "Albanian", "sw", "Swahili",
                "gl", "Galician", "mr", "Marathi", "pa", "Punjabi", "si", "Sinhala", "km", "Khmer",
                "sn", "Shona", "yo", "Yoruba", "so", "Somali", "af", "Afrikaans", "oc", "Occitan",
                "ka", "Georgian", "be", "Belarusian", "tg", "Tajik", "sd", "Sindhi", "gu", "Gujarati",
                "am", "Amharic", "yi", "Yiddish", "lo", "Lao", "uz", "Uzbek", "fo", "Faroese",
                "ht", "Haitian Creole", "ps", "Pashto", "tk", "Turkmen", "nn", "Nynorsk", "mt", "Maltese",
                "sa", "Sanskrit", "lb", "Luxembourgish", "my", "Myanmar", "bo", "Tibetan", "tl", "Tagalog",
                "mg", "Malagasy", "as", "Assamese", "tt", "Tatar", "haw", "Hawaiian", "ln", "Lingala",
                "ha", "Hausa", "ba", "Bashkir", "jw", "Javanese", "su", "Sundanese", "yue", "Cantonese",
        };
        for (int i = 0; i < pairs.length; i += 2) {
            LANGUAGES.put(pairs[i], pairs[i + 1]);
        }
    }

    private WhisperLanguages() {
    }

    public static boolean isKnown(String code) {
        return AUTO.equals(code) || LANGUAGES.containsKey(code);
    }

    public static Component getDisplayName(String code) {
        if (AUTO.equals(code)) {
            return Component.translatable("voicemessagestranscriber.language.auto");
        }
        String english = LANGUAGES.getOrDefault(code, code);
        String localized = getLocalizedName(code);
        return Component.literal(localized != null && !localized.equalsIgnoreCase(english) ? localized + " (" + english + ")" : english);
    }

    private static String getLocalizedName(String code) {
        try {
            String mcLanguage = Minecraft.getInstance().options.languageCode;
            Locale uiLocale = Locale.forLanguageTag(mcLanguage.replace('_', '-'));
            String name = Locale.of(code).getDisplayLanguage(uiLocale);
            if (name.isEmpty() || name.equalsIgnoreCase(code)) {
                return null;
            }
            return name.substring(0, 1).toUpperCase(uiLocale) + name.substring(1);
        } catch (Exception e) {
            return null;
        }
    }

}

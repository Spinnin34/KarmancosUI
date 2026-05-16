package p.karmancos.gui.bedrock;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Options used when translating a KarmancosUI inventory GUI into a Bedrock form.
 */
public class BedrockFormOptions {

    private String content = "";
    private boolean fallbackToJavaInventory = true;
    private boolean includeLore = true;
    private boolean includeSlotNumbers = false;
    private Locale locale = Locale.ROOT;
    private BiFunction<String, Locale, String> translator;

    public static BedrockFormOptions defaults() {
        return new BedrockFormOptions();
    }

    public String getContent() {
        return content;
    }

    public BedrockFormOptions setContent(String content) {
        this.content = Objects.requireNonNullElse(content, "");
        return this;
    }

    public boolean isFallbackToJavaInventory() {
        return fallbackToJavaInventory;
    }

    public BedrockFormOptions setFallbackToJavaInventory(boolean fallbackToJavaInventory) {
        this.fallbackToJavaInventory = fallbackToJavaInventory;
        return this;
    }

    public boolean isIncludeLore() {
        return includeLore;
    }

    public BedrockFormOptions setIncludeLore(boolean includeLore) {
        this.includeLore = includeLore;
        return this;
    }

    public boolean isIncludeSlotNumbers() {
        return includeSlotNumbers;
    }

    public BedrockFormOptions setIncludeSlotNumbers(boolean includeSlotNumbers) {
        this.includeSlotNumbers = includeSlotNumbers;
        return this;
    }

    public Locale getLocale() {
        return locale;
    }

    public BedrockFormOptions setLocale(Locale locale) {
        this.locale = Objects.requireNonNullElse(locale, Locale.ROOT);
        return this;
    }

    public BiFunction<String, Locale, String> getTranslator() {
        return translator;
    }

    public BedrockFormOptions setTranslator(BiFunction<String, Locale, String> translator) {
        this.translator = translator;
        return this;
    }

    public String translate(String text) {
        if (text == null || text.isEmpty() || translator == null) {
            return text == null ? "" : text;
        }
        String translated = translator.apply(text, locale);
        return translated == null ? text : translated;
    }
}

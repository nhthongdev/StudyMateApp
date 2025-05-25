package hcmute.edu.vn.thongvavan.finalproject.model;

/**
 * Model class for translation result
 */
public class TranslationResult {
    private final boolean success;
    private final String translatedText;
    private final String errorMessage;
    private final String originalText;

    /**
     * Constructor for TranslationResult
     * @param success Whether translation was successful
     * @param translatedText Translated text
     * @param errorMessage Error message if translation failed
     * @param originalText Original text that was translated
     */
    public TranslationResult(boolean success, String translatedText, String errorMessage, String originalText) {
        this.success = success;
        this.translatedText = translatedText;
        this.errorMessage = errorMessage;
        this.originalText = originalText;
    }

    /**
     * Check if translation was successful
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Get the translated text
     * @return Translated text
     */
    public String getTranslatedText() {
        return translatedText;
    }

    /**
     * Get the error message
     * @return Error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Get the original text
     * @return Original text
     */
    public String getOriginalText() {
        return originalText;
    }

    /**
     * Check if the translation result is empty
     * @return true if empty, false otherwise
     */
    public boolean isEmpty() {
        return translatedText == null || translatedText.isEmpty();
    }
}

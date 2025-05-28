package hcmute.edu.vn.thongvavan.finalproject.model;

/**
 * Model class for storing translation history items
 */
public class TranslationHistoryItem {
    
    private int id;
    private String sourceText;
    private String translatedText;
    private String sourceLanguageCode;
    private String targetLanguageCode;
    private String sourceLanguageName;
    private String targetLanguageName;
    private long timestamp;
    private String imageUri;
    
    /**
     * Constructor for TranslationHistoryItem
     * 
     * @param sourceText Source text
     * @param translatedText Translated text
     * @param sourceLanguageCode Source language code
     * @param targetLanguageCode Target language code
     * @param sourceLanguageName Source language name
     * @param targetLanguageName Target language name
     * @param timestamp Timestamp when the translation was created
     * @param imageUri URI of the image (if any)
     */
    public TranslationHistoryItem(String sourceText, String translatedText, 
                                 String sourceLanguageCode, String targetLanguageCode,
                                 String sourceLanguageName, String targetLanguageName,
                                 long timestamp, String imageUri) {
        this.sourceText = sourceText;
        this.translatedText = translatedText;
        this.sourceLanguageCode = sourceLanguageCode;
        this.targetLanguageCode = targetLanguageCode;
        this.sourceLanguageName = sourceLanguageName;
        this.targetLanguageName = targetLanguageName;
        this.timestamp = timestamp;
        this.imageUri = imageUri;
    }
    
    // Getters and Setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSourceText() {
        return sourceText;
    }
    
    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }
    
    public String getTranslatedText() {
        return translatedText;
    }
    
    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
    }
    
    public String getSourceLanguageCode() {
        return sourceLanguageCode;
    }
    
    public void setSourceLanguageCode(String sourceLanguageCode) {
        this.sourceLanguageCode = sourceLanguageCode;
    }
    
    public String getTargetLanguageCode() {
        return targetLanguageCode;
    }
    
    public void setTargetLanguageCode(String targetLanguageCode) {
        this.targetLanguageCode = targetLanguageCode;
    }
    
    public String getSourceLanguageName() {
        return sourceLanguageName;
    }
    
    public void setSourceLanguageName(String sourceLanguageName) {
        this.sourceLanguageName = sourceLanguageName;
    }
    
    public String getTargetLanguageName() {
        return targetLanguageName;
    }
    
    public void setTargetLanguageName(String targetLanguageName) {
        this.targetLanguageName = targetLanguageName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getImageUri() {
        return imageUri;
    }
    
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}

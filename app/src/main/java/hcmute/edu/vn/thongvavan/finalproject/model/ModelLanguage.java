package hcmute.edu.vn.thongvavan.finalproject.model;

/**
 * Model class to represent a language with its code and display name
 */
public class ModelLanguage {
    private String languageCode;
    private String languageTitle;

    public ModelLanguage(String languageCode, String languageTitle) {
        this.languageCode = languageCode;
        this.languageTitle = languageTitle;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    public String getLanguageTitle() {
        return languageTitle;
    }

    public void setLanguageTitle(String languageTitle) {
        this.languageTitle = languageTitle;
    }

    @Override
    public String toString() {
        // This will be displayed in the Spinner
        return languageTitle;
    }
}

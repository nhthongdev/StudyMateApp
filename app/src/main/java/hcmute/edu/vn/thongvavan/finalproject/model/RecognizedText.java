package hcmute.edu.vn.thongvavan.finalproject.model;

/**
 * Model class representing recognized text data
 */
public class RecognizedText {
    private String text;
    private String statistics;
    private boolean isSuccess;
    private String errorMessage;

    // Constructor for successful text recognition
    public RecognizedText(String text, String statistics) {
        this.text = text;
        this.statistics = statistics;
        this.isSuccess = true;
        this.errorMessage = null;
    }

    // Constructor for failed text recognition
    public RecognizedText(String errorMessage) {
        this.text = "";
        this.statistics = "";
        this.isSuccess = false;
        this.errorMessage = errorMessage;
    }

    // Getters
    public String getText() {
        return text;
    }

    public String getStatistics() {
        return statistics;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Check if text is empty
    public boolean isEmpty() {
        return text == null || text.isEmpty();
    }
}

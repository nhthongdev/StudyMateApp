package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TextRecognitionUtils {

    /**
     * Process recognized text to improve readability
     * @param text The text recognized by ML Kit
     * @return Processed text with improved formatting
     */
    public static String processRecognizedText(Text text) {
        StringBuilder processedText = new StringBuilder();
        List<String> blocks = new ArrayList<>();

        // Extract text blocks
        for (Text.TextBlock block : text.getTextBlocks()) {
            blocks.add(block.getText());
        }

        // Join blocks with proper spacing
        for (int i = 0; i < blocks.size(); i++) {
            processedText.append(blocks.get(i));
            // Add line break between blocks for better readability
            if (i < blocks.size() - 1) {
                processedText.append("\n\n");
            }
        }

        return processedText.toString();
    }

    /**
     * Copy text to clipboard
     * @param context Application context
     * @param text Text to copy
     * @param label Label for the clipboard data
     */
    public static void copyToClipboard(Context context, String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get text statistics
     * @param text The text to analyze
     * @return String containing statistics about the text
     */
    public static String getTextStatistics(String text) {
        if (text == null || text.isEmpty()) {
            return "No text available";
        }

        int charCount = text.length();
        int wordCount = text.split("\\s+").length;
        int lineCount = text.split("\n").length;

        return String.format("Characters: %d | Words: %d | Lines: %d", 
                charCount, wordCount, lineCount);
    }
}

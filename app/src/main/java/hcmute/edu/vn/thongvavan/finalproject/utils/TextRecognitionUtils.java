package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;

/**
 * Tiện ích xử lý nhận diện và xử lý văn bản
 */
public class TextRecognitionUtils {
    private static final String TAG = "TextRecognitionUtils";

    /**
     * Nhận diện văn bản từ URI ảnh
     * @param context Context để truy cập ContentResolver
     * @param imageUri URI của ảnh cần nhận diện văn bản
     * @param listener Listener để nhận kết quả hoặc lỗi
     */
    public static void recognizeTextFromImage(Context context, Uri imageUri, TextRecognitionListener listener) {
        if (context == null || imageUri == null || listener == null) {
            if (listener != null) {
                listener.onError("Invalid parameters");
            }
            return;
        }

        // Thử nhận diện với nhiều loại TextRecognizer khác nhau để hỗ trợ nhiều ngôn ngữ
        try {
            recognizeWithMultipleRecognizers(context, imageUri, listener);
        } catch (IOException e) {
            Log.e(TAG, "recognizeTextFromImage: ", e);
            listener.onError(e.getMessage());
        }
    }

    /**
     * Nhận diện văn bản từ URI ảnh và trả về Task
     * @param context Context để truy cập ContentResolver
     * @param imageUri URI của ảnh cần nhận diện văn bản
     * @return Task<Text> để xử lý kết quả hoặc lỗi
     */
    public static Task<Text> recognizeTextFromImageTask(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            throw new IllegalArgumentException("Context or imageUri is null");
        }

        // Khởi tạo TextRecognizer mặc định (Latin)
        TextRecognizer textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        try {
            // Chuẩn bị InputImage từ URI ảnh
            InputImage inputImage = InputImage.fromFilePath(context, imageUri);

            // Bắt đầu quá trình nhận diện văn bản và trả về Task
            return textRecognizer.process(inputImage);
        } catch (IOException e) {
            Log.e(TAG, "recognizeTextFromImageTask: ", e);
            throw new RuntimeException("Error processing image: " + e.getMessage());
        }
    }

    /**
     * Nhận diện văn bản với nhiều loại TextRecognizer khác nhau
     * @param context Context để truy cập ContentResolver
     * @param imageUri URI của ảnh cần nhận diện văn bản
     * @param listener Listener để nhận kết quả hoặc lỗi
     * @throws IOException Nếu có lỗi khi xử lý ảnh
     */
    private static void recognizeWithMultipleRecognizers(Context context, Uri imageUri, TextRecognitionListener listener) throws IOException {
        // Chuẩn bị InputImage từ URI ảnh
        InputImage inputImage = InputImage.fromFilePath(context, imageUri);
        
        // Danh sách các TextRecognizer để thử
        List<TextRecognizer> recognizers = new ArrayList<>();
        
        // Thêm các loại TextRecognizer khác nhau
        recognizers.add(TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)); // Latin
        recognizers.add(TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build())); // Tiếng Trung
        recognizers.add(TextRecognition.getClient(new JapaneseTextRecognizerOptions.Builder().build())); // Tiếng Nhật
        recognizers.add(TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build())); // Tiếng Hàn
        recognizers.add(TextRecognition.getClient(new DevanagariTextRecognizerOptions.Builder().build())); // Tiếng Ấn Độ
        
        // Biến để theo dõi kết quả tốt nhất
        final Text[] bestResult = {null};
        final int[] completedCount = {0};
        final boolean[] hasSucceeded = {false};
        
        // Thử từng TextRecognizer
        for (TextRecognizer recognizer : recognizers) {
            recognizer.process(inputImage)
                .addOnSuccessListener(text -> {
                    completedCount[0]++;
                    
                    // Nếu kết quả có văn bản và tốt hơn kết quả hiện tại
                    if (!text.getText().isEmpty() && (bestResult[0] == null || text.getText().length() > bestResult[0].getText().length())) {
                        bestResult[0] = text;
                    }
                    
                    // Nếu đã hoàn thành tất cả các recognizer hoặc đã tìm thấy kết quả tốt
                    if (completedCount[0] == recognizers.size() && !hasSucceeded[0]) {
                        hasSucceeded[0] = true;
                        if (bestResult[0] != null) {
                            String processedText = processRecognizedText(bestResult[0]);
                            String statistics = getTextStatistics(processedText);
                            listener.onSuccess(new RecognizedText(processedText, statistics));
                        } else {
                            listener.onError("Không thể nhận diện văn bản từ hình ảnh");
                        }
                        
                        // Đóng tất cả các recognizer
                        for (TextRecognizer rec : recognizers) {
                            rec.close();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    completedCount[0]++;
                    
                    // Nếu đã hoàn thành tất cả các recognizer và không có kết quả nào
                    if (completedCount[0] == recognizers.size() && !hasSucceeded[0]) {
                        hasSucceeded[0] = true;
                        if (bestResult[0] != null) {
                            String processedText = processRecognizedText(bestResult[0]);
                            String statistics = getTextStatistics(processedText);
                            listener.onSuccess(new RecognizedText(processedText, statistics));
                        } else {
                            listener.onError("Không thể nhận diện văn bản từ hình ảnh: " + e.getMessage());
                        }
                        
                        // Đóng tất cả các recognizer
                        for (TextRecognizer rec : recognizers) {
                            rec.close();
                        }
                    }
                });
        }
    }
    
    /**
     * Xử lý văn bản đã nhận diện để cải thiện khả năng đọc
     * @param text Văn bản được nhận diện bởi ML Kit
     * @return Văn bản đã được xử lý với định dạng tốt hơn
     */
    public static String processRecognizedText(Text text) {
        StringBuilder processedText = new StringBuilder();
        List<String> blocks = new ArrayList<>();

        // Trích xuất các khối văn bản
        for (Text.TextBlock block : text.getTextBlocks()) {
            blocks.add(block.getText());
        }

        // Nối các khối với khoảng cách thích hợp
        for (int i = 0; i < blocks.size(); i++) {
            processedText.append(blocks.get(i));
            // Thêm ngắt dòng giữa các khối để dễ đọc hơn
            if (i < blocks.size() - 1) {
                processedText.append("\n\n");
            }
        }

        return processedText.toString();
    }

    /**
     * Sao chép văn bản vào clipboard
     * @param context Context để truy cập ClipboardManager
     * @param text Văn bản cần sao chép
     * @param label Nhãn cho dữ liệu clipboard
     */
    public static void copyToClipboard(Context context, String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Đã sao chép vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
    }

    /**
     * Lấy thống kê văn bản
     * @param text Văn bản cần phân tích
     * @return Chuỗi chứa thống kê về văn bản
     */
    public static String getTextStatistics(String text) {
        if (text == null || text.isEmpty()) {
            return "Không có văn bản";
        }

        int charCount = text.length();
        int wordCount = text.split("\\s+").length;
        int lineCount = text.split("\n").length;

        return String.format("Ký tự: %d | Từ: %d | Dòng: %d", 
                charCount, wordCount, lineCount);
    }

    /**
     * Kiểm tra xem văn bản có trống không
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản trống, false nếu không
     */
    public static boolean isTextEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Interface để lắng nghe kết quả nhận diện văn bản
     */
    public interface TextRecognitionListener {
        void onSuccess(RecognizedText recognizedText);
        void onError(String errorMessage);
    }
}

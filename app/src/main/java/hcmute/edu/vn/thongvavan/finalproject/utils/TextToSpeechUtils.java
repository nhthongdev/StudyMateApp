package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Lớp tiện ích xử lý Text-to-Speech
 */
public class TextToSpeechUtils {
    private static final String TAG = "TextToSpeechUtils";
    
    private TextToSpeech tts;
    private Context context;
    private boolean isInitialized = false;
    
    /**
     * Khởi tạo TextToSpeechUtils với context
     * @param context Context để khởi tạo TextToSpeech
     */
    public TextToSpeechUtils(Context context) {
        this.context = context;
        initTextToSpeech();
    }
    
    /**
     * Khởi tạo Text-to-Speech engine
     */
    private void initTextToSpeech() {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true;
                Log.d(TAG, "Text-to-Speech đã được khởi tạo thành công");
            } else {
                isInitialized = false;
                Log.e(TAG, "Không thể khởi tạo Text-to-Speech");
            }
        });
    }
    
    /**
     * Đọc văn bản bằng Text-to-Speech
     * @param text Văn bản cần đọc
     * @param languageCode Mã ngôn ngữ
     * @return true nếu thành công, false nếu thất bại
     */
    public boolean speakText(String text, String languageCode) {
        if (!isInitialized || tts == null) {
            Log.e(TAG, "Text-to-Speech chưa được khởi tạo");
            return false;
        }
        
        // Thiết lập ngôn ngữ
        int result = tts.setLanguage(new Locale(languageCode));
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e(TAG, "Ngôn ngữ không được hỗ trợ: " + languageCode);
            return false;
        } else {
            // Đọc văn bản
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            return true;
        }
    }
    
    /**
     * Kiểm tra xem ngôn ngữ có được hỗ trợ không
     * @param languageCode Mã ngôn ngữ cần kiểm tra
     * @return true nếu ngôn ngữ được hỗ trợ, false nếu không
     */
    public boolean isLanguageSupported(String languageCode) {
        if (!isInitialized || tts == null) {
            return false;
        }
        
        int result = tts.setLanguage(new Locale(languageCode));
        return !(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED);
    }
    
    /**
     * Giải phóng tài nguyên khi không còn sử dụng
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isInitialized = false;
        }
    }
}

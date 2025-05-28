package hcmute.edu.vn.thongvavan.finalproject.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseApp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;
import hcmute.edu.vn.thongvavan.finalproject.utils.LanguageIdentificationUtils;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hcmute.edu.vn.thongvavan.finalproject.model.ModelLanguage;
import hcmute.edu.vn.thongvavan.finalproject.model.TranslationResult;

/**
 * Repository for translation operations
 */
public class TranslationRepository {
    private static final String TAG = "TranslationRepository";

    private final Application application;
    private final MutableLiveData<List<ModelLanguage>> availableLanguages = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadingMessage = new MutableLiveData<>("");
    private final MutableLiveData<TranslationResult> translationResult = new MutableLiveData<>();
    private final MutableLiveData<String> detectedLanguage = new MutableLiveData<>();

    private Translator translator;
    private LanguageIdentifier languageIdentifier;
    
    // Các biến cho việc xử lý nhận diện ngôn ngữ với độ trễ
    private static final long DEBOUNCE_DELAY = 500; // 500ms delay
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable languageDetectionRunnable;

    public TranslationRepository(Application application) {
        this.application = application;
        
        // Ensure Firebase is initialized
        try {
            FirebaseApp.initializeApp(application);
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
        }
        
        fetchAvailableLanguages();
        initLanguageIdentifier();
    }

    /**
     * Initialize the language identifier
     */
    private void initLanguageIdentifier() {
        try {
            // Đảm bảo Firebase đã được khởi tạo trước khi tạo LanguageIdentifier
            if (FirebaseApp.getApps(application).isEmpty()) {
                FirebaseApp.initializeApp(application);
            }
            
            // Sử dụng phương thức getClient() mặc định
            languageIdentifier = LanguageIdentification.getClient();
            
            Log.d(TAG, "Language identifier initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Language identifier initialization failed: " + e.getMessage());
            languageIdentifier = null;
            // Set a fallback behavior when language identification fails
            detectedLanguage.setValue("en"); // Default to English
        }
    }

    /**
     * Fetch available languages for translation
     */
    public void fetchAvailableLanguages() {
        // Create a list to store languages
        List<ModelLanguage> languageArrayList = new ArrayList<>();
        
        // Get all available languages from ML Kit
        // TranslateLanguage.getAllLanguages() returns a Set of language codes
        // but we need to handle it differently to avoid ClassCastException
        List<String> languageCodeList = new ArrayList<>();
        for (String code : TranslateLanguage.getAllLanguages()) {
            languageCodeList.add(code);
        }
        
        // Convert language codes to ModelLanguage objects with display names
        for (String languageCode : languageCodeList) {
            // Hiển thị tên ngôn ngữ theo tiếng Việt hoặc tiếng Anh tùy theo ngôn ngữ
            Locale locale = new Locale(languageCode);
            
            // Thử lấy tên hiển thị bằng tiếng Việt trước
            String languageTitle = locale.getDisplayLanguage(new Locale("vi"));
            
            // Nếu tên hiển thị trùng với mã ngôn ngữ, thử lấy bằng tiếng Anh
            if (languageTitle.equals(languageCode)) {
                languageTitle = locale.getDisplayLanguage(new Locale("en"));
            }
            
            // Nếu vẫn trùng với mã ngôn ngữ, sử dụng tên hiển thị mặc định
            if (languageTitle.equals(languageCode)) {
                languageTitle = locale.getDisplayLanguage();
            }
            
            // Xử lý một số trường hợp đặc biệt
            if (languageCode.equals("zh")) {
                languageTitle = "Tiếng Trung";
            } else if (languageCode.equals("ko")) {
                languageTitle = "Tiếng Hàn";
            } else if (languageCode.equals("ja")) {
                languageTitle = "Tiếng Nhật";
            }
            
            // Viết hoa chữ cái đầu tiên nếu chưa viết hoa
            if (!languageTitle.isEmpty() && Character.isLowerCase(languageTitle.charAt(0))) {
                languageTitle = Character.toUpperCase(languageTitle.charAt(0)) + languageTitle.substring(1);
            }
            
            ModelLanguage modelLanguage = new ModelLanguage(languageCode, languageTitle);
            languageArrayList.add(modelLanguage);
        }
        
        // Sort languages alphabetically by display name
        Collections.sort(languageArrayList, (l1, l2) -> 
            l1.getLanguageTitle().compareToIgnoreCase(l2.getLanguageTitle()));
        
        // Update LiveData with the list of languages
        availableLanguages.setValue(languageArrayList);
    }

    /**
     * Get available languages
     * @return LiveData containing list of available languages
     */
    public LiveData<List<ModelLanguage>> getAvailableLanguages() {
        return availableLanguages;
    }

    /**
     * Get loading state
     * @return LiveData containing loading state
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }

    /**
     * Get loading message
     * @return LiveData containing loading message
     */
    public LiveData<String> getLoadingMessage() {
        return loadingMessage;
    }

    /**
     * Get translation result
     * @return LiveData containing translation result
     */
    public LiveData<TranslationResult> getTranslationResult() {
        return translationResult;
    }

    /**
     * Get detected language
     * @return LiveData containing detected language code
     */
    public LiveData<String> getDetectedLanguage() {
        return detectedLanguage;
    }

    // Biến đã được khai báo ở trên

    /**
     * Identify the language of a text
     * @param text Text to identify language for
     */
    public void identifyLanguage(String text) {
        if (text == null || text.isEmpty()) {
            detectedLanguage.setValue("");
            return;
        }

        // Hiển thị trạng thái đang tải (chỉ khi có văn bản dài hơn 5 ký tự)
        if (text.length() > 5) {
            isLoading.setValue(true);
            loadingMessage.setValue("Đang phát hiện ngôn ngữ...");
        }
        
        // Hủy bỏ các lần gọi trước đó chưa hoàn thành
        if (languageDetectionRunnable != null) {
            debounceHandler.removeCallbacks(languageDetectionRunnable);
        }
        
        // Tạo một runnable mới để thực hiện nhận diện ngôn ngữ
        languageDetectionRunnable = () -> {
            // Thực hiện trên luồng phụ để tránh khóa luồng UI
            new Thread(() -> {
                // Sử dụng lớp LanguageIdentificationUtils mới để nhận diện ngôn ngữ
                final LanguageIdentificationUtils languageIdUtils = LanguageIdentificationUtils.getInstance(application);
                
                // Kiểm tra trước xem văn bản có phải tiếng Việt không
                boolean isVietnamese = checkVietnameseLanguage(text);
                if (isVietnamese) {
                    Log.i(TAG, "Detected Vietnamese language based on character patterns");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        detectedLanguage.setValue("vi"); // Mã ngôn ngữ tiếng Việt
                        isLoading.setValue(false);
                    });
                    return;
                }
                
                // Kiểm tra xem văn bản có phải tiếng Tây Ban Nha không
                boolean isSpanish = checkSpanishLanguage(text);
                if (isSpanish) {
                    Log.i(TAG, "Detected Spanish language based on word patterns");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        detectedLanguage.setValue("es"); // Mã ngôn ngữ tiếng Tây Ban Nha
                        isLoading.setValue(false);
                    });
                    return;
                }
                
                // Nếu không phải tiếng Tây Ban Nha, tiếp tục sử dụng LanguageIdentificationUtils
                languageIdUtils.identifyLanguage(text, new LanguageIdentificationUtils.LanguageIdentificationListener() {
                    @Override
                    public void onLanguageIdentified(String languageCode, float confidence) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (languageCode != null && !languageCode.isEmpty()) {
                                Log.i(TAG, "Identified language: " + languageCode + " with confidence: " + confidence);
                                
                                // Kiểm tra xem ngôn ngữ có được hỗ trợ bởi Translation không
                                if (isLanguageSupportedByTranslation(languageCode)) {
                                    detectedLanguage.setValue(languageCode);
                                } else {
                                    // Nếu không được hỗ trợ, mặc định là tiếng Anh
                                    Log.i(TAG, "Language not supported by translation, defaulting to English");
                                    detectedLanguage.setValue("en");
                                }
                            } else {
                                // Nếu không xác định được ngôn ngữ, mặc định là tiếng Anh
                                Log.i(TAG, "Could not identify language, defaulting to English");
                                detectedLanguage.setValue("en");
                            }
                            isLoading.setValue(false);
                        });
                    }
                });
            }).start();
        };
        
        // Thêm độ trễ để tránh gọi liên tục khi người dùng đang nhập
        debounceHandler.postDelayed(languageDetectionRunnable, DEBOUNCE_DELAY);
    }

    /**
     * Phương thức này đã được thay thế bởi LanguageIdentificationUtils
     * @deprecated Sử dụng LanguageIdentificationUtils thay thế
     */
    private String detectLanguageFromText(String text) {
        // Mặc định là tiếng Anh
        return "en";
    }
    
    /**
     * Kiểm tra nếu mã ngôn ngữ được hỗ trợ bời ML Kit Translation
     * @param languageCode Mã ngôn ngữ cần kiểm tra
     * @return true nếu ngôn ngữ được hỗ trợ
     */
    private boolean isLanguageSupportedByTranslation(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        
        // Kiểm tra nếu mã ngôn ngữ nằm trong danh sách các ngôn ngữ hỗ trợ bời ML Kit Translation
        return TranslateLanguage.getAllLanguages().contains(languageCode);
    }
    
    /**
     * Kiểm tra xem văn bản có phải tiếng Việt không dựa trên các đặc điểm của tiếng Việt
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản có khả năng là tiếng Việt
     */
    private boolean checkVietnameseLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Đếm số lượng ký tự đặc trưng của tiếng Việt
        int vietnameseCharCount = 0;
        
        // Các ký tự đặc trưng của tiếng Việt
        String vietnameseChars = "àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổỗộơờởỡợùúủũụưứừửữđÀÁẢÃẠĂẰẲẴẶÂẦẨẪẬÈÉẺẼẸÊỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỔỖỘƠỜỞỠỢÙÚỦŨỤƯỨỪỬỮĐ";
        
        // Các từ đặc trưng của tiếng Việt
        String[] vietnameseWords = {"của", "và", "là", "trong", "với", "được", "có", "không", "này", "cho", "các", "bị", "sẽ", "đã", "phải", "còn", "bạn", "tôi", "anh", "chị", "em", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười", "chúng", "họ", "các", "những", "rằng", "thì", "vào", "ra", "trên", "dưới", "trước", "sau", "rồi", "lại", "nên", "cần", "phải", "có", "không", "cùng", "với", "của", "về", "cho", "tại", "vì", "nếu", "mà", "làm", "biết", "nói", "từ", "bởi", "khi", "cứ", "sẽ", "đã", "đang", "được", "bị", "rất", "nhiều", "ít", "quá", "còn", "vẫn", "mới", "cũng", "thêm", "gì", "nào", "ai", "mỗi", "cách", "vậy", "thế", "nên", "chỉ", "bất", "từ", "lúc", "vừa", "lại", "hay", "tất", "mọi", "vài", "cùng", "theo", "như", "vậy", "với", "thì", "làm", "việc", "người", "nước", "thời", "quốc", "phát", "triển", "kinh", "xã", "hội", "chính", "quản", "học", "sinh", "viên", "trường", "lớp", "giáo", "dục", "sức", "khỏe", "bệnh", "viện", "bác", "sĩ", "thuốc", "chữa", "bệnh", "nhân", "dân", "chính", "phủ", "quốc", "hội", "luật", "pháp", "công", "an", "quân", "đội", "quốc", "phòng", "an", "ninh", "quốc", "gia", "chống", "tội", "phạm", "bảo", "vệ", "môi", "trường", "tài", "nguyên", "thiên", "nhiên", "biển", "đảo", "sông", "núi", "rừng", "đất", "nước", "khí", "hậu", "thời", "tiết", "mưa", "nắng", "gió", "bão", "lũ", "lụt", "hạn", "hán", "nóng", "lạnh", "tuyết", "sương", "mù", "mây", "trời", "trăng", "sao", "mặt", "trời", "mặt", "trăng", "vũ", "trụ", "hành", "tinh", "sao", "chổi", "thiên", "thạch", "vũ", "trụ", "không", "gian", "thời", "gian", "năm", "tháng", "tuần", "ngày", "giờ", "phút", "giây", "quá", "khứ", "hiện", "tương", "lai", "trước", "đây", "sau", "này", "sớm", "muộn", "lâu", "mau", "chóng", "chậm", "chạp", "nhanh", "chóng", "từ", "từ", "dần", "dần", "liên", "tục", "luôn", "luôn", "thường", "xuyên", "thỉnh", "thoảng", "hàng", "ngày", "hàng", "tháng", "hàng", "năm", "thường", "niên", "hằng", "năm", "hằng", "ngày", "hằng", "giờ", "hằng", "phút", "hằng", "giây", "trước", "đây", "sau", "đó", "lúc", "này", "bây", "giờ", "lúc", "đó", "hôm", "nay", "hôm", "qua", "ngày", "mai", "tuần", "này", "tuần", "sau", "tháng", "này", "tháng", "sau", "năm", "nay", "năm", "ngoái", "năm", "sau", "mùa", "xuân", "mùa", "hạ", "mùa", "thu", "mùa", "đông", "mùa", "mưa", "mùa", "nắng", "mùa", "khô", "mùa", "lũ", "mùa", "bão", "mùa", "hè", "mùa", "đông", "mùa", "màng", "mùa", "gặt", "mùa", "vụ", "mùa", "màng", "mùa", "thu", "hoạch", "mùa", "giáng", "sinh", "mùa", "tết", "mùa", "lễ", "hội", "mùa", "cưới", "mùa", "thi", "mùa", "tuyển", "sinh", "mùa", "tuyển", "dụng", "mùa", "du", "lịch", "mùa", "mua", "sắm", "mùa", "giảm", "giá", "mùa", "khuyến", "mãi", "mùa", "sale", "mùa", "black", "friday"};
        
        // Đếm số lượng ký tự đặc trưng của tiếng Việt
        for (char c : text.toCharArray()) {
            if (vietnameseChars.indexOf(c) >= 0) {
                vietnameseCharCount++;
            }
        }
        
        // Đếm số lượng từ đặc trưng của tiếng Việt
        int vietnameseWordCount = 0;
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            for (String vWord : vietnameseWords) {
                if (word.equals(vWord)) {
                    vietnameseWordCount++;
                    break;
                }
            }
        }
        
        // Nếu có ít nhất 2 ký tự đặc trưng hoặc 1 từ đặc trưng của tiếng Việt, có khả năng là tiếng Việt
        return (vietnameseCharCount >= 2 || vietnameseWordCount >= 1);
    }
    
    /**
     * Kiểm tra xem văn bản có phải tiếng Tây Ban Nha không dựa trên các từ đặc trưng
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản có khả năng là tiếng Tây Ban Nha
     */
    private boolean checkSpanishLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Các từ đặc trưng của tiếng Tây Ban Nha
        String[] spanishWords = {"el", "la", "los", "las", "un", "una", "unos", "unas", "y", "o", "pero", "porque", 
                "como", "cuando", "donde", "quien", "que", "yo", "tu", "el", "ella", "nosotros", 
                "vosotros", "ellos", "ellas", "mi", "tu", "su", "nuestro", "vuestro", "su", "este", 
                "esta", "estos", "estas", "es", "son", "era", "serán", "tener", "hacer", "decir", 
                "ir", "ver", "venir", "querer", "poder", "deber", "para", "por", "con", "sin", "de", "en", "a", "entre",
                "hasta", "desde", "sobre", "bajo", "tras", "durante", "mediante", "según", "contra", "hacia"};
        
        // Đếm số lượng từ đặc trưng của tiếng Tây Ban Nha
        int spanishWordCount = 0;
        String[] words = text.toLowerCase().split("\\s+");
        for (String word : words) {
            for (String sWord : spanishWords) {
                if (word.equals(sWord)) {
                    spanishWordCount++;
                    break;
                }
            }
        }
        
        // Nếu có ít nhất 3 từ đặc trưng của tiếng Tây Ban Nha, có khả năng là tiếng Tây Ban Nha
        return spanishWordCount >= 3;
    }
    
    /**
     * @param text Văn bản cần kiểm tra
     * @param word Từ cần tìm
     * @return true nếu tìm thấy từ
     */
    private boolean containsWord(String text, String word) {
        String pattern = "\\b" + word + "\\b";
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            return m.find();
        } catch (Exception e) {
            // Fallback nếu có lỗi với regex
            return text.contains(" " + word + " ") || 
                   text.startsWith(word + " ") || 
                   text.endsWith(" " + word) || 
                   text.equals(word);
        }
    }
    
    /**
     * Phương thức này đã được thay thế bởi LanguageIdentificationUtils
     * @deprecated Sử dụng LanguageIdentificationUtils thay thế
     */
    private boolean containsVietnameseWords(String text) {
        return false;
    }

    /**
     * Translate text from source language to target language
     * @param text Text to translate
     * @param sourceLanguageCode Source language code
     * @param targetLanguageCode Target language code
     */
    public void translateText(String text, String sourceLanguageCode, String targetLanguageCode) {
        // Sử dụng MutableLiveData hiện có thay vì tạo mới
        
        // Check if text is empty
        if (text == null || text.isEmpty()) {
            TranslationResult result = new TranslationResult(false, "", "Text is empty", text);
            this.translationResult.setValue(result);
            return;
        }
        
        // Kiểm tra xem mã ngôn ngữ có hợp lệ không
        if (sourceLanguageCode == null || sourceLanguageCode.isEmpty() ||
            targetLanguageCode == null || targetLanguageCode.isEmpty()) {
            TranslationResult result = new TranslationResult(false, "", "Invalid language code", text);
            this.translationResult.setValue(result);
            return;
        }
        
        // Kiểm tra xem ngôn ngữ nguồn và đích có giống nhau không
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            // Nếu giống nhau, trả về văn bản gốc mà không cần dịch
            TranslationResult result = new TranslationResult(true, text, "", text);
            this.translationResult.setValue(result);
            return;
        }
        
        // Set loading state
        isLoading.setValue(true);
        
        // Log thông tin dịch để debug
        Log.d(TAG, "Translating from " + sourceLanguageCode + " to " + targetLanguageCode + ": " + text);
        
        // Create translation options
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguageCode)
                .setTargetLanguage(targetLanguageCode)
                .build();
        
        // Đóng translator cũ nếu có
        if (translator != null) {
            translator.close();
        }
        
        // Get translator
        translator = Translation.getClient(options);
        
        // Check if model is downloaded
        DownloadConditions conditions = new DownloadConditions.Builder()
                .requireWifi()
                .build();
                
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    // Model downloaded successfully, translate text
                    translator.translate(text)
                            .addOnSuccessListener(translatedText -> {
                                // Translation successful
                                Log.d(TAG, "Translation result: " + translatedText);
                                TranslationResult result = new TranslationResult(true, translatedText, "", text);
                                this.translationResult.setValue(result);
                                isLoading.setValue(false);
                            })
                            .addOnFailureListener(e -> {
                                // Translation failed
                                Log.e(TAG, "Translation error: " + e.getMessage());
                                TranslationResult result = new TranslationResult(false, "", "Translation failed: " + e.getMessage(), text);
                                this.translationResult.setValue(result);
                                isLoading.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed: " + e.getMessage());
                    TranslationResult result = new TranslationResult(false, "", "Failed to download language model: " + e.getMessage(), text);
                    this.translationResult.setValue(result);
                    isLoading.setValue(false);
                });
    }

    // Phương thức performTranslation đã được tích hợp vào translateText

    /**
     * Close the translator and language identifier
     */
    public void shutdown() {
        if (translator != null) {
            translator.close();
        }
        if (languageIdentifier != null) {
            languageIdentifier.close();
        }
    }
}

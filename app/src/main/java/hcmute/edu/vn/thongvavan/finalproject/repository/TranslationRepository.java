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

    // Biến để theo dõi thời gian chờ giữa các lần gọi identifyLanguage
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable languageDetectionRunnable;
    private static final long DEBOUNCE_DELAY = 500; // 500ms

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
                // Sử dụng phương pháp phát hiện ngôn ngữ dựa trên mẫu ký tự đặc biệt
                final String detectedCode = detectLanguageFromText(text);
                
                // Chuyển kết quả về luồng chính để cập nhật UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    // Nếu ML Kit đã được khởi tạo thành công, thử sử dụng nó như một phương pháp bổ sung
                    if (languageIdentifier != null) {
                        try {
                            // Sử dụng ML Kit để nhận diện ngôn ngữ
                            languageIdentifier.identifyLanguage(text)
                                    .addOnSuccessListener(languageCode -> {
                                        if (!languageCode.equals("und")) {
                                            Log.i(TAG, "ML Kit detected language: " + languageCode);
                                            
                                            // Ưu tiên kiểm tra các ngôn ngữ dựa trên mẫu ký tự đặc trưng
                                            if (!detectedCode.equals("en")) {
                                                Log.i(TAG, "Using pattern detection result (strong match): " + detectedCode);
                                                detectedLanguage.setValue(detectedCode);
                                            }
                                            // Kiểm tra nếu văn bản chứa nhiều từ tiếng Việt thông dụng
                                            else if (containsVietnameseWords(text)) {
                                                Log.i(TAG, "Detected Vietnamese words in text");
                                                detectedLanguage.setValue("vi");
                                            }
                                            // Nếu ML Kit phát hiện ngôn ngữ không phải tiếng Anh và được hỗ trợ bởi Translation
                                            else if (!languageCode.equals("en") && isLanguageSupportedByTranslation(languageCode)) {
                                                Log.i(TAG, "Using ML Kit non-English result: " + languageCode);
                                                detectedLanguage.setValue(languageCode);
                                            }
                                            // Thử phát hiện các ngôn ngữ phổ biến khác
                                            else {
                                                String detectedLanguageCode = detectCommonLanguages(text);
                                                if (!detectedLanguageCode.equals("en")) {
                                                    Log.i(TAG, "Detected common language: " + detectedLanguageCode);
                                                    detectedLanguage.setValue(detectedLanguageCode);
                                                } 
                                                // Cuối cùng mới sử dụng kết quả ML Kit nếu là tiếng Anh
                                                else {
                                                    Log.i(TAG, "Using ML Kit result: " + languageCode);
                                                    detectedLanguage.setValue(languageCode);
                                                }
                                            }
                                        } else {
                                            // ML Kit không thể xác định ngôn ngữ, sử dụng kết quả từ phương pháp mẫu
                                            Log.i(TAG, "ML Kit couldn't identify language, using pattern detection: " + detectedCode);
                                            detectedLanguage.setValue(detectedCode);
                                        }
                                        isLoading.setValue(false);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "ML Kit language identification failed: " + e.getMessage());
                                        // Sử dụng kết quả từ phương pháp mẫu
                                        detectedLanguage.setValue(detectedCode);
                                        isLoading.setValue(false);
                                    });
                        } catch (Exception e) {
                            Log.e(TAG, "Exception during ML Kit language identification: " + e.getMessage());
                            // Sử dụng kết quả từ phương pháp mẫu
                            detectedLanguage.setValue(detectedCode);
                            isLoading.setValue(false);
                        }
                    } else {
                        // ML Kit không khả dụng, chỉ sử dụng phương pháp mẫu
                        Log.i(TAG, "Using only pattern-based language detection: " + detectedCode);
                        detectedLanguage.setValue(detectedCode);
                        isLoading.setValue(false);
                    }
                });
            }).start();
        };
        
        // Trì hoãn việc thực hiện nhận diện ngôn ngữ để tránh gọi liên tục
        debounceHandler.postDelayed(languageDetectionRunnable, DEBOUNCE_DELAY);
    }

    /**
     * Phát hiện ngôn ngữ dựa trên các ký tự đặc biệt trong văn bản
     * @param text Văn bản cần phát hiện ngôn ngữ
     * @return Mã ngôn ngữ (vi, zh, ja, ko, en, ...)
     */
    private String detectLanguageFromText(String text) {
        if (text == null || text.isEmpty()) {
            return "en";
        }
        
        // Các mẫu ký tự đặc trưng cho từng ngôn ngữ
        String vietnameseChars = "[\u00e0\u00e1\u1ea1\u1ea3\u00e3\u00e2\u1ea7\u1ea5\u1ead\u1ea9\u1eab\u0103\u1eb1\u1eaf\u1eb7\u1eb3\u1eb5\u00e8\u00e9\u1eb9\u1ebb\u1ebd\u00ea\u1ec1\u1ebf\u1ec7\u1ec3\u1ec5\u00ec\u00ed\u1ecb\u1ec9\u0129\u00f2\u00f3\u1ecd\u1ecf\u00f5\u00f4\u1ed3\u1ed1\u1ed9\u1ed5\u1ed7\u01a1\u1edd\u1edb\u1ee3\u1edf\u1ee1\u00f9\u00fa\u1ee5\u1ee7\u0169\u01b0\u1ee9\u1ee9\u1ef1\u1eef\u1eef\u1ef3\u00fd\u1ef5\u1ef7\u1ef9\u0111]"; 
        String chineseChars = "[\u4E00-\u9FFF]"; // Phạm vi Unicode cho chữ Hán
        String japaneseChars = "[\u3040-\u309F\u30A0-\u30FF]"; // Hiragana và Katakana
        String koreanChars = "[\uAC00-\uD7AF\u1100-\u11FF]"; // Hangul
        String thaiChars = "[\u0E00-\u0E7F]"; // Thai
        
        // Đếm số lượng ký tự đặc trưng
        int vietnameseCount = countMatches(text.toLowerCase(), vietnameseChars);
        int chineseCount = countMatches(text, chineseChars);
        int japaneseCount = countMatches(text, japaneseChars);
        int koreanCount = countMatches(text, koreanChars);
        int thaiCount = countMatches(text, thaiChars);
        
        Log.d(TAG, "Language detection counts - VI: " + vietnameseCount + ", ZH: " + chineseCount + 
              ", JA: " + japaneseCount + ", KO: " + koreanCount + ", TH: " + thaiCount);
        
        // Xác định ngôn ngữ dựa trên số lượng ký tự đặc trưng
        if (vietnameseCount > 2) {
            return "vi"; // Tiếng Việt
        } else if (chineseCount > 2) {
            return "zh"; // Tiếng Trung
        } else if (japaneseCount > 2) {
            return "ja"; // Tiếng Nhật
        } else if (koreanCount > 2) {
            return "ko"; // Tiếng Hàn
        } else if (thaiCount > 2) {
            return "th"; // Tiếng Thái
        }
        
        // Mặc định là tiếng Anh nếu không phát hiện được ngôn ngữ khác
        return "en";
    }
    private int countMatches(String text, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        } catch (Exception e) {
            Log.e(TAG, "Error counting matches: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Kiểm tra nếu mã ngôn ngữ được hỗ trợ bởi ML Kit Translation
     * @param languageCode Mã ngôn ngữ cần kiểm tra
     * @return true nếu ngôn ngữ được hỗ trợ
     */
    private boolean isLanguageSupportedByTranslation(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return false;
        }
        
        // Kiểm tra nếu mã ngôn ngữ nằm trong danh sách các ngôn ngữ hỗ trợ bởi ML Kit Translation
        return TranslateLanguage.getAllLanguages().contains(languageCode);
    }
    
    /**
     * Phát hiện các ngôn ngữ phổ biến dựa trên các từ và mẫu đặc trưng
     * @param text Văn bản cần phát hiện ngôn ngữ
     * @return Mã ngôn ngữ hoặc "en" nếu không phát hiện được
     */
    private String detectCommonLanguages(String text) {
        if (text == null || text.isEmpty()) {
            return "en";
        }
        
        // Chuẩn hóa văn bản
        String normalizedText = text.toLowerCase().trim();
        
        // Các mẫu và từ đặc trưng cho các ngôn ngữ phổ biến
        
        // Tiếng Pháp (fr)
        String[] frenchWords = {"le", "la", "les", "un", "une", "des", "et", "ou", "mais", "donc", "car", "ni", "que", 
                               "qui", "quoi", "où", "comment", "pourquoi", "quand", "je", "tu", "il", "elle", "nous", 
                               "vous", "ils", "elles", "mon", "ton", "son", "notre", "votre", "leur", "ce", "cette", 
                               "ces", "est", "sont", "était", "seront", "avoir", "faire", "dire", "aller", "voir", 
                               "venir", "vouloir", "pouvoir", "falloir", "devoir"};
        
        // Tiếng Tây Ban Nha (es)
        String[] spanishWords = {"el", "la", "los", "las", "un", "una", "unos", "unas", "y", "o", "pero", "porque", 
                                "como", "cuando", "donde", "quien", "que", "yo", "tu", "el", "ella", "nosotros", 
                                "vosotros", "ellos", "ellas", "mi", "tu", "su", "nuestro", "vuestro", "su", "este", 
                                "esta", "estos", "estas", "es", "son", "era", "serán", "tener", "hacer", "decir", 
                                "ir", "ver", "venir", "querer", "poder", "deber"};
        
        // Tiếng Đức (de)
        String[] germanWords = {"der", "die", "das", "ein", "eine", "und", "oder", "aber", "weil", "wie", "wenn", 
                               "wo", "wer", "was", "ich", "du", "er", "sie", "es", "wir", "ihr", "sie", "mein", 
                               "dein", "sein", "unser", "euer", "ihr", "dieser", "diese", "dieses", "ist", "sind", 
                               "war", "werden", "haben", "machen", "sagen", "gehen", "sehen", "kommen", "wollen", 
                               "können", "müssen", "sollen"};
        
        // Tiếng Ý (it)
        String[] italianWords = {"il", "lo", "la", "i", "gli", "le", "un", "uno", "una", "e", "o", "ma", "perché", 
                                "come", "quando", "dove", "chi", "che", "cosa", "io", "tu", "lui", "lei", "noi", 
                                "voi", "loro", "mio", "tuo", "suo", "nostro", "vostro", "loro", "questo", "questa", 
                                "è", "sono", "era", "saranno", "avere", "fare", "dire", "andare", "vedere", 
                                "venire", "volere", "potere", "dovere"};
        
        // Tiếng Bồ Đào Nha (pt)
        String[] portugueseWords = {"o", "a", "os", "as", "um", "uma", "uns", "umas", "e", "ou", "mas", "porque", 
                                   "como", "quando", "onde", "quem", "que", "eu", "tu", "ele", "ela", "nós", 
                                   "vós", "eles", "elas", "meu", "teu", "seu", "nosso", "vosso", "seu", "este", 
                                   "esta", "estes", "estas", "é", "são", "era", "serão", "ter", "fazer", "dizer", 
                                   "ir", "ver", "vir", "querer", "poder", "dever"};
        
        // Tiếng Nga (ru)
        String[] russianWords = {"и", "в", "не", "что", "он", "на", "я", "с", "со", "как", 
                               "а", "то", "все", "она", "так", "его", "но", "да", "ты", "к", 
                               "у", "же", "вы", "за", "бы", "по", "только", "ее", "мне", "был", 
                               "чтобы", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему"};
        
        // Tiếng Ả Rập (ar)
        String arabicChars = "[\u0600-\u06FF]";
        
        // Tiếng Hindi (hi)
        String hindiChars = "[\u0900-\u097F]";
        
        // Đếm số lượng từ đặc trưng cho mỗi ngôn ngữ
        int frenchCount = 0;
        int spanishCount = 0;
        int germanCount = 0;
        int italianCount = 0;
        int portugueseCount = 0;
        int russianCount = 0;
        
        // Đếm số lượng từ tiếng Pháp
        for (String word : frenchWords) {
            if (containsWord(normalizedText, word)) {
                frenchCount++;
                if (frenchCount >= 2) {
                    Log.d(TAG, "Detected French words: " + frenchCount);
                    return "fr";
                }
            }
        }
        
        // Đếm số lượng từ tiếng Tây Ban Nha
        for (String word : spanishWords) {
            if (containsWord(normalizedText, word)) {
                spanishCount++;
                if (spanishCount >= 2) {
                    Log.d(TAG, "Detected Spanish words: " + spanishCount);
                    return "es";
                }
            }
        }
        
        // Đếm số lượng từ tiếng Đức
        for (String word : germanWords) {
            if (containsWord(normalizedText, word)) {
                germanCount++;
                if (germanCount >= 2) {
                    Log.d(TAG, "Detected German words: " + germanCount);
                    return "de";
                }
            }
        }
        
        // Đếm số lượng từ tiếng Ý
        for (String word : italianWords) {
            if (containsWord(normalizedText, word)) {
                italianCount++;
                if (italianCount >= 2) {
                    Log.d(TAG, "Detected Italian words: " + italianCount);
                    return "it";
                }
            }
        }
        
        // Đếm số lượng từ tiếng Bồ Đào Nha
        for (String word : portugueseWords) {
            if (containsWord(normalizedText, word)) {
                portugueseCount++;
                if (portugueseCount >= 2) {
                    Log.d(TAG, "Detected Portuguese words: " + portugueseCount);
                    return "pt";
                }
            }
        }
        
        // Đếm số lượng từ tiếng Nga
        for (String word : russianWords) {
            if (normalizedText.contains(word)) {
                russianCount++;
                if (russianCount >= 2) {
                    Log.d(TAG, "Detected Russian words: " + russianCount);
                    return "ru";
                }
            }
        }
        
        // Kiểm tra các ký tự tiếng Ả Rập
        int arabicCount = countMatches(normalizedText, arabicChars);
        if (arabicCount >= 3) {
            Log.d(TAG, "Detected Arabic characters: " + arabicCount);
            return "ar";
        }
        
        // Kiểm tra các ký tự tiếng Hindi
        int hindiCount = countMatches(normalizedText, hindiChars);
        if (hindiCount >= 3) {
            Log.d(TAG, "Detected Hindi characters: " + hindiCount);
            return "hi";
        }
        
        // Mặc định là tiếng Anh nếu không phát hiện được ngôn ngữ khác
        return "en";
    }
    
    /**
     * Kiểm tra nếu văn bản chứa từ cụ thể (tách biệt bởi khoảng trắng hoặc dấu câu)
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
     * Kiểm tra nếu văn bản chứa các từ tiếng Việt thông dụng
     * @param text Văn bản cần kiểm tra
     * @return true nếu chứa từ tiếng Việt thông dụng
     */
    private boolean containsVietnameseWords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Chuyển văn bản về chữ thường và loại bỏ dấu cách để dễ kiểm tra
        String normalizedText = text.toLowerCase().trim();
        
        // Danh sách các từ tiếng Việt thông dụng có dấu
        String[] vietnameseWords = {
            "có", "không", "và", "là", "của", "cho", "trong", "với", "bị", "bởi", 
            "từ", "đến", "còn", "đã", "sẽ", "rằng", "nhưng", "nên", "cần", "phải", 
            "trên", "dưới", "tôi", "anh", "chị", "em", "bạn", "chúng", "họ", "người", 
            "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười", 
            "trăm", "nghìn", "triệu", "tỷ", "thì", "mà", "hoặc", "hay", "cũng", "vậy",
            "này", "đó", "ấy", "kia", "thế", "về", "đi", "lên", "xuống", "ra"
        };
        
        // Danh sách các từ tiếng Việt thông dụng không dấu
        String[] vietnameseWordsNoDiacritics = {
            "co", "khong", "va", "la", "cua", "cho", "trong", "voi", "bi", "boi", 
            "tu", "den", "con", "da", "se", "rang", "nhung", "nen", "can", "phai", 
            "tren", "duoi", "toi", "anh", "chi", "em", "ban", "chung", "ho", "nguoi", 
            "mot", "hai", "ba", "bon", "nam", "sau", "bay", "tam", "chin", "muoi", 
            "tram", "nghin", "trieu", "ty", "thi", "ma", "hoac", "hay", "cung", "vay",
            "nay", "do", "ay", "kia", "the", "ve", "di", "len", "xuong", "ra",
            "vao", "lai", "noi", "lam", "an", "uong", "ngu", "choi", "hoc", "biet",
            "thich", "yeu", "ghet", "muon", "cam", "thay", "nhin", "nghe", "noi", "goi",
            "chao", "tam", "biet", "xin", "cam", "on", "vui", "buon", "gian", "so"
        };
        
        // Các cụm từ tiếng Việt đặc trưng
        String[] vietnamesePhrases = {
            "cam on", "xin chao", "tam biet", "rat vui", "khong co", "co the", "da co", 
            "se co", "rat tot", "rat hay", "rat dep", "rat ngon", "rat vui", "rat buon", 
            "rat kho", "rat de", "rat nhieu", "rat it", "rat lon", "rat nho",
            "nguoi viet", "tieng viet", "viet nam", "nguoi ta", "nha toi", "cho toi",
            "di hoc", "di lam", "di choi", "di ngu", "di an", "di uong", "di ve",
            "lam on", "lam gi", "lam sao", "lam the nao", "lam viec", "lam bai",
            "noi chuyen", "noi gi", "noi sao", "noi the", "noi voi", "noi vay",
            "biet roi", "biet chua", "biet gi", "biet sao", "biet the", "biet vay"
        };
        
        // Kiểm tra từ tiếng Việt có dấu
        int vietnameseWordCount = 0;
        for (String word : vietnameseWords) {
            if (normalizedText.contains(word)) {
                vietnameseWordCount++;
                Log.d(TAG, "Found Vietnamese word with diacritics: " + word);
                if (vietnameseWordCount >= 1) {
                    return true;
                }
            }
        }
        
        // Kiểm tra từ tiếng Việt không dấu
        int vietnameseWordNoDiacriticsCount = 0;
        for (String word : vietnameseWordsNoDiacritics) {
            if (containsWord(normalizedText, word)) {
                vietnameseWordNoDiacriticsCount++;
                Log.d(TAG, "Found Vietnamese word without diacritics: " + word);
                if (vietnameseWordNoDiacriticsCount >= 2) {
                    return true;
                }
            }
        }
        
        // Kiểm tra cụm từ tiếng Việt đặc trưng
        for (String phrase : vietnamesePhrases) {
            if (normalizedText.contains(phrase)) {
                Log.d(TAG, "Found Vietnamese phrase: " + phrase);
                return true;
            }
        }
        
        // Kiểm tra các từ có dấu tiếng Việt
        String vietnameseChars = "[\u00e0\u00e1\u1ea1\u1ea3\u00e3\u00e2\u1ea7\u1ea5\u1ead\u1ea9\u1eab\u0103\u1eb1\u1eaf\u1eb7\u1eb3\u1eb5\u00e8\u00e9\u1eb9\u1ebb\u1ebd\u00ea\u1ec1\u1ebf\u1ec7\u1ec3\u1ec5\u00ec\u00ed\u1ecb\u1ec9\u0129\u00f2\u00f3\u1ecd\u1ecf\u00f5\u00f4\u1ed3\u1ed1\u1ed9\u1ed5\u1ed7\u01a1\u1edd\u1edb\u1ee3\u1edf\u1ee1\u00f9\u00fa\u1ee5\u1ee7\u0169\u01b0\u1ee9\u1ee9\u1ef1\u1eef\u1eef\u1ef3\u00fd\u1ef5\u1ef7\u1ef9\u0111]";
        int vietnameseCharCount = countMatches(normalizedText, vietnameseChars);
        if (vietnameseCharCount >= 2) {
            Log.d(TAG, "Detected Vietnamese characters: " + vietnameseCharCount);
            return true;
        }
        
        return false;
    }

    /**
     * Translate text from source language to target language
     * @param text Text to translate
     * @param sourceLanguageCode Source language code
     * @param targetLanguageCode Target language code
     * @return LiveData<TranslationResult>
     */
    public LiveData<TranslationResult> translateText(String text, String sourceLanguageCode, String targetLanguageCode) {
        // Create MutableLiveData for translation result
        MutableLiveData<TranslationResult> translationResult = new MutableLiveData<>();
        
        // Check if text is empty
        if (text == null || text.isEmpty()) {
            TranslationResult result = new TranslationResult(false, "", "Text is empty", text);
            translationResult.setValue(result);
            return translationResult;
        }
        
        // Kiểm tra xem mã ngôn ngữ có hợp lệ không
        if (sourceLanguageCode == null || sourceLanguageCode.isEmpty() ||
            targetLanguageCode == null || targetLanguageCode.isEmpty()) {
            TranslationResult result = new TranslationResult(false, "", "Invalid language code", text);
            translationResult.setValue(result);
            return translationResult;
        }
        
        // Kiểm tra xem ngôn ngữ nguồn và đích có giống nhau không
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            // Nếu giống nhau, trả về văn bản gốc mà không cần dịch
            TranslationResult result = new TranslationResult(true, text, "", text);
            translationResult.setValue(result);
            return translationResult;
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
                                translationResult.setValue(result);
                                isLoading.setValue(false);
                            })
                            .addOnFailureListener(e -> {
                                // Translation failed
                                Log.e(TAG, "Translation error: " + e.getMessage());
                                TranslationResult result = new TranslationResult(false, "", "Translation failed: " + e.getMessage(), text);
                                translationResult.setValue(result);
                                isLoading.setValue(false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Model download failed: " + e.getMessage());
                    TranslationResult result = new TranslationResult(false, "", "Failed to download language model: " + e.getMessage(), text);
                    translationResult.setValue(result);
                    isLoading.setValue(false);
                });
                
        return translationResult;
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

package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lớp tiện ích để nhận diện ngôn ngữ chính xác hơn
 */
public class LanguageIdentificationUtils {
    private static final String TAG = "LanguageIdUtils";
    
    // Singleton instance
    private static LanguageIdentificationUtils instance;
    
    // Danh sách mã ngôn ngữ được hỗ trợ
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList(
            "en", "ar", "bg", "bn", "ca", "cs", "da", "de", "el", "es", "et", 
            "fa", "fi", "fil", "fr", "gu", "he", "hi", "hr", "hu", "id", "it", 
            "ja", "kn", "ko", "lt", "lv", "ml", "mr", "ms", "nl", "no", "pl", 
            "pt", "ro", "ru", "sk", "sl", "sr", "sv", "sw", "ta", "te", "th", 
            "tr", "uk", "ur", "vi", "zh"
    );
    
    // Danh sách từ điển cho các ngôn ngữ phổ biến
    private Map<String, List<String>> languageDictionaries;
    
    // Danh sách mẫu ký tự đặc trưng cho các ngôn ngữ
    private Map<String, String> characterPatterns;
    
    // Danh sách từ đặc trưng cho tiếng Pháp
    private List<String> frenchSpecificWords;
    
    /**
     * Khởi tạo LanguageIdentificationUtils
     * @param context Context
     */
    private LanguageIdentificationUtils(Context context) {
        // Khởi tạo từ điển và mẫu ký tự
        initLanguageDictionaries();
        initCharacterPatterns();
        initFrenchSpecificWords();
        
        Log.d(TAG, "LanguageIdentificationUtils initialized successfully");
    }
    
    /**
     * Lấy instance của LanguageIdentificationUtils
     * @param context Context
     * @return LanguageIdentificationUtils instance
     */
    public static synchronized LanguageIdentificationUtils getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageIdentificationUtils(context);
        }
        return instance;
    }
    
    /**
     * Khởi tạo từ điển cho các ngôn ngữ phổ biến
     */
    private void initLanguageDictionaries() {
        languageDictionaries = new HashMap<>();
        
        // Tiếng Pháp (fr)
        List<String> frenchWords = Arrays.asList(
                "le", "la", "les", "un", "une", "des", "et", "ou", "mais", "donc", "car", "ni", "que", 
                "qui", "quoi", "où", "comment", "pourquoi", "quand", "je", "tu", "il", "elle", "nous", 
                "vous", "ils", "elles", "mon", "ton", "son", "notre", "votre", "leur", "ce", "cette", 
                "ces", "est", "sont", "était", "seront", "avoir", "faire", "dire", "aller", "voir", 
                "venir", "vouloir", "pouvoir", "falloir", "devoir"
        );
        languageDictionaries.put("fr", frenchWords);
        
        // Tiếng Tây Ban Nha (es)
        List<String> spanishWords = Arrays.asList(
                "el", "la", "los", "las", "un", "una", "unos", "unas", "y", "o", "pero", "porque", 
                "como", "cuando", "donde", "quien", "que", "yo", "tu", "el", "ella", "nosotros", 
                "vosotros", "ellos", "ellas", "mi", "tu", "su", "nuestro", "vuestro", "su", "este", 
                "esta", "estos", "estas", "es", "son", "era", "serán", "tener", "hacer", "decir", 
                "ir", "ver", "venir", "querer", "poder", "deber"
        );
        languageDictionaries.put("es", spanishWords);
        
        // Tiếng Đức (de)
        List<String> germanWords = Arrays.asList(
                "der", "die", "das", "ein", "eine", "und", "oder", "aber", "weil", "wie", "wenn", 
                "wo", "wer", "was", "ich", "du", "er", "sie", "es", "wir", "ihr", "sie", "mein", 
                "dein", "sein", "unser", "euer", "ihr", "dieser", "diese", "dieses", "ist", "sind", 
                "war", "werden", "haben", "machen", "sagen", "gehen", "sehen", "kommen", "wollen", 
                "können", "müssen", "sollen"
        );
        languageDictionaries.put("de", germanWords);
        
        // Tiếng Ý (it)
        List<String> italianWords = Arrays.asList(
                "il", "lo", "la", "i", "gli", "le", "un", "uno", "una", "e", "o", "ma", "perché", 
                "come", "quando", "dove", "chi", "che", "cosa", "io", "tu", "lui", "lei", "noi", 
                "voi", "loro", "mio", "tuo", "suo", "nostro", "vostro", "loro", "questo", "questa", 
                "è", "sono", "era", "saranno", "avere", "fare", "dire", "andare", "vedere", 
                "venire", "volere", "potere", "dovere"
        );
        languageDictionaries.put("it", italianWords);
        
        // Tiếng Bồ Đào Nha (pt)
        List<String> portugueseWords = Arrays.asList(
                "o", "a", "os", "as", "um", "uma", "uns", "umas", "e", "ou", "mas", "porque", 
                "como", "quando", "onde", "quem", "que", "eu", "tu", "ele", "ela", "nós", 
                "vós", "eles", "elas", "meu", "teu", "seu", "nosso", "vosso", "seu", "este", 
                "esta", "estes", "estas", "é", "são", "era", "serão", "ter", "fazer", "dizer", 
                "ir", "ver", "vir", "querer", "poder", "dever"
        );
        languageDictionaries.put("pt", portugueseWords);
        
        // Tiếng Nga (ru)
        List<String> russianWords = Arrays.asList(
                "и", "в", "не", "что", "он", "на", "я", "с", "со", "как", 
                "а", "то", "все", "она", "так", "его", "но", "да", "ты", "к", 
                "у", "же", "вы", "за", "бы", "по", "только", "ее", "мне", "был", 
                "чтобы", "было", "вот", "от", "меня", "еще", "нет", "о", "из", "ему"
        );
        languageDictionaries.put("ru", russianWords);
        
        // Tiếng Hàn (ko)
        List<String> koreanWords = Arrays.asList(
                "나", "너", "우리", "그", "그녀", "그들", "이", "저", "그것", "이것", 
                "저것", "무엇", "누구", "어디", "언제", "왜", "어떻게", "하다", "되다", "있다", 
                "없다", "가다", "오다", "보다", "먹다", "마시다", "자다", "일어나다", "앉다", "서다"
        );
        languageDictionaries.put("ko", koreanWords);
        
        // Tiếng Nhật (ja)
        List<String> japaneseWords = Arrays.asList(
                "私", "あなた", "彼", "彼女", "私たち", "あなたたち", "彼ら", "これ", "それ", "あれ", 
                "何", "誰", "どこ", "いつ", "なぜ", "どのように", "する", "ある", "いる", "行く", 
                "来る", "見る", "食べる", "飲む", "寝る", "起きる", "座る", "立つ", "話す", "聞く"
        );
        languageDictionaries.put("ja", japaneseWords);
        
        // Tiếng Trung (zh)
        List<String> chineseWords = Arrays.asList(
                "我", "你", "他", "她", "我们", "你们", "他们", "这", "那", "什么", 
                "谁", "哪里", "什么时候", "为什么", "怎么样", "是", "有", "做", "去", "来", 
                "看", "吃", "喝", "睡觉", "起床", "坐", "站", "说话", "听", "理解"
        );
        languageDictionaries.put("zh", chineseWords);
        
        // Tiếng Việt (vi)
        List<String> vietnameseWords = Arrays.asList(
                "tôi", "bạn", "anh", "chị", "nó", "chúng tôi", "các bạn", "họ", "này", "đó", 
                "kia", "gì", "ai", "đâu", "khi nào", "tại sao", "làm sao", "là", "có", "làm", 
                "đi", "đến", "xem", "ăn", "uống", "ngủ", "thức dậy", "ngồi", "đứng", "nói", 
                "nghe", "hiểu", "không", "và", "nhưng", "hoặc", "nếu", "vì", "của", "trong"
        );
        languageDictionaries.put("vi", vietnameseWords);
    }
    
    /**
     * Khởi tạo mẫu ký tự đặc trưng cho các ngôn ngữ
     */
    private void initCharacterPatterns() {
        characterPatterns = new HashMap<>();
        
        // Tiếng Việt
        characterPatterns.put("vi", "[\\u00e0\\u00e1\\u1ea1\\u1ea3\\u00e3\\u00e2\\u1ea7\\u1ea5\\u1ead\\u1ea9\\u1eab\\u0103\\u1eb1\\u1eaf\\u1eb7\\u1eb3\\u1eb5\\u00e8\\u00e9\\u1eb9\\u1ebb\\u1ebd\\u00ea\\u1ec1\\u1ebf\\u1ec7\\u1ec3\\u1ec5\\u00ec\\u00ed\\u1ecb\\u1ec9\\u0129\\u00f2\\u00f3\\u1ecd\\u1ecf\\u00f5\\u00f4\\u1ed3\\u1ed1\\u1ed9\\u1ed5\\u1ed7\\u01a1\\u1edd\\u1edb\\u1ee3\\u1edf\\u1ee1\\u00f9\\u00fa\\u1ee5\\u1ee7\\u0169\\u01b0\\u1ee9\\u1ee9\\u1ef1\\u1eef\\u1eef\\u1ef3\\u00fd\\u1ef5\\u1ef7\\u1ef9\\u0111]");
        
        // Tiếng Trung
        characterPatterns.put("zh", "[\\u4E00-\\u9FFF]");
        
        // Tiếng Nhật
        characterPatterns.put("ja", "[\\u3040-\\u309F\\u30A0-\\u30FF]");
        
        // Tiếng Hàn
        characterPatterns.put("ko", "[\\uAC00-\\uD7AF\\u1100-\\u11FF]");
        
        // Tiếng Thái
        characterPatterns.put("th", "[\\u0E00-\\u0E7F]");
        
        // Tiếng Ả Rập
        characterPatterns.put("ar", "[\\u0600-\\u06FF]");
        
        // Tiếng Hindi
        characterPatterns.put("hi", "[\\u0900-\\u097F]");
        
        // Tiếng Nga và các ngôn ngữ sử dụng bảng chữ cái Cyrillic
        characterPatterns.put("ru", "[\\u0400-\\u04FF]");
        
        // Tiếng Hy Lạp
        characterPatterns.put("el", "[\\u0370-\\u03FF]");
        
        // Tiếng Pháp (các ký tự đặc trưng)
        characterPatterns.put("fr", "[\\u00e0\\u00e2\\u00e4\\u00e6\\u00e7\\u00e8\\u00e9\\u00ea\\u00eb\\u00ee\\u00ef\\u00f4\\u0153\\u00f9\\u00fb\\u00fc\\u00ff]");
    }
    
    /**
     * Khởi tạo danh sách từ đặc trưng cho tiếng Pháp
     */
    private void initFrenchSpecificWords() {
        frenchSpecificWords = Arrays.asList(
            "il est", "elle est", "ils sont", "elles sont", "je suis", "tu es", "nous sommes", "vous êtes",
            "français", "anglais", "allemand", "espagnol", "italien", "chinois", "japonais",
            "américain", "canadien", "belge", "suisse", "mexicain", "norvégien", "danois",
            "européen", "grec", "colombien", "malien"
        );
    }
    
    /**
     * Nhận diện ngôn ngữ từ văn bản
     * @param text Văn bản cần nhận diện
     * @param listener Listener để nhận kết quả
     */
    public void identifyLanguage(String text, LanguageIdentificationListener listener) {
        if (text == null || text.isEmpty()) {
            if (listener != null) {
                listener.onLanguageIdentified("", 0);
            }
            return;
        }
        
        // Thực hiện nhận diện theo nhiều phương pháp
        multiMethodLanguageIdentification(text, listener);
    }
    
    /**
     * Nhận diện ngôn ngữ bằng nhiều phương pháp kết hợp
     * @param text Văn bản cần nhận diện
     * @param listener Listener để nhận kết quả
     */
    private void multiMethodLanguageIdentification(String text, LanguageIdentificationListener listener) {
        // 0. Kiểm tra đặc biệt cho tiếng Pháp
        if (isFrenchText(text)) {
            Log.d(TAG, "Detected French text by specific patterns");
            if (listener != null) {
                listener.onLanguageIdentified("fr", 0.95f);
            }
            return;
        }
        
        // 1. Kiểm tra mẫu ký tự đặc trưng
        String patternBasedLanguage = detectLanguageFromCharacterPattern(text);
        if (!patternBasedLanguage.isEmpty() && !patternBasedLanguage.equals("en")) {
            Log.d(TAG, "Detected language by character pattern: " + patternBasedLanguage);
            if (listener != null) {
                listener.onLanguageIdentified(patternBasedLanguage, 0.9f);
            }
            return;
        }
        
        // 2. Kiểm tra từ điển
        String dictionaryBasedLanguage = detectLanguageFromDictionary(text);
        if (!dictionaryBasedLanguage.isEmpty() && !dictionaryBasedLanguage.equals("en")) {
            Log.d(TAG, "Detected language by dictionary: " + dictionaryBasedLanguage);
            if (listener != null) {
                listener.onLanguageIdentified(dictionaryBasedLanguage, 0.8f);
            }
            return;
        }
        
        // 3. Nếu không phát hiện được ngôn ngữ, mặc định là tiếng Anh
        Log.d(TAG, "Could not identify language, defaulting to English");
        if (listener != null) {
            listener.onLanguageIdentified("en", 0.5f);
        }
    }
    
    /**
     * Kiểm tra xem văn bản có phải là tiếng Pháp không dựa trên các mẫu đặc trưng
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản có vẻ là tiếng Pháp
     */
    private boolean isFrenchText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Chuẩn hóa văn bản
        String normalizedText = text.toLowerCase().trim();
        
        // Kiểm tra các từ đặc trưng của tiếng Pháp
        for (String word : frenchSpecificWords) {
            if (normalizedText.contains(word)) {
                Log.d(TAG, "Found French specific word: " + word);
                return true;
            }
        }
        
        // Kiểm tra các mẫu đặc trưng của tiếng Pháp
        if (normalizedText.contains("il est") || 
            normalizedText.contains("elle est") || 
            normalizedText.contains("c'est") || 
            normalizedText.contains("je suis") ||
            normalizedText.contains("allemand") ||
            normalizedText.contains("anglais") ||
            normalizedText.contains("français") ||
            normalizedText.contains("espagnol") ||
            normalizedText.contains("italien") ||
            normalizedText.contains("danois") ||
            normalizedText.contains("grec") ||
            normalizedText.contains("japonais")) {
            Log.d(TAG, "Found French pattern in text");
            return true;
        }
        
        // Đếm số lượng ký tự đặc trưng của tiếng Pháp
        int frenchAccentCount = countMatches(normalizedText, characterPatterns.get("fr"));
        if (frenchAccentCount >= 2) {
            Log.d(TAG, "Detected French by accent count: " + frenchAccentCount);
            return true;
        }
        
        return false;
    }
    
    /**
     * Phát hiện ngôn ngữ dựa trên mẫu ký tự đặc trưng
     * @param text Văn bản cần phát hiện
     * @return Mã ngôn ngữ hoặc chuỗi rỗng nếu không phát hiện được
     */
    private String detectLanguageFromCharacterPattern(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        Map<String, Integer> languageCounts = new HashMap<>();
        
        // Kiểm tra mỗi mẫu ký tự
        for (Map.Entry<String, String> entry : characterPatterns.entrySet()) {
            String languageCode = entry.getKey();
            String pattern = entry.getValue();
            
            int count = countMatches(text, pattern);
            if (count > 0) {
                languageCounts.put(languageCode, count);
            }
        }
        
        // Tìm ngôn ngữ có số lượng ký tự đặc trưng nhiều nhất
        String detectedLanguage = "";
        int maxCount = 0;
        
        for (Map.Entry<String, Integer> entry : languageCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                detectedLanguage = entry.getKey();
            }
        }
        
        // Chỉ trả về ngôn ngữ nếu có ít nhất 2 ký tự đặc trưng
        return (maxCount >= 2) ? detectedLanguage : "";
    }
    
    /**
     * Phát hiện ngôn ngữ dựa trên từ điển
     * @param text Văn bản cần phát hiện
     * @return Mã ngôn ngữ hoặc chuỗi rỗng nếu không phát hiện được
     */
    private String detectLanguageFromDictionary(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Chuẩn hóa văn bản
        String normalizedText = text.toLowerCase().trim();
        
        // Đếm số từ khớp với từ điển của mỗi ngôn ngữ
        Map<String, Integer> languageWordCounts = new HashMap<>();
        
        for (Map.Entry<String, List<String>> entry : languageDictionaries.entrySet()) {
            String languageCode = entry.getKey();
            List<String> dictionary = entry.getValue();
            
            int wordCount = 0;
            for (String word : dictionary) {
                if (containsWord(normalizedText, word)) {
                    wordCount++;
                }
            }
            
            if (wordCount > 0) {
                languageWordCounts.put(languageCode, wordCount);
            }
        }
        
        // Tìm ngôn ngữ có số từ khớp nhiều nhất
        String detectedLanguage = "";
        int maxWordCount = 0;
        
        for (Map.Entry<String, Integer> entry : languageWordCounts.entrySet()) {
            if (entry.getValue() > maxWordCount) {
                maxWordCount = entry.getValue();
                detectedLanguage = entry.getKey();
            }
        }
        
        // Chỉ trả về ngôn ngữ nếu có ít nhất 2 từ khớp
        return (maxWordCount >= 2) ? detectedLanguage : "";
    }
    
    /**
     * Đếm số lần xuất hiện của mẫu trong văn bản
     * @param text Văn bản
     * @param pattern Mẫu regex
     * @return Số lần xuất hiện
     */
    private int countMatches(String text, String pattern) {
        if (text == null || pattern == null) {
            return 0;
        }
        
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(text);
        
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        
        return count;
    }
    
    /**
     * Kiểm tra xem văn bản có chứa từ cụ thể không
     * @param text Văn bản
     * @param word Từ cần kiểm tra
     * @return true nếu văn bản chứa từ
     */
    private boolean containsWord(String text, String word) {
        if (text == null || word == null) {
            return false;
        }
        
        // Tạo mẫu regex để tìm từ hoàn chỉnh
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern).matcher(text).find();
    }
    
    /**
     * Kiểm tra xem ngôn ngữ có được hỗ trợ bởi hệ thống không
     * @param languageCode Mã ngôn ngữ
     * @return true nếu ngôn ngữ được hỗ trợ
     */
    public boolean isLanguageSupported(String languageCode) {
        return SUPPORTED_LANGUAGES.contains(languageCode);
    }
    
    /**
     * Giải phóng tài nguyên
     */
    public void shutdown() {
        instance = null;
    }
    
    /**
     * Interface để lắng nghe kết quả nhận diện ngôn ngữ
     */
    public interface LanguageIdentificationListener {
        /**
         * Được gọi khi ngôn ngữ được nhận diện
         * @param languageCode Mã ngôn ngữ
         * @param confidence Độ tin cậy (0-1)
         */
        void onLanguageIdentified(String languageCode, float confidence);
    }
}

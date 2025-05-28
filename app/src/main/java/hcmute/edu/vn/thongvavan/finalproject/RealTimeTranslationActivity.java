package hcmute.edu.vn.thongvavan.finalproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.ViewGroup;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.mlkit.nl.translate.TranslateLanguage;

import hcmute.edu.vn.thongvavan.finalproject.analyzer.TextAnalyzer;
import hcmute.edu.vn.thongvavan.finalproject.model.ModelLanguage;
import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.repository.TranslationRepository;
import hcmute.edu.vn.thongvavan.finalproject.utils.LanguageUIUtils;

@androidx.camera.core.ExperimentalGetImage
public class RealTimeTranslationActivity extends AppCompatActivity {
    private static final String TAG = "RealTimeTranslation";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    // UI Components
    private PreviewView previewView;
    private CardView translationOverlay;
    private TextView detectedLanguageTextView;
    private TextView originalTextView;
    private TextView translatedTextView;
    private ImageButton closeButton;
    private ImageButton flashButton;
    private ImageButton swapLanguageButton;
    private Spinner sourceLanguageSpinner;
    private Spinner targetLanguageSpinner;
    private View loadingOverlay;

    // Camera variables
    private Camera camera;
    private ExecutorService cameraExecutor;
    private boolean flashEnabled = false;

    // Translation variables
    private TranslationRepository translationRepository;
    private List<ModelLanguage> availableLanguages = new ArrayList<>();
    private ModelLanguage selectedSourceLanguage;
    private ModelLanguage selectedTargetLanguage;
    private String lastDetectedText = "";
    private String lastTranslatedText = "";
    private boolean autoDetectSource = true;
    private boolean isTranslationObserverSet = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realtime_translation);
        
        // Initialize UI components
        initializeViews();
        
        // Giới hạn kích thước của overlay
        limitOverlaySize();
        
        // Initialize translation repository
        translationRepository = new TranslationRepository(getApplication());
        
        // Set up language spinners
        setupLanguageSpinners();
        
        // Set up click listeners
        setupClickListeners();

        // Check camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        translationOverlay = findViewById(R.id.translationOverlay);
        detectedLanguageTextView = findViewById(R.id.detectedLanguageTextView);
        originalTextView = findViewById(R.id.originalTextView);
        translatedTextView = findViewById(R.id.translatedTextView);
        closeButton = findViewById(R.id.closeButton);
        flashButton = findViewById(R.id.flashButton);
        swapLanguageButton = findViewById(R.id.swapLanguageButton);
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        
        // Initially hide translation overlay until text is detected
        translationOverlay.setVisibility(View.GONE);
    }

    private void setupLanguageSpinners() {
        // Get available languages from TranslateLanguage
        availableLanguages = new ArrayList<>();
        
        // TranslateLanguage.getAllLanguages() trả về Set<String>
        List<String> languageCodeList = new ArrayList<>();
        for (String code : TranslateLanguage.getAllLanguages()) {
            languageCodeList.add(code);
        }
        
        // Chuyển đổi mã ngôn ngữ thành đối tượng ModelLanguage với tên hiển thị
        for (String languageCode : languageCodeList) {
            Locale locale = new Locale(languageCode);
            String displayName = locale.getDisplayLanguage(new Locale("vi"));
            
            // Nếu tên hiển thị trùng với mã ngôn ngữ, thử lấy bằng tiếng Anh
            if (displayName.equals(languageCode)) {
                displayName = locale.getDisplayLanguage(Locale.ENGLISH);
            }
            
            // Viết hoa chữ cái đầu của tên ngôn ngữ
            if (!displayName.isEmpty()) {
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            } else {
                displayName = languageCode; // Sử dụng mã ngôn ngữ nếu không lấy được tên
            }
            
            availableLanguages.add(new ModelLanguage(languageCode, displayName));
        }
        
        // Create adapter for source language spinner
        List<String> sourceLanguageNames = new ArrayList<>();
        sourceLanguageNames.add("Tự động nhận diện");
        for (ModelLanguage language : availableLanguages) {
            sourceLanguageNames.add(language.getLanguageTitle());
        }
        
        // Tạo adapter với layout tùy chỉnh để có chữ màu trắng
        ArrayAdapter<String> sourceAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_item, sourceLanguageNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK); // Màu đen cho dropdown
                return view;
            }
        };
        sourceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sourceLanguageSpinner.setAdapter(sourceAdapter);
        
        // Create adapter for target language spinner
        List<String> targetLanguageNames = new ArrayList<>();
        for (ModelLanguage language : availableLanguages) {
            targetLanguageNames.add(language.getLanguageTitle());
        }
        
        // Tạo adapter với layout tùy chỉnh để có chữ màu trắng
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<String>(this, 
                android.R.layout.simple_spinner_item, targetLanguageNames) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.WHITE);
                return view;
            }
            
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(Color.BLACK); // Màu đen cho dropdown
                return view;
            }
        };
        targetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        targetLanguageSpinner.setAdapter(targetAdapter);
        
        // Set default target language to English or first available
        int defaultTargetIndex = 0;
        for (int i = 0; i < availableLanguages.size(); i++) {
            if (availableLanguages.get(i).getLanguageCode().equals("en")) {
                defaultTargetIndex = i;
                break;
            }
        }
        targetLanguageSpinner.setSelection(defaultTargetIndex);
        selectedTargetLanguage = availableLanguages.get(defaultTargetIndex);
        
        // Set up source spinner listener
        sourceLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // Auto-detect selected
                    autoDetectSource = true;
                    selectedSourceLanguage = null;
                } else {
                    autoDetectSource = false;
                    selectedSourceLanguage = availableLanguages.get(position - 1);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                autoDetectSource = true;
                selectedSourceLanguage = null;
            }
        });
        
        // Set up target spinner listener
        targetLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTargetLanguage = availableLanguages.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep current selection
            }
        });
    }

    private void setupClickListeners() {
        // Close button
        closeButton.setOnClickListener(v -> finish());
        
        // Flash button
        flashButton.setOnClickListener(v -> toggleFlash());
        
        // Swap language button
        swapLanguageButton.setOnClickListener(v -> swapLanguages());
    }
    
    /**
     * Giới hạn kích thước của overlay để không tràn màn hình
     */
    private void limitOverlaySize() {
        // Lấy kích thước màn hình
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        
        // Giới hạn chiều cao của overlay không quá 1/3 màn hình
        int maxHeight = screenHeight / 3;
        
        // Áp dụng giới hạn kích thước
        ViewGroup.LayoutParams params = translationOverlay.getLayoutParams();
        params.height = maxHeight;
        translationOverlay.setLayoutParams(params);
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            flashEnabled = !flashEnabled;
            camera.getCameraControl().enableTorch(flashEnabled);
            flashButton.setImageResource(flashEnabled ? 
                    R.drawable.ic_flash_on_24 : R.drawable.ic_flash_off_24);
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ đèn flash", Toast.LENGTH_SHORT).show();
        }
    }

    private void swapLanguages() {
        if (autoDetectSource || selectedSourceLanguage == null) {
            Toast.makeText(this, "Không thể hoán đổi khi nguồn là tự động", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Get current positions
        int sourcePos = -1;
        int targetPos = -1;
        
        for (int i = 0; i < availableLanguages.size(); i++) {
            if (availableLanguages.get(i).getLanguageCode().equals(selectedSourceLanguage.getLanguageCode())) {
                sourcePos = i;
            }
            if (availableLanguages.get(i).getLanguageCode().equals(selectedTargetLanguage.getLanguageCode())) {
                targetPos = i;
            }
        }
        
        if (sourcePos != -1 && targetPos != -1) {
            // Swap selections
            sourceLanguageSpinner.setSelection(sourcePos + 1); // +1 because of "Auto" option
            targetLanguageSpinner.setSelection(sourcePos);
            
            // Update selected languages
            ModelLanguage temp = selectedSourceLanguage;
            selectedSourceLanguage = selectedTargetLanguage;
            selectedTargetLanguage = temp;
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Set up the preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Set up the image analyzer use case
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                // Set up text analyzer
                TextAnalyzer textAnalyzer = new TextAnalyzer(this, 
                        new TextAnalyzer.TextAnalyzerCallback() {
                    @Override
                    @androidx.camera.core.ExperimentalGetImage
                    public void onTextDetected(RecognizedText recognizedText) {
                        if (recognizedText != null && recognizedText.isSuccess()) {
                            String detectedText = recognizedText.getText();
                            if (!detectedText.isEmpty() && !detectedText.equals(lastDetectedText)) {
                                lastDetectedText = detectedText;
                                processDetectedText(detectedText);
                            }
                        }
                    }
                });
                
                imageAnalysis.setAnalyzer(cameraExecutor, textAnalyzer);

                // Select back camera as default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                        ((LifecycleOwner) this), 
                        cameraSelector, 
                        preview, 
                        imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processDetectedText(String text) {
        // Khi không có văn bản, ẩn overlay
        if (text.isEmpty()) {
            runOnUiThread(() -> {
                if (translationOverlay.getVisibility() == View.VISIBLE) {
                    translationOverlay.setVisibility(View.GONE);
                    originalTextView.setText("Văn bản nhận diện sẽ hiện ở đây");
                    translatedTextView.setText("Văn bản đã dịch sẽ hiện ở đây");
                    detectedLanguageTextView.setText("");
                }
            });
            return;
        }
        
        // Giới hạn độ dài văn bản để tránh tràn màn hình nhưng vẫn hiển thị đủ thông tin
        final int MAX_TEXT_LENGTH = 500; // Tăng giới hạn độ dài văn bản
        final String displayText = text.length() > MAX_TEXT_LENGTH ? 
                text.substring(0, MAX_TEXT_LENGTH) + "..." : text;
        
        runOnUiThread(() -> {
            
            // Hiển thị overlay nếu chưa hiển thị
            if (translationOverlay.getVisibility() != View.VISIBLE) {
                translationOverlay.setVisibility(View.VISIBLE);
            }
            
            // Cập nhật văn bản gốc đã được giới hạn độ dài
            originalTextView.setText(displayText);
            
            // Show loading
            loadingOverlay.setVisibility(View.VISIBLE);
            
            // Nếu tự động nhận diện ngôn ngữ nguồn
            if (autoDetectSource) {
                // Kiểm tra ngôn ngữ không phải Latin (tiếng Trung, Nhật, Hàn, v.v.)
                String nonLatinLanguage = detectNonLatinLanguage(text);
                if (!nonLatinLanguage.isEmpty()) {
                    Log.d(TAG, "Phát hiện ngôn ngữ không phải Latin: " + nonLatinLanguage);
                    String languageCode = nonLatinLanguage;
                    
                    // Lấy tên ngôn ngữ từ mã ngôn ngữ
                    Locale locale = new Locale(languageCode);
                    String languageName = locale.getDisplayLanguage(new Locale("vi"));
                    if (languageName.equals(languageCode)) {
                        languageName = locale.getDisplayLanguage(Locale.ENGLISH);
                    }
                    languageName = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
                    
                    // Hiển thị ngôn ngữ được nhận diện
                    detectedLanguageTextView.setText("Nhận diện: " + languageName);
                    
                    // Cập nhật spinner ngôn ngữ nguồn
                    updateSourceLanguageSpinner(languageCode);
                    
                    // Dịch văn bản
                    translateText(text, languageCode, selectedTargetLanguage.getLanguageCode());
                }
                // Nhận diện trực tiếp tiếng Việt dựa trên các ký tự đặc trưng
                else if (containsVietnameseCharacters(text)) {
                    Log.d(TAG, "Phát hiện tiếng Việt dựa trên ký tự đặc trưng");
                    String languageCode = "vi";
                    String languageName = "Tiếng Việt";
                    
                    // Hiển thị ngôn ngữ được nhận diện
                    detectedLanguageTextView.setText("Nhận diện: " + languageName);
                    
                    // Cập nhật spinner ngôn ngữ nguồn
                    updateSourceLanguageSpinner(languageCode);
                    
                    // Dịch văn bản
                    translateText(text, languageCode, selectedTargetLanguage.getLanguageCode());
                }
                // Nếu không phải tiếng Việt hoặc ngôn ngữ không phải Latin, sử dụng ML Kit để nhận diện
                else {
                    translationRepository.identifyLanguage(text);
                    translationRepository.getDetectedLanguage().observe(this, languageCode -> {
                        if (languageCode != null && !languageCode.isEmpty()) {
                            // Cập nhật hiển thị ngôn ngữ được nhận diện
                            Locale locale = new Locale(languageCode);
                            String languageName = locale.getDisplayLanguage(new Locale("vi"));
                            if (languageName.equals(languageCode)) {
                                languageName = locale.getDisplayLanguage(Locale.ENGLISH);
                            }
                            languageName = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
                            detectedLanguageTextView.setText("Nhận diện: " + languageName);
                            
                            // Cập nhật spinner ngôn ngữ nguồn
                            updateSourceLanguageSpinner(languageCode);
                            
                            // Dịch văn bản
                            translateText(text, languageCode, selectedTargetLanguage.getLanguageCode());
                        }
                    });
                }
            } else {
                // Use selected source language
                detectedLanguageTextView.setText("Nhận diện: " + selectedSourceLanguage.getLanguageTitle());
                translateText(text, selectedSourceLanguage.getLanguageCode(), 
                        selectedTargetLanguage.getLanguageCode());
            }
        });
    }

    private void translateText(String text, String sourceLanguageCode, String targetLanguageCode) {
        if (sourceLanguageCode.equals(targetLanguageCode)) {
            // Same language, no need to translate
            translatedTextView.setText(text);
            loadingOverlay.setVisibility(View.GONE);
            return;
        }
        
        // Đảm bảo chỉ quan sát kết quả dịch một lần
        if (!isTranslationObserverSet) {
            translationRepository.getTranslationResult().observe(this, result -> {
                if (result != null) {
                    if (result.isSuccess()) {
                        lastTranslatedText = result.getTranslatedText();
                        translatedTextView.setText(lastTranslatedText);
                    } else {
                        translatedTextView.setText("Lỗi dịch văn bản: " + result.getErrorMessage());
                        Log.e(TAG, "Translation error: " + result.getErrorMessage());
                    }
                }
                loadingOverlay.setVisibility(View.GONE);
            });
            isTranslationObserverSet = true;
        }
        
        // Gọi phương thức dịch
        translationRepository.translateText(text, sourceLanguageCode, targetLanguageCode);
    }

    /**
     * Cập nhật spinner ngôn ngữ nguồn dựa trên mã ngôn ngữ được nhận diện
     * @param languageCode Mã ngôn ngữ được nhận diện
     */
    private void updateSourceLanguageSpinner(String languageCode) {
        if (languageCode == null || languageCode.isEmpty()) {
            return;
        }
        
        try {
            // Lấy danh sách ngôn ngữ từ translationRepository
            List<ModelLanguage> languages = translationRepository.getAvailableLanguages().getValue();
            if (languages == null) {
                Log.e(TAG, "Danh sách ngôn ngữ chưa sẵn sàng");
                return;
            }
            
            // Tìm ngôn ngữ trong danh sách
            for (int i = 0; i < languages.size(); i++) {
                ModelLanguage language = languages.get(i);
                if (language.getLanguageCode().equals(languageCode)) {
                    // Cập nhật spinner và biến selectedSourceLanguage
                    sourceLanguageSpinner.setSelection(i);
                    selectedSourceLanguage = language;
                    Log.d(TAG, "Đã cập nhật spinner ngôn ngữ nguồn: " + language.getLanguageTitle());
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật spinner ngôn ngữ nguồn: " + e.getMessage());
        }
    }

    /**
     * Phát hiện ngôn ngữ không phải Latin từ văn bản
     * @param text Văn bản cần kiểm tra
     * @return Mã ngôn ngữ hoặc chuỗi rỗng nếu không phát hiện được
     */
    private String detectNonLatinLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // Đếm số lượng ký tự của mỗi ngôn ngữ
        int chineseCount = 0;  // Tiếng Trung
        int japaneseCount = 0;  // Tiếng Nhật
        int koreanCount = 0;  // Tiếng Hàn
        int russianCount = 0;  // Tiếng Nga
        int arabicCount = 0;   // Tiếng Ả Rập
        int thaiCount = 0;     // Tiếng Thái
        
        // Kiểm tra từng ký tự trong văn bản
        for (char c : text.toCharArray()) {
            // Tiếng Trung (CJK Unified Ideographs)
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3400 && c <= 0x4DBF)) {
                chineseCount++;
            }
            // Tiếng Nhật (Hiragana, Katakana)
            else if ((c >= 0x3040 && c <= 0x309F) || (c >= 0x30A0 && c <= 0x30FF)) {
                japaneseCount++;
            }
            // Tiếng Hàn (Hangul)
            else if ((c >= 0xAC00 && c <= 0xD7AF) || (c >= 0x1100 && c <= 0x11FF)) {
                koreanCount++;
            }
            // Tiếng Nga (Cyrillic)
            else if (c >= 0x0400 && c <= 0x04FF) {
                russianCount++;
            }
            // Tiếng Ả Rập (Arabic)
            else if (c >= 0x0600 && c <= 0x06FF) {
                arabicCount++;
            }
            // Tiếng Thái (Thai)
            else if (c >= 0x0E00 && c <= 0x0E7F) {
                thaiCount++;
            }
        }
        
        // Ngưỡng nhận diện (có ít nhất 2 ký tự)
        final int THRESHOLD = 2;
        
        // Trả về ngôn ngữ có số lượng ký tự nhiều nhất
        if (chineseCount >= THRESHOLD && chineseCount >= japaneseCount && chineseCount >= koreanCount && 
            chineseCount >= russianCount && chineseCount >= arabicCount && chineseCount >= thaiCount) {
            Log.d(TAG, "Detected Chinese characters: " + chineseCount);
            return "zh"; // Tiếng Trung
        } else if (japaneseCount >= THRESHOLD && japaneseCount >= chineseCount && japaneseCount >= koreanCount && 
                   japaneseCount >= russianCount && japaneseCount >= arabicCount && japaneseCount >= thaiCount) {
            Log.d(TAG, "Detected Japanese characters: " + japaneseCount);
            return "ja"; // Tiếng Nhật
        } else if (koreanCount >= THRESHOLD && koreanCount >= chineseCount && koreanCount >= japaneseCount && 
                   koreanCount >= russianCount && koreanCount >= arabicCount && koreanCount >= thaiCount) {
            Log.d(TAG, "Detected Korean characters: " + koreanCount);
            return "ko"; // Tiếng Hàn
        } else if (russianCount >= THRESHOLD && russianCount >= chineseCount && russianCount >= japaneseCount && 
                   russianCount >= koreanCount && russianCount >= arabicCount && russianCount >= thaiCount) {
            Log.d(TAG, "Detected Russian characters: " + russianCount);
            return "ru"; // Tiếng Nga
        } else if (arabicCount >= THRESHOLD && arabicCount >= chineseCount && arabicCount >= japaneseCount && 
                   arabicCount >= koreanCount && arabicCount >= russianCount && arabicCount >= thaiCount) {
            Log.d(TAG, "Detected Arabic characters: " + arabicCount);
            return "ar"; // Tiếng Ả Rập
        } else if (thaiCount >= THRESHOLD && thaiCount >= chineseCount && thaiCount >= japaneseCount && 
                   thaiCount >= koreanCount && thaiCount >= russianCount && thaiCount >= arabicCount) {
            Log.d(TAG, "Detected Thai characters: " + thaiCount);
            return "th"; // Tiếng Thái
        }
        
        // Không phát hiện ngôn ngữ không phải Latin
        return "";
    }

    /**
     * Kiểm tra xem văn bản có chứa các ký tự đặc trưng của tiếng Việt không
     * @param text Văn bản cần kiểm tra
     * @return true nếu văn bản chứa ký tự đặc trưng của tiếng Việt
     */
    private boolean containsVietnameseCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Các ký tự đặc trưng của tiếng Việt
        String vietnameseChars = "àáảãạăằẳẵặâầẩẫậèéẻẽẹêềểễệìíỉĩịòóỏõọôồổỗộơờởỡợùúủũụưứừửữđÀÁẢÃẠĂẰẲẴẶÂẦẨẪẬÈÉẺẼẸÊỀỂỄỆÌÍỈĨỊÒÓỎÕỌÔỒỔỖỘƠỜỞỠỢÙÚỦŨỤƯỨỪỬỮĐ";
        
        // Các từ đặc trưng của tiếng Việt
        String[] vietnameseWords = {"của", "và", "là", "trong", "với", "được", "có", "không", "này", "cho", "các", "bị", "sẽ", "đã", "phải", "còn", "bạn", "tôi", "anh", "chị", "em", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín", "mười"};
        
        // Đếm số lượng ký tự đặc trưng của tiếng Việt
        int vietnameseCharCount = 0;
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
        
        // Nếu có ít nhất 1 ký tự đặc trưng hoặc 1 từ đặc trưng của tiếng Việt, có khả năng là tiếng Việt
        boolean isVietnamese = (vietnameseCharCount >= 1 || vietnameseWordCount >= 1);
        if (isVietnamese) {
            Log.d(TAG, "Phát hiện tiếng Việt: " + vietnameseCharCount + " ký tự, " + vietnameseWordCount + " từ");
        }
        return isVietnamese;
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != 
                    PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần cấp quyền camera để sử dụng tính năng này", 
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
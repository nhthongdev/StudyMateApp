package hcmute.edu.vn.thongvavan.finalproject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.thongvavan.finalproject.model.ModelLanguage;
import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.model.TranslationResult;
import hcmute.edu.vn.thongvavan.finalproject.utils.DialogManager;
import hcmute.edu.vn.thongvavan.finalproject.utils.ImageCaptureUtils;
import hcmute.edu.vn.thongvavan.finalproject.viewmodel.MainViewModel;

/**
 * MainActivity - Main screen of the application
 * Refactored to use MVVM architecture
 */
public class MainActivity extends AppCompatActivity {
    // UI Views
    private MaterialButton inputImageBtn;
    private MaterialButton recognizeTextBtn;
    private MaterialButton translateBtn;
    private MaterialButton realtimeTranslateBtn;
    private MaterialButton historyBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;
    private View loadingLayout;
    private TextView loadingMessageTv;
    private LinearLayout translationControlsLayout;
    private LinearLayout translatedTextLayout;
    private TextView sourceLanguageTv;
    private TextView targetLanguageDisplay;
    private TextView targetLanguageLabelTv;
    private TextView translatedTextTv;
    private AutoCompleteTextView targetLanguageSpinner;
    private ImageButton swapLanguagesBtn;
    private ImageButton speakSourceBtn;
    private ImageButton copySourceBtn;
    private ImageButton speakTranslatedBtn;
    private ImageButton copyTranslatedBtn;
    private ImageButton shareTranslatedBtn;
    private ImageButton clearTextBtn;
    private ImageButton clearImageBtn;
    
    // Text-to-Speech engine
    private android.speech.tts.TextToSpeech tts;
    private com.google.android.material.textfield.TextInputLayout targetLanguageLayout;
    
    // Translation variables
    private String detectedLanguageCode = "";
    private List<ModelLanguage> availableLanguages = new ArrayList<>();
    private ModelLanguage selectedTargetLanguage;
    
    // TAG
    private static final String TAG = "MAIN_TAG";

    // Dialog manager
    private DialogManager dialogManager;

    // ViewModel
    private MainViewModel viewModel;
    
    // TextWatcher for recognizedTextEt
    private TextWatcher recognizedTextWatcher;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher;
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Khởi tạo Text-to-Speech engine
        tts = new android.speech.tts.TextToSpeech(this, status -> {
            if (status != android.speech.tts.TextToSpeech.SUCCESS) {
                Log.e(TAG, "Không thể khởi tạo Text-to-Speech");
            }
        });

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title

        // Initialize UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        translateBtn = findViewById(R.id.translateBtn);
        realtimeTranslateBtn = findViewById(R.id.realtimeTranslateBtn);
        historyBtn = findViewById(R.id.historyBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);
        loadingLayout = findViewById(R.id.loadingLayout);
        loadingMessageTv = findViewById(R.id.loadingMessageTv);
        translationControlsLayout = findViewById(R.id.translationControlsLayout);
        translatedTextLayout = findViewById(R.id.translatedTextLayout);
        sourceLanguageTv = findViewById(R.id.sourceLanguageTv);
        targetLanguageDisplay = findViewById(R.id.targetLanguageDisplay);
        targetLanguageLabelTv = findViewById(R.id.targetLanguageLabelTv);
        translatedTextTv = findViewById(R.id.translatedTextTv);
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner);
        targetLanguageLayout = findViewById(R.id.targetLanguageLayout);
        swapLanguagesBtn = findViewById(R.id.swapLanguagesBtn);
        speakSourceBtn = findViewById(R.id.speakSourceBtn);
        copySourceBtn = findViewById(R.id.copySourceBtn);
        speakTranslatedBtn = findViewById(R.id.speakTranslatedBtn);
        copyTranslatedBtn = findViewById(R.id.copyTranslatedBtn);
        shareTranslatedBtn = findViewById(R.id.shareTranslatedBtn);
        clearTextBtn = findViewById(R.id.clearTextBtn);
        clearImageBtn = findViewById(R.id.clearImageBtn);

        // Create TextWatcher for recognizedTextEt to update translation when text changes
        recognizedTextWatcher = new TextWatcher() {
            private Handler textChangeHandler = new Handler(Looper.getMainLooper());
            private Runnable textChangeRunnable;
            private static final long TEXT_CHANGE_DELAY = 1000; // 1 giây
            
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Hủy bỏ các lần gọi trước đó chưa hoàn thành
                if (textChangeRunnable != null) {
                    textChangeHandler.removeCallbacks(textChangeRunnable);
                }
                
                // Tạo một runnable mới để xử lý văn bản
                textChangeRunnable = () -> {
                    String newText = s.toString().trim();
                    
                    // Xử lý trường hợp văn bản trống
                    if (newText.isEmpty()) {
                        // Nếu văn bản trống và đang hiển thị văn bản đã dịch
                        if (translatedTextLayout.getVisibility() == View.VISIBLE) {
                            // Xóa văn bản đã dịch
                            translatedTextTv.setText("");
                            
                            // Ẩn phần hiển thị văn bản đã dịch
                            translatedTextLayout.setVisibility(View.GONE);
                            
                            // Ẩn các nút điều khiển dịch
                            translationControlsLayout.setVisibility(View.GONE);
                            
                            // Ẩn hiệu ứng loading nếu đang hiển thị
                            hideLoadingEffect();
                            
                            // Đặt lại mã ngôn ngữ đã phát hiện
                            detectedLanguageCode = "";
                            sourceLanguageTv.setText("Phát hiện ngôn ngữ");
                            targetLanguageDisplay.setText("Chọn ngôn ngữ");
                            selectedTargetLanguage = null;
                        }
                        return;
                    }
                    
                    // Xử lý trường hợp văn bản không trống
                    // Chỉ hiển thị hiệu ứng loading khi văn bản dài hơn 5 ký tự
                    if (newText.length() > 5) {
                        // Hiển thị hiệu ứng đang nhận diện ngôn ngữ
                        showLoadingEffect("Đang nhận diện ngôn ngữ...");
                    }
                    
                    // Identify language for the new text
                    identifyLanguage(newText);
                    
                    // If translation controls are visible and we have a target language selected
                    if (translationControlsLayout.getVisibility() == View.VISIBLE && 
                        selectedTargetLanguage != null && 
                        !detectedLanguageCode.isEmpty()) {
                        
                        // Hiển thị hiệu ứng đang dịch
                        showLoadingEffect("Đang dịch văn bản...");
                        
                        // Automatically translate the new text
                        translateText(newText, detectedLanguageCode, selectedTargetLanguage.getLanguageCode());
                    }
                };
                
                // Trì hoãn việc xử lý văn bản để tránh gọi liên tục
                textChangeHandler.postDelayed(textChangeRunnable, TEXT_CHANGE_DELAY);
            }
        };
        
        // Add TextWatcher to recognizedTextEt
        recognizedTextEt.addTextChangedListener(recognizedTextWatcher);

        // Initially hide translation controls until text is recognized
        translationControlsLayout.setVisibility(View.INVISIBLE);
        translatedTextLayout.setVisibility(View.GONE);

        // Register context menu for recognized text EditText
        registerForContextMenu(recognizedTextEt);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        // Initialize dialog manager
        dialogManager = new DialogManager(this);

        // Initialize activity result launchers
        initActivityResultLaunchers();

        // Observe ViewModel LiveData
        observeViewModel();

        // Set click listeners
        setupClickListeners();
    }

    /**
     * Set up click listeners for buttons
     */
    private void setupClickListeners() {
        // Input image button click
        inputImageBtn.setOnClickListener(v -> {
            showInputImageDialog();
        });

        // Recognize text button click
        recognizeTextBtn.setOnClickListener(v -> {
            // Get image URI from ViewModel
            Uri imageUri = viewModel.getImageUri().getValue();
            
            if (imageUri == null) {
                showError("Vui lòng chọn một hình ảnh trước");
                return;
            }
            
            // Show loading effect
            showLoadingEffect("Đang nhận diện văn bản...");
            
            // Recognize text from image
            viewModel.recognizeTextFromImage(imageUri);
        });
        
        // Clear text button click
        clearTextBtn.setOnClickListener(v -> {
            // Xóa văn bản trong EditText
            recognizedTextEt.setText("");
            
            // Xóa văn bản đã dịch
            translatedTextTv.setText("");
            
            // Ẩn phần hiển thị văn bản đã dịch
            translatedTextLayout.setVisibility(View.GONE);
            
            // Ẩn các nút điều khiển dịch
            translationControlsLayout.setVisibility(View.GONE);
            
            // Đặt lại mã ngôn ngữ đã phát hiện
            detectedLanguageCode = "";
            sourceLanguageTv.setText("Phát hiện ngôn ngữ");
            targetLanguageDisplay.setText("Chọn ngôn ngữ");
            selectedTargetLanguage = null;
        });
        
        // Clear image button click
        clearImageBtn.setOnClickListener(v -> {
            // Đặt lại hình ảnh về mặc định
            imageIv.setImageResource(R.drawable.ic_image_placeholder);
            
            // Đặt lại URI hình ảnh trong ViewModel
            viewModel.setImageUri(null);
            
            // Xóa văn bản đã nhận diện
            recognizedTextEt.setText("");
            
            // Xóa văn bản đã dịch
            translatedTextTv.setText("");
            
            // Ẩn phần hiển thị văn bản đã dịch
            translatedTextLayout.setVisibility(View.GONE);
            
            // Ẩn các nút điều khiển dịch
            translationControlsLayout.setVisibility(View.GONE);
            
            // Đặt lại mã ngôn ngữ đã phát hiện
            detectedLanguageCode = "";
            sourceLanguageTv.setText("Phát hiện ngôn ngữ");
            targetLanguageDisplay.setText("Chọn ngôn ngữ");
            selectedTargetLanguage = null;
            
            // Hiển thị thông báo
            Toast.makeText(MainActivity.this, "Đã xóa ảnh", Toast.LENGTH_SHORT).show();
        });
        
        // Translate button click
        translateBtn.setOnClickListener(v -> {
            String text = recognizedTextEt.getText().toString().trim();
            if (text.isEmpty()) {
                showError("Vui lòng nhập văn bản để dịch");
                return;
            }
            
            if (selectedTargetLanguage == null) {
                showError("Vui lòng chọn ngôn ngữ đích");
                return;
            }
            
            // Show translated text layout
            translatedTextLayout.setVisibility(View.VISIBLE);
            
            // Translate text
            translateText(text, detectedLanguageCode, selectedTargetLanguage.getLanguageCode());
        });
        
        // Real-time translation button click - chức năng chưa phát triển
        realtimeTranslateBtn.setOnClickListener(v -> {
            // Tạm thời thông báo tính năng đang phát triển
            Toast.makeText(MainActivity.this, "Tính năng đang được phát triển", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(MainActivity.this, RealtimeTranslationActivity.class);
            // startActivity(intent);
        });
        
        // History button click
        historyBtn.setOnClickListener(v -> {
            // TODO: Implement history feature
            Toast.makeText(this, "Tính năng lịch sử đang được phát triển", Toast.LENGTH_SHORT).show();
        });
        
        // Swap languages button click
        swapLanguagesBtn.setOnClickListener(v -> {
            // Vô hiệu hóa nút để tránh nhấn nhiều lần
            swapLanguagesBtn.setEnabled(false);
            
            // Kiểm tra xem có thể hoán đổi ngôn ngữ hay không
            if (detectedLanguageCode.isEmpty() || selectedTargetLanguage == null) {
                showError("Không thể hoán đổi ngôn ngữ");
                swapLanguagesBtn.setEnabled(true);
                return;
            }
            
            try {
                // Lưu trữ tạm thời ngôn ngữ nguồn và đích
                String tempSourceCode = detectedLanguageCode;
                String tempTargetCode = selectedTargetLanguage.getLanguageCode();
                String tempTargetName = selectedTargetLanguage.getLanguageTitle();
                
                Log.d(TAG, "Hoán đổi ngôn ngữ: Từ " + tempSourceCode + " sang " + tempTargetCode);
                
                // Lưu trữ văn bản nguồn và văn bản đã dịch
                String sourceText = recognizedTextEt.getText().toString().trim();
                String translatedText = "";
                if (translatedTextLayout.getVisibility() == View.VISIBLE) {
                    translatedText = translatedTextTv.getText().toString().trim();
                }
                
                // Tìm ModelLanguage cho ngôn ngữ nguồn cũ
                ModelLanguage sourceLanguageModel = null;
                for (ModelLanguage lang : availableLanguages) {
                    if (lang.getLanguageCode().equals(tempSourceCode)) {
                        sourceLanguageModel = lang;
                        break;
                    }
                }
                
                // Nếu không tìm thấy trong danh sách, tạo một ModelLanguage mới
                if (sourceLanguageModel == null) {
                    // Tạo tên hiển thị từ mã ngôn ngữ sử dụng Locale
                    Locale locale = new Locale(tempSourceCode);
                    String displayName = locale.getDisplayLanguage(new Locale("vi"));
                    
                    // Nếu tên hiển thị trùng với mã ngôn ngữ, thử lấy bằng tiếng Anh
                    if (displayName.equals(tempSourceCode)) {
                        displayName = locale.getDisplayLanguage(new Locale("en"));
                    }
                    
                    // Viết hoa chữ cái đầu của tên ngôn ngữ
                    if (!displayName.isEmpty()) {
                        displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
                    } else {
                        displayName = tempSourceCode; // Sử dụng mã ngôn ngữ nếu không lấy được tên
                    }
                    
                    // Tạo ModelLanguage mới
                    sourceLanguageModel = new ModelLanguage(tempSourceCode, displayName);
                }
                
                // Lưu lại giá trị cuối cùng
                final ModelLanguage finalSourceLanguageModel = sourceLanguageModel;
                final String finalSourceText = sourceText;
                final String finalTranslatedText = translatedText;
                
                // Cập nhật UI trên luồng chính
                runOnUiThread(() -> {
                    // Cập nhật ngôn ngữ nguồn thành ngôn ngữ đích cũ
                    detectedLanguageCode = tempTargetCode;
                    sourceLanguageTv.setText(tempTargetName);
                    
                    // Cập nhật ngôn ngữ đích thành ngôn ngữ nguồn cũ
                    selectedTargetLanguage = finalSourceLanguageModel;
                    targetLanguageDisplay.setText(finalSourceLanguageModel.getLanguageTitle());
                    targetLanguageSpinner.setText(finalSourceLanguageModel.getLanguageTitle(), false);
                    
                    // Hoán đổi văn bản nguồn và văn bản đã dịch
                    if (!finalSourceText.isEmpty() && !finalTranslatedText.isEmpty() && translatedTextLayout.getVisibility() == View.VISIBLE) {
                        // Đặt văn bản đã dịch trước đó vào trường nhập liệu
                        recognizedTextEt.setText(finalTranslatedText);
                        
                        // Đặt văn bản nguồn trước đó vào trường văn bản đã dịch
                        translatedTextTv.setText(finalSourceText);
                        
                        // Đã hoán đổi văn bản, không cần dịch lại
                        hideLoadingEffect();
                    } else if (!finalSourceText.isEmpty()) {
                        // Nếu chỉ có văn bản nguồn, dịch văn bản đó với cặp ngôn ngữ mới
                        translationControlsLayout.setVisibility(View.VISIBLE);
                        translatedTextLayout.setVisibility(View.VISIBLE);
                        showLoadingEffect("Đang dịch văn bản...");
                        
                        // Gọi API dịch với cặp ngôn ngữ đã hoán đổi
                        translateText(finalSourceText, detectedLanguageCode, selectedTargetLanguage.getLanguageCode());
                    }
                    
                    // Thông báo cho người dùng
                    Toast.makeText(MainActivity.this, "Đã hoán đổi ngôn ngữ", Toast.LENGTH_SHORT).show();
                    
                    // Kích hoạt lại nút
                    swapLanguagesBtn.setEnabled(true);
                });
            } catch (Exception e) {
                Log.e(TAG, "Lỗi khi hoán đổi ngôn ngữ: " + e.getMessage());
                showError("Lỗi khi hoán đổi ngôn ngữ");
                swapLanguagesBtn.setEnabled(true);
            }
        });
        
        // Target language display click (to show dropdown)
        targetLanguageDisplay.setOnClickListener(v -> {
            targetLanguageLayout.setVisibility(View.VISIBLE);
            targetLanguageSpinner.showDropDown();
        });
        
        // Copy source text button click
        copySourceBtn.setOnClickListener(v -> {
            String text = recognizedTextEt.getText().toString().trim();
            if (!text.isEmpty()) {
                copyToClipboard(text);
                Toast.makeText(this, "Đã sao chép văn bản nguồn", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Copy translated text button click
        copyTranslatedBtn.setOnClickListener(v -> {
            String text = translatedTextTv.getText().toString().trim();
            if (!text.isEmpty()) {
                copyToClipboard(text);
                Toast.makeText(this, "Đã sao chép văn bản đã dịch", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Share translated text button click
        shareTranslatedBtn.setOnClickListener(v -> {
            String text = translatedTextTv.getText().toString().trim();
            if (!text.isEmpty()) {
                shareText(text);
            }
        });
        
        // Speak source text button click
        speakSourceBtn.setOnClickListener(v -> {
            String text = recognizedTextEt.getText().toString().trim();
            if (!text.isEmpty()) {
                speakText(text, detectedLanguageCode);
            }
        });
        
        // Speak translated text button click
        speakTranslatedBtn.setOnClickListener(v -> {
            String text = translatedTextTv.getText().toString().trim();
            if (!text.isEmpty() && selectedTargetLanguage != null) {
                speakText(text, selectedTargetLanguage.getLanguageCode());
            }
        });
    }

    /**
     * Show dialog to choose image source (camera or gallery)
     */
    private void showInputImageDialog() {
        // Use DialogManager to show input image dialog
        dialogManager.showInputImageDialog(inputImageBtn, new DialogManager.ImageSourceCallback() {
            @Override
            public void onCameraSelected() {
                // Handle camera selection
                if (viewModel.checkCameraPermissions()) {
                    // Camera permissions already granted, launch camera
                    pickImageFromCamera();
                } else {
                    // Request camera permissions
                    viewModel.requestCameraPermissions(MainActivity.this);
                }
            }

            @Override
            public void onGallerySelected() {
                // Handle gallery selection
                if (viewModel.checkStoragePermission()) {
                    // Storage permissions already granted, launch gallery
                    pickImageFromGallery();
                } else {
                    // Request storage permissions
                    viewModel.requestStoragePermission(MainActivity.this);
                }
            }
        });
    }

    /**
     * Pick image from camera
     */
    private void pickImageFromCamera() {
        Uri imageUri = viewModel.pickImageFromCamera(cameraActivityResultLauncher);
        if (imageUri != null) {
            viewModel.setImageUri(imageUri);
        }
    }

    /**
     * Pick image from gallery
     */
    private void pickImageFromGallery() {
        viewModel.pickImageFromGallery(galleryActivityResultLauncher);
    }

    /**
     * Handle image URI from camera or gallery
     * @param imageUri URI of the image
     */
    private void handleImageUri(Uri imageUri) {
        if (imageUri == null) {
            Log.e(TAG, "handleImageUri received null URI");
            showError("Không thể truy cập ảnh");
            return;
        }

        try {
            // Kiểm tra quyền truy cập
            try (InputStream stream = getContentResolver().openInputStream(imageUri)) {
                if (stream == null) {
                    throw new IOException("Cannot open input stream for URI: " + imageUri);
                }
            }

            // Cập nhật URI trong ViewModel
            viewModel.setImageUri(imageUri);

            // Xử lý và hiển thị ảnh với hướng đúng
            Bitmap bitmap = loadBitmapWithCorrectOrientation(imageUri);
            if (bitmap != null) {
                imageIv.setImageBitmap(bitmap);
                imageIv.setVisibility(View.VISIBLE);
            } else {
                // Fallback nếu không thể xử lý bitmap
                imageIv.setImageURI(null); // Clear cache
                imageIv.setImageURI(imageUri);
                imageIv.setVisibility(View.VISIBLE);
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Security error with URI: " + e.getMessage());
            showError("Không thể truy cập ảnh");
        } catch (IOException e) {
            Log.e(TAG, "IO error with URI: " + e.getMessage());
            showError("Không thể đọc ảnh");
        } catch (Exception e) {
            Log.e(TAG, "Error handling URI: " + e.getMessage());
            showError("Có lỗi xảy ra khi xử lý ảnh");
        }
    }
    
    /**
     * Tải bitmap từ URI và sửa hướng dựa trên thông tin EXIF
     * @param imageUri URI của ảnh
     * @return Bitmap đã được sửa hướng hoặc null nếu có lỗi
     */
    private Bitmap loadBitmapWithCorrectOrientation(Uri imageUri) {
        try {
            // Đọc bitmap từ URI
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;
            
            // Tạo bitmap từ input stream
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // Đọc thông tin EXIF để xác định hướng
            ExifInterface exif = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Với Android 7.0 trở lên, có thể đọc EXIF từ URI
                    inputStream = getContentResolver().openInputStream(imageUri);
                    if (inputStream != null) {
                        exif = new ExifInterface(inputStream);
                        inputStream.close();
                    }
                } else {
                    // Với Android cũ hơn, cần chuyển URI thành đường dẫn file
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = getContentResolver().query(imageUri, projection, null, null, null);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                            exif = new ExifInterface(path);
                        }
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading EXIF: " + e.getMessage());
            }
            
            // Nếu không thể đọc EXIF, trả về bitmap gốc
            if (exif == null) return bitmap;
            
            // Xác định góc xoay dựa trên thông tin EXIF
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int rotationAngle = 0;
            
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationAngle = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationAngle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationAngle = 270;
                    break;
            }
            
            // Nếu không cần xoay, trả về bitmap gốc
            if (rotationAngle == 0) return bitmap;
            
            // Xoay bitmap theo góc đã xác định
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationAngle);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle(); // Giải phóng bộ nhớ
            
            return rotatedBitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap: " + e.getMessage());
            return null;
        }
    }
    private void showError(String message) {
        // Vibrate để báo lỗi
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(300);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating: " + e.getMessage());
        }
        
        // Show error message
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    private void initActivityResultLaunchers() {
        // Camera activity result launcher
        cameraActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Handle result of camera intent
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // Lấy URI ảnh từ ViewModel
                        Uri imageUri = viewModel.getImageUri().getValue();
                        Log.d(TAG, "Camera result OK, image URI: " + (imageUri != null ? imageUri.toString() : "null"));

                        if (imageUri != null) {
                            handleImageUri(imageUri);
                        } else {
                            Log.e(TAG, "Camera returned null URI");
                            showError("Không thể lấy ảnh từ camera");
                        }
                    } else {
                        showError("Hủy chụp ảnh");
                        viewModel.setImageUri(null);
                    }
                });

        // Gallery activity result launcher
        galleryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri imageUri = data.getData();
                            Log.d(TAG, "Gallery result OK, image URI: " + imageUri.toString());
                            handleImageUri(imageUri);
                        }
                    } else {
                        showError("Hủy chọn ảnh");
                    }
                });
    }

    /**
     * Observe LiveData from ViewModel
     */
    private void observeViewModel() {
        // Observe image URI
        viewModel.getImageUri().observe(this, uri -> {
            if (uri != null) {
                imageIv.setImageURI(uri);
            }
        });

        // Observe loading state
        viewModel.isLoading().observe(this, isLoading -> {
            loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe loading message
        viewModel.getLoadingMessage().observe(this, message -> {
            loadingMessageTv.setText(message);
        });

        // Observe recognized text
        viewModel.getRecognizedText().observe(this, recognizedText -> {
            if (recognizedText != null) {
                if (recognizedText.isSuccess()) {
                    // Show recognized text - temporarily remove TextWatcher to avoid double translation
                    recognizedTextEt.removeTextChangedListener(recognizedTextWatcher);
                    
                    // Set text
                    recognizedTextEt.setText(recognizedText.getText());
                    
                    // Re-add TextWatcher
                    recognizedTextEt.addTextChangedListener(recognizedTextWatcher);
                    
                    // Show statistics in Toast
                    Toast.makeText(this, recognizedText.getStatistics(), Toast.LENGTH_LONG).show();
                    
                    // Identify language of recognized text
                    if (!recognizedText.isEmpty()) {
                        identifyLanguage(recognizedText.getText());
                    }
                } else {
                    // Show error
                    showError(recognizedText.getErrorMessage());
                    translationControlsLayout.setVisibility(View.INVISIBLE);
                }
            }
        });

        // Observe translation controls visibility
        viewModel.isTranslationControlsVisible().observe(this, isVisible -> {
            translationControlsLayout.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
        });
        
        // Observe translated text visibility
        viewModel.isTranslatedTextVisible().observe(this, isVisible -> {
            translatedTextLayout.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        });
        
        // Observe available languages
        viewModel.getAvailableLanguages().observe(this, languages -> {
            if (languages != null && !languages.isEmpty()) {
                availableLanguages = languages;
                setupLanguageSpinner();
            }
        });
        
        // Observe detected language
        viewModel.getDetectedLanguage().observe(this, languageCode -> {
            if (languageCode != null && !languageCode.isEmpty()) {
                detectedLanguageCode = languageCode;
                
                // Get the display name of the language
                String languageName = "Unknown";
                for (ModelLanguage lang : availableLanguages) {
                    if (lang.getLanguageCode().equals(languageCode)) {
                        languageName = lang.getLanguageTitle();
                        break;
                    }
                }
                
                // Update source language display
                sourceLanguageTv.setAlpha(0f);
                sourceLanguageTv.setText(languageName);
                sourceLanguageTv.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                
                // Find and select English as target language by default if source is not English
                // Otherwise select Vietnamese
                String defaultTargetCode = languageCode.equals("en") ? "vi" : "en";
                for (int i = 0; i < availableLanguages.size(); i++) {
                    if (availableLanguages.get(i).getLanguageCode().equals(defaultTargetCode)) {
                        targetLanguageSpinner.setText(availableLanguages.get(i).getLanguageTitle(), false);
                        targetLanguageDisplay.setText(availableLanguages.get(i).getLanguageTitle());
                        selectedTargetLanguage = availableLanguages.get(i);
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.recognizedTextEt) {
            menu.add(0, v.getId(), 0, "Copy");
            menu.add(0, v.getId(), 0, "Share");
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        String text = recognizedTextEt.getText().toString().trim();
        if (text.isEmpty()) {
            showError("No text to " + item.getTitle().toString().toLowerCase());
            return true;
        }

        if (item.getTitle().equals("Copy")) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Recognized Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getTitle().equals("Share")) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            return true;
        }

        return super.onContextItemSelected(item);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_text_options, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        String text = recognizedTextEt.getText().toString().trim();
        if (text.isEmpty() && (item.getItemId() == R.id.action_copy || 
                              item.getItemId() == R.id.action_share || 
                              item.getItemId() == R.id.action_clear)) {
            showError("No text to perform this action");
            return true;
        }
        
        int id = item.getItemId();
        
        if (id == R.id.action_copy) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Recognized Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
            return true;
        } else if (id == R.id.action_clear) {
            recognizedTextEt.setText("");
            translateBtn.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Text cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Sử dụng phương thức handlePermissionResult của ViewModel
        boolean permissionGranted = viewModel.handlePermissionResult(this, requestCode, grantResults);
        
        if (permissionGranted) {
            // Nếu quyền được cấp, thực hiện hành động tương ứng
            if (requestCode == ImageCaptureUtils.CAMERA_REQUEST_CODE) {
                pickImageFromCamera();
            } else if (requestCode == ImageCaptureUtils.STORAGE_REQUEST_CODE) {
                pickImageFromGallery();
            }
        } else {
            // Nếu quyền bị từ chối
            showError("Cần cấp quyền để sử dụng tính năng này");
        }
    }
    
    /**
     * Set up language spinner with available languages
     */
    private void setupLanguageSpinner() {
        // Create adapter for language spinner
        ArrayAdapter<ModelLanguage> adapter = new ArrayAdapter<ModelLanguage>(this, android.R.layout.simple_dropdown_item_1line, availableLanguages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).getLanguageTitle());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).getLanguageTitle());
                return textView;
            }
        };

        // Set adapter to spinner
        targetLanguageSpinner.setAdapter(adapter);

        // Set item click listener
        targetLanguageSpinner.setOnItemClickListener((parent, view, position, id) -> {
            // Get selected language
            selectedTargetLanguage = availableLanguages.get(position);
            
            // Update target language display
            targetLanguageDisplay.setText(selectedTargetLanguage.getLanguageTitle());
            
            // Hide the dropdown layout after selection
            targetLanguageLayout.setVisibility(View.GONE);
            
            // Show translation controls
            translationControlsLayout.setVisibility(View.VISIBLE);
        });
    }
    
    /**
     * Identify language of text
     * @param text Text to identify language for
     */
    private void identifyLanguage(String text) {
        // Hiển thị thông báo đang nhận diện ngôn ngữ
        showLoadingEffect("Đang nhận diện ngôn ngữ...");
        
        viewModel.identifyLanguage(text).observe(this, languageCode -> {
            // Ẩn hiệu ứng loading
            hideLoadingEffect();
            
            if (languageCode != null && !languageCode.isEmpty()) {
                detectedLanguageCode = languageCode;
                
                // Get the display name of the language
                String languageName = "Unknown";
                for (ModelLanguage lang : availableLanguages) {
                    if (lang.getLanguageCode().equals(languageCode)) {
                        languageName = lang.getLanguageTitle();
                        break;
                    }
                }
                
                // Update source language display
                sourceLanguageTv.setAlpha(0f);
                sourceLanguageTv.setText(languageName);
                sourceLanguageTv.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .start();
                
                // Find and select English as target language by default if source is not English
                // Otherwise select Vietnamese
                String defaultTargetCode = languageCode.equals("en") ? "vi" : "en";
                for (int i = 0; i < availableLanguages.size(); i++) {
                    if (availableLanguages.get(i).getLanguageCode().equals(defaultTargetCode)) {
                        targetLanguageSpinner.setText(availableLanguages.get(i).getLanguageTitle(), false);
                        targetLanguageDisplay.setText(availableLanguages.get(i).getLanguageTitle());
                        selectedTargetLanguage = availableLanguages.get(i);
                        break;
                    }
                }
            }
        });
    }
    
    /**
     * Hiển thị hiệu ứng loading với thông báo
     * @param message Thông báo hiển thị
     */
    private void showLoadingEffect(String message) {
        loadingMessageTv.setText(message);
        if (loadingLayout.getVisibility() != View.VISIBLE) {
            loadingLayout.setAlpha(0f);
            loadingLayout.setVisibility(View.VISIBLE);
            loadingLayout.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }
    
    /**
     * Ẩn hiệu ứng loading
     */
    private void hideLoadingEffect() {
        if (loadingLayout.getVisibility() == View.VISIBLE) {
            loadingLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> loadingLayout.setVisibility(View.GONE))
                    .start();
        }
    }
    
    /**
     * Sao chép văn bản vào clipboard
     * @param text Văn bản cần sao chép
     */
    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Translated Text", text);
        clipboard.setPrimaryClip(clip);
    }
    
    /**
     * Chia sẻ văn bản
     * @param text Văn bản cần chia sẻ
     */
    private void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }
    
    /**
     * Đọc văn bản bằng Text-to-Speech
     * @param text Văn bản cần đọc
     * @param languageCode Mã ngôn ngữ
     */
    private void speakText(String text, String languageCode) {
        if (tts == null) {
            showError("Không thể khởi tạo Text-to-Speech");
            return;
        }
        
        // Thiết lập ngôn ngữ
        int result = tts.setLanguage(new Locale(languageCode));
        
        if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA ||
            result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
            showError("Ngôn ngữ này không được hỗ trợ đọc");
        } else {
            // Đọc văn bản
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    /**
     * Translate text from source language to target language
     * @param text Text to translate
     * @param sourceLanguageCode Source language code
     * @param targetLanguageCode Target language code
     */
    private void translateText(String text, String sourceLanguageCode, String targetLanguageCode) {
        // Kiểm tra văn bản có trống không
        if (text == null || text.trim().isEmpty()) {
            hideLoadingEffect();
            showError("Không có văn bản để dịch");
            return;
        }
        
        // Kiểm tra mã ngôn ngữ có hợp lệ không
        if (sourceLanguageCode == null || sourceLanguageCode.isEmpty() ||
            targetLanguageCode == null || targetLanguageCode.isEmpty()) {
            hideLoadingEffect();
            showError("Mã ngôn ngữ không hợp lệ");
            return;
        }
        
        // Lấy tên hiển thị của ngôn ngữ
        String sourceLanguageName = "Không xác định";
        String targetLanguageName = "Không xác định";
        
        // Tìm tên ngôn ngữ nguồn
        for (ModelLanguage lang : availableLanguages) {
            if (lang.getLanguageCode().equals(sourceLanguageCode)) {
                sourceLanguageName = lang.getLanguageTitle();
                break;
            }
        }
        
        // Tìm tên ngôn ngữ đích
        for (ModelLanguage lang : availableLanguages) {
            if (lang.getLanguageCode().equals(targetLanguageCode)) {
                targetLanguageName = lang.getLanguageTitle();
                break;
            }
        }
        
        // Hiển thị thông báo đang dịch
        showLoadingEffect("Đang dịch từ " + sourceLanguageName + " sang " + targetLanguageName + "...");
        
        // Lưu lại các giá trị để sử dụng trong lambda
        final String finalSourceLanguageName = sourceLanguageName;
        final String finalTargetLanguageName = targetLanguageName;
        
        viewModel.translateText(text, sourceLanguageCode, targetLanguageCode).observe(this, result -> {
            // Ẩn hiệu ứng loading
            hideLoadingEffect();
            
            if (result != null) {
                if (result.isSuccess()) {
                    Log.d("TRANSLATION_DEBUG", "Dịch thành công từ " + finalSourceLanguageName + 
                          " sang " + finalTargetLanguageName + ": " + result.getTranslatedText());
                    
                    // Hiển thị văn bản đã dịch với hiệu ứng fade-in
                    translatedTextTv.setAlpha(0f);
                    translatedTextTv.setText(result.getTranslatedText());
                    translatedTextTv.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start();
                    
                    // Hiển thị layout chứa văn bản đã dịch nếu chưa hiển thị
                    if (translatedTextLayout.getVisibility() != View.VISIBLE) {
                        translatedTextLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    // Hiển thị lỗi
                    Log.e("TRANSLATION_DEBUG", "Lỗi dịch: " + result.getErrorMessage());
                    showError(result.getErrorMessage());
                    translatedTextTv.setText("");
                }
            }
        });
    }
}

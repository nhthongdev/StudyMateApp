package hcmute.edu.vn.thongvavan.finalproject;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.IOException;
import java.io.InputStream;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
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
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;
    private View loadingLayout;
    private TextView loadingMessageTv;

    // TAG
    private static final String TAG = "MAIN_TAG";

    // Dialog manager
    private DialogManager dialogManager;

    // ViewModel
    private MainViewModel viewModel;

    // Activity Result Launchers
    private ActivityResultLauncher<Intent> galleryActivityResultLauncher;
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title

        // Initialize UI Views
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        translateBtn = findViewById(R.id.translateBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);
        loadingLayout = findViewById(R.id.loadingLayout);
        loadingMessageTv = findViewById(R.id.loadingMessageTv);

        // Initially hide translate button until text is recognized
        translateBtn.setVisibility(View.INVISIBLE);

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
        // Set click listener for translate button (will be implemented later)
        translateBtn.setOnClickListener(v -> {
            // This will be implemented when we add translation feature
            Toast.makeText(MainActivity.this, "Translation feature coming soon!", Toast.LENGTH_SHORT).show();
        });

        // Handle click, show input image dialog
        inputImageBtn.setOnClickListener(v -> showInputImageDialog());

        // Handle click, recognize text from image
        recognizeTextBtn.setOnClickListener(view -> {
            Uri currentImageUri = viewModel.getImageUri().getValue();
            if (currentImageUri == null) {
                showError("Pick image first...");
            } else {
                // Use ViewModel to recognize text
                viewModel.recognizeTextFromImage(currentImageUri);
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

            // Cập nhật giao diện
            imageIv.setImageURI(null); // Clear cache
            imageIv.setImageURI(imageUri);
            imageIv.setVisibility(View.VISIBLE);

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
     * Show error message in Toast
     * @param message Error message to show
     */
    private void showError(String message) {
        runOnUiThread(() -> {
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Initialize activity result launchers for camera and gallery
     */
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
                try {
                    // Ghi log để debug
                    Log.d(TAG, "Setting image URI: " + uri.toString());

                    // Kiểm tra xem URI có thể truy cập được không
                    getContentResolver().openInputStream(uri).close();

                    // Clear ảnh cũ và cache
                    imageIv.setImageURI(null);

                    // Set ảnh mới
                    imageIv.setImageURI(uri);

                    // Enable nút recognize text
                    recognizeTextBtn.setEnabled(true);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting image URI: " + e.getMessage());
                    viewModel.setImageUri(null);
                    showError("Không thể truy cập ảnh");
                }
            } else {
                // Nếu URI là null, đặt ảnh mặc định
                imageIv.setImageResource(R.drawable.ic_image_placeholder);
                recognizeTextBtn.setEnabled(false);
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
                    // Set recognized text to EditText
                    recognizedTextEt.setText(recognizedText.getText());
                    // Show statistics in a toast
                    Toast.makeText(MainActivity.this, recognizedText.getStatistics(), Toast.LENGTH_SHORT).show();
                    // Show translate button
                    translateBtn.setVisibility(View.VISIBLE);
                } else {
                    // Show error message
                    showError("Error: " + recognizedText.getErrorMessage());
                    translateBtn.setVisibility(View.INVISIBLE);
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
}

package hcmute.edu.vn.thongvavan.finalproject.viewmodel;

import android.app.Activity;
import android.app.Application;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.repository.ImageRepository;
import hcmute.edu.vn.thongvavan.finalproject.repository.TextRecognitionRepository;

/**
 * ViewModel for the main screen
 */
public class MainViewModel extends AndroidViewModel {
    private final ImageRepository imageRepository;
    private final TextRecognitionRepository textRecognitionRepository;

    // LiveData for UI state
    private final MutableLiveData<Boolean> isTranslateButtonVisible = new MutableLiveData<>(false);
    private final MutableLiveData<RecognizedText> recognizedTextLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadingMessage = new MutableLiveData<>("");

    public MainViewModel(@NonNull Application application) {
        super(application);
        imageRepository = new ImageRepository(application);
        textRecognitionRepository = new TextRecognitionRepository(application);
    }

    /**
     * Get the current image URI
     * @return LiveData containing the image URI
     */
    public LiveData<Uri> getImageUri() {
        return imageRepository.getImageUri();
    }

    /**
     * Set the image URI
     * @param uri Image URI
     */
    public void setImageUri(Uri uri) {
        imageRepository.setImageUri(uri);
    }

    /**
     * Get the recognized text
     * @return LiveData containing the recognized text
     */
    public LiveData<RecognizedText> getRecognizedText() {
        return recognizedTextLiveData;
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
     * Check if camera permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    public boolean checkCameraPermissions() {
        return imageRepository.checkCameraPermissions();
    }

    /**
     * Request camera permissions
     * @param activity Activity to request permissions from
     */
    public void requestCameraPermissions(Activity activity) {
        imageRepository.requestCameraPermissions(activity);
    }

    /**
     * Check if storage permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    public boolean checkStoragePermission() {
        return imageRepository.checkStoragePermission();
    }

    /**
     * Request storage permissions
     * @param activity Activity to request permissions from
     */
    public void requestStoragePermission(Activity activity) {
        imageRepository.requestStoragePermission(activity);
    }

    /**
     * Pick image from gallery
     * @param galleryLauncher ActivityResultLauncher for gallery
     */
    public void pickImageFromGallery(ActivityResultLauncher<android.content.Intent> galleryLauncher) {
        imageRepository.pickImageFromGallery(galleryLauncher);
    }

    /**
     * Pick image from camera
     * @param cameraLauncher ActivityResultLauncher for camera
     * @return URI of the image file
     */
    public Uri pickImageFromCamera(ActivityResultLauncher<android.content.Intent> cameraLauncher) {
        return imageRepository.pickImageFromCamera(cameraLauncher);
    }

    // Observers for loading state and message
    private Observer<Boolean> loadingObserver;
    private Observer<String> loadingMessageObserver;
    private Observer<RecognizedText> recognizedTextObserver;
    
    /**
     * Recognize text from an image URI
     * @param imageUri URI of the image to recognize text from
     * @return LiveData containing the recognized text result
     */
    public LiveData<RecognizedText> recognizeTextFromImage(Uri imageUri) {
        // Remove any existing observers to prevent duplicates
        removeObservers();
        
        // Get result from repository
        LiveData<RecognizedText> result = textRecognitionRepository.recognizeTextFromImage(imageUri);

        // Create and register loading state observer
        loadingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                isLoading.setValue(loading);
            }
        };
        textRecognitionRepository.isLoading().observeForever(loadingObserver);
        
        // Create and register loading message observer
        loadingMessageObserver = new Observer<String>() {
            @Override
            public void onChanged(String message) {
                loadingMessage.setValue(message);
            }
        };
        textRecognitionRepository.getLoadingMessage().observeForever(loadingMessageObserver);

        // Create and register recognized text observer
        recognizedTextObserver = new Observer<RecognizedText>() {
            @Override
            public void onChanged(RecognizedText recognizedText) {
                if (recognizedText != null) {
                    recognizedTextLiveData.setValue(recognizedText);
                    isTranslateButtonVisible.setValue(recognizedText.isSuccess() && !recognizedText.isEmpty());
                }
            }
        };
        result.observeForever(recognizedTextObserver);

        return result;
    }
    
    /**
     * Remove all observers to prevent memory leaks
     */
    private void removeObservers() {
        if (loadingObserver != null) {
            try {
                textRecognitionRepository.isLoading().removeObserver(loadingObserver);
                loadingObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing loading observer: " + e.getMessage());
            }
        }
        
        if (loadingMessageObserver != null) {
            try {
                textRecognitionRepository.getLoadingMessage().removeObserver(loadingMessageObserver);
                loadingMessageObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing message observer: " + e.getMessage());
            }
        }
        
        if (recognizedTextObserver != null) {
            // Không thể trực tiếp gỡ bỏ observer từ result LiveData trước đó
            // vì chúng ta không lưu trữ tham chiếu đến nó
            // Đánh dấu observer là null để tránh sử dụng lại
            recognizedTextObserver = null;
        }
    }
    
    /**
     * Called when the ViewModel is no longer used and will be destroyed
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        removeObservers();
        textRecognitionRepository.shutdown();
    }

    /**
     * Get translate button visibility state
     * @return LiveData containing the visibility state
     */
    public LiveData<Boolean> isTranslateButtonVisible() {
        return isTranslateButtonVisible;
    }

    /**
     * Set translate button visibility
     * @param visible true to show, false to hide
     */
    public void setTranslateButtonVisible(boolean visible) {
        isTranslateButtonVisible.setValue(visible);
    }

    /**
     * Handle permission result
     * @param activity Activity that received the permission result
     * @param requestCode Request code
     * @param grantResults Grant results
     * @return true if permission was granted, false otherwise
     */
    public boolean handlePermissionResult(Activity activity, int requestCode, int[] grantResults) {
        return imageRepository.handlePermissionResult(activity, requestCode, grantResults);
    }

    // Phương thức onCleared đã được định nghĩa ở trên
}

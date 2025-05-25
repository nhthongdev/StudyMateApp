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

import java.util.List;

import hcmute.edu.vn.thongvavan.finalproject.model.ModelLanguage;
import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.model.TranslationResult;
import hcmute.edu.vn.thongvavan.finalproject.repository.ImageRepository;
import hcmute.edu.vn.thongvavan.finalproject.repository.TextRecognitionRepository;
import hcmute.edu.vn.thongvavan.finalproject.repository.TranslationRepository;

/**
 * ViewModel for the main screen
 */
public class MainViewModel extends AndroidViewModel {
    private final ImageRepository imageRepository;
    private final TextRecognitionRepository textRecognitionRepository;
    private final TranslationRepository translationRepository;

    // LiveData for UI state
    private final MutableLiveData<Boolean> isTranslateButtonVisible = new MutableLiveData<>(false);
    private final MutableLiveData<RecognizedText> recognizedTextLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadingMessage = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isTranslationControlsVisible = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isTranslatedTextVisible = new MutableLiveData<>(false);

    // Observers for translation repository
    private Observer<Boolean> translationLoadingObserver;
    private Observer<String> translationLoadingMessageObserver;
    private Observer<TranslationResult> translationResultObserver;
    private Observer<String> detectedLanguageObserver;

    public MainViewModel(@NonNull Application application) {
        super(application);
        imageRepository = new ImageRepository(application);
        textRecognitionRepository = new TextRecognitionRepository(application);
        translationRepository = new TranslationRepository(application);
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
    
    // LiveData for detected language
    private final MutableLiveData<String> detectedLanguage = new MutableLiveData<>();
    
    /**
     * Get detected language
     * @return LiveData containing detected language code
     */
    public LiveData<String> getDetectedLanguage() {
        return detectedLanguage;
    }
    
    /**
     * Set detected language
     * @param languageCode Language code
     */
    public void setDetectedLanguage(String languageCode) {
        detectedLanguage.setValue(languageCode);
    }
    
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
     * Remove text recognition observers to prevent memory leaks
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
     * Remove translation observers to prevent memory leaks
     */
    private void removeTranslationObservers() {
        if (translationLoadingObserver != null) {
            try {
                translationRepository.isLoading().removeObserver(translationLoadingObserver);
                translationLoadingObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing translation loading observer: " + e.getMessage());
            }
        }
        
        if (translationLoadingMessageObserver != null) {
            try {
                translationRepository.getLoadingMessage().removeObserver(translationLoadingMessageObserver);
                translationLoadingMessageObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing translation message observer: " + e.getMessage());
            }
        }
        
        if (translationResultObserver != null) {
            try {
                translationRepository.getTranslationResult().removeObserver(translationResultObserver);
                translationResultObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing translation result observer: " + e.getMessage());
            }
        }
        
        if (detectedLanguageObserver != null) {
            try {
                translationRepository.getDetectedLanguage().removeObserver(detectedLanguageObserver);
                detectedLanguageObserver = null;
            } catch (Exception e) {
                android.util.Log.e("MainViewModel", "Error removing detected language observer: " + e.getMessage());
            }
        }
    }
    
    /**
     * Called when the ViewModel is no longer used and will be destroyed
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        removeObservers();
        removeTranslationObservers();
        textRecognitionRepository.shutdown();
        translationRepository.shutdown();
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
    
    /**
     * Get available languages for translation
     * @return LiveData containing list of available languages
     */
    public LiveData<List<ModelLanguage>> getAvailableLanguages() {
        return translationRepository.getAvailableLanguages();
    }
    
    /**
     * Get translation controls visibility state
     * @return LiveData containing visibility state
     */
    public LiveData<Boolean> isTranslationControlsVisible() {
        return isTranslationControlsVisible;
    }
    
    /**
     * Set translation controls visibility
     * @param visible true to show, false to hide
     */
    public void setTranslationControlsVisible(boolean visible) {
        isTranslationControlsVisible.setValue(visible);
    }
    
    /**
     * Get translated text visibility state
     * @return LiveData containing visibility state
     */
    public LiveData<Boolean> isTranslatedTextVisible() {
        return isTranslatedTextVisible;
    }
    
    /**
     * Translate text from source language to target language
     * @param text Text to translate
     * @param sourceLanguageCode Source language code
     * @param targetLanguageCode Target language code
     * @return LiveData containing translation result
     */
    public LiveData<TranslationResult> translateText(String text, String sourceLanguageCode, String targetLanguageCode) {
        // Remove any existing translation observers to prevent duplicates
        removeTranslationObservers();
        
        // Log dịch văn bản để debug
        android.util.Log.d("MainViewModel", "Translating from " + sourceLanguageCode + " to " + targetLanguageCode + ": " + text);
        
        // Get result from repository
        LiveData<TranslationResult> result = translationRepository.translateText(text, sourceLanguageCode, targetLanguageCode);
        
        // Create and register loading state observer
        translationLoadingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                isLoading.setValue(loading);
            }
        };
        translationRepository.isLoading().observeForever(translationLoadingObserver);
        
        // Create and register loading message observer
        translationLoadingMessageObserver = new Observer<String>() {
            @Override
            public void onChanged(String message) {
                loadingMessage.setValue(message);
            }
        };
        translationRepository.getLoadingMessage().observeForever(translationLoadingMessageObserver);
        
        // Create and register translation result observer
        translationResultObserver = new Observer<TranslationResult>() {
            @Override
            public void onChanged(TranslationResult translationResult) {
                if (translationResult != null) {
                    android.util.Log.d("MainViewModel", "Translation result received: " + 
                                      (translationResult.isSuccess() ? "SUCCESS" : "FAILURE") + 
                                      ", Text: " + translationResult.getTranslatedText());
                    
                    if (translationResult.isSuccess()) {
                        isTranslatedTextVisible.setValue(true);
                    }
                }
            }
        };
        translationRepository.getTranslationResult().observeForever(translationResultObserver);
        
        return result;
    }
    
    /**
     * Set translated text visibility
     * @param visible true to show, false to hide
     */
    public void setTranslatedTextVisible(boolean visible) {
        isTranslatedTextVisible.setValue(visible);
    }
    
    /**
     * Identify the language of a text
     * @param text Text to identify language for
     * @return LiveData containing detected language code
     */
    public LiveData<String> identifyLanguage(String text) {
        // Remove any existing observers to prevent duplicates
        removeTranslationObservers();
        
        // Identify language
        translationRepository.identifyLanguage(text);
        
        // Create and register loading state observer
        translationLoadingObserver = new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean loading) {
                isLoading.setValue(loading);
            }
        };
        translationRepository.isLoading().observeForever(translationLoadingObserver);
        
        // Create and register loading message observer
        translationLoadingMessageObserver = new Observer<String>() {
            @Override
            public void onChanged(String message) {
                loadingMessage.setValue(message);
            }
        };
        translationRepository.getLoadingMessage().observeForever(translationLoadingMessageObserver);
        
        // Create and register detected language observer
        detectedLanguageObserver = new Observer<String>() {
            @Override
            public void onChanged(String languageCode) {
                // Show translation controls when language is detected
                if (languageCode != null && !languageCode.isEmpty()) {
                    isTranslationControlsVisible.setValue(true);
                }
            }
        };
        translationRepository.getDetectedLanguage().observeForever(detectedLanguageObserver);
        
        return translationRepository.getDetectedLanguage();
    }
}

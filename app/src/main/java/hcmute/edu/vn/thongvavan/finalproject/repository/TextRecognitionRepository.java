package hcmute.edu.vn.thongvavan.finalproject.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.utils.TextRecognitionUtils;

/**
 * Repository for text recognition operations
 */
public class TextRecognitionRepository {
    private static final String TAG = "TextRecognitionRepo";
    
    private final Context context;
    private final TextRecognizer textRecognizer;
    
    // LiveData for loading state
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadingMessage = new MutableLiveData<>("");
    
    public TextRecognitionRepository(Context context) {
        this.context = context;
        
        // Initialize text recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
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
     * Recognize text from an image URI
     * @param imageUri URI of the image to recognize text from
     * @return LiveData containing the recognized text result
     */
    public LiveData<RecognizedText> recognizeTextFromImage(Uri imageUri) {
        MutableLiveData<RecognizedText> resultLiveData = new MutableLiveData<>();
        
        // Update loading state
        isLoading.postValue(true);
        loadingMessage.postValue("Preparing image...");
        
        try {
            // Prepare InputImage from image uri
            InputImage inputImage = InputImage.fromFilePath(context, imageUri);
            
            // Update loading message
            loadingMessage.postValue("Recognizing text...");
            
            // Start text recognition process
            textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            // Update loading state
                            isLoading.postValue(false);
                            
                            // Process the recognized text for better formatting
                            String recognizedText = TextRecognitionUtils.processRecognizedText(text);
                            Log.d(TAG, "onSuccess: recognizedText: " + recognizedText);
                            
                            // Get text statistics
                            String statistics = TextRecognitionUtils.getTextStatistics(recognizedText);
                            
                            // Create and post result
                            resultLiveData.postValue(new RecognizedText(recognizedText, statistics));
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Update loading state
                            isLoading.postValue(false);
                            Log.d(TAG, "onFailure: ", e);
                            
                            // Create and post error result
                            resultLiveData.postValue(new RecognizedText(e.getMessage()));
                        }
                    });
        } catch (IOException e) {
            // Update loading state
            isLoading.postValue(false);
            Log.d(TAG, "recognizeTextFromImage: ", e);
            
            // Create and post error result
            resultLiveData.postValue(new RecognizedText(e.getMessage()));
        }
        
        return resultLiveData;
    }
    
    /**
     * Release resources when no longer needed
     */
    public void shutdown() {
        textRecognizer.close();
    }
}

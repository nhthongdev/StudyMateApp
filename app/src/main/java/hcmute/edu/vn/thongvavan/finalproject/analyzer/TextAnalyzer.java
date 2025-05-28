package hcmute.edu.vn.thongvavan.finalproject.analyzer;

import android.content.Context;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;

/**
 * Analyzer class for real-time text recognition from camera frames
 */
@androidx.camera.core.ExperimentalGetImage
public class TextAnalyzer implements ImageAnalysis.Analyzer {
    private static final String TAG = "TextAnalyzer";
    private final TextRecognizer textRecognizer;
    private final Context context;
    private final TextAnalyzerCallback callback;
    private final Executor executor;
    
    // Flag to prevent processing multiple frames simultaneously
    private boolean isProcessing = false;
    
    // Counter to skip frames (not necessary to process every frame)
    private int frameCounter = 0;
    private static final int FRAME_SKIP = 10; // Process every 10th frame

    /**
     * Callback interface for text detection results
     */
    public interface TextAnalyzerCallback {
        void onTextDetected(RecognizedText recognizedText);
    }

    /**
     * Constructor
     * @param context Application context
     * @param callback Callback to receive text detection results
     */
    public TextAnalyzer(Context context, TextAnalyzerCallback callback) {
        this.context = context;
        this.callback = callback;
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    @androidx.camera.core.ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        frameCounter++;
        
        // Skip frames to improve performance
        if (frameCounter % FRAME_SKIP != 0 || isProcessing) {
            imageProxy.close();
            return;
        }
        
        isProcessing = true;
        
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(
                    mediaImage, 
                    imageProxy.getImageInfo().getRotationDegrees());
            
            // Process the image
            Task<Text> task = textRecognizer.process(image)
                    .addOnSuccessListener(executor, new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            // Extract text
                            String recognizedText = text.getText();
                            
                            // Skip if no text detected or text is too short
                            if (recognizedText.isEmpty() || recognizedText.length() < 3) {
                                isProcessing = false;
                                imageProxy.close();
                                return;
                            }
                            
                            // Create result object - sử dụng constructor phù hợp
                            RecognizedText result = new RecognizedText(recognizedText, "Detected at " + System.currentTimeMillis());
                            
                            // Send result to callback
                            callback.onTextDetected(result);
                            
                            isProcessing = false;
                            imageProxy.close();
                        }
                    })
                    .addOnFailureListener(executor, new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Text recognition failed: " + e.getMessage());
                            RecognizedText result = new RecognizedText(e.getMessage());
                            callback.onTextDetected(result);
                            isProcessing = false;
                            imageProxy.close();
                        }
                    });
        } else {
            isProcessing = false;
            imageProxy.close();
        }
    }
}
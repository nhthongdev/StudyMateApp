package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Utility class for Firebase operations
 */
public class FirebaseUtils {
    private static final String TAG = "FirebaseUtils";

    /**
     * Initialize Firebase with default options if not already initialized
     * @param context Application context
     */
    public static void initializeFirebase(Context context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                // Create default FirebaseOptions if no custom options are needed
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setProjectId("studymate-app")
                        .setApplicationId("1:123456789012:android:1234567890123456789012")
                        .setApiKey("AIzaSyDummyApiKeyForMLKitUsage")
                        .build();
                
                FirebaseApp.initializeApp(context, options);
                Log.d(TAG, "Firebase initialized successfully");
            } else {
                Log.d(TAG, "Firebase already initialized");
            }
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed: " + e.getMessage());
        }
    }
}

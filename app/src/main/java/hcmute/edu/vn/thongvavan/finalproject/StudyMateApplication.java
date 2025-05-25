package hcmute.edu.vn.thongvavan.finalproject;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

import hcmute.edu.vn.thongvavan.finalproject.utils.FirebaseUtils;

/**
 * Application class for initializing Firebase and other libraries
 */
public class StudyMateApplication extends Application {
    private static final String TAG = "StudyMateApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase using FirebaseUtils
        FirebaseUtils.initializeFirebase(this);
        
        Log.d(TAG, "Application initialized");
    }
}

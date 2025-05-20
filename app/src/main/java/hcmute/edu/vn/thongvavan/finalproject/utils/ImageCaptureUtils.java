package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for handling image capture and permissions
 */
public class ImageCaptureUtils {

    // Permission request codes
    public static final int CAMERA_REQUEST_CODE = 100;
    public static final int STORAGE_REQUEST_CODE = 101;

    /**
     * Check if camera permissions are granted
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    public static boolean checkCameraPermissions(Context context) {
        boolean cameraResult = ContextCompat.checkSelfPermission(context, 
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        
        // Storage permission check depends on Android version
        boolean storageResult = true;
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // For Android 10 (Q) and below
            storageResult = ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            // For Android 11 (R)
            storageResult = ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // For Android 12+ (S and above)
            storageResult = ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        }

        return cameraResult && storageResult;
    }

    /**
     * Request camera permissions
     * @param activity Activity to request permissions from
     */
    public static void requestCameraPermissions(Activity activity) {
        // Camera permission is always needed
        String[] cameraPermissions;
        
        // Storage permission depends on Android version
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // For Android 10 (Q) and below
            cameraPermissions = new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            // For Android 11 (R)
            cameraPermissions = new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        } else {
            // For Android 12+ (S and above)
            cameraPermissions = new String[] {
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        }
        
        ActivityCompat.requestPermissions(activity, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    /**
     * Check if storage permissions are granted
     * @param context Application context
     * @return true if permissions are granted, false otherwise
     */
    public static boolean checkStoragePermission(Context context) {
        // Storage permission check depends on Android version
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // For Android 10 (Q) and below
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            // For Android 11 (R)
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // For Android 12+ (S and above)
            return ContextCompat.checkSelfPermission(context, 
                    Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permissions
     * @param activity Activity to request permissions from
     */
    public static void requestStoragePermission(Activity activity) {
        String[] storagePermissions;
        
        // Storage permission depends on Android version
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // For Android 10 (Q) and below
            storagePermissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.R) {
            // For Android 11 (R)
            storagePermissions = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE};
        } else {
            // For Android 12+ (S and above)
            storagePermissions = new String[] {Manifest.permission.READ_MEDIA_IMAGES};
        }
        
        ActivityCompat.requestPermissions(activity, storagePermissions, STORAGE_REQUEST_CODE);
    }

    /**
     * Launch gallery to pick an image
     * @param galleryLauncher ActivityResultLauncher for gallery
     */
    public static void pickImageFromGallery(ActivityResultLauncher<Intent> galleryLauncher) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    /**
     * Launch camera to take a photo
     * @param context Application context
     * @param cameraLauncher ActivityResultLauncher for camera
     * @return Uri of the image file
     */
    public static Uri pickImageFromCamera(Context context, ActivityResultLauncher<Intent> cameraLauncher) {
        Uri imageUri = null;
        
        try {
            // Luôn sử dụng FileProvider để tạo URI
            File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile(
                    "StudyMate_" + System.currentTimeMillis(),
                    ".jpg",
                    storageDir
            );
            
            String authority = context.getPackageName() + ".fileprovider";
            imageUri = FileProvider.getUriForFile(context, authority, imageFile);
            
            if (imageUri != null) {
                // Tạo intent chụp ảnh
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                
                // Cấp quyền cho camera app
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                // Log để debug
                android.util.Log.d("ImageCaptureUtils", "Created image URI: " + imageUri);
                
                // Khởi chạy camera
                cameraLauncher.launch(intent);
            } else {
                throw new IOException("Failed to create image URI");
            }
            
        } catch (IOException e) {
            android.util.Log.e("ImageCaptureUtils", "Error creating image file: " + e.getMessage());
        } catch (Exception e) {
            android.util.Log.e("ImageCaptureUtils", "Error launching camera: " + e.getMessage());
        }
        
        return imageUri;
    }
    
    /**
     * Handle permission result
     * @param activity Activity that received the permission result
     * @param requestCode Request code
     * @param grantResults Grant results
     * @return true if permission was granted, false otherwise
     */
    public static boolean handlePermissionResult(Activity activity, int requestCode, @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            boolean permissionGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = false;
                    break;
                }
            }
            return permissionGranted;
        } else {
            return false;
        }
    }
}

package hcmute.edu.vn.thongvavan.finalproject.repository;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import hcmute.edu.vn.thongvavan.finalproject.utils.ImageCaptureUtils;

/**
 * Repository for image capture operations
 */
public class ImageRepository {
    private final Context context;
    private final MutableLiveData<Uri> imageUriLiveData = new MutableLiveData<>();
    
    public ImageRepository(Context context) {
        this.context = context;
    }
    
    /**
     * Get the current image URI
     * @return LiveData containing the image URI
     */
    public LiveData<Uri> getImageUri() {
        return imageUriLiveData;
    }
    
    /**
     * Set the image URI
     * @param uri Image URI
     */
    public void setImageUri(Uri uri) {
        imageUriLiveData.setValue(uri);
    }
    
    /**
     * Check if camera permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    public boolean checkCameraPermissions() {
        return ImageCaptureUtils.checkCameraPermissions(context);
    }
    
    /**
     * Request camera permissions
     * @param activity Activity to request permissions from
     */
    public void requestCameraPermissions(Activity activity) {
        ImageCaptureUtils.requestCameraPermissions(activity);
    }
    
    /**
     * Check if storage permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    public boolean checkStoragePermission() {
        return ImageCaptureUtils.checkStoragePermission(context);
    }
    
    /**
     * Request storage permissions
     * @param activity Activity to request permissions from
     */
    public void requestStoragePermission(Activity activity) {
        ImageCaptureUtils.requestStoragePermission(activity);
    }
    
    /**
     * Pick image from gallery
     * @param galleryLauncher ActivityResultLauncher for gallery
     */
    public void pickImageFromGallery(ActivityResultLauncher<android.content.Intent> galleryLauncher) {
        ImageCaptureUtils.pickImageFromGallery(galleryLauncher);
    }
    
    /**
     * Pick image from camera
     * @param cameraLauncher ActivityResultLauncher for camera
     * @return URI of the image file
     */
    public Uri pickImageFromCamera(ActivityResultLauncher<android.content.Intent> cameraLauncher) {
        // Lấy URI ảnh từ ImageCaptureUtils
        Uri imageUri = ImageCaptureUtils.pickImageFromCamera(context, cameraLauncher);
        
        // Ghi log để debug
        android.util.Log.d("ImageRepository", "Image URI from camera: " + imageUri);
        
        // Lưu URI vào LiveData ngay lập tức để MainActivity có thể sử dụng
        imageUriLiveData.setValue(imageUri);
        
        return imageUri;
    }
    
    /**
     * Handle permission result
     * @param activity Activity that received the permission result
     * @param requestCode Request code
     * @param grantResults Grant results
     * @return true if permission was granted, false otherwise
     */
    public boolean handlePermissionResult(Activity activity, int requestCode, int[] grantResults) {
        return ImageCaptureUtils.handlePermissionResult(activity, requestCode, grantResults);
    }
}

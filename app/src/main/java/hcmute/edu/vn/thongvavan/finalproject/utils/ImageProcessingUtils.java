package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tiện ích xử lý ảnh
 * Cung cấp các phương thức để xử lý ảnh như tải bitmap, xoay ảnh theo thông tin EXIF, v.v.
 */
public class ImageProcessingUtils {
    private static final String TAG = "ImageProcessingUtils";

    /**
     * Tải bitmap từ URI và sửa hướng dựa trên thông tin EXIF
     * @param context Context để truy cập ContentResolver
     * @param imageUri URI của ảnh
     * @return Bitmap đã được sửa hướng hoặc null nếu có lỗi
     */
    public static Bitmap loadBitmapWithCorrectOrientation(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            Log.e(TAG, "Context or imageUri is null");
            return null;
        }

        ContentResolver contentResolver = context.getContentResolver();
        try {
            // Đọc bitmap từ URI
            InputStream inputStream = contentResolver.openInputStream(imageUri);
            if (inputStream == null) return null;
            
            // Tạo bitmap từ input stream
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            // Đọc thông tin EXIF để xác định hướng
            ExifInterface exif = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Với Android 7.0 trở lên, có thể đọc EXIF từ URI
                    inputStream = contentResolver.openInputStream(imageUri);
                    if (inputStream != null) {
                        exif = new ExifInterface(inputStream);
                        inputStream.close();
                    }
                } else {
                    // Với Android cũ hơn, cần chuyển URI thành đường dẫn file
                    String[] projection = {MediaStore.Images.Media.DATA};
                    Cursor cursor = contentResolver.query(imageUri, projection, null, null, null);
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
        } catch (IOException e) {
            Log.e(TAG, "IO error: " + e.getMessage());
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error processing bitmap: " + e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm tra xem URI có hợp lệ và có thể truy cập được không
     * @param context Context để truy cập ContentResolver
     * @param imageUri URI cần kiểm tra
     * @return true nếu URI hợp lệ và có thể truy cập, false nếu ngược lại
     */
    public static boolean isValidImageUri(Context context, Uri imageUri) {
        if (context == null || imageUri == null) {
            return false;
        }

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                return false;
            }
            inputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error validating image URI: " + e.getMessage());
            return false;
        }
    }
}

package hcmute.edu.vn.thongvavan.finalproject.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import hcmute.edu.vn.thongvavan.finalproject.model.RecognizedText;
import hcmute.edu.vn.thongvavan.finalproject.utils.TextRecognitionUtils;

/**
 * Repository cho các hoạt động nhận diện văn bản
 */
public class TextRecognitionRepository {
    private static final String TAG = "TextRecognitionRepo";
    
    private final Context context;
    
    // LiveData cho trạng thái loading
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> loadingMessage = new MutableLiveData<>("");
    
    public TextRecognitionRepository(Context context) {
        this.context = context;
    }
    
    /**
     * Lấy trạng thái loading
     * @return LiveData chứa trạng thái loading
     */
    public LiveData<Boolean> isLoading() {
        return isLoading;
    }
    
    /**
     * Lấy thông báo loading
     * @return LiveData chứa thông báo loading
     */
    public LiveData<String> getLoadingMessage() {
        return loadingMessage;
    }
    
    /**
     * Nhận diện văn bản từ URI ảnh
     * @param imageUri URI của ảnh cần nhận diện văn bản
     * @return LiveData chứa kết quả văn bản đã nhận diện
     */
    public LiveData<RecognizedText> recognizeTextFromImage(Uri imageUri) {
        MutableLiveData<RecognizedText> resultLiveData = new MutableLiveData<>();
        
        // Cập nhật trạng thái loading
        isLoading.postValue(true);
        loadingMessage.postValue("Đang chuẩn bị ảnh...");
        
        // Sử dụng TextRecognitionUtils để nhận diện văn bản
        TextRecognitionUtils.recognizeTextFromImage(context, imageUri, new TextRecognitionUtils.TextRecognitionListener() {
            @Override
            public void onSuccess(RecognizedText recognizedText) {
                // Cập nhật trạng thái loading
                isLoading.postValue(false);
                
                // Gửi kết quả
                resultLiveData.postValue(recognizedText);
            }
            
            @Override
            public void onError(String errorMessage) {
                // Cập nhật trạng thái loading
                isLoading.postValue(false);
                Log.e(TAG, "onError: " + errorMessage);
                
                // Tạo và gửi kết quả lỗi
                resultLiveData.postValue(new RecognizedText(errorMessage));
            }
        });
        
        return resultLiveData;
    }
    
    /**
     * Giải phóng tài nguyên khi không còn cần thiết
     */
    public void shutdown() {
        // Không cần đóng TextRecognizer vì nó được đóng trong TextRecognitionUtils
    }
}

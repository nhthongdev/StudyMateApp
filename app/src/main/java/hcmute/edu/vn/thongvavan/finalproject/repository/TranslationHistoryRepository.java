package hcmute.edu.vn.thongvavan.finalproject.repository;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.thongvavan.finalproject.model.TranslationHistoryItem;

/**
 * Repository để quản lý lịch sử dịch
 */
public class TranslationHistoryRepository {
    private static final String TAG = "TranslationHistoryRepo";
    private static final String PREF_NAME = "translation_history_pref";
    private static final String KEY_HISTORY = "translation_history";
    
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private final MutableLiveData<List<TranslationHistoryItem>> historyItemsLiveData = new MutableLiveData<>();
    
    public TranslationHistoryRepository(Application application) {
        sharedPreferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadHistoryItems();
    }
    
    /**
     * Lấy danh sách các mục lịch sử dịch
     * @return LiveData chứa danh sách các mục lịch sử dịch
     */
    public LiveData<List<TranslationHistoryItem>> getHistoryItems() {
        return historyItemsLiveData;
    }
    
    /**
     * Tìm kiếm các mục lịch sử dịch theo từ khóa
     * @param query Từ khóa tìm kiếm
     * @return Danh sách các mục lịch sử dịch phù hợp với từ khóa
     */
    public List<TranslationHistoryItem> searchHistoryItems(String query) {
        if (query == null || query.isEmpty()) {
            return historyItemsLiveData.getValue();
        }
        
        List<TranslationHistoryItem> result = new ArrayList<>();
        List<TranslationHistoryItem> items = historyItemsLiveData.getValue();
        
        if (items != null) {
            String lowerCaseQuery = query.toLowerCase();
            for (TranslationHistoryItem item : items) {
                if (item.getSourceText().toLowerCase().contains(lowerCaseQuery) ||
                    item.getTranslatedText().toLowerCase().contains(lowerCaseQuery)) {
                    result.add(item);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Thêm một mục lịch sử dịch mới
     * @param sourceText Văn bản nguồn
     * @param translatedText Văn bản đã dịch
     * @param sourceLanguageCode Mã ngôn ngữ nguồn
     * @param targetLanguageCode Mã ngôn ngữ đích
     * @param imageUri URI của hình ảnh (có thể null)
     */
    public void addHistoryItem(String sourceText, String translatedText, 
                              String sourceLanguageCode, String targetLanguageCode,
                              String sourceLanguageName, String targetLanguageName,
                              Uri imageUri) {
        // Tạo mục lịch sử mới
        TranslationHistoryItem newItem = new TranslationHistoryItem(
                sourceText,
                translatedText,
                sourceLanguageCode,
                targetLanguageCode,
                sourceLanguageName,
                targetLanguageName,
                System.currentTimeMillis(),
                imageUri != null ? imageUri.toString() : null
        );
        
        // Lấy danh sách hiện tại
        List<TranslationHistoryItem> currentItems = historyItemsLiveData.getValue();
        if (currentItems == null) {
            currentItems = new ArrayList<>();
        }
        
        // Thêm mục mới vào đầu danh sách
        currentItems.add(0, newItem);
        
        // Cập nhật LiveData
        historyItemsLiveData.setValue(currentItems);
        
        // Lưu vào SharedPreferences
        saveHistoryItems();
    }
    
    /**
     * Xóa một mục lịch sử dịch
     * @param item Mục lịch sử cần xóa
     */
    public void deleteHistoryItem(TranslationHistoryItem item) {
        List<TranslationHistoryItem> currentItems = historyItemsLiveData.getValue();
        if (currentItems != null && currentItems.remove(item)) {
            historyItemsLiveData.setValue(currentItems);
            saveHistoryItems();
        }
    }
    
    /**
     * Xóa toàn bộ lịch sử dịch
     */
    public void clearHistory() {
        historyItemsLiveData.setValue(new ArrayList<>());
        saveHistoryItems();
    }
    
    /**
     * Tải danh sách các mục lịch sử dịch từ SharedPreferences
     */
    private void loadHistoryItems() {
        String json = sharedPreferences.getString(KEY_HISTORY, null);
        if (json != null) {
            try {
                Type type = new TypeToken<ArrayList<TranslationHistoryItem>>() {}.getType();
                List<TranslationHistoryItem> items = gson.fromJson(json, type);
                historyItemsLiveData.setValue(items != null ? items : new ArrayList<>());
            } catch (Exception e) {
                Log.e(TAG, "Error loading history items: " + e.getMessage());
                historyItemsLiveData.setValue(new ArrayList<>());
            }
        } else {
            historyItemsLiveData.setValue(new ArrayList<>());
        }
    }
    
    /**
     * Lưu danh sách các mục lịch sử dịch vào SharedPreferences
     */
    private void saveHistoryItems() {
        List<TranslationHistoryItem> items = historyItemsLiveData.getValue();
        if (items != null) {
            try {
                String json = gson.toJson(items);
                sharedPreferences.edit().putString(KEY_HISTORY, json).apply();
            } catch (Exception e) {
                Log.e(TAG, "Error saving history items: " + e.getMessage());
            }
        }
    }
}

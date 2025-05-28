package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.thongvavan.finalproject.model.ModelLanguage;

/**
 * Lớp tiện ích xử lý việc hiển thị và chọn ngôn ngữ
 */
public class LanguageUIUtils {
    
    /**
     * Thiết lập spinner chọn ngôn ngữ
     * @param context Context
     * @param targetLanguageSpinner AutoCompleteTextView hiển thị spinner
     * @param availableLanguages Danh sách ngôn ngữ có sẵn
     * @param targetLanguageLayout TextInputLayout chứa spinner
     * @param targetLanguageDisplay TextView hiển thị ngôn ngữ đã chọn
     * @param translationControlsLayout Layout chứa các điều khiển dịch
     * @param onLanguageSelectedListener Listener khi ngôn ngữ được chọn
     */
    public static void setupLanguageSpinner(
            Context context,
            AutoCompleteTextView targetLanguageSpinner,
            List<ModelLanguage> availableLanguages,
            TextInputLayout targetLanguageLayout,
            TextView targetLanguageDisplay,
            View translationControlsLayout,
            OnLanguageSelectedListener onLanguageSelectedListener) {
        
        // Tạo adapter cho spinner ngôn ngữ
        ArrayAdapter<ModelLanguage> adapter = new ArrayAdapter<ModelLanguage>(
                context, android.R.layout.simple_dropdown_item_1line, availableLanguages) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                textView.setText(getItem(position).getLanguageTitle());
                return textView;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getDropDownView(position, convertView, parent);
                textView.setText(getItem(position).getLanguageTitle());
                return textView;
            }
        };

        // Thiết lập adapter cho spinner
        targetLanguageSpinner.setAdapter(adapter);

        // Thiết lập listener khi chọn item
        targetLanguageSpinner.setOnItemClickListener((parent, view, position, id) -> {
            // Lấy ngôn ngữ đã chọn
            ModelLanguage selectedLanguage = availableLanguages.get(position);
            
            // Cập nhật hiển thị ngôn ngữ đích
            targetLanguageDisplay.setText(selectedLanguage.getLanguageTitle());
            
            // Ẩn layout dropdown sau khi chọn
            targetLanguageLayout.setVisibility(View.GONE);
            
            // Hiển thị điều khiển dịch
            translationControlsLayout.setVisibility(View.VISIBLE);
            
            // Gọi callback
            if (onLanguageSelectedListener != null) {
                onLanguageSelectedListener.onLanguageSelected(selectedLanguage);
            }
        });
    }
    
    /**
     * Tạo danh sách ngôn ngữ với tên hiển thị đầy đủ
     * @param languageCodes Danh sách mã ngôn ngữ
     * @return Danh sách ModelLanguage chứa mã ngôn ngữ và tên hiển thị
     */
    public static List<ModelLanguage> createLanguageList(List<String> languageCodes) {
        List<ModelLanguage> languageList = new java.util.ArrayList<>();
        
        for (String code : languageCodes) {
            Locale locale = new Locale(code);
            String displayName = locale.getDisplayLanguage();
            
            // Viết hoa chữ cái đầu tiên
            if (displayName.length() > 0) {
                displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
            }
            
            languageList.add(new ModelLanguage(code, displayName));
        }
        
        return languageList;
    }
    
    /**
     * Cập nhật hiển thị ngôn ngữ nguồn
     * @param sourceLanguageTv TextView hiển thị ngôn ngữ nguồn
     * @param languageCode Mã ngôn ngữ
     * @param availableLanguages Danh sách ngôn ngữ có sẵn
     * @return Tên hiển thị của ngôn ngữ
     */
    public static String updateSourceLanguageDisplay(
            TextView sourceLanguageTv,
            String languageCode,
            List<ModelLanguage> availableLanguages) {
        
        String languageName = "";
        
        // Tìm tên hiển thị của ngôn ngữ từ mã ngôn ngữ
        for (ModelLanguage lang : availableLanguages) {
            if (lang.getLanguageCode().equals(languageCode)) {
                languageName = lang.getLanguageTitle();
                sourceLanguageTv.setText(languageName);
                break;
            }
        }
        
        return languageName;
    }
    
    /**
     * Cập nhật hiển thị ngôn ngữ đích
     * @param targetLanguageDisplay TextView hiển thị ngôn ngữ đích
     * @param languageCode Mã ngôn ngữ
     * @param availableLanguages Danh sách ngôn ngữ có sẵn
     * @return ModelLanguage đã chọn
     */
    public static ModelLanguage updateTargetLanguageDisplay(
            TextView targetLanguageDisplay,
            String languageCode,
            List<ModelLanguage> availableLanguages) {
        
        ModelLanguage selectedLanguage = null;
        
        // Tìm ngôn ngữ từ mã ngôn ngữ
        for (ModelLanguage lang : availableLanguages) {
            if (lang.getLanguageCode().equals(languageCode)) {
                selectedLanguage = lang;
                targetLanguageDisplay.setText(lang.getLanguageTitle());
                break;
            }
        }
        
        return selectedLanguage;
    }
    
    /**
     * Interface lắng nghe sự kiện khi ngôn ngữ được chọn
     */
    public interface OnLanguageSelectedListener {
        void onLanguageSelected(ModelLanguage selectedLanguage);
    }
}

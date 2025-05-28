package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Lớp tiện ích xử lý Clipboard và chia sẻ văn bản
 */
public class ClipboardUtils {
    
    /**
     * Sao chép văn bản vào clipboard
     * @param context Context để truy cập ClipboardManager
     * @param text Văn bản cần sao chép
     * @param label Nhãn cho dữ liệu clipboard
     */
    public static void copyToClipboard(Context context, String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }
    
    /**
     * Sao chép văn bản vào clipboard và hiển thị thông báo
     * @param context Context để truy cập ClipboardManager
     * @param text Văn bản cần sao chép
     * @param label Nhãn cho dữ liệu clipboard
     * @param toastMessage Thông báo hiển thị sau khi sao chép
     */
    public static void copyToClipboardWithToast(Context context, String text, String label, String toastMessage) {
        copyToClipboard(context, text, label);
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Chia sẻ văn bản
     * @param context Context để khởi tạo Intent
     * @param text Văn bản cần chia sẻ
     * @param chooserTitle Tiêu đề cho dialog chọn ứng dụng
     */
    public static void shareText(Context context, String text, String chooserTitle) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle));
    }
    
    /**
     * Lấy văn bản từ clipboard
     * @param context Context để truy cập ClipboardManager
     * @return Văn bản từ clipboard hoặc null nếu không có
     */
    public static String getTextFromClipboard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText() != null ? item.getText().toString() : null;
        }
        return null;
    }
    
    /**
     * Kiểm tra xem clipboard có văn bản không
     * @param context Context để truy cập ClipboardManager
     * @return true nếu clipboard có văn bản, false nếu không
     */
    public static boolean hasText(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        return clipboard.hasPrimaryClip() && 
               clipboard.getPrimaryClip() != null && 
               clipboard.getPrimaryClip().getItemCount() > 0 &&
               clipboard.getPrimaryClip().getItemAt(0).getText() != null;
    }
}

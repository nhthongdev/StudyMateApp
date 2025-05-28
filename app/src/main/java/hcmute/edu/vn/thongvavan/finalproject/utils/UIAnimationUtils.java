package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.view.View;
import android.widget.TextView;

/**
 * Lớp tiện ích xử lý các hiệu ứng animation trong giao diện người dùng
 */
public class UIAnimationUtils {
    
    /**
     * Hiển thị hiệu ứng loading với thông báo
     * @param loadingLayout Layout hiển thị loading
     * @param loadingMessageTv TextView hiển thị thông báo
     * @param message Thông báo hiển thị
     */
    public static void showLoadingEffect(View loadingLayout, TextView loadingMessageTv, String message) {
        if (loadingLayout == null || loadingMessageTv == null) {
            return;
        }
        
        loadingMessageTv.setText(message);
        if (loadingLayout.getVisibility() != View.VISIBLE) {
            loadingLayout.setAlpha(0f);
            loadingLayout.setVisibility(View.VISIBLE);
            loadingLayout.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }
    
    /**
     * Ẩn hiệu ứng loading
     * @param loadingLayout Layout hiển thị loading
     */
    public static void hideLoadingEffect(View loadingLayout) {
        if (loadingLayout == null) {
            return;
        }
        
        if (loadingLayout.getVisibility() == View.VISIBLE) {
            loadingLayout.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> loadingLayout.setVisibility(View.GONE))
                    .start();
        }
    }
    
    /**
     * Hiển thị view với hiệu ứng fade in
     * @param view View cần hiển thị
     * @param duration Thời gian hiệu ứng (ms)
     */
    public static void fadeIn(View view, int duration) {
        if (view == null) {
            return;
        }
        
        view.setAlpha(0f);
        view.setVisibility(View.VISIBLE);
        view.animate()
                .alpha(1f)
                .setDuration(duration)
                .start();
    }
    
    /**
     * Ẩn view với hiệu ứng fade out
     * @param view View cần ẩn
     * @param duration Thời gian hiệu ứng (ms)
     */
    public static void fadeOut(View view, int duration) {
        if (view == null) {
            return;
        }
        
        view.animate()
                .alpha(0f)
                .setDuration(duration)
                .withEndAction(() -> view.setVisibility(View.GONE))
                .start();
    }
}

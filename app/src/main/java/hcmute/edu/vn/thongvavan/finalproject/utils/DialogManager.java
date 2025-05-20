package hcmute.edu.vn.thongvavan.finalproject.utils;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import hcmute.edu.vn.thongvavan.finalproject.R;

/**
 * Manager class for handling dialogs and popup menus
 */
public class DialogManager {
    
    private final Context context;
    
    public interface ImageSourceCallback {
        void onCameraSelected();
        void onGallerySelected();
    }
    
    public DialogManager(Context context) {
        this.context = context;
    }
    
    /**
     * Show input image dialog to select image source (camera or gallery)
     * @param anchorView View to anchor the popup menu to
     * @param callback Callback to handle user selection
     */
    public void showInputImageDialog(View anchorView, final ImageSourceCallback callback) {
        // Initialize popup menu
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        
        // Add menu items
        popupMenu.getMenu().add(1, 1, 1, "Camera");
        popupMenu.getMenu().add(1, 2, 2, "Gallery");
        
        // Show popup menu
        popupMenu.show();
        
        // Handle menu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                
                if (id == 1) {
                    // Camera clicked
                    if (callback != null) {
                        callback.onCameraSelected();
                    }
                    return true;
                } else if (id == 2) {
                    // Gallery clicked
                    if (callback != null) {
                        callback.onGallerySelected();
                    }
                    return true;
                }
                
                return false;
            }
        });
    }
    
    /**
     * Show a toast message
     * @param message Message to show
     * @param duration Duration of the toast
     */
    public void showToast(String message, int duration) {
        Toast.makeText(context, message, duration).show();
    }
}

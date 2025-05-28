package hcmute.edu.vn.thongvavan.finalproject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.thongvavan.finalproject.R;
import hcmute.edu.vn.thongvavan.finalproject.model.TranslationHistoryItem;

/**
 * Adapter để hiển thị các mục lịch sử dịch trong RecyclerView
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
    
    // Danh sách các mục lịch sử dịch
    private List<TranslationHistoryItem> historyItems = new ArrayList<>();
    private final Context context;
    private final OnHistoryItemClickListener listener;
    
    /**
     * Interface để xử lý các sự kiện click trên mục lịch sử
     */
    public interface OnHistoryItemClickListener {
        void onCopyClick(TranslationHistoryItem item);
        void onShareClick(TranslationHistoryItem item);
        void onDeleteClick(TranslationHistoryItem item);
        void onItemClick(TranslationHistoryItem item);
    }
    
    /**
     * Constructor cho HistoryAdapter
     * @param context Context của ứng dụng
     * @param listener Listener để xử lý sự kiện click
     */
    public HistoryAdapter(Context context, OnHistoryItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tạo view cho mỗi mục lịch sử từ layout item_translation_history.xml
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_translation_history, parent, false);
        return new HistoryViewHolder(itemView);
    }
    
    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        // Lấy mục lịch sử tại vị trí position
        TranslationHistoryItem currentItem = historyItems.get(position);
        
        // Hiển thị thông tin ngôn ngữ (ví dụ: "Tiếng Việt → English")
        String languages = currentItem.getSourceLanguageName() + " → " + currentItem.getTargetLanguageName();
        holder.languagesTv.setText(languages);
        
        // Hiển thị thời gian dịch
        holder.timestampTv.setText(formatTimestamp(currentItem.getTimestamp()));
        
        // Hiển thị văn bản nguồn và văn bản đã dịch
        holder.sourceTextTv.setText(currentItem.getSourceText());
        holder.translatedTextTv.setText(currentItem.getTranslatedText());
        
        // Thiết lập sự kiện click cho các nút
        holder.copyBtn.setOnClickListener(v -> listener.onCopyClick(currentItem));
        holder.shareBtn.setOnClickListener(v -> listener.onShareClick(currentItem));
        holder.deleteBtn.setOnClickListener(v -> listener.onDeleteClick(currentItem));
        
        // Thiết lập sự kiện click cho toàn bộ mục
        holder.itemView.setOnClickListener(v -> listener.onItemClick(currentItem));
    }
    
    @Override
    public int getItemCount() {
        return historyItems.size();
    }
    
    /**
     * Cập nhật danh sách các mục lịch sử
     * @param historyItems Danh sách mới các mục lịch sử
     */
    public void setHistoryItems(List<TranslationHistoryItem> historyItems) {
        this.historyItems = historyItems;
        notifyDataSetChanged();
    }
    
    /**
     * Định dạng thời gian thành chuỗi dễ đọc
     * @param timestamp Thời gian tính bằng milliseconds
     * @return Chuỗi thời gian đã định dạng (ví dụ: "Hôm nay, 15:30")
     */
    private String formatTimestamp(long timestamp) {
        Date now = new Date();
        Date date = new Date(timestamp);
        
        // Tính khoảng cách thời gian
        long diff = now.getTime() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        // Định dạng thời gian dựa trên khoảng cách
        SimpleDateFormat sdf;
        if (days == 0) {
            // Cùng ngày
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Hôm nay, " + sdf.format(date);
        } else if (days == 1) {
            // Hôm qua
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return "Hôm qua, " + sdf.format(date);
        } else {
            // Các ngày khác
            sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return sdf.format(date);
        }
    }
    
    /**
     * ViewHolder để giữ các view của mỗi mục lịch sử
     */
    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView languagesTv;
        private final TextView timestampTv;
        private final TextView sourceTextTv;
        private final TextView translatedTextTv;
        private final ImageButton copyBtn;
        private final ImageButton shareBtn;
        private final ImageButton deleteBtn;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Tìm các view trong layout item_translation_history.xml
            languagesTv = itemView.findViewById(R.id.languagesTv);
            timestampTv = itemView.findViewById(R.id.timestampTv);
            sourceTextTv = itemView.findViewById(R.id.sourceTextTv);
            translatedTextTv = itemView.findViewById(R.id.translatedTextTv);
            copyBtn = itemView.findViewById(R.id.copyBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }
}

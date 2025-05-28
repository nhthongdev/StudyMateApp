package hcmute.edu.vn.thongvavan.finalproject;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.thongvavan.finalproject.adapter.HistoryAdapter;
import hcmute.edu.vn.thongvavan.finalproject.model.TranslationHistoryItem;
import hcmute.edu.vn.thongvavan.finalproject.viewmodel.MainViewModel;

/**
 * Activity hiển thị lịch sử dịch
 */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnHistoryItemClickListener {
    
    // UI components
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private EditText searchEditText;
    
    // Adapter
    private HistoryAdapter adapter;
    
    // ViewModel
    private MainViewModel viewModel;
    
    // Data
    private List<TranslationHistoryItem> historyItems = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        
        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        
        // Initialize views
        initViews();
        
        // Set up RecyclerView
        setupRecyclerView();
        
        // Observe history data
        observeHistoryData();
        
        // Set up search functionality
        setupSearch();
        
        // Set up click listeners
        setupClickListeners();
    }
    
    /**
     * Initialize views
     */
    private void initViews() {
        recyclerView = findViewById(R.id.historyRecyclerView);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        searchEditText = findViewById(R.id.searchEt);
    }
    
    /**
     * Set up RecyclerView
     */
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this, this);
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * Observe history data from ViewModel
     */
    private void observeHistoryData() {
        viewModel.getHistoryItems().observe(this, items -> {
            historyItems = items;
            updateHistoryList(historyItems);
        });
    }
    
    /**
     * Set up search functionality
     */
    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    updateHistoryList(historyItems);
                } else {
                    List<TranslationHistoryItem> filteredList = viewModel.searchHistoryItems(query);
                    updateHistoryList(filteredList);
                }
            }
        });
    }
    
    /**
     * Set up click listeners
     */
    private void setupClickListeners() {
        ImageView backButton = findViewById(R.id.backBtn);
        ImageView clearHistoryButton = findViewById(R.id.clearHistoryBtn);
        
        backButton.setOnClickListener(v -> finish());
        
        clearHistoryButton.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xóa lịch sử")
                    .setMessage("Bạn có chắc chắn muốn xóa toàn bộ lịch sử dịch?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        viewModel.clearHistory();
                        Toast.makeText(this, "Đã xóa toàn bộ lịch sử", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
    }
    
    /**
     * Update history list
     * @param historyItems List of history items
     */
    private void updateHistoryList(List<TranslationHistoryItem> historyItems) {
        if (historyItems.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
            adapter.setHistoryItems(historyItems);
        }
    }
    
    @Override
    public void onCopyClick(TranslationHistoryItem item) {
        String textToCopy = item.getSourceText() + "\n\n" + item.getTranslatedText();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Translation", textToCopy);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép vào bộ nhớ tạm", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onShareClick(TranslationHistoryItem item) {
        String textToShare = item.getSourceText() + "\n\n" + item.getTranslatedText();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
    }
    
    @Override
    public void onDeleteClick(TranslationHistoryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa mục này?")
                .setMessage("Bạn có chắc chắn muốn xóa mục này khỏi lịch sử?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteHistoryItem(item);
                    Toast.makeText(this, "Đã xóa mục khỏi lịch sử", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    @Override
    public void onItemClick(TranslationHistoryItem item) {
        // Trả về dữ liệu cho MainActivity
        Intent intent = new Intent();
        intent.putExtra("SOURCE_TEXT", item.getSourceText());
        intent.putExtra("TRANSLATED_TEXT", item.getTranslatedText());
        intent.putExtra("SOURCE_LANGUAGE_CODE", item.getSourceLanguageCode());
        intent.putExtra("TARGET_LANGUAGE_CODE", item.getTargetLanguageCode());
        setResult(RESULT_OK, intent);
        finish();
    }
}

package com.example.chatcompanion;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

public class SettingsActivity extends Activity {
    
    private TextView modelNameText;
    private TextView modelSizeText;
    private TextView modelStatusText;
    private TextView downloadProgressText;
    private ProgressBar downloadProgressBar;
    private Button downloadButton;
    private Button deleteButton;
    private Button backButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Initialize UI components
        modelNameText = findViewById(R.id.modelNameText);
        modelSizeText = findViewById(R.id.modelSizeText);
        modelStatusText = findViewById(R.id.modelStatusText);
        downloadProgressText = findViewById(R.id.downloadProgressText);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        downloadButton = findViewById(R.id.downloadButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        
        // Set model info
        modelNameText.setText("Mistral 7B Instruct v0.2");
        modelSizeText.setText("Size: 4.1 GB");
        
        // Update UI state
        updateModelStatus();
        
        // Set up button listeners
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadModel();
            }
        });
        
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteModel();
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    
    private void updateModelStatus() {
        if (ModelManager.modelExists(this)) {
            long size = ModelManager.getModelSize(this);
            boolean isComplete = ModelManager.isModelComplete(this);
            
            if (isComplete) {
                modelStatusText.setText("✅ Model Ready");
                modelStatusText.setTextColor(0xFF4CAF50); // Green
                downloadButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                downloadProgressBar.setVisibility(View.GONE);
                downloadProgressText.setVisibility(View.GONE);
            } else {
                modelStatusText.setText("⬇️ Downloading... (" + formatFileSize(size) + ")");
                modelStatusText.setTextColor(0xFFFF9800); // Orange
                downloadButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);
                
                // Update progress
                int progress = (int) ((size * 100) / 4370911744L); // Total size
                downloadProgressBar.setProgress(progress);
                downloadProgressText.setText(progress + "%");
            }
        } else {
            modelStatusText.setText("❌ Model Not Downloaded");
            modelStatusText.setTextColor(0xFFF44336); // Red
            downloadButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.GONE);
            downloadProgressBar.setVisibility(View.GONE);
            downloadProgressText.setVisibility(View.GONE);
        }
    }
    
    private void downloadModel() {
        long existingSize = ModelManager.getModelSize(this);
        int initialProgress = (int) ((existingSize * 100) / 4370911744L);
        
        modelStatusText.setText(existingSize > 0 ? "⬇️ Resuming Download..." : "⬇️ Starting Download...");
        downloadButton.setVisibility(View.GONE);
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressText.setVisibility(View.VISIBLE);
        downloadProgressBar.setProgress(initialProgress);
        downloadProgressText.setText(initialProgress + "%");
        
        ModelDownloader.DownloadProgress progress = new ModelDownloader.DownloadProgress() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    downloadProgressBar.setProgress(percent);
                    downloadProgressText.setText(percent + "%");
                    modelStatusText.setText("⬇️ Downloading... (" + percent + "%)");
                });
            }
            
            @Override
            public void onComplete(java.io.File modelFile) {
                runOnUiThread(() -> {
                    updateModelStatus();
                    Toast.makeText(SettingsActivity.this, "Model downloaded successfully!", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    modelStatusText.setText("❌ Download Failed");
                    modelStatusText.setTextColor(0xFFF44336); // Red
                    downloadButton.setVisibility(View.VISIBLE);
                    downloadProgressBar.setVisibility(View.GONE);
                    downloadProgressText.setVisibility(View.GONE);
                    Toast.makeText(SettingsActivity.this, "Download failed: " + error, Toast.LENGTH_LONG).show();
                });
            }
        };
        
        ModelDownloader.downloadModel(this, progress);
    }
    
    private void deleteModel() {
        ModelManager.deleteModel(this);
        updateModelStatus();
        Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

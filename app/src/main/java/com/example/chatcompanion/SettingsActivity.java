package com.example.chatcompanion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    
    private TextView modelNameText;
    private TextView modelSizeText;
    private TextView modelStatusText;
    private TextView downloadProgressText;
    private TextView versionText;
    private TextView updateStatusText;
    private TextView currentModelText;
    private TextView backupStatusText;
    private ProgressBar downloadProgressBar;
    private Button downloadButton;
    private Button deleteButton;
    private Button backButton;
    private Button checkUpdateButton;
    private Button backupButton;
    private Button restoreButton;
    private Spinner modelSpinner;
    
    private int selectedModelIndex = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Initialize all UI components
        modelNameText = findViewById(R.id.modelNameText);
        modelSizeText = findViewById(R.id.modelSizeText);
        modelStatusText = findViewById(R.id.modelStatusText);
        downloadProgressText = findViewById(R.id.downloadProgressText);
        downloadProgressBar = findViewById(R.id.downloadProgressBar);
        downloadButton = findViewById(R.id.downloadButton);
        deleteButton = findViewById(R.id.deleteButton);
        backButton = findViewById(R.id.backButton);
        versionText = findViewById(R.id.versionText);
        updateStatusText = findViewById(R.id.updateStatusText);
        checkUpdateButton = findViewById(R.id.checkUpdateButton);
        currentModelText = findViewById(R.id.currentModelText);
        backupStatusText = findViewById(R.id.backupStatusText);
        
        try {
            backupButton = findViewById(R.id.backupButton);
            restoreButton = findViewById(R.id.restoreButton);
            modelSpinner = findViewById(R.id.modelSpinner);
        } catch (Exception e) {
            // These views might not exist in older layouts
        }
        
        // Load saved model preference
        selectedModelIndex = getSharedPreferences("settings", MODE_PRIVATE).getInt("selected_model", 0);
        
        // Set up model spinner if available
        if (modelSpinner != null) {
            String[] modelNames = ModelDownloader.getModelNames();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, modelNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            modelSpinner.setAdapter(adapter);
            modelSpinner.setSelection(selectedModelIndex);
            modelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != selectedModelIndex) {
                        selectedModelIndex = position;
                        getSharedPreferences("settings", MODE_PRIVATE)
                            .edit().putInt("selected_model", position).apply();
                        updateModelStatus();
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
        
        // Set version info
        versionText.setText("Current version: " + UpdateManager.getCurrentVersion());
        
        // Update model display
        updateModelDisplay();
        
        // Set up button listeners
        downloadButton.setOnClickListener(v -> downloadModel());
        deleteButton.setOnClickListener(v -> deleteModel());
        backButton.setOnClickListener(v -> finish());
        checkUpdateButton.setOnClickListener(v -> checkForUpdates());
        
        // Backup/Restore buttons
        if (backupButton != null) {
            backupButton.setOnClickListener(v -> createBackup());
        }
        if (restoreButton != null) {
            restoreButton.setOnClickListener(v -> restoreBackup());
        }
        
        updateModelStatus();
    }
    
    private void updateModelDisplay() {
        String[] modelNames = ModelDownloader.getModelNames();
        if (selectedModelIndex < modelNames.length) {
            String name = modelNames[selectedModelIndex];
            if (currentModelText != null) {
                currentModelText.setText("Selected: " + name);
            }
            modelNameText.setText(name.split("\\(")[0].trim());
        }
    }
    
    private void updateModelStatus() {
        updateModelDisplay();
        
        File modelFile = ModelDownloader.getModelFile(this, selectedModelIndex);
        
        if (modelFile.exists()) {
            long size = modelFile.length();
            long expectedSize = ModelDownloader.getExpectedSize(selectedModelIndex);
            boolean isComplete = size >= expectedSize - 1000000;
            
            if (isComplete) {
                modelStatusText.setText("✅ Downloaded");
                modelStatusText.setTextColor(0xFF4CAF50);
                downloadButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                downloadProgressBar.setVisibility(View.GONE);
                downloadProgressText.setVisibility(View.GONE);
            } else {
                long modelSize = ModelManager.getCurrentModelSizeInBytes(this);
                int percent = (int) ((size * 100) / modelSize);
                modelStatusText.setText("⬇️ " + percent + "% downloaded");
                modelStatusText.setTextColor(0xFFFF9800);
                downloadProgressBar.setVisibility(View.VISIBLE);
                downloadProgressText.setVisibility(View.VISIBLE);
                downloadProgressBar.setProgress(percent);
                downloadProgressText.setText(percent + "%");
            }
        } else {
            modelStatusText.setText("❌ Not downloaded");
            modelStatusText.setTextColor(0xFFF44336);
            downloadButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.GONE);
            downloadProgressBar.setVisibility(View.GONE);
            downloadProgressText.setVisibility(View.GONE);
        }
    }
    
    private void downloadModel() {
        modelStatusText.setText("⬇️ Downloading model...");
        downloadButton.setVisibility(View.GONE);
        downloadProgressBar.setVisibility(View.VISIBLE);
        downloadProgressText.setVisibility(View.VISIBLE);
        
        ModelDownloader.DownloadProgress progress = new ModelDownloader.DownloadProgress() {
            @Override
            public void onProgress(int percent) {
                runOnUiThread(() -> {
                    downloadProgressBar.setProgress(percent);
                    downloadProgressText.setText(percent + "%");
                    modelStatusText.setText("⬇️ Downloading... " + percent + "%");
                });
            }
            
            @Override
            public void onComplete(java.io.File modelFile) {
                runOnUiThread(() -> {
                    // Set this as the current model to use
                    ModelManager.setCurrentModelIndex(SettingsActivity.this, selectedModelIndex);
                    Toast.makeText(SettingsActivity.this, "Model ready!", Toast.LENGTH_SHORT).show();
                    updateModelStatus();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    modelStatusText.setText("❌ Error: " + error);
                    modelStatusText.setTextColor(0xFFF44336);
                    downloadButton.setVisibility(View.VISIBLE);
                    downloadProgressBar.setVisibility(View.GONE);
                    downloadProgressText.setVisibility(View.GONE);
                });
            }
        };
        
        ModelDownloader.downloadModel(this, selectedModelIndex, progress);
    }
    
    private void deleteModel() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Model")
            .setMessage("Delete this model? You can re-download later.")
            .setPositiveButton("Delete", (d, w) -> {
                ModelManager.deleteModel(this);
                updateModelStatus();
                Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void createBackup() {
        if (backupStatusText != null) {
            backupStatusText.setText("Creating backup...");
        }
        
        new Thread(() -> {
            String timestamp = java.text.SimpleDateFormat.getDateTimeInstance()
                .format(new java.util.Date()).replace(" ", "_").replace(":", "-");
            boolean success = ModelManager.createBackup(this, "models_" + timestamp);
            
            runOnUiThread(() -> {
                if (success) {
                    backupStatusText.setText("✅ Backup saved!");
                    Toast.makeText(this, "Backup created!", Toast.LENGTH_SHORT).show();
                } else {
                    backupStatusText.setText("❌ Backup failed");
                }
            });
        }).start();
    }
    
    private void restoreBackup() {
        java.io.File[] backups = ModelManager.listBackups(this);
        
        if (backups == null || backups.length == 0) {
            Toast.makeText(this, "No backups found", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] backupNames = new String[backups.length];
        for (int i = 0; i < backups.length; i++) {
            backupNames[i] = backups[i].getName();
        }
        
        new AlertDialog.Builder(this)
            .setTitle("Restore Backup")
            .setItems(backupNames, (d, which) -> {
                if (backupStatusText != null) {
                    backupStatusText.setText("Restoring...");
                }
                final String name = backupNames[which].replace(".zip", "");
                
                new Thread(() -> {
                    boolean success = ModelManager.restoreBackup(this, name);
                    runOnUiThread(() -> {
                        if (success) {
                            backupStatusText.setText("✅ Restored!");
                            updateModelStatus();
                            Toast.makeText(this, "Restored!", Toast.LENGTH_SHORT).show();
                        } else {
                            backupStatusText.setText("❌ Restore failed");
                        }
                    });
                }).start();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void checkForUpdates() {
        updateStatusText.setText("Checking...");
        
        UpdateManager.checkForUpdates(this, new UpdateManager.UpdateCheckListener() {
            @Override
            public void onUpdateAvailable(String version, String url) {
                runOnUiThread(() -> {
                    updateStatusText.setText("Update: v" + version);
                    updateStatusText.setTextColor(0xFF4CAF50);
                    
                    new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Update Available")
                        .setMessage("Version " + version + " available.\n\nNote: Samsung may show \"Untrusted app\" warning. This is normal - tap \"Install anyway\" or go to Settings > Security to allow.")
                        .setPositiveButton("Download", (d, w) -> {
                            UpdateManager.downloadAndInstall(SettingsActivity.this, url);
                        })
                        .setNegativeButton("Later", null)
                        .show();
                });
            }
            
            @Override
            public void onUpdateNotAvailable() {
                runOnUiThread(() -> updateStatusText.setText("Up to date"));
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    updateStatusText.setText("Error: " + error);
                    updateStatusText.setTextColor(0xFFF44336);
                });
            }
        });
    }
}

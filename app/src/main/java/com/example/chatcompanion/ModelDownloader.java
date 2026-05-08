package com.example.chatcompanion;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class ModelDownloader {
    private static final String TAG = "ModelDownloader";
    private static final String MODEL_URL = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final String MODEL_FILE_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    
    public interface DownloadProgress {
        void onProgress(int percent);
        void onComplete(File modelFile);
        void onError(String error);
    }
    
    public static CompletableFuture<File> downloadModel(Context context, DownloadProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                File modelFile = new File(context.getFilesDir(), MODEL_FILE_NAME);
                
                // Check if model already exists
                if (modelFile.exists() && modelFile.length() > 1400000000L) { // ~1.4GB
                    Log.d(TAG, "Model already exists");
                    if (progress != null) progress.onComplete(modelFile);
                    return modelFile;
                }
                
                Log.d(TAG, "Starting model download...");
                URL url = new URL(MODEL_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                
                int fileLength = connection.getContentLength();
                InputStream input = connection.getInputStream();
                FileOutputStream output = new FileOutputStream(modelFile);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (progress != null && fileLength > 0) {
                        int percent = (int) ((totalBytesRead * 100) / fileLength);
                        progress.onProgress(percent);
                    }
                }
                
                output.flush();
                output.close();
                input.close();
                connection.disconnect();
                
                Log.d(TAG, "Model download completed");
                if (progress != null) progress.onComplete(modelFile);
                return modelFile;
                
            } catch (Exception e) {
                Log.e(TAG, "Model download failed", e);
                if (progress != null) progress.onError("Download failed: " + e.getMessage());
                return null;
            }
        });
    }
}

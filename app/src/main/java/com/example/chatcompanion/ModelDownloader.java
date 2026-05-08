package com.example.chatcompanion;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class ModelDownloader {
    private static final String TAG = "ModelDownloader";
    private static final String MODEL_URL = "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final String MODEL_FILE_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final long MISTRAL_7B_SIZE = 4370911744L;
    
    public interface DownloadProgress {
        void onProgress(int percent);
        void onComplete(File modelFile);
        void onError(String error);
    }
    
    public static CompletableFuture<File> downloadModel(Context context, DownloadProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            RandomAccessFile output = null;
            InputStream input = null;
            
            try {
                File modelFile = new File(context.getFilesDir(), MODEL_FILE_NAME);
                long existingLength = 0;
                
                if (modelFile.exists()) {
                    existingLength = modelFile.length();
                    if (existingLength >= MISTRAL_7B_SIZE) {
                        Log.d(TAG, "Model already fully downloaded");
                        if (progress != null) progress.onComplete(modelFile);
                        return modelFile;
                    }
                    Log.d(TAG, "Resuming download from: " + existingLength);
                }
                
                Log.d(TAG, "Starting/Resuming model download...");
                URL url = new URL(MODEL_URL);
                connection = (HttpURLConnection) url.openConnection();
                
                // Set timeouts to prevent hanging
                connection.setConnectTimeout(15000); // 15 seconds
                connection.setReadTimeout(30000);    // 30 seconds
                
                // Add Range header for resuming
                if (existingLength > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingLength + "-");
                }
                
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Server responded with: " + responseCode);
                
                // 200 = OK (starting new), 206 = Partial Content (resuming)
                if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new Exception("Server returned code: " + responseCode);
                }
                
                long totalSize = connection.getContentLengthLong();
                if (responseCode == HttpURLConnection.HTTP_PARTIAL || existingLength > 0) {
                    totalSize += existingLength;
                }
                
                input = connection.getInputStream();
                output = new RandomAccessFile(modelFile, "rw");
                output.seek(existingLength);
                
                byte[] buffer = new byte[65536]; // Larger 64KB buffer for faster download
                int bytesRead;
                long totalBytesRead = existingLength;
                
                long lastUpdateTime = 0;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Throttle UI updates to once per 200ms
                    long currentTime = System.currentTimeMillis();
                    if (progress != null && totalSize > 0 && (currentTime - lastUpdateTime > 200)) {
                        int percent = (int) ((totalBytesRead * 100) / totalSize);
                        progress.onProgress(percent);
                        lastUpdateTime = currentTime;
                    }
                }
                
                Log.d(TAG, "Model download completed");
                if (progress != null) progress.onComplete(modelFile);
                return modelFile;
                
            } catch (Exception e) {
                Log.e(TAG, "Model download failed or timed out", e);
                if (progress != null) progress.onError("Download failed: " + e.getMessage() + ". Tap to resume.");
                return null;
            } finally {
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) {}
            }
        });
    }
}

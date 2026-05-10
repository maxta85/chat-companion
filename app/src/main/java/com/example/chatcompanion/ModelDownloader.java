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
    
    // Available models - smaller first
    private static final String[][] MODELS = {
        {"tinyllama-1b", "TinyLlama 1B", "tinyllama-1.1b-chat.Q4_K_M.gguf", "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat.Q4_K_M.gguf", "637MB"},
        {"phi-3-mini", "Phi-3 Mini", "phi-3-mini-4k-instruct-q4.gguf", "https://huggingface.co/TheBloke/Phi-3-mini-4k-instruct-GGUF/resolve/main/phi-3-mini-4k-instruct-q4.gguf", "2.3GB"},
        {"mistral-7b", "Mistral 7B", "mistral-7b-instruct-v0.2.Q4_K_M.gguf", "https://huggingface.co/TheBloke/Mistral-7B-Instruct-v0.2-GGUF/resolve/main/mistral-7b-instruct-v0.2.Q4_K_M.gguf", "4.1GB"},
    };
    
    public interface DownloadProgress {
        void onProgress(int percent);
        void onComplete(File modelFile);
        void onError(String error);
    }
    
    public static String[] getModelNames() {
        String[] names = new String[MODELS.length];
        for (int i = 0; i < MODELS.length; i++) {
            names[i] = MODELS[i][1] + " (" + MODELS[i][4] + ")";
        }
        return names;
    }
    
    public static String getModelUrl(int index) {
        if (index < 0 || index >= MODELS.length) index = 0;
        return MODELS[index][3];
    }
    
    public static String getModelFileName(int index) {
        if (index < 0 || index >= MODELS.length) index = 0;
        return MODELS[index][2];
    }
    
    public static File getModelFile(int index) {
        // Use external storage that survives app updates
        // We'll need context, so this is a placeholder - will be called with context
        return null; // Will be set properly when called
    }
    
    public static File getModelFile(Context context, int index) {
        if (index < 0 || index >= MODELS.length) index = 0;
        String fileName = MODELS[index][2];
        File modelDir = new File(context.getExternalFilesDir(null), "LLMModels");
        if (!modelDir.exists()) modelDir.mkdirs();
        return new File(modelDir, fileName);
    }
    
    public static long getExpectedSize(int index) {
        if (index < 0 || index >= MODELS.length) return 637 * 1024 * 1024;
        String size = MODELS[index][4];
        try {
            return (long)(Float.parseFloat(size.replace("GB", "").replace("MB", "").trim()) 
                * (size.contains("GB") ? 1024 * 1024 * 1024 : 1024 * 1024));
        } catch (Exception e) {
            return 637 * 1024 * 1024;
        }
    }
    
    public static CompletableFuture<File> downloadModel(Context context, int modelIndex, DownloadProgress progress) {
        return CompletableFuture.supplyAsync(() -> {
            HttpURLConnection connection = null;
            RandomAccessFile output = null;
            InputStream input = null;
            
            try {
                // Use external storage - survives app updates!
                File modelDir = new File(context.getExternalFilesDir(null), "LLMModels");
                if (!modelDir.exists()) modelDir.mkdirs();
                
                String fileName = getModelFileName(modelIndex);
                File modelFile = new File(modelDir, fileName);
                
                long expectedSize = getExpectedSize(modelIndex);
                long existingLength = modelFile.exists() ? modelFile.length() : 0;
                
                Log.d(TAG, "Model file: " + modelFile.getAbsolutePath());
                Log.d(TAG, "Existing: " + existingLength + " / " + expectedSize);
                
                if (existingLength >= expectedSize - 1000000) {
                    Log.d(TAG, "Model already complete!");
                    if (progress != null) progress.onComplete(modelFile);
                    return modelFile;
                }
                
                String modelUrl = getModelUrl(modelIndex);
                Log.d(TAG, "Downloading from: " + modelUrl);
                
                URL url = new URL(modelUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(60000);
                
                if (existingLength > 0) {
                    connection.setRequestProperty("Range", "bytes=" + existingLength + "-");
                }
                
                connection.connect();
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response: " + responseCode);
                
                if (responseCode != 200 && responseCode != 206) {
                    throw new Exception("Server error: " + responseCode);
                }
                
                long totalSize = connection.getContentLengthLong();
                if (responseCode == 206 || existingLength > 0) {
                    totalSize += existingLength;
                }
                if (totalSize <= 0) {
                    totalSize = expectedSize;
                }
                
                input = connection.getInputStream();
                output = new RandomAccessFile(modelFile, "rw");
                output.seek(existingLength);
                
                byte[] buffer = new byte[65536];
                long totalBytesRead = existingLength;
                long lastUpdateTime = 0;
                int bytesRead = 0;
                
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    long currentTime = System.currentTimeMillis();
                    if (progress != null && (currentTime - lastUpdateTime > 500)) {
                        int percent = (int) ((totalBytesRead * 100) / totalSize);
                        progress.onProgress(Math.min(percent, 100));
                        lastUpdateTime = currentTime;
                    }
                }
                
                Log.d(TAG, "Download complete: " + totalBytesRead);
                if (progress != null) progress.onComplete(modelFile);
                return modelFile;
                
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
                if (progress != null) progress.onError("Error: " + e.getMessage());
                return null;
            } finally {
                try { if (input != null) input.close(); } catch (Exception ignored) {}
                try { if (output != null) output.close(); } catch (Exception ignored) {}
                try { if (connection != null) connection.disconnect(); } catch (Exception ignored) {}
            }
        });
    }
}

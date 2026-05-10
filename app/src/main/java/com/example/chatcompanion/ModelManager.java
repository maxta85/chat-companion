package com.example.chatcompanion;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ModelManager {
    private static final String TAG = "ModelManager";
    
    // Available models and their sizes
    public static final String[][] MODELS = {
        {"mistral-7b", "Mistral 7B", "mistral-7b-instruct-v0.2.Q4_K_M.gguf", "4.1 GB"},
        {"phi-3-mini", "Phi-3 Mini", "phi-3-mini-4k-instruct-q4.gguf", "2.3 GB"},
        {"llama-3-8b", "Llama 3 8B", "Meta-Llama-3-8B-Instruct-Q4_K_M.gguf", "4.9 GB"},
        {"gemma-7b", "Gemma 7B", "gemma-7b-it-q4.gguf", "4.2 GB"}
    };
    
    private static final String MODEL_DIR_NAME = "LLMModels";
    private static final String BACKUP_DIR_NAME = "ChatCompanionBackups";
    
    // Get external storage directory (survives app updates)
    public static File getModelDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(null), MODEL_DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
    
    public static File getBackupDirectory(Context context) {
        File dir = new File(context.getExternalFilesDir(null), BACKUP_DIR_NAME);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }
    
    public static File getModelFile(Context context, int modelIndex) {
        if (modelIndex < 0 || modelIndex >= MODELS.length) modelIndex = 0;
        String fileName = MODELS[modelIndex][2];
        return new File(getModelDirectory(context), fileName);
    }
    
    // Backward compatible - use current model
    public static File getModelFile(Context context) {
        return getCurrentModelFile(context);
    }
    
    public static File getCurrentModelFile(Context context) {
        int currentModel = getCurrentModelIndex(context);
        return getModelFile(context, currentModel);
    }
    
    public static int getCurrentModelIndex(Context context) {
        int index = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getInt("current_model", 0);
        return Math.min(index, MODELS.length - 1);
    }
    
    public static void setCurrentModelIndex(Context context, int index) {
        if (index >= 0 && index < MODELS.length) {
            context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().putInt("current_model", index).apply();
        }
    }
    
    public static String getCurrentModelName(Context context) {
        int index = getCurrentModelIndex(context);
        return MODELS[index][1];
    }
    
    public static long getCurrentModelSizeInBytes(Context context) {
        int index = getCurrentModelIndex(context);
        return parseSizeToBytes(MODELS[index][3]);
    }
    
    private static long parseSizeToBytes(String size) {
        try {
            String num = size.replace("GB", "").trim();
            return (long)(Float.parseFloat(num) * 1024 * 1024 * 1024);
        } catch (Exception e) {
            return 4370911744L;
        }
    }
    
    public static boolean modelExists(Context context) {
        File modelFile = getCurrentModelFile(context);
        return modelFile.exists() && modelFile.length() > 1024 * 1024; // At least 1MB
    }
    
    public static long getModelSize(Context context) {
        File modelFile = getCurrentModelFile(context);
        return modelFile.exists() ? modelFile.length() : 0;
    }
    
    public static boolean isModelComplete(Context context) {
        if (!modelExists(context)) return false;
        long expectedSize = getCurrentModelSizeInBytes(context);
        long actualSize = getModelSize(context);
        return actualSize >= expectedSize - 1000000;
    }
    
    public static void deleteModel(Context context) {
        File modelFile = getCurrentModelFile(context);
        if (modelFile.exists()) {
            boolean deleted = modelFile.delete();
            Log.d(TAG, "Model deleted: " + deleted);
        }
    }
    
    public static void deleteAllModels(Context context) {
        File modelDir = getModelDirectory(context);
        if (modelDir.exists()) {
            for (File f : modelDir.listFiles()) {
                f.delete();
            }
        }
    }
    
    // Backup methods
    public static boolean createBackup(Context context, String backupName) {
        try {
            File modelDir = getModelDirectory(context);
            File backupFile = new File(getBackupDirectory(context), 
                backupName + ".zip");
            
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile));
            
            // Add each model file to the zip
            for (File modelFile : modelDir.listFiles()) {
                if (modelFile.length() > 1024 * 1024) { // Only files > 1MB
                    addToZip(zos, modelFile, modelFile.getName());
                }
            }
            
            zos.close();
            Log.d(TAG, "Backup created: " + backupFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Backup failed", e);
            return false;
        }
    }
    
    private static void addToZip(ZipOutputStream zos, File file, String entryName) throws Exception {
        byte[] buffer = new byte[8192];
        FileInputStream fis = new FileInputStream(file);
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        
        int len;
        while ((len = fis.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }
        fis.close();
        zos.closeEntry();
    }
    
    // Restore from backup
    public static boolean restoreBackup(Context context, String backupName) {
        try {
            File backupFile = new File(getBackupDirectory(context), backupName + ".zip");
            if (!backupFile.exists()) return false;
            
            ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile));
            ZipEntry entry;
            File modelDir = getModelDirectory(context);
            
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = new File(modelDir, entry.getName());
                FileOutputStream fos = new FileOutputStream(outFile);
                
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                Log.d(TAG, "Restored: " + entry.getName());
            }
            zis.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Restore failed", e);
            return false;
        }
    }
    
    public static File[] listBackups(Context context) {
        File backupDir = getBackupDirectory(context);
        return backupDir.listFiles((dir, name) -> name.endsWith(".zip"));
    }
    
    public static String[] getModelNames() {
        String[] names = new String[MODELS.length];
        for (int i = 0; i < MODELS.length; i++) {
            names[i] = MODELS[i][1] + " (" + MODELS[i][3] + ")";
        }
        return names;
    }
}

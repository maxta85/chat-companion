package com.example.chatcompanion;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class ModelManager {
    private static final String TAG = "ModelManager";
    private static final String MODEL_FILE_NAME = "mistral-7b-instruct-v0.2.Q4_K_M.gguf";
    private static final long MODEL_SIZE_BYTES = 4370911744L; // ~4.1GB for Mistral 7B
    
    public static File getModelFile(Context context) {
        return new File(context.getFilesDir(), MODEL_FILE_NAME);
    }
    
    public static boolean modelExists(Context context) {
        File modelFile = getModelFile(context);
        return modelFile.exists() && modelFile.length() > 0;
    }
    
    public static long getModelSize(Context context) {
        File modelFile = getModelFile(context);
        return modelFile.exists() ? modelFile.length() : 0;
    }
    
    public static boolean isModelComplete(Context context) {
        return modelExists(context) && getModelSize(context) >= MODEL_SIZE_BYTES - 1000000; // Allow 1MB tolerance
    }
    
    public static void copyModelFromAssets(Context context) {
        // TODO: Implement model copying from assets if bundled
        Log.d(TAG, "Model copying from assets not implemented yet");
    }
    
    public static void deleteModel(Context context) {
        File modelFile = getModelFile(context);
        if (modelFile.exists()) {
            boolean deleted = modelFile.delete();
            Log.d(TAG, "Model deleted: " + deleted);
        }
    }
}

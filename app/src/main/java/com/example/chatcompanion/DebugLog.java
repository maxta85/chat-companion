package com.example.chatcompanion;

import android.util.Log;

/**
 * Debug logging utility for troubleshooting
 */
public class DebugLog {
    private static final String TAG = "ChatCompanion";
    private static boolean enabled = true; // Enable debug logging
    
    public static void log(String message) {
        if (enabled) Log.d(TAG, message);
    }
    
    public static void log(String tag, String message) {
        if (enabled) Log.d(TAG, "[" + tag + "] " + message);
    }
    
    public static void error(String message) {
        if (enabled) Log.e(TAG, message);
    }
    
    public static void error(String tag, String message) {
        if (enabled) Log.e(TAG, "[" + tag + "] " + message);
    }
    
    public static void setEnabled(boolean enabled) {
        DebugLog.enabled = enabled;
    }
}
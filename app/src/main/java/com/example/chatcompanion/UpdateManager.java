package com.example.chatcompanion;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String CURRENT_VERSION = "2.2";
    private static final String VERSION_CHECK_URL = "https://raw.githubusercontent.com/maxta85/chat-companion/main/version.txt";
    
    public interface UpdateCheckListener {
        void onUpdateAvailable(String version, String downloadUrl);
        void onUpdateNotAvailable();
        void onError(String error);
    }
    
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
    
    public static void checkForUpdates(Context context, UpdateCheckListener listener) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Checking: " + VERSION_CHECK_URL);
                HttpURLConnection conn = (HttpURLConnection) new URL(VERSION_CHECK_URL).openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Response: " + responseCode);
                
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line = reader.readLine();
                    reader.close();
                    
                    if (line != null && line.contains("|")) {
                        String[] parts = line.split("\\|");
                        String latestVersion = parts[0].trim();
                        String downloadUrl = parts[1].trim();
                        
                        boolean needsUpdate = isNewerVersion(latestVersion, CURRENT_VERSION);
                        Log.d(TAG, "Latest: " + latestVersion + ", needs: " + needsUpdate);
                        
                        final String ver = latestVersion;
                        final String url = downloadUrl;
                        final boolean needs = needsUpdate;
                        
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (needs) {
                                listener.onUpdateAvailable(ver, url);
                            } else {
                                listener.onUpdateNotAvailable();
                            }
                        });
                    } else {
                        new Handler(Looper.getMainLooper()).post(() -> listener.onError("Invalid format"));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> listener.onError("HTTP " + responseCode));
                }
            } catch (Exception e) {
                Log.e(TAG, "Check failed", e);
                new Handler(Looper.getMainLooper()).post(() -> listener.onError(e.getMessage()));
            }
        }).start();
    }
    
    private static boolean isNewerVersion(String latest, String current) {
        try {
            float latestFloat = Float.parseFloat(latest.replace("v", ""));
            float currentFloat = Float.parseFloat(current.replace("v", ""));
            return latestFloat > currentFloat;
        } catch (Exception e) {
            return latest.compareTo(current) > 0;
        }
    }
    
    public static void downloadAndInstall(Context context, String downloadUrl) {
        // Download to app's external files directory
        File apkFile = new File(context.getExternalFilesDir(null), "chat-companion-update.apk");
        
        new Thread(() -> {
            try {
                Log.d(TAG, "Downloading: " + downloadUrl);
                HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                
                try (FileOutputStream fos = new FileOutputStream(apkFile);
                     java.io.InputStream is = conn.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                Log.d(TAG, "Downloaded: " + apkFile.getAbsolutePath());
                
                final File f = apkFile;
                final String url = downloadUrl;
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    } catch (Exception e) {
                        // Fallback: open in browser
                        Log.e(TAG, "Direct install failed, opening browser", e);
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        context.startActivity(intent);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Download failed", e);
            }
        }).start();
    }
}
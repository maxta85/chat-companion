package com.example.chatcompanion;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateManager {
    private static final String TAG = "UpdateManager";
    private static final String REPO_OWNER = "maxta85";
    private static final String REPO_NAME = "chat-companion";
    private static final String CURRENT_VERSION = "1.3";
    
    public interface UpdateCheckListener {
        void onUpdateAvailable(String latestVersion, String downloadUrl);
        void onUpdateNotAvailable();
        void onError(String error);
    }
    
    public static void checkForUpdates(Context context, UpdateCheckListener listener) {
        new Thread(() -> {
            try {
                String latestVersion = getLatestReleaseVersion();
                if (latestVersion == null) {
                    listener.onError("Could not fetch latest version");
                    return;
                }
                
                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    String downloadUrl = getLatestApkDownloadUrl();
                    listener.onUpdateAvailable(latestVersion, downloadUrl);
                } else {
                    listener.onUpdateNotAvailable();
                }
            } catch (Exception e) {
                Log.e(TAG, "Update check failed", e);
                listener.onError(e.getMessage());
            }
        }).start();
    }
    
    private static String getLatestReleaseVersion() throws Exception {
        String apiUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
        
        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse tag_name from JSON response
            String json = response.toString();
            int tagIndex = json.indexOf("\"tag_name\":\"");
            if (tagIndex >= 0) {
                int start = tagIndex + 10;
                int end = json.indexOf("\"", start);
                return json.substring(start, end);
            }
        }
        return null;
    }
    
    private static String getLatestApkDownloadUrl() throws Exception {
        String apiUrl = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME + "/releases/latest";
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        
        if (conn.getResponseCode() == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Look for .apk asset
            String json = response.toString();
            int apkIndex = json.indexOf(".apk");
            if (apkIndex >= 0) {
                int browserDownloadUrlIndex = json.lastIndexOf("\"browser_download_url\":\"", apkIndex - 50);
                if (browserDownloadUrlIndex >= 0) {
                    int start = browserDownloadUrlIndex + 24;
                    int end = json.indexOf("\"", start);
                    return json.substring(start, end);
                }
            }
        }
        return null;
    }
    
    private static boolean isNewerVersion(String latest, String current) {
        try {
            float latestFloat = Float.parseFloat(latest.replace("v", ""));
            float currentFloat = Float.parseFloat(current.replace("v", ""));
            return latestFloat > currentFloat;
        } catch (NumberFormatException e) {
            return latest.compareTo(current) > 0;
        }
    }
    
    public static void downloadAndInstall(Context context, String downloadUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("Chat Companion Update");
        request.setDescription("Downloading new version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "chat-companion-update.apk");
        
        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = dm.enqueue(request);
    }
    
    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }
}
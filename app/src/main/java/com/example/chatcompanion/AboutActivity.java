package com.example.chatcompanion;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import android.util.Log;
import androidx.core.content.FileProvider;
import android.os.Build;

public class AboutActivity extends Activity {
    
    private TextView versionText;
    private TextView updateStatusText;
    private Button checkUpdateButton;
    private Button downloadUpdateButton;
    private ProgressBar updateProgressBar;
    private Button backButton;
    private Button githubButton;
    
    private static final String GITHUB_VERSION_URL = "https://raw.githubusercontent.com/maxta85/chat-companion/main/version.txt";
    private static final String GITHUB_APK_URL = "https://github.com/maxta85/chat-companion/releases/download/latest/chat-companion.apk";
    private static final String GITHUB_REPO_URL = "https://github.com/maxta85/chat-companion";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        
        // Initialize UI components
        versionText = findViewById(R.id.versionText);
        updateStatusText = findViewById(R.id.updateStatusText);
        checkUpdateButton = findViewById(R.id.checkUpdateButton);
        downloadUpdateButton = findViewById(R.id.downloadUpdateButton);
        updateProgressBar = findViewById(R.id.updateProgressBar);
        backButton = findViewById(R.id.backButton);
        githubButton = findViewById(R.id.githubButton);
        
        // Set current version
        versionText.setText("Chat Companion v4.1");
        updateStatusText.setText("Tap 'Check for Updates' to see if a new version is available.");
        
        // Set up button listeners
        checkUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });
        
        downloadUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadUpdate();
            }
        });
        
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        githubButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGitHubRepo();
            }
        });
        
        // Initially hide download button and progress
        downloadUpdateButton.setVisibility(View.GONE);
        updateProgressBar.setVisibility(View.GONE);
    }
    
    private void checkForUpdates() {
        updateStatusText.setText("Checking for updates...");
        checkUpdateButton.setEnabled(false);
        updateProgressBar.setVisibility(View.VISIBLE);
        
        new CheckUpdateTask().execute();
    }
    
    private class CheckUpdateTask extends AsyncTask<Void, Void, String> {
        private String latestVersion;
        private String downloadUrl;
        
        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(GITHUB_VERSION_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line = reader.readLine();
                    if (line != null) {
                        String[] parts = line.split("\\|");
                        latestVersion = parts[0].trim();
                        if (parts.length > 1) {
                            downloadUrl = parts[1].trim();
                        }
                    }
                    reader.close();
                    
                    // Compare versions
                    if ("4.1".equals(latestVersion)) {
                        return "up_to_date";
                    } else {
                        return "update_available";
                    }
                } else {
                    return "error";
                }
            } catch (Exception e) {
                return "error";
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            runOnUiThread(() -> {
                checkUpdateButton.setEnabled(true);
                updateProgressBar.setVisibility(View.GONE);
                
                switch (result) {
                    case "up_to_date":
                        updateStatusText.setText("✅ You have the latest version!");
                        downloadUpdateButton.setVisibility(View.GONE);
                        break;
                    case "update_available":
                        updateStatusText.setText("🔄 Update available! Version " + latestVersion + " is ready to download.");
                        downloadUpdateButton.setVisibility(View.VISIBLE);
                        // Save the download URL for the next task
                        if (downloadUrl != null) {
                            getSharedPreferences("updates", MODE_PRIVATE).edit().putString("download_url", downloadUrl).apply();
                        }
                        break;
                    case "error":
                        updateStatusText.setText("❌ Failed to check for updates. Please check your internet connection.");
                        downloadUpdateButton.setVisibility(View.GONE);
                        break;
                }
            });
        }
    }
    
    private void downloadUpdate() {
        String downloadUrl = getSharedPreferences("updates", MODE_PRIVATE).getString("download_url", GITHUB_APK_URL);
        updateStatusText.setText("Downloading update...");
        downloadUpdateButton.setEnabled(false);
        updateProgressBar.setVisibility(View.VISIBLE);
        
        new DownloadUpdateTask(downloadUrl).execute();
    }
    
    private class DownloadUpdateTask extends AsyncTask<Void, Integer, Boolean> {
        private File apkFile;
        private String urlStr;

        public DownloadUpdateTask(String url) {
            this.urlStr = url;
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    int fileLength = connection.getContentLength();
                    
                    // Create temp file for download
                    apkFile = new File(getExternalFilesDir(null), "chat-companion-update.apk");
                    FileOutputStream outputStream = new FileOutputStream(apkFile);
                    
                    InputStream inputStream = connection.getInputStream();
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalBytesRead = 0;
                    
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                        
                        // Update progress
                        if (fileLength > 0) {
                            int progress = (int) ((totalBytesRead * 100) / fileLength);
                            publishProgress(progress);
                        }
                    }
                    
                    outputStream.close();
                    inputStream.close();
                    return true;
                } else {
                    return false;
                }
            } catch (Exception e) {
                return false;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... progress) {
            updateProgressBar.setProgress(progress[0]);
            updateStatusText.setText("Downloading update... " + progress[0] + "%");
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            runOnUiThread(() -> {
                downloadUpdateButton.setEnabled(true);
                updateProgressBar.setVisibility(View.GONE);
                
                if (success && apkFile != null && apkFile.exists()) {
                    updateStatusText.setText("✅ Update downloaded! Installing...");
                    installUpdate(apkFile);
                } else {
                    updateStatusText.setText("❌ Failed to download update. Please try again.");
                }
            });
        }
    }
    
    private void installUpdate(File apkFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("AboutActivity", "Installation failed", e);
            updateStatusText.setText("❌ Failed to install update. Please install manually.");
        }
    }
    
    private void openGitHubRepo() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(GITHUB_REPO_URL));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open GitHub repository", Toast.LENGTH_SHORT).show();
        }
    }
}

package com.example.chatcompanion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.view.MenuItem;

public class MainActivity extends Activity {
    
    private EditText messageInput;
    private Button sendButton;
    private ImageButton menuButton;
    private TextView chatDisplay;
    private TextView responseText;
    private TextView debugText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        menuButton = findViewById(R.id.menuButton);
        chatDisplay = findViewById(R.id.chatDisplay);
        responseText = findViewById(R.id.responseText);
        
        // Update debug panel
        debugText = findViewById(R.id.debugText);
        updateDebugPanel();
        
        // Settings button
        Button openSettingsButton = findViewById(R.id.openSettingsButton);
        openSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
        
        // Set initial text
        chatDisplay.setText("Chat Companion - Ready!\n\n");
        responseText.setText("LLM Response will appear here");

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        // Check if model exists and redirect to settings if needed
        if (!ModelManager.modelExists(this)) {
            responseText.setText("⚠️ AI model not downloaded. Go to Settings to download.");
        } else {
            loadAIModel();
        }
        
        // Set up button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString().trim();
                if (!message.isEmpty()) {
                    chatDisplay.append("You: " + message + "\n");
                    messageInput.setText("");
                    responseText.setText("AI thinking...");
                    
                    LLMService.getInstance(MainActivity.this).generateResponse(message)
                        .thenAccept(response -> {
                            runOnUiThread(() -> {
                                responseText.setText(response);
                                chatDisplay.append("Bot: " + response + "\n\n");
                                // Update debug panel after response
                                updateDebugPanel();
                            });
                        });
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateDebugPanel() {
        if (debugText == null) return;
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEBUG ===\n");
        
        boolean modelExists = ModelManager.modelExists(this);
        sb.append("Model: ").append(modelExists ? "✅ Loaded" : "❌ Not found").append("\n");
        
        java.io.File modelFile = ModelManager.getCurrentModelFile(this);
        if (modelFile != null && modelFile.exists()) {
            long mb = modelFile.length() / (1024 * 1024);
            sb.append("Size: ").append(mb).append(" MB\n");
        }
        
        sb.append("First Run: ").append(ModelManager.isFirstRun(this)).append("\n");
        sb.append("Version: 1.4");
        
        debugText.setText(sb.toString());
    }
    
    private void showMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Model Settings");
        popup.getMenu().add("About");
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getTitle().equals("Model Settings")) {
                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    return true;
                } else if (item.getTitle().equals("About")) {
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void loadAIModel() {
        LLMService llmService = LLMService.getInstance(this);
        responseText.setText("Loading model...");
        
        llmService.loadModel().thenAccept(success -> {
            runOnUiThread(() -> {
                if (success) {
                    responseText.setText("✅ Model loaded! Real AI ready!");
                } else {
                    responseText.setText("❌ Failed to load model. Using smart responses.");
                }
            });
        });
    }
}

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
                    responseText.setText("Mistral 7B is thinking...");
                    
                    LLMService.getInstance(MainActivity.this).generateResponse(message)
                        .thenAccept(response -> {
                            runOnUiThread(() -> {
                                responseText.setText(response);
                                chatDisplay.append("Bot: " + response + "\n\n");
                            });
                        });
                } else {
                    Toast.makeText(MainActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                }
            }
        });
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
        responseText.setText("Loading Mistral 7B model (4.1GB)...");
        
        llmService.loadModel().thenAccept(success -> {
            runOnUiThread(() -> {
                if (success) {
                    responseText.setText("✅ Mistral 7B loaded! Real AI ready!");
                } else {
                    responseText.setText("❌ Failed to load Mistral 7B. Using smart responses.");
                }
            });
        });
    }
}

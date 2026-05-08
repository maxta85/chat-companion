package com.example.chatcompanion;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    private EditText messageInput;
    private Button sendButton;
    private TextView chatDisplay;
    private TextView responseText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize UI components
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatDisplay = findViewById(R.id.chatDisplay);
        responseText = findViewById(R.id.responseText);
        
        // Set initial text
        chatDisplay.setText("Chat Companion - Ready!\n\n");
        responseText.setText("LLM Response will appear here");

        // Add settings and about buttons
        LinearLayout inputLayout = (LinearLayout) sendButton.getParent();
        
        Button settingsButton = new Button(this);
        settingsButton.setText("⚙️ Settings");
        settingsButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.5f
        ));

        Button aboutButton = new Button(this);
        aboutButton.setText("ℹ️ About");
        aboutButton.setLayoutParams(new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            0.5f
        ));

        // Create a horizontal layout for settings and about buttons
        LinearLayout extraButtonsLayout = new LinearLayout(this);
        extraButtonsLayout.setOrientation(LinearLayout.HORIZONTAL);
        extraButtonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        extraButtonsLayout.addView(settingsButton);
        extraButtonsLayout.addView(aboutButton);

        // Add the extra buttons layout to the main input layout
        // We need to add it to the parent of inputLayout if it's a vertical layout
        LinearLayout mainLayout = (LinearLayout) inputLayout.getParent();
        int index = mainLayout.indexOfChild(inputLayout);
        mainLayout.addView(extraButtonsLayout, index);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        aboutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });

        // Check if model exists and redirect to settings if needed
        if (!ModelManager.modelExists(this)) {
            responseText.setText("⚠️ AI model not downloaded. Go to Settings to download.");
            // Don't auto-download - let user choose in settings
        } else {
            loadAIModel();
        }
        
        // Set up button click listener
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = messageInput.getText().toString().trim();
                if (!message.isEmpty()) {
                    // Add user message to chat
                    chatDisplay.append("You: " + message + "\n");
                    
                    // Clear input
                    messageInput.setText("");
                    
                    // Show loading indicator
                    responseText.setText("Mistral 7B is thinking...");
                    
                    // Get LLM response
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

    private void loadAIModel() {
        LLMService llmService = LLMService.getInstance(this);
        responseText.setText("Loading Mistral 7B model (4.1GB)...");
        
        llmService.loadModel().thenAccept(success -> {
            runOnUiThread(() -> {
                if (success) {
                    responseText.setText("✅ Mistral 7B loaded! Real AI ready!");
                    Log.d("MainActivity", "Model load successful");
                } else {
                    responseText.setText("❌ Failed to load Mistral 7B. Using smart responses.");
                    Log.e("MainActivity", "Model load failed");
                }
            });
        });
    }
}

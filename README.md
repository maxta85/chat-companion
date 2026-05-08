# Chat Companion Android App

A basic Android app for testing offline LLM integration on Samsung S26 Ultra.

## Project Structure

```
android_chat_companion/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/example/chatcompanion/
│   │       │   └── MainActivity.java
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml
│   │       │   └── values/
│   │       │       ├── strings.xml
│   │       │       └── styles.xml
│   │       └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── gradle.properties
└── README.md
```

## Current Features

- Basic chat interface
- Message input and display
- Simulated bot responses
- Clean Material Design UI

## Next Steps

1. Install Android development tools
2. Build the APK file
3. Transfer to S26 Ultra via Windows Phone Link
4. Install and test
5. Research offline LLM options (llama.cpp, TensorFlow Lite, etc.)

## Offline LLM Options to Research

- **llama.cpp** - C++ implementation, Android support
- **TensorFlow Lite** - Google's mobile ML framework
- **ONNX Runtime** - Cross-platform ML inference
- **MediaPipe** - Google's on-device ML framework

## Build Requirements

- Android SDK 24+
- Java 8+
- Gradle 8.0+

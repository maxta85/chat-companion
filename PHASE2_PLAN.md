# Phase 2: Offline LLM Integration Plan

## Architecture Overview
```
MainActivity (UI Layer)
    ↓
LLMService (Business Logic)
    ↓
llama.cpp Android Binding (Inference Engine)
    ↓
Gemma 2B GGUF Model (AI Brain)
```

## Implementation Steps

### Step 1: Add Dependencies
- Add llama.cpp Android binding to build.gradle
- Add required permissions for model access

### Step 2: Create LLM Service Class
- LLMService.java - handles model loading and inference
- ModelManager.java - manages GGUF model file
- ResponseGenerator.java - formats LLM responses

### Step 3: Download Model
- Download Gemma 2B GGUF model (1.3GB)
- Place in app/src/main/assets/models/
- Update build.gradle to include model

### Step 4: Integration
- Update MainActivity to use LLMService
- Replace simulated responses with real inference
- Add loading indicators and error handling

### Step 5: Testing & Optimization
- Test inference speed on S26 Ultra
- Optimize context size and token limits
- Add memory management

## Key Files to Create/Modify
1. app/build.gradle - Add dependencies
2. LLMService.java - Main inference class
3. ModelManager.java - Model file management
4. MainActivity.java - Update UI integration
5. AndroidManifest.xml - Add permissions

## Expected Performance
- Model Size: 1.3GB (Gemma 2B)
- RAM Usage: ~2GB
- Inference Speed: 2-5 tokens/second
- Context Length: 2048 tokens

## Gemini Agent Prompts
Will provide specific prompts for each step to guide the Android Studio AI agent.

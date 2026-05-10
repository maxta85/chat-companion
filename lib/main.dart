import 'dart:io';
import 'package:flutter/material.dart';
import 'package:llama_cpp_dart/llama_cpp_dart.dart';
import 'package:path_provider/path_provider.dart';
import 'package:file_picker/file_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const ChatCompanionApp());
}

class ChatCompanionApp extends StatelessWidget {
  const ChatCompanionApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Chat Companion',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const ChatScreen(),
    );
  }
}

class ChatMessage {
  final String text;
  final bool isUser;
  ChatMessage({required this.text, required this.isUser});
}

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  State<ChatScreen> createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final List<ChatMessage> _messages = [];
  final TextEditingController _controller = TextEditingController();
  final ScrollController _scrollController = ScrollController();

  bool _isLoading = false;
  bool _modelLoaded = false;
  Llama? _llama;
  String _modelName = 'No model loaded';

  @override
  void initState() {
    super.initState();
    _loadSavedModel();
  }

  Future<void> _loadSavedModel() async {
    final prefs = await SharedPreferences.getInstance();
    final savedPath = prefs.getString('model_path');
    if (savedPath != null && await File(savedPath).exists()) {
      await _loadModel(savedPath);
    }
  }

  Future<void> _loadModel(String path) async {
    setState(() => _isLoading = true);
    try {
      _llama?.dispose();
      final ctxParams = ContextParams();
      ctxParams.nCtx = 2048;
      _llama = Llama(
        path,
        modelParams: ModelParams(),
        contextParams: ctxParams,
      );
      setState(() {
        _modelLoaded = true;
        _modelName = path.split('/').last;
        _isLoading = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Model loaded: $_modelName')),
        );
      }
    } catch (e) {
      setState(() => _isLoading = false);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading model: $e')),
        );
      }
    }
  }

  Future<void> _pickModel() async {
    final result = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['gguf'],
    );
    if (result != null && result.files.single.path != null) {
      final path = result.files.single.path!;
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('model_path', path);
      await _loadModel(path);
    }
  }

  Future<void> _downloadModel() async {
    const url = 'https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-GGUF/resolve/main/tinyllama-1.1b-chat.Q4_K_M.gguf';
    final dir = await getApplicationDocumentsDirectory();
    final path = '${dir.path}/tinyllama.gguf';

    setState(() => _isLoading = true);
    try {
      final response = await http.get(Uri.parse(url));
      if (response.statusCode == 200) {
        final file = File(path);
        await file.writeAsBytes(response.bodyBytes);
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString('model_path', path);
        await _loadModel(path);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Download failed: $e')),
        );
      }
    }
    setState(() => _isLoading = false);
  }

  Future<void> _sendMessage() async {
    final text = _controller.text.trim();
    if (text.isEmpty) return;

    _controller.clear();
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true));
      _isLoading = true;
    });

    _scrollToBottom();

    try {
      String response;
      if (_llama != null && _modelLoaded) {
        _llama!.setPrompt('''<|system|>
You are a helpful AI assistant running locally on device.
<|user|>
$text
<|assistant|>
'''.trim());
        final result = await _llama!.generateCompleteText(maxTokens: 256);
        response = result.trim();
        if (response.isEmpty) response = "I couldn't generate a response. Try again.";
      } else {
        response = _generateFallback(text);
      }

      setState(() {
        _messages.add(ChatMessage(text: response, isUser: false));
      });
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(text: 'Error: $e', isUser: false));
      });
    }

    setState(() => _isLoading = false);
    _scrollToBottom();
  }

  String _generateFallback(String prompt) {
    prompt = prompt.toLowerCase();
    if (prompt.contains('hello') || prompt.contains('hi')) {
      return 'Hello! I\'m ready to help. Load a model to enable local AI responses.';
    }
    if (prompt.contains('help')) {
      return 'I can help you with various tasks. Currently in fallback mode - load a GGUF model for full AI.';
    }
    return 'This is a fallback response. Load a GGUF model file for proper local AI inference.';
  }

  void _scrollToBottom() {
    Future.delayed(const Duration(milliseconds: 100), () {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Chat Companion - $_modelName'),
        backgroundColor: Colors.deepPurple,
        actions: [
          IconButton(
            icon: const Icon(Icons.folder_open),
            onPressed: _pickModel,
            tooltip: 'Load GGUF Model',
          ),
          IconButton(
            icon: const Icon(Icons.download),
            onPressed: _downloadModel,
            tooltip: 'Download Model',
          ),
        ],
      ),
      body: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            color: _modelLoaded ? Colors.green[100] : Colors.orange[100],
            child: Row(
              children: [
                Icon(
                  _modelLoaded ? Icons.check_circle : Icons.warning,
                  color: _modelLoaded ? Colors.green : Colors.orange,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _modelLoaded
                        ? 'Local AI ready ($_modelName)'
                        : 'No model - tap folder to load GGUF file',
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(8),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                return Align(
                  alignment: msg.isUser
                      ? Alignment.centerRight
                      : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.symmetric(vertical: 4),
                    padding: const EdgeInsets.all(12),
                    constraints: BoxConstraints(
                      maxWidth: MediaQuery.of(context).size.width * 0.75,
                    ),
                    decoration: BoxDecoration(
                      color: msg.isUser
                          ? Colors.deepPurple[100]
                          : Colors.grey[200],
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(msg.text),
                  ),
                );
              },
            ),
          ),
          if (_isLoading)
            const Padding(
              padding: EdgeInsets.all(8),
              child: CircularProgressIndicator(),
            ),
          Padding(
            padding: const EdgeInsets.all(8),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _controller,
                    decoration: const InputDecoration(
                      hintText: 'Type a message...',
                      border: OutlineInputBorder(),
                    ),
                    onSubmitted: (_) => _sendMessage(),
                  ),
                ),
                const SizedBox(width: 8),
                IconButton.filled(
                  onPressed: _isLoading ? null : _sendMessage,
                  icon: const Icon(Icons.send),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

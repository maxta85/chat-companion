import 'package:flutter_test/flutter_test.dart';
import 'package:chat_companion/main.dart';

void main() {
  testWidgets('App loads', (WidgetTester tester) async {
    await tester.pumpWidget(const ChatCompanionApp());
  });
}

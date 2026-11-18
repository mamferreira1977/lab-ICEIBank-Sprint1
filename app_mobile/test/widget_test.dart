
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import '../lib/main.dart';

void main() {
  testWidgets('ICEIBank abre a tela inicial', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: BankPage(),
      ),
    );

    expect(find.text('ICEIBank • Sprint 3'), findsOneWidget);
  });
}

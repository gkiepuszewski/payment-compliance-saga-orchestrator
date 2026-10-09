// Basic smoke test: the app starts on the payments list screen. No backend
// is reachable in this test, so the list surfaces the error/retry state -
// this still proves the widget tree builds and the app bar/FAB render.
import 'package:flutter_test/flutter_test.dart';

import 'package:payment_app/main.dart';

void main() {
  testWidgets('Payments list screen renders with its app bar and FAB', (tester) async {
    await tester.pumpWidget(const PaymentApp());
    await tester.pump();

    expect(find.text('Payments'), findsOneWidget);
    expect(find.text('New payment'), findsOneWidget);
  });
}

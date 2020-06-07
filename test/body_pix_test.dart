import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:body_pix/body_pix.dart';

void main() {
  const MethodChannel channel = MethodChannel('body_pix');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

}

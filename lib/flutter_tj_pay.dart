import 'package:flutter/services.dart';

class FlutterTJPay {
 static const methodChannel = MethodChannel('flutter_tj_pay');

  static Future<dynamic> init(Map<String, dynamic> config) {
    return methodChannel.invokeMethod('init', config);
  }

  static Future<dynamic> transaction(Map<String, dynamic> transaction) {
    return methodChannel.invokeMethod('transaction', transaction);
  }
}

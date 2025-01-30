import 'package:flutter/services.dart';

class FlutterTJPay {
 static const methodChannel = MethodChannel('flutter_tj_pay');

  static Future<dynamic> init() {
    return methodChannel.invokeMethod('INIT_SERVICE');
  }

  static Future<dynamic> transact(Map<String, dynamic> transaction) {
    return methodChannel.invokeMethod('ACTION_SEND', transaction);
  }
}

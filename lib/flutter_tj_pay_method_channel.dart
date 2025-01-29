import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'flutter_tj_pay_platform_interface.dart';

/// An implementation of [FlutterTjPayPlatform] that uses method channels.
class MethodChannelFlutterTJPay extends FlutterTJPayPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('flutter_tj_pay');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}

import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'flutter_tj_pay_method_channel.dart';

abstract class FlutterTJPayPlatform extends PlatformInterface {
  /// Constructs a FlutterTjPayPlatform.
  FlutterTJPayPlatform() : super(token: _token);

  static final Object _token = Object();

  static FlutterTJPayPlatform _instance = MethodChannelFlutterTJPay();

  /// The default instance of [FlutterTJPayPlatform] to use.
  ///
  /// Defaults to [MethodChannelFlutterTJPay].
  static FlutterTJPayPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [FlutterTjPayPlatform] when
  /// they register themselves.
  static set instance(FlutterTJPayPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}

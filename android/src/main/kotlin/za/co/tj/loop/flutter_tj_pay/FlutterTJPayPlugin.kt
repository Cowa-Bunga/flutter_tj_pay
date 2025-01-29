package za.co.tj.loop.flutter_tj_pay

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler


/** FlutterTJPayPlugin **/
class FlutterTJPayPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

  private val triggerTJ = 36
  private val loggingTag = "TJPayPlugin"
  private var act: Activity? = null
  private lateinit var channel: MethodChannel
  private lateinit var result: MethodChannel.Result
  private lateinit var resultLauncher: ActivityResultLauncher<Intent>

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_tj_pay")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    this.result = result

    when (call.method) {
      "INIT_SERVICE" -> {
        /** Configures the TJPay application to be able to communicate with Junction@POS and the device hardware **/

        val intent: Intent = Intent("za.co.transactionjunction.tjpay.android.INIT_SERVICE")
          .also {
            it.setClassName(
              "za.co.transactionjunction.tjpay.android",
              "za.co.transactionjunction.tjpay.android.MainActivity"
            )
          }

        act?.startActivityForResult(intent, triggerTJ)
      }
      "ACTION_SEND" -> {
        /** Brings the TJPay application to the foreground and start a transaction **/

        val cardReq = call.arguments as Map<String, Any>
        val cardReqBundle: Bundle = TJPayBundle().apply {
          put(cardReq)
        }.toBundle()
        val intent: Intent = Intent("android.intent.action.SEND")
          .also {
            it.setClassName(
              "za.co.transactionjunction.tjpay.android",
              "za.co.transactionjunction.tjpay.android.MainActivity"
            )
            it.addCategory(Intent.CATEGORY_DEFAULT)
            it.setType("text/plain")
            it.putExtra("cardReq", cardReqBundle)

            it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            it.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }

        val chooser = Intent.createChooser(intent, "Choose a payment method")
        chooser.setFlags(
          Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                  Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or
                  Intent.FLAG_ACTIVITY_CLEAR_TOP
        )

        try {
          resultLauncher.launch(chooser)
        } catch (e: ActivityNotFoundException) {
          Log.e(loggingTag, "Unable to start application.", e)
        }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    act = binding.activity

    if (binding.activity is ComponentActivity) {
      val componentActivity = binding.activity as ComponentActivity
      resultLauncher = componentActivity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
      ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
          // check request code also?? (requestCode == TRIGGER_TJ)
          val resultMap = mutableMapOf<String, Any>()
          val bundle = result.data?.extras
          bundle?.keySet()?.forEach { key ->
            val value = bundle.get(key)
            resultMap[key] = value ?: "null"
          }
          this.result.success(resultMap)
        }
      }
    } else {
      Log.e(loggingTag, "Activity is not a ComponentActivity, cannot use registerForActivityResult")
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    act = null;
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    act = binding.activity
  }

  override fun onDetachedFromActivity() {
    act = null;
  }


  class TJPayBundle {
    private val bundle = Bundle()

    fun put(values: Map<String, Any>) {
      for ((key, value) in values) {
        when (value) {
          is String -> bundle.putString(key, value)
          is Int -> bundle.putInt(key, value)
          is Byte -> bundle.putByte(key, value)
          is Char -> bundle.putChar(key, value)
          is Short -> bundle.putShort(key, value)
          is Double -> bundle.putDouble(key, value)
          is Boolean -> bundle.putBoolean(key, value)
          else -> throw IllegalArgumentException("Unsupported type for value $value with key $key")
        }
      }
    }

    fun toBundle(): Bundle {
      return bundle
    }
  }
}
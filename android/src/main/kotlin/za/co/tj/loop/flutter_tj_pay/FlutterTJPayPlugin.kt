package za.co.tj.loop.flutter_tj_pay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/** FlutterTJPayPlugin **/
class FlutterTJPayPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {

  private val loggingTag = "TJPayPlugin"
  private var activity: FlutterFragmentActivity? = null
  private lateinit var channel: MethodChannel
  private lateinit var result: MethodChannel.Result
  private lateinit var resultLauncher: ActivityResultLauncher<Intent>

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(loggingTag, "onAttachedToEngine called!")
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_tj_pay")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    Log.d(loggingTag, "onMethodCall called!")
    this.result = result

    when (call.method) {
      "INIT_SERVICE" -> {
        try {
          /** Configures the TJPay application to be able to communicate with Junction@POS and the device hardware **/

          val intent: Intent = Intent("za.co.transactionjunction.tjpay.android.INIT_SERVICE")
            .also {
              it.setClassName(
                "za.co.transactionjunction.tjpay.android",
                "za.co.transactionjunction.tjpay.android.MainActivity"
              )
            }

          // Start activity using the ActivityResultLauncher
          activity?.startActivity(intent)
          result.success("Service initialized successfully.")
        } catch (e: Exception) {
          Log.e(loggingTag, "Unable to start application.", e)
          result.error("UNABLE_TO_START", "Unable to start application.", null)
        }
      }
      "ACTION_SEND" -> {
        /** Brings the TJPay application to the foreground and start a transaction **/

        val cardReq = call.arguments as Map<*, *>
        Log.d(loggingTag, "cardReq: $cardReq")

        val cardReqBundle: Bundle = TJPayBundle().apply {
          put(cardReq)
        }.toBundle()

        // Log all key-value pairs in the bundle
        for (key in cardReqBundle.keySet()) {
          val value = cardReqBundle.get(key)
          Log.d(loggingTag, "cardReqBundle[$key] = $value (${value?.javaClass?.simpleName})")
        }

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

        // Create chooser and launch
        val chooser = Intent.createChooser(intent, "Choose a payment method")
        chooser.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT or
                Intent.FLAG_ACTIVITY_CLEAR_TOP)

        resultLauncher.launch(chooser)
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    Log.d(loggingTag, "onDetachedFromEngine called!")
    channel.setMethodCallHandler(null)
  }

  // Handle result from launched activity
  private fun handleResult(resultCode: Int, data: Intent?) {
    if (!::result.isInitialized) {
      Log.e(loggingTag, "Result object is not initialized!")
      return
    }

    if (data == null) {
      Log.e(loggingTag, "Intent data is null!")
      result.error("NO_DATA_RECEIVED", "No data received from the activity.", null)
      return
    }

    if (resultCode == Activity.RESULT_OK) {
      Log.d(loggingTag, "Transaction completed successfully.")
      val resultMap = data.extras?.let { bundleToMap(it) } ?: mutableMapOf<String, Any>()

      for ((key, value) in resultMap) {
        if (value is Map<*, *>) {
          for ((nestedKey, nestedValue) in value) {
            Log.d(loggingTag, "resultMap[$key][$nestedKey] = $nestedValue (${nestedValue?.javaClass?.simpleName})")
          }
        } else {
          Log.d(loggingTag, "resultMap[$key] = $value (${value?.javaClass?.simpleName})")
        }
      }

      Log.d(loggingTag, "data response: $resultMap")
      result.success(resultMap)
    } else {
      Log.e(loggingTag, "Transaction failed.")
      result.error("TRANSACTION_FAILED", "Transaction failed.", null)
    }
  }

    private fun bundleToMap(bundle: Bundle?): Map<String, Any?> {
      val map = mutableMapOf<String, Any?>()
      if (bundle == null) return map

      for (key in bundle.keySet()) {
          when (val value = bundle.get(key)) {
              is Bundle -> map[key] = bundleToMap(value)
              is ArrayList<*> -> map[key] = value.toList()
              is Int, is Boolean, is Double, is String, is Float, is Long, is Byte, is Short -> map[key] = value
              else -> map[key] = value?.toString()
          }
      }
      return map.toMap() // Ensure immutable map
    }

  // Initialize the result launcher
  private fun initializeResultLauncher(activity: FlutterFragmentActivity) {
    resultLauncher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      handleResult(result.resultCode, result.data)
    }
  }

  // Attach activity and initialize the result launcher
  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    Log.d(loggingTag, "onAttachedToActivity called!")
    activity = binding.activity as? FlutterFragmentActivity
    initializeResultLauncher(activity!!)
    Log.d(loggingTag, "onAttachedToActivity COMPLETED!")
  }

  override fun onDetachedFromActivityForConfigChanges() {
    Log.d(loggingTag, "onDetachedFromActivityForConfigChanges called!")
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    Log.d(loggingTag, "onReattachedToActivityForConfigChanges called!")
    activity = binding.activity as FlutterFragmentActivity
    initializeResultLauncher(activity!!)
  }

  override fun onDetachedFromActivity() {
    Log.d(loggingTag, "onDetachedFromActivity called!")
    activity = null
  }

  class TJPayBundle {
    private val bundle = Bundle()

    fun put(values: Map<*, *>) {
      for ((prop, value) in values) {
        val key = prop as String
        when (value) {
          is String -> bundle.putString(key, value)
          is Int -> bundle.putLong(key, value.toLong())
          is Byte -> bundle.putByte(key, value)
          is Char -> bundle.putChar(key, value)
          is Short -> bundle.putShort(key, value)
          is Double -> bundle.putDouble(key, value)
          is Boolean -> bundle.putBoolean(key, value)
          is ArrayList<*> -> {
            if(value.isNotEmpty()) {
              when (value[0]) {
                is String -> bundle.putStringArrayList(key, value as ArrayList<String>)
                is Int -> bundle.putIntegerArrayList(key, value as ArrayList<Int>)
                else -> throw IllegalArgumentException("Unsupported type for value $value with key $key")
              }
            }
          }
          is Array<*> -> {
            if(value.isNotEmpty()) {
              when (value[0]) {
                is String -> bundle.putStringArray(key, value as Array<String>)
                is Int -> bundle.putIntArray(key, value as IntArray)
                else -> throw IllegalArgumentException("Unsupported type for value $value with key $key")
              }
            }
          }
          else -> throw IllegalArgumentException("Unsupported type for value $value with key $key")
        }
      }
    }

    fun toBundle(): Bundle {
      return bundle
    }
  }
}

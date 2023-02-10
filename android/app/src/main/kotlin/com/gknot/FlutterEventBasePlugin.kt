package com.gknot

import android.content.Intent
import android.util.Log
import com.gknot.pseudo.BasePseudoFlutterActivity
import io.flutter.plugin.common.MethodChannel

private const val MODULE_NAME: String = "eventChannel";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

interface ChannelDart : MethodChannel.Result {
   val target: BasePseudoFlutterActivity
   var cb: ((Any?) -> Unit)?
   var methodname: String?
   override fun notImplemented() {
      Log.e("Err.Dart", "$methodname Method NotImplemented on target: ${target::class.java.name}",
           NotImplementedError("Dart method: $methodname not implemented"))
   }
   override fun error(p0: String?, p1: String?, p2: Any?) {
      Log.e("Err.Dart", "Errors occurred while invoking method: $methodname on target: ${target::class.java.name} \np0:$p0, p1:$p1, p2:$p2")
   }
   override fun success(p0: Any?) {
      if (cb != null) cb!!(p0)
   }
}

interface ChannelPlatform : MethodChannel.Result {
   val target: BasePseudoFlutterActivity
   var cb: ((Any?) -> Unit)?
   var methodname: String?
   override fun notImplemented() {
      Log.e("Err.Platm", "$methodname Method NotImplemented target: ${target::class.java.name}")
   }
   override fun error(p0: String?, p1: String?, p2: Any?) {
      Log.e("Err.Platm", "$methodname Errors occurred while invoking method:\np0:$p0, p1:$p1, p2:$p2")
   }
   override fun success(p0: Any?) {
      if (cb != null) cb!!(p0)
   }
}


/*
class BaseEventPlugin(private val registrar: Registrar, val channel: EventChannel)
    : BroadcastReceiver(), EventChannel.StreamHandler, IntentAware
{
    var intent_filter: IntentFilter = IntentFilter();
    var mintent: Intent?= null;
    var eventSink: EventChannel.EventSink? = null;
    *//*
    *       interface for flutter PluginRegistry
    *
    * *//*
    companion object {
        @JvmStatic
        val CHANNEL:String = CHANNEL_NAME;

        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar): Unit {
            val channel = EventChannel(registrar.messenger(), CHANNEL_NAME)
            val instance = BaseEventPlugin(registrar, channel);
            channel.setStreamHandler(instance)
            registrar.addNewIntentListener(instance);
        }
    }
    *//*
            initialize intentFilter for to be used in onnNewIntent handler

    * *//*
    override fun intentFilterInit(){
        intent_filter.addAction("");
    }
    *//*
    *       initialize
    *           - intentFilter
    *           - localBroadCastManager
    *           - registerReceiever
    * *//*
    override fun broadCastInit(){
        intentFilterInit()
        val manager: LocalBroadcastManager = LocalBroadcastManager.getInstance(registrar.context())
        manager.registerReceiver(this, intent_filter)
    }
    *//*
            implement intent matcher here
    *//*
    private fun isIncommingIntentMatched(intent:Intent): Boolean{
        return intent_filter.matchAction(intent.action)
    }
    *//*
            implement how intent data be fetched
            @return: Map<String, Any>
    *//*
    override fun fetchDataByIntent(intent:Intent): Map<String, Any> {
        var ret: Map<String, Any>? = null;
        when(intent.action){
            "Album" -> {
                ret = (intent.getParcelableExtra("Album") as Album).toMap()
            }
        }
        if (ret == null)
            throw ExceptionInInitializerError("");
        return ret;
    }



    *//*
            broadCast handler (receiver)

    *//*
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == null)
            return;
        val ret = fetchDataByIntent(intent);
        if(eventSink != null){
        }
    }

    *//*
            new intent handler
    *//*
    override fun onNewIntent(intent: Intent): Boolean {
        if (isIncommingIntentMatched(intent)){
            registrar.activity().intent = intent;
            return true;
        }
        return false;
    }

    override fun onListen(arguments: Any, sink: EventChannel.EventSink) {
        eventSink = sink;
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null;
    }
}*/



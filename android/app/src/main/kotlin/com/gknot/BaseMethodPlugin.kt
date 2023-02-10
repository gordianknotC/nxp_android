package com.gknot

import android.content.Intent
import android.content.IntentFilter

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

import com.gknot.pseudo.IntentAware
import com.gknot.datatypes.Album
import com.gknot.datatypes.REQUEST_CODE

private const val MODULE_NAME: String = "methodChannel";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";


// deprecate, fixme:

open class BaseMethodPlugin(var registrar: Registrar, var channel: MethodChannel)
    : IntentAware, MethodCallHandler
{
    var intent_filter: IntentFilter = IntentFilter();
    var mintent: Intent?= null;
    /*
    *       interface for flutter PluginRegistry
    *
    * */
    companion object {
        @JvmStatic
        val CHANNEL:String = CHANNEL_NAME;

        @JvmStatic
        fun registerWith(registrar: PluginRegistry.Registrar): Unit {
            val channel = MethodChannel(registrar.messenger(), CHANNEL)
            val instance = BaseMethodPlugin(registrar, channel);
            channel.setMethodCallHandler(instance)
            registrar.addNewIntentListener(instance);
            registrar.addActivityResultListener(instance);

        }
    }


    /*
            implement how intent data be fetched
            @return: Map<String, Any>
    */
    override fun processIntentOnReceive(intent:Intent, requestCode: Int?): Boolean{
        //todo:
        var ret: Map<String, Any>? = null;
        when(intent.action){
            "Album" -> {
                ret = (intent.getParcelableExtra("Album") as Album).toMap()
            }
        }
        channel.invokeMethod(intent.action, ret);
        return true;
    }
    /*
            new intent handler
    */
    override fun onNewIntent(intent: Intent): Boolean {
        return true
    }
    /*
            activity result handler
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
        when (requestCode) {
            //todo: fixme:
            REQUEST_CODE.readNDEF.id -> print("readNDEF.id")
            else -> return false
        }
        return true
    }
    /*
        method handler for handling method calls from dart
    */
    override fun onMethodCall(call: MethodCall, result: Result): Unit {
        print("onMethodCall: ${call.method}");
        when (call.method) {
            //todo: fixme:
        }
    }



}








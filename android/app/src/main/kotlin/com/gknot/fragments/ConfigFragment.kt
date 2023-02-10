package com.gknot.fragments

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.activities.*
import com.gknot.pseudo.BasePseudoFlutterActivity

import com.gknot.datatypes.CompNamePseudo

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "ConfigFragment";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";


class ConfigFragment(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {
   /*
   *     calls from Platform to Dart or constants passed from Platform to Dart
   * */
   private class Dart (){
      companion object {
      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   * */
   private class Platform (){
      companion object {
         //consts
         const val READ_MEMORY:String = "readMemory";
         const val RESET_MEMORY:String = "ResetMemoryActivity";
         const val CONFIG_SESSION_REGISTER:String = "configSessionRegister";
         const val CONFIG_REGISTER:String = "configRegister";

         //methods
         const val onReadMemoryClick:String = "onReadMemoryClick"
         fun onReadMemoryClick(call:MethodCall, result:MethodChannel.Result){
            instance?.onXClick(Platform.READ_MEMORY)
         }
         const val onResetMemoryActivityClick:String = "onResetMemoryActivityClick"
         fun onResetMemoryActivityClick(call:MethodCall, result:MethodChannel.Result){
            instance?.onXClick(Platform.RESET_MEMORY)
         }
         const val onConfigSessionRegisterClick:String = "onConfigSessionRegisterClick"
         fun onConfigSessionRegisterClick(call:MethodCall, result:MethodChannel.Result){
            instance?.onXClick(Platform.CONFIG_SESSION_REGISTER)
         }
         const val onConfigRegisterClick:String = "onConfigRegisterClick"
         fun onConfigRegisterClick(call:MethodCall, result:MethodChannel.Result){
            instance?.onXClick(Platform.CONFIG_REGISTER)
         }

      }
   }
   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity)
   }

   fun onXClick(id:String){
      var intent: Intent? = null
      val activity = registrar.activity()
      when (id) {
         Platform.READ_MEMORY -> intent = Intent(activity, ReadMemoryActivity::class.java)
         Platform.RESET_MEMORY -> intent = Intent(activity, ResetMemoryActivity::class.java)
         Platform.CONFIG_SESSION_REGISTER -> intent = Intent(activity, RegisterSessionActivity::class.java)
         Platform.CONFIG_REGISTER -> intent = Intent(activity, RegisterConfigActivity::class.java)
      }
      if (intent == null)
         throw ExceptionInInitializerError("Intent should not be null")
      val mintent = PseudoMainActivity.getmIntent()
      if (mintent != null)
         intent.putExtras(mintent)
      SharedCodes.log("CfgFrag", "onXClick");
      ActivityFlutterMediator.startActivity(intent)
   }

   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      when (call.method) {
         Platform.onConfigRegisterClick ->
            Platform.onConfigRegisterClick(call, result)
         Platform.onConfigSessionRegisterClick ->
            Platform.onConfigSessionRegisterClick(call, result)
         Platform.onReadMemoryClick ->
            Platform.onReadMemoryClick(call, result)
         Platform.onResetMemoryActivityClick ->
            Platform.onResetMemoryActivityClick(call, result)
         else ->
            throw NoSuchMethodException("method: ${call.method} not found")
      }
   }

   companion object {
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      var instance: ConfigFragment? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = ConfigFragment(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = ConfigFragment::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name)  ?:
            throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as ConfigFragment, component, intent_filters)
         SharedCodes.log("CfgFrag", "registerPlugin")
      }
   }
}

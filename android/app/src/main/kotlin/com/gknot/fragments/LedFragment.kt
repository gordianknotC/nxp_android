package com.gknot.fragments

import kotlin.experimental.and

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.activities.SharedCodes
import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.nxp.ntagi2cdemo.R

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "LedFragment";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";




class LedFragment(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {
   /*
   *     calls from Platform to Dart or constants passed from Platform to Dart
   */
   private class Dart (){
      companion object {
         // consts
         const val PRESSED: String = "pressed"
         // -----------
         const val TEXT_CB: String = "textCallback"
         const val TRANSFER_DIR: String = "texttransferDir"
         // -----------
         const val VOLTAGE: String = "voltage"
         const val TEMP_C: String = "temperatureC"
         const val TEMP_F: String = "temperatureF"
         // methods
         const val SET_VOLTAGE: String = "setVoltage"
         const val SET_TEMPC: String =  "setTemperatureC"
         const val SET_TEMPF: String = "setTemperatureF"
         const val SET_TRANSFER_DIR: String = "setTransferDirection"
         const val UPDATE_TO_UI: String = "updateToUi"

         fun setVoltage(value:Double){
            // set from Ntag_Demo
            LedFragment.ch?.invokeMethod(SET_VOLTAGE, value)
         }
         fun setTemperatureC(value: Double){
            // set from Ntag_Demo
            LedFragment.ch?.invokeMethod(SET_TEMPC, value)
         }
         fun setTemperatureF(value:Double){
            // set from Ntag_Demo
            LedFragment.ch?.invokeMethod(SET_TEMPF, value)
         }
         fun setTransferDirection(answer: String){
            // set from Ntag_Demo
            LedFragment.ch?.invokeMethod(SET_TRANSFER_DIR, answer)
         }
         fun updateToUi(data: Map<String, Any>){
            // set from Ntag_Demo
            LedFragment.ch?.invokeMethod(UPDATE_TO_UI, data);
         }

      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   */
   private class Platform (){
      companion object {
         // methods
         const val UPDATE_TO_PLATFORM: String = "updateToPlatform"
         // consts
         const val PRESSED: String = "pressed"
         const val LCD_CHECK: String = "lcdCheck"
         const val TEMP_CHECK: String = "tempCheck"
         const val SCROLL_CHECK: String = "Scroll_check"
         // -----------
         const val OPTION: String = "option"
         const val LAST_OPTION: String = "lastOption"


         /*
         // followings are ignored since it's send from platform to dart
         textCallback("textCallback"),
         pressed("pressed"),
         voltage("voltage"),
         temperatureC("temperatureC"),
         temperatureF("temperatureF"),
         texttransferDir("texttransferDir"),*/
         fun updateToPlatform(call: MethodCall, result: MethodChannel.Result){
            LedFragment.updateToPlatform(call, result)
         }
      }
   }
   private var isSwitchedOn: Boolean = false

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity)
      voltage = 0.0
      temperatureC = 0.0
      temperatureF = 0.0
      // We start with L2 so that Blue LED is switched on
      option = blueButton
      lastOption = blueButton
      isSwitchedOn = true
      pressed = 0;
      initVariables()
   }

   private fun initVariables() {
      lcdCheck = false
      tempCheck = false
      Scroll_check = false
      textCallback = R.id.textCallback.toString()
      texttransferDir = R.id.TransferDirection.toString()
   }

   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      when (call.method) {
         Platform.UPDATE_TO_PLATFORM ->
            Platform.updateToPlatform(call, result)
         else ->
            throw NoSuchMethodException("method: ${call.method} not found")
      }
   }
   companion object {
      private var pressed: Int = 0;
      private var lcdCheck: Boolean = false
      private var tempCheck: Boolean = false
      private var Scroll_check: Boolean = false

      private var textCallback: String = ""
      private var texttransferDir: String = ""
      private var offButton:String = "L0";
      private var redButton:String = "L1";
      private var blueButton:String = "L2";
      private var greenButton:String = "L4";
      @JvmStatic var option: String = blueButton
         private set
      private var lastOption: String = blueButton

      @JvmStatic var voltage: Double = 0.toDouble()
      set(value) {
         field = value
         Dart.setVoltage(value)
      }
      @JvmStatic var temperatureC: Double = 0.toDouble()
      set(value) {
         field = value
         Dart.setTemperatureC(value)
      }
      @JvmStatic var temperatureF: Double = 0.toDouble()
      set(value) {
         field = value
         Dart.setTemperatureF(value)
      }
      @JvmStatic fun setTransferDir(answer: String) {
         texttransferDir = answer
         Dart.setTransferDirection(answer)
      }
      @JvmStatic fun setButton(Bit_field: Byte) {
         pressed = 0
         val a: Byte = 0x01;
         val b: Byte = 0x02;
         val c: Byte = 0x04
         if ((Bit_field and a) == a) {
            pressed += 1
         }
         if (Bit_field and b == b) {
            pressed += 2
         }
         if (Bit_field and c == c) {
            pressed += 4
         }
      }

      @JvmStatic val isScrollEnabled: Boolean
         get() = Scroll_check

      @JvmStatic val isLCDEnabled: Boolean
         get() = lcdCheck

      @JvmStatic val isTempEnabled: Boolean
         get() = tempCheck

      @JvmStatic
      fun setAnswer(answer: String) {
         textCallback = answer
      }
      @JvmStatic
      fun updateToUi(){
         // 📖   : read from ui(dart)
         // ✒️: write on platform
         // 🏳️  : constant String
         val data = mapOf(
           Dart.PRESSED to pressed,
           // 📖   :"lcdCheck" to lcdCheck,
           // 📖   :"tempCheck" to tempCheck,
           // 📖   :"Scroll_check" to Scroll_check,

           Dart.TEXT_CB to textCallback,
           Dart.TRANSFER_DIR to texttransferDir,
           // 📖   :"option" to option,
           // 📖   :"lastOption" to lastOption,

           Dart.VOLTAGE to voltage,
           Dart.TEMP_C to temperatureC,
           Dart.TEMP_F to temperatureF
         )
         Dart.updateToUi(data)
      }
      /*
      *     call from dart
      * */
      @JvmStatic
      fun updateToPlatform(call:MethodCall, result: MethodChannel.Result){
         pressed = call.argument<Int>(Platform.PRESSED) as Int
         lcdCheck = call.argument<Boolean>(Platform.LCD_CHECK) as Boolean
         tempCheck = call.argument<Boolean>(Platform.TEMP_CHECK) as Boolean
         Scroll_check = call.argument<Boolean>(Platform.SCROLL_CHECK) as Boolean

         // ✒️: textCallback = call.argument<String>(Platform.TEXT_CB) as String
         // ✒️: texttransferDir = call.argument<String>(Platform.TRANSFER_DIR) as String
         option = call.argument<String>(Platform.OPTION) as String
         lastOption = call.argument<String>(Platform.LAST_OPTION) as String

         // ✒️: voltage = call.argument<Double>(Platform.VOLTAGE) as Double
         // ✒️: temperatureC = call.argument<Double>(Platform.TEMP_C) as Double
         // ✒️: temperatureF = call.argument<Double>(Platform.TEMP_F) as Double
      }

      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         val instance = LedFragment(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = FlutterRegisterSessionPseudo::class.java.`package`
         val entity_name    = LedFragment::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance, component, intent_filters)
         SharedCodes.log("LedFrag", "registerPlugin")
      }
   }
}
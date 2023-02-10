package com.gknot.fragments


import android.content.ComponentName
import android.content.Intent
import java.io.IOException
import java.io.UnsupportedEncodingException

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.os.Bundle
import android.util.Log
import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.activities.SharedCodes
import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.BasePseudoFlutterActivity

import com.nxp.nfc_demo.activities.MainActivity
import com.nxp.ntagi2cdemo.R
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


private const val MODULE_NAME: String = "SpeedTestFragment";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

class SpeedTestFragment(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {

   private class Dart (){
      companion object {
         //methodds
         const val UPDATE_TO_UI = "updateToUi"
         @JvmStatic fun updateToUi(data:Map<String, Any?>){
            SpeedTestFragment.ch?.invokeMethod(UPDATE_TO_UI, data)
         }
         //consts
         const val OVERHEAD = "overhead"
         const val TEXT_CB = "rfTextCallback"
         const val DATARATE_CB = "rfDatarateCallback"
      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   */
   private class Platform (){
      companion object {
         //methods
         const val UPDATE_TO_PLATFORM = "updateToPlatform"
         @JvmStatic fun updateToPlatform(call: MethodCall, result: MethodChannel.Result ){
            SpeedTestFragment.updateToPlatform(call, result)
         }
         const val ON_MEM_OPTION_CHECKED = "onRfMemOptionChecked"
         @JvmStatic fun onRfMemOptionChecked(call: MethodCall, result: MethodChannel.Result ){
            val option: MemOptions = MemOptions.from(call.argument<String>("MemOptions") as String)
            instance?.onRfMemOptionChecked(option)
         }
         const val START_SPEED_TEST = "onStartSpeedTest"
         @JvmStatic fun onStartSpeedTest(call: MethodCall, result: MethodChannel.Result ){
            instance?.onStartSpeedTest()
         }
         //consts
         const val READ_OPTIONS = "rfReadOptions"
         const val MEM_OPTIONS = "rfMemOptions"
         // ------------------
         const val CHOSEN = "rfChosen"
         const val MEM_CHOSEN = "rfMemChosen"
         const val EDIT_CHAR = "rfEditCharMulti"
         // ------------------
         const val TEXT_CHAR = "rfTextCharMulti"
      }
   }

   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      when (call.method) {
         Platform.UPDATE_TO_PLATFORM ->
            Platform.updateToPlatform(call, result)
         Platform.ON_MEM_OPTION_CHECKED ->
            Platform.onRfMemOptionChecked(call, result)
         Platform.START_SPEED_TEST ->
            Platform.onStartSpeedTest(call, result)
         else ->
            throw NoSuchMethodException("method: ${call.method} not found")
      }
   }

   fun onAfterTextChanged(){
      if (rfMemChosen == false) {
         val resources = registrar.activity().resources
         try {
            val bytes = Integer.parseInt(rfEditCharMulti)
            overhead = eepromCalculateOverhead(bytes)
            if (overhead > 0) {
               rfTextCharMulti = "+ " + overhead + " " + resources.getString(R.string.Block_multipl_eeprom_overhead)
            } else {
               rfTextCharMulti  = resources.getString(R.string.Block_multipl_eeprom)
            }
         } catch (ex: NumberFormatException) {
            rfTextCharMulti = resources.getString(R.string.Block_multipl_eeprom)
            ex.printStackTrace()
         }
      }
   }


   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity)
      val resources = registrar.activity().resources
      // Values per default
      rfMemChosen = true
      rfChosen = true

      /*rfButtonSpeedtest = layout.findViewById<View>(R.id.startSpeedtest) as Button
      rfButtonSpeedtest!!.setOnClickListener(this)
      rfReadOptions = layout.findViewById<View>(R.id.radioReadOptions) as RadioGroup
      rfMemOptions = layout.findViewById<View>(R.id.radioMemoryOptions) as RadioGroup
      rfMemOptions!!.setOnCheckedChangeListener(this)*/

      rfTextCallback = ""
      rfDatarateCallback = ""
      editChar = ""
      rfTextCharMulti = ""
      editChar = "10"
   }



   fun onRfMemOptionChecked(checkedId: MemOptions) {
      if (checkedId == MemOptions.RadioMemoryEeprom) {
         rfMemChosen = false
         val textCharMulti = getrf_ndef_value_charmulti()

         // getting text multiplier
         var chMultiplier = 1
         val chMultiLength = textCharMulti.length
         if (chMultiLength == 0) {
            chMultiplier = 1
         } else {
            chMultiplier = Integer.parseInt(textCharMulti)
         }
         editChar = Integer.toString(chMultiplier * 64)
         // rf_ndef_CharMulti.setText("");
      } else if (checkedId == MemOptions.RadioMemorySram) {
         val resources = registrar.activity().resources
         rfMemChosen = true
         rfTextCharMulti = resources.getString(R.string.Block_multipl_sram)
         val textCharMulti = getrf_ndef_value_charmulti()

         // getting text multiplier
         var chMultiplier = 1
         val chMultiLength = textCharMulti.length
         if (chMultiLength == 0) {
            chMultiplier = 1
         } else {
            chMultiplier = Integer.parseInt(textCharMulti)
         }
         editChar = Integer.toString(chMultiplier / 64)
      }
   }

   private fun eepromCalculateOverhead(bytes: Int): Int {
      overhead = 0

      var messageText = ""
      for (i in 0 until bytes) {
         messageText = "$messageText "
      }

      // Calculate the overhead
      val msg: NdefMessage
      try {
         msg = createNdefMessage(messageText)
         var ndef_message_size = msg.toByteArray().size + 5
         ndef_message_size = Math.round((ndef_message_size / 4).toFloat()) * 4
         overhead = ndef_message_size - bytes
      } catch (e: UnsupportedEncodingException) {
         e.printStackTrace()
         SharedCodes.log("sfrag", e.toString())
      }

      return overhead
   }

   @Throws(UnsupportedEncodingException::class)
   private fun createNdefMessage(text: String): NdefMessage {
      val lang = "en"
      val textBytes = text.toByteArray()
      val langBytes = lang.toByteArray(charset("US-ASCII"))
      val langLength = langBytes.size
      val textLength = textBytes.size
      val payload = ByteArray(1 + langLength + textLength)
      payload[0] = langLength.toByte()
      System.arraycopy(langBytes, 0, payload, 1, langLength)
      System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

      val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN,
              NdefRecord.RTD_TEXT, ByteArray(0), payload)
      val records = arrayOf(record)
      return NdefMessage(records)
   }

   private fun StartEEPROMSpeedTest() {
      if (MainActivity.demo.isReady && MainActivity.demo.isConnected) {
         MainActivity.demo.finishAllTasks()
         try {
            MainActivity.demo.EEPROMSpeedtest()
         } catch (e: IOException) {
            e.printStackTrace()
         } catch (e: FormatException) {
            e.printStackTrace()
            SharedCodes.log("sfrag",e.toString())
         }

      }
   }

   private fun StartSRAMSpeedTest() {
      if (MainActivity.demo.isReady && MainActivity.demo.isConnected) {
         MainActivity.demo.finishAllTasks()
         try {
            MainActivity.demo.SRAMSpeedtest()
         } catch (e: IOException) {
            e.printStackTrace()
            SharedCodes.log("sfrag", e.toString())
         } catch (e: FormatException) {
            e.printStackTrace()
            SharedCodes.log("sfrag", e.toString())
         }

      }
   }

   fun onStartSpeedTest() {
      if (rfMemChosen) {
         StartSRAMSpeedTest()
      } else {
         StartEEPROMSpeedTest()
      }
   }

   companion object {
      enum class ReadOptions(val data:String) {
         RadioFastMode("radioFastMode"),
         RadioPollingMode("radioPollingMode");

         companion object {
            fun from(findValue: String): ReadOptions =
                 ReadOptions.values().first { it.data == findValue }
         }
      }
      enum class MemOptions(val data:String) {
         RadioMemorySram("radioMemorySram"),
         RadioMemoryEeprom("radioMemoryEeprom");

         companion object {
            fun from(findValue: String): MemOptions =
                    MemOptions.values().first { it.data == findValue }
         }
      }
      private var overhead: Int = 0;
      private var rfReadOptions: ReadOptions = ReadOptions.from(ReadOptions.RadioFastMode.data)
      private var rfMemOptions: MemOptions = MemOptions.from(MemOptions.RadioMemorySram.data)
      @JvmStatic var memOptions: MemOptions
         get() = rfMemOptions
         set(value){
            instance?.onRfMemOptionChecked(value)
            rfMemOptions = value
         }
      private var rfTextCallback: String = ""
      private var rfDatarateCallback: String = ""
      private var rfChosen:Boolean = false
      private var rfMemChosen:Boolean = false
      private var rfEditCharMulti: String = ""
      @JvmStatic var editChar: String
         get() = rfEditCharMulti
         set(value){
            instance?.onAfterTextChanged()
            rfEditCharMulti = value
         }
      private var rfTextCharMulti: String = ""

      @JvmStatic fun getrf_ndef_value_charmulti(): String {
         return rfEditCharMulti
      }

      @JvmStatic val isSRamEnabled: Boolean?
         get() = rfMemChosen

      @JvmStatic fun setAnswer(answer: String) {
         rfTextCallback = answer

         // Reset datarate textview
         rfDatarateCallback = ""
      }

      @JvmStatic val readOptions: String
         get() {
            return rfReadOptions.data
         }

      @JvmStatic fun setReadOptions(option: ReadOptions) {
         rfReadOptions = option
      }

      @JvmStatic var datarateCallback: String
         get() = rfDatarateCallback
         set(datarate) {
            rfDatarateCallback = datarate
         }


      @JvmStatic
      fun updateToUi(){
         // 📖   : read from ui(dart)
         // ✒️: write on platform
         // 🏳️  : constant String
         val data:Map<String, Any?> = mapOf(
            Dart.OVERHEAD to overhead,
           // 📖   : "rfReadOptions" to rfReadOptions,
           // 📖   : "rfMemOptions" to rfMemOptions,
            Dart.TEXT_CB to rfTextCallback,

            Dart.DATARATE_CB to rfDatarateCallback
           // 📖   : "rfChosen" to rfChosen,
           // 📖   : "rfMemChosen" to rfMemChosen,
           // 📖   : "rfEditCharMulti" to rfEditCharMulti,

           // 📖   : "rfTextCharMulti" to rfTextCharMulti
         );
         SpeedTestFragment.Dart.updateToUi(data)
      }
      /*
      *     call from dart
      */
      @JvmStatic
      fun updateToPlatform(call: MethodCall, result: MethodChannel.Result){
         // ✒️: overhead = call.argument<Int>("overhead") as Int
         rfReadOptions = ReadOptions.from(call.argument<String>(Platform.READ_OPTIONS) as String)
         rfMemOptions = MemOptions.from(call.argument<String>(Platform.MEM_OPTIONS) as String)
         // ✒️: rfTextCallback = call.argument<String>("rfTextCallback") as String

         // ✒️: rfDatarateCallback = call.argument<String>("rfDatarateCallback") as String
         rfChosen = call.argument<Boolean>(Platform.CHOSEN) as Boolean
         rfMemChosen = call.argument<Boolean>(Platform.MEM_CHOSEN) as Boolean
         rfEditCharMulti = call.argument<String>(Platform.EDIT_CHAR) as String

         rfTextCharMulti = call.argument<String>(Platform.TEXT_CHAR) as String
      }
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      var instance:SpeedTestFragment? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = SpeedTestFragment(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = FlutterRegisterSessionPseudo::class.java.`package`
         val entity_name    = SpeedTestFragment::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name)  ?:
            throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as SpeedTestFragment, component, intent_filters)
         SharedCodes.log("SpdFrag", "registerPlugin")
      }
   }
}

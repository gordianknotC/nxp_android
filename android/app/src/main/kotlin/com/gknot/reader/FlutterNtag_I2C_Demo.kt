package com.gknot.reader

import android.content.Context
import android.nfc.Tag
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.gknot.BASE_NAME
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.nfc.NfcAdapter
import android.os.Vibrator
import com.gknot.activities.AuthActivity
import com.gknot.activities.PseudoMainActivity
import com.gknot.activities.SharedCodes
import com.gknot.fragments.NdefFragment
import com.gknot.fragments.NdefFragment.Companion.WriteOptions
import com.gknot.pseudo.*
import com.nxp.nfc_demo.reader.Ntag_Get_Version
import java.nio.charset.StandardCharsets
import android.nfc.NfcManager
import android.content.pm.PackageManager
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Handler
import android.os.Looper
import com.gknot.activities.NtagRawRecord
import com.nxp.nfc_demo.exceptions.CommandNotSupportedException
import android.util.Log
import com.gknot.MainActivity
import com.nxp.nfc_demo.reader.MinimalNtag_I2C_Commands


private const val MODULE_NAME: String = "FlutterNtag_I2C_Demo";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

internal class MethodResultWrapper(private val methodResult: MethodChannel.Result) : MethodChannel.Result {
   private var handler: Handler = Handler(Looper.getMainLooper())

   override fun notImplemented() {
      handler.post { methodResult.notImplemented() }
   }

   override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
      handler.post { methodResult.error(errorCode, errorMessage, errorDetails) }
   }

   override fun success(result: Any?) {
      handler.post { methodResult.success(result) }



   }

}

/*
*
*     Call Ntag_I2C_Demo from Dart not the other way round
*
* */
class FlutterNtag_I2C_Demo(registrar: Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel), SetBoardVersionListener, ShowAlertListener,
        ShowToastListener, UIDelegateListener, InstanceDelegateListener, OnWriteEEPROMListener,
        OnWriteSRAMListener, OnNDEFReadListener, OnNDEFWriteListener {


   // dispatch all ui message into "uiSender"
   val uiSender: Message = Message(registrar, channel)
   // delegate Ntag_I2C_Demo
   var host: Ntag_I2C_Demo? = null
   // constants mapped to public method names of Ntag_I2C_Demo
   private val getLog  : String = "getLog";
   private val crash   : String = "crash";
   private val miniNtagOnly: String = "miniNtagOnly";
   private val flutterReady: String = "flutterReady";
   private val dartBytesToPlatformString: String = "dartBytesToPlatformString";
   private val resetRegister: String = "resetRegister";
   private val onJavaException: String = "onJavaException";
   private val resetRegisterCustomBytes  : String = "resetRegisterCustomBytes";
   private val writeEEPROMCustomBytes    : String = "writeEEPROMCustomBytes";
   private val readEEPROMCustomBytes     : String = "readEEPROMCustomBytes";
   private val scanFile                  : String = "scanFile";

   private val checkNFC: String = "checkNFC";
   private val checkNFC2: String = "checkNFC2";
   private val checkNFC3: String = "checkNFC3";
   private val checkNFC4: String = "checkNFC4";
   private val onLogToDart: String = "onLogToDart";
   private val obtainAuthStatus:String = "obtainAuthStatus";
   private val doProcessOnExistingTagWithoutAuthCheck: String = "doProcessOnExistingTagWithoutAuthCheck";
   val isReady: String = "isReady";
   val isConnected: String = "isConnected";
   val isTagPresent: String = "isTagPresent";
   private val setFactoryPassword: String = "setFactoryPassword";
   private val setPassword: String = "setPassword";
   private val clearAuth:String = "clearAuth";
   private val isAuthUnProtected:String = "isAuthUnProtected";
   private val isSRAMAuthUnProtected:String = "isSRAMAuthUnProtected";
   private val isReadAuthUnProtected:String = "isReadAuthUnProtected";
   private val isAuthProtected:String = "isAuthProtected";

   private val vibrate: String = "vibrate";
   private val finishAllTasks: String = "finishAllTasks";
   private val getProduct: String = "getProduct";
   private val setBoardVersion: String = "setBoardVersion";
   private val resetTagMemory: String = "resetTagMemory";
   private val readSessionRegisters: String = "readSessionRegisters";
   private val readSessionRegistersCustom: String = "readSessionRegistersCustom";
   private val readWriteConfigRegister: String = "readWriteConfigRegister";
   private val readTagContent: String = "readTagContent";
   private val resetTagContent: String = "resetTagContent";
   private val LED: String = "LED";
   private val LEDFinish: String = "LEDFinish";
   private val NDEFReadFinish: String = "NDEFReadFinish";
   private val NDEF: String = "NDEF";
   private val readNDEF: String = "readNDEF";
   private val readNDEFCustom:String = "readNDEFCustom";
   private val writeNDEF: String = "writeNDEF";

   private val Flash: String = "Flash";
   private val Auth: String = "Auth";
   private val SRAMSpeedtest: String = "SRAMSpeedtest";
   private val SRAMSpeedFinish: String = "SRAMSpeedFinish";
   private val EEPROMSpeedtest: String = "EEPROMSpeedtest";
   private val EEPROMSpeedFinish: String = "EEPROMSpeedFinish";
   private val WriteEmptyNdefFinish: String = "WriteEmptyNdefFinish";
   private val WriteDefaultNdefFinish: String = "WriteDefaultNdefFinish";
   private val onWriteEEPROM: String = "onWriteEEPROM";
   private val onWriteSRAM: String = "onWriteSRAM";
   private val ObtainAuthStatus: String = "ObtainAuthStatus";

   private val NO_NFC_DEVICE: String = "NO_NFC_DEVICE";
   private val NFC_DISABLED: String  = "NFC_DISABLED";
   private val NFC_AVAILABLE: String = "NFC_AVAILABLE";
   private val HAS_NFC_FUNCTIONALITY: String = "HAS_NFC_FUNCTIONALITY";
   private val HAS_NONFC_FUNCTIONALITY: String = "HAS_NONFC_FUNCTIONALITY";


   companion object {
      fun memStringToBytes(string:String):ByteArray{
         return string.toByteArray();
      }

      fun memStringToPrintableByte(string:String):String{
         return string.toByteArray().joinToString(",");
      }

      @JvmStatic
      val CHANNEL: String = CHANNEL_NAME;
      @JvmStatic
      var instance: FlutterNtag_I2C_Demo? = null;
      @JvmStatic
      val demo: Ntag_I2C_Demo
         get() = instance?.host!!

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = FlutterNtag_I2C_Demo(registrar, channel);
         channel.setMethodCallHandler(instance)
         //registrar.addNewIntentListener(instance);
         // direct setBoardVersion into dart
         Ntag_I2C_Demo.setBoardVersionHandler(instance)
         // direct ui alert into dart
         Ntag_I2C_Demo.setShowAlertHandler(instance)
         Ntag_I2C_Demo.setShowOkDialogueHandler(instance)
         Ntag_I2C_Demo.setShowOkDialogueForActionHandler(instance)
         Ntag_I2C_Demo.setToastMakeTextHandler(instance)
         Ntag_I2C_Demo.setInstanceDelegateHandler(instance)
         Ntag_I2C_Demo.setOnWriteEEPROMHandler(instance)
         Ntag_I2C_Demo.setOnWriteSRAMHandler(instance)

      }
   }

   private fun hostIsTagPresent(tag: Tag?): Boolean {
      if (tag == null) {
         val _tag = registrar.activity().intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
         if (_tag != null) {
            val result: Boolean = Ntag_I2C_Demo.isTagPresent(_tag)
            SharedCodes.log(SharedCodes.libTagName(this::class.java), "isTagPresent1: $result");
            return result
         }
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "isTagPresent2: false");
         return false
      }
      val result: Boolean = Ntag_I2C_Demo.isTagPresent(tag)
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "isTagPresent3: $result");
      return result
   }

   override fun onNDEFWrite(bytes: Int) {
      SharedCodes.log("onNDEFWrite", bytes.toString())
   }

   override fun onNDEFRead(bytes: Int) {
      SharedCodes.log("onNDEFRead", bytes.toString())
   }

   override fun onWriteSRAM() {
      SharedCodes.log("onWriteSRAM", "")
   }

   override fun onWriteEEPROM(bytes: Int) {
      SharedCodes.log("onWriteEEPROM", bytes.toString())
   }

   private fun genPassword(text: String): ByteArray {
      return text.substring(0..3).toByteArray(StandardCharsets.US_ASCII)
   }

   fun onNFCServiceDead(){
      uiSender.onNFCServiceDead();
   }



   fun setPassword(text: String, result: MethodChannel.Result) {
      if (demo.isReady && demo.isConnected) {
         val bytes = genPassword(text)
         bytes.toString(StandardCharsets.US_ASCII)
         PseudoMainActivity.password = bytes;
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "setPassword: $bytes")
         result.success(true);
      }else{
         NdefFragment.setAnswer("Tap tag to set password", false, NdefFragment.Dart.TAP_TOWRITE)
         result.error("setPasswrod", "tagNotFound", "");
      }
   }

   fun setFactoryPassword(result: MethodChannel.Result) {
      if (demo.isReady && demo.isConnected) {
         val bytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
         bytes.toString(StandardCharsets.US_ASCII)
         PseudoMainActivity.password = bytes;
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "setFactoryPassword: $bytes");
         result.success(true);
      }else{
         NdefFragment.setAnswer("Tap tag to set password", false, NdefFragment.Dart.TAP_TOWRITE)
         result.error("setPasswrod", "tagNotFound", "");
      }
   }

   fun isAuthUnProtected(mAuthStatus: Int): Boolean {
      return mAuthStatus == AuthActivity.AuthStatus.Disabled.value
              || mAuthStatus == AuthActivity.AuthStatus.Unprotected.value
              || mAuthStatus == AuthActivity.AuthStatus.Authenticated.value
   }

   fun isSRAMAuthUnProtected(mAuthStatus: Int): Boolean {
      return (mAuthStatus == AuthActivity.AuthStatus.Disabled.value
              || mAuthStatus == AuthActivity.AuthStatus.Unprotected.value
              || mAuthStatus == AuthActivity.AuthStatus.Authenticated.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_W.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_RW.value)
   }

   fun isReadAuthUnProtected(mAuthStatus: Int) {
      mAuthStatus == AuthActivity.AuthStatus.Disabled.value
              || mAuthStatus == AuthActivity.AuthStatus.Unprotected.value
              || mAuthStatus == AuthActivity.AuthStatus.Authenticated.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_W.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_W_SRAM.value
   }

   fun isAuthProtected(mAuthStatus: Int): Boolean {
      return (mAuthStatus == AuthActivity.AuthStatus.Protected_RW.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_RW_SRAM.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_W.value
              || mAuthStatus == AuthActivity.AuthStatus.Protected_W_SRAM.value)
   }

   fun readNDEFCustom(call: MethodCall, result: MethodChannel.Result)  {
      try {
         NdefFragment.reset();
         NdefFragment.isWriteChosen = false
         val startAt: ByteArray = call.argument<ByteArray>("startAt")!!;
         val length : Int       = call.argument<Int>("length")!!;
         demo.NDEFReadFinish()
         NtagRawRecord.seekWrittenAt = startAt.first();
         if (demo.isReady && demo.isConnected) {
            SharedCodes.log("Demo",
                    "text: ${NdefFragment.text}, type: ${NdefFragment.ndefType}, " +
                            "aar: ${NdefFragment.isAarRecordSelected}, _aar: false")

            PseudoMainActivity.launchNdefDemoCustom(startAt.first(), byteArrayOf(),length,  result)
         } else {
            NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
         }
      } catch (e: Exception) {
         result.error("readNDEF", "launchNdefDemo failed", "")
         throw Exception("launchNdefDemo failed!\n $e")
      }
   }

   fun readNDEF(call: MethodCall, result: MethodChannel.Result)  {
      try {
         NdefFragment.reset();
         NdefFragment.isWriteChosen = false
         demo.NDEFReadFinish()
         if (demo.isReady && demo.isConnected) {
            demo.finishAllTasks()
            //if (PseudoMainActivity.password == null)
            //   PseudoMainActivity.password = AuthActivity.Pwds.PWD1.value
            //throw ExceptionInInitializerError("password should not be null")
            PseudoMainActivity.launchNdefDemo(
                    PseudoMainActivity.authStatus,
                    PseudoMainActivity.password, result)
            //result.success(true);
         }else{
            NdefFragment.setAnswer("please tap tag to read", false, NdefFragment.Dart.TAP_TOREAD)
            //result.success(false);
         }
      } catch (e: Exception) {
         result.error("readNDEF", "launchNdefDemo failed", "")
         throw Exception("launchNdefDemo failed!\n $e")
      }
   }

   // call from Dart
   @ExperimentalUnsignedTypes
   fun writeNDEF(call: MethodCall, result: MethodChannel.Result)  {
      try {
         val _type = call.argument<String>("type")
                  ?: throw Exception("NdefType should not be null")
         val type = WriteOptions.from(_type)
         val ndef_message_bytes = call.argument<ByteArray>("NdefMessage");
         val ndef_message       = dartBytesToPlatformString(ndef_message_bytes);
         var ndef_bytes:ByteArray = byteArrayOf();
         if (ndef_message_bytes  != null){
            ndef_bytes = ndef_message_bytes;
         }

         SharedCodes.log("writeNDEF", "bytes : ${ndef_message_bytes?.toUByteArray()?.joinToString(",")}");
         SharedCodes.log("writeNDEF", "string: ${ndef_message}");

         writeNDEF(ndef_message, ndef_bytes, type, false, result);
//         result.success()
      } catch (e: Exception) {
         result.error("writeNDEF", "failed to writeNDEF", "" )
         throw Exception("failed to writeNDEF\n $e")
      }

   }

   fun clearAuth(call: MethodCall, result: MethodChannel.Result){
      PseudoMainActivity.authStatus = AuthActivity.AuthStatus.Disabled.value
      PseudoMainActivity.password = null
      return result.success(true);
   }
   fun obtainAuthStatus(call: MethodCall, result: MethodChannel.Result){
      if (demo.isReady && demo.isConnected) {
         val status = demo.ObtainAuthStatus();
         result.success(status);
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "obtainAuth: $status");
      }else{
         NdefFragment.setAnswer("please tap tag to read", false, NdefFragment.Dart.TAP_TOREAD)
         result.error("obtainAuthStatus", "tagNotFound", "");
      }
   }
   fun getLog(call: MethodCall, result: MethodChannel.Result){
      val res:String = SharedCodes.getLog();
      return result.success(res);
   }


   private fun flutterReady(call: MethodCall, result: MethodChannel.Result) {
      val msg = MainActivity.crashed;
      val arg = mapOf(
        "error" to msg
      );
      result.success(arg);
   }

   public fun triggerException(){
      Log.e("main", "launchNdefDemo failed");
      throw Exception("launchNdefDemo failed!");
   }

   fun checkNFC(call: MethodCall, result: MethodChannel.Result){
      SharedCodes.instance.mAdapter = SharedCodes.getNfcAdapter(registrar.activity());
      SharedCodes.log("demo", "checkNFC")
      if (SharedCodes.instance.mAdapter != null){
         if (!SharedCodes.instance.mAdapter!!.isEnabled){
            SharedCodes.log("demo", "checkNFC :: show setting")
//            PseudoMainActivity.Dart(PseudoMainActivity.instance!!, null)
//                    .showSettingNoNfcAlert((PseudoMainActivity.instance as PseudoMainActivity))
            return result.success(NFC_DISABLED);
         }else{
            SharedCodes.log("demo", "checkNFC :: available")
//            PseudoMainActivity.Dart(PseudoMainActivity.instance!!, null)
//                    .nfcAvailable((PseudoMainActivity.instance as PseudoMainActivity))
            return result.success(NFC_AVAILABLE);
         }
      }else{
         SharedCodes.log("demo", "checkNFC :: close app")
//         PseudoMainActivity.Dart(PseudoMainActivity.instance!!, null)
//                 .closeAppNoNfcAlert((PseudoMainActivity.instance as PseudoMainActivity))
         return result.success(NO_NFC_DEVICE);
      }
   }

   fun checkNFC2(call: MethodCall, result: MethodChannel.Result){
      val manager = registrar.context().getSystemService(Context.NFC_SERVICE) as NfcManager
      val adapter = manager.defaultAdapter
      if (adapter == null)
         return result.success(NO_NFC_DEVICE);
      if (!adapter.isEnabled)
         return result.success(NFC_DISABLED);
      return result.success(NFC_AVAILABLE);
   }

   fun checkNFC3(call: MethodCall, result: MethodChannel.Result){
      val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(registrar.activity());
      if (adapter == null)
         return result.success(NO_NFC_DEVICE);
      if (!adapter.isEnabled)
         return result.success(NFC_DISABLED);
      return result.success(NFC_AVAILABLE);
   }

   fun checkNFC4(call: MethodCall, result: MethodChannel.Result){
      val pm = registrar.context().getPackageManager()
      if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
         //This one is returned
         result.success(HAS_NFC_FUNCTIONALITY);
      } else {
         result.success(HAS_NONFC_FUNCTIONALITY);
      }
   }

   fun renewNFC(call: MethodCall, result: MethodChannel.Result){

   }

   fun writeNDEF(data: String, bytes: ByteArray, type: WriteOptions = WriteOptions.RadioNdefText, aar: Boolean = false, result: MethodChannel.Result?, custom:Boolean = false):Boolean {
      NdefFragment.reset();
      NdefFragment.isWriteChosen = true
      NdefFragment.ndefType = type.data
      NdefFragment.setAarRecord(aar)
      NdefFragment.text   = data
      NdefFragment.bytes  = bytes;

      if(data.isEmpty()){
         NdefFragment.text = "no content!";
         NdefFragment.setAnswer("input data is empty",false, NdefFragment.Dart.TAP_TOWRITE)
         //return false;
      }

      if (demo.isReady && demo.isConnected) {
         SharedCodes.log("Demo",
            "text: ${NdefFragment.text}, type: ${NdefFragment.ndefType}, " +
                  "aar: ${NdefFragment.isAarRecordSelected}, _aar: $aar")
         demo.NDEFReadFinish()
         demo.finishAllTasks()
         /*if (PseudoMainActivity.password == null)
            throw ExceptionInInitializerError("password should not be null")*/
         PseudoMainActivity.launchNdefDemo(
                 PseudoMainActivity.authStatus,
                 PseudoMainActivity.password,
                 result)
         return true;
      } else {
         NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
         return false;
      }
   }


   fun writeNDEFCustom(data: String, bytes:ByteArray, type: WriteOptions = WriteOptions.RadioNdefText, aar: Boolean = false, result: MethodChannel.Result?):Boolean {
      SharedCodes.customWrite = true;
      return writeNDEF(data, bytes, type, aar, result);
   }

   fun registerShowAuthDialog(tag: Tag){
      PseudoMainActivity.authStatus = AuthActivity.AuthStatus.Disabled.value
      PseudoMainActivity.password = null
      // revert back if prone errors
//      demo.NDEFReadFinish()
//      if (demo.isReady){
//         demo.finishAllTasks();
//      }
      //RegisterSessionActivity.instance!!.startDemo(tag, false);
      NdefFragment.setAnswer("read failed", false, NdefFragment.Dart.READ_LOST)
   }

   override fun onDelegateInstance(demo: Ntag_I2C_Demo) {
      instance?.host = demo
   }

   @ExperimentalUnsignedTypes
   fun writeEEPROMCustomBytes(call: MethodCall, result: MethodChannel.Result){
      val startAt: ByteArray = call.argument<ByteArray>("startAt")!!;
      val ndef_message_bytes = call.argument<ByteArray>("NdefMessage");
      val extra              = call.argument<HashMap<String, Any>>("extra");
      val writeForCommand:Boolean = extra!!["writeCommand"] as Boolean;
      val message: String    = if (!writeForCommand) "successfully written" else "command written";

      NtagRawRecord.seekWrittenAt = startAt.first();
      SharedCodes.log("Demo", "writeEEPROMCustomBytes startAt $startAt")
      if (demo.isReady && demo.isConnected) {
         try{
            SharedCodes.log("Demo", "writeEEPROMCustomBytes from ${startAt.first()} to $ndef_message_bytes");
            demo.reader.writeEEPROMCustomBytes (ndef_message_bytes, startAt.first());
            NdefFragment.setAnswer (message, false, NdefFragment.Dart.WRITE_SUCCESS)
            SharedCodes.log("Demo", "writeEEPROMCustomBytes bytes written: ${ndef_message_bytes?.toUByteArray()?.joinToString(",")}")
            result.success(true);
         }  catch (e: Exception) {
            NdefFragment.setAnswer("Error writing NDEF\n$e", true, NdefFragment.Dart.WRITE_ERROR);
            e.printStackTrace();
            SharedCodes.Companion.log("Demo", e.toString());
            result.success(mapOf("error" to e.toString()));
         }
      }else{
         NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
         result.success(false);
      }
   }

   @ExperimentalUnsignedTypes
   fun readEEPROMCustomBytes(call: MethodCall, result: MethodChannel.Result){
      val startAt: ByteArray = call.argument<ByteArray>("startAt")!!;
      val length : Int       = call.argument<Int>("length")!!;

      NtagRawRecord.seekWrittenAt = startAt.first();
      if (demo.isReady && demo.isConnected) {
         try{
            val blockSize: Int = demo.reader.blockSize;
            val data: ByteArray;
            val type: String = if (demo.reader is MinimalNtag_I2C_Commands) "mini" else "ntag";
            val extra              = call.argument<HashMap<String, Any>>("extra");
            val readForCommand:Boolean = extra!!["readCommand"] as Boolean;
            val message: String        = if (!readForCommand) "successfully read" else "command read";

            data = demo.reader.readEEPROMCustomBytes(startAt.toUByteArray().first().toInt(), startAt.first() + length / blockSize);
            SharedCodes.log("Demo", "$type.readEEPROMCustomBytes from ${startAt.first()} to ${startAt.first() + length}, " +
                    "\nbytes: ${data.toUByteArray().joinToString(",")}" +
                    "\nlength: ${data.size}, blocSize: $blockSize");


            NdefFragment.setAnswer(message, false, NdefFragment.Dart.WRITE_SUCCESS)
            result.success(mapOf("data" to data));
         } catch( e: CommandNotSupportedException){
            e.printStackTrace()
            SharedCodes.log("Demo", e.toString())
            NdefFragment.setAnswer(
                    "The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device",
                    true, NdefFragment.Dart.READ_ERROR)
            result.success(
                    mapOf("error" to "The NDEF Message is to long to read from an NTAG I2C 2K with this Nfc Device"));
         } catch (e: Exception){
            e.printStackTrace()
            SharedCodes.log("Demo", e.toString())
            NdefFragment.setAnswer(e.toString(), true, NdefFragment.Dart.READ_ERROR)
            result.success(
                    mapOf("error" to e.toString())
            );
         }
      } else {
         NdefFragment.setAnswer("please tap tag to read", false, NdefFragment.Dart.TAP_TOWRITE)
         result.success(
                 mapOf("error" to "please tap tag to read")
         );
      }
   }

   @ExperimentalUnsignedTypes
   fun resetRegisterCustomBytes(call: MethodCall, result: MethodChannel.Result){
      val isWrite            = call.argument<Boolean> ("isWrite")!!;
      if (demo.isReady && demo.isConnected) {
         demo.NDEFReadFinish()
         demo.finishAllTasks()
         if (isWrite){
            SharedCodes.log("Demo", "resetRegisterCustomBytes for write")
            writeEEPROMCustomBytes(call, result);
         }else{
            val startAt: ByteArray = call.argument<ByteArray>("startAt")!!;
            val length : Int       = call.argument<Int>("length")!!;
            SharedCodes.log("Demo", "resetRegisterCustomBytes for read")
            NtagRawRecord.seekWrittenAt = startAt.first();
            demo.readSessionRegistersCustom(startAt.first(), length, result);
         }
      } else {
         NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
         result.success(false);
      }
   }

   @ExperimentalUnsignedTypes
   fun resetRegister(call: MethodCall, result: MethodChannel.Result){
      val startAt: ByteArray = call.argument<ByteArray>("startAt")!!;
      val length : Int       = call.argument<Int>("startAt")!!;
      val _type              = call.argument<String>("type") ?: throw Exception("NdefType should not be null")
      val ndef_message_bytes = call.argument<ByteArray>("NdefMessage");
      val isWrite            = call.argument<Boolean> ("isWrite")!!;
      val ndef_message       = dartBytesToPlatformString(ndef_message_bytes);
      var ndef_bytes:ByteArray= byteArrayOf();
      val type                = WriteOptions.from(_type)

      NtagRawRecord.seekWrittenAt = startAt.first();
      if (ndef_message_bytes  != null){
         ndef_bytes = ndef_message_bytes;
      }

      SharedCodes.log("resetRegister", "bytes : ${ndef_message_bytes?.toUByteArray()?.joinToString(",")}");
      SharedCodes.log("resetRegister", "string: ${ndef_message}");

      NdefFragment.reset();
      NdefFragment.isWriteChosen = isWrite;
      NdefFragment.ndefType = type.data
      NdefFragment.setAarRecord(false)
      NdefFragment.text   = ndef_message
      NdefFragment.bytes  = ndef_bytes;
      NtagRawRecord.seekWrittenAt = startAt.first();

      if (demo.isReady && demo.isConnected) {
         SharedCodes.log("Demo",
                 "text: ${NdefFragment.text}, type: ${NdefFragment.ndefType}, " +
                         "aar: ${NdefFragment.isAarRecordSelected}, _aar: false")
         demo.NDEFReadFinish()
         demo.finishAllTasks()
         PseudoMainActivity.launchNdefDemoCustom(startAt.first(), ndef_bytes, length, result)
      } else {
         NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
      }
   }


   fun crash(msg:String){
      channel.invokeMethod(crash, msg);
   }

   fun logToDart(msg:String, error: Boolean){
//      channel.invokeMethod(onLogToDart, msg);
      registrar.activity().runOnUiThread {
         channel.invokeMethod(onLogToDart, msg);
      }
   }


   fun onJavaException(e: Exception){
      channel.invokeMethod(onLogToDart, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
      channel.invokeMethod(onJavaException, mapOf("exception" to (e.stackTrace.joinToString("\n")))  )
      SharedCodes.error("onJavaException2", e.toString());
   }

   override fun onSetBoardVersion(ver: String, fwver: String, request: Any) {
      val data = mapOf(
              "ver" to ver,
              "fwver" to fwver,
              "\$request" to Ntag_I2C_Demo::class.java.name
      )
      channel.invokeMethod("onSetBoardVersion", data)
   }

   fun onReading(){
      channel.invokeMethod("onReading", null);
   }


   override fun onShowAlert(message: String, title: String, request: Any) {
      uiSender.onShowAlert(message, title, request)
   }

   override fun onShowOkDialogue(message: String, title: String, request: Any) {
      uiSender.onShowOkDialogue(message, title, request)
   }

   override fun onShowOkDialogueForAction(message: String, title: String, request: Any, action: (b: Boolean) -> Unit) {
      uiSender.onShowOkDialogueForAction(message, title, request, action)
   }

   override fun onToastMakeText(message: String, time: Int, request: Any) {
      uiSender.onToastMakeText(message, time, request)
   }

   @ExperimentalUnsignedTypes
   fun dartBytesToBytes(dartbytes:ByteArray): UByteArray{
      return dartbytes.asUByteArray();
   }

   @ExperimentalUnsignedTypes
   fun dartBytesToPlatformString(bytes: ByteArray?): String{
      return bytes?.toUByteArray()?.map({it -> it.toUByte().toInt().toChar()})?.joinToString("")!!;
   }



   @ExperimentalUnsignedTypes
   fun dartBytesToPlatformString(call: MethodCall, result: MethodChannel.Result){
      val bytes = (call.argument<ByteArray>("bytes"))!!;
      val dstring = call.argument<String>("string")!!;

      val string = bytes.toString(StandardCharsets.US_ASCII);
      val string1 = bytes.toUByteArray().map({it -> it.toUByte().toInt().toChar()}).joinToString("");
      val string2 = dartBytesToPlatformString(bytes);
      val string_bytes = string.toByteArray()

      SharedCodes.log("dbyte", "bytes : ${bytes.asUByteArray().joinToString(",")}");
      SharedCodes.log("dbyte", "string: $string");
      SharedCodes.log("dbyte", "string_bytes : ${string_bytes.joinToString(",")}");
      SharedCodes.log("dbyte", "string_bytesU: ${string_bytes.toUByteArray().joinToString(",")}");
      SharedCodes.log("dbyte", "bytestring1: ${bytes.toUByteArray().toString()}");
      SharedCodes.log("dbyte", "bytestring2: ${bytes.toUByteArray().joinToString(",")}");
      SharedCodes.log("dbyte", "bytestring3: ${string1}");
      SharedCodes.log("dbyte", "bytestring4: ${bytes.map({it -> it.toUByte().toInt()}).joinToString(",")}");
      SharedCodes.log("dbyte", "bytestring5: ${ bytes.map({it -> it.toUByte().toInt()}).toString()}");
      SharedCodes.log("dbyte", "----------------");
      SharedCodes.log("dbyte", "dartbyte tostring: ${string2}");
      SharedCodes.log("dbyte", "string_bytes8: ${string2.toByteArray().joinToString(",")}");
      SharedCodes.log("dbyte", "string_bytes9: ${string2.toByteArray().asUByteArray().joinToString(",")}");
      SharedCodes.log("dbyte", string2.toByteArray().map({it -> it.toUByte().toInt()}).joinToString(",") );
      SharedCodes.log("dbyte", string2.toByteArray(StandardCharsets.US_ASCII).joinToString(",") );
      SharedCodes.log("dbyte", "----------------");
      SharedCodes.log("dbyte", dstring.toByteArray(StandardCharsets.US_ASCII).joinToString(",") );
      SharedCodes.log("dbyte", dstring.toByteArray().joinToString(",") );
      SharedCodes.log("dbyte", dstring.toByteArray().toUByteArray().joinToString(",") );

      result.success(mapOf(
        "string"       to string,
        "string1"       to string1,
        "string2"       to string2,
        "string_bytes" to bytes
      ));
   }

   fun miniNtagOnly(call: MethodCall, result: MethodChannel.Result){
      result.success(SharedCodes.miniNtagOnly);
   }

   fun doProcessOnExistingTagWithoutAuthCheck(call: MethodCall, result: MethodChannel.Result){
      val intent: Intent? = PseudoMainActivity.nfcIntent;
      val tag = intent?.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      PseudoMainActivity.demo = Ntag_I2C_Demo(tag, registrar.activity(), PseudoMainActivity.password, PseudoMainActivity.authStatus)
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "doProcessOnExistingTag");
      if (PseudoMainActivity.demo != null && PseudoMainActivity.demo!!.isReady) {
         // Retrieve Auth Status before doing any operation
         PseudoMainActivity.launchNdefDemo(
                 PseudoMainActivity.authStatus,
                 PseudoMainActivity.password,
                 result
              )
      }else{
         result.success(false);
      }
   }

   fun vibrate(t: Long? = 100){
      val pattern: LongArray;
      val time: Long;
      if (t == null) time = 100;
      else           time = t;
      val vibrator = registrar.activity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      vibrator.vibrate(time);
   }

   fun getProduct(prod: Ntag_Get_Version.Prod, result: MethodChannel.Result?): Map<String, Any>? {
      if (demo.isReady && demo.isConnected){
         var product_name: String = ""
         var ver: Ntag_Get_Version? = null
         when (prod) {
            Ntag_Get_Version.Prod.NTAG_I2C_1k -> {
               ver = Ntag_Get_Version.NTAG_I2C_1k; product_name = "NTAG_I2C_1k"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_1k_Plus -> {
               ver = Ntag_Get_Version.NTAG_I2C_1k_Plus; product_name = "NTAG_I2C_1k_Plus"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_1k_T -> {
               ver = Ntag_Get_Version.NTAG_I2C_1k_T; product_name = "NTAG_I2C_1k_T"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_1k_V -> {
               ver = Ntag_Get_Version.NTAG_I2C_1k_V; product_name = "NTAG_I2C_1k_V"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_2k -> {
               ver = Ntag_Get_Version.NTAG_I2C_2k; product_name = "NTAG_I2C_2k"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_2k_Plus -> {
               ver = Ntag_Get_Version.NTAG_I2C_2k_Plus; product_name = "NTAG_I2C_2k_Plus"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_2k_T -> {
               ver = Ntag_Get_Version.NTAG_I2C_2k_T; product_name = "NTAG_I2C_2k_T"
            }
            Ntag_Get_Version.Prod.NTAG_I2C_2k_V -> {
               ver = Ntag_Get_Version.NTAG_I2C_2k_V; product_name = "NTAG_I2C_2k_V"
            }
            Ntag_Get_Version.Prod.MTAG_I2C_1k -> {
               ver = Ntag_Get_Version.MTAG_I2C_1k; product_name = "MTAG_I2C_1k"
            }
            Ntag_Get_Version.Prod.MTAG_I2C_2k -> {
               ver = Ntag_Get_Version.MTAG_I2C_2k; product_name = "MTAG_I2C_2k"
            }
            else ->
               uiSender.onShowAlert("not a supported ntag product", "Ntag product not found", this)
         }
         if (ver == null)
            return null;
         val ret =  mapOf(
            "vendor_ID" to ver.vendor_ID,
            "product_type" to ver.product_type,
            "product_subtype" to ver.product_subtype,
            "major_product_version" to ver.major_product_version,
            "minor_product_version" to ver.minor_product_version,
            "storage_size" to ver.storage_size,
            "protocol_type" to ver.protocol_type,
            "product_name" to prod.name,
            "memsize" to prod.memsize
         );
         result?.success(ret);
         return ret;
      }
      result?.error("getProduct", "tag not connected", "");
      return null;
   }

   private fun scanFile(call: MethodCall, result: MethodChannel.Result) {
      val paths  = call.argument<MutableList<String>>("paths");
      MediaScannerConnection.scanFile(registrar.activity(),
        paths?.toTypedArray(), null, MediaScannerConnection.OnScanCompletedListener { path, uri ->  SharedCodes.log("scanFile", path)}
      );
   }


   /*
        method handler for handling method calls from dart
    */
   @ExperimentalUnsignedTypes
   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      SharedCodes.log("demo", "onMethodCall: ${call.method}");
      if (host == null){
         // allow vibrate and checkNFC to bypass nfc hardware check
         if (call.method != vibrate && call.method != scanFile && call.method != flutterReady &&
            (call.method != checkNFC && call.method != checkNFC2 && call.method != checkNFC3 && call.method != miniNtagOnly
              && call.method != checkNFC4 && call.method != getLog && call.method != dartBytesToPlatformString))
         {
            NdefFragment.setAnswer(
           "please tap ndef tag first", false, NdefFragment.Dart.TAP_TOREAD);
            return result.error("Demo", "onMethodCall: ${call.method}", "tag not connected");
         }
         // vibrate
         if (call.method == vibrate){
            vibrate(call.argument<Int>("time")?.toLong())
            return
         }
         // checkNFC
         when (call.method){
            scanFile -> {
               scanFile(call, result);
            }
            flutterReady -> {
               flutterReady(call, result);
            }
            miniNtagOnly ->{
               SharedCodes.log("demo", "miniNtagOnly");
               miniNtagOnly(call, result);
            }
            dartBytesToPlatformString ->{
               SharedCodes.log("demo", "dartBytesToPlatformString${call.method}");
               dartBytesToPlatformString(call, result);
            }
            getLog ->{
               SharedCodes.log("demo", "call getLog, ${call.method}");
               getLog(call, result);
            }
            checkNFC -> {
               SharedCodes.log("demo", "call checkNFC, ${call.method}");
               checkNFC(call, result);
            }
            checkNFC2 -> {
               SharedCodes.log("demo", "call checkNFC2, ${call.method}");
               checkNFC2(call, result);
            }
            checkNFC3 -> {
               SharedCodes.log("demo", "call checkNFC3, ${call.method}");
               checkNFC3(call, result);
            }
            checkNFC4 -> {
               SharedCodes.log("demo", "call checkNFC4, ${call.method}");
               checkNFC4(call, result);
            } doProcessOnExistingTagWithoutAuthCheck -> {
               doProcessOnExistingTagWithoutAuthCheck(call, result);
            }
         }
         return
      }
      val h: Ntag_I2C_Demo = host!!
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "call ${call.method}")
      when (call.method) {

         flutterReady -> {
            flutterReady(call, result);
         }
         scanFile -> {
            scanFile(call, result);
         }
         writeEEPROMCustomBytes ->{
            writeEEPROMCustomBytes(call, result);
         }
         readEEPROMCustomBytes -> {
            readEEPROMCustomBytes(call, result);
         }
         resetRegisterCustomBytes -> {
            resetRegisterCustomBytes(call, result);
         }
         resetRegister -> {
            SharedCodes.log("demo", "resetRegister");
            resetRegister(call, result);
         }
         readSessionRegistersCustom -> {
            val startAt: ByteArray  = call.argument<ByteArray>("startAt")!!;
            val length: Int  = call.argument<Int>("length")!!
            NtagRawRecord.seekWrittenAt = startAt.first();
            SharedCodes.log("demo", "readSessionRegistersCustom: $startAt");
            h.readSessionRegistersCustom(startAt.first(), length, result);
         }

         miniNtagOnly -> {
            SharedCodes.log("demo", "miniNtagOnly");
            miniNtagOnly(call, result);
         }
         doProcessOnExistingTagWithoutAuthCheck -> {
            doProcessOnExistingTagWithoutAuthCheck(call, result);
         }
         getLog ->{
            SharedCodes.log("demo", "call getLog, ${call.method}");
            getLog(call, result);
         }
         clearAuth -> {
            clearAuth(call, result);
         }
         ObtainAuthStatus -> {
            obtainAuthStatus(call, result);
         }
         setFactoryPassword->{
            setFactoryPassword(result);
            result.success(true);
         }
         setPassword->{
            val t = call.argument<String>("password");
            if (t != null){
               setPassword(t, result);
               result.success(true);
            }else{
               result.error("setPassword", "password should not be null", "");
            }
         }
         checkNFC -> {
            checkNFC(call, result);
         }
         checkNFC2 -> {
            checkNFC2(call, result);
         }
         checkNFC3 -> {
            checkNFC3(call, result);
         }
         checkNFC4 -> {
            checkNFC4(call, result);
         }
         vibrate -> {
            val t = call.argument<Int>("time")?.toLong();
            vibrate(t)
         }
         isReady ->
            result.success(h.isReady) //boolean
         isConnected ->
            result.success(h.isConnected) //boolean
         isTagPresent ->
            result.success(hostIsTagPresent(null)) // boolean
         finishAllTasks ->
            h.finishAllTasks() // void
         getProduct ->
            getProduct(h.product, result)
         setBoardVersion -> {
            h.setBoardVersion(); //void
         }
         resetTagMemory ->
            result.success(h.resetTagMemory() != -1) //int, indicates number of bytes written
         readSessionRegisters ->
            h.readSessionRegisters();
         readWriteConfigRegister ->
            h.readWriteConfigRegister()
         readTagContent ->
            result.success(h.readTagContent()) // byre[]
         resetTagContent ->
            result.success(h.tryResetTagContent()) // boolean

         readNDEF -> {
            readNDEF(call, result)
         }
         readNDEFCustom -> {
            readNDEFCustom(call, result)
         }
         writeNDEF -> {
            writeNDEF(call, result)
         }

         Auth -> {
            val pwd = call.argument<ByteArray>("pwd") as ByteArray
            val authStatus = call.argument<Int>("authStatus") as Int
            result.success(h.Auth(pwd, authStatus)) // boolean
         }
         /*
         *
         *        unused functionalities
         * */
         LED ->
            result.success(h.LED())
         LEDFinish ->
            result.success(h.LEDFinish())
         NDEFReadFinish ->
            result.success(h.NDEFReadFinish())
         NDEF -> {
            result.success(h.NDEF())
         }
         Flash ->
            result.success(h.Flash(call.argument<ByteArray>("bytesToFlash")))
         SRAMSpeedtest ->
            result.success(h.SRAMSpeedtest())
         SRAMSpeedFinish ->
            result.success(h.SRAMSpeedFinish())
         EEPROMSpeedtest ->
            result.success(h.EEPROMSpeedtest())
         EEPROMSpeedFinish ->
            result.success(h.EEPROMSpeedFinish())
         WriteEmptyNdefFinish ->
            result.success(h.WriteEmptyNdefFinish())
         WriteDefaultNdefFinish ->
            result.success(h.WriteDefaultNdefFinish())

         /*onWriteEEPROM ->{
            val bytes = call.argument<Int>("bytes") as Int
            result.success(h.onWriteEEPROM(bytes))
         }
         onWriteSRAM -> {
            result.success(h.onWriteSRAM())
         }*/
         else ->
            throw ExceptionInInitializerError("uncaught method: ${call.method}")
      }
   }
}
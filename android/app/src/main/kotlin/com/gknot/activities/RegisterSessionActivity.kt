package com.gknot.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.nfc.TagLostException;

import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.ChannelDart
import com.gknot.MainActivity
import com.gknot.datatypes.CompNamePseudo
import com.gknot.datatypes.REQUEST_CODE
import com.gknot.fragments.NdefFragment
import com.gknot.pseudo.Message
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.exceptions.CommandNotSupportedException
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.nfc_demo.reader.Ntag_I2C_Registers
import com.nxp.ntagi2cdemo.R

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.io.IOException

private const val MODULE_NAME: String = "RegisterSessionActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";



class RegisterSessionActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {
   private var pendingIntent: PendingIntent
      get() = SharedCodes.instance.mPendingIntent!!
      set(value){
         SharedCodes.instance.mPendingIntent = value;
      }
   private var mAdapter: NfcAdapter?
      get() = SharedCodes.instance.mAdapter
      set(value){
         SharedCodes.instance.mAdapter = value;
      }
   private var demo: Ntag_I2C_Demo
      get() = SharedCodes.instance.demo!!
      set(value){
         SharedCodes.instance.demo = value;
      }

   private var uidSender: Message? = null
   /*
   *     calls from Platform to Dart or constants passed from Platform to Dart
   */
   class Dart(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart{
      override var methodname: String? = null

      @ExperimentalUnsignedTypes
      fun onSetAnswer(answer:MutableMap<String, Any>){
         methodname = SETANSWER
         SharedCodes.log("","try invoke method $methodname on Dart")
         if (answer.containsKey("NDEF_Message")){
            val message = answer["NDEF_Message"] as String;
            val l = message.length;
            val msgarray = answer["NDEF_Message_BYTES"] as ByteArray?;
            val msarraystring = msgarray?.map({it -> it.toUByte().toInt()})?.joinToString(",");
            SharedCodes.log("RegSesAct", "NDEF_Message: [$l] ${answer["NDEF_Message"]}");
            SharedCodes.log("RegSesAct", "RAW_RECORD  : ${answer["NDEF_RAW_RECORD"]}");
            SharedCodes.log("RegSesAct", "NDEF_Message_bytes: ${msarraystring}");
         }
         ch?.invokeMethod(methodname, answer, this)
      }

      fun onTransceiveFailed():Unit{
         methodname = TRANSCEIVE_FAILED
         ch?.invokeMethod(methodname, mutableMapOf("" to ""), this)
      }

      fun onTagLost(): Unit{
         methodname = TAGLOST
         ch?.invokeMethod(methodname, mutableMapOf("" to ""), this)
      }

      companion object {
         const val TRANSCEIVE_FAILED:String = "onTransceiveFailed"
         const val TAGLOST:String = "onTagLost"
         const val SETANSWER:String = "onSetAnswer"
      }
   }

   companion object {
      var instance: RegisterSessionActivity? = null;
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = RegisterSessionActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`

         val entity_name = RegisterSessionActivity::class.java.name
         val pseudoComp  = CompNamePseudo.unflattenFromString(entity_name) ?:
                 throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as RegisterSessionActivity,pseudoComp, intent_filters)
         // initialize PseudoActivity
         instance?.uidSender = Message(registrar, channel)
         SharedCodes.log("RegSesAct", "registerPlugins")
      }


      @ExperimentalUnsignedTypes
      @JvmStatic fun initAnswer(answer: Ntag_I2C_Registers, cont: Context): MutableMap<String, Any>{
         val ret:MutableMap<String, Any> = mutableMapOf(
                 "Manufacture" to answer.Manufacture,
                 "Mem_size" to answer.Mem_size
         )
         ret.put("I2C_RST_ON_OFF", answer.I2C_RST_ON_OFF.toString() == "true")

         if (answer.FD_OFF == "00b") {
            ret.put("FD_OFF", cont.resources.getString(R.string.FD_OFF00))
         } else if (answer.FD_OFF == "01b") {
            ret.put("FD_OFF", cont.resources.getString(R.string.FD_OFF01))
         } else if (answer.FD_OFF == "10b") {
            ret.put("FD_OFF", cont.resources.getString(R.string.FD_OFF10))
         } else
            ret.put("FD_OFF", cont.resources.getString(R.string.FD_OFF11))

         if (answer.FD_ON == "00b") {
            ret.put("FD_ON", cont.resources.getString(R.string.FD_ON00))
         } else if (answer.FD_ON == "01b") {
            ret.put("FD_ON", cont.resources.getString(R.string.FD_ON01))
         } else if (answer.FD_ON == "10b") {
            ret.put("FD_ON", cont.resources.getString(R.string.FD_ON10))
         } else
            ret.put("FD_ON", cont.resources.getString(R.string.FD_ON11))

         ret.put("LAST_NDEF_PAGE", answer.LAST_NDEF_PAGE)
         ret.put("NDEF_DATA_READ", answer.NDEF_DATA_READ.toString() == "true")
         ret.put("RF_FIELD_PRESENT", answer.RF_FIELD_PRESENT.toString() == "true")
         ret.put("PTHRU_ON_OFF", answer.PTHRU_ON_OFF.toString() == "true")

         ret.put("I2C_LOCKED", answer.I2C_LOCKED.toString() == "true")
         ret.put("RF_LOCKED", answer.RF_LOCKED.toString() == "true")
         ret.put("SRAM_I2C_READY", answer.SRAM_I2C_READY.toString() == "true")
         ret.put("SRAM_RF_READY", answer.SRAM_RF_READY.toString() == "true")

         ret.put("PTHRU_DIR", answer.PTHRU_DIR.toString() == "true")
         ret.put("SRAM_MIRROR_ON_OFF", answer.SRAM_MIRROR_ON_OFF.toString() == "true")

         ret.put("SM_Reg", answer.SM_Reg)
         ret.put("WD_LS_Reg", answer.WD_LS_Reg)
         ret.put("WD_MS_Reg", answer.WD_MS_Reg)

         ret.put("I2C_CLOCK_STR", answer.I2C_CLOCK_STR.toString() == "true")
         ret.put("NDEF_Message", answer.NDEF_Message)
         ret.put("NDEF_Message_BYTES", answer.NDEF_Message_BYTES)


         var rawRecord: MutableMap<String, Any>;
         if (answer.NDEF_RAW_RECORD != null){
            rawRecord = mutableMapOf(
                    "tlvPlusNdef" to answer.NDEF_RAW_RECORD.tlvPlusNdef,
                    "tlvSize" to answer.NDEF_RAW_RECORD.tlvSize,
                    "valid" to answer.NDEF_RAW_RECORD.valid,
                    "data" to answer.NDEF_RAW_RECORD.data
            )
            if (answer.NDEF_RAW_RECORD.data != null)
               SharedCodes.log("RegSesAct", "NDEF_RAW_RECORD: ${answer.NDEF_RAW_RECORD.data.toUByteArray().joinToString(",")}");
         }else{
            rawRecord = mutableMapOf()
         }

         ret.put("NDEF_RAW_RECORD", rawRecord)
         SharedCodes.log("RegSesAct", "NDEF_Message   : ${answer.NDEF_Message}");
         return ret;
      }

      @ExperimentalUnsignedTypes
      @JvmStatic fun SetAnswer(answer: Ntag_I2C_Registers, cont: Context) {
         val ret:MutableMap<String, Any> = initAnswer(answer, cont);
         Dart(instance!!, null).onSetAnswer(ret)
      }
   }
   /*
   *     activity invoke from configSession triggered by click event
   * */
   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onCreate");

      // Capture intent to check whether the operation should be automatically launch or not
      val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      if (tag != null && Ntag_I2C_Demo.isTagPresent(tag)) {
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onCreate -> startDemo");
         startDemo(tag, false)
      }
      // Add Foreground dispatcher
      mAdapter = NfcAdapter.getDefaultAdapter(activity)
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
      // about SINGLE_TOP
      // If set, the activity will not be launched if it is already running
      // at the top of the history stack.

   }

   public override fun onPause() {
      super.onPause()
      if (mAdapter != null)
         mAdapter?.disableForegroundDispatch(registrar.activity())
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onPause");
   }

   public override fun onResume() {
      super.onResume()
      if (mAdapter != null) {
         /*
         [Description]

            About enableForegroundDispatch

            ⚠ Note: If you pass null for both the filters and techLists parameters that
            acts a wild card and will cause the foreground activity to receive all tags
            via the ACTION_TAG_DISCOVERED intent.

            ⚠ Note: This method must be called from the main thread, and only when the
            activity is in the foreground (resumed). Also, activities must call disable-
            ForegroundDispatch(Activity) before the completion of their Activity.onPause()
            callback to disable foreground dispatch after it has been enabled.
         */
         mAdapter?.enableForegroundDispatch(
              registrar.activity(),
              pendingIntent, null, null
         )
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onResume");
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      // Check which request we're responding to
      // Make sure the request was successful
      if (requestCode == REQUEST_CODE.AUTH_REQUEST.id
              && resultCode == Activity.RESULT_OK
              && demo != null
              && demo.isReady) {
         // Launch the thread
         try {
            demo.readSessionRegisters()
            return true
         } catch (e: CommandNotSupportedException) {
            uidSender?.onShowOkDialogue(
              "This NFC device does not support the NFC Forum " + "commands needed to access the session register",
              "Command not supported", this
            )
            return false
         }
      }
      return false
   }

   override fun onNewIntent(intent: Intent): Boolean {
      try{
         val nfcIntent = intent;
         // Set the initial auth parameters to current AuthStatus
         PseudoMainActivity.authStatus = AuthActivity.AuthStatus.Disabled.value
         // set password to current auth
         PseudoMainActivity.password = null
         // set intent information to replace the old one
         PseudoMainActivity.nfcIntent = nfcIntent
         val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
         //fixme:....  start here.. gordianknot 20190513!!!!
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onNewIntent, getAuthStatus:true")
         startDemo(tag, true)
         return true
      }catch(e: Exception) {
         NdefFragment.setAnswer("read register failed", false, NdefFragment.Dart.READ_LOST)
         SharedCodes.log(SharedCodes.libTagName(this::class.java), e.toString());
         return false;
      }
   }

   fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      if (tag == null)
         return NdefFragment.setAnswer("tag lost", false, NdefFragment.Dart.READ_LOST)

      try {
         demo = Ntag_I2C_Demo(
                 tag, registrar.activity(),
                 PseudoMainActivity.password,
                 PseudoMainActivity.authStatus)

         if (demo != null && !demo.isReady) {
            return
         }

         // Retrieve the Auth Status
         if (getAuthStatus) {
            PseudoMainActivity.authStatus = demo.ObtainAuthStatus();
            SharedCodes.log(SharedCodes.libTagName(this::class.java),
                    "startDemo - getAuthStatus:${PseudoMainActivity.authStatus}")
         }

         // Demo is available when the tag is not protected or the memory is only write-protected
         if (PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Disabled.value
                 || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Unprotected.value
                 || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Authenticated.value
                 || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Protected_W.value
                 || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Protected_W_SRAM.value) {
            try {
               // rever back following code(line1,2,3) if prone-error
//               FlutterNtag_I2C_Demo.demo.NDEFReadFinish()
//               SharedCodes.log(SharedCodes.libTagName(this::class.java), "startDemo - NDEFReadFinish")

//               if (FlutterNtag_I2C_Demo.demo.isReady && FlutterNtag_I2C_Demo.demo.isConnected)
//                  FlutterNtag_I2C_Demo.demo.finishAllTasks()

//               SharedCodes.log(SharedCodes.libTagName(this::class.java),"startDemo - finishAllTasks")
               SharedCodes.log(SharedCodes.libTagName(this::class.java),
                       "startDemo - readSessionRegisters")
               demo.readSessionRegisters()

            } catch (e: CommandNotSupportedException) {
               uidSender?.onShowOkDialogue(
                       "This NFC device does not support the NFC Forum commands needed to access the session register",
                       "Command not supported",
                       this
               )
               return
            }
         } else {
            //todo:
            SharedCodes.log(SharedCodes.libTagName(this::class.java), "showAuthDialog")
            showAuthDialog(tag)
         }
      } catch(e: TagLostException){
         Dart(instance!!, null).onTagLost();
      } catch (e: IOException){
         val error: String = e.toString();
         SharedCodes.log(SharedCodes.libTagName(this::class.java), error);

         if (error.contains("Transceive failed")){
            Dart(instance!!, null).onTransceiveFailed();
            return;
         }
         NdefFragment.setAnswer("failed to read", false, NdefFragment.Dart.READ_LOST)
      }
   }

   fun showAuthDialog(tag: Tag) {
      val intent: Intent = Intent(registrar.activity(), AuthActivity::class.java)
//      if (PseudoMainActivity.nfcIntent != null)
//         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      //ActivityFlutterMediator.startActivityForResult(intent, PseudoMainActivity.AUTH_REQUEST, RegisterSessionActivity::class.java)
      SharedCodes.log("RegAct", "registerShowAuthDialog")
      FlutterNtag_I2C_Demo.instance!!.registerShowAuthDialog(tag);
   }

   fun showAboutDialog() {
      val intent: Intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      SharedCodes.log("RegAct", "showAboutDialog")
      ActivityFlutterMediator.startActivity(intent)
   }



}

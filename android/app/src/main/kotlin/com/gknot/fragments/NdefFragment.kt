package com.gknot.fragments

import android.content.Intent
import android.os.Bundle
import com.gknot.*
import com.gknot.activities.*

import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.datatypes.CompNamePseudo

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.ntagi2cdemo.R

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "NdefFragment";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";


class NdefFragment(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {


   /*
   *     calls from Platform to Dart or constants passed from Platform to Dart
   * */
   class Dart(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart {
      companion object {
         const val INCORRECT_CONTENT= "ndefWriteIncorrectContent"
         const val DETECTED_RESPONSE= "ndefDetected"

         const val WRITE_SUCCESS        = "ndefWriteSuccess"
         const val READ_SUCCESS        = "ndefReadSuccess"

         const val READ_LOST        = "ndefReadLost"
         const val READ_ERROR       = "ndefReadError"
         const val READ_RESPONSE    = "ndefReadResponse"
         const val READ_PROTECTED   = "ndefReadProtected"

         const val WRITE_LOST       = "ndefWriteLost"
         const val WRITE_ERROR      = "ndefWriteError"
         const val WRITE_RESPONSE   = "ndefWriteResponse"
         const val WRITE_PROTECTED  = "ndefWriteProtected"

         const val TAP_TOREAD       = "ndefTapRead"
         const val TAP_TOWRITE      = "ndefTapWrite"

         const val NDEF_TEXT        = "ndefText"
         const val NDEF_TEXT_BYTES  = "ndefTextBytes"
         const val NDEF_TYPE_TEXT   = "ndefTypeText"
         const val NDEF_CB          = "ndefCallback"
         const val NDEF_DATARATE_CB = "ndefDataRateCallback"
         const val ADD_AAR          = "addAar"

         const val UPDATE_DATARATE  = "updateDataRate"
         const val UPDATE_TO_UI     = "updateToUi"
         const val UPDATE_PROGRESS  = "updateProgress"
         const val PERFORMANCE      = "ndefPerformance"
         const val TOAST_MESSAGE    = "toastMessage"

         const val NDEF_RAW_RECORD = "ndefRawRecord"
      }
      override var methodname: String? = null

      fun updateToUi(data: Any){
         methodname = UPDATE_TO_UI
         SharedCodes.log("NdeFrag","try invoke method $methodname on Dart")
         ch?.invokeMethod(UPDATE_TO_UI, data, this)
      }

      fun toastMessage(msg:String?, error: Boolean = false, tag: String? = TOAST_MESSAGE){
         methodname = TOAST_MESSAGE;
         SharedCodes.log("NdeFrag","toastMessage: $msg, tag:$tag")
         ch?.invokeMethod(methodname, mapOf(
           "msg" to msg,
           "error" to error,
           "tag" to tag
         ), this)
      }

      fun updateDataRate(datarate:String){
         val m:String = UPDATE_DATARATE;
         methodname = m;
         SharedCodes.log("NdeFrag","try invoke method $methodname on Dart")
         ch?.invokeMethod(m, mapOf(
                 "datarate" to datarate
         ))
      }

      fun updateProgress(datarate:String, ndefmsg:String, ndeftype:String){
         val bytes: ByteArray? = ndefmsg.toByteArray();
         val m:String = UPDATE_PROGRESS;
         methodname = m;
         SharedCodes.log("NdeFrag", "try invoke method $methodname on Dart, daterate: $datarate, msg: $ndefmsg, type: $ndeftype")
         ch?.invokeMethod(m, mapOf(
               "ndefDataRateCallback" to datarate,
               "ndefText" to ndefmsg,
               "ndefBytes" to bytes,
               "ndefTypeText" to ndeftype
         ));
      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   * */
   class Platform(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelPlatform {
      override var methodname: String? = null
      companion object {
         // methods
         const val UPDATE_EDIT_TEXT   = "updateEditText"
         const val UPDATE_TO_PLATFORM = "updateToPlatform"
         const val READ_NDEF_CLICK = "readNdefClick"
         const val WRITE_NDEF_CLICK = "writeNdefClick"
         const val WRITE_DEFAULT_NDEF_CLICK = "writeDefaultNdefClick"
         // consts
         const val IS_WRITE_CHOSEN  = "isWriteChosen"
         const val WRITE_OPTIONS    = "writeOptions"
         // -----------
         const val NDEF_EDIT_TEXT   = "ndefEditText"
         const val NDEF_EDIT_MAC    = "ndefEditMac"
         const val NDEF_EDIT_NAME   = "ndefEditName"
         const val NDEF_EDIT_CLASS  = "ndefEditClass"
         // -----------
         const val NDEF_EDIT_TITLE  = "ndefEditTitle"
         const val NDEF_EDIT_LINK   = "ndefEditLink"
         // -----------
         const val NDEF_READ_LOOP   = "ndefReadLoop"
         const val ADD_AAR          = "addAar"
      }
      fun writeDefaultNdefClick(call:MethodCall, result: MethodChannel.Result){
         instance?.writeDefaultNdefClick( )
      }
      fun updateToPlatform(call:MethodCall, result: MethodChannel.Result ){
         NdefFragment.updateToPlatform(call, result)
      }
      fun readNdefClick(call:MethodCall, result: MethodChannel.Result){
         instance?.readNdefClick(call, result)
      }
      fun writeNdefClick(call: MethodCall, result: MethodChannel.Result){
         instance?.writeNdefClick(call, result)
      }
      fun updateEditText(call: MethodCall, result: MethodChannel.Result){
         val text:String? = call.argument<String>("ndefEditText")
         if (text != null)
            ndefEditText = text
         SharedCodes.log("NdeFrag", "updateEditText $text")
      }
   }


   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      val methodname:String = call.method.toString();
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "receive call: $methodname from Dart")
      when (call.method) {
         NdefFragment.Platform.UPDATE_EDIT_TEXT ->
            Platform(this, null).updateEditText(call, result)
         NdefFragment.Platform.UPDATE_TO_PLATFORM ->
            Platform(this, null).updateToPlatform(call, result)
         NdefFragment.Platform.READ_NDEF_CLICK ->
            Platform(this, null).readNdefClick(call, result)
         NdefFragment.Platform.WRITE_NDEF_CLICK ->
            Platform(this, null).writeNdefClick(call, result)
         NdefFragment.Platform.WRITE_DEFAULT_NDEF_CLICK ->
            Platform(this, null).writeDefaultNdefClick(call, result)

         /*
         NdefFragment.Dart.TOAST_MESSAGE -> {
            val msg:String?    = call.argument<String>("message")
            val error:Boolean? = call.argument<Boolean>("error")
            val tag:String?    = call.argument<String>("tag")
            return Dart(this, null).toastMessage(msg, error, tag)
         }
         NdefFragment.Dart.UPDATE_TO_UI -> {
            return Dart(this, null).updateToUi(call.arguments)
         }
         NdefFragment.Dart.UPDATE_DATARATE ->{
            val datarate:String =
            return Dart(this, null).updateDataRate(datarate)
         }*/
         else ->
            throw NoSuchMethodException("Uncaught method: ${call.method} not found")
      }
   }


   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: MainActivity) {
      super.onCreate(savedInstanceState, intent, activity)
      isWriteChosen = false
      writeOptions = Companion.WriteOptions.RadioNdefText
   }

   fun readNdefClick(call: MethodCall, result: MethodChannel.Result){
      if (PseudoMainActivity.demo == null){
         result.error("demo should not be null", "", "");
         throw ExceptionInInitializerError("demo should not be null")
      }
      SharedCodes.log("NdeFrag", "readNdefClick")
      val demo: Ntag_I2C_Demo = PseudoMainActivity.demo as Ntag_I2C_Demo;

      demo.NDEFReadFinish()
      val resources = registrar.activity().resources
      ndefPerformance = resources.getString(R.string.layout_input_ndef_read)
      ndefCallback = resources.getString(R.string.readNdefMsg)
      isWriteChosen = false

      // Read content
      if (demo.isReady) {
         demo.finishAllTasks()
         /*if (PseudoMainActivity.password == null){
            result.error("password should not be null", "", "");
            throw ExceptionInInitializerError("password should not be null")
         }*/
         PseudoMainActivity.launchNdefDemo(
                 PseudoMainActivity.authStatus,
                 PseudoMainActivity.password, null)
         result.success(true);
      }
   }

   fun writeNdefClick(call: MethodCall, result: MethodChannel.Result){
      val resources = registrar.activity().resources
      ndefPerformance = resources.getString(R.string.layout_input_ndef_write)
      ndefCallback = resources.getString(R.string.writeNdefMsg)
      if (PseudoMainActivity.demo == null)
         throw ExceptionInInitializerError("demo should not be null")
      val demo: Ntag_I2C_Demo = PseudoMainActivity.demo as Ntag_I2C_Demo;
      isWriteChosen = true;
      // Close the ReadNdef Taks
      demo.NDEFReadFinish()
      if (demo.isReady) {
         demo.finishAllTasks()
         if (PseudoMainActivity.password == null)
            throw ExceptionInInitializerError("password should not be null")
         PseudoMainActivity.launchNdefDemo(
                 PseudoMainActivity.authStatus,
                 PseudoMainActivity.password as ByteArray, null)
      }
      result.success(true);
   }

   fun writeDefaultNdefClick(){
      val resources = registrar.activity().resources
      ndefCallback = resources.getString(R.string.writeNdefMsg)
//      writeNdefButton!!.setBackgroundResource(R.drawable.btn_blue)
//      readNdefButton!!.setBackgroundColor(Color.BLACK)
//      ndefWriteOptions!!.visibility = View.VISIBLE
//      ndefReadType!!.visibility = View.GONE
//      val uri = ndefWriteOptions!!.getChildAt(6) as RadioButton
//      uri.isChecked = true
      writeOptions = Companion.WriteOptions.RadioNdefUrl;
//      linearSp!!.visibility = View.VISIBLE
//      linearBt!!.visibility = View.GONE
//      ndefEditText!!.visibility = View.GONE
      ndefEditTitle = resources.getString(R.string.ndef_default_text)
      ndefEditLink = resources.getString(R.string.ndef_default_uri)
//      ndefText!!.visibility = View.GONE
      addAar = true
      isWriteChosen = true

      // Write content
      if (PseudoMainActivity.demo!!.isReady) {
         PseudoMainActivity.demo!!.finishAllTasks()
         if (PseudoMainActivity.password == null)
            throw ExceptionInInitializerError("password should not be null")
         PseudoMainActivity.launchNdefDemo(
                 PseudoMainActivity.authStatus,
                 PseudoMainActivity.password as ByteArray, null)
      }
   }


   fun ndefWriteOptionCheckedChanged(checkedId: WriteOptions) {
      if (checkedId == Companion.WriteOptions.RadioNdefText) {
//         ndefEditText!!.visibility = View.VISIBLE
         ndefEditText = ""
//         linearBt!!.visibility = View.GONE
//         linearSp!!.visibility = View.GONE
      } else if (checkedId == Companion.WriteOptions.RadioNdefUrl) {
//         ndefEditText!!.visibility = View.VISIBLE
         ndefEditText = "http://www."
//         linearBt!!.visibility = View.GONE
//         linearSp!!.visibility = View.GONE
      } else if (checkedId == Companion.WriteOptions.RadioNdefBt) {
//         ndefEditText!!.visibility = View.GONE
//         linearBt!!.visibility = View.VISIBLE
//         linearSp!!.visibility = View.GONE
      } else if (checkedId == Companion.WriteOptions.RadioNdefSp) {
//         ndefEditText!!.visibility = View.GONE
//         linearBt!!.visibility = View.GONE
//         linearSp!!.visibility = View.VISIBLE
         ndefEditLink = "http://www."
      }
   }


   companion object {
      enum class WriteOptions(val data:String){
         RadioNdefBytes("radioNdefBytes"),
         RadioNdefText("radioNdefText"),
         RadioNdefUrl("radioNdefUrl"),
         RadioNdefBt("radioNdefBt"),
         RadioNdefSp("radioNdefSp");

         companion object {
            fun from(findValue: String): WriteOptions
                    = WriteOptions.values().first { it.data.toLowerCase() == findValue.toLowerCase() }
         }
      }
      @JvmStatic var isWriteChosen = false
      private var ndefWriteOptions: WriteOptions? = null
      var writeOptions: WriteOptions?
         get () = ndefWriteOptions
         set (value){
            if (value != null){
               instance?.ndefWriteOptionCheckedChanged(value)
            }
            ndefWriteOptions = value
         }


      private var ndefPerformance: String = ""
      private var ndefText: String=""
      private var ndefBytes: ByteArray = byteArrayOf();

      private var ndefEditText: String = ""
      private var ndefEditBytes: ByteArray = byteArrayOf();
      private var ndefEditMac: String = ""
      private var ndefEditName:String = ""
      private var ndefEditClass: String = ""

      private var ndefEditTitle:String = ""
      private var ndefEditLink:String = ""
      private var ndefTypeText:String = ""
      private var ndefCallback:String = ""
      private var ndefRawRecord: NtagRawRecord? = null;
      private var ndefDataRateCallback:String = ""
      private var ndefReadLoop:Boolean = false
      private var addAar: Boolean = false

      @JvmStatic fun reset(){
         setNdefMessage("")
         ndefBytes = byteArrayOf()
         ndefType = ""
         setDatarate("")
         ndefType = ""
      }
      @JvmStatic fun resetNdefDemo() {
         if (isWriteChosen == true) {
            setAnswer("please tap tag to write")
         } else {
            setAnswer("please tap tag to read")
         }
         setNdefMessage("")
         setNdefMessage(byteArrayOf())
         ndefType = ""
         setDatarate("")
         ndefType = ""
      }

      @JvmStatic var text: String
         get() = ndefEditText
         set(t) {
            ndefEditText = t
         }

      @JvmStatic var bytes: ByteArray
         get() = ndefEditBytes
         set(t) {
            ndefEditBytes = t
         }

      @JvmStatic val btMac: String
         get() = ndefEditMac

      @JvmStatic val btName: String
         get() = ndefEditName

      @JvmStatic val btClass: String
         get() = ndefEditClass

      @JvmStatic val spTitle: String
         get() = ndefEditTitle

      @JvmStatic val spLink: String
         get() = ndefEditLink

      @JvmStatic val isAarRecordSelected: Boolean
         get() = addAar

      @JvmStatic val isNdefReadLoopSelected: Boolean
         get() = ndefReadLoop

      @JvmStatic fun setAnswer(answer: String, error: Boolean = false, tag:String = Dart.TOAST_MESSAGE ) {
         ndefCallback = answer
         Dart(instance!!, null).toastMessage(answer, error, tag)
      }

      @JvmStatic fun updateProgress(datarate:String, ndefmsg: String, ntype:String){
         ndefText = ndefmsg;
         ndefDataRateCallback = datarate
         ndefTypeText = ntype
         Dart(instance!!, null).updateProgress(ndefDataRateCallback, ndefText, ndefTypeText);
      }

      @JvmStatic fun setDatarate(datarate: String) {
         ndefDataRateCallback = datarate
         Dart(instance!!, null).updateDataRate(datarate);
      }

      @JvmStatic fun setNdefMessage(answer: String) {
         if (SharedCodes.customWrite){
            ndefText = answer;
            SharedCodes.log("setNdefMessage", ndefText);
         }else{
            ndefText = answer
         }
      }

      @JvmStatic fun setNdefMessage(answer: ByteArray) {
         if (SharedCodes.customWrite){
            ndefBytes = answer.copyOfRange(SharedCodes.shiftBits, answer.size);

            SharedCodes.log("setNdefMessage", ndefText);
         }else{
            ndefBytes = answer
         }
      }

      @JvmStatic val isRadioBytes: Boolean
         get(){
            return ndefType.toLowerCase() == WriteOptions.RadioNdefBytes.data.toLowerCase()
         }
      @JvmStatic val isRadioText: Boolean
         get(){
            return ndefType.toLowerCase() == WriteOptions.RadioNdefText.data.toLowerCase()
         }
      @JvmStatic val isRadioUrl: Boolean
         get(){
            return ndefType.toLowerCase() == WriteOptions.RadioNdefUrl.data.toLowerCase()
         }
      @JvmStatic val isRadioSp: Boolean
         get(){
            return ndefType.toLowerCase() == WriteOptions.RadioNdefSp.data.toLowerCase()
         }
      @JvmStatic val isRadioBt: Boolean
         get(){
            return ndefType.toLowerCase() == WriteOptions.RadioNdefBt.data.toLowerCase()
         }

      @JvmStatic var rawRecord: NtagRawRecord?
         get(){
            return ndefRawRecord;
         }
         set(record: NtagRawRecord?){
            ndefRawRecord = record;
         }
      @JvmStatic var ndefType: String
         get() {
            return ndefTypeText;
         }
         set(type) {
            ndefTypeText = type
            /*ndefWriteOptions = Companion.WriteOptions.values().firstOrNull{v -> v.data == type} ?:
               throw ExceptionInInitializerError("invalid ndefWriteOptions: $type")*/
         }

      @JvmStatic fun setAarRecord(value: Boolean){
         addAar = value;
      }

      @JvmStatic
      fun asMap(): Map<String, Any?>{
         if (writeOptions == null)
            writeOptions = Companion.WriteOptions.RadioNdefText;

         val data:Map<String, Any?> = mapOf(
                 // provide from dart then update into platform via updateToPlatform
                 Platform.IS_WRITE_CHOSEN to isWriteChosen ,
                 Platform.WRITE_OPTIONS   to writeOptions.toString(),
                 Platform.NDEF_EDIT_TEXT  to ndefEditText,
                 Platform.NDEF_EDIT_MAC   to ndefEditMac,
                 Platform.NDEF_EDIT_NAME  to ndefEditName,
                 Platform.NDEF_EDIT_CLASS to ndefEditClass,
                 Platform.NDEF_EDIT_TITLE to ndefEditTitle,
                 Platform.NDEF_EDIT_LINK  to ndefEditLink,
                 Platform.NDEF_READ_LOOP  to ndefReadLoop,
                 // generate in platform, update to dart
                 Dart.PERFORMANCE      to ndefPerformance,
                 Dart.NDEF_TEXT        to ndefText,
                 Dart.NDEF_TEXT_BYTES  to ndefBytes,
                 Dart.NDEF_TYPE_TEXT   to ndefTypeText,
                 Dart.NDEF_CB          to ndefCallback,
                 Dart.NDEF_DATARATE_CB to ndefDataRateCallback,
                 Dart.NDEF_RAW_RECORD  to mapOf(
                         "tlvSize" to ndefRawRecord?.tlvSize,
                         "tlvPlusNdef" to ndefRawRecord?.tlvPlusNdef,
                         "valid" to ndefRawRecord?.valid,
                         "data" to ndefRawRecord?.data
                 ) ,
                 // changed on platform
                 Dart.ADD_AAR          to addAar
         )
         return data;
      }

      @JvmStatic
      fun updateToUi(){
         // 📖   : read from ui(dart)
         // ✒️: write on platform
         // 🏳️  : constant String
         // call from Ndef_Demo
         /*val data:Map<String, Any?> = mapOf(
                 // 📖   : "isWriteChosen" to isWriteChosen,
                 // 📖   : "writeOptions" to writeOptions,
                 // 🏳️  :"ndefPerformance" to ndefPerformance,
                 Dart.NDEF_TEXT to ndefText,

                 // 📖   : "ndefEditText" to ndefEditText,
                 // 📖   : "ndefEditMac" to ndefEditMac,
                 // 📖   : "ndefEditName" to ndefEditName,
                 // 📖   : "ndefEditClass" to ndefEditClass,

                 // 📖   : "ndefEditTitle" to ndefEditTitle,
                 // 📖   : "ndefEditLink" to ndefEditLink,
                 Dart.NDEF_TYPE_TEXT to ndefTypeText,
                 Dart.NDEF_CB to ndefCallback,

                 Dart.NDEF_DATARATE_CB to ndefDataRateCallback,
                 // 📖   : "ndefReadLoop" to ndefReadLoop,
                 // changed on platform
                 Dart.ADD_AAR to addAar
         )*/

         if (writeOptions == null)
            writeOptions = Companion.WriteOptions.RadioNdefText;

         val data:Map<String, Any?> = asMap();
         SharedCodes.log("NdeFrag", "updateToUi: $data");
         Dart(instance!!, null).updateToUi(data)
      }
      /*
      *     call from dart
      */
      @JvmStatic
      fun updateToPlatform(call: MethodCall, result: MethodChannel.Result){
         //call from Dart
         isWriteChosen = call.argument<Boolean>(Platform.IS_WRITE_CHOSEN) as Boolean
         writeOptions = WriteOptions.from(call.argument<String>(Platform.WRITE_OPTIONS) as String)
         // 🏳️  :ndefPerformance = call.argument<String>("ndefPerformance") as String
         // ✒️: ndefText = call.argument<String>("ndefText") as String

         ndefEditText = call.argument<String>(Platform.NDEF_EDIT_TEXT) as String
         ndefEditMac = call.argument<String>(Platform.NDEF_EDIT_MAC) as String
         ndefEditName = call.argument<String>(Platform.NDEF_EDIT_NAME) as String
         ndefEditClass = call.argument<String>(Platform.NDEF_EDIT_CLASS) as String

         ndefEditTitle = call.argument<String>(Platform.NDEF_EDIT_TITLE) as String
         ndefEditLink = call.argument<String>(Platform.NDEF_EDIT_LINK) as String
         // ✒️: ndefTypeText = call.argument<String>("ndefTypeText") as String
         // ✒️: ndefCallback = call.argument<String>("ndefCallback") as String

         // ✒️: ndefDataRateCallback = call.argument<String>("ndefDataRateCallback") as String
         ndefReadLoop = call.argument<Boolean>(Platform.NDEF_READ_LOOP) as Boolean
         // changed either on platform and ui
         addAar = call.argument<Boolean>(Platform.ADD_AAR) as Boolean
         result.success(true);
      }
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      var instance: NdefFragment? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = NdefFragment(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         // val pkg          = FlutterRegisterSessionPseudo::class.java.`package`
         val entity_name    = NdefFragment::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name)
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as NdefFragment, component, intent_filters)
         SharedCodes.log("NdeFrag", "registerPlugin")
      }
   }
}

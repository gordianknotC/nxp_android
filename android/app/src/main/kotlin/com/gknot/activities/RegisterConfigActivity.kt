package com.gknot.activities


import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.MainActivity
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.pseudo.Message

import com.gknot.activities.AuthActivity.AuthStatus
import com.gknot.datatypes.CompNamePseudo
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.exceptions.CommandNotSupportedException

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.nfc_demo.reader.Ntag_I2C_Plus_Registers
import com.nxp.nfc_demo.reader.Ntag_I2C_Registers
import com.nxp.ntagi2cdemo.R
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "RegisterConfigActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";



class RegisterConfigActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel)  {
   private var pendingIntent: PendingIntent? = null
   private var mAdapter: NfcAdapter? = null
   private var uiSender: Message? = null
   private val demo: Ntag_I2C_Demo?
      get() = FlutterNtag_I2C_Demo.instance?.host
   private var isWriteProtected: Boolean = false
   enum class FD(val data:String){
      OFF0("FD_OFF00"),
      OFF1("FD_OFF01"),
      OFF2("FD_OFF10"),
      OFF3("FD_OFF11"),

      ON0("FD_ON00"),
      ON1("FD_ON01"),
      ON2("FD_ON10"),
      ON3("FD_ON11");

      fun getPos():Int{
         return when (this){
            OFF0-> 0
            OFF1-> 1
            OFF2-> 2
            OFF3-> 3
            ON0-> 0
            ON1-> 1
            ON2-> 2
            ON3-> 3
            else->
               throw Exception("Invalid Usage")
         }
      }
      companion object {
         fun from(findValue: String): FD =
           FD.values().first { it.data == findValue }
      }
   }
   enum class SWitch(val data:String){
      PTHRU_DIR_switch_on("RF to I2C"),
      PTHRU_DIR_switch_off("I2C to RF"),
   }
   /*
   *     calls from Platform to Dart or constants passed from Platform to Dart
   */
   private class Dart{
      companion object {
         fun onPTHRU_Dir_Changed(){

         }
         fun onWRITE_ACCESS_Changed(){

         }
         val UPDATE_TO_DART = "updateToDart"
         fun updateToDart(data: Map<String, Any?>){
            ch?.invokeMethod(UPDATE_TO_DART, data)
         }
      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   */
   private class Platform{
      companion object {
         fun onReadConfigClick(call: MethodCall, result: MethodChannel.Result){
            instance?.onReadConfigClick();
            result.success(null);
         }
         fun onWriteConfigClick(call: MethodCall, result: MethodChannel.Result){
            instance?.onReadConfigClick();
            result.success(null);
         }
         fun updateToPlatform(call: MethodCall, result: MethodChannel.Result){
            RegisterConfigActivity.updateToPlatform(call, result)
         }
      }
   }

   @SuppressLint("NewApi")
   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);

      ChipInfo_1_text = ""
      ChipInfo_2_text = ""

      I2C_RST_switch = null
      FD_OFF_spinner = null
      FD_ON_spinner = null

      // Set the Write protection check
      isWriteProtected = false

      LAST_NDEF_PAGE_edit = "0"
      // PTHRU_ON_OFF_switch = (Switch) findViewById(R.id.PTHRU_ON_OFF);
      PTHRU_DIR_switch = null
      WRITE_ACCESS_switch = null

      // SRAM_MIRROR_ON_OFF_switch = (Switch)
      // findViewById(R.id.SRAM_MIRROR_ON_OFF);
      SRAM_MIRROR_PAGE_edit_edit = "SRAM Mirror block is"
      I2C_WD_LS_Timer_edit ="0"
      I2C_WD_MS_edit = "0"

      I2C_CLOCK_STR_switch = null

      PLUS_AUTH0_edit = "0"
      PLUS_NFC_Prot_switch = null
      PLUS_NFC_Disc_Sec1_switch = null
      PLUS_AUTHLim_edit = "0"
      PLUS_2K_Prot_switch = null
      PLUS_Sram_Prot_switch = null
      PLUS_I2C_Prot_edit = "0"

      // Default Selection: Read
      isWriteChosen = false

      // Capture intent to check whether the operation should be automatically launch or not
      val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      if (tag != null && Ntag_I2C_Demo.isTagPresent(tag)) {
         startDemo(tag, false)
      }

      // Add Foreground dispatcher
      mAdapter = SharedCodes.getNfcAdapter(activity); // NfcAdapter.getDefaultAdapter(activity)
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
   }

   public override fun onPause() {
      super.onPause()
      if (mAdapter != null)
         mAdapter!!.disableForegroundDispatch(registrar.activity())
   }

   public override fun onResume() {
      super.onResume()

      if (mAdapter != null) {
         mAdapter!!.enableForegroundDispatch(registrar.activity(), pendingIntent, null, null)
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      // Check which request we're responding to
      // Make sure the request was successful
      if (requestCode == PseudoMainActivity.AUTH_REQUEST
              && resultCode == Activity.RESULT_OK
              && demo != null
              && demo!!.isReady) {
         if (isWriteProtected) {
            isWriteChosen = true
            return true
         } else {
            try {
               demo!!.readWriteConfigRegister()
            } catch (e: CommandNotSupportedException) {
               uiSender?.onShowOkDialogue(
                       "This NFC device does not support the NFC Forum " + "commands needed to access the config register",
                       "Command not supported",
                       this
               )
               return false
            }
            return true
         }
      }
      return false
   }

   override fun onNewIntent(intent: Intent):Boolean {
      val nfc_intent = intent
      // Set the initial auth parameters
      PseudoMainActivity.authStatus = AuthStatus.Disabled.value
      PseudoMainActivity.password = null

      // Set the Write protection check
      isWriteProtected = false

      // Store the intent information
      PseudoMainActivity.nfcIntent = nfc_intent
      val tag = nfc_intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      startDemo(tag, true)
      return true;
   }

   private fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      //demo = Ntag_I2C_Demo(tag, registrar.activity(), PseudoMainActivity.password, PseudoMainActivity.authStatus)
      Ntag_I2C_Demo(tag, registrar.activity(), PseudoMainActivity.password, PseudoMainActivity.authStatus)
      if (!demo!!.isReady)
         return

      // Calculate the Register Values according to what has been selected by
      // the user
      calcConfiguration()

      // Retrieve the Auth Status
      if (getAuthStatus == true) {
         PseudoMainActivity.authStatus = demo!!.ObtainAuthStatus()
      }
      if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value
              || PseudoMainActivity.authStatus == AuthStatus.Unprotected.value
              || PseudoMainActivity.authStatus == AuthStatus.Authenticated.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W_SRAM.value) {
         try {
            demo!!.readWriteConfigRegister()
         } catch (e: CommandNotSupportedException) {
            uiSender?.onShowOkDialogue(
              "Command not supported",
              "This NFC device does not support the NFC Forum commands needed to access the config register",
              this
            )
            return
         }
      } else {
         showAuthDialog()
      }
   }

   fun showAuthDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), AuthActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      SharedCodes.log("RegCfgAct", "showAuthDialog");
      ActivityFlutterMediator.startActivityForResult(intent, PseudoMainActivity.AUTH_REQUEST, this::class.java)
   }

   //dartui:
   fun onPTHRU_Dir_Changed(){
      if (PTHRU_DIR_switch == true) {
         WRITE_ACCESS_switch = true
      } else {
         WRITE_ACCESS_switch = false
         PTHRU_DIR_switch = false
      }
   }
   //dartui:
   fun onWRITE_ACCESS_Changed(){
      if (WRITE_ACCESS_switch == true) {
         PTHRU_DIR_switch = true
      } else {
         PTHRU_DIR_switch = false
         WRITE_ACCESS_switch = false
      }
   }
   //platform:
   fun onReadConfigClick(){
      isWriteChosen = false
      if (demo!!.isConnected)
         try {
            demo!!.readWriteConfigRegister()
         } catch (e: CommandNotSupportedException) {
            // can never happen
            e.printStackTrace()
            SharedCodes.log("reg", e.toString())

         }
   }
   //platform:
   fun onWriteConfigClick(){
      if (isWriteChosen == true) {
         // Calculate the Register Values according to what has been
         // selected by the user
         calcConfiguration()
         if (demo!!.isConnected)
            try {
               demo!!.readWriteConfigRegister()
            } catch (e: CommandNotSupportedException) {
               // can never happen
               e.printStackTrace()
               SharedCodes.log("regact", e.toString())

            }
      } else {
         if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value
                 || PseudoMainActivity.authStatus == AuthStatus.Unprotected.value
                 || PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
            isWriteChosen = true
         } else {
            // Enable the write protection check
            isWriteProtected = true
            showAuthDialog()
         }
      }
   }

   fun showAboutDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      SharedCodes.log("RegCfgAct", "showAboutDialog");
      ActivityFlutterMediator.startActivity(intent)
   }

   companion object {
      private var ChipInfo_1_text: String = ""
      private var ChipInfo_2_text: String = ""
      private var I2C_RST_switch: Boolean? = null
      private var FD_OFF_spinner: FD? = null

      private var FD_ON_spinner: FD? = null
      private var LAST_NDEF_PAGE_edit: String = ""
      private var PTHRU_DIR_switch: Boolean? = null
      private var WRITE_ACCESS_switch: Boolean? = null

      private var SRAM_MIRROR_PAGE_edit_edit: String = ""
      private var I2C_WD_LS_Timer_edit: String = ""
      private var I2C_WD_MS_edit: String = ""
      private var I2C_CLOCK_STR_switch: Boolean? = null

      private var PLUS_AUTH0_edit: String = ""
      private var PLUS_NFC_Prot_switch: Boolean? = null
      private var PLUS_NFC_Disc_Sec1_switch: Boolean? = null
      private var PLUS_AUTHLim_edit: String = ""

      private var PLUS_2K_Prot_switch: Boolean? = null
      private var PLUS_Sram_Prot_switch: Boolean? = null
      private var PLUS_I2C_Prot_edit: String = ""

      @JvmStatic var isWriteChosen: Boolean = false

      // return registers
      // set registers
      @JvmStatic var nC_Reg = 0
      @JvmStatic var lD_Reg = 0
      @JvmStatic var sM_Reg = 0
      @JvmStatic var nS_Reg = 0

      @JvmStatic var wD_LS_Reg = 0
      @JvmStatic var wD_MS_Reg = 0
      @JvmStatic var i2C_CLOCK_STR = 0
      @JvmStatic var auth0 = 0
      @JvmStatic var access = 0
      @JvmStatic var ptI2C = 0

      private var FD_OFF_Value = 0
      private var FD_ON_Value = 0

      //Platform:
      @JvmStatic fun setAnswer(answer: Ntag_I2C_Registers) {
         val cont = instance?.registrar?.activity();

         ChipInfo_1_text = answer.Manufacture
         ChipInfo_2_text = answer.Mem_size.toString() + " Bytes"

         // I2C_RST Switch
         I2C_RST_switch = answer.I2C_RST_ON_OFF

         if (answer.FD_OFF == cont?.getString(R.string.FD_OFF_ON_11)) {
            FD_OFF_spinner = FD.OFF3
         }
         if (answer.FD_OFF == cont?.getString(R.string.FD_OFF_ON_10)) {
            FD_OFF_spinner = FD.OFF2
         }
         if (answer.FD_OFF == cont?.getString(R.string.FD_OFF_ON_01)) {
            FD_OFF_spinner = FD.OFF1
         }
         if (answer.FD_OFF == cont?.getString(R.string.FD_OFF_ON_00)) {
            FD_OFF_spinner = FD.OFF0
         }
         if (answer.FD_ON == cont?.getString(R.string.FD_OFF_ON_11)) {
            FD_ON_spinner = FD.ON3
         }
         if (answer.FD_ON == cont?.getString(R.string.FD_OFF_ON_10)) {
            FD_ON_spinner = FD.ON2
         }
         if (answer.FD_ON == cont?.getString(R.string.FD_OFF_ON_01)) {
            FD_ON_spinner = FD.ON1
         }
         if (answer.FD_ON == cont?.getString(R.string.FD_OFF_ON_00)) {
            FD_ON_spinner = FD.ON0
         }

         // Get the Last NDEF Page
         LAST_NDEF_PAGE_edit = answer.LAST_NDEF_PAGE.toString()

         // PassThrough Dir + Write Access
         if (answer.PTHRU_DIR) {
            PTHRU_DIR_switch = true
            WRITE_ACCESS_switch = true
         } else {
            PTHRU_DIR_switch = false
            WRITE_ACCESS_switch = false
         }
         SRAM_MIRROR_PAGE_edit_edit = answer.SM_Reg.toString()
         I2C_WD_LS_Timer_edit = answer.WD_LS_Reg.toString()
         I2C_WD_MS_edit = answer.WD_MS_Reg.toString()

         // SRAM_MIRROR_ON_OFF_switch.setChecked(answer.SRAM_MIRROR_ON_OFF);
         I2C_CLOCK_STR_switch = answer.I2C_CLOCK_STR
      } // END set Answer

      // Platform:
      @JvmStatic fun setAnswerPlus(answer: Ntag_I2C_Plus_Registers) {
         // Auth0 Register
         PLUS_AUTH0_edit = answer.auth0.toString()

         // Access Register
         //		PLUS_NFC_Prot_edit.setText(String.valueOf(answer.nfcProt));
         PLUS_NFC_Prot_switch = answer.nfcProt

         //		PLUS_NFC_Disc_Sec1_edit.setText(String.valueOf(answer.nfcDisSec1));
         PLUS_NFC_Disc_Sec1_switch = answer.nfcDisSec1
         PLUS_AUTHLim_edit = answer.authlim.toString()

         // PT_I2C Register
         PLUS_2K_Prot_switch = answer.k2Prot

         //		PLUS_NFC_Disc_Sec1_edit.setText(String.valueOf(answer.nfcDisSec1));
         PLUS_Sram_Prot_switch = answer.sram_prot
         PLUS_I2C_Prot_edit = answer.i2CProt.toString()
      }

      fun calcConfiguration() {
         FD_OFF_Value = FD_OFF_spinner?.getPos() as Int;
         FD_ON_Value = FD_ON_spinner?.getPos()  as Int;

         if (FD_OFF_Value == 3) {
            nC_Reg = nC_Reg or 0x30
         }

         if (FD_OFF_Value == 2) {
            nC_Reg = nC_Reg and 0xcf
            nC_Reg = nC_Reg or 0x20
         }

         if (FD_OFF_Value == 1) {
            nC_Reg = nC_Reg and 0xcf
            nC_Reg = nC_Reg or 0x10
         }

         if (FD_OFF_Value == 0) {
            nC_Reg = nC_Reg and 0xcf
         }

         if (FD_ON_Value == 3) {
            nC_Reg = nC_Reg or 0x0c
         }

         if (FD_ON_Value == 2) {
            nC_Reg = nC_Reg and 0xf3
            nC_Reg = nC_Reg or 0x08
         }

         if (FD_ON_Value == 1) {
            nC_Reg = nC_Reg and 0xf3
            nC_Reg = nC_Reg or 0x04
         }

         if (FD_ON_Value == 0) {
            nC_Reg = nC_Reg and 0xf3
         }

         if (PTHRU_DIR_switch == true) {
            nC_Reg = nC_Reg or 0x01
         } else {
            nC_Reg = nC_Reg and 0xfe
            PTHRU_DIR_switch = false
         }
         lD_Reg = Integer.parseInt(LAST_NDEF_PAGE_edit)
         sM_Reg = Integer.parseInt(SRAM_MIRROR_PAGE_edit_edit)
         wD_LS_Reg = Integer.parseInt(I2C_WD_LS_Timer_edit)
         wD_MS_Reg = Integer.parseInt(I2C_WD_MS_edit)

         if (I2C_CLOCK_STR_switch == true) {
            i2C_CLOCK_STR = 1
         } else {
            i2C_CLOCK_STR = 0
            I2C_CLOCK_STR_switch = false
         }

         if (I2C_RST_switch == true) {
            nC_Reg = nC_Reg or 0x80
         } else {
            nC_Reg = nC_Reg and 0x7f
            I2C_RST_switch = false
         }
         auth0 = Integer.parseInt(PLUS_AUTH0_edit)

         if (PLUS_NFC_Prot_switch == true) {
            access = access or 0x80
         } else {
            access = access and 0x7f
            PLUS_NFC_Prot_switch = false
         }

         if (PLUS_NFC_Disc_Sec1_switch == true) {
            access = access or 0x20
         } else {
            access = access and 0xdf
            PLUS_NFC_Disc_Sec1_switch = false
         }
         access = access or Integer.parseInt(PLUS_AUTHLim_edit)

         if (PLUS_2K_Prot_switch == true) {
            ptI2C = ptI2C or 0x08
         } else {
            ptI2C = ptI2C and 0xf7
            PLUS_2K_Prot_switch = false
         }

         if (PLUS_Sram_Prot_switch == true) {
            ptI2C = ptI2C or 0x04
         } else {
            ptI2C = ptI2C and 0xfB
            PLUS_Sram_Prot_switch = false
         }
         ptI2C = ptI2C or Integer.parseInt(PLUS_I2C_Prot_edit)
         return
      }

      fun setPlus_Auth0_Reg(plus_Auth0_Reg: Int) {
         auth0 = plus_Auth0_Reg
      }

      fun setPlus_Access_Reg(plus_Access_Reg: Int) {
         access = plus_Access_Reg
      }

      fun setPlus_Pti2c_Reg(plus_Pti2c_Reg: Int) {
         ptI2C = plus_Pti2c_Reg
      }

      @JvmStatic fun updateToDart(){
         val data = mapOf(
              "ChipInfo_1_text" to ChipInfo_1_text,
                 "ChipInfo_2_text" to ChipInfo_2_text,
                 "I2C_RST_switch" to I2C_RST_switch,
                 "FD_OFF_spinner" to FD_OFF_spinner,

                 "FD_ON_spinner" to FD_ON_spinner,
                 "LAST_NDEF_PAGE_edit" to LAST_NDEF_PAGE_edit,
                 "PTHRU_DIR_switch" to PTHRU_DIR_switch,
                 "WRITE_ACCESS_switch" to WRITE_ACCESS_switch,

                 "SRAM_MIRROR_PAGE_edit_edit" to SRAM_MIRROR_PAGE_edit_edit,
                 "I2C_WD_LS_Timer_edit" to I2C_WD_LS_Timer_edit,
                 "I2C_WD_MS_edit" to I2C_WD_MS_edit,
                 "I2C_CLOCK_STR_switch" to I2C_CLOCK_STR_switch,

                 "PLUS_AUTH0_edit" to PLUS_AUTH0_edit,
                 "PLUS_NFC_Prot_switch" to PLUS_NFC_Prot_switch,
                 "PLUS_NFC_Disc_Sec1_switch" to PLUS_NFC_Disc_Sec1_switch,
                 "PLUS_AUTHLim_edit" to PLUS_AUTHLim_edit,

                 "PLUS_2K_Prot_switch" to PLUS_2K_Prot_switch,
                 "PLUS_Sram_Prot_switch" to PLUS_Sram_Prot_switch,
                 "PLUS_I2C_Prot_edit" to PLUS_I2C_Prot_edit,
                 "isWriteChosen" to isWriteChosen,

                 "nC_Reg" to nC_Reg,
                 "lD_Reg" to lD_Reg,
                 "sM_Reg" to sM_Reg,
                 "nS_Reg" to nS_Reg,

                 "wD_LS_Reg" to wD_LS_Reg,
                 "wD_MS_Reg" to wD_MS_Reg,
                 "i2C_CLOCK_STR" to i2C_CLOCK_STR,
                 "auth0" to auth0,

                 "access" to access,
                 "ptI2C" to ptI2C,
                 "FD_OFF_Value" to FD_OFF_Value,
                 "FD_ON_Value" to FD_ON_Value
         )
         Dart.updateToDart(data);
      }
      fun updateToPlatform(call:MethodCall, result: MethodChannel.Result){
         ChipInfo_1_text = call.argument<String>("ChipInfo_1_text") as String
         ChipInfo_2_text = call.argument<String>("ChipInfo_2_text" ) as String
         I2C_RST_switch = call.argument<Boolean>("I2C_RST_switch") as Boolean
         FD_OFF_spinner = FD.from(call.argument<String>("FD_OFF_spinner") as String)

         FD_ON_spinner = FD.from(call.argument<String>("FD_ON_spinner") as String)
         LAST_NDEF_PAGE_edit = call.argument<String>("LAST_NDEF_PAGE_edit" ) as String
         PTHRU_DIR_switch = call.argument<Boolean>("PTHRU_DIR_switch") as Boolean
         WRITE_ACCESS_switch = call.argument<Boolean>("WRITE_ACCESS_switch") as Boolean

         SRAM_MIRROR_PAGE_edit_edit = call.argument<String>("SRAM_MIRROR_PAGE_edit_edit" ) as String
         I2C_WD_LS_Timer_edit = call.argument<String>("I2C_WD_LS_Timer_edit" ) as String
         I2C_WD_MS_edit = call.argument<String>("I2C_WD_MS_edit" ) as String
         I2C_CLOCK_STR_switch = call.argument<Boolean>("I2C_CLOCK_STR_switch" ) as Boolean

         PLUS_AUTH0_edit = call.argument<String>("PLUS_AUTH0_edit") as String
         PLUS_NFC_Prot_switch = call.argument<Boolean>("PLUS_NFC_Prot_switch" ) as Boolean
         PLUS_NFC_Disc_Sec1_switch = call.argument<Boolean>("PLUS_NFC_Disc_Sec1_switch") as Boolean
         PLUS_AUTHLim_edit = call.argument<String>("PLUS_AUTHLim_edit") as String

         PLUS_2K_Prot_switch = call.argument<Boolean>("PLUS_2K_Prot_switch" ) as Boolean
         PLUS_Sram_Prot_switch = call.argument<Boolean>("PLUS_Sram_Prot_switch") as Boolean
         PLUS_I2C_Prot_edit = call.argument<String>("PLUS_I2C_Prot_edit") as String
         isWriteChosen = call.argument<Boolean>("isWriteChosen" ) as Boolean

         nC_Reg = call.argument<Int>("nC_Reg" ) as Int
         lD_Reg = call.argument<Int>( "lD_Reg" ) as Int
         sM_Reg = call.argument<Int>("sM_Reg") as Int
         nS_Reg = call.argument<Int>("nS_Reg" ) as Int

         wD_LS_Reg = call.argument<Int>("wD_LS_Reg" ) as Int
         wD_MS_Reg = call.argument<Int>("wD_MS_Reg" ) as Int
         i2C_CLOCK_STR = call.argument<Int>("i2C_CLOCK_STR") as Int
         auth0 = call.argument<Int>("auth0"  ) as Int

         access = call.argument<Int>("access" ) as Int
         ptI2C = call.argument<Int>("ptI2C" ) as Int
         FD_OFF_Value = call.argument<Int>("FD_OFF_Value") as Int
         FD_ON_Value = call.argument<Int>("FD_ON_Value") as Int
      }

      private var instance: RegisterConfigActivity? = null;
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = RegisterConfigActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = RegisterConfigActivity::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as RegisterConfigActivity,component, intent_filters)
         // initialize PseudoActivity
         instance?.uiSender = Message(registrar, channel)
         SharedCodes.log("RegCfgAct", "registerPlugin")
      }
   }
}

package com.gknot.activities


import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Configuration
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.DeadObjectException
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.gknot.*

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.ntagi2cdemo.R

import com.gknot.pseudo.Message
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.activities.AuthActivity.AuthStatus
import com.gknot.datatypes.CompNamePseudo
import com.gknot.fragments.*
import com.gknot.reader.FlutterNtag_I2C_Demo

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.io.IOException
import java.util.*

private const val MODULE_NAME: String = "PseudoMainActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

class PseudoMainActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {

   private var mPendingIntent: PendingIntent
      get() = SharedCodes.instance.mPendingIntent!!
      set(value){
         SharedCodes.instance.mPendingIntent = value;
      }
   private var mAdapter: NfcAdapter?
      get() = SharedCodes.instance.mAdapter
      set(value){
         SharedCodes.instance.mAdapter = value;
      }
   private var tabname: TABID = TABID.ndef_rf
   var uiSender: Message? = null

   enum class TABID(val data:String){
      leds("leds"),
      ndef("ndef"),
      ndef_rf("ndef_rf"),
      config("config")
   }

   class Dart(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart {
      override var methodname:String? = ""
      fun showSettingNoNfcAlert(inst: PseudoMainActivity){
         //methodname = SHOWSETTING_NO_NFC_ALERT
         inst.channel.invokeMethod(SHOWSETTING_NO_NFC_ALERT, mapOf("" to ("")))
//         inst.uiSender.onShowOkDialogueForAction(
//                 "NFC not enabled",
//                 "Go to Settings?", inst
//         ) {
//            if (it){
//               ActivityFlutterMediator.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
//            }else{
//               System.exit(0)
//            }
//         }
      }
      fun closeAppNoNfcAlert(inst: PseudoMainActivity){
//         methodname = CLOSEAPP_NO_NFC_ALERT
         inst.channel.invokeMethod(CLOSEAPP_NO_NFC_ALERT, mapOf("" to ("")))
//         inst.uiSender.onShowOkDialogue(
//                 "No NFC available. App is going to be closed.",
//                 "Ok", inst
//         )
      }
      fun nfcAvailable(inst: PseudoMainActivity){
//         methodname = CLOSEAPP_NO_NFC_ALERT
         inst.channel.invokeMethod(NFC_AVAILABLE, mapOf("" to ("")))
//         inst.uiSender.onShowOkDialogue(
//                 "No NFC available. App is going to be closed.",
//                 "Ok", inst
//         )
      }
      companion object {
         val CLOSEAPP_NO_NFC_ALERT = "closeAppNoNfcAlert"
         val SHOWSETTING_NO_NFC_ALERT = "showSettingNoNfcAlert"
         val NFC_AVAILABLE = "nfcAvailable"
      }
   }
   class Platform(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelPlatform {
      override var methodname:String? = ""
      companion object {

      }
   }

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: MainActivity) {
      super.onCreate(savedInstanceState, intent, activity)
      // Application package name to be used by the AAR record
      SharedCodes.log(SharedCodes.libTagName(this::class.java),"onCreate")
      instance = this;
      PACKAGE_NAME = activity.packageName
      val languageToLoad = "en"
      val locale = Locale(languageToLoad)
      Locale.setDefault(locale)
      val config = Configuration()
      config.locale = locale

      tabname = TABID.ndef_rf
      // Get App version
      appVersion = ""
      try {
         val pInfo = activity.packageManager.getPackageInfo(
                 activity.packageName, 0)
         appVersion = pInfo.versionName
      } catch (e: NameNotFoundException) {
         e.printStackTrace()
         SharedCodes.log("pseudo", e.toString())
      }

      // Board firmware version
      boardFirmwareVersion = "Unknown"

      // Notifier to be used for the demo changing
      /*tabname!!.setOnTabChangedListener { tabId ->
         if (demo!!.isReady) {
            demo!!.finishAllTasks()
            if (tabId.equals("leds", ignoreCase = true) && demo!!.isConnected) {
               launchDemo(tabId)
            }
         }
         mTabsAdapter!!.onTabChanged(tabId)
      }*/

      try{
         // When we open the application by default we set the status to disabled (we don't know the product yet)
         authStatus = AuthStatus.Disabled.value
         // Initialize the demo in order to handle tab change events
         demo = Ntag_I2C_Demo(null, registrar.activity(), null, 0)
         SharedCodes.log(SharedCodes.libTagName(this::class.java),"demo init")
//         val adp  =  NfcAdapter.getDefaultAdapter(activity);
//         if (adp != null)
//            mAdapter = adp;
         mAdapter = SharedCodes.getNfcAdapter(registrar.activity());
         SharedCodes.log(SharedCodes.libTagName(this::class.java),"mAdapter init")
         setNfcForeground()
      }catch(e: Exception){
         e.printStackTrace()
         SharedCodes.log("pseudo", e.toString())
      }

      try {
         checkNFC()
      }catch(e: Exception){
         e.printStackTrace()
         SharedCodes.log("pseudo", e.toString())
      }
   }


   private fun checkNFCSecondary(){

   }

   @SuppressLint("InlinedApi")
   private fun checkNFC() {
//      val adp  =  NfcAdapter.getDefaultAdapter(registrar.activity());
//      if (adp != null)
      mAdapter = SharedCodes.getNfcAdapter(registrar.activity());
      SharedCodes.log("pseudo", "checkNFC adapter: ${mAdapter}");
      if (mAdapter != null) {
         if (!mAdapter!!.isEnabled) {
            SharedCodes.log("pseudo","showSettingNoNfcAlert")
            Dart(this, null).showSettingNoNfcAlert(this)
         }else{
            SharedCodes.log("pseudo","nfcAvailable")
            Dart(this, null).nfcAvailable(this)
         }
      } else {
         SharedCodes.log("pseudo","closeAppNoNfcAlert")
          Dart(this, null).closeAppNoNfcAlert(this)
      }
   }
   fun setNfcForeground() {
      // Create a generic PendingIntent that will be delivered to this
      // activity. The NFC stack will fill
      // in the intent with the details of the discovered tag before
      // delivering it to this activity.
      mPendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
      /*mPendingIntent = PendingIntent.getActivity(
              registrar.context(), 0,
              Intent(
                      registrar.context(), MainActivity::class.java)
                      .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), 0)*/
      SharedCodes.log(SharedCodes.libTagName(this::class.java),"setNfcForeGround, pendingIntent init")
   }
   public override fun onPause() {
      checkNFC()
      super.onPause()
      if (mAdapter != null) {
         SharedCodes.log(SharedCodes.libTagName(this::class.java),
                 "onPause disableForegroundDispatch")
         try{
            mAdapter!!.disableForegroundDispatch(registrar.activity())
         }catch (e: DeadObjectException){
            // fixme: untested:
//            val adp  =  NfcAdapter.getDefaultAdapter(registrar.activity());
//            if (adp != null)
//               mAdapter = adp;
            mAdapter = SharedCodes.getNfcAdapter(registrar.activity());
            FlutterNtag_I2C_Demo.instance!!.onNFCServiceDead();
         }catch(e: IOException){
            // todo:
         }
      }else{
         try{
            SharedCodes.log(SharedCodes.libTagName(this::class.java),"null adapter!")
            setNfcForeground()
            checkNFC()
         }catch(e: Exception){
            e.printStackTrace()
            SharedCodes.log("pseudo",e.toString());
         }
      }
      if (demo != null && demo.isReady){
         demo.finishAllTasks()
      }

   }
   public override fun onResume() {
      super.onResume()
      checkNFC()
      // Update the Auth Status Icon
      // updateAuthIcon(authStatus)
      if (mAdapter != null) {
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onResume enableForegroundDispatch")
         mAdapter!!.enableForegroundDispatch(
            registrar.activity(), mPendingIntent, null, null)
      }else{
         // fixme: untested:
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onResume2 enableForegroundDispatch")
         setNfcForeground()
         checkNFC()
         if (mAdapter != null)
            mAdapter!!.enableForegroundDispatch(
                    registrar.activity(), mPendingIntent, null, null)
      }
   }

   public override fun onDestroy() {
      super.onDestroy()
      authStatus = 0
      password = null
      nfcIntent = null
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      // Check which request we're responding to
      // Make sure the request was successful
      if ((requestCode == AUTH_REQUEST
                      && resultCode == Activity.RESULT_OK
                      && demo != null
                      && demo!!.isReady)) {
         launchDemo(tabname)
         return true
      }
      return false
   }

   override fun onNewIntent(intent: Intent):Boolean {
      val nfc_intent = intent;
      super.onNewIntent(nfc_intent)
      // Set the pattern for vibration
      val pattern = longArrayOf(0, 100)

      // Set the initial auth parameters
      authStatus = AuthStatus.Disabled.value
      password = null
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onNewIntent")
      // Vibrate on new Intent
      val vibrator = registrar.activity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      vibrator.vibrate(pattern, -1)
      doProcess(nfc_intent)
      return true;
   }

   fun tabPageHandler(tabId:TABID){
      if (demo != null && demo!!.isReady) {
         demo!!.finishAllTasks()
         if (tabId.equals("leds") && demo!!.isConnected) {
            launchDemo(tabId)
         }
      }
      //mTabsAdapter!!.onTabChanged(tabId)
   }
   /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
      menuInflater.inflate(R.menu.menu, menu)

      // Get the reference to the menu item
      mAuthMenuItem = menu.findItem(R.id.action_auth)
      return true
   }

   override fun onOptionsItemSelected(item: MenuItem): Boolean {
      // Handle presses on the action bar items
      when (item.itemId) {
         R.id.action_auth -> {
            showAuthDialog()
            return true
         }
         R.id.action_flash -> {
            showFlashDialog()
            return true
         }
         R.id.action_about -> {
            showAboutDialog()
            return true
         }
         R.id.action_feedback -> {
            sendFeedback()
            return true
         }
         R.id.action_help -> {
            showHlepDialog()
            return true
         }
         R.id.action_debug -> {
            showDebugDialog()
            return true
         }
         else -> return super.onOptionsItemSelected(item)
      }
   }*/

   fun doProcess(nfc_intent: Intent) {
      nfcIntent = nfc_intent
      val tag = nfc_intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      demo = Ntag_I2C_Demo(tag, registrar.activity(), password, authStatus)
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "doProcess, init demo");
      if (demo != null && demo!!.isReady) {
         // Retrieve Auth Status before doing any operation
         authStatus = obtainAuthStatus()
         val currTab = tabname
         launchDemo(currTab)
      }
   }

   private fun launchDemo(currTab: TABID) {
      val activity = registrar.activity();
      if (authStatus == AuthStatus.Authenticated.value) {
         demo!!.Auth(password, AuthStatus.Protected_RW.value)
      }

      // ===========================================================================
      // LED Test
      // ===========================================================================
      if (currTab.data == "leds") {
         //gordianknot....
         if (demo != null && demo!!.isReady) {
            demo!!.finishAllTasks()
            if ( !demo!!.isConnected) {
               throw Exception("Demo should be connected")
            }
         }
         // This demo is available even if the product is protected
         // as long as the SRAM is unprotected
         if ((authStatus == AuthStatus.Disabled.value
                         || authStatus == AuthStatus.Unprotected.value
                         || authStatus == AuthStatus.Authenticated.value
                         || authStatus == AuthStatus.Protected_W.value
                         || authStatus == AuthStatus.Protected_RW.value)) {
            try {
               // if (LedFragment.getChosen()) {
               demo!!.LED()
            } catch (e: Exception) {
               e.printStackTrace()
               SharedCodes.log("pseudo", e.toString())
               LedFragment.setAnswer(activity.getString(R.string.Tag_lost))
            }

         } else {
            uiSender?.onToastMakeText("NTAG I2C Plus memory is protected", Toast.LENGTH_LONG, this)
            showAuthDialog()
         }
      }
      // ===========================================================================
      // NDEF Demo
      // ===========================================================================
      if (currTab.data == "ndef") {
         // This demo is only available when the tag is not protected
         if ((authStatus == AuthStatus.Disabled.value
                         || authStatus == AuthStatus.Unprotected.value
                         || authStatus == AuthStatus.Authenticated.value)) {
            NdefFragment.setAnswer("Tag detected", false, NdefFragment.Dart.DETECTED_RESPONSE)
            try {
               demo!!.NDEF()
            } catch (e: Exception) {
               // NdefFragment.setAnswer(getString(R.string.Tag_lost));
            }
         } else {
            if (NdefFragment.isWriteChosen){
               NdefFragment.setAnswer(
                       "NTAG I2C Plus memory is protected",
                       true, NdefFragment.Dart.WRITE_PROTECTED);
            }else{
               NdefFragment.setAnswer(
                       "NTAG I2C Plus memory is protected",
                       true, NdefFragment.Dart.READ_PROTECTED);
            }

            uiSender?.onToastMakeText("NTAG I2C Plus memory is protected",Toast.LENGTH_LONG, this)

            showAuthDialog()
         }
      }
      // ===========================================================================
      // Config
      // ===========================================================================
      if (currTab.data == "config") {
      }
      // ===========================================================================
      // Speedtest
      // ===========================================================================
      if (currTab.data == "ntag_rf") {
         try {
            // SRAM Test
            if ((SpeedTestFragment.isSRamEnabled == true)) {
               // This demo is available even if the product is protected
               // as long as the SRAM is unprotected
               if ((authStatus == AuthStatus.Disabled.value
                               || authStatus == AuthStatus.Unprotected.value
                               || authStatus == AuthStatus.Authenticated.value
                               || authStatus == AuthStatus.Protected_W.value
                               || authStatus == AuthStatus.Protected_RW.value)) {
                  demo!!.SRAMSpeedtest()
               } else {
                  uiSender?.onToastMakeText("NTAG I2C Plus memory is protected",Toast.LENGTH_LONG, this)
                  showAuthDialog()
               }
            }
            // EEPROM Test
            if ((SpeedTestFragment.isSRamEnabled == false)) {
               // This demo is only available when the tag is not protected
               if ((authStatus == AuthStatus.Disabled.value
                               || authStatus == AuthStatus.Unprotected.value
                               || authStatus == AuthStatus.Authenticated.value)) {
                  demo!!.EEPROMSpeedtest()
               } else {
                  uiSender?.onToastMakeText("NTAG I2C Plus memory is protected",Toast.LENGTH_LONG, this)
                  showAuthDialog()
               }
            } // end if eeprom test
         } catch (e: Exception) {
            SpeedTestFragment.setAnswer(activity.getString(R.string.Tag_lost))
            e.printStackTrace()
            SharedCodes.log("flma",e.toString())
         }
      }
   }

   private fun obtainAuthStatus(): Int {
      authStatus = demo!!.ObtainAuthStatus()
      // Update the Auth Status Icon
      // updateAuthIcon(authStatus)
      return authStatus
   }

   fun sendFeedback() {
      val intent = Intent(Intent.ACTION_SENDTO)
      intent.type = "text/plain"
      intent.putExtra(Intent.EXTRA_SUBJECT,
              registrar.activity().getString(R.string.email_titel_feedback))
      intent.putExtra(Intent.EXTRA_TEXT, ("Android Version: "
              + android.os.Build.VERSION.RELEASE + "\nManufacurer: "
              + android.os.Build.MANUFACTURER + "\nModel: "
              + android.os.Build.MODEL + "\nBrand: " + android.os.Build.BRAND
              + "\nDisplay: " + android.os.Build.DISPLAY + "\nProduct: "
              + android.os.Build.PRODUCT + "\nIncremental: "
              + android.os.Build.VERSION.INCREMENTAL))
      intent.data = Uri.parse(registrar.activity().getString(R.string.support_email))
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      SharedCodes.log("Pseudo", "sendFeedBack");
      ActivityFlutterMediator.startActivity(intent)
   }

   fun showHlepDialog() {
      throw NotImplementedError("help not implemented")
      /*var intent: Intent? = null
      intent = Intent(registrar.activity(), HelpActivity::class.java)
      ActivityFlutterMediator.
      startActivity(intent)*/
   }

   fun showDebugDialog() {
      throw NotImplementedError("debug not implemented")
      /*var intent: Intent? = null
      intent = Intent(registrar.activity(), DebugActivity::class.java)
      ActivityFlutterMediator.
      startActivity(intent)*/
   }

   fun showAboutDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent!!)
      SharedCodes.log("pseudo", "showAboutDialog")
      ActivityFlutterMediator.startActivity(intent)
   }

   fun showFlashDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), FlashMemoryActivity::class.java)
      SharedCodes.log("pseudo", "showFlashDialog")
      ActivityFlutterMediator.startActivity(intent)
   }

   fun showAuthDialog() {
      if (authStatus != AuthStatus.Disabled.value) {
         var intent: Intent? = null
         intent = Intent(registrar.activity(), AuthActivity::class.java)
         if (PseudoMainActivity.nfcIntent != null)
            intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
         SharedCodes.log("pseudo", "showAuthDialog");
         ActivityFlutterMediator.startActivityForResult(intent, AUTH_REQUEST, this::class.java)
      } else {
         uiSender?.onToastMakeText("You need to tap a NTAG I2C " + "Plus product to access authentication features", Toast.LENGTH_LONG, this)
      }
   }



   /**
    * Set the Icon that informs the user about the protection status.
    */
   /*fun updateAuthIcon(status: Int) {
      val resources = registrar.activity().resources
      if (mAuthMenuItem != null) {
         if (status == AuthStatus.Disabled.value) {
            mAuthMenuItem!!.icon = resources.getDrawable(R.drawable.disabled)
         } else if (status == AuthStatus.Unprotected.value) {
            mAuthMenuItem!!.icon = resources.getDrawable(R.drawable.unlock)
         } else if (status == AuthStatus.Authenticated.value) {
            mAuthMenuItem!!.icon = resources.getDrawable(R.drawable.authenticated)
         } else if ((status == AuthStatus.Protected_RW.value
                         || status == AuthStatus.Protected_W.value
                         || status == AuthStatus.Protected_RW_SRAM.value
                         || status == AuthStatus.Protected_W_SRAM.value)) {
            mAuthMenuItem!!.icon = resources.getDrawable(R.drawable.lock)
         }
      }
   }*/

   companion object {
      @JvmStatic var demo: Ntag_I2C_Demo
         get() = SharedCodes.instance.demo!!
         set(value){
            SharedCodes.instance.demo = value
         }

      @JvmStatic val AUTH_REQUEST = 0
      @JvmStatic var PACKAGE_NAME: String = ""

      // Android app Version
      private var appVersion = ""

      // Board firmware Version
      @JvmStatic var boardFirmwareVersion = ""

      // referenced in RegisterSessionActivity
      var nfcIntent: Intent? = null

      // Current authentication state
      // ===========================================================================
      // NTAG I2C Plus getters and setters
      // ===========================================================================
      // referenced in RegeisterSessionActivity
      // referenced in RegisterSessionActivity
      @JvmStatic var authStatus: Int = 0

      // Current used password
      // referenced in RegisterSessionActivity
      @JvmStatic var password: ByteArray? = null

      @JvmStatic fun getmIntent(): Intent? {
         return nfcIntent
      }

      /**
       * NDEF Demo execution is launched from its fragmend.
       */
      fun launchNdefDemo(auth: Int, pwd: ByteArray?, result: MethodChannel.Result?) {
         if (demo != null && demo!!.isReady) {
            if (demo!!.isConnected) {
               if (auth == AuthStatus.Authenticated.value) {
                  demo!!.Auth(pwd, AuthStatus.Protected_RW.value)
               }
               //NdefFragment.setAnswer("Tag detected", false, NdefFragment.Dart.DETECTED_RESPONSE)
               try {
                  demo!!.NDEF();
                  if (NdefFragment.isWriteChosen){
                     NdefFragment.setAnswer("successfully written",true, NdefFragment.Dart.WRITE_SUCCESS)
                     result?.success(true);
                  }else{

                     NdefFragment.setAnswer("successfully read",true, NdefFragment.Dart.READ_SUCCESS)
                     result?.success(true);
                  }
               } catch (e: Exception) {
                  if (NdefFragment.isWriteChosen){
                     NdefFragment.setAnswer("Tag lost, try again",true, NdefFragment.Dart.WRITE_LOST)
                     result?.success(false);
                  }else{
                     NdefFragment.setAnswer("Tag lost, try again",true, NdefFragment.Dart.READ_LOST)
                     result?.success(false);
                  }
                  e.printStackTrace()
                  SharedCodes.log("pseudo", e.toString())
               }
            } else {
               if (NdefFragment.isWriteChosen) {
                  NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
                  result?.success(false);
               } else {
                  NdefFragment.setAnswer("please tap tag to read", false, NdefFragment.Dart.TAP_TOREAD)
                  result?.success(false);
               }
            }
         }
      }

      fun launchNdefDemoCustom( startAty:Byte, bytes: ByteArray, length:Int, result: MethodChannel.Result?) {
         if (demo != null && demo!!.isReady) {
            if (demo!!.isConnected) {
               try {
                  demo!!.NDEFCustom(startAty, bytes, length, result);
               } catch (e: Exception) {
                  if (NdefFragment.isWriteChosen){
                     NdefFragment.setAnswer("Tag lost, try again",true, NdefFragment.Dart.WRITE_LOST)
                     result?.success(false);
                  }else{
                     NdefFragment.setAnswer("Tag lost, try again",true, NdefFragment.Dart.READ_LOST)
                     result?.success(false);
                  }
                  e.printStackTrace()
                  SharedCodes.log("pseudo", e.toString())
               }
            } else {
               if (NdefFragment.isWriteChosen) {
                  NdefFragment.setAnswer("please tap tag to write", false, NdefFragment.Dart.TAP_TOWRITE)
                  result?.success(false);
               } else {
                  NdefFragment.setAnswer("please tap tag to read", false, NdefFragment.Dart.TAP_TOREAD)
                  result?.success(false);
               }
            }
         }
      }

      @JvmStatic
      var instance: BasePseudoFlutterActivity? = null;

      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = PseudoMainActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = PseudoMainActivity::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as PseudoMainActivity,component, intent_filters)
         // initialize PseudoActivity
         (instance as PseudoMainActivity).uiSender = Message(registrar, channel)
         SharedCodes.log("PdoManAct", "registerPlugin")
      }
   }
}

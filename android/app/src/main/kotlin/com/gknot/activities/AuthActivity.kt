package com.gknot.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import com.gknot.*

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.ntagi2cdemo.R

import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.pseudo.Message
import com.gknot.reader.FlutterNtag_I2C_Demo

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

private const val MODULE_NAME: String = "AuthActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

/*
todo:
   [X] shared properties
         - demo
         - pendingIntent
         - mAdapter
   [X] lifecycle delegate
*/

class AuthActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
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

   private var statusText: String?   = null
   private var uiSender: Message? = null;
   enum class AuthStatus private constructor(val value: Int) {
      Disabled(0),
      Unprotected(1),
      Authenticated(2),
      Protected_W(3),
      Protected_RW(4),
      Protected_W_SRAM(5),
      Protected_RW_SRAM(6)
   }

   enum class Pwds private constructor(val value: ByteArray) {
      PWD1(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())),
      PWD2(byteArrayOf(0x55.toByte(), 0x55.toByte(), 0x55.toByte(), 0x55.toByte())),
      PWD3(byteArrayOf(0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte(), 0xAA.toByte()))
   }

   private class Dart (
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart {
      override var methodname: String? = null
      fun showAuthenticating(msg:String, prod:String){
         methodname = SHOW_AUTH
         ch?.invokeMethod("showAuthenticating", mapOf(
                 "msg" to msg,
                 "prod" to prod
         ))
      }
      fun closeAuthenticating(){
         methodname = CLOSE_AUTH
         ch?.invokeMethod("closeAuthenticating", null)
      }
      fun showToastText(sender: Message, msg:String, time: Int){
         methodname = SHOW_TOAST
         sender.onToastMakeText(msg, time, AuthActivity)
      }
      fun showYNDialogue(sender: Message, title:String, msg:String, action: (Boolean) -> Unit){
         methodname = SHOW_YN_DIALOG
         sender.onShowYNDialogueForAction(msg, title, action, AuthActivity)
      }
      fun closeYNDialogue(sender: Message){
         methodname = CLOSE_YN_DIALOG
         sender.closeYNDialogue(AuthActivity)
      }
      companion object {
         val SHOW_AUTH = "showAuthenticating"
         val CLOSE_AUTH = "closeAuthenticating"
         val SHOW_TOAST = "showToastText"
         val SHOW_YN_DIALOG = "showYNDialogue"
         val CLOSE_YN_DIALOG = "closeYNDialogue"
      }
   }
   /*
   *     calls from Dart to Platform or some constants passed from Dart to Platform
   */
   private class Platform (){
      companion object {
         fun updateToPlatform(call: MethodCall, result: MethodChannel.Result){
         }
         // dartui: rewrite into ui
         fun updateAuthPwdTask(text:String){
            instance?.updateAuthPwdTask(text)
         }
         // dartui: rewrite into ui
         fun showDisableAuthenticationDialog(){
            instance?.showDisableAuthenticationDialog()
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);

      // Capture intent to check whether the operation should be automatically launch or not
      mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
      if (mTag != null && Ntag_I2C_Demo.isTagPresent(mTag)) {
         //demo =
         Ntag_I2C_Demo(
                 mTag, activity,
                 PseudoMainActivity.password, PseudoMainActivity.authStatus)
      }
      // Add Foreground dispatcher
      mAdapter = NfcAdapter.getDefaultAdapter(activity)
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
      statusText = "Auth_Disabled"

      // Set the Auth Status on the screen
      updateAuthStatus(PseudoMainActivity.authStatus)
      return  // end onCreate
   }

   public override fun onPause() {
      super.onPause()
      if (mAdapter != null) {
         mAdapter?.disableForegroundDispatch(registrar.activity())
      }
   }

   public override fun onResume() {
      super.onResume()
      if (mAdapter != null) {
         mAdapter?.enableForegroundDispatch(
           registrar.activity(),
           pendingIntent, null, null)
      }
   }

   private fun setPassword(bytes: ByteArray){
      PseudoMainActivity.password = bytes
   }
   private fun setPassword(text:String, charset:Charset = StandardCharsets.US_ASCII){
      val bytes = text.toByteArray(charset)
      setPassword(bytes);
   }
   fun updateAuthPwdTask(bytes: ByteArray){
      setPassword(bytes)
      startDemo(mTag)
   }
   fun updateAuthPwdTask(text:String){
      setPassword(text)
      startDemo(mTag)
   }


   override fun onNewIntent(intent: Intent):Boolean {
      return true;
      /*val nfc_intent: Intent = intent;
      val activity = registrar.activity();
      super.onNewIntent(nfc_intent)
      // Set the pattern for vibration
      val pattern = longArrayOf(0, 100)

      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onNewIntent")
      // Vibrate on new Intent
      val vibrator = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
      vibrator.vibrate(pattern, -1)

      // Get the tag and start the demo
      val tag = nfc_intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      // demo =
      Ntag_I2C_Demo(tag, activity, PseudoMainActivity.password, PseudoMainActivity.authStatus)
      PseudoMainActivity.authStatus = demo.ObtainAuthStatus()

      // This authentication is added in order to avoid authentication problems with old NFC Controllers
      if (PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
         demo.Auth(PseudoMainActivity.password, AuthStatus.Protected_RW.value)
      }

      // Set the Auth Status on the screen
      updateAuthStatus(PseudoMainActivity.authStatus)
      return true;*/
   }

   private fun startDemo(tag: Tag?) {
      // This authentication is added in order to avoid authentication problems with old NFC Controllers
      if (PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
         demo.Auth(PseudoMainActivity.password, AuthStatus.Protected_RW.value)
      }
      if (demo != null && demo.isReady) {
         // Launch the thread
         task = authTask()
         task!!.execute()
      }
   }

   private inner class authTask : AsyncTask<Intent, Int, Boolean>() {
      override fun onPostExecute(success: Boolean?) {
         // Inform the user about the task completion
         authCompleted(success!!)
         // Action completed
         Dart(instance!!, null).closeAuthenticating()
      }

      override fun doInBackground(vararg nfc_intent: Intent): Boolean {
         // Perform auth operation based on the actual status
         return demo.Auth(
                 PseudoMainActivity.password,
                 PseudoMainActivity.authStatus)
      }

      override fun onPreExecute() {
         // Show the progress dialog on the screen to inform about the action
         /*dialog = ProgressDialog.show(
           this@AuthActivity, "Authenticating",
           "Authenticating against NTAG I2C Plus ...",
           true, true
         )*/
         Dart(instance!!, null).showAuthenticating(
                 "Authenticating",
                 "Authenticating against NTAG I2C Plus ..."         )
      }
   }

   fun authCompleted(success: Boolean) {
      val activity = registrar.activity()
      if (success) {
         // Update the status
         if (PseudoMainActivity.authStatus == AuthStatus.Unprotected.value) {
            PseudoMainActivity.authStatus = AuthStatus.Authenticated.value
            Dart(instance!!, null).showToastText(
                    uiSender as Message,
                    "Tag Successfully protected",
                    Toast.LENGTH_SHORT)
            /*Toast.makeText(mContext, "Tag Successfully protected", Toast.LENGTH_SHORT)
                    .show()*/

            // Authenticate in order to let the user use the demos
            demo.Auth(PseudoMainActivity.password, AuthStatus.Protected_RW.value)
         } else if (PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
            PseudoMainActivity.authStatus = AuthStatus.Unprotected.value
            Dart(instance!!, null).showToastText(
                    uiSender as Message,
                    "Tag Successfully unprotected",
                    Toast.LENGTH_SHORT)
            /*Toast.makeText(
                    mContext, "Tag Successfully unprotected",
                    Toast.LENGTH_SHORT).show()*/
         } else if (PseudoMainActivity.authStatus == AuthStatus.Protected_RW.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_W.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_RW_SRAM.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_W_SRAM.value) {
            PseudoMainActivity.authStatus = AuthStatus.Authenticated.value
            Dart(instance!!, null)
                    .showToastText(uiSender as Message, "Successful authentication", Toast.LENGTH_SHORT)
            /*Toast.makeText(mContext, "Successful authentication", Toast.LENGTH_SHORT)
                    .show()*/
         }
         updateAuthStatus(PseudoMainActivity.authStatus)

         // Prepare the result intent for the PseudoMainActivity
         val resultIntent = Intent()
         activity.setResult(Activity.RESULT_OK, resultIntent)
         ActivityFlutterMediator.finish(this, LifeCycle.onStart)
      } else {
         if (PseudoMainActivity.authStatus == AuthStatus.Unprotected.value) {
            /*Toast.makeText(mContext, "Error protecting tag",
                    Toast.LENGTH_SHORT).show()*/
            Dart(instance!!, null).showToastText(uiSender as Message,
                    "Error protecting tag",
                     Toast.LENGTH_SHORT)
         } else if (PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
            /*Toast.makeText(
                    mContext,
                    "Error unprotecting tag",
                    Toast.LENGTH_SHORT).show()*/
            Dart(instance!!, null).showToastText(uiSender as Message,
                    "Error protecting tag",
                    Toast.LENGTH_SHORT)
         } else if (PseudoMainActivity.authStatus == AuthStatus.Protected_RW.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_W.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_RW_SRAM.value
                 || PseudoMainActivity.authStatus == AuthStatus.Protected_W_SRAM.value) {
            /*Toast.makeText(mContext, "Password was not correct, please try again",
                    Toast.LENGTH_SHORT).show()*/
            Dart(instance!!, null).showToastText(uiSender as Message,
                    "Password was not correct, please try again",
                    Toast.LENGTH_SHORT)
         }
      }
   }

   /*
   *     to put it simply -> update statusText
   */
   fun updateAuthStatus(status: Int) {
      val resources = registrar.activity().resources;
      if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value) {
         statusText = resources.getString(R.string.Auth_disabled)
      } else if (PseudoMainActivity.authStatus == AuthStatus.Unprotected.value) {
         statusText = resources.getString(R.string.Auth_unprotected)
      } else if (PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
         statusText = resources.getString(R.string.Auth_authenticated)
      } else if (PseudoMainActivity.authStatus == AuthStatus.Protected_RW.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_RW_SRAM.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W_SRAM.value) {
         statusText = resources.getString(R.string.Auth_protected)
      }
   }

   fun showDisableAuthenticationDialog() {
      val resources = registrar.activity().resources;
      /*
      // Use the Builder class for convenient dialog construction
      val builder = AlertDialog.Builder(this@AuthActivity)

      builder.setTitle(resources.getString(R.string.Dialog_disable_auth_title))
      builder.setMessage(resources.getString(R.string.Dialog_disable_auth_msg))
      builder.setPositiveButton("YES") { dialog, index ->
         // Unprotect tag
         startDemo(mTag)
         diaSharedCodes.logismiss()
      }
      builder.setNegativeButton("NO") { dialog, id ->
         // We are done with this view
         finish()
         diaSharedCodes.logismiss()
      }
      // Create the AlertDialog object and return it
      builder.create()
      builder.show()*/

      Dart(instance!!, null).showYNDialogue(
              uiSender as Message,
              resources.getString(R.string.Dialog_disable_auth_title),
              resources.getString(R.string.Dialog_disable_auth_msg)
      ) {
         if (it){
            startDemo(mTag)
         }else{
            Dart(instance!!, null).closeYNDialogue(uiSender as Message)
            ActivityFlutterMediator.finish(this, LifeCycle.onStart)
         }
      }
   }

   companion object {
      private var task: authTask? = null
      private var mTag: Tag? = null

      private var instance: AuthActivity? = null;
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = AuthActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = AuthActivity::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as AuthActivity,component, intent_filters)
         // initialize PseudoActivity
         instance?.uiSender = Message(registrar, channel)
         SharedCodes.log("AutAct", "registerPlugin")
      }
   }
}

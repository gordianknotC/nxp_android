package com.gknot.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.ChannelDart
import com.gknot.MainActivity
import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.pseudo.Message
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.ntagi2cdemo.R

import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


private const val MODULE_NAME: String = "ResetMemoryActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

class ResetMemoryActivity (registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {
   private var pendingIntent: PendingIntent? = null
   private var mAdapter: NfcAdapter? = null
   private val demo: Ntag_I2C_Demo?
      get() = FlutterNtag_I2C_Demo.instance?.host;
   private var uiSender: Message? = null

   class Dart(
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart {
      override var methodname: String? = null
      fun showFormattingDialogue(title:String, msg:String){
         methodname = "showFormattingDialogue"
         ch?.invokeMethod("showFormattingDialogue", mapOf(
                 "title" to title,
                 "msg" to msg
         ))
      }
      fun informFormattingComplete(  datarate:String, bytes: Int, time:Long){
         methodname = "informFormattingComplete"
         ch?.invokeMethod("informFormattingComplete", mapOf(
                 "datarate" to datarate,
                 "bytes" to bytes,
                 "time" to time
         ))
      }
   }

   private class Platform (){
      companion object {
      }
   }

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);
      val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      if (tag != null && Ntag_I2C_Demo.isTagPresent(tag)) {
         startDemo(tag, false)
      }
      // Add Foreground dispatcher
      mAdapter = NfcAdapter.getDefaultAdapter(activity)
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
   }

   public override fun onPause() {
      super.onPause()
      if (mAdapter != null) {
         mAdapter!!.disableForegroundDispatch(registrar.activity())
      }
   }

   public override fun onResume() {
      super.onResume()
      if (mAdapter != null) {
         mAdapter!!.enableForegroundDispatch(
                 registrar.activity(),
                 pendingIntent, null, null)
      }
   }

   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      // Check which request we're responding to
      // Make sure the request was successful
      if (requestCode == PseudoMainActivity.AUTH_REQUEST
              && resultCode == Activity.RESULT_OK
              && demo != null
              && demo!!.isReady) {
         // Launch the thread
         RestTask().execute()
         return true
      }
      return false
   }

   override fun onNewIntent(intent: Intent): Boolean {
      val nfcIntent = intent;
      // Set the initial auth parameters to current AuthStatus
      PseudoMainActivity.authStatus = AuthActivity.AuthStatus.Disabled.value
      // set password to current auth
      PseudoMainActivity.password = null
      // set intent information to replace the old one
      PseudoMainActivity.nfcIntent = nfcIntent

      // Complete the task in a new thread in order to be able to show the dialog
      val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      startDemo(tag, true)
      return true
   }

   private fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      //demo =
      Ntag_I2C_Demo(tag, registrar.activity(),
              PseudoMainActivity.password,
              PseudoMainActivity.authStatus)
      if (!demo!!.isReady) {
         return
      }

      // Retrieve the Auth Status
      if (getAuthStatus) {
         PseudoMainActivity.authStatus = demo!!.ObtainAuthStatus()
      }

      // Demo is only available when the tag is not protected
      if (PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Disabled.value
              || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Unprotected.value
              || PseudoMainActivity.authStatus == AuthActivity.AuthStatus.Authenticated.value) {
         // Launch the thread
         RestTask().execute()
      } else {
         showAuthDialog()
      }
   }

   fun showAuthDialog() {
      val intent: Intent = Intent(registrar.activity(), AuthActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      ActivityFlutterMediator.
      startActivityForResult(intent, PseudoMainActivity.AUTH_REQUEST, this::class.java)
   }

   fun setDataRate(bytes: Int, time: Long) {
      if (bytes > 0) {
         var readTimeMessage = ""
         // Transmission Results
         readTimeMessage += "NTAG Memory reset\n"
         readTimeMessage += ("Speed (" + bytes + " Byte / "
                 + time + " ms): "
                 + String.format("%.0f", bytes / (time / 1000.0))
                 + " Bytes/s")
         // Make the board input layout visible
         /*(findViewById<View>(R.id.layoutResetMemoryStatistics) as LinearLayout).visibility = View.VISIBLE
         dataRate_callback.text = readTimeMessage*/
         datarateCallback = readTimeMessage;
         //uiSender?.onSetDataRate(readTimeMessage, bytes, time, this)
      } else {
         // Make the board input layout visible
         /*(findViewById<View>(R.id.layoutResetMemoryStatistics) as LinearLayout).visibility = View.GONE
         dataRate_callback.text = ""*/
         datarateCallback = ""
         //uiSender?.onSetDataRate("", 0, 0, this)
      }
   }

   fun contentReseted(bytes: Int) {
      if (bytes > 0) {
         uiSender?.onToastMakeText("Reset Completed", Toast.LENGTH_SHORT, this)
      } else {
         uiSender?.onToastMakeText("Error during memory content reset", Toast.LENGTH_SHORT, this)
      }
   }

   fun showAboutDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      ActivityFlutterMediator.startActivity(intent)
   }

   companion object {
      var datarateCallback:String = "";
      var instance: ResetMemoryActivity? = null
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = ResetMemoryActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         val entity_name    = ResetMemoryActivity::class.java.name
         val component      = CompNamePseudo.unflattenFromString(entity_name)
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as ResetMemoryActivity,component, intent_filters)
         // initialize PseudoActivity
         instance?.uiSender = Message(registrar, channel)
         SharedCodes.log("RstMemAct", "registerPlugin")
      }

      private class RestTask : AsyncTask<Intent, Int, Int>() {
         private var timeToResetMemory: Long = 0

         override fun onPostExecute(bytes: Int?) {
            // Inform the user about the task completion
            if (bytes != null){
               instance?.contentReseted(bytes)
               instance?.setDataRate(bytes, timeToResetMemory)
            }else{
               throw ExceptionInInitializerError("bytes should not be null")
            }
            // Action completed
            //dialog!!.dismiss()
            Dart(instance!!, null)
              .informFormattingComplete(datarateCallback, bytes, timeToResetMemory);
         }

         override fun doInBackground(vararg nfc_intent: Intent): Int? {
            val regTimeOutStart = System.currentTimeMillis()
            // Reset the tag content
            var bytes = 0
            // Reset Tag demo will return the number of bytes written
            bytes = instance?.demo!!.resetTagMemory()
            // Memory erase time statistics
            timeToResetMemory = System.currentTimeMillis() - regTimeOutStart
            return bytes
         }

         override fun onPreExecute() {
            // Show the progress dialog on the screen to inform about the action
            /*dialog = ProgressDialog.show(this@ResetMemoryActivity, "Formatting",
                    "Reseting memory content ...", true, true)*/
            Dart(instance!!, null)
              .showFormattingDialogue("Formatting", "Reseting memory content ...")
         }
      }
   }
}

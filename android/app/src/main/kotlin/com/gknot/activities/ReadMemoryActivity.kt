package com.gknot.activities


import java.io.IOException
import java.util.Locale

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.ChannelDart
import com.gknot.MainActivity
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.pseudo.Message

import com.gknot.activities.AuthActivity.AuthStatus
import com.gknot.datatypes.CompNamePseudo
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.reader.Ntag_Get_Version.Prod
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "ReadMemoryActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

/*
todo:
   [X] shared properties
         - demo
         - pendingIntent
         - mAdapter
   [X] lifecycle delegate
*/

class ReadMemoryActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel)  {
   private var pendingIntent: PendingIntent? = null
   private var mAdapter: NfcAdapter? = null
   private val demo: Ntag_I2C_Demo?
      get() = FlutterNtag_I2C_Demo.instance?.host;
   private var uiSender: Message? = null

   private class Dart (
           override val target: BasePseudoFlutterActivity,
           override var cb: ((Any?) -> Unit)?): ChannelDart {
      override var methodname: String? = null
      // dartui:
      fun showReadMemory(content:MutableList<String>, datarateCallback:String){
         methodname = SHOW_READ_MEM
         ch?.invokeMethod("showReadMemory", mapOf(
                 "content" to content.toTypedArray(),
                 "datarate" to datarateCallback
         ))
      }
      // dartui:
      fun showProgressDialog(title:String, msg:String){
         methodname = SHOW_PROGRESS_DIALOG;
         ch?.invokeMethod("showProgressDialog", mapOf(
                 "title" to title,
                 "msg" to msg
         ))
      }
      companion object {
         val SHOW_READ_MEM = "showReadMemory";
         val SHOW_PROGRESS_DIALOG = "showProgressDialog"
      }
   }

   private class Platform (){
      companion object {
      }
   }

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);
      // Capture intent to check whether the operation should be automatically launch or not
      val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      if (tag != null && Ntag_I2C_Demo.isTagPresent(tag)) {
         startDemo(tag, false)
      }

      // Add Foreground dispatcher
      mAdapter = SharedCodes.getNfcAdapter(activity); // NfcAdapter.getDefaultAdapter(registrar.activity())
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
         readTask().execute()
         return true
      }
      return false
   }

   override fun onNewIntent(intent: Intent): Boolean {
      val nfcIntent = intent
      // Set the initial auth parameters
      PseudoMainActivity.authStatus = AuthStatus.Disabled.value
      PseudoMainActivity.password = null

      // Store the intent information
      PseudoMainActivity.nfcIntent = nfcIntent

      val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      startDemo(tag, true)
      return true
   }

   private fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      // Complete the task in a new thread in order to be able to show the dialog
      // demo =
      Ntag_I2C_Demo(tag, registrar.activity(), PseudoMainActivity.password, PseudoMainActivity.authStatus)
      if (!demo!!.isReady) {
         return
      }
      // Retrieve the Auth Status
      if (getAuthStatus) {
         PseudoMainActivity.authStatus = demo!!.ObtainAuthStatus()
      }
      if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value
              || PseudoMainActivity.authStatus == AuthStatus.Unprotected.value
              || PseudoMainActivity.authStatus == AuthStatus.Authenticated.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W.value
              || PseudoMainActivity.authStatus == AuthStatus.Protected_W_SRAM.value) {
         // Launch the thread
         readTask().execute()
      } else {
         showAuthDialog()
      }
   }

   fun showAuthDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), AuthActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      SharedCodes.log("RMemAct", "showAuthDialog");
      ActivityFlutterMediator.startActivityForResult(intent, PseudoMainActivity.AUTH_REQUEST, this::class.java)
   }

   fun setContent(b: ByteArray?) {
      val ret = mutableListOf<String>();
      // Check if the data has successfully been read
      if (b == null) {
         uiSender?.onShowAlert("Error reading the memory content", "", this);
      } else {
         val div = 1
         var i = 0
         var j = 0
         while (i < b.size) {
            var sPage = ""
            try {
               if (demo!!.product == Prod.NTAG_I2C_2k_Plus) {
                  if (j / div in 226..255) {
                     // Make sure we don't lose data
                     i -= 4
                     i += 4
                     j++
                     continue
                  }
               }
            } catch (e: IOException) {
               e.printStackTrace()
               SharedCodes.log("readma",e.toString())
            }

            // Page Number
            sPage += ("["
                    + "000".substring(Integer.toHexString(j / div)
                    .length)
                    + Integer.toHexString(j / div).toUpperCase(
                    Locale.getDefault()) + "]  ")

            // Hexadecimal values

            sPage += ("00".substring(Integer.toHexString(
                    b[i].toInt() and 0xFF).length)
                    + Integer.toHexString(b[i].toInt() and 0xFF).toUpperCase(
                    Locale.getDefault())
                    + ":"
                    + "00".substring(Integer.toHexString(b[i + 1].toInt() and 0xFF)
                    .length)
                    + Integer.toHexString(b[i + 1].toInt() and 0xFF).toUpperCase(
                    Locale.getDefault())
                    + ":"
                    + "00".substring(Integer.toHexString(b[i + 2].toInt() and 0xFF)
                    .length)
                    + Integer.toHexString(b[i + 2].toInt() and 0xFF).toUpperCase(
                    Locale.getDefault())
                    + ":"
                    + "00".substring(Integer.toHexString(b[i + 3].toInt() and 0xFF)
                    .length)
                    + Integer.toHexString(b[i + 3].toInt() and 0xFF).toUpperCase(
                    Locale.getDefault()) + " ")

            // ASCII values
            if (j > 3) {
               val tempAsc = ByteArray(4)

               // Only printable characters are displayed
               for (k in 0..3) {
                  if (b[i + k] < 0x20 || b[i + k] > 0x7D)
                     tempAsc[k] = '.'.toByte()
                  else
                     tempAsc[k] = b[i + k]
               }
               sPage += ("|" + String(tempAsc) + "|")
            }
            ret.add(sPage)
            i += 4
            j++
         }
      }
      content = ret
   }

   fun setDataRate(b: ByteArray?, time: Long) {
      if (b != null) {
         var readTimeMessage = ""

         // Transmission Results
         readTimeMessage += "NTAG Memory read\n"
         readTimeMessage += ("Speed (" + b.size + " Byte / "
                 + time + " ms): "
                 + String.format("%.0f", b.size / (time / 1000.0))
                 + " Bytes/s")
         datarateCallback = readTimeMessage
      }
   }

   private inner class readTask : AsyncTask<Intent, Int, ByteArray>() {
      private var timeToReadMemory: Long = 0

      override fun onPostExecute(bytes: ByteArray) {
         // Action completed
         //dialog.dismiss()
         setContent(bytes)
         setDataRate(bytes, timeToReadMemory)
         Dart(instance!!, null).showReadMemory(content, datarateCallback)
      }

      override fun doInBackground(vararg nfcIntent: Intent): ByteArray {
         val regTimeOutStart = System.currentTimeMillis()
         // Read content and print it on the screen
         val response = demo!!.readTagContent()
         // NDEF Reading time statistics
         timeToReadMemory = System.currentTimeMillis() - regTimeOutStart
         // Get the tag
         return response
      }

      override fun onPreExecute() {
         // Show the progress dialog on the screen to inform about the action
         Dart(instance!!, null).showProgressDialog("Reading", "Reading memory content ...")
         /*dialog = ProgressDialog.show(this@ReadMemoryActivity, "Reading",
                 "Reading memory content ...", true, true)*/
      }
   }

   protected fun showAboutDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      SharedCodes.log("RMemAct", "showAboutDialog");
      ActivityFlutterMediator.startActivity(intent)

   }

   companion object {
      private var datarateCallback: String = "";
      private var content: MutableList<String> = mutableListOf();
      private var instance: ReadMemoryActivity? = null;

      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = ReadMemoryActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         // val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = ReadMemoryActivity::class.java.name
         val pseudoComp      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as ReadMemoryActivity,pseudoComp, intent_filters)
         // initialize PseudoActivity
         instance?.uiSender = Message(registrar, channel)
         SharedCodes.log("RMemAct", "registerPlugin")
      }
   }
}

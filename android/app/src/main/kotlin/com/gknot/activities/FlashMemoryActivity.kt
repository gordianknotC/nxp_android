package com.gknot.activities

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.MainActivity
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.activities.AuthActivity.AuthStatus
import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.Message
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import com.nxp.ntagi2cdemo.R

import io.flutter.plugin.common.MethodCall

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "FlashMemoryActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

@SuppressLint("NewApi")


class FlashMemoryActivity (registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {

   private var pendingIntent: PendingIntent? = null
   private var mAdapter: NfcAdapter? = null

   private var filePath: String? = null
   private var dataRateCallback: String? = null
   private val demo: Ntag_I2C_Demo?
      get() = FlutterNtag_I2C_Demo.instance?.host;
   private var isAppFW = true
   private var indexFW = 0
   private var uiSender:Message? = null

   private class Dart (){
      //todo consts
      companion object {
         fun showAuthenticating(msg:String, prod:String){

         }
         fun closeAuthenticating(){

         }
         fun showDisableAuthentication(){

         }
         fun showWriteToFlash(){
            ch?.invokeMethod("showWriteToFlash", null)
         }
         fun flashDialogue(i: Int){
            ch?.invokeMethod("flashDialogue", i)
         }
         fun flashDialogueProgress(i:Int){
            ch?.invokeMethod("flashDialogueProgress", i)
         }
         fun showToastText(sender: Message, msg:String, time: Int){
            sender.onToastMakeText(msg, time, AuthActivity)
         }
         fun showYNDialogue(sender: Message, title:String, msg:String, action: (Boolean) -> Unit){
            sender.onShowYNDialogueForAction(msg, title, action, AuthActivity)
         }
         fun closeYNDialogue(sender: Message){
            sender.closeYNDialogue(AuthActivity)
         }
      }
   }
   private class Platform(){
      //argument: bytesToFlash
      companion object {
         fun writeToFlash(call: MethodCall, result: MethodChannel.Result){
            val bytesToFlash: ByteArray = call.argument<ByteArray>("bytesToFlash") as ByteArray;
            // Flash the new firmware
            val RegTimeOutStart = System.currentTimeMillis()
            val demo = ActivityFlutterMediator.inject(
                    MainActivity::class.java.name,
                    Ntag_I2C_Demo::class.java.name) as Ntag_I2C_Demo;
            val success = demo.Flash(bytesToFlash)
            val timeToFlashFirmware = System.currentTimeMillis() - RegTimeOutStart
            result.success(mapOf(
                    "success" to success,
                    "timeToFlashFirmware" to timeToFlashFirmware
            ))
         }
      }
   }

   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);
      val resources = activity.resources;
      // Add Foreground dispatcher
      mAdapter = NfcAdapter.getDefaultAdapter(activity)
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)

      //todo: redirect following codes
      /*(findViewById<View>(R.id.selectFlashStorage) as Button).setOnClickListener { startFileChooser() }
      (findViewById<View>(R.id.selectFlashApp) as Button).setOnClickListener {
         val firmwares = arrayOf<CharSequence>("Demo App", "LED Blinker")
         val builder = AlertDialog.Builder(this@FlashMemoryActivity)
         builder.setTitle(resources.getString(R.string.flash_app_select))
         builder.setItems(firmwares) { dialog, which ->
            isAppFW = true
            indexFW = which
            when (indexFW) {
               0 -> filePath!!.text = resources.getString(R.string.file_default_demo)
               1 -> filePath!!.text = resources.getString(R.string.file_default_blinker)
               else -> {
               }
            }
         }
         builder.show()
      }*/

      filePath = resources.getString(R.string.file_default_demo)
      //dataRateCallback = findViewById<View>(R.id.flashfwdata_datarateCallback) as TextView
      return  // end onCreate
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
         mAdapter!!.enableForegroundDispatch(registrar.activity(), pendingIntent, null, null)
      }
   }
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      val activity = registrar.activity()
      if (requestCode == PseudoMainActivity.AUTH_REQUEST && resultCode == Activity.RESULT_OK) {
         if (demo != null && demo!!.isReady) {
            // Read the bin to be flashed
            try {
               if (isAppFW) {
                  bytesToFlash = readFileAssets(indexFW)
               } else {
                  val path = activity.resources.getString(R.string.file_default_demo)
                  bytesToFlash = readFileMemory(path)
               }
            } catch (e: IOException) {
               e.printStackTrace()
               SharedCodes.log("flagh", e.toString())
               // Set bytesToFlash to null so that the task is not started
               bytesToFlash = null
            }

            if (bytesToFlash == null || bytesToFlash!!.size == 0) {
               /*Toast.makeText(mContext, "Error could not open File",
               Toast.LENGTH_SHORT).show()*/
               uiSender?.onToastMakeText("Error could not open File", Toast.LENGTH_SHORT, this);
               return false
            }
            // Launch the thread
            // task = flashTask()
            // task!!.execute()
         }
         return true
      }
      return false
   }

   /*private fun startFileChooser() {
      val chooser = FileChooser(this)
      chooser.setExtension("bin")
      chooser.setFileListener { file ->
         val path = file.absolutePath
         filePath!!.text = path

         // We do not use the default binary anymore
         isAppFW = false
      }.showDialog()
   }*/

   override fun onNewIntent(intent: Intent):Boolean {
      val nfcIntent = intent;
      // Set the initial auth parameters
      PseudoMainActivity.authStatus = AuthStatus.Disabled.value
      PseudoMainActivity.password = null

      // Store the intent information
      PseudoMainActivity.nfcIntent = nfcIntent
      val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      startDemo(tag, true)
      return true
   }

   // Retrieve the binary from assets folder
   @Throws(IOException::class)
   private fun readFileAssets(indexFW: Int): ByteArray {
      var data: ByteArray? = null
      val assManager = registrar.activity().applicationContext.assets
      var `is`: InputStream? = null
      try {
         when (indexFW) {
            0 -> `is` = assManager.open("demo.bin")
            1 -> `is` = assManager.open("blink.bin")
            else -> {
            }
         }
         val byteCount = `is`!!.available()
         data = ByteArray(byteCount)
         `is`.read(data, 0, byteCount)
      } finally {
         `is`?.close()
      }
      return data as ByteArray
   }

   // Retrieve the memory from the Internal Storage
   @Throws(IOException::class)
   private fun readFileMemory(path: String): ByteArray? {
      val file = File(path)
      if (!file.exists()) {
         return null
      }
      // Open file
      val f = RandomAccessFile(file, "r")
      try {
         // Get and check length
         val longlength = f.length()
         val length = longlength.toInt()
         if (length.toLong() != longlength) {
            throw IOException("File size >= 2 GB")
         }
         // Read file and return data
         val data = ByteArray(length)
         f.readFully(data)
         return data
      } finally {
         f.close()
      }
   }

   private fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      val activity = registrar.activity()
      try {
         if (isAppFW) {
            bytesToFlash = readFileAssets(indexFW)
         } else {
            //val path = (findViewById<View>(R.id.file_path) as TextView).text.toString()
            val path = activity.resources.getString(R.string.file_default_demo)
            bytesToFlash = readFileMemory(path)
         }
      } catch (e: IOException) {
         e.printStackTrace()
         SharedCodes.log("flma",e.toString())

         // Set bytesToFlash to null so that the task is not started
         bytesToFlash = null
      }

      if (bytesToFlash == null || bytesToFlash!!.size == 0) {
         uiSender?.onToastMakeText("Error could not open File",  Toast.LENGTH_SHORT, this)
         /*Toast.makeText(mContext, "Error could not open File",
                 Toast.LENGTH_SHORT).show()*/
         return
      }

      //demo = Ntag_I2C_Demo(tag, activity, PseudoMainActivity.password, PseudoMainActivity.authStatus)
      Ntag_I2C_Demo(tag, activity, PseudoMainActivity.password, PseudoMainActivity.authStatus)
      if (demo != null && !demo!!.isReady)
         return

      // Retrieve the Auth Status
      if (getAuthStatus)
         PseudoMainActivity.authStatus = demo!!.ObtainAuthStatus()

      // Flashing is only available when the tag is not protected
      if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value
              || PseudoMainActivity.authStatus == AuthStatus.Unprotected.value
              || PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
         // Launch the thread
         //task = flashTask()
         //task!!.execute()
         Dart.showWriteToFlash()
      } else {
         showAuthDialog()
      }
   }

   fun showAuthDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), AuthActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      SharedCodes.log("flashMem", "showAuthDialog");
      ActivityFlutterMediator.startActivityForResult(intent, PseudoMainActivity.AUTH_REQUEST, this::class.java)
   }

   /*private inner class flashTask : AsyncTask<Intent, Int, Boolean>() {
      private var timeToFlashFirmware: Long = 0

      override fun onPostExecute(success: Boolean?) {
         // Inform the user about the task completion
         flashCompleted(success!!, bytesToFlash!!.size, timeToFlashFirmware)
         // Action completed
         dialog.dismiss()
      }

      override fun doInBackground(vararg nfc_intent: Intent): Boolean? {
         val RegTimeOutStart = System.currentTimeMillis()

         // Flash the new firmware
         val success = demo!!.Flash(bytesToFlash)!!

         // Flash firmware time statistics
         timeToFlashFirmware = System.currentTimeMillis() - RegTimeOutStart

         return success
      }

      override fun onPreExecute() {
         // Show the progress dialog on the screen to inform about the action
         dialog = ProgressDialog(this@FlashMemoryActivity)
         dialog.setTitle("Flashing")
         dialog.setMessage("Writing sector ...")
         dialog.setCancelable(false)
         dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
         dialog.progress = 0
         dialog.show()
      }
   }*/

   /*fun flashCompleted(success: Boolean, bytes: Int, time: Long) {
      if (success) {
         Toast.makeText(mContext, "Flash Completed", Toast.LENGTH_SHORT)
                 .show()
         var readTimeMessage = ""

         // Transmission Results
         readTimeMessage = readTimeMessage + "Flash Firmware\n"
         readTimeMessage = readTimeMessage + ("Speed (" + bytes + " Byte / "
                 + time + " ms): "
                 + String.format("%.0f", bytes / (time / 1000.0))
                 + " Bytes/s")

         // Make the board input layout visible
         (findViewById<View>(R.id.layoutFlashStatistics) as LinearLayout).visibility = View.VISIBLE
         dataRateCallback!!.text = readTimeMessage
      } else
         Toast.makeText(mContext, "Error during memory flash",
                 Toast.LENGTH_SHORT).show()
   }*/

   fun showAboutDialog() {
      var intent: Intent? = null
      intent = Intent(registrar.activity(), VersionInfoActivity::class.java)
      SharedCodes.log("flashMem", "showAbout");
      ActivityFlutterMediator.startActivity(intent)
   }

   companion object {
      val REQUEST_FILE_CHOOSER = 1
      var bytesToFlash: ByteArray? = null

      @JvmStatic fun setFLashDialogMax(max: Int) {
         //task!!.dialog.max = max
         Dart.flashDialogue(max)
      }

      @JvmStatic fun updateFLashDialog() {
         //task!!.dialog.incrementProgressBy(1)
         Dart.flashDialogueProgress(1)
      }
      private var instance: FlashMemoryActivity? = null;
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = FlashMemoryActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = FlashMemoryActivity::class.java.name
         val pseudoComp      = CompNamePseudo.unflattenFromString(entity_name) ?:
               throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance as FlashMemoryActivity,pseudoComp, intent_filters)
         // initialize PseudoActivity
         instance?.uiSender = Message(registrar, channel)
         SharedCodes.log("FlsMemAct", "registerPlugin")
      }
   }
}

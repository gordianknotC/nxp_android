package com.gknot.activities

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Environment
import android.util.Log
import com.gknot.MainActivity
import com.gknot.datatypes.ACTIONS
import com.gknot.datatypes.FRAGMENTS
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files.exists
import android.os.Environment.getExternalStorageDirectory
import com.gknot.datatypes.READERS
import com.gknot.reader.FlutterNtag_I2C_Demo
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugins.pathprovider.PathProviderPlugin
import io.flutter.util.PathUtils



class NtagRawRecord(var tlvPlusNdef: Int, var valid: Boolean, var tlvSize: Int, ndefSize: Int, var data: ByteArray, var message: NdefMessage?)  {
   companion object {
      var seekWrittenAt:Byte = 0x10;
      var dataLength: Int = 96;
   }
}


/*
      shared instances and methods for flutter, since flutter only have one
      activity which is main activity.

*/
val _shiftBits = 3;
val _shiftString = "000"; //ByteArray(_shiftBits).map {it -> 0.toChar()}.toString();
class SharedCodes{
   var mAdapter: NfcAdapter? = null
   var mTag: Tag? = null
   var demo: Ntag_I2C_Demo? = null
   var mPendingIntent: PendingIntent? = null

   companion object {
      var pendingLogs: String = ""
      var shiftString: String = _shiftString;
      var shiftBits  : Int    = _shiftBits;
      var customWrite: Boolean = true;
      var miniNtagOnly: Boolean = false;
      var logToFile: Boolean = true;
      var instance: SharedCodes = SharedCodes()
      var sdCard: File? = null;
      fun initApplicationDir(act: FlutterActivity){
         sdCard = File(PathUtils.getDataDirectory(act.applicationContext));
      }

      @ExperimentalUnsignedTypes
      fun join(bytes: ByteArray):String{
         return bytes.toUByteArray().joinToString(",");
      }
      /*
      *  transform classname into representative tag name for logging.
      */
      fun libTagName(cls: Class<*>):String{
         val module = "com.gknot.activities."
         val module2 = "com.gknot.fragments."
         val module3 = "com.gknot.reader."
         val name   = cls.name
         if (name == "com.gknot.MainActivity")
            return "Main"
         when(name.replace(module3, "")){
            READERS.FlutterNtag_I2C_Demo.data -> return "FI2C"
         }
         when(name.replace(module, "")){
            ACTIONS.VersionInfoActivity.data -> return "VerInfAct"
            ACTIONS.SplashActivity.data -> return "SlsAct"
            ACTIONS.ResetMemoryActivity.data -> return "RstMemAct"
            ACTIONS.RegisterSessionActivity.data -> return "RegSesAct"
            ACTIONS.RegisterConfigActivity.data -> return "RegCfgAct"
            ACTIONS.ReadMemoryActivity.data -> return "RedMemAct"
            ACTIONS.PseudoMainActivity.data -> return "PdoManAct"
            ACTIONS.FlashMemoryActivity.data -> return "FlsMemAct"
            ACTIONS.AuthActivity.data -> return "AutAct"
         }
         when (name.replace(module2, "")){
            FRAGMENTS.ConfigFragment.data -> return "CfgFrag"
            FRAGMENTS.LedFragment.data -> return "LedFrag"
            FRAGMENTS.NdefFragment.data -> return "NdeFrag"
            FRAGMENTS.SpeedTestFragment.data -> return "SpdFrag"
            else ->
               throw NotImplementedError("Tag name: $name not implemnted")
         }
      }


      fun startDemoIfTagFounded(intent:Intent, demoCaller: (tag:Tag)->Unit){
         instance.mTag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
         if (instance.mTag != null && Ntag_I2C_Demo.isTagPresent(instance.mTag)) {
            demoCaller(instance.mTag!!)
         }
      }
      fun genPendingIntent(activity:Activity, cls: Class<*>):PendingIntent{
         instance.mPendingIntent = PendingIntent.getActivity(activity.applicationContext, 0,
           Intent(
                   activity.applicationContext, cls)
                   .addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT), 0)
         return instance.mPendingIntent!!
      }
      fun getNfcAdapter(activity:Activity):NfcAdapter?{
         if (instance.mAdapter != null)
            return instance.mAdapter;

         instance.mAdapter  =  NfcAdapter.getDefaultAdapter(activity);

         SharedCodes.log("Share", "getDefaultAdapter: ${instance.mAdapter}");
         return instance.mAdapter;
      }

      fun onPause(registrar: Registrar) {
         if (instance.mAdapter != null) {
            instance.mAdapter?.disableForegroundDispatch(registrar.activity())
         }
      }

      fun onResume(registrar: Registrar, pendingIntent:PendingIntent) {
         if (instance.mAdapter != null) {
            instance.mAdapter?.enableForegroundDispatch(
                    registrar.activity(),
                    pendingIntent, null, null)
         }
      }
      fun onNewIntent(intent: Intent, request: BasePseudoFlutterActivity, startDemo: (Tag, Boolean) -> Unit): Boolean{
         val nfcIntent = intent;
         // Set the initial auth parameters
         PseudoMainActivity.authStatus = AuthActivity.AuthStatus.Disabled.value
         // set password to current auth
         PseudoMainActivity.password = null
         // set intent information to replace the old one
         PseudoMainActivity.nfcIntent = nfcIntent
         val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
         startDemo(tag, true)
         return true
      }

      fun showAuthDialog(){

      }

      fun error(tag:String, msg:String){
         Log.e(tag, msg);
//         appendLog("$tag: $msg");
      }
      //fixme:
      fun log(tag:String, msg:String){
         Log.d(tag, msg);
//         appendLog("$tag: $msg");
         if (msg.isEmpty())
            return;
         if (FlutterNtag_I2C_Demo.instance == null){
            pendingLogs += "[$tag] $msg\n";
         }else{
            if (pendingLogs.isNotEmpty()){
               FlutterNtag_I2C_Demo.instance?.logToDart(pendingLogs, false);
               pendingLogs = "";
            }
            FlutterNtag_I2C_Demo.instance?.logToDart("[$tag] $msg", false);
         }
      }
      fun getLog(): String{
         val logFile = File(sdCard?.absolutePath, "log.file")
         if(logFile.canRead()){
            return logFile.readText();
         }
         return "";
      }
      private fun  appendLog(text: String) {
         if (!logToFile || sdCard == null)
            return;

         val logFile = File(sdCard?.absolutePath, "log.file")
         if (!logFile.exists()) {
            try {
               Log.d("comon", sdCard?.absolutePath);
               logFile.createNewFile()
            } catch (e: IOException) {
               // TODO Auto-generated catch block
               e.printStackTrace()
               SharedCodes.log("common", e.toString())
            }
         }
         try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append(text)
            buf.newLine()
            buf.close()
         } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
            SharedCodes.log("common", e.toString())
         }

      }
   }
}
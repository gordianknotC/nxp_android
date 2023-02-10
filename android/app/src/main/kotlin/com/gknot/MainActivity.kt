package com.gknot

import   android.content.Intent
import android.os.Bundle
import android.util.Log
import com.gknot.activities.*
import com.gknot.fragments.SpeedTestFragment
import com.gknot.fragments.NdefFragment
import com.gknot.fragments.ConfigFragment
import com.gknot.fragments.LedFragment

import com.gknot.reader.FlutterNtag_I2C_Demo
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugins.GeneratedPluginRegistrant


class MainActivity : FlutterActivity() {
   var mediators: ActivityFlutterMediator? = null;
   companion object {
      var instance: MainActivity? = null
      var mediator: ActivityFlutterMediator? = null
      var crashed: String = "";
   }

   private fun raiseException(e: Exception){
      Log.e("javaException", e.toString());
      Log.e("instance == null", "${FlutterNtag_I2C_Demo.instance == null}")
      FlutterNtag_I2C_Demo.instance?.onJavaException(e);
   }

   private fun triggerException(){
      Log.e("main", "launchNdefDemo failed");
      throw Exception("launchNdefDemo failed!");
   }

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState);
      try {
         Log.e("main", "GeneratedPluginRegistrant");
         GeneratedPluginRegistrant.registerWith(this)
         Log.e("main", "GeneratedPluginRegistrant crashed");
      } catch(e : Exception) {
         crashed = e.stackTrace.joinToString("\n");
      }
      try{
         registerPlugins(this)
         instance = this
         mediator = ActivityFlutterMediator.instance
      }catch(e: Exception ){
         raiseException(e)
      }
   }

   private fun registerPlugins(registry: PluginRegistry): Unit {
      SharedCodes.log("Main", "registerPlugins")
      SharedCodes.initApplicationDir(this)
      FlutterNtag_I2C_Demo.registerWith(registry.registrarFor(FlutterNtag_I2C_Demo.CHANNEL))
      //FlutterNtag_Get_Version.registerWith(registry.registrarFor(FlutterNtag_Get_Version.CHANNEL))

      ActivityFlutterMediator.registerWith(registry.registrarFor(ActivityFlutterMediator.CHANNEL))
      RegisterSessionActivity.registerWith(registry.registrarFor(RegisterSessionActivity.CHANNEL))
      AuthActivity.registerWith(registry.registrarFor(AuthActivity.CHANNEL))
      FlashMemoryActivity.registerWith(registry.registrarFor(FlashMemoryActivity.CHANNEL))
      ReadMemoryActivity.registerWith(registry.registrarFor(ReadMemoryActivity.CHANNEL))
      RegisterConfigActivity.registerWith(registry.registrarFor(RegisterConfigActivity.CHANNEL))
      ResetMemoryActivity.registerWith(registry.registrarFor(ResetMemoryActivity.CHANNEL))
      VersionInfoActivity.registerWith(registry.registrarFor(VersionInfoActivity.CHANNEL))

      SpeedTestFragment.registerWith(registry.registrarFor(SpeedTestFragment.CHANNEL))
      NdefFragment.registerWith(registry.registrarFor(NdefFragment.CHANNEL))
      ConfigFragment.registerWith(registry.registrarFor(ConfigFragment.CHANNEL))
      LedFragment.registerWith(registry.registrarFor(LedFragment.CHANNEL))
      PseudoMainActivity.registerWith(registry.registrarFor(PseudoMainActivity.CHANNEL))

      ActivityFlutterMediator.showMediators()
   }

   /*
   *     todo:
   *     delegate following on events into FlutterActivityMediator
   * */
   override fun onStart() {
      try{
         super.onStart()

         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onStart")
         ActivityFlutterMediator.onStart()

         SharedCodes.log(SharedCodes.libTagName(this::class.java), "startAcitivity for PseudoMain")
         val intent = Intent(this, PseudoMainActivity::class.java)
         ActivityFlutterMediator.onReady()
         ActivityFlutterMediator.startActivity(intent)
      }catch(e: Exception ){
         raiseException(e)
      }
   }

   override fun onPause() {
      try{
         super.onPause()
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onPause")
         ActivityFlutterMediator.onPause()
      }catch(e: Exception ){
         raiseException(e)
      }
   }

   override fun onResume() {
      try{
         super.onResume()

         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onResume")
         ActivityFlutterMediator.onResume()
      }catch(e: Exception ){
         raiseException(e)
      }
   }

   override fun onDestroy() {
      try{
         super.onDestroy()
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onDestroy")
         ActivityFlutterMediator.onDestroy()
      }catch(e: Exception ){
         FlutterNtag_I2C_Demo.instance?.onJavaException(e)
      }
   }

   override fun onBackPressed() {
      try{
         super.onBackPressed()
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onBackPressed")
         ActivityFlutterMediator.onBackPressed()
      }catch(e: Exception ){
         raiseException(e);
      }
   }

   override fun onNewIntent(intent: Intent?) {
      try{
         SharedCodes.log(SharedCodes.libTagName(this::class.java), "onNewIntent: ${intent?.component?.className}")
         super.onNewIntent(intent)
      }catch(e: android.os.DeadObjectException){
         FlutterNtag_I2C_Demo.instance!!.onNFCServiceDead();
      }catch(e: Exception){
         raiseException(e)
      }
   }
}

package com.gknot.pseudo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.gknot.BaseMethodPlugin
import com.gknot.MainActivity
import com.gknot.activities.SharedCodes
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


open class BasePseudoFlutterActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BaseMethodPlugin(registrar, channel), FlutterActivityAware {


   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onActivityResult")
      return true
   }
   override fun onNewIntent(intent: Intent): Boolean {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onNewIntent")
      return true
   }
   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: MainActivity) {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onCreate")
   }
   override fun onPause() {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onPause")
   }
   override fun onStop() {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onStop")
   }
   override fun onResume() {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onResume")
   }
   override fun onDestroy(){
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onDestroy")
   }
   override fun onStart() {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onStart")
   }
   override fun onBackPressed() {
      SharedCodes.log(SharedCodes.libTagName(this::class.java), "onBackPressed")
   }

}

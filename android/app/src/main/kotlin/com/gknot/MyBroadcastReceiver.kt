package com.gknot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.gknot.datatypes.ACTIONS

class MyBroadcastReceiver: BroadcastReceiver() {
   override fun onReceive(context: Context?, intent: Intent?) {
      val action = intent?.getAction() ?: return
      //todo:
      when(action){
         ACTIONS.AuthActivity.data -> {

         }
         ACTIONS.DebugActivity.data -> {}
         ACTIONS.FlashMemoryActivity.data -> {}
         ACTIONS.HelpActivity.data -> {}
         ACTIONS.MainActivity.data -> {}
         ACTIONS.ReadMemoryActivity.data -> {}
         ACTIONS.RegisterConfigActivity.data -> {}
         ACTIONS.RegisterSessionActivity.data -> {}
         ACTIONS.ResetMemoryActivity.data -> {}
         ACTIONS.SplashActivity.data -> {}
         ACTIONS.VersionInfoActivity.data -> {}
         else -> {}
      }
   }
}
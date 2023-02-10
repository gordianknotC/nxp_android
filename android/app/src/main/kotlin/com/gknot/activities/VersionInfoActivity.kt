package com.gknot.activities

import java.io.IOException

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.FormatException
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log

import com.gknot.ActivityFlutterMediator
import com.gknot.BASE_NAME
import com.gknot.MainActivity
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.activities.AuthActivity.AuthStatus
import com.gknot.datatypes.CompNamePseudo
import com.gknot.pseudo.Message
import com.gknot.reader.FlutterNtag_I2C_Demo

import com.nxp.nfc_demo.exceptions.CommandNotSupportedException
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo

import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

private const val MODULE_NAME: String = "VersionInfoActivity";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";



class VersionInfoActivity(registrar: PluginRegistry.Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel) {
   private var VersionInfoExpanded = false
   private var pendingIntent: PendingIntent? = null
   private var mAdapter: NfcAdapter? = null
   private val demo: Ntag_I2C_Demo?
      get() = FlutterNtag_I2C_Demo.instance?.host;
   private var uiSender: Message? = null
   override fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: com.gknot.MainActivity) {
      super.onCreate(savedInstanceState, intent, activity);
      uiSender = Message(registrar, channel)
      /*
      *
      *     todo: transform followings into dependency injection
      *
      */
      // Capture intent to check whether the operation should be automatically launch or not
      /*val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      if (tag != null && Ntag_I2C_Demo.isTagPresent(tag)) {
         startDemo(tag, false)
      }*/
      SharedCodes.startDemoIfTagFounded(intent){tag -> startDemo(tag, false)}
      // Add Foreground dispatcher
      //mAdapter = NfcAdapter.getDefaultAdapter(activity)
      mAdapter = SharedCodes.getNfcAdapter(activity)
      /*pendingIntent = PendingIntent.getActivity(
              activity, 0,
              Intent(activity, this::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
              0)*/
      pendingIntent = SharedCodes.genPendingIntent(MainActivity.instance!!, MainActivity::class.java)
      return  // end onCreate
   }
   /*
   *  💼 NOTE:
   *     Should I delegate all kinds of this (onPause, onResume, onBackPressed)
   *     into flutter? Since it's all the same everywhere in activities of nxp
   *     official application
   * */
   public override fun onPause() {
      super.onPause()
      /*if (mAdapter != null) {
         mAdapter!!.disableForegroundDispatch(registrar.activity())
      }*/
      SharedCodes.onPause(registrar)
   }
   public override fun onResume() {
      super.onResume()
      /*if (mAdapter != null) {
         mAdapter!!.enableForegroundDispatch(
                 registrar.activity(),
                 pendingIntent, null, null)
      }*/
      SharedCodes.onResume(registrar, pendingIntent!!);
   }
   override fun onBackPressed() {
      val output = Intent()
      registrar.activity()?.setResult(Activity.RESULT_OK, output)
      ActivityFlutterMediator.finish(this)
   }
   /*
      setBoardVersion if demo is ready
   */
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent):Boolean {
      // Check which request we're responding to
      // Make sure the request was successful
      if (requestCode == PseudoMainActivity.AUTH_REQUEST
              && resultCode == Activity.RESULT_OK
              && demo != null
              && demo!!.isReady) {
         // Launch the thread
         try {
            demo!!.setBoardVersion()
            return true
         } catch (e: IOException) {
            e.printStackTrace()
            SharedCodes.log("flma", e.toString())
            return false
         } catch (e: FormatException) {
            e.printStackTrace()
            SharedCodes.log("flma", e.toString())
            return false
         } catch (e: CommandNotSupportedException) {
            uiSender?.onShowOkDialogue(
              "VersionInfo not supported",
              "Tag not supported",
              this)
            return false
         }
      }
      return false
   }
   override fun onNewIntent(intent: Intent):Boolean {
      return SharedCodes.onNewIntent(intent, this, ::startDemo)
      /*val nfcIntent = intent;
      // Set the initial auth parameters
      PseudoMainActivity.authStatus = AuthStatus.Disabled.value
      // set password to current auth
      PseudoMainActivity.password = null
      // set intent information to replace the old one
      PseudoMainActivity.nfcIntent = nfcIntent
      val tag = nfcIntent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
      startDemo(tag, true)
      return true*/
   }
   private fun startDemo(tag: Tag, getAuthStatus: Boolean) {
      // Complete the task in a new thread in order to be able to show the dialog
      //demo =
      Ntag_I2C_Demo(
         tag, registrar.activity(),
         PseudoMainActivity.password, PseudoMainActivity.authStatus
      )
      if (!demo!!.isReady)
         return
      // Retrieve the Auth Status
      if (getAuthStatus == true) {
         PseudoMainActivity.authStatus = demo!!.ObtainAuthStatus()
      }
      if (PseudoMainActivity.authStatus == AuthStatus.Disabled.value
              || PseudoMainActivity.authStatus == AuthStatus.Unprotected.value
              || PseudoMainActivity.authStatus == AuthStatus.Authenticated.value) {
         // Launch the thread
         try {
            demo!!.setBoardVersion()
         } catch (e: IOException) {
            e.printStackTrace()
            SharedCodes.log("vera",e.toString())
         } catch (e: FormatException) {
            e.printStackTrace()
            SharedCodes.log("vera",e.toString())
         } catch (e: CommandNotSupportedException) {
            uiSender?.onShowOkDialogue(
              "VersionInfo not supported",
              "Tag not supported", this)
            return
         }
      } else {
         showAuthDialog()
      }
   }

   fun showAuthDialog() {
      var intent: Intent? = null
      intent = Intent(
           registrar.activity(),
           AuthActivity::class.java)
      if (PseudoMainActivity.nfcIntent != null)
         intent.putExtras(PseudoMainActivity.nfcIntent as Intent)
      SharedCodes.log("verAct", "showAuthDialog");
      ActivityFlutterMediator.startActivityForResult(
        intent, PseudoMainActivity.AUTH_REQUEST, this::class.java
      )
   }

   companion object {
      private var Board_Version_text: String? = null
      private var boardFwVersionText: String? = null

      @JvmStatic fun setBoardVersion(version: String) {
         Board_Version_text = version
      }

      @JvmStatic fun setBoardFWVersion(version: String) {
         boardFwVersionText = version
      }

      @JvmStatic val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic var ch:MethodChannel? = null;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         // register channelMethod to Dart
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         val instance = VersionInfoActivity(registrar, channel);
         channel.setMethodCallHandler(instance)
         ch = channel;
         registrar.addNewIntentListener(instance);
         // register mediator to ActivityMediator
         //val pkg          = RegisterSessionActivity::class.java.`package`
         val entity_name    = VersionInfoActivity::class.java.name
         val pseudoComp      = CompNamePseudo.unflattenFromString(entity_name) ?:
         throw ClassNotFoundException("cannot generate CompNamePseudo from entity:$entity_name")
         val intent_filters = null
         ActivityFlutterMediator.registerMediator(instance,pseudoComp, intent_filters)
         // initialize PseudoActivity
         instance.uiSender = Message(registrar, channel)
         SharedCodes.log("VerInfAct", "registerPlugin")
      }

   }
}

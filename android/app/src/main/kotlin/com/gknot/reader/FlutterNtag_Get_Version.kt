package com.gknot.reader
import android.app.Activity
import android.nfc.FormatException
import android.nfc.Tag
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo as Ntag_I2C_Demo_Host
import com.gknot.BaseMethodPlugin
import com.gknot.BASE_NAME
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import android.nfc.NfcAdapter
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.gknot.pseudo.Message
import com.nxp.nfc_demo.reader.Ntag_Commands
import java.io.IOException


private const val MODULE_NAME: String = "FlutterNtag_Get_Version";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";


/*
*
*     Call Ntag_I2C_Demo from Dart not the other way round
*
* */
class FlutterNtag_Get_Version(registrar: Registrar, channel: MethodChannel)
   : BasePseudoFlutterActivity(registrar, channel)
{

   companion object {
      @JvmStatic
      val CHANNEL:String = CHANNEL_NAME;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         val instance = FlutterNtag_Get_Version(registrar, channel);
         channel.setMethodCallHandler(instance)
      }
   }
   override fun onMethodCall(call: MethodCall, result: MethodChannel.Result): Unit {
      print("onMethodCall: ${call.method}");
      when (call.method) {

      }
   }
}



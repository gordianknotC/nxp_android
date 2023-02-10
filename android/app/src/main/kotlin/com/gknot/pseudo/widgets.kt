package com.gknot.pseudo

import android.app.SearchManager
import android.util.Log
import com.gknot.activities.SharedCodes
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry


class OnResult(
        val _error: (p0:String?, p1:String?, p2:Any?) -> Unit,
        val _sucess: (result: Any?)->Unit ) : MethodChannel.Result{
   override fun error(p0: String?, p1: String?, p2: Any?) {
      _error(p0, p1, p2)
   }

   override fun success(result: Any?) {
      _sucess(result)
   }

   override fun notImplemented() {
      throw NotImplementedError("OnResult")
   }

}

class Message (
  private val registrar: PluginRegistry.Registrar,
  private val channel: MethodChannel
): ShowAlertListener, SetBoardVersionListener, ShowToastListener
{
   private fun onTittleMsg(message:String, title:String, methodname:String, request: Any){
         channel.invokeMethod(methodname, mapOf(
         "message" to (message),
         "title" to (title),
         "\$request" to (request::class.java.name)
      ))
   }
   private fun onTittleMsg(message:String, title:String, methodname:String, request: Any, result: MethodChannel.Result){
      channel.invokeMethod(methodname, mapOf(
              "message" to (message),
              "title" to (title),
              "\$request" to (request::class.java.name)
      ), result)
   }
   override fun onToastMakeText(message:String, time: Int, request: Any) {
      channel.invokeMethod("onToastMakeText", mapOf(
        "message" to (message),
        "time" to (time),
        "\$request" to (request::class.java.name)
      ))
   }
   override fun onShowOkDialogue(message:String, title:String, request: Any){
      onTittleMsg(message, title, "onShowOkDialogue", request)
   }
   override fun onShowOkDialogueForAction(message:String, title:String, request: Any, action: (b:Boolean) -> Unit){
      fun error(p0:String?, p1:String?, p2:Any?):Unit{
         Log.e("onShowOkDialog", "error: $p0, $p1, $p2")
      }
      fun success(result:Any?): Unit{
         if (result is Boolean){
            return action(result)
         }
         throw Exception("Invalid result")
      }
      SharedCodes.log("Msg", "onShowOkDialogue");
      onTittleMsg(message, title, "onShowOkDialogue", request, OnResult(::error, ::success));
   }

   fun onShowYNDialogueForAction(message:String, title:String, action: (b:Boolean) -> Unit, request: Any){
      fun error(p0:String?, p1:String?, p2:Any?):Unit{
         Log.e("Dart.Err", "while calling onShowYNDialog, error: $p0, $p1, $p2")
      }
      fun success(result:Any?): Unit{
         if (result is Boolean){
            return action(result)
         }
         throw Exception("Invalid result")
      }
      SharedCodes.log("Msg", "onShowYNDialogue");
      val result = onTittleMsg(message, title, "onShowYNDialogue", request, OnResult(::error, ::success));
   }
   fun closeYNDialogue(request: Any){
      return channel.invokeMethod("onCloseYNDialogue", mapOf(
           "\$request" to (request::class.java.name)
      ))
   }
   override fun onShowAlert(message:String, title:String, request: Any){
      onTittleMsg(message, title, "onShowAlert", request)
   }
   override fun onSetBoardVersion(ver: String, fwver: String, request: Any) {
      channel.invokeMethod("onSetBoardVersion", mapOf(
        "ver" to (ver),
        "fwver" to (fwver),
        "\$request" to (request::class.java.name)
      ))
   }
   fun onSetDataRate(message:String, bytes:Int, time:Long,  request: Any){
      channel.invokeMethod("onSetDataRate", mapOf(
           "message" to (message),
           "bytes" to (bytes),
           "time" to (time),
           "\$request" to (request::class.java.name)
      ))
   }

   fun onNFCServiceDead(){
      channel.invokeMethod("onNFCServiceDead", mapOf("" to ("")))
   }


   fun onShowProgressDialogue(title:String, msg:String, cancelable:SearchManager.OnCancelListener, ident:String){

   }
}


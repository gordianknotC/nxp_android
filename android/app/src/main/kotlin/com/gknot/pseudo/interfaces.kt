package com.gknot.pseudo

import android.content.Intent
import android.os.Bundle
import io.flutter.plugin.common.PluginRegistry
import android.os.Parcelable
import com.gknot.ActivityMediator
import com.gknot.MainActivity
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo

interface SetBoardVersionListener {
    fun onSetBoardVersion(ver: String, fwver: String, request: Any)
}
interface OnWriteSRAMListener{
    fun onWriteSRAM()
}
interface OnWriteEEPROMListener{
    fun onWriteEEPROM(bytes:Int)
}
interface OnNDEFWriteListener{
    fun onNDEFWrite(bytes: Int)
}
interface OnNDEFReadListener{
    fun onNDEFRead(bytes: Int)
}
interface InstanceDelegateListener {
    fun onDelegateInstance(demo: Ntag_I2C_Demo)
}
interface ShowAlertListener {
    fun onShowAlert(message: String, title: String, request: Any)
    fun onShowOkDialogue(message:String, title:String, request: Any)
    fun onShowOkDialogueForAction(message:String, title:String, request: Any, action: (b:Boolean) -> Unit)
}

interface ShowToastListener {
    fun onToastMakeText(message: String, time: Int, request: Any)
}

interface UIDelegateListener: SetBoardVersionListener{

}





interface ParcelableMap: Parcelable {
    fun toMap(): Map<String, Any>
}

// todo: interface here should consider mediator definition
interface IntentAware: PluginRegistry.NewIntentListener, PluginRegistry.ActivityResultListener {
    fun processIntentOnReceive(intent: Intent, requestCode: Int? = null): Boolean
}

interface ActivityAware: IntentAware {
    fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: MainActivity)
    fun onPause()
    fun onResume()
}

interface FlutterActivityAware: IntentAware {
    fun onCreate(savedInstanceState: Bundle?, intent: Intent, activity: MainActivity)
    fun onPause()
    fun onStop()
    fun onStart()
    fun onResume()
    fun onDestroy()
    fun onBackPressed()
}

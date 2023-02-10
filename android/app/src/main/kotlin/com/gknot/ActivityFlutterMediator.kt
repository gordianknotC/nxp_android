package com.gknot

import android.app.Activity
import android.content.*
import android.util.Log
import com.gknot.activities.*

import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar

import com.gknot.pseudo.IntentAware
import com.gknot.datatypes.Album
import com.gknot.datatypes.CompNamePseudo
import com.gknot.fragments.ConfigFragment
import com.gknot.fragments.LedFragment
import com.gknot.fragments.NdefFragment
import com.gknot.fragments.SpeedTestFragment
import com.gknot.pseudo.BasePseudoFlutterActivity
import com.nxp.nfc_demo.reader.Ntag_I2C_Demo

private const val MODULE_NAME: String = "ActivityFlutterMediator";
private const val CHANNEL_NAME = "$BASE_NAME.$MODULE_NAME";

/*
   to use Class as key | subkey, it must at least contains definition of compaion object
   within the Class, for pseudo:
   [OK] --------------------
      class TempA{
         companion object{}
      }
   [FAILED] ----------------
      class TempA {}
*/
class Dependency (var key: Any, var subkey: Any, var instance: Any){
}

class DependencyInject{
   companion object {
      var data: HashMap<Any, HashMap<Any, Any>> = hashMapOf();
      @JvmStatic fun register(reg: Dependency){
         try{
            if (hasReg(reg)) return;
            if (data.containsKey(reg.key)){
               if (data[reg.key]!!.containsKey(reg.subkey)){
                  throw ExceptionInInitializerError("Unchaught Exception");
               }
               data[reg.key]!![reg.subkey] = reg.instance;
            }
            data[reg.key] = hashMapOf(reg.subkey to reg.instance)
         }catch(e: Exception) {
            throw e
         }
      }

      @JvmStatic fun inject(key: Any, subKey: Any): Any? {
         if (data.containsKey(key)){
            if (data[key]!!.containsKey(subKey))
               return data[key]!![subKey];
         }
         return null;
      }

      @JvmStatic fun hasReg(reg: Dependency):Boolean{
         if (data.containsKey(reg.key)){
            if (data[reg.key]!!.containsKey(reg.subkey)){
               //return data[reg.key]!![reg.subkey] == reg.pseudo;
               return true; // subkey is decisive
            }
            return false
         }
         return false;
      }
   }
}
class NXPMediator(
   var registrar: Registrar
){

}
class ActivityMediator(
        var pseudo_activity: BasePseudoFlutterActivity,
        var mediator: ActivityFlutterMediator,
        var pseudoComp: CompNamePseudo,
        var intent_filters: Array<IntentFilter>?
) {
   // decide pseudo activity is created or not
   // if created, allways call onNewIntent instread of onCreate
   // if not    , call onCreate
   var created: Boolean = false;

   init{
      // setup default intent filter for implicit intent request
      var defaults: Array<IntentFilter>? = null;
      when (pseudo_activity){
         is RegisterSessionActivity -> {}
         is PseudoMainActivity -> {
            defaults = arrayOf(
               // fixme:
               // note: first intentFilter may work in an unexpected way with pseudoActivity???
               IntentFilter().apply{
                  addAction(android.content.Intent.ACTION_MAIN)
                  addCategory(android.content.Intent.CATEGORY_LAUNCHER)},
               IntentFilter().apply{
                  addAction(android.nfc.NfcAdapter.ACTION_NDEF_DISCOVERED)
                  addCategory(android.content.Intent.CATEGORY_DEFAULT)}
            )
         }
         is AuthActivity            -> {}
         is ReadMemoryActivity      -> {}
         is RegisterConfigActivity  -> {}
         is ResetMemoryActivity     -> {}
         is VersionInfoActivity     -> {}
         is FlashMemoryActivity     -> {}
         is SpeedTestFragment       -> {}
         is LedFragment             -> {}
         is NdefFragment            -> {}
         is ConfigFragment          -> {}
         else -> {
            throw ExceptionInInitializerError("Uncaught Exception: \n Unknown ActivityMediator: ${pseudo_activity::class.java.name}");
         }
      }
      if (defaults == null){
         //pass
      }else{
         intent_filters = defaults
      }
   }

   /*
      Matching Sequence
   1) action   - NO_MATCH_ACTION                - if specified but no match
   2) data     - NO_MATCH_TYPE | NO_MATCH_DATA  - if specified but no match
   3) category - NO_MATCH_CATEGORY              - if specified but no match
   */
   fun isIntentMatched(intent:Intent): Boolean {
      if (intent_filters != null){
         SharedCodes.log("isIntentMatchedA", "${intent.component?.className}, ${pseudoComp.name}")
         if (intent.component?.className == pseudoComp.name)
            return true;
         return (intent_filters as Array<IntentFilter>).any {
            val result = it.match(intent.action,intent.type,intent.scheme,intent.data,intent.categories,null)
            if (intent.action != null)
               return result != IntentFilter.NO_MATCH_ACTION;
            if (intent.data != null || intent.type != null || intent.scheme != null)
               return result != IntentFilter.NO_MATCH_DATA
                   && result != IntentFilter.NO_MATCH_TYPE
            if(intent.categories != null)
               return result != IntentFilter.NO_MATCH_CATEGORY
            return false;
         }
      }else{
         SharedCodes.log("isIntentMatchedB", "${intent.component?.className}, ${pseudoComp.name}")
         if (intent.component?.className == pseudoComp.name)
            return true
         return false;
      }
   }
}

enum class LifeCycle(val data: String){
   onCreate("onCreate"),
   onStart("onStart"),
   onStop("onStop"),
   onPause("onPause"),
   onResume("onResume"),
   onDestroy("onDestroy")
}

class ActivityFlutterMediator(var registrar: Registrar, var channel: MethodChannel)
   : BroadcastReceiver(), IntentAware, MethodCallHandler {

   companion object {
      var currentMediator: ActivityMediator? = null;

      private class Dart(
              override val target: BasePseudoFlutterActivity,
              override var cb: ((Any?) -> Unit)?): ChannelDart {
         override var methodname:String? = ""
         /*
            🖊️ startActivity from Mediator
                 - call onCreate from mediator
                    - onCreate from pseudo
                       - call Dart.startActivity
                          - call Dart.startActivity.callback -> onCreate
                 - call onNewIntent from mediator
                    - onNewIntent from pseudo
                       - call Dart.startActivity
                          - call Dart.startActivity.callback -> onNewIntent
               -----------------------------------------
            🖊️ startActivityForResult from Mediator
                  ....
                     call Dart.startActivity with setting listenForResult to true
                        ... after user interaction and change navigation
                           - [in dart] call Platform.activityResult
            */
         fun startActivity(created: Boolean, listenForResult: Boolean, requestCode:Int, intent:Intent, target:BasePseudoFlutterActivity ,receiver: BasePseudoFlutterActivity?){
            methodname = START_ACTIVITY
            val receiver_name:String = if (receiver == null) "" else receiver::class.java.name
            val target_name:String = target::class.java.name
            val args = mapOf (
                    "created" to created,
                    "listenForResult" to listenForResult,
                    "requestCode" to requestCode,
                    "receiver" to receiver_name,
                    "target" to target_name
            )
            SharedCodes.log("Dart.stACT", "args to be send: \n\t created: $created, listen:$listenForResult, request:$requestCode, receiver:$receiver_name, target: $target_name")
            try{
               instance?.channel?.invokeMethod(
                       START_ACTIVITY, args, this
               )
            }catch(e:Exception){
               throw Exception("\nInvoke dart method failed\n$e")
            }

         }
         companion object {
            val START_ACTIVITY = "startActivity";

         }
      }
      private class Platform{
         companion object {
            val ACTIVITY_RESULT = "activityResult"

            private fun getMediatorByCall(call: MethodCall, key:String = "receiver"): ActivityMediator{
               val receiver = call.argument<String>(key) as String
               val comp = CompNamePseudo.unflattenFromString(receiver) ?:
                  throw ExceptionInInitializerError("cannot parse receiver: $receiver to CompNamePseudo")
               val mediator = getMediator(comp);
               return mediator
            }

            fun activityResult(call:MethodCall, result:MethodChannel.Result){
               val mediator = getMediatorByCall(call)
               val data = Intent()
               val requestCode  = call.argument<Int>("requestCode") as Int
               val success      = call.argument<Boolean>("success") as Boolean
               val ok = if (success) Activity.RESULT_OK else 0
               putResultDataToIntent(mediator, call, data);
               SharedCodes.log("Plfm.actRlt", "receiver: ${mediator.pseudo_activity::class.java.name}")
               mediator.pseudo_activity.onActivityResult(requestCode, ok, data);
            }

            fun putResultDataToIntent(mediator:ActivityMediator, call:MethodCall, intent:Intent){
               when(mediator.pseudoComp.name){
                  // nothing here for now...
                  // note: in nxp programming, nothing to be done here
                  /*"AuthActivity" -> {

                  }
                  else ->
                     throw Exception("Uncaught pseudoComp name: ${mediator.pseudoComp.name}")*/
               }
            }
         }
      }
      /*
      *
      *       interface for flutter PluginRegistry
      *
      */
      @JvmStatic var mediators: MutableList<ActivityMediator> = mutableListOf<ActivityMediator>();
      @JvmStatic var instance: ActivityFlutterMediator? = null;
      @JvmStatic var pseudo: PseudoMainActivity? = null;
      @JvmStatic val CHANNEL: String = CHANNEL_NAME;

      @JvmStatic
      fun registerWith(registrar: PluginRegistry.Registrar): Unit {
         val channel = MethodChannel(registrar.messenger(), CHANNEL)
         instance = ActivityFlutterMediator(registrar, channel);
         channel.setMethodCallHandler(instance)
         registrar.addNewIntentListener(instance);
         registrar.addActivityResultListener(instance);
         instance?.broadCastInit();
         SharedCodes.log("Mediator", "registerPlugins")
      }

      /*
      *
      *           M e d i a t o r s
      *
      *
      **/
      @JvmStatic fun getMediatorNames():String{
         var ret:String = ""
         for(mediator in mediators){
            ret += mediator.pseudo_activity::class.java.name + "\n";
         }
         return ret
      }
      @JvmStatic fun showMediators(){
         Log.i("Mediator", "showMediators")
         for(mediator in mediators){
            Log.i("Mediator", "${mediator.pseudo_activity::class.java.name}, ${mediator.pseudoComp.name}")
         }
      }
      @JvmStatic fun registerMediator(pseudo: BasePseudoFlutterActivity, pseudoComp: CompNamePseudo, intent_filters: Array<IntentFilter>?) {
         val instance = instance as ActivityFlutterMediator;
         assert(pseudo::class.java.name == pseudoComp.name, fun():String{
            return "path name missmatched between pseudo and pseudoComp"
         })
         SharedCodes.log("Medator", "add mediator: ${pseudo::class.java.name}")
         mediators.add(ActivityMediator(pseudo,instance,pseudoComp, intent_filters))
      }
      //fixme: followings buggy!!
      @JvmStatic fun getMediator(pseudo: BasePseudoFlutterActivity): ActivityMediator{
         return mediators.lastOrNull { mediator -> mediator.pseudo_activity == pseudo } ?:
                  throw ClassNotFoundException("cannot fetch mediator on class: ${pseudo::class.java.name}" +
                          "\nMediators: ${getMediatorNames()}")
      }
      @JvmStatic fun getMediator(component: ComponentName): ActivityMediator{
         return mediators.lastOrNull { mediator -> mediator.pseudoComp.name == component.className } ?:
            throw ClassNotFoundException("cannot fetch mediator on compName: ${component.className}" +
                    "\nMediators: ${getMediatorNames()}")
      }
      @JvmStatic fun getMediator(pseudocomp: CompNamePseudo): ActivityMediator {
         return mediators.lastOrNull { mediator -> mediator.pseudoComp.name == pseudocomp.name } ?:
            throw ClassNotFoundException("cannot fetch mediator on comp: ${pseudocomp.name}" +
                    "\nMediators: ${getMediatorNames()}")
      }
      @JvmStatic fun getMediator(fake_intent:Intent): ActivityMediator{
         //mediators.lastOrNull {m -> m.pseudo_activity::class.java.name == fake_intent.component?.toString()}  ?:
         return mediators.lastOrNull {m -> m.isIntentMatched(fake_intent)}  ?:
            throw ClassNotFoundException("cannot fetch mediator on Intent: \n\t${fake_intent.component}" +
                    "\n\tfake_intent cls:${fake_intent.component?.className}" +
                    "\nMediators: ${getMediatorNames()}")
      }

      /*
      *     Dependencies - DI (Dependency Injection) related
      *
      * */
      fun register(key: Any, subKey: Any, value: Any){
         DependencyInject.register(Dependency(key, subKey, value))
      }
      fun inject(key: Any, subKey:Any): Any?{
         return DependencyInject.inject(key, subKey)
      }


      /*
      *     called while MainActivity already delegate all it's lifecycles to mediators
      *     like onPause, onResume, on ....
      */
      private var isReady:Boolean = false
      @JvmStatic fun onReady(){
         isReady = true;
         // fixme:
      }
      /*
      *
      *     A c t i v i t y
      *
      *     activity related -
      *        pseudo functionality for delegating to PseudoMainActivity
      *        since flutter only has one activity (PseudoMainActivity)
      *
      */
      @JvmStatic fun startActivity(intent: Intent){
         // ?: if it's null (...)
         val target_mediator = getMediator(intent)
         val target = target_mediator.pseudo_activity
         SharedCodes.log("Mediator", "startActivity, target: ${target::class.java.name}")
         currentMediator = target_mediator
         /*Dart(target, fun(result:Any?){
            if (result is Boolean && result == true){

            }
            target.onCreate(null,intent, MainActivity.instance!!)
         }).startActivity(false,false, -1, intent, target, null)*/
         target.onCreate(null,intent, MainActivity.instance!!)
      }
      @JvmStatic fun startActivityForResult(intent: Intent, requestCode: Int, _receiver: Class<*>){
         val target_comp = intent.component ?:
              throw ExceptionInInitializerError("target pseudoComp not found in Intent")
         val target_mediator = getMediator(target_comp)
         val target = target_mediator.pseudo_activity
         val receiver_comp = CompNamePseudo.unflattenFromString(_receiver.name)
         val receiver = getMediator(receiver_comp).pseudo_activity
         val created = target_mediator.created

         SharedCodes.log("Dart.stAFR", "requestCode: $requestCode, intent: ${intent.component?.className}, target: ${target::class.java.name}, receiver: ${receiver::class.java.name}")

         Dart(target, fun(result:Any?){
            if (result is Boolean && result == true){

            }
            target.onActivityResult(requestCode, Activity.RESULT_OK,intent)
         }).startActivity(created,true, requestCode, intent, target, receiver)
         currentMediator = target_mediator
      }

      /*

      *        L  i  f  e  C  y  c  l  e  s
      *
      *
      *     lifecycle related -
      *        pseudo functionality for delegating to PseudoMainActivity
      *        since flutter only has one activity (PseudoMainActivity)
      *
      */
      @JvmStatic fun onStart(pseudo: BasePseudoFlutterActivity? = null){
         if (currentMediator != null){
            if (pseudo == null){
               currentMediator?.pseudo_activity?.onStart();
            }else{
               if (currentMediator!!.pseudo_activity::class.java.name == pseudo::class.java.name){
                  currentMediator?.pseudo_activity?.onStart();
               }else{
                  pseudo.onStart();
               }
            }
         }
      }
      @JvmStatic fun onPause(pseudo: BasePseudoFlutterActivity? = null){
         if (currentMediator != null){
            if (pseudo == null){
               currentMediator?.pseudo_activity?.onPause();
            }else{
               if (currentMediator!!.pseudo_activity::class.java.name == pseudo::class.java.name){
                  currentMediator?.pseudo_activity?.onPause();
               }else{
                  pseudo.onPause();
               }
            }
         }
      }
      @JvmStatic fun onResume(pseudo: BasePseudoFlutterActivity? = null){
         if (currentMediator != null){
            if (pseudo == null){
               currentMediator?.pseudo_activity?.onResume();
            }else{
               if (currentMediator!!.pseudo_activity::class.java.name == pseudo::class.java.name){
                  currentMediator?.pseudo_activity?.onResume();
               }else{
                  pseudo.onResume();
               }
            }
         }
      }
      @JvmStatic fun onDestroy(pseudo: BasePseudoFlutterActivity? = null){
         if (currentMediator != null){
            if (pseudo == null){
               currentMediator?.pseudo_activity?.onDestroy();
            }else{
               if (currentMediator!!.pseudo_activity::class.java.name == pseudo::class.java.name){
                  currentMediator?.pseudo_activity?.onDestroy();
               }else{
                  pseudo.onDestroy();
               }
            }
         }
      }
      @JvmStatic fun onBackPressed(pseudo: BasePseudoFlutterActivity? = null){
         if (currentMediator != null){
            if (pseudo == null){
               currentMediator?.pseudo_activity?.onBackPressed();
            }else{
               if (currentMediator!!.pseudo_activity::class.java.name == pseudo::class.java.name){
                  currentMediator?.pseudo_activity?.onBackPressed();
               }else{
                  pseudo.onBackPressed();
               }
            }
         }
      }
      @JvmStatic fun onNewIntent(p0: Intent?) {

      }

      @JvmStatic fun onActivityResult(p0: Int, p1: Int, p2: Intent?) {

      }

      /*
       onCreate() counter part is onDestroy()
       onStart() counter part is onStop()
       onPause() counter part is onResume()
       ---------------------------------------------
       for detail
       https://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
      */
      @JvmStatic fun finish(pseudo: BasePseudoFlutterActivity, lifecycle: LifeCycle = LifeCycle.onStart){
         when (lifecycle){
            LifeCycle.onCreate ->{
            }
            LifeCycle.onStart ->{
               pseudo.onStop()
            }
            LifeCycle.onResume ->{
            }
            LifeCycle.onStop ->{
            }
            LifeCycle.onPause ->{
               pseudo.onResume()
            }
            else ->
               throw Exception("Invalid usage")
         }
         pseudo.onDestroy()
      }
   }

   init {
      ActivityFlutterMediator.instance = this
   }





   /*
   *
   *
   *              C h a n n e l   H a n d l e r
   *
   *
   */
   override fun onMethodCall(call: MethodCall, result: Result): Unit {
      SharedCodes.log("onMethodCall", call.method);
      when (call.method) {
         Platform.ACTIVITY_RESULT -> {
            Platform.activityResult(call, result)
         }else ->
            throw NotImplementedError("method: ${call.method} for channel: $CHANNEL not found")
      }
   }
   /*
   *       handle new intent request from Activity (not a pseudo one)
   */
   override fun onNewIntent(intent: Intent): Boolean {
      //nothing here for now...
      return false;
   }

   /*
   *       activity result handler (
   *       handle activity result from Activity (not a pseudo one)
   */
   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Boolean {
      SharedCodes.log("onActivityResult", "requestCode: $requestCode")
      // nothing here for now...
      return true
   }

   /*
   *           B r o a d C a s t
   *
   *
   *
   *       initialize
   *           - intentFilter
   *           - localBroadCastManager
   *           - registerReceiever
   * */
   //todo:
   fun broadCastInit() {
      val manager: LocalBroadcastManager = LocalBroadcastManager.getInstance(registrar.context())
      mediators.forEach {
         manager.registerReceiver(this, it.pseudo_activity.intent_filter)
      }
   }

   /*
           implement how intent data be fetched
           @return: Map<String, Any>
   */
   override fun processIntentOnReceive(intent: Intent, requestCode: Int?): Boolean {
      //todo:
      var ret: Map<String, Any>? = null;
      when (intent.action) {
         "Album" ->
            ret = (intent.getParcelableExtra("Album") as Album).toMap()
          else ->
            return false
      }
      channel.invokeMethod(intent.action, ret);
      return true
   }

   /*
           broadCast handler (receiver)

   */
   override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == null)
         return;
      processIntentOnReceive(intent);
   }




}








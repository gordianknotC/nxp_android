import 'dart:typed_data';

import "package:nxp/platform/channels.dart";
import "package:flutter/services.dart";
import "package:flutter/foundation.dart" show debugPrint;
import "package:nxp_bloc/consts/datatypes.dart";
import "package:nxp/bloc/activity_bloc.dart";
import "package:nxp/di/injection.dart";
import "package:common/common.dart";
import 'package:nxp/impl/index.dart';
import 'package:nxp_bloc/mediators/sketch/configs.dart';


final D = Injection.get<TLogWriter>();


T findEnumByCall<T>(List<T> _enum, MethodCall call, MethodChannel CH){
   return _enum
       .firstWhere((e) => e.toString().split('.')[1] == call.method,
       orElse: () =>
         throw Exception("Method:${call.method} not found on MethodChannel: ${CH.name}"));
}

T findEnumByArg<T>(List<T> _enum, String arg, MethodChannel CH){
   return _enum
       .firstWhere((e) => e.toString().split('.')[1] == arg,
       orElse: () =>
       throw Exception("Method:$arg not found on MethodChannel: ${CH.name}"));
}




class RegisterSession {
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$RegisterSessionActivity");

   static init() {
      D("RegisterSession init......");
      CH.setMethodCallHandler((MethodCall call) async {
         ERegisterSessionMethods method = findEnumByCall(ERegisterSessionMethods.values, call, CH);
         D("RegisterSession receive method: ${call.method} from platform, args:${call.arguments}");
         switch (method) {
            case ERegisterSessionMethods.onSetAnswer:
               D("method matched: $method");
               return _onSetAnswer(call);
         }
         throw UnimplementedError("Dart method: ${method.toString()} not implemented yet");
      });
      D("RegisterSession init");
   }

   // call from platform to dart
   static _onSetAnswer(MethodCall call) {
      D("RegisterSession > _onSetAnser, arguments: ${call.arguments}");
      Ntag_I2C_Registers result = Ntag_I2C_Registers.fromMap(call.arguments as Map);
      bloC.onRegisterSessionResponse(result);
   }
}
class NDEFFragment with CommonUI{
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$ndefFragment");

   static init(){
      CH.setMethodCallHandler((call) async {
         EndefFragment_Methods method = findEnumByCall(EndefFragment_Methods.values, call, CH);
         D("NDEFFragment receive method: ${call.method} from platform, args:${call.arguments}");
         switch(method){
            case EndefFragment_Methods.toastMessage:     return _onToastMessage(call);
            case EndefFragment_Methods.updateDataRate:   return _onDataRateUpdate(call);
            case EndefFragment_Methods.updateToUi:       return _onUpdate(call);
            case EndefFragment_Methods.updateProgress:    return _onUpdateProgress(call);
            default:
               throw UnimplementedError("Uncaught Dart method: $method");
         }
      });
   }

   static updateEditText(String text){
      text ??= "";
      CH.invokeMethod(ename(EndefFragment_Methods.updateEditText), {
         ename(EndefFragment.ndefEditText): text
      });
   }
   /*
   |
   |     Methods call from Dart to Platform
   |
   */
   /*static Future<bool> readNdef(){
      D("NDEFFragment -> readNdef");
      return CH.invokeMethod(ename(EndefFragment_Methods.readNdefClick), null).then((res){
         assert (res is bool, "return type of readNDEF should be bool");
         return res;
      });
   }
   static writeNdef( ){
      D("NDEFFragment -> writeNdef");
      return CH.invokeMethod(ename(EndefFragment_Methods.writeNdefClick), null).then((res){
         assert (res is bool, "return type of writeNdef should be bool");
         return res;
      });
   }*/
   static Future<bool> updateToPlatform(NdefFragmentOption data){
      D("NDEFFragment -> updateToPlatform: $data");
      return CH.invokeMethod(ename(EndefFragment_Methods.updateToPlatform), data.delegate).then((res){
         assert(res is bool, "return type of updateToPlatform should be bool");
         return res as bool;
      }).catchError(() => throw Exception("CallPlatformError"));
   }
   /*
   |
   |     Call from Platform to dart
   |        Messages Call From Dart to Platform and received on Dart side
   |
   */
   // response after calling readNdef on dart
   static Future<bool>_onUpdate(MethodCall call){
      var data = NdefFragmentOption.fromMap(call.arguments as Map);
      D("NDEFFragment -> readNdef - onUpdate: $data");
      bloC.onNDEFReadResponse(data);
      return Future.value(true);
   }
   static Future<bool> _onUpdateProgress(MethodCall call){
      var data = NdefFragmentOption()..fromUpdate(call.arguments as Map);
      D("NDEFFragment -> readNdef - onUpdateProgress: text:${data.ndefText}, type:${data.ndefType}, rate: ${data.datarate}");
      //todo:
//      bloC.onNDEFReadUpdate(data);
      return Future.value(true);

   }
   // response while calling readNdef in progress
   static  Future<bool>_onDataRateUpdate(MethodCall call){
      final datarate = call.arguments['datarate'] as String;
      D("NDEFFragment -> readNdef - _onDataRateUpdate: $datarate");
      bloC.onDataRateUpdate(datarate);
      return Future.value(true);
   }
   static  Future<bool>_onToastMessage(MethodCall call){
      final message = call.arguments['msg'] as String;
      final error = call.arguments['error'] as bool;
      final tag   = call.arguments['tag'] as String;
      D("NDEFFragment -> _onToastMessage: $message, $error, $tag");
      final ENdefResponses tagenum = tag == null
         ? null
         : findEnumByArg(ENdefResponses.values, tag, CH);
      if (tagenum == null || tagenum == ENdefResponses.toastMessage){
         bloC.onToastMessage(message, error);
      }else{
         _onNDEFResponseMessage(message, error, tagenum);
      }
      return Future.value(true);
   }
   /*
   *     NDEF Response Messages
   */
   static _onNDEFResponseMessage(String message, bool error, ENdefResponses tagenum){
      switch (tagenum){
         case ENdefResponses.ndefDetected:
            bloC.onNDEFDetected(message);
            break;
         case ENdefResponses.ndefWriteIncorrectContent:
            bloC.onNDEFWriteIncorrectContent(message);
            break;
         case ENdefResponses.ndefReadLost:
            bloC.onNDEFReadLost(message);
            break;
         case ENdefResponses.ndefReadError:
            bloC.onNDEFReadError(message);
            break;
         /*case ENdefResponses.ndefReadResponse:
            bloC.onNDEFReadResponse(ndefOption);*/
            break;
         case ENdefResponses.ndefReadProtected:
            bloC.onNDEFReadProtected(message);
            break;
         case ENdefResponses.ndefWriteLost:
            bloC.onNDEFWriteLost(message);
            break;
         case ENdefResponses.ndefWriteError:
            bloC.onNDEFWriteError(message);
            break;
         case ENdefResponses.ndefWriteResponse:
            bloC.onNDEFWriteResponse(message);
            break;
         case ENdefResponses.ndefWriteProtected:
            bloC.onNDEFWriteProtected(message);
            break;
         case ENdefResponses.ndefTapRead:
            bloC.onNDEFTapToRead(message);
            break;
         case ENdefResponses.ndefTapWrite:
            bloC.onNDEFTapToWrite(message);
            break;
         default:
            throw Exception('Uncaught Exception: tagenum: $tagenum not propertly caught');
      }
   }
}




class NtagI2CDemo with CommonUI {
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$Ntag_I2C_Demo");

   static init() {
      CH.setMethodCallHandler((MethodCall call) async {
         ENtagDemoMethods method = findEnumByCall(ENtagDemoMethods.values, call, CH);
         print("NtagI2CDemo receive method: ${call.method} from platform, args:${call.arguments}");

         //throw Exception("Uncaught exception, method:${call.method} not implemented yet");
      });

      CH.setMethodCallHandler((MethodCall call) async {
         ECommonUI_Methods method = findEnumByCall(ECommonUI_Methods.values, call, CH);
         switch(method){
            case ECommonUI_Methods.onSetBoardVersion:          return CommonUI._onSetBoardVersion(call);
            case ECommonUI_Methods.onSetDataRate:              return CommonUI._onSetDataRate(call);
            case ECommonUI_Methods.onShowAlert:                return CommonUI._onShowAlert(call);
            case ECommonUI_Methods.onShowOkDialogue:           return CommonUI._onShowOKDialogue(call);
            case ECommonUI_Methods.onShowOkDialogueForAction:  return CommonUI._onShowOKDialogueForAction(call);
            case ECommonUI_Methods.onToastMakeText:            return CommonUI._onToastMakeText(call);
            case ECommonUI_Methods.onShowYNDialogueForAction:  return CommonUI._onShowYNDialogueForAction(call);
            case ECommonUI_Methods.closeYNDialogue:            return CommonUI._closeYNDialogue(call);
            case ECommonUI_Methods.onShowProgressDialogue:     return CommonUI._onShowProgressDialogue(call);
         }
         throw UnimplementedError("Uncaught Dart method: ${method.toString()} not implemented yet");
      });
   }
   static Future<bool> isReady(){
      D("DEMO -> isReady");
      return CH.invokeMethod(ename(ENtagDemoMethods.isReady));
   }
   static Future<bool> isConnected(){
      D("DEMO -> isConnected");
      return CH.invokeMethod(ename(ENtagDemoMethods.isConnected));
   }
   static Future<bool> isTagPresent(){
      D("DEMO -> isTagPresent");
      return CH.invokeMethod(ename(ENtagDemoMethods.isTagPresent));
   }
   static void finishAllTasks(){
      D("DEMO -> finishAllTask");
      CH.invokeMethod(ename(ENtagDemoMethods.finishAllTasks));
   }
   static Future<ProductInfo> getProduct(){
      D("DEMO -> getProduct");
      return CH.invokeMethod(ename(ENtagDemoMethods.getProduct)).then((res){
         assert(res is Map, "Invalid return type of getProduct");
         final result = ProductInfo.fromMap(res as Map);
         bloC.onReadProductInfo(result);
         return result;
      });
   }
   static Future<bool> resetTagMemory(){
      D("DEMO -> resetTagMemory");
      return CH.invokeMethod(ename(ENtagDemoMethods.resetTagMemory)).then((result) => result != -1);
   }
   static readSessionRegisters(){
      D("DEMO -> readSessionRegisters");
      isReady().then((b){
         if (b)
            return CH.invokeMethod(ename(ENtagDemoMethods.readSessionRegisters));
         bloC.onToastMessage("please tap tag first", false);
      });
      // fetch call backs on NdefFragment.onSetAnswer
   }
   static Future<Uint8List> readTagContent(){
      D("DEMO -> readTagContent");
      return CH.invokeMethod(ename(ENtagDemoMethods.readTagContent));
   }
   static Future<bool> resetTagContent(){
      D("DEMO -> resetTagContent");
      return CH.invokeMethod(ename(ENtagDemoMethods.resetTagContent));
   }


   static writeNDEF(String data, [WriteOptions type = WriteOptions.RadioNdefText]){
      D("DEMO -> writeNDEF: $data");
      Map args = {"type": ename(type), "NdefMessage": data};
      vibrate();
      CH.invokeMethod(ename(ENtagDemoMethods.writeNDEF), args);
   }
   static void readNDEF([Uint8List pwd]){
      pwd ??= genPassword();
      D("DEMO -> readNDEF");
      CH.invokeMethod(ename(ENtagDemoMethods.readNDEF));
   }
   static Future<EAuthStatus> obtainAuthStatus(){
      D("DEMO -> ObtainAuthStatus");
      return CH.invokeMethod(ename(ENtagDemoMethods.ObtainAuthStatus)).then((s){
         return EAuthStatus.values.firstWhere((v) => v.index == s as int);
      });
   }
   static vibrate([int time = 40]){
      CH.invokeMethod(ename(ENtagDemoMethods.vibrate), {
         "time": time
      });
   }
   static Future<bool> auth(String text, int authStatus){
      final args = {
        "pwd": genPassword(text),
        "authStatus": authStatus
      };
      return CH.invokeMethod(ename(ENtagDemoMethods.Auth), args);
   }
   static Uint8List genPassword([String text]){
      return Uint8List.fromList(text?.substring(0, 4)?.codeUnits ?? [256, 256, 256, 256]);
   }
}



mixin CommonUI{
   static ActivityBloCImp bloC ;
   static MethodChannel CH;

   static Future<bool> _onToastMessage(MethodCall call){
      throw Exception('_onToastMessage...');
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("toast message: $arg");
      final message = call.arguments['msg'] as String;
      final error = call.arguments['error'] as bool;
      final tag   = call.arguments['tag'] as String;
      bloC.onToastMessage(message, error);
      return Future.value(true);
   }

   static Future<bool> _onToastMakeText(MethodCall call){
      throw Exception('_onToastMakeText...');
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("toast: $arg");
      return Future.value(true);
   }

   static Future<bool> _onSetBoardVersion(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("setboard: $arg");
      return Future.value(true);
   }

   static Future<bool> _onShowAlert(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("setboard: $arg");
      return Future.value(true);
   }

   static Future<bool> _closeYNDialogue(MethodCall call){
      D("closeYNDialogue");
      return Future.value(true);
   }

   static Future<bool> _onShowYNDialogueForAction(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("showYN Dialogue for action: $arg");
      var yesOrNo = true;
      return Future.value(yesOrNo);
   }
   static Future<bool> _onShowOKDialogue(MethodCall call){
      var message = call.arguments["message"];
      var title = call.arguments["title"];
      var request = call.arguments["\$request"];
      D("onShowOKDialogue: $message, $title, $request");
      var isOk = true;
      return Future.value(isOk);
   }
   static Future<bool> _onShowOKDialogueForAction(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("onShowOKDialogueForAction: $arg");
      var isOk = true;
      return Future.value(isOk);
   }

   static Future<bool> _onSetDataRate(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      D("onSetDataRate: $arg");
      return Future.value(true);
   }
   static _onShowProgressDialogue(MethodCall call){

   }
}





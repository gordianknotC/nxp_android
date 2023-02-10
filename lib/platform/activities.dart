import 'dart:async';
import 'dart:typed_data';

import 'package:PatrolParser/PatrolParser.dart';
import 'package:flutter/material.dart';
import "package:nxp/platform/channels.dart";
import "package:flutter/services.dart";
import 'package:nxp/ui/flareExtra/nfcAnimController.dart';
import "package:nxp_bloc/consts/datatypes.dart";
import "package:nxp/bloc/activity_bloc.dart";
import 'package:nxpapp_config/src/di/injection.dart';
import "package:common/common.dart";
import 'package:nxpapp_config/src/index.dart';
import 'package:nxp_bloc/mediators/controllers/app_bloc.dart';
import 'package:shared_preferences/shared_preferences.dart';


import '../main.dart';


final _D = AppState.getLogger("ACTIV");
final D_android = AppState.getLogger("Android", showOutput: false);


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


class TEEPROMResponse{
   bool success;
   String error;
   Uint8List data;
   int bytesShift = 0;
   
   TEEPROMResponse.fromDynamic(dynamic data){
      if (data is Map){
         _fromMap(data as Map );
      }else if (data is bool){
         success = data;
      }else{
         throw Exception('Invalid type of data: $data');
      }
   }
   TEEPROMResponse.fromMap(Map  data){
      _fromMap(data);
   }
   void _fromMap(Map  data){
      if (data.containsKey('error')){
         error = data['error'] as String;
      }
      if (data.containsKey('data')){
         this.data = data['data'] as Uint8List;
      }
      success = error == null;
   }
}



/// :: dart to dart
/// -> platfrom to dart
/// <- dart to platform
class PseudoMain {
   static AppBloC bloc = Injection.injector.get<AppBloC>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$PseudoMainActivity");
   
   static void init() {
      _D("PseudoMain init......");
      CH.setMethodCallHandler((MethodCall call) async {
         EPseudoMainActivityMethods method = findEnumByCall(EPseudoMainActivityMethods.values, call, CH);
         _D("PseudoMainActivity receive method: ${call.method} from platform, args:${call.arguments}");
         switch (method) {
            case EPseudoMainActivityMethods.closeAppNoNfcAlert:
               _D("method matched: $method");
               return _onCloseAppNoNfcAlert(call);
            case EPseudoMainActivityMethods.showSettingNoNfcAlert:
               _D("method matched: $method");
               return _onShowSettingNoNfcAlert(call);
            case EPseudoMainActivityMethods.nfcAvailable:
               _D("method matched: $method");
               return _onNfcAvaialble(call);
            default:
               throw Exception("uncaught method: $method");
         }
         throw UnimplementedError("Dart method: ${method.toString()} not implemented yet");
      });
      _D("RegisterSession init");
   }
   
   // call from platform to dart
   static _onCloseAppNoNfcAlert(MethodCall call) {
      _D("PseudoMainActivity -> _onCloseAppNoNfcAlert, arguments: ${call.arguments}");
      bloc.onDispatch(AppCloseNoNfcEvent());
   }
   
   static _onShowSettingNoNfcAlert(MethodCall call) {
      _D("PseudoMainActivity -> _onShowSettingNoNfcAlert, arguments: ${call.arguments}");
      bloc.onDispatch(AppShowSettingNoNfcEvent());
   }
   
   static _onNfcAvaialble(MethodCall call) {
      _D("PseudoMainActivity -> _onNfcAvaialble, arguments: ${call.arguments}");
      bloc.onDispatch(AppNFCAvailableEvent());
   }
}



/// :: dart to dart
/// -> platfrom to dart
/// <- dart to platform
class RegisterSession {
   static bool _blockRegSessionOnce = false;
   static bool get blockRegSessionOnce => _blockRegSessionOnce;
   static void set blockRegSessionOnce(bool v){
      _D.debug('set blockRegSessionOnce $v');
      _blockRegSessionOnce = v;
   }
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$RegisterSessionActivity");
   
   static init() {
      _D("RegisterSession init......");
      CH.setMethodCallHandler((MethodCall call) async {
         ERegisterSessionMethods method = findEnumByCall(ERegisterSessionMethods.values, call, CH);
         _D("RegisterSession receive method: ${call.method} from platform, args:${call.arguments}");
         switch (method) {
            case ERegisterSessionMethods.onSetAnswer:
               _D("method matched: $method");
               return _onSetAnswer(call);
            case ERegisterSessionMethods.onTagLost:
               _D("method matched: $method");
               return _onTagLost(call);
            case ERegisterSessionMethods.onTransceiveFailed:
               _D("method matched: $method");
               return _onTransceiveFailed(call);
         }
         throw UnimplementedError("Dart method: ${method.toString()} not implemented yet");
      });
   }
   
   // read session register from platform...
   // call from platform to dart
   static _onSetAnswer(MethodCall call) {
      if (!blockRegSessionOnce){
         _D("RegisterSession -> _onSetAnser, arguments: ${call.arguments}");
         Ntag_I2C_Registers result = Ntag_I2C_Registers.fromMap(call.arguments as Map);
         _D("RegisterSession -> _onSetAnser, register: ${result}");
         if((call.arguments as Map).containsKey("NDEF_Message")){
            _D('recieve NDEF_Message: ${result.NDEF_Message.length}, ${result.NDEF_Message}');
            _D('receive NDEF_Message_BYTES: ${result.NDEF_Message_BYTES}');
            _D('receive NDEF_RAW_RECORD   : ${result.NDEF_RAW_RECORD.asMap()}');
         }
         bloC.onRegisterSessionResponse(result);
      }else{
         _D("block RegisterSession -> _onSetAnser, arguments: ${call.arguments}");
      }
      blockRegSessionOnce = false;
   }
   
   static void _onTagLost(MethodCall call) {
      bloC.onTagLost();
   }
   
   static void _onTransceiveFailed(MethodCall call) {
      bloC.onTransceiveFailed();
   }
}

class NDEFFragment with CommonUI{
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$ndefFragment");
   
   static init(){
      CH.setMethodCallHandler((call) async {
         EndefFragment_Methods method = findEnumByCall(EndefFragment_Methods.values, call, CH);
         _D("NDEFFragment -> ${call.method} from platform, args:${call.arguments}");
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
      _D("NDEFFragment -> readNdef");
      return CH.invokeMetho_D(ename(EndefFragment_Methods.readNdefClick), null).then((res){
         assert (res is bool, "return type of readNDEF should be bool");
         return res;
      });
   }
   static writeNdef( ){
      _D("NDEFFragment -> writeNdef");
      return CH.invokeMetho_D(ename(EndefFragment_Methods.writeNdefClick), null).then((res){
         assert (res is bool, "return type of writeNdef should be bool");
         return res;
      });
   }*/
   static Future<bool> updateToPlatform(NdefFragmentOption data){
      _D("NDEFFragment <- updateToPlatform: $data");
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
      // PatrolRecord.memBytesToByeStringg
      var data = NdefFragmentOption.fromMap(call.arguments as Map);
      _D("NDEFFragment -> readNdef - onUpdate: $data");
      bloC.onNDEFReadResponse(data);
      return Future.value(true);
   }
   static Future<bool> _onUpdateProgress(MethodCall call){
      // PatrolRecord.memBytesToByeStringg
      var data = NdefFragmentOption()..fromUpdate(call.arguments as Map);
      _D("NDEFFragment -> readNdef - onUpdateProgress: text:${data.ndefText}, type:${data.ndefType}, rate: ${data.datarate}");
//      bloC.onNDEFReadUpdate(data);
      return Future.value(true);
      
   }
   // response while calling readNdef in progress
   static  Future<bool>_onDataRateUpdate(MethodCall call){
      final datarate = call.arguments['datarate'] as String;
      _D("NDEFFragment -> read/write - _onDataRateUpdate: $datarate");
      bloC.onDataRateUpdate(datarate);
      return Future.value(true);
   }
   static  Future<bool>_onToastMessage(MethodCall call){
      final message = call.arguments['msg'] as String;
      final error = call.arguments['error'] as bool;
      final tag   = call.arguments['tag'] as String;
      _D("NDEFFragment -> _onToastMessage: $message, $error, $tag");
      final ENdefResponses tagenum = tag == null
          ? null
          : findEnumByArg(ENdefResponses.values, tag, CH);
      if (tagenum == null || tagenum == ENdefResponses.toastMessage){
         bloC.onToastMessage(message, error:error);
      }else{
         _onNDEFResponseMessage(message, error, tagenum);
      }
      return Future.value(true);
   }
   /*
   *     NDEF Response Messages
   */
   static _onNDEFResponseMessage(String message, bool error, ENdefResponses tagEnum){
      switch (tagEnum){
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
//         case ENdefResponses.toastMessage:
//            bloC.onToastMessage(message);
//            break;
//         case ENdefResponses.ndefReadResponse:
//            break;
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
         case ENdefResponses.ndefReadSuccess:
            bloC.onNDEFReadSuccess(message, tagEnum);
            break;
         case ENdefResponses.ndefWriteSuccess:
            bloC.onNDEFWriteSuccess(message, tagEnum);
            break;
         default:
            throw Exception('uncaught: $tagEnum');
      }
   }
}




class NtagI2CDemo with CommonUI {
   static ActivityBloCImp bloC = Injection.injector.get<ActivityBloCImp>();
   static AppBloC appbloc = Injection.injector.get<AppBloC>();
   static MethodChannel CH = MethodChannel(
       "$BASE_NAME.$Ntag_I2C_Demo");
   
   static init() {
      flutterReady();
      CH.setMethodCallHandler((MethodCall call) async {
         ECommonUI_Methods method = findEnumByCall(ECommonUI_Methods.values, call, CH);
         _D("NtagI2CDemo -> ${call.method} from platform, args:${call.arguments}");
         switch(method){
            case ECommonUI_Methods.onReading        :          return CommonUI._onReading(call);
            case ECommonUI_Methods.onLogToDart      :          return CommonUI._onLogToDart(call);
            case ECommonUI_Methods.onSetBoardVersion:          return CommonUI._onSetBoardVersion(call);
            case ECommonUI_Methods.onSetDataRate    :          return CommonUI._onSetDataRate(call);
            case ECommonUI_Methods.onShowAlert      :          return CommonUI._onShowAlert(call);
            case ECommonUI_Methods.onShowOkDialogue :          return CommonUI._onShowOKDialogue(call);
            case ECommonUI_Methods.onShowOkDialogueForAction:  return CommonUI._onShowOKDialogueForAction(call);
            case ECommonUI_Methods.onToastMakeText  :          return CommonUI._onToastMakeText(call);
            case ECommonUI_Methods.onShowYNDialogueForAction:  return CommonUI._onShowYNDialogueForAction(call);
            case ECommonUI_Methods.closeYNDialogue  :          return CommonUI._closeYNDialogue(call);
            case ECommonUI_Methods.onShowProgressDialogue:     return CommonUI._onShowProgressDialogue(call);
            case ECommonUI_Methods.onNFCServiceDead :          return CommonUI._onShowNFCServiceDead(call);
            case ECommonUI_Methods.onJavaException  :          return CommonUI._onShowJavaException(call);
         }
         throw UnimplementedError("Uncaught Dart method: ${method.toString()} not implemented yet");
      });
   }
   
   
   /*static Future<String>
   getAndroidLogs(){
      _D("DEMO <- getAndroidLogs");
      return CH.invokeMethod(ename(ENtagDemoMethods.getLog)).then((res){
         return res as String;
      });
   }*/
   
   static Future<ENFCState>
   checkNFC({bool checkOnly = true}){
      _D("DEMO <- checkNFC");
      return CH.invokeMethod(ename(ENtagDemoMethods.checkNFC)).then((res){
         assert (res is String, "Invalid return type");
         final result = ENFCState.values.firstWhere((v) => v.toString().endsWith(res as String));
         print("DEMO <- checkNFC, result:$result, $checkOnly");
         if (!checkOnly){
            switch(result){
               case ENFCState.NFC_AVAILABLE:
                  appbloc.onDispatch(AppNFCAvailableEvent());
                  break;
               case ENFCState.NFC_DISABLED:
                  appbloc.onDispatch(AppShowSettingNoNfcEvent());
                  break;
               case ENFCState.NO_NFC_DEVICE:
                  appbloc.onDispatch(AppCloseNoNfcEvent());
                  break;
               default:
                  throw Exception('uncaught exception');
            }
         }
         return result;
      });
   }
   
   static Future<bool>
   doProcessOnExistingTagWithoutAuthCheck(){
      _D("DEMO <- doProcessOnExistingTagWithoutAuthCheck");
      return CH.invokeMethod(ename(ENtagDemoMethods.doProcessOnExistingTagWithoutAuthCheck)).then((res){
         return res as bool;
      });
   }
   
   static Future<String>
   flutterReady(){
      _D("DEMO <- flutterReady");
      return CH.invokeMethod(ename(ENtagDemoMethods.flutterReady)).then((res){
         final result = res as Map;
         if(result.containsKey('error') && (result['error'] as String).isNotEmpty){
            print('triggerCrash...');
            AppState.bloC.onDispatch(AppCrashEvent(result['error'] as String));
         }
      });
   }
   
   static Future<String>
   checkNFC2(){
      _D("DEMO <- checkNFC2");
      return CH.invokeMethod(ename(ENtagDemoMethods.checkNFC2)).then((res){
         return res as String;
      });
   }
   static Future<String>
   checkNFC3(){
      _D("DEMO <- checkNFC3");
      return CH.invokeMethod(ename(ENtagDemoMethods.checkNFC3)).then((res){
         return res as String;
      });
   }
   static Future<String>
   checkNFC4(){
      _D("DEMO <- checkNFC4");
      return CH.invokeMethod(ename(ENtagDemoMethods.checkNFC4)).then((res){
         return res as String;
      });
   }
   static Future<bool> isReady(){
      _D("DEMO <- isReady");
      return CH.invokeMethod(ename(ENtagDemoMethods.isReady));
   }
   static Future<bool> isConnected(){
      _D("DEMO <- isConnected");
      return CH.invokeMethod(ename(ENtagDemoMethods.isConnected));
   }
   static Future<bool> isTagPresent([String callFrom= '']){
      _D("DEMO <- isTagPresent $callFrom");
      return CH.invokeMethod(ename(ENtagDemoMethods.isTagPresent));
   }
   static void finishAllTasks(){
      _D("DEMO <- finishAllTask");
      CH.invokeMethod(ename(ENtagDemoMethods.finishAllTasks));
   }
   static Future<ProductInfo> getProduct(){
      _D("DEMO <- getProduct");
      return CH.invokeMethod(ename(ENtagDemoMethods.getProduct)).then((res){
         assert(res is Map, "Invalid return type of getProduct");
         final result = ProductInfo.fromMap(res as Map);
         bloC.onReadProductInfo(result);
         return result;
      });
   }
   static Future<bool> resetTagMemory(){
      _D("DEMO <- resetTagMemory");
      return CH.invokeMethod(ename(ENtagDemoMethods.resetTagMemory)).then((result) => result != -1);
   }
   
   
   
   static Future<Uint8List> readTagContent(){
      _D("DEMO <- readTagContent");
      return CH.invokeMethod(ename(ENtagDemoMethods.readTagContent));
   }
   
   static Future<bool> resetTagContent(){
      _D("DEMO <- resetTagContent");
      return CH.invokeMethod(ename(ENtagDemoMethods.resetTagContent));
   }
   
   static Future<EAuthStatus> obtainAuthStatus(){
      _D("DEMO <- ObtainAuthStatus");
      return CH.invokeMethod(ename(ENtagDemoMethods.ObtainAuthStatus)).then((s){
         return EAuthStatus.values.firstWhere((v) => v.index == s as int);
      });
   }
   
   static Future<Map> dartBytesToPlatformString(Map<String,dynamic> data){
      _D("DEMO <- dartBytesToPlatformString: ${data}");
      return CH.invokeMethod("dartBytesToPlatformString", data);
   }
   
   static Future<Ntag_I2C_Registers>
   readSessionRegistersCustom({int startAt = 13, int length = 32}){
      final arg = {
         'startAt': Uint8List.fromList([startAt]),
         'length' : length,
      };
      _D("DEMO <- readSessionRegistersCustom");
      return isReady().then((b){
         if (b){
            return CH.invokeMethod(ename(ENtagDemoMethods.readSessionRegistersCustom), arg).then((res){
               if ((res as Map).containsKey("error")) {
                  _D('resetRes error: ${res['error']}');
                  return Ntag_I2C_Registers.errorResponse(res["error"] as String);
               }else{
                  Ntag_I2C_Registers result = Ntag_I2C_Registers.fromMap(res as Map);
                  _D('readSessionRegistersCustom NDEF_Message      : ${result.NDEF_Message.length}, ${result.NDEF_Message}');
                  _D('readSessionRegistersCustom NDEF_Message_BYTES: ${result.NDEF_Message_BYTES}');
                  _D('readSessionRegistersCustom NDEF_RAW_RECORD   : ${result.NDEF_RAW_RECORD.asMap()}');
                  return result;
               }
            });
         }
         bloC.onToastMessage("please tap tag first", error: false);
         return Ntag_I2C_Registers.errorResponse("please tap tag first");
      });
   }
   
   static List<int> inferPosition(int pos){
      final startShift = 0x10 + (pos / 4).floor();
      final bytesShift = startShift > 0 ? pos % 4 : 0;
      return [startShift, bytesShift];
   }
   
   static Future<TEEPROMResponse>
   readIdField() async {
      // sub id #2
      final hybridPos = inferPosition(20);
      final res = await readEEPROMCustomBytes(hybridPos[0], length: 3);
      res.bytesShift = hybridPos[1];
      return res;
   }
   
   static Future<TEEPROMResponse>
   readCommandField() async {
      // 16 17 18 19
      //# 0  1  2  3
      // 20 21 22 23
      //# 4  5  6  7
      // 30 31 32 33
      //#20 21 22 23
      final hybridPos = inferPosition(20);
      final res = await readEEPROMCustomBytes(hybridPos[0], length: 1, readCommand: true);
      res.bytesShift = hybridPos[1];
      return res;
   }
   
   static Future<TEEPROMResponse>
   readEEPROMCustomBytes(int startAt, {bool vib = true, int length = 32, bool readCommand = false}){
      final arg = {
         'startAt': Uint8List.fromList([startAt]),
         'length' : length,
         'extra': {
            'readCommand': readCommand
         }
      };
      _D("DEMO <- readEEPROMCustomBytes: $arg");
      if(vib)
         vibrate();
      return CH.invokeMethod(ename(ENtagDemoMethods.readEEPROMCustomBytes), arg).then((res){
         final result = res as Map;
         if (result.containsKey("error")){
            return TEEPROMResponse.fromMap(result as Map );
         }else{
            _D("DEMO <- readEEPROMCustomBytes: ${result['data'] as Uint8List}");
            return TEEPROMResponse.fromMap(result as Map );
         }
      });
   }
   
   static Future<TEEPROMResponse>
   writeIdField(int command, int id1, int id2, int id3) async {
      // 16 17 18 19
      //# 0  1  2  3
      // 20 21 22 23
      //#4  5  6  7
      // 30 31 32 33
      //#20 21 22 23
      final hybridPos = NtagI2CDemo.inferPosition(20);
      final data = [command, id1, id2, id3];
      return await NtagI2CDemo.writeEEPROMCustomBytes(hybridPos[0], Uint8List.fromList(data));
   }
   
   static Future<TEEPROMResponse>
   writeCommandField(int command, int id1, int id2, int id3) async {
      final hybridPos = NtagI2CDemo.inferPosition(20);
      final data = [command, id1, id2, id3];
      return await NtagI2CDemo.writeEEPROMCustomBytes(hybridPos[0], Uint8List.fromList(data));
   }
   
   static Future<TEEPROMResponse>
   writeEEPROMCustomBytes(int startAt, Uint8List bytes, {bool vib = true, bool writeCommand = false}){
      final completer = Completer<TEEPROMResponse>();
      final arg = {
         'startAt'    : Uint8List.fromList([startAt]),
         "NdefMessage": Uint8List.fromList(bytes),
         'extra':{
            'writeCommand': writeCommand
         }
      };
      if (vib)
         vibrate();
      _D("DEMO <- write bytes: $bytes\nat $startAt");
      CH.invokeMethod(ename(ENtagDemoMethods.writeEEPROMCustomBytes), arg).then((res){
         _D.debug('response from writeEEPROMCustomBytes: $res');
         if (res is bool){
         } else if (res is Map){
            final result = res;
            if (result.containsKey("error")){
            }
         }
         completer.complete(TEEPROMResponse.fromDynamic(res));
         return res;
      }).catchError((_, s){
         completer.complete(TEEPROMResponse.fromMap({
            'error': _ ?? s
         }));
         _D.error('on writeEEPROMCustomBytes failed: $_\n$s');
      });
      return completer.future;
   }
   static Future<Ntag_I2C_Registers>
   resetRegisterCustomBytes({int startAt = 0x10, String memstring, bool isWrite, int length = 32}){
      final bytes = PatrolRecord.memStringToBytes(memstring);
      _D("DEMO <- resetRegister start at: $startAt, memString: $memstring");
      final arg = {
         'startAt': Uint8List.fromList([startAt]),
         'length': length,
         "type": ename(WriteOptions.RadioNdefBytes),
         "NdefMessage": Uint8List.fromList(bytes),
         "isWrite": isWrite
      };
      return CH.invokeMethod(ename(ENtagDemoMethods.resetRegisterCustomBytes), arg).then((res){
         if (res is bool){
            _D('resetRes writtenResponse: ${res}');
            return Ntag_I2C_Registers.writtenResponse(res);
         }
         if (res is Map){
            if (res.containsKey("error")){
               _D('resetRes error: ${res['error']}');
               ResetAnimController.instance.resetError();
               return Ntag_I2C_Registers.errorResponse(res["error"] as String);
            }else{
               final data = res['data'] as Uint8List;
               Ntag_I2C_Registers result = Ntag_I2C_Registers.fromMap(res as Map);
               _D('resetRes NDEF_Message      : ${result.NDEF_Message.length}, ${result.NDEF_Message}');
               _D('resetRes NDEF_Message_BYTES: ${result.NDEF_Message_BYTES}');
               _D('resetRes NDEF_RAW_RECORD   : ${result.NDEF_RAW_RECORD.asMap()}');
               return result;
            }
         }
         
      }).catchError((e){
         return null;
      });
   }
   
   static readSessionRegisters(){
      _D("DEMO <- readSessionRegisters");
      return isReady().then((b){
         if (b){
            CH.invokeMethod(ename(ENtagDemoMethods.readSessionRegisters));
            return true;
         }
         bloC.onToastMessage("please tap tag first", error: false);
         return false;
      });
   }
   
   static Future<bool> writeNDEF(PatrolRecord record, {int startAt = 0x10}) async {
      final bytes = record.toMemAddressBytes();
      return await NtagI2CDemo.writeEEPROMCustomBytes(startAt, Uint8List.fromList(bytes)).then((reg){
         if (reg.success){
            _D("onWriteEEPROM, write success!");
            StoreImpl.logNtag(record: record, key: AppState.IOMode, onScanFiles: NtagI2CDemo.scanFile);
            return true;
         }else{
            _D("onWriteEEPROM, write failed!");
            return false;
         }
      });
   }
   
   static void readNDEF([Uint8List pwd]){
      //fixme:
      pwd ??= genPassword();
      _D("DEMO <- readNDEF");
      CH.invokeMethod(ename(ENtagDemoMethods.readNDEF)).then((res){
         print('readNDEF future result: $res');
         if (res == true){
         }else{
         }
      }).catchError((e){
         bloC.onToastMessage(e.toString(), error: false);
      });
   }
   
   static void scanFile(List<String> paths){
      CH.invokeMethod(ename(ENtagDemoMethods.scanFile), {
         "paths": paths
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
   static Future<bool> clearAuth(){
      return CH.invokeMethod(ename(ENtagDemoMethods.clearAuth));
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
   }
   
   static Future<bool> _onToastMakeText(MethodCall call){
      throw Exception('_onToastMakeText...');
   }
   
   static _onLogToDart(MethodCall call){
      final data = call.arguments as String;
      D_android(data);
      if ((data?.length ?? 0) > 0){
         if (data.startsWith('[NDemo] android.nfc.TagLostException')){
            RegisterSession._onTagLost(call);
         }else if (data.startsWith('[NDemo] java.io.IOException: Transceive failed')){
            RegisterSession._onTransceiveFailed(call);
         }
      }
   }
   
   static Future<bool> _onSetBoardVersion(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      _D("-> setboard: $arg");
      return Future.value(true);
   }
   
   static Future<bool> _onShowAlert(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      _D("-> setboard: $arg");
      return Future.value(true);
   }
   
   static Future<bool> _closeYNDialogue(MethodCall call){
      _D("-> closeYNDialogue");
      return Future.value(true);
   }
   
   static Future<bool> _onShowYNDialogueForAction(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      _D("-> showYN Dialogue for action: $arg");
      var yesOrNo = true;
      return Future.value(yesOrNo);
   }
   static Future<bool> _onShowOKDialogue(MethodCall call){
      var message = call.arguments["message"];
      var title = call.arguments["title"];
      var request = call.arguments["\$request"];
      _D("-> onShowOKDialogue: $message, $title, $request");
      var isOk = true;
      return Future.value(isOk);
   }
   static Future<bool> _onShowOKDialogueForAction(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      _D("-> onShowOKDialogueForAction: $arg");
      var isOk = true;
      return Future.value(isOk);
   }
   
   static Future<bool> _onSetDataRate(MethodCall call){
      var arg = CommonUIArg.fromMap(call.arguments as Map);
      _D("-> onSetDataRate: $arg");
      return Future.value(true);
   }
   
   static _onShowProgressDialogue(MethodCall call){
   
   }
   
   static _onShowNFCServiceDead(MethodCall call) {
      AppHome.instance.showAlert(
          title: Text("Error: NFC service dead", style: TextStyle(fontSize: 21)),
          content: Text("NFC service dead, please save your work and restart app."),
          onPressed: (){
             //todo:
             SharedPreferences.getInstance().then((_){
                print(_.getKeys());
             });
          }
      );
   }
   
   static _onShowJavaException(MethodCall call) {
      print('_onShowJavaException');
      String message = call.arguments["exception"] as String;
      AppState.bloC.onDispatch(AppCrashEvent(message));
   }
   
   static _onReading(MethodCall call) {
      _D("-> onReading");
      ReadAnimationController.instance.readingAnim.onStartOnce((){
      });
      ActivityStates.bloC.onSetState();
      ReadAnimationController.instance.reading();
   }
}




class ReadAdvancedOptions{
   EAuthStatus authStatus;
   dynamic resetContent;
   String bytestring;
   bool isReady;
   bool isConnected;
   bool isPresent;
   bool clearAuth;
   dynamic registers;
}



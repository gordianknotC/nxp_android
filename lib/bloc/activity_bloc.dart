import 'dart:async';
import 'dart:convert';
import 'package:PatrolParser/PatrolParser.dart';
import 'package:flutter/material.dart';
import 'package:bloc/bloc.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp/platform/activities.dart';
import "package:nxp_bloc/consts/datatypes.dart";
import 'package:nxp_bloc/mediators/controllers/app_bloc.dart';
import 'package:nxp_bloc/mediators/sketch/configs.dart';

import 'package:nxp_bloc/mediators/controllers/activiy_states.dart';

Map<String, dynamic> tryDecodeJson(String text) {
  try {
    return jsonDecode(text) as Map<String, dynamic>;
  } catch (e) {
    return null;
  }
}

class ActivityStates {
  static ActivityBloCImp bloC = Injection.get<ActivityBloCImp>();
  static void beforeEncodingAware(BaseNDEFState result){
    print('\nbeforeEncoding');
    print("R: ${result.rawMessageReadIn}");
    print("R: ${result.encodedMessageReadIn}");
    print("W: ${result.rawMessageToBeWrittenOut}");
    print("W: ${result.encodedMessageToBEWrittenOut}");
  }
  static void afterEncoding(BaseNDEFState result){
    print('\nafterEncoding');
    print("R: ${result.rawMessageReadIn}");
    print("R: ${result.encodedMessageReadIn}");
    print("W: ${result.rawMessageToBeWrittenOut}");
    print("W: ${result.encodedMessageToBEWrittenOut}");
  }
  static void encodingAware(String text, BaseNDEFState result, [bool write = false]) {
    if (AppState.autoEncode) {
      beforeEncodingAware(result);
      if (PatrolRecord.isValidByteString(text)) {
        // already encoded, do nothing
        final patrol = result.patrol = PatrolRecord.fromByteString(text);
        if (write) {
          result.rawMessageToBeWrittenOut = jsonEncode(patrol.toJson());
          result.encodedMessageToBEWrittenOut = patrol.toString();
          return afterEncoding(result);
        }
        result.rawMessageReadIn = jsonEncode(patrol.toJson());
        result.encodedMessageReadIn = patrol.toString();
      } else {
        final json = tryDecodeJson(text);
        if (json == null) {
          // not a patrol record, do nothing...
          if (write) {
            result.rawMessageToBeWrittenOut = text;
            result.encodedMessageToBEWrittenOut = text;
            return afterEncoding(result);
          }
          result.rawMessageReadIn = text;
          result.encodedMessageReadIn = text;
        } else {
          // maybe a patrol record
          PatrolRecord patrol;
          try {
            patrol = result.patrol = PatrolRecord.fromLog(text);

            if (write) {
              result.rawMessageToBeWrittenOut = text;
              result.encodedMessageToBEWrittenOut = patrol.toString();
              return afterEncoding(result);
            }
            result.rawMessageReadIn = text;
            result.encodedMessageReadIn = patrol.toString();
          } catch (e) {
            if (write) {
              result.rawMessageToBeWrittenOut = text;
              result.encodedMessageToBEWrittenOut = text;
              return afterEncoding(result);
            }
            result.rawMessageReadIn = text;
            result.encodedMessageReadIn = text;
          }
        }
      }
      afterEncoding(result);
    } else {}
  }

  /*
  *       R E A D    N D E F
  *
  */
  static BaseNDEFState read() {
    NtagI2CDemo.readNDEF();
    return ReadNdefState();
  }

  static BaseNDEFState readResponse(NDEFReadResponseEvent event) {
    bloC.D("bloC -> readResponse");
    bloC.onToastMessage('read success', false);
    final text = event.ndefOption.ndefText;
    final result = ReadNdefStateResponse(event.ndefOption, event.message);
    encodingAware(text, result);
    return result;
  }

  static BaseNDEFState readLost(NDEFReadLostEvent event) {
    bloC.onToastMessage(event.message, true);
    return ReadNDEFLostState(event.message);
  }

  static BaseNDEFState readProtected(NDEFReadProtectedEvent event) {
    bloC.onToastMessage(event.message, true);
    return ReadNDEFProtectedState(event.message);
  }

  static BaseNDEFState readError(NDEFReadErrorEvent event) {
    bloC.onToastMessage(event.message, true);
    return ReadNDEFErrorState(event.message);
  }

  static BaseNDEFState tapToRead(NDEFTapToReadEvent event) {
    bloC.onToastMessage(event.message, false);
    return NDEFTapToReadState(event.message);
  }

  /*
  *
  *    P R O D U C T     I N F O
  *
  */
  static BaseNDEFState detected(NDEFDetectedResponseEvent event) {
    bloC.onToastMessage(event.message, false);
    return NDEFDetectedState(event.message);
  }

  static BaseNDEFState readProductInfo() {
    return ReadProductInfoState();
  }

  static BaseNDEFState readProductInfoResponse(ProductInfoResponseEvent event) {
    final result = ProductInfoResponseState(event.productInfo);
    bloC.onToastMessage("productInfo read", false);
    return result;
  }

  /*
  *
  *     S E S S I O N    R E G I S T E R
  *
  */
  static BaseNDEFState readSessionRegister() {
    return RegisterSessionState();
  }

  static BaseNDEFState readSessionRegisterResponse(
      RegisterSessionResponseEvent event) {
    bloC.D("bloC -> readSessionRegisterResponse");
    bloC.onToastMessage('read success');
    final ndefmessage = event.register.NDEF_Message;
    final manufacture = event.register.Manufacture;
    final result = RegisterSessionResponseState(event.register);
    encodingAware(ndefmessage, result);
    return result;
  }

  /*
  *
  *      W R I T E    N D E F
  *
  */
  static BaseNDEFState write(NDEFWriteEvent event) {
    final data = event.rawMessageToBeWritten;
    final result = WriteNdefState(data);
    encodingAware(data, result);
    final dataToBeWritten = AppState.autoEncode
        ? result.encodedMessageToBEWrittenOut
        : result.rawMessageToBeWrittenOut;
    NtagI2CDemo.writeNDEF(dataToBeWritten);
    return result;
  }
  
  static BaseNDEFState writeResponse(NDEFWriteResponseEvent event){
      final result = WriteNDEFStateResponse(event.ndefOption, event.message);
      final text = event.ndefOption.ndefText;
      encodingAware(text, result);
      bloC.onToastMessage(result.message, false);
      return result;
  }
  
  static BaseNDEFState writeLost(NDEFWriteLostEvent event) {
    bloC.onToastMessage(event.message, true);
    return WriteNDEFLostState(event.message);
  }

  static BaseNDEFState writeProtected(NDEFWriteProtectedEvent event) {
    bloC.onToastMessage(event.message, true);
    return WriteNDEFProtectedState(event.message);
  }

  static BaseNDEFState writeError(NDEFWriteErrorEvent event) {
    bloC.onToastMessage(event.message, true);
    return WriteNDEFErrorState(event.message);
  }

  static BaseNDEFState tapTowrite(NDEFTapToWriteEvent event) {
    bloC.onToastMessage(event.message, false);
    return NDEFTapToWriteState(event.message);
  }

  static BaseNDEFState writeIncorrect(NDEFWriteIncorrectContentEvent event) {
    bloC.onToastMessage(event.message, true);
    return WriteNDEFIncorrectState(event.message);
  }
}

/*
*
*         B   L   O   C
*
* */

class ActivityBloCImp extends Bloc<BaseNDEFEvents, BaseNDEFState> {
  BaseNDEFState nextState;
  TLogWriter D;
  TShowToast ShowToast;

  ActivityBloCImp(this.D, this.ShowToast);

  @override
  BaseNDEFState get initialState => BaseNDEFState();

  /*
   [NOTE]
      new states tobe yielded must be a new instance other than
      currentState, hence following codes not working since it's
      not a new instance.
   --------------------------------------------------------------

      if (event == CounterEvents.update){
        new_state = currentState;
        new_state.update();
     }else if (event == CounterEvents.decrement){
        new_state = currentState;
        new_state.dec();
        new_state.update();
     }else if (event == CounterEvents.increment){
        new_state = currentState;
        new_state.inc();
        new_state.update();
     }
     yield new_state;
   */
  @override
  Stream<BaseNDEFState> mapEventToState(
      BaseNDEFState currentState, BaseNDEFEvents event) async* {
    BaseNDEFState new_state;
    D("onDispatch event: ${event.runtimeType.toString()}, msg: ${event?.message}");
    try{
      switch (event.event) {
        case ActivityEvents.onNDEFDetected:
          new_state = ActivityStates.detected(event as NDEFDetectedResponseEvent);
          break;
        case ActivityEvents.onReadRegisterSession:
          new_state = ActivityStates.readSessionRegister();
          break;
        case ActivityEvents.onRegisterSessionResponse:
          new_state = ActivityStates.readSessionRegisterResponse(
              event as RegisterSessionResponseEvent);
          break;
        case ActivityEvents.onReadProductInfo:
          new_state = ActivityStates.readProductInfo();
          break;
        case ActivityEvents.onProductInfoResponse:
          new_state = ActivityStates.readProductInfoResponse(
              event as ProductInfoResponseEvent);
          break;
      /*
      *      R E A D
      */
        case ActivityEvents.onNDEFRead:
          new_state = ActivityStates.read();
          break;
        case ActivityEvents.onNDEFReadResponse:
          new_state = ActivityStates.readResponse(event as NDEFReadResponseEvent);
          break;
        case ActivityEvents.onNDEFReadLost:
          new_state = ActivityStates.readLost(event as NDEFReadLostEvent);
          break;
        case ActivityEvents.onNDEFReadProtected:
          new_state =
              ActivityStates.readProtected(event as NDEFReadProtectedEvent);
          break;
        case ActivityEvents.onNDEFReadError:
          new_state = ActivityStates.readError(event as NDEFReadErrorEvent);
          break;
        case ActivityEvents.onNDEFTapToRead:
          new_state = ActivityStates.tapToRead(event as NDEFTapToReadEvent);
          break;
      /*
      *      W R I T E
      */
        case ActivityEvents.onNDEFWrite:
          new_state = ActivityStates.write(event as NDEFWriteEvent);
          break;
        case ActivityEvents.onNDEFWriteResponse:
          new_state =
              ActivityStates.writeResponse(event as NDEFWriteResponseEvent);
          break;
        case ActivityEvents.onDataRateUpdate:
          new_state = NdefDataRateUpdateState(event.message);
          break;
        case ActivityEvents.onNDEFWriteLost:
          new_state = ActivityStates.writeLost(event as NDEFWriteLostEvent);
          break;
        case ActivityEvents.onNDEFWriteProtected:
          new_state =
              ActivityStates.writeProtected(event as NDEFWriteProtectedEvent);
          break;
        case ActivityEvents.onNDEFWriteError:
          new_state = ActivityStates.writeError(event as NDEFWriteErrorEvent);
          break;
        case ActivityEvents.onNDEFTapToWrite:
          new_state = ActivityStates.tapTowrite(event as NDEFTapToWriteEvent);
          break;
        case ActivityEvents.onNDEFWriteIncorrectContent:
          new_state = ActivityStates.writeIncorrect(
              event as NDEFWriteIncorrectContentEvent);
          break;
      }
      yield new_state;
    }catch(e){
      throw Exception(
          "SendingBloCEvent: ${event.runtimeType.toString()} Error\n"
          "${StackTrace.fromString(e.toString())}"
      );
    }
    
  }

  void onNDEFDetected(String message) {
    dispatch(NDEFDetectedResponseEvent(message));
  }

  /*

  *   Session Register
  *
  */
  void onRegisterSessionResponse(Ntag_I2C_Registers register) {
    dispatch(RegisterSessionResponseEvent(register));
  }

  void onReadSessionRegister() {
    final event = ReadRegisterSessionEvent();
    dispatch(event);
  }

  /*

  *   NDEF read
  *
  */
  void onNDEFRead() {
    dispatch(NDEFReadEvent());
  }

  void onReadProductInfo(ProductInfo productInfo) {
    dispatch(ProductInfoResponseEvent(productInfo));
  }

  void onNDEFReadResponse(NdefFragmentOption ndefOption) {
    dispatch(NDEFReadResponseEvent(ndefOption));
  }

  void onNDEFReadLost(String message) {
    dispatch(NDEFReadLostEvent(message));
  }

  void onNDEFReadError(String message) {
    dispatch(NDEFReadErrorEvent(message));
  }

  void onNDEFReadProtected(String message) {
    dispatch(NDEFReadProtectedEvent(message));
  }

  void onNDEFTapToRead(String message) {
    dispatch(NDEFTapToReadEvent(message));
  }

  /*

  *   NDEF write
  *
  */
  void onNDEFWrite(String rawMessage) {
    dispatch(NDEFWriteEvent(rawMessage));
  }

  void onNDEFWriteLost(String message) {
    dispatch(NDEFWriteLostEvent(message));
  }

  void onNDEFWriteError(String message) {
    dispatch(NDEFWriteErrorEvent(message));
  }

  void onNDEFWriteResponse(String message) {
    dispatch(NDEFWriteResponseEvent(message));
  }

  void onNDEFWriteProtected(String message) {
    dispatch(NDEFWriteProtectedEvent(message));
  }

  void onNDEFWriteIncorrectContent(String message) {
    dispatch(NDEFWriteIncorrectContentEvent(message));
  }

  void onNDEFTapToWrite(String message) {
    dispatch(NDEFTapToWriteEvent(message));
  }

  void onDataRateUpdate(String datarate) {
    dispatch(DataRateUpdateEvent(datarate));
  }

  void onToastMessage(String message, [bool error = false]) {
    ShowToast(
        msg: message,
        toastLength: 1,
        gravity: 1,
        timeInSecForIos: 1,
        normalBgColor: Colors.blue.value,
        errBgColor: Colors.red.value,
        textColor: Colors.white.value,
        fontSize: 16.0,
        error: error);
  }
}

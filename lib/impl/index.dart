import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp_bloc/mediators/sketch/configs.dart';
import "package:common/src/common.log.dart";
import 'package:path/path.dart' as Path;

final D   = (m) => debugPrint(m.toString());
final log = Logger(name:"Activity", levels: LEVEL1, writer: D);


final TShowToast ShowToast = ({
   String msg, int toastLength, int gravity, int timeInSecForIos,
   int normalBgColor, int errBgColor, int textColor, double fontSize, bool error = false}){
   final tlen = toastLength == 1 ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG;
   final gindex = gravity == 1
         ? ToastGravity.BOTTOM
         : gravity == 2
            ? ToastGravity.CENTER
            : ToastGravity.TOP;

   Fluttertoast.showToast(
       msg: msg,
       toastLength: tlen,
       gravity: gindex,
       timeInSecForIos: 1,
       backgroundColor: error ? Color(errBgColor) : Color(normalBgColor),
       textColor: Color(textColor),
       fontSize: 16.0
   );
};


class Store implements StoreInf{
   static ConfigInf config = Injection.get<ConfigInf>();
   @override String path;
   @override String result;
   Store(this.path);

   String getPath([String pth]) => Path.join(config.app.directory, pth ?? path);

   @override Future<bool> existsAsync() {
      return File(getPath()).exists();
   }

   @override bool existsSync() {
      return File(getPath()).existsSync();
   }

   @override StoreInf open() {
      return this;
   }

   @override Future<String> readAsync() {
      final completer = Completer<String>();
      rootBundle.loadString(getPath()).then((r){
         result = r;
         completer.complete(r);
      });
      return completer.future;
   }

   @override String readSync() {
      throw Exception("method: `readSync` Not Implemented yet");
   }

   @override Future<StoreInf> writeAsync(String content) {
      final completer = Completer<StoreInf>();

      File(getPath()).writeAsString(content).then((r){
         completer.complete(this);
      });
      return completer.future;
   }

   @override void writeSync(String content) {
      return File(getPath()).writeAsStringSync(content);
   }

}












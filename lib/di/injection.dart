import 'dart:async';
import 'dart:io';
import "package:nxp/bloc/activity_bloc.dart";
import 'package:flutter_simple_dependency_injection/injector.dart';
import 'package:common/src/common.log.dart' show LoggerSketch;
import 'package:nxp_bloc/impl/services.dart';
import 'package:nxp_bloc/mediators/controllers/index.dart';
import 'package:nxp_bloc/mediators/controllers/patrol_bloc.dart';
import 'package:nxp_bloc/mediators/sketch/configs.dart';
import 'package:nxp_bloc/impl/config.dart';
import 'package:nxp/impl/index.dart';
import 'package:path/path.dart' as Path;

//@fmt:off
class Injection implements NxpInjector{
   @override ConfigInf configImpl;
   @override LoggerSketch logImpl;
   @override TLogWriter logWriterImpl;
   @override TShowToast toastImpl;
   @override StoreInf store;

   static Injector injector;
   static Future initInjection({String config_path = "config.yaml", String data, String app_dir}) async {
      final ij         = Injection()
         ..toastImpl   = ShowToast
         ..logWriterImpl= D
         ..logImpl     = log
         ..configImpl  = Config(config_path, data, app_dir);
      final config = ij.configImpl;
      final http = Http(
          dumplog: false,
          port   : 8888,
          host   : config.database.host as String,
      );
      injector = Injector.getInjector();
      injector.map<Injection>    ((i) => ij,                isSingleton: true);
      injector.map<ConfigInf>    ((i) => ij.configImpl,     isSingleton: true);
      injector.map<LoggerSketch> ((i) => ij.logImpl,        isSingleton: true);
      injector.map<TLogWriter>   ((i) => ij.logWriterImpl,  isSingleton: true);
      injector.map<TShowToast>   ((i) => ij.toastImpl,      isSingleton: true);

      final usermanager    = UserMainStateManager(config, (String path) => Store(path));
      final optionmanager  = DescOptMainStateManager(config, (String path) => Store(path));
      final devmanager     = DeviceMainStateManager (config, (String path) => Store(path));
      final patrolmanager  = PatrolMainStateManager(config, (String path) => Store(path));

      injector.map<ActivityBloCImp>((i) => ActivityBloCImp(
          ij.logWriterImpl, ij.toastImpl
      ), isSingleton: true);
      DescOptMainStateManager(config, (String path) => Store(path));
      injector.map<MessageBloC>((i) => MessageBloC(), isSingleton: true);
      injector.map<IJBloC>     ((i) => IJBloC(),      isSingleton: true);
      injector.map<IJOptBloC>  ((i) => IJOptBloC(optionmanager), isSingleton: true);
      injector.map<IJDevBloC>  ((i) => IJDevBloC(devmanager), isSingleton: true);
      injector.map<UserBloC>   ((i) => UserBloC(usermanager), isSingleton: true);
      injector.map<PatrolBloC> ((i) => PatrolBloC(patrolmanager), isSingleton: true);
   }

   static T get<T>(){
      return Injection.injector.get<T>();
   }

   Injection._(){

   }
   factory Injection(){
      Injection.injector = Injector.getInjector();
      return Injection._();
   }


}
//@fmt:on

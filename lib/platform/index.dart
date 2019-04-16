import "package:nxp/platform/activities.dart";

class Platform{
  static init(){
    RegisterSession.init();
    NDEFFragment.init();
    NtagI2CDemo.init();
  }
}

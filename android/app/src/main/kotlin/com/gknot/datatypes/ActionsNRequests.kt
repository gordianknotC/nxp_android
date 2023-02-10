package com.gknot.datatypes

enum class REQUEST_CODE(val id:Int) {
   AUTH_REQUEST(0),
   REQUEST_FILE_CHOOSER(1),
   readNDEF(2),
}
enum class READERS(val data:String){
   FlutterNtag_I2C_Demo("FlutterNtag_I2C_Demo"),
}
enum class ACTIONS(val data:String){
   // activities
   AuthActivity("AuthActivity"),
   DebugActivity("DebugActivity"),
   FlashMemoryActivity("FlashMemoryActivity"),
   HelpActivity("HelpActivity"),
   PseudoMainActivity("PseudoMainActivity"),
   MainActivity("MainActivity"),
   ReadMemoryActivity("ReadMemoryActivity"),
   RegisterConfigActivity("RegisterConfigActivity"),
   RegisterSessionActivity("RegisterSessionActivity"),
   ResetMemoryActivity("ResetMemoryActivity"),
   SplashActivity("SplashActivity"),
   VersionInfoActivity("VersionInfoActivity"),
   // Ntag_I2C_Demo
}
enum class FRAGMENTS(val data: String){
   ConfigFragment("ConfigFragment"),
   LedFragment("LedFragment"),
   NdefFragment("NdefFragment"),
   SpeedTestFragment("SpeedTestFragment")
}

class CompNamePseudo(val cls: Class<*>){
   companion object {
      fun unflattenFromString(string:String):CompNamePseudo{
         val cls = Class.forName(string);
         return CompNamePseudo(cls)
      }
   }
   val name:String
      get() = cls.name

   val className :String
     get() = cls.simpleName

}
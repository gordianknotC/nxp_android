
__2018 android nfc(npx/flutter) 專案, 待整理, 參考不建議使用__

分為 flutter 前端及 android 原生端開發，
原生端開發使用 NXP sdk 及 flutter 串接，這裡只放部份代碼 (android NXP sdk 端與 
flutter channel 溝通相關實作部份), 見 

- [MainActivity.kt](./android/app/src/main/kotlin/com/gknot/MainActivity.kt)
```kotlin
class MainActivity : FlutterActivity() {
    private fun registerPlugins(registry: PluginRegistry): Unit {
      SharedCodes.log("Main", "registerPlugins")
      SharedCodes.initApplicationDir(this)
      FlutterNtag_I2C_Demo.registerWith(registry.registrarFor(FlutterNtag_I2C_Demo.CHANNEL))
      //FlutterNtag_Get_Version.registerWith(registry.registrarFor(FlutterNtag_Get_Version.CHANNEL))
    
      ActivityFlutterMediator.registerWith(registry.registrarFor(ActivityFlutterMediator.CHANNEL))
      RegisterSessionActivity.registerWith(registry.registrarFor(RegisterSessionActivity.CHANNEL))
      AuthActivity.registerWith(registry.registrarFor(AuthActivity.CHANNEL))
      FlashMemoryActivity.registerWith(registry.registrarFor(FlashMemoryActivity.CHANNEL))
      ReadMemoryActivity.registerWith(registry.registrarFor(ReadMemoryActivity.CHANNEL))
      RegisterConfigActivity.registerWith(registry.registrarFor(RegisterConfigActivity.CHANNEL))
      ResetMemoryActivity.registerWith(registry.registrarFor(ResetMemoryActivity.CHANNEL))
      VersionInfoActivity.registerWith(registry.registrarFor(VersionInfoActivity.CHANNEL))
    
      SpeedTestFragment.registerWith(registry.registrarFor(SpeedTestFragment.CHANNEL))
      NdefFragment.registerWith(registry.registrarFor(NdefFragment.CHANNEL))
      ConfigFragment.registerWith(registry.registrarFor(ConfigFragment.CHANNEL))
      LedFragment.registerWith(registry.registrarFor(LedFragment.CHANNEL))
      PseudoMainActivity.registerWith(registry.registrarFor(PseudoMainActivity.CHANNEL))
    
      ActivityFlutterMediator.showMediators()
    }
}
```

- [channels.dart](./lib/platform/channels.dart)
- [activities.dart](./lib/platform/activities.dart)


# nxp

__目錄__ Table of Content

- [__App檔案設置及結構__](readme_filestructure.md) 
- [__APP語系編輯及UI__](readme_ui.md)
- [__資料庫設定__](readme_db.md)
- [__日誌後台__](readme_backend.md)

__Dart Packages__
 - nxp (android app)
 - nxp_bloc (部份UI 邏輯層,Business LogiC)
 - nxp_db (雲端資料庫)
 - PatrolParser (解析ntag位元) 


----------------------------------

#### Troubles Encountered During Tests
- MissingPluginException

    > Solution:
    >   implement MethodChannel in test


- Assets not available during tests
    > see https://github.com/flutter/flutter/issues/12999
    > Solution:
    >   to implement a setMockMessageHanndler for key "flutter/assets"

    ```dart
    await tester.runAsync(() async {
           BinaryMessages.setMockMessageHandler('flutter/assets', (message) {
              // The key is the asset key.
              String key = utf8.decode(message.buffer.asUint8List());
              // Manually load the file.
              var file = File('./${key}');
              print(Path.absolute(file.path));
              final Uint8List encoded = utf8.encoder.convert(file.readAsStringSync()) as Uint8List;
              return Future.value(encoded.buffer.asByteData());
           });
           // continue your test, now your widget should be able to load the asset
           await Main.main();
     });

    ```

- Installation Failed While path contains special characters like '#'
    > message:
    > Crash when compiling file:///E:/MyDocument/#%23Programing%23%23/Dart/flutter/nxp_nfcApp/nxp/test_driver/app_test.dart,
      at character offset null:
      Unsupported operation: Cannot extract a file path from a URI with a fragment component

    - it's an known issue that dart dosen't properly compile '#' and '?' character in uri path
        > Temporary Solution:
            rename your path

- Remote error "JSON-RPC error -32601 (method not found): Method not found"
    > Solution[App side]:
    > flutter run test_driver/app.dart --observatory-port 6666
       
    > Solution[hot reload Test Side]
    > dart test_driver/app.test
    > or
    > dart test_driver/app.test --enable-vm-service=http://127.0.0.1:6666
    

- While capture video via adb  shows  
  <span style="color:red">__ERROR: unable to configure video/avc codec at 1080x1920 (err=-1010)__</span>
  > __Solution__
  > 1) reboot phone
  > 2) reboot adb
  > 3) try ```batch
        adb.exe shell screenrecord --verbose /sdcard/kitkat.mp4```
    
        讀取訊息, 可能會出現成功的設置方 式, 如下所示   
        Main display is 1080x1920 @60.00fps (orientation=0)   
        Configuring recorder for 1080x1920 video/avc at 4.00Mbps   
        ERROR: unable to configure video/avc codec at 1080x1920 (err=-1010)   
        WARNING: failed at 1080x1920, retrying at 720x1280   
        Configuring recorder for 720x1280 video/avc at 4.00Mbps   
        Content area is 720x1280 at offset x=0 y=0   
        Time limit reached   
        Encoder stopping; recorded 2 frames in 3 seconds   
        Stopping encoder and muxer   
        Executing: /system/bin/am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/kitkat.mp4   
        Broadcasting: Intent { act=android.intent.action.MEDIA_SCANNER_SCAN_FILE dat=file: }   
        Broadcast completed: result=0   
    
    其顯示720x1280為成功的設定.


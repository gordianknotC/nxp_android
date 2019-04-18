# 📦 Packge: <span style="color: purple">nxp_db</span>
myPackages\nxp_db

## DB 設定
#### 1. nxp_db/config.yaml - <span style="color:orange">用於server</span>
#### 2. nxp_db/config.src.yaml - <span style="color:orange">用於測試環境</span>

![](.readme_db_images/config_yaml.jpeg)

- __database__
    - host: __localhost__
    - port: __5432__  
        視需要, 可變更PORT, 因DB主要使用POSTGRES database, 這裡的設定需要與
        postgres database 一致.
    - username: __dart__  
        登入db時的username, 需要與postgres database 設置一致
    - password: __dart__  
        登入db時的password, 需要與postgres database 設置一致
    - databaseName: __heroes__  
        databaseName, 需要與postgres database 設置一致
- assets   
    參照用, 實際上的設置, 設定於 nxp package
- app
    - history  
        無功用, 不可刪除
    - clientid  
        OAuth clientId, 需與 app(nxp package) 裡的 clientId 一致
        否則無法正確取得登入授權.
    - clientsecret  
        OAuth clientSecret, 需與 app(nxp package) 裡的 clientSecret 一致
        否則無法正確取得登入授權.
    - directory  
        無功用, 不可刪除
- web  
    參照用, 實際上的設置, 設定於 nxp package




## 如何運行DB

```bash
aqueduct serve
```
- 於nxp_db的資料夾下, Run `aqueduct serve` 
- For running within IDE environment, run `bin/main.dart`.   
  By default, a configuration file named `config.yaml` will be used.

> To generate a SwaggerUI client, run `aqueduct document client`.

> 以上需先行安裝dart sdk及 [aqueduct framework](https://pub.dev/packages/aqueduct)



## Entry Point
#### 1.nxp_db/lib/channel.dart
    為DB主要的entry point, 包含router 及初始data seed設置
- running on test environment  
  因測試時的設定與一般狀態使用不同的設定檔, 因此若要在test environment  
  下測試DB, 需要取消某部份程式碼
  -  ![](.readme_db_images/channel_test_code1.jpeg)  
     從 Injection 到 ImageModel.RESIZE 需要 block 掉, 上圖為server運行時的設置
  -  ![](.readme_db_images/channel_test_code2.png)
     router.route 整個 closure 需要 block 掉, 上圖為server運行時的設置
     
- running on server  
  如前項圖示
  
#### 2.nxp_db/bin/main.dart
    主要為以IDE環境下運行DB所設, 可忽略
    
    
    
## Running Application Tests

To run all tests for this application, run the following in this directory:

```
pub run test
```

The default configuration file used when testing is `config.src.yaml`. This file should be checked into version control. It also the template for configuration files used in deployment.

## Deploying an Application

See the documentation for [Deployment](https://aqueduct.io/docs/deploy/).



to generate migration schema

```bash
aqueduct db generate
```

to update schema into database
```bash
aqueduct db upgrade --connect postgres://dart:dart@localhost:5432/database
```



### trouble shooting
-  PostgreSQLSeverity.error 42P01: relation "_authclient" does not exist
   > triggered while adding auth client id in aqueduct cli
   ```batch
   aqueduct auth add-client --id nxp.app.test --secret nxp.app.client.secret --database-config database.yaml
   ```
   
   > solution:
   
   ```batch
   aqueduct db upgrade 
   --database-config database.yaml
   ```

- PostgreSQLSeverity.error 42601: syntax error at or near "null" 
  > 不支援LIST INT,   


#### Bad state: Future already complete
> Exceptions occurs while running aqueduct application
```batch
  **** Stacktrace
  * #0      _Completer.completeError (dart:async/future_impl.dart:21:31)
  * #1      ApplicationIsolateSupervisor._handleIsolateException (package:aqueduct/src/application/isolate_supervisor.dart:129:24)
  * #2      ApplicationIsolateSupervisor.listener (package:aqueduct/src/application/isolate_supervisor.dart:102:7)
  * #3      _RootZone.runUnaryGuarded (dart:async/zone.dart:1314:10)
  * #4      _BufferingStreamSubscription._sendData (dart:async/stream_impl.dart:336:11)
  * #5      _BufferingStreamSubscription._add (dart:async/stream_impl.dart:263:7)
  * #6      _SyncStreamController._sendData (dart:async/stream_controller.dart:764:19)
  * #7      _StreamController._add (dart:async/stream_controller.dart:640:7)
  * #8      _StreamController.add (dart:async/stream_controller.dart:586:5)
  * #9      _RawReceivePortImpl._handleMessage (dart:isolate/runtime/libisolate_patch.dart:171:12)

```

> solution

- caused by configuration members not implemented
  e.g. you have set a "port" value in config.yaml but not implemented it in your dart code
       or implemented in dart code but overlook in config

- caused by another dart thread running aqueduct on the same port
   - close all dart app in TaskManager would solve this problem.
     -- all PostSqlServer

- caused by some unknown reason , as following code demonstrates
```Dart
   //[X]
   @Operation.post('del') Future<Response>
   del(@Bind.path('del') int id, @Bind.body() List<int> ids) async {
      final q = Query<DescriptionOptions>(context)
         ..where((d) => d.id).oneOf(ids);
      final res = await q.delete();
      return Response.ok(res);
   }
   
   //[V]
   @Operation.post('del') Future<Response>
   del() async {
      final ids = List<int>.from(
         await request.body.decode() as List);
      
      final q = Query<DescriptionOptions>(context)
         ..where((d) => d.id).oneOf(ids);
      final res = await q.delete();
      return Response.ok(res);
   }
```


import 'package:flutter/material.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp/platform/index.dart';
import 'package:nxp/ui/flare/flare_example.dart';
import 'package:nxp/utils/PageNames.dart' as PageNames;
import 'package:nxp/utils/imports.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as Path;

void main() async {
  print("Flutter Main...");
  final app_dir = (await getApplicationDocumentsDirectory()).path;
  final config_path = Path.join(app_dir, 'assets');
  print('APP DIR: $app_dir');
  print('CFG DIR: $config_path');
  await Injection.initInjection(
      data: await rootBundle.loadString("assets/config.yaml"),
      app_dir: app_dir,
      config_path: config_path
  );
  Platform.init();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: Strings.appName,
      theme: ThemeData(
        fontFamily: Strings.fontRobotoRegular,
      ),
      debugShowCheckedModeBanner: false,
      home: WebLayoutTempA(title: Strings.appName),
      routes: routes(),
    );
  }

  ///returns the named routes
  Map<String, WidgetBuilder> routes() {
    return <String, WidgetBuilder>{
      Strings.appBarExampleRoute : (context) => WebLayoutTempA(title: PageNames.WEBLAYOUT_TEMP_A),

      /*'/${PageNames.WEBLAYOUT}'           : (context) => WebExamples(title: PageNames.WEBLAYOUT),
         '/${PageNames.WEBLAYOUT_CENTERING}' : (context) => WebLayoutColumnAlignA(title: PageNames.WEBLAYOUT_CENTERING),
         '/${PageNames.WEBLAYOUT_CENTERING_B}': (context) => WebLayoutColumnAlignB(title: PageNames.WEBLAYOUT_CENTERING_B),
         '/${PageNames.WEBLAYOUT_TEMP_A}'    : (context) => WebLayoutTempA(title: PageNames.WEBLAYOUT_TEMP_A),
         '/${PageNames.WEBLAYOUT_TEMP_B}'    : (context) => WebLayoutTempB(title: PageNames.WEBLAYOUT_TEMP_B),
         Strings.appBarExampleRoute: (context) => AppBarExample(),
         Strings.tabBarExampleRoute: (context) =>
            TabBarExample(
               title: Strings.TabBarTitle,
            ),
         Strings.navigationDrawerExampleRoute: (BuildContext context) =>
            NavigationDrawer(),
         Strings.bottomNavigationExampleRoute: (BuildContext context) =>
            BottomNavigation(title: Strings.bottomNavigationTitle),
         Strings.collapsibleToolbarExampleRoute: (BuildContext context) =>
            CollapsibleToolbar(title: Strings.appName),
         Strings.animatedIconsExampleRoute: (BuildContext context) =>
            AnimatedIconsDemo(title: Strings.animatedIconsTitle),
         Strings.animatedSizeExampleRoute: (BuildContext context) =>
            AnimatedSizeDemo(title: Strings.animatedSizeTitle),
         Strings.progressButtonExampleRoute: (BuildContext context) =>
            ProgressButton(title: Strings.progressButtonTitle),
         Strings.staggerDemoExampleRoute: (BuildContext context) =>
            StaggerDemo(title: Strings.staggerDemoTitle),
         Strings.stepperExampleRoute: (BuildContext context) =>
            StepperExample(title: Strings.stepperExampleTitle),
         Strings.hardwareKeyExampleRoute: (BuildContext context) =>
            RawKeyboardDemo(title: Strings.hardwareKeyExampleTitle),
         Strings.dragDropExampleRoute: (BuildContext context) =>
            DragAndDropExample(title: Strings.dragDropExampleTitle),
         Strings.animatedSwitcherExampleRoute: (BuildContext context) =>
            AnimatedSwitcherExample(title: Strings.animatedSwitcherExampleTitle),
         Strings.textExampleExampleRoute: (BuildContext context) =>
            TextExamples(title: Strings.textExampleExampleTitle),
         Strings.textSpanExampleRoute: (BuildContext context) =>
            TextSpanExample(title: Strings.textSpanExampleTitle),
         Strings.textUnderlineExampleRoute: (BuildContext context) =>
            TextUnderline(title: Strings.textUnderlineExampleTitle),
         Strings.aboutListTileExampleRoute: (BuildContext context) =>
            AboutListTileExample(title: Strings.aboutListTileExampleTitle),
         Strings.lifeCycleStateExampleRoute: (BuildContext context) =>
            Lifecycle(title: Strings.lifeCycleStateExampleTitle),
         Strings.localAuthExampleRoute: (BuildContext context) =>
            LocalAuthExample(title: Strings.localAuthTitle),
         Strings.rotatedBoxExampleRoute: (BuildContext context) =>
            RotatedBoxExample(Strings.rotatedBoxTitle),
         Strings.nestedListExampleRoute: (BuildContext context) =>
            NestedList(Strings.nestedListTitle),
         Strings.cupertinoTimerPickerRoute: (BuildContext context) =>
            CupertinoTimerPickerExample(Strings.cupertinoTimerPickerTitle),
         Strings.CupertinoActionSheetRoute: (BuildContext context) =>
            CupertinoActionSheetExample(Strings.CupertinoActionSheetTitle),
         Strings.CupertinoProgressIndicatorRoute: (BuildContext context) =>
            CupertinoProgressIndicatorExample(
               Strings.CupertinoProgressIndicatorTitle),
         Strings.GridPaperRoute: (BuildContext context) =>
            GridPaperExample(Strings.GridPaperTitle),
         Strings.ChipsExampleRoute: (BuildContext context) =>
            ChipsExample(Strings.ChipsExampleTitle),
         Strings.ExpansionTileRoute: (BuildContext context) =>
            ExpansionTileExample(Strings.ExpansionTileTitle),
         Strings.RotationTransitionRoute: (BuildContext context) =>
            RotationTransitionExample(Strings.RotationTransitionTitle),
         Strings.FlowWidgetExampleRoute: (BuildContext context) =>
            FlowWidgetExample(Strings.FlowWidgetExampleTitle),
         Strings.dismissibleExampleRoute: (BuildContext context) =>
            DismissibleExample(Strings.dismissibleExampleTitle),
         Strings.BackdropFilterExampleRoute: (BuildContext context) =>
            BackdropFilterExample(Strings.BackdropFilterExampleTitle),
         Strings.googleMapsExampleRoute: (BuildContext context) =>
            GoogleMapsExample(Strings.googleMapsExampleTitle),
         Strings.toolTipExampleRoute: (BuildContext context) =>
            ToolTipExample(Strings.toolTipExampleTitle),
         Strings.animatedCrossFadeExampleRoute: (BuildContext context) =>
            AnimatedCrossFadeExample(Strings.animatedCrossFadeExampleTitle),
         Strings.flareRoute: (BuildContext context) =>
            FlareExample(Strings.flareTitle),*/
    };
  }
}


// The list displayed by this app.
final List<CategoryName> names = <CategoryName>[
  CategoryName(Strings.aboutListTileExampleTitle),
  CategoryName(Strings.animatedCrossFadeExampleTitle),
  CategoryName(Strings.animatedIconsTitle),
  CategoryName(Strings.animatedSizeTitle),
  CategoryName(Strings.animatedSwitcherExampleTitle),
  CategoryName(Strings.appBarTitle),
  CategoryName(Strings.BackdropFilterExampleTitle),
  CategoryName(Strings.bottomNavigationTitle),
  CategoryName(Strings.ChipsExampleTitle),
  CategoryName(Strings.collapsibleToolbarTitle),
  CategoryName(Strings.CupertinoActionSheetTitle),
  CategoryName(Strings.CupertinoProgressIndicatorTitle),
  CategoryName(Strings.cupertinoTimerPickerTitle),
  CategoryName(Strings.dismissibleExampleTitle),
  CategoryName(Strings.dragDropExampleTitle),
  CategoryName(Strings.ExpansionTileTitle),
  CategoryName(Strings.flareTitle),
  CategoryName(Strings.FlowWidgetExampleTitle),
  CategoryName(Strings.googleMapsExampleTitle),
  CategoryName(Strings.GridPaperTitle),
  CategoryName(Strings.hardwareKeyExampleTitle),
  CategoryName(Strings.lifeCycleStateExampleTitle),
  CategoryName(Strings.localAuthTitle),
  CategoryName(Strings.navigationDrawerTitle),
  CategoryName(Strings.nestedListTitle),
  CategoryName(Strings.progressButtonTitle),
  CategoryName(Strings.rotatedBoxTitle),
  CategoryName(Strings.RotationTransitionTitle),
  CategoryName(Strings.staggerDemoTitle),
  CategoryName(Strings.stepperExampleTitle),
  CategoryName(Strings.TabBarTitle),
  CategoryName(Strings.textExampleExampleTitle),
  CategoryName(Strings.toolTipExampleTitle),

  CategoryName(PageNames.WEBLAYOUT),
];


/*
import 'package:flutter/material.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:nxp/bloc/activity_bloc.dart';
import 'package:nxp/bloc/activiy_states.dart';
import 'package:nxp/platform/activities.dart';
import "package:nxp/platform/index.dart";
import "package:nxp/di/injection.dart";
import 'package:flutter_bloc/flutter_bloc.dart';

void main() {
  print("Flutter Main...");
  Injection.initInjection();
  Platform.init();
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({Key key, this.title}) : super(key: key);
  final String title;

  @override
  _MyHomePageState createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  int _counter = 0;
  ActivityBloCImp _bloc = Injection.injector.get<ActivityBloCImp>();

  void _incrementCounter() {
    setState(() {
      _counter++;
      print(_counter);
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
      ),
      body: Center(child: tempContent()),
      floatingActionButton: FloatingActionButton(
        onPressed: _incrementCounter,
        tooltip: 'Increment',
        child: Icon(Icons.add),
      ), // This trailing comma makes auto-formatting nicer for build methods.
    );
  }

  Widget tempContent() {
    return BlocBuilder<BaseNDEFEvents, BaseNDEFState>(
        bloc: _bloc,
        builder: (context, BaseNDEFState state) {
          return Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text('state message: ${state.message}'),
              Text('Session Registers:'),
              Text(state.getRegister()),
              Text("product: ${state.product?.tempMap()}"),
              Text("rawNDEF   : ${state.rawMessageReadIn.trim()}"),
              Text("encodeNDEF: ${state.encodedMessageReadIn.trim()}"),
              Text("write NDEFMessage"),
              TextField(
                maxLength: 128,
                onChanged: (result) =>
                    setState((){
                      state.rawMessageToBeWrittenOut = result;
                      NDEFFragment.updateEditText(state.rawMessageToBeWrittenOut);
                    }),
                textAlign: TextAlign.center,
                maxLines: 4,
                style: TextStyle(color: Colors.blueGrey),
                controller:
                    TextEditingController(text: state.rawMessageToBeWrittenOut),
              ),
              FlatButton(
                child: Text('readNDEF'),
                onPressed: NtagI2CDemo.readNDEF,
                color: Colors.blue,
                textColor: Colors.white,
              ),
              FlatButton(
                child: Text('writeNDEF'),
                onPressed: () {
                  NtagI2CDemo.writeNDEF(state.rawMessageToBeWrittenOut);
                },
                color: Colors.blue,
                textColor: Colors.white,
              ),
              FlatButton(
                child: Text('readProductInfo'),
                onPressed: () async {
                  await NtagI2CDemo.getProduct();
                },
                color: Colors.blueGrey,
                textColor: Colors.white,
              )

            ],
          );
        });
  }
}
*/

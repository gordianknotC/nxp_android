import 'package:flutter/material.dart';
import 'package:nxp/utils/Panels.dart';


class WebLayoutColumnAlignB extends StatefulWidget {
   final String title;
   WebLayoutColumnAlignB({Key key, this.title}) : super(key: key);
   
   @override
   _WebLayoutCenteringBStateB createState() => _WebLayoutCenteringBStateB();
}

class _WebLayoutCenteringBStateB extends State<WebLayoutColumnAlignB> {
   @override
   Widget build(BuildContext context) {
      return Scaffold(
         appBar: AppBar(
            title: Text(widget.title),
            backgroundColor: Colors.black54,
         ),
         body: page(),
         backgroundColor: Colors.white10,
      );
   }
   
   Widget threeEltsRow() {
      return Row(
         children: [
            Container(
               color: Colors.orange,
               child: FlutterLogo(
                  size: 60.0,
               ),
            ),
            Container(
               color: Colors.blue,
               child: FlutterLogo(
                  size: 60.0,
               ),
            ),
            Container(
               color: Colors.purple,
               child: FlutterLogo(
                  size: 60.0,
               ),
            ),
         ],
      );
   }
   
   Widget page() {
      return Center(
         child: Container(
            child: Column(
               children: <Widget>[
                  content(),
                  centerController(),
                  Text('hello', style:TextStyle(color: Colors.amber)),
                  Text('Hello World', style:TextStyle(color: Colors.amber)),
                  Text('mainAxisAlignment: MainAxisAlignment.center',
                     style:TextStyle(color: Colors.red[900]))
               ],
               mainAxisAlignment: MainAxisAlignment.center,
            ),
            width: 320,
            height: 500,
            decoration: BoxDecoration(
               color: Colors.black54,
               border: Border.all(
                  color: Colors.amber,
                  width: 2.0,
               ),
            ),
         ),
      );
   }
   
   Widget content() {
      return Row(
         mainAxisAlignment: MainAxisAlignment.end,
         children: [
            Text('col1', style: TextStyle(color: Colors.amber)),
            Center(
               heightFactor: height_factor,
               widthFactor: width_factor,
               child: Container(
                  child: Text(
                     "this is a text box centered by Center Class with heightFactor of 1.4. Which means the corresponding dimension of this widget will be the product of the child's dimension and the size factor.",
                     style: TextStyle(color: Colors.white70),
                  ),
                  decoration: BoxDecoration(
                     color: Colors.white10,
                  ),
                  padding: EdgeInsets.all(10.0),
                  width: 240
               ),
            ),
         ]
      );
   }
   
   double height_factor = 1.4;
   double width_factor = 1;
   
   void onHeightFactorChanged(num value) => setState(() => height_factor = value.toDouble());
   
   void onWidthFactorChanged(num value) => setState(() => width_factor = value.toDouble());
   
   
   Widget centerController() {
      return SliderControllers([
         SliderController(color: Colors.white30, label: 'width', min: 1, max: 1.8, getter: () => width_factor, setter: onWidthFactorChanged),
         SliderController(color: Colors.white30, label: 'height', min: 1, max: 1.8, getter: () => height_factor, setter: onHeightFactorChanged),
      ]).toWidget();
   }
}


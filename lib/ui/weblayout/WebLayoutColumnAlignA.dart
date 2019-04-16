import 'package:flutter/material.dart';
import 'package:nxp/utils/Panels.dart';


class WebLayoutColumnAlignA extends StatefulWidget {
   final String title;
   
   WebLayoutColumnAlignA({Key key, this.title}) : super(key: key);
   
   @override
   _WebLayoutColumnAlignAState createState() => _WebLayoutColumnAlignAState();
}

class _WebLayoutColumnAlignAState extends State<WebLayoutColumnAlignA> {
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
   
   Widget page() {
      return Center(
         child: Container(
            child: Column(
               children: <Widget>[
                  content(),
                  centerController(),
                  Text('hello', style:TextStyle(color: Colors.amber)),
                  Text('Hello World', style:TextStyle(color: Colors.amber)),
                  Text('crossAxisAlignment: CrossAxisAlignment.end',
                     style:TextStyle(color: Colors.red[900]))
               ],
               crossAxisAlignment: CrossAxisAlignment.end,
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
   Widget content(){
      return Row(
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
   
   void onHeightFactorChanged(num value) => setState(()=> height_factor = value.toDouble());
   void onWidthFactorChanged(num value) => setState(()=> width_factor = value.toDouble());
  

   Widget centerController() {
      return SliderControllers([
         SliderController(color:Colors.white30,label: 'width', min: 1, max: 1.8, getter: () => width_factor, setter: onWidthFactorChanged),
         SliderController(color:Colors.white30,label: 'height', min: 1, max: 1.8, getter: () => height_factor, setter: onHeightFactorChanged),
      ]).toWidget();
   }
}


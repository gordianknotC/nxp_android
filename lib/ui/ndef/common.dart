
import 'package:flutter/material.dart';


mixin MixinWidget{
   /*
   *
   *
   *                       E L E M E N T S
   *
   *
   * */
   Widget hr() {
      return Container(
         margin: EdgeInsets.only(left: 10, right: 10, top: 25, bottom: 25),
         height: 1,
         color: Colors.black12,
      );
   }

   Widget hr2() {
      return Container(
         margin: EdgeInsets.only(left: 80, right: 80, top: 5, bottom: 5),
         height: 1,
         color: Colors.black12,
      );
   }

   Widget H3(String text) {
      return Container(
          child:
          Text(text, style: TextStyle(fontSize: 21, color: Colors.black87)),
          padding: EdgeInsets.all(25.0));
   }

   Widget H5(String text, [String subtext]) {
      return Container(
          child: subtext == null
              ? Text(text, style: TextStyle(fontSize: 16, color: Colors.black87))
              : Column(
             children: <Widget>[
                Text(text,
                    style: TextStyle(fontSize: 16, color: Colors.black87)),
                Text(subtext,
                    style: TextStyle(fontSize: 12, color: Colors.black54))
             ],
          ),
          padding: EdgeInsets.all(25.0));
   }
}

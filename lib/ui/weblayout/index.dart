import 'package:flutter/material.dart';
import 'package:nxp/utils/PageNames.dart' as PageNames;
export 'package:nxp/ui/weblayout/WebLayoutColumnAlignA.dart' show WebLayoutColumnAlignA;
export 'package:nxp/ui/weblayout/WebLayoutColumnAlignB.dart' show WebLayoutColumnAlignB;
export 'package:nxp/ui/weblayout/WebLayoutTempA.dart' show WebLayoutTempA;
export 'package:nxp/ui/weblayout/WebLayoutTempB.dart' show WebLayoutTempB;


class WebExamples extends StatefulWidget {
   final String title;
   
   WebExamples({Key key, this.title}) : super(key: key);
   
   @override
   _WebExamplesState createState() => _WebExamplesState();
}

class _WebExamplesState extends State<WebExamples> {
   @override
   Widget build(BuildContext context) {
      return Scaffold(
         appBar: AppBar(
            title: Text(widget.title),
         ),
         body: subLinks(),
         backgroundColor: Colors.blue[800],
      );
   }
   
   Widget subLinks() {
      return Center(
         child: Column(
            crossAxisAlignment: CrossAxisAlignment.center,
            children: <Widget>[
               textButtons(
                  'column cross align',
                  '/${PageNames.WEBLAYOUT_CENTERING}',
               ),
               textButtons(
                  'column main align',
                  '/${PageNames.WEBLAYOUT_CENTERING_B}',
               ),
               textButtons(
                  'tempA',
                  '/${PageNames.WEBLAYOUT_TEMP_A}',
               ),
               textButtons(
                  'tempB',
                  '/${PageNames.WEBLAYOUT_TEMP_B}',
               ),
            ],
         ),
      );
   }
   
   Widget textButtons(String text, String route) {
      return Padding(
         padding: const EdgeInsets.all(8.0),
         child: FlatButton(
            child: Text(text),
            shape: RoundedRectangleBorder(),
            color: Colors.white,
            textColor: Colors.blue,
            onPressed: () {
               Navigator.pushNamed(context, route);
            },
         ),
      );
   }
}

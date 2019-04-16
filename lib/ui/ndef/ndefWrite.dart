import 'package:PatrolParser/PatrolParser.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:nxp/bloc/activity_bloc.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp/platform/activities.dart';
import 'package:nxp_bloc/mediators/controllers/index.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:nxp/ui/ndef/common.dart';

class WriteNDEFWidget extends StatefulWidget {
   @override State<StatefulWidget> createState() => WriteNDEFState();
}

class WriteNDEFState extends State<WriteNDEFWidget> with MixinWidget {
   final ActivityBloCImp _bloc = Injection.injector.get<ActivityBloCImp>();

   Widget writeDecodeSwitch(BaseNDEFState state) {
      //fixme:
      var read_decoded = false;
      return Column(
         children: <Widget>[
            Column(
               children: <Widget>[
                  Container(
                      child: Text("EncodeMessage"),
                      padding: EdgeInsets.only(top: 30)),
                  Text("enable message decoder",
                      style: TextStyle(fontSize: 10, color: Colors.black26))
               ],
            ),
            Switch(
               value: AppSetting.autoEncode,
               onChanged: (result) => setState(() => AppSetting.autoEncode = result),
               materialTapTargetSize: MaterialTapTargetSize.padded,
               activeColor: Colors.white,
               activeTrackColor: Colors.blue,
            ),
         ],
      );
   }

   Widget originWrite(BaseNDEFState state) {
      return Container(
         padding: EdgeInsets.only(left: 15, right: 15),
         child: TextField(
            maxLength: 384,
            onChanged: (result){
               ActivityStates.encodingAware(result, state, true);
               NDEFFragment.updateEditText(result);
            },
            textAlign: TextAlign.center,
            maxLines: 6,
            style: TextStyle(color: Colors.blueGrey),
            controller: TextEditingController(text: state.rawMessageToBeWrittenOut),
         ),
      );
   }

   Widget encodedWrite(BaseNDEFState state) {
      final content = Column(children: <Widget>[
         Text('encoded message to be written out',
             textAlign: TextAlign.left,
             style: TextStyle(color: Colors.black26),
             maxLines: 5),
         Text(state.encodedMessageToBEWrittenOut,
             textAlign: TextAlign.left,
             style: TextStyle(color: Colors.blueGrey),
             maxLines: 5)
      ]);
      return Container(
         padding: EdgeInsets.all(5),
         child: content,
      );
   }

   @override
   Widget build(BuildContext context) {
      final ret = BlocBuilder<BaseNDEFEvents, BaseNDEFState>(
          bloc: _bloc,
          builder: (context, BaseNDEFState state) {
             return Column(
                mainAxisAlignment: MainAxisAlignment.start,
                children: <Widget>[
                   H3("Write NDEF Message"),
                   originWrite(state),
                   encodedWrite(state),
                   hr2(),
                   FlatButton(
                       child: Text("WriteNDEF", style: TextStyle(color: Colors.white)),
                       color: Colors.blue,
                       onPressed: () {
                          NtagI2CDemo.vibrate(20);
                          NtagI2CDemo.writeNDEF(state.rawMessageToBeWrittenOut);
                       }),
                   FlatButton(
                       child: Text("generate message", style: TextStyle(color: Colors.white)),
                       color: Colors.blue,
                       onPressed: () {
                          NtagI2CDemo.vibrate(20);
                          final text = PatrolRecord.generate().toString();
                          ActivityStates.encodingAware(text, state, true);
                          NDEFFragment.updateEditText(text);
                          setState((){
                          });
                       }),

                   writeDecodeSwitch(state),
                ],
             );
          });
      return ret;
   }
}

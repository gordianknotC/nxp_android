import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:nxp/bloc/activity_bloc.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp/platform/activities.dart';
import 'package:nxp_bloc/mediators/controllers/index.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:nxp/ui/ndef/common.dart';

class ReadNDEFWidget extends StatefulWidget {
  @override
  State<StatefulWidget> createState() => ReadNDEFState();
}

class ReadNDEFState extends State<ReadNDEFWidget> with MixinWidget {
  final ActivityBloCImp _bloc = Injection.injector.get<ActivityBloCImp>();

  Widget readDecodeSwitch(BaseNDEFState state) {
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

  Widget originRead(BaseNDEFState state) {
    final content = Column(children: <Widget>[
      Text('raw message',
          textAlign: TextAlign.left,
          style: TextStyle(color: Colors.black26),
          maxLines: 5),
      Text(state.rawMessageReadIn,
          textAlign: TextAlign.left,
          style: TextStyle(color: Colors.blueGrey),
          maxLines: 5)
    ]);
    return Container(
      padding: EdgeInsets.all(5),
      child: content,
    );
  }

  Widget encodedRead(BaseNDEFState state) {
    final content = Column(children: <Widget>[
      Text('encoded message',
          textAlign: TextAlign.left,
          style: TextStyle(color: Colors.black26),
          maxLines: 5),
      Text(state.encodedMessageReadIn,
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
              H3("Read NDEF Message"),
              originRead(state),
              hr2(),
              encodedRead(state),
              hr2(),
              FlatButton(
                  child:
                      Text("readNDEF", style: TextStyle(color: Colors.white)),
                  color: Colors.blue,
                  onPressed: () {
                    NtagI2CDemo.vibrate();
                    NtagI2CDemo.readNDEF();
                  }),
              FlatButton(
                  child: Text("readRegister",
                      style: TextStyle(color: Colors.white)),
                  color: Colors.blue,
                  onPressed: () {
                    NtagI2CDemo.vibrate();
                    NtagI2CDemo.readSessionRegisters();
                  }),
              readDecodeSwitch(state),
            ],
          );
        });
    return ret;
  }
}



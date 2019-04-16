import 'package:PatrolParser/PatrolParser.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:nxp/bloc/activity_bloc.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp/platform/activities.dart';
import 'package:nxp_bloc/mediators/controllers/index.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:nxp/ui/ndef/common.dart';

class SessionRegisterWidget extends StatefulWidget {
   @override State<StatefulWidget> createState() => SessionRegisterState();
}

class SessionRegisterState extends State<SessionRegisterWidget> with MixinWidget {
   final ActivityBloCImp _bloc = Injection.injector.get<ActivityBloCImp>();

   Widget registerList(BaseNDEFState state) {
      final infomap = state.register?.infoMap();
      List<Widget> list = infomap != null
         ? infomap.keys.map<ListTile>((String key) {
               return ListTile(
                  leading: Container(
                      child: Icon(Icons.description, size: 24),
                      padding: EdgeInsets.only(left: 12)),
                  title: Text(key, style: TextStyle(color: Colors.black54)),
                  subtitle: Text(
                      infomap[key].toString(),
                      style: TextStyle(color: Colors.blueGrey)),
                  contentPadding: EdgeInsets.fromLTRB(10.0, 0, 10.0, 0),
                  dense: true,
               );
            }).toList()
         : [];
      if (list.isNotEmpty){
         list = [H3("Session Registers:")] + list;
      }
      return Column(mainAxisAlignment: MainAxisAlignment.center, children: list);
   }

   @override
   Widget build(BuildContext context) {
      final ret = BlocBuilder<BaseNDEFEvents, BaseNDEFState>(
          bloc: _bloc,
          builder: (context, BaseNDEFState state) {
             return registerList(state);
          });
      return ret;
   }
}

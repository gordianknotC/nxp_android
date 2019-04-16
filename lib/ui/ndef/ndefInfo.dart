
import 'dart:async';
import 'dart:convert';
import 'package:flutter_picker/flutter_picker.dart';
import 'package:carousel_slider/carousel_slider.dart';
import 'package:PatrolParser/PatrolParser.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:nxp/bloc/activity_bloc.dart';
import 'package:nxp/di/injection.dart';
import 'package:nxp_bloc/mediators/controllers/index.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:nxp/ui/ndef/common.dart';
import 'package:nxp_bloc/mediators/controllers/patrol_bloc.dart';
import 'package:nxp_bloc/mediators/controllers/patrol_state.dart';
import 'package:nxp_bloc/mediators/models/image_model.dart';

final HOURS = List.generate(24, (i) => i);
final MINUTES = List.generate(60, (i) => i);


class NDEFInfoWidget extends StatefulWidget {
   @override
   State<StatefulWidget> createState() => NDEFInfoState();
}

class NDEFInfoState extends State<NDEFInfoWidget> with MixinWidget {
   final ActivityBloCImp _actbloc = Injection.injector.get<ActivityBloCImp>();
   final PatrolBloC      _tagbloc = Injection.injector.get<PatrolBloC>();
   final _formKey = GlobalKey<FormState>();

   BlocBuilder<BaseNDEFEvents, BaseNDEFState> carouselBuilder;
   BlocBuilder<PatrolEvents, BasePatrolState> tagBuilder;

   void Function(void Function()) currentDialogSetState;

   void onDialogFormSubmit() {
      _formKey.currentState.save();
      Navigator.of(context).pop();
   }

   String Function(String value) onDateValidate(int idx){
      return (String value){
         var v = int.tryParse(value);
         var e = [
            "Invalid format - hh(0~24)",
            "Invalid format - mm(0~59)",
            "Invalid format - ss(0-59)"
         ];
         if (v == null) return e[idx];
         if (idx == 0 && (v < 0 || v > 23))
            return e[idx];
         if (idx >= 1 && (v < 0 || v > 59))
            return e[idx];
      };
   }


   Future<T> openDialog<T>(String title, String subtitle, List<Widget> widget, BasePatrolState state, [Widget createBt]) {
      return showDialog(
         context: context,
         builder: (ctx) {
            return StatefulBuilder(
               builder: (ctx, _setState) {
                  currentDialogSetState = _setState;
                  final children = [
                     Form(
                         key: _formKey,
                         child: Column(
                             mainAxisAlignment: MainAxisAlignment.center,
                             children: widget)
                     ),
                     FlatButton(
                         child: Text("submit", style: TextStyle(color: Colors.white)),
                         color: Colors.blue,
                         onPressed: onDialogFormSubmit),
                  ];
                  if (createBt != null)
                     children.add(createBt);
                  return SimpleDialog(title: H5(title, subtitle), children: children);
               },
            );
         },
      );
   }

   Widget genDateField(String option_name, int option_value, BasePatrolState state){
      return SimpleDialogOption(
          child: TextFormField(
              initialValue: option_value.toString(),
              decoration: InputDecoration(labelText: option_name),
              onSaved: (String v){
                 state.model.setKeyVal(option_name, option_value);
              },
              validator: (String v){
                 final value = int.parse(v);
                  switch(option_name)   {
                     case "hh":
                        return value >= 0 && value <= 23 ? null : 'expect 0 ~ 23';
                     case "mm":
                        return value >=0 && value <= 59 ? null : 'expect 0 ~ 59';
                     case "ss":
                        return value >=0 && value <= 59 ? null : 'expect 0 ~ 59';
                  }
              },
          )
      );
   }

   Widget genRadioField(String option_name, int id, BasePatrolState state, void changed(int v)){
      return SimpleDialogOption(
          child: RadioListTile(
              value: id,
              title: Text(option_name),
              selected: state.model.setup[0]  == id,
              groupValue: state.model.setup[0] ,
              dense: true,
              onChanged: changed,
          )
      );
   }
   void openTimestampDialog(BasePatrolState state){
      final fields = [HOURS, MINUTES, MINUTES];
      return Picker(
          selecteds: [state.model.hh.first, state.model.mm.first, state.model.ss.first],
          adapter: PickerDataAdapter<String>(pickerdata: fields, isArray: true),
          hideHeader: true,
          title: Text("Select timestamp"),
          onConfirm: (Picker picker, List value) {
             final list = List<int>.from(picker.getSelectedValues());
             final time = state.model.date;
             state.model.patrol.hh[0] = list[0];
             state.model.patrol.mm[0] = list[1];
             state.model.patrol.ss[0] = list[2];
             _tagbloc.onEdit(EditPatrolEvent("date", time, state.model));
          },
      ).showDialog(context);

      /*final fields = <Widget>[
         genDateField("hh/hour", state.model.hh.last, state),
         genDateField("mm/minute", state.model.mm.last, state),
         genDateField("ss/second", state.model.ss.last, state),
      ];
      return openDialog<String>(
         "modify timestamp",
         "hh/mm/ss (hour/minute/seconds)",
          fields,
          state
      );*/
   }
   Future openSetupDialog (BasePatrolState state){
      final setup = PatrolSetup.values.toList();
      final fields = <Widget>[];
      for (var i = 0; i < setup.length; ++i) {
         var s = setup[i];
         fields.add(genRadioField(PatrolRecord.getSetupTextForLiteral(i),i, state, (int v){
            setState((){
               currentDialogSetState((){
                  print('select: $v');
                  state.model.patrol.setup[0] = v;
                  //_tagbloc.onEdit(EditPatrolEvent("setup", v, state.model));
               });
            });
         }));
      }
      return openDialog<int>(
         "maintainess setup",
         "value between 12 month to 15 days",
         fields, //options
         state
      );
   }

   Future openTypeDialog (BasePatrolState state){
      final device_names = (DevState.manager.get.initialData as List).map((d){
         return (d as Map<String,dynamic>)["name"] as String;
      }).toList();
      final fields = <Widget>[];
      for (var i = 0; i < device_names.length; ++i) {
         var name = device_names[i];
         fields.add(genRadioField(name,i, state, (int v){
            print("device: $v");
            currentDialogSetState((){
               state.model.patrol.type2[0] = v;
               _tagbloc.onEdit(EditPatrolEvent("type", v, state.model));
            });
         }));
      }
      return openDialog<int>(
          "machine type",
          "select existing type or create a new one",
          fields, //options
          state
      );
   }

   ListTile tile<T>(String title,String subtitle, T value, IconData data, BasePatrolState state) {
      void Function(BasePatrolState state) ontap = (_state) => print("onTap");
      switch (title) {
         case "setup":
            ontap = openSetupDialog;
            break;
         case "timestamp":
            ontap = openTimestampDialog;
            break;
         case "type":
            ontap = openTypeDialog;
            break;
      }
      return ListTile(
         leading: Container(
             child  : Icon(data, size: 24),
             padding: EdgeInsets.only(left: 12)),
         title   : Text(title),
         subtitle: Text(subtitle),
         onTap   : () => ontap(state),
      );
   }


   Widget ndefList(BaseNDEFState state) {
      final patrol = state.patrol;
      if (patrol == null) return Container();
      /*PatrolState.onUniquePatrol(patrol, (){
         _tagbloc.onAdd(AddPatrolEvent(PatrolRecordModel(patrol)));
      });*/
      //return renderInfoByPatrol(patrol);

      tagBuilder ??= BlocBuilder<PatrolEvents, BasePatrolState>(
         bloc: _tagbloc,
         builder: (context, BasePatrolState patrolState){
            print('receive BasePatrolState: $patrolState ${PatrolState.states.values.length}');
            if (patrolState is DefaultPatrolState)
               return Column();
            //return renderInfoByPatrol(patrolState.model.patrol);
            final patrolChildren = PatrolState.states.values.map((v) => renderInfoByPatrol(v.last)).toList();
            if (patrolChildren.isEmpty)
               return Column();
            else if (patrolChildren.length > 1)
               return patrolCarousel(patrolChildren, MediaQuery.of(context).orientation);
            else
               return patrolChildren.first;
         }
      );
      final event = AddPatrolEvent(PatrolRecordModel(patrol));
      _tagbloc.onAdd(event);
      return tagBuilder;
   }

   Widget renderInfoByPatrol(BasePatrolState state){
      final patrol = state.model.patrol;
      return Column(
         children: <Widget>[
            H3("List infograph of current encoded message"),
            Text(
               jsonEncode(patrol.toJson()),
               textAlign: TextAlign.center,
               overflow: TextOverflow.ellipsis,
               style: TextStyle(fontSize: 10.0),
               maxLines: 6,
            ),
            tile("id", patrol.getId().toString(),        patrol.id,           Icons.accessibility, state),
            tile("setup", patrol.getSetup().toString(),  patrol.setup.first,  Icons.timelapse, state),
            tile("timestamp", patrol.getTime(),          patrol.date,         Icons.access_time, state),
            tile("type", patrol.getType().toString(),    patrol.type2.first,  Icons.devices, state),
            tile("inject", patrol.getInject().toString(), patrol.inject2.first, Icons.settings, state),
            tile("patrol", patrol.getPatrol().toString(), patrol.patrol2.first, Icons.group, state),
            tile("temperature", patrol.getTemp().toString(),   patrol.temperature.first,  Icons.whatshot, state),
            tile("voltage", patrol.getVoltage().toString(),    patrol.voltage.first,      Icons.power, state),
            tile("pressure1", patrol.getPressure().toString(), patrol.pressure1.first,    Icons.network_check, state),
            tile("pressure2", patrol.getPressure().toString(), patrol.pressure2.first,    Icons.network_check, state),
            H3("Status"),
            tile("Capacity",   patrol.statusCapacity().toString(),  patrol.status1, Icons.hourglass_empty, state),
            tile("Battery",    patrol.statusBattery().toString(),   patrol.status2, Icons.battery_charging_full, state),
            tile("Temperature",patrol.statusTempurature().toString(),patrol.status3,Icons.hot_tub, state),
            tile("Pressure",   patrol.statusPressure().toString(),  patrol.status4, Icons.network_check, state),
            hr(),
         ],
      );
   }

   @override
   Widget build(BuildContext context) {
      return carouselBuilder ??= BlocBuilder<BaseNDEFEvents, BaseNDEFState>(
         bloc: _actbloc,
         builder: (context, BaseNDEFState state) {
            print('receive BaseNDEFSstate: $state');
            return ndefList(state);
         });
   }


   Widget patrolCarousel(List<Widget> children, Orientation orientation) {
      final basicSlider = CarouselSlider(
         items: children,
         autoPlay: false,
         enlargeCenterPage: false,
         viewportFraction: 0.9,
         height: orientation == Orientation.portrait ? 1350 : 1350

      );
      return Column(children: [
         basicSlider,
         Row(children: [
            Expanded(
               child: Padding(
                  padding: EdgeInsets.symmetric(horizontal: 15.0),
                  child: RaisedButton(
                     onPressed: () => basicSlider.previousPage(
                         duration: Duration(milliseconds: 300), curve: Curves.linear),
                     child: Text('prev slider'),
                  ),
               ),
            ),
            Expanded(
               child: Padding(
                  padding: EdgeInsets.symmetric(horizontal: 15.0),
                  child: RaisedButton(
                     onPressed: () => basicSlider.nextPage(
                         duration: Duration(milliseconds: 300), curve: Curves.linear),
                     child: Text('next slider'),
                  ),
               ),
            ),
         ]),
      ]);
   }

}

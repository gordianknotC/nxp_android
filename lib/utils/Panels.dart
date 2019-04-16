import 'package:flutter/material.dart';


class SliderController{
   String label;
   double min;
   double max;
   Color color;
   void Function(num v) setter;
   num Function() getter;
   String render_label (num v) => '$label ${(v * 10).round()/10}';
   
   SliderController({this.label, this.min, this.max, this.setter, this.getter, this.color = Colors.black});
}

class SliderControllers{
   List<SliderController> controllers;
   SliderControllers(this.controllers);
   Widget toWidget(){
      return
         Column(children:
         controllers.map((ctr) {
            var value = ctr.getter();
            return Row(
               children: [
                  Text(ctr.render_label(value), style: TextStyle(color: ctr.color)),
                  Slider(value: value.toDouble(), onChanged: ctr.setter, min: ctr.min, max: ctr.max)
               ],
               mainAxisAlignment: MainAxisAlignment.center,
               crossAxisAlignment: CrossAxisAlignment.center,
            );
         }).toList(),
            mainAxisAlignment: MainAxisAlignment.end,
            crossAxisAlignment: CrossAxisAlignment.stretch
         );
      
   }
}
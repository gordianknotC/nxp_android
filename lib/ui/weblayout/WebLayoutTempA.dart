import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:nxp/ui/ndef/ndefInfo.dart';
import 'package:nxp/ui/ndef/ndefRead.dart';
import 'package:nxp/ui/ndef/ndefWrite.dart';
import 'package:nxp/ui/ndef/sessionRegister.dart';
import 'package:nxp/utils/Panels.dart';
import 'package:PatrolParser/src/parser.dart';

class WebLayoutTempA extends StatefulWidget {
  final String title;

  WebLayoutTempA({Key key, this.title}) : super(key: key);

  @override WebLayoutCenteringBStateB createState() => WebLayoutCenteringBStateB();
}

class WebLayoutCenteringBStateB extends State<WebLayoutTempA> {
  final _formKey = GlobalKey<FormState>();
  String MessageRead = "please tap ndef tag on to be read...";
  String encodedMessageRead = "encoded...";
  String MessageWrite = "type message here to be write into tag...";
  String generatedOrigin = "generated message ....";
  bool read_decoded = false;
  bool write_decoded = true;
  PatrolRecord patrol = null;
  Map<String, dynamic> registers = {
    "Manufacture": "Lenovo",
    "Version": 1.02,
    "Turbo": true,
    "CPU": "intelUlimited"
  };
  int _page = 0;
  PageController _pageController;

  void onPageChanged(int page) {
    setState(() {
      this._page = page;
    });
  }

  void navigationTapped(int page) {
    _pageController.animateToPage(
      page,
      duration: Duration(milliseconds: 300),
      curve: Curves.easeIn,
    );
  }

  decodeNDEFMessage(String msg, {bool read, bool write}) {
    if (read) read_decoded = true;
    if (write) write_decoded = true;
  }

  encodeNDEFMessage(String msg, {bool read, bool write}) {
    if (read) read_decoded = false;
    if (write) write_decoded = false;
  }

  @override
  void initState() {
    super.initState();
    _pageController =   PageController();
  }

  @override
  void dispose() {
    super.dispose();
    _pageController.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.title),
        backgroundColor: Colors.black54,
      ),
      body: PageView(
        children: <Widget>[
          pageTest(),
          pageDemo(),
          pageJournal(),
          pageDemoAdvanced(),
        ],
        controller: _pageController,
        physics: BouncingScrollPhysics(),
        onPageChanged: onPageChanged,
      ),
      backgroundColor: Colors.white,
      bottomNavigationBar: BottomNavigationBar(
        //fixedColor: Colors.blue,
        items: [
          BottomNavigationBarItem(
            icon: Icon(Icons.tab, color: Colors.blueGrey,),
            title: Text("NFC", style: TextStyle(color: Colors.blueGrey)),
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.featured_play_list, color: Colors.blueGrey),
            title: Text("Demo", style: TextStyle(color: Colors.blueGrey)),
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.featured_play_list, color: Colors.blueGrey),
            title: Text("WJournal", style: TextStyle(color: Colors.blueGrey)),
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.account_circle, color: Colors.blueGrey),
            title: Text("Advanced", style: TextStyle(color: Colors.blueGrey)),
          ),
        ],
        onTap: navigationTapped,
        currentIndex: _page,
      ),
    );
  }


  /*
 *
 *           D E M O     G E N E R A T O R
 *
 *
 * */
  Widget pageJournal() {

  }
  /*
   *
   *           D E M O     G E N E R A T O R
   *
   *
   * */
  void Function() onWriteNDEF([cb()]) {
    return () {
      if (cb != null) cb();
    };
  }

  void Function() onReadNDEF([cb()]) {
    return () {
      if (cb != null) cb();
    };
  }

  onDialogFormSubmit() {
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

  Widget generatedList() {
    if (patrol == null) return Container();
  }

  onGeneratedRecordSubmit(String result) {
    // parse patroll into human readable
    patrol = PatrolRecord.fromByteString(result);
  }

  onPatrolGeneratorPressed() {
    setState(() => generatedOrigin = PatrolRecord.generate().toString());
    onGeneratedRecordSubmit(generatedOrigin);
  }

  Widget pageDemo() {
    return Center();
  }

  Widget pageDemoAdvanced() {}

  Widget pageTest() {
    return Center(
      child: Container(
        child: SingleChildScrollView(
            child: Column(
          mainAxisAlignment: MainAxisAlignment.start,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            content(),
          ],
        )),
      ),
    );
  }

  Widget content() {
    return Column(mainAxisAlignment: MainAxisAlignment.end, children: [
      registerList(),
      ndefInfoGraph(),
      hr(),
      readNDEF(),
      hr(),
      writeNDEF(),
      Container(
          padding: EdgeInsets.only(top: 20, bottom: 10),
          child: Text("nfc reader demo for nxp product",
              style: TextStyle(color: Colors.blueGrey)))
    ]);
  }

  Widget ndefInfoGraph(){
    return NDEFInfoWidget();
  }
  /*
   *                  R E G I S T E R    L I S T
   */
  Widget registerList() {
    return SessionRegisterWidget();
  }

  /*
   *           W R I T E    B U T T O N S
   */
  Widget writeNDEF() {
    return WriteNDEFWidget();
  }
  /*
   *           R E A D    B U T T O N S
   */
  Widget readNDEF() {
    return ReadNDEFWidget();
  }

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

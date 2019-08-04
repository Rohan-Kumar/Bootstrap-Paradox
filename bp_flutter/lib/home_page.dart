import 'dart:math';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {


  List<String> rooms = [
    'Boolywood panga',
    'Kannada ',
    'Tamil Rockers',
    'Mumbaikars '
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Home'),
      ),
      body: Container(
        child: ListView.builder(itemBuilder: (context, n){
          return _singleTile(rooms[n]);
        }, itemCount: rooms.length,),
      ),
      floatingActionButton: FloatingActionButton(onPressed: () async {
         const platform = const MethodChannel('myChannel');
         await platform.invokeMethod("ar");
      }, child: Icon(Icons.add),),
    );
  }

  Widget _singleTile(String room) {
    return Padding(
      padding: const EdgeInsets.all(8.0),
      child: Material(
        elevation: 4,
        borderRadius: BorderRadius.all(Radius.circular(8)),
        type: MaterialType.card,
        child: ListTile(
          onTap: () async {
            const platform = const MethodChannel('myChannel');
            await platform.invokeMethod("au");
          },
          title: Text(room, style: TextStyle(color: Colors.blue, fontWeight: FontWeight.bold),),
          subtitle: Text(room.split(" ")[0].toUpperCase()),
          trailing: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Icon(Icons.supervised_user_circle),
              Text('${Random().nextInt(50)+1}'),
            ],
          ),
        ),
      ),
    );
  }
}

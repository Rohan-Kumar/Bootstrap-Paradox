import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class HomePage extends StatefulWidget {
  @override
  _HomePageState createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {



  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Home'),
      ),
      body: Container(
        child: ListView.builder(itemBuilder: (context, n){
          return _singleTile();
        }, itemCount: 15,),
      ),
      floatingActionButton: FloatingActionButton(onPressed: () async {
         const platform = const MethodChannel('myChannel');
         await platform.invokeMethod("ar");
      }, child: Icon(Icons.add),),
    );
  }

  Widget _singleTile() {
    return ListTile(
      title: Text('Title'),
      subtitle: Text('Language selected'),
      trailing: Column(
        children: <Widget>[
          Icon(Icons.supervised_user_circle),
          Text('23'),
        ],
      ),
    );
  }
}

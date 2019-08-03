import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'home_page.dart';
import 'login_page.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: SplashScreen(),
    );
  }
}

class SplashScreen extends StatefulWidget {
  @override
  _SplashScreenState createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  bool _isAuthenticated = false;

  @override
  void initState() {
    super.initState();

    FirebaseAuth.instance.currentUser().then((user) {
      user != null
          ? setState(() {
              _isAuthenticated = true;
              print('uid ' + user.uid);
            })
          : null;
    });
    new Timer(Duration(seconds: 2), _checkAuth);
  }

  _checkAuth() {
    if (_isAuthenticated) {
      Navigator.of(context).push(MaterialPageRoute(builder: (context) {
        return HomePage();
      }));
    } else {
      Navigator.of(context).push(MaterialPageRoute(builder: (context) {
        return LoginPage();
      }));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        color: Colors.blueAccent,
        child: Center(
          child: Material(
            elevation: 4,
            child: Image.asset(
              'assets/logo.png',
              width: 100,
              height: 100,
            ),
          ),
        ),
      ),
    );
  }
}

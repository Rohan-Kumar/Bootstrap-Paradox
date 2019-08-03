import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:google_sign_in/google_sign_in.dart';

class LoginPage extends StatefulWidget {
  @override
  _LoginPageState createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('Login'),
      ),
      body: Container(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Container(
              width: 200,
              child: MaterialButton(
                color: Colors.blueAccent,
                onPressed: () {
                  signInWithGoogle().then((user) async {
                    await storeUserData(user);
                    Navigator.of(context).pop();
                  });
                },
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: <Widget>[
                    Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: Icon(
                        Icons.person,
                        color: Colors.white,
                      ),
                    ),
                    Text(
                      'Login',
                      style: TextStyle(color: Colors.white),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  static const String USERS_COLLECTION = 'users';
  final _googleSignIn = new GoogleSignIn();
  final _auth = FirebaseAuth.instance;
  final _fireStore = Firestore.instance;

  Future<FirebaseUser> signInWithGoogle() async {
    final GoogleSignInAccount googleUser = await _googleSignIn.signIn();
    final GoogleSignInAuthentication googleAuth =
        await googleUser.authentication;

    final AuthCredential credential = GoogleAuthProvider.getCredential(
      accessToken: googleAuth.accessToken,
      idToken: googleAuth.idToken,
    );

    return await _auth.signInWithCredential(credential);
  }

  Future<void> logout() {
    return _auth.signOut();
  }

  Future storeUserData(FirebaseUser user) {
    print('store data called for ${user.email}');
    return _fireStore
        .collection(USERS_COLLECTION)
        .document(user.uid)
        .setData({'uid': user.uid, 'email': user.email}, merge: true);
  }
}

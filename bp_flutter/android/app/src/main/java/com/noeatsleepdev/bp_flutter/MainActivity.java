package com.noeatsleepdev.bp_flutter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    private static final String CHANNEL = "myChannel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(this);

        new MethodChannel(getFlutterView(), CHANNEL).setMethodCallHandler(
                (call, result) -> {
                    Log.d("TAG", "onCreate: ");
                    if (call.method.equals("ar")) {
                        startActivity(new Intent(MainActivity.this, BroadcasterActivity.class));
                    }
                    if (call.method.equals("au")) {
                        startActivity(new Intent(MainActivity.this, AudienceActivity.class));
                    }
                });

    }


}

/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.noeatsleepdev.bp_flutter;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = Main2Activity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String LOG_TAG = "agora tag";

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    RtcEngine mRtcEngine;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid1, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "Join channel success" + uid1 + " " + uid);
                if (uid1 == uid) {
                    setupLocalVideo();
                }
            });
        }

        @Override
        public void onUserJoined(int uid1, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(() -> {
                Log.d(TAG, "Join channel success" + uid1 + " " + uid);
                if (uid1 != uid) {
                    Log.d(TAG, "onUserJoined: " + uid);
                    setupRemoteVideo(uid1);
                }
            });
        }

        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "First remote video decoded");
                    setupRemoteVideo(uid);
                }
            });
        }

        @Override
        public void onUserOffline(final int uid, int reason) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "User offline, uid:" + uid);
//                    onRemoteUserLeft();
                }
            });
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        Log.d(TAG, "onCreate: called");
        setContentView(R.layout.activity_main2);
        Log.d(TAG, "onCreate: success");
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ModelRenderable.builder()
                .setSource(this, Uri.parse("andy.sfb"))
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (andyRenderable == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);
                    andy.select();
                });

        initializeAgoraEngine();
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }

//    agora

    //    String token = "006a8b49b20db924e35aed61800807c5d7fIADoqSD+gQ0p5bKqtK+1C0/2iU5Zt4dfirmtAA8KmpZgtIT13LIAAAAAEAAZzUhZYhRHXQEAAQBgFEdd";
    String token = "006a8b49b20db924e35aed61800807c5d7fIADoqSD+gQ0p5bKqtK+1C0/2iU5Zt4dfirmtAA8KmpZgtIT13LIAAAAAEAAZzUhZYhRHXQEAAQBgFEdd";
    int uid = 4;

    private void initializeAgoraEngine() {
        try {

            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
        setVideoProfile();
        mRtcEngine.enableVideo();

        delayTest();

    }

    private void delayTest() {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start join channel");
                joinchannel(token, "demoChannel1", uid);

            }
        }, 5000);

    }

    void joinchannel(String token, String channel, int uid) {
        mRtcEngine.joinChannel("", channel, "", uid);
//        mRtcEngine.joinChannel(token, channel, "", uid);
    }

    void setVideoProfile() {
        VideoEncoderConfiguration.ORIENTATION_MODE
                orientationMode =
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT;

        VideoEncoderConfiguration.VideoDimensions dimensions = new VideoEncoderConfiguration.VideoDimensions(360, 640);

        VideoEncoderConfiguration videoEncoderConfiguration = new VideoEncoderConfiguration(dimensions, VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_24, VideoEncoderConfiguration.STANDARD_BITRATE, orientationMode);

        mRtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration);
    }

    private void setupLocalVideo() {
        FrameLayout container = (FrameLayout) findViewById(R.id.videoFragment);
        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));
    }

    private void setupRemoteVideo(int uid) {
        Log.d(TAG, "setupRemoteVideo: ");
        FrameLayout container = (FrameLayout) findViewById(R.id.videoFragment2);

        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

        surfaceView.setTag(uid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // For latest Agora sdk versions (2.4.1+),
        // it is required no more to call leave
        // channel before destroying the engine.
        // But it is recommended in most cases.

        leaveChannel();
        RtcEngine.destroy();
    }


    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }
}

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
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.VideoSource;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import android.media.MediaRecorder;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.mediaio.IVideoFrameConsumer;
import io.agora.rtc.mediaio.IVideoSource;
import io.agora.rtc.mediaio.MediaIO;
import io.agora.rtc.ss.Constant;
import io.agora.rtc.ss.ScreenShare;
import io.agora.rtc.video.AgoraVideoFrame;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;


public class BroadcasterActivity extends AppCompatActivity {
    private static final String TAG = BroadcasterActivity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String LOG_TAG = "agora tag";

    private MediaProjectionManager mediaProjectionManager;
    private static final int REQUEST_CODE_CAPTURE_PERM = 1234;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private ViewRenderable vRenderable;


    private RtcEngine mRtcEngine;
    private FrameLayout mFlCam;
    private FrameLayout mFlSS;
    private boolean mSS = false;
    private VideoEncoderConfiguration mVEC;
    private ScreenShare mSSInstance;
    String channelName = "demoChannel1";

    public static final int SCREEN_SHARE_UID = 2;
    public static final int AUDIENCE_UID = 3;

    private final ScreenShare.IStateListener mListener = new ScreenShare.IStateListener() {
        @Override
        public void onError(int error) {
            Log.e(LOG_TAG, "Screen share service error happened: " + error);
        }

        @Override
        public void onTokenWillExpire() {
            Log.d(LOG_TAG, "Screen share service token will expire");
            mSSInstance.renewToken(null); //Replace the token with your valid token
        }
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserOffline(int uid, int reason) {
            Log.d(LOG_TAG, "onUserOffline: " + uid + " reason: " + reason);
        }

        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            Log.d(LOG_TAG, "onJoinChannelSuccess: " + channel + " " + elapsed);


        }

        @Override
        public void onUserJoined(final int uid, int elapsed) {
            Log.d(LOG_TAG, "onUserJoined: " + (uid & 0xFFFFFFL));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if(uid == Constant.SCREEN_SHARE_UID) {
//                        setupRemoteView(uid);
//                    }
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

        mSSInstance = ScreenShare.getInstance();
        mSSInstance.setListener(mListener);

//        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
//        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_PERM);


        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        ViewRenderable.builder()
                .setView(this, R.layout.view_renderable)
                .build()
                .thenAccept(renderable -> vRenderable = renderable);

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
                    //TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    //andy.setParent(anchorNode);
                   // andy.setRenderable(andyRenderable);

                    //Node imgView = new Node();
                    TransformableNode imgView = new TransformableNode(arFragment.getTransformationSystem());
                    ViewRenderable.builder()
                            .setView(this, R.layout.view_renderable)
                            .build()
                            .thenAccept(viewRenderable -> {
                                vRenderable = viewRenderable;
                            });
                    imgView.setRenderable(vRenderable);
                    ViewRenderable rend = (ViewRenderable) imgView.getRenderable();

                    ImageView imageView = (ImageView) rend.getView();
                    imageView.setImageResource(item);


                    imgView.setParent(anchorNode);
                    imgView.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
                    imgView.select();
                });

        initAgoraEngineAndJoinChannel();

    }

    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();
        setupVideoProfile();
//        setupLocalVideo();

        delayTest();
    }

    private void delayTest() {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start join channel");
                joinChannel();

            }
        }, 5000);

    }

    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannel() {
        mRtcEngine.joinChannel("", channelName, "Extra Optional Data", SCREEN_SHARE_UID); // if you do not specify the uid, we will generate the uid for you

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start join channel");
                mSSInstance.start(getApplicationContext(), getResources().getString(R.string.agora_app_id), null,
                        channelName, SCREEN_SHARE_UID, mVEC);
            }
        }, 2000);



    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
        if (mSS) {
            mSSInstance.stop(getApplicationContext());
        }
    }

    private void setupVideoProfile() {
        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.enableVideo();
        mVEC = new VideoEncoderConfiguration(new VideoEncoderConfiguration.VideoDimensions(200, 300),
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_1,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT);
        mRtcEngine.setVideoEncoderConfiguration(mVEC);
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
    }

    private int item = 0;

    public void ImageButtonClicked(View view) {
        // ImageView imageView = (ImageView)findViewById(R.id.planetInfoCard);
        switch (view.getId()) {
            case R.id.button2:
                item = R.drawable.car;
                //     imageView.setBackgroundResource(R.drawable.car);
                break;
            case R.id.button3:
                item = R.drawable.bike;
                //   imageView.setBackgroundResource(R.drawable.haya);
                break;
            case R.id.button1:
                item = R.drawable.tree;
                //   imageView.setBackgroundResource(R.drawable.tree);
                break;
            case R.id.button4:
                item = R.drawable.fan;
                //     imageView.setBackgroundResource(R.drawable.car);
                break;
            case R.id.button5:
                item = R.drawable.king;
                //   imageView.setBackgroundResource(R.drawable.haya);
                break;
            case R.id.button6:
                item = R.drawable.doll;
                //   imageView.setBackgroundResource(R.drawable.tree);
                break;
            case R.id.button7:
                item = R.drawable.plus;
                //     imageView.setBackgroundResource(R.drawable.car);
                break;
            case R.id.button8:
                item = R.drawable.tom;
                //   imageView.setBackgroundResource(R.drawable.haya);
                break;

        }

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

    /*private void setupLocalVideo() {
        SurfaceView camV = RtcEngine.CreateRendererView(getApplicationContext());
        camV.setZOrderOnTop(true);
        camV.setZOrderMediaOverlay(true);
        mFlCam.addView(camV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mRtcEngine.setupLocalVideo(new VideoCanvas(camV, VideoCanvas.RENDER_MODE_FIT, 1));
        mRtcEngine.enableLocalVideo(false);
    }

    private void setupRemoteView(int uid) {
        SurfaceView ssV = RtcEngine.CreateRendererView(getApplicationContext());
        ssV.setZOrderOnTop(true);
        ssV.setZOrderMediaOverlay(true);
        mFlSS.addView(ssV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mRtcEngine.setupRemoteVideo(new VideoCanvas(ssV, VideoCanvas.RENDER_MODE_FIT, uid));
    }*/


//    agora

}

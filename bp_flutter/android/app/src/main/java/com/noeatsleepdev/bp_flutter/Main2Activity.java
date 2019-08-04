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
import io.agora.rtc.video.AgoraVideoFrame;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class Main2Activity extends AppCompatActivity {
    private static final String TAG = Main2Activity.class.getSimpleName();
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String LOG_TAG = "agora tag";

    private MediaProjectionManager mediaProjectionManager;
    private static final int REQUEST_CODE_CAPTURE_PERM = 1234;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;
    private ViewRenderable vRenderable;
    RtcEngine mRtcEngine;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid1, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "Join channel success" + uid1 + " " + uid);

                startRecording();

            });
        }

        @Override
        public void onUserJoined(int uid1, int elapsed) {
            super.onUserJoined(uid, elapsed);
            runOnUiThread(() -> {
                Log.d(TAG, "Join channel success" + uid1 + " " + uid);
                if (uid1 != uid) {
                    Log.d(TAG, "onUserJoined: " + uid);
                }
            });
        }


        @Override
        public void onFirstRemoteVideoDecoded(final int uid, int width, int height, int elapsed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "First remote video decoded");
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


        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_CAPTURE_PERM);


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
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(andyRenderable);

                    Node imgView = new Node();

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


                    imgView.setParent(andy);
                    imgView.setLocalScale(new Vector3(0.3f, 0.3f, 0.3f));
                    andy.select();
                });

        initializeAgoraEngine();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (REQUEST_CODE_CAPTURE_PERM == requestCode) {
            if (resultCode == RESULT_OK) {
                mMediaProjection = mediaProjectionManager.getMediaProjection(resultCode, intent);
                Log.d(TAG, "done");
//                startRecording(); // defined below
            } else {
                Log.d(TAG, "error");
                // user did not grant permissions
            }
        }
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
                item = R.drawable.haya;
                //   imageView.setBackgroundResource(R.drawable.haya);
                break;
            case R.id.button1:
                item = R.drawable.tree;
                //   imageView.setBackgroundResource(R.drawable.tree);
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

//        mRtcEngine.setExternalVideoSource(
//                true,
//                false,
//                true
//        );
        setVideoSource();

        delayTest();

    }

    IVideoFrameConsumer mConsumer;
    boolean mHasStarted;

    private void setVideoSource() {

        IVideoSource source = new IVideoSource() {
            @Override
            public boolean onInitialize(IVideoFrameConsumer consumer) {
                mConsumer = consumer;
                return true;
            }

            @Override
            public boolean onStart() {
                mHasStarted = true;
                return true;
            }

            @Override
            public void onStop() {
                mHasStarted = false;
            }

            @Override
            public void onDispose() {
                mConsumer = null;
            }

            @Override
            public int getBufferType() {
                return MediaIO.BufferType.BYTE_ARRAY.intValue();
            }
        };

        mRtcEngine.setVideoSource(source);

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

//        mRtcEngine.setVideoEncoderConfiguration(videoEncoderConfiguration);
        mRtcEngine.setVideoEncoderConfiguration(new VideoEncoderConfiguration(VideoEncoderConfiguration.VD_640x480,
                VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                VideoEncoderConfiguration.STANDARD_BITRATE,
                VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE));
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

    private boolean mMuxerStarted = false;
    private MediaProjection mMediaProjection;
    private Surface mInputSurface;
    private MediaMuxer mMuxer;
    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private int mTrackIndex = -1;

    private final Handler mDrainHandler = new Handler(Looper.getMainLooper());
    private Runnable mDrainEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    private void pushVideo(byte[] data) {
        Log.d(TAG, "pushVideo: called");

//        AgoraVideoFrame av = new AgoraVideoFrame();
//        av.format = AgoraVideoFrame.BUFFER_TYPE_ARRAY;
//        av.height = Resources.getSystem().getDisplayMetrics().heightPixels;
//        av.buf = data;
//        mRtcEngine.pushExternalVideoFrame(av);

        setVideoSource();
        if (mHasStarted && mConsumer != null) {
            mConsumer.consumeByteArrayFrame(data, MediaIO.PixelFormat.RGBA.intValue(), 300, 500, 0, new Date().getTime());
        }
    }


    private void startRecording() {
        DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display defaultDisplay = dm.getDisplay(Display.DEFAULT_DISPLAY);
        if (defaultDisplay == null) {
            throw new RuntimeException("No display found.");
        }
        prepareVideoEncoder();

        try {
            mMuxer = new MediaMuxer(Environment.getExternalStorageDirectory().toString() + "/video.mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException ioe) {
            throw new RuntimeException("MediaMuxer creation failed", ioe);
        }

        // Get the display size and density.
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int screenDensity = metrics.densityDpi;

        // Start the video input.
        mMediaProjection.createVirtualDisplay("Recording Display", screenWidth,
                screenHeight, screenDensity, 0 /* flags */, mInputSurface,
                null /* callback */, null /* handler */);

        // Start the encoders
        drainEncoder();
    }


    private static final String VIDEO_MIME_TYPE = "video/avc";
    private static final int VIDEO_WIDTH = Resources.getSystem().getDisplayMetrics().widthPixels;
    private static final int VIDEO_HEIGHT = Resources.getSystem().getDisplayMetrics().heightPixels;

    private void prepareVideoEncoder() {
        mVideoBufferInfo = new MediaCodec.BufferInfo();
        MediaFormat format = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, VIDEO_WIDTH, VIDEO_HEIGHT);
        int frameRate = 30; // 30 fps

        // Set some required properties. The media codec may fail if these aren't defined.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 6Mbps
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_CAPTURE_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / frameRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 1 seconds between I-frames

        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
        try {
            mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mInputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (IOException e) {
            releaseEncoders();
        }
    }

    private boolean drainEncoder() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        while (true) {
            int bufferIndex = mVideoEncoder.dequeueOutputBuffer(mVideoBufferInfo, 0);

            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // nothing available yet
                break;
            } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mTrackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }
//                mTrackIndex = mMuxer.addTrack(mVideoEncoder.getOutputFormat());
//                if (!mMuxerStarted && mTrackIndex >= 0) {
//                    mMuxer.start();
//                    mMuxerStarted = true;
//                }
            } else if (bufferIndex < 0) {
                // not sure what's going on, ignore it
            } else {
                ByteBuffer encodedData = mVideoEncoder.getOutputBuffer(bufferIndex);
                if (encodedData == null) {
                    throw new RuntimeException("couldn't fetch buffer at index " + bufferIndex);
                }

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    mVideoBufferInfo.size = 0;
                }

                if (mVideoBufferInfo.size != 0) {
//                    if (mMuxerStarted) {
                    encodedData.position(mVideoBufferInfo.offset);
                    encodedData.limit(mVideoBufferInfo.offset + mVideoBufferInfo.size);
//                        mMuxer.writeSampleData(mTrackIndex, encodedData, mVideoBufferInfo);
                    Log.d(TAG, String.valueOf(encodedData));
                    byte[] arr = new byte[encodedData.remaining()];
                    encodedData.get(arr);
                    pushVideo(arr);
//                        Log.d(TAG, String.valueOf(encodedData.array()));
//                    } else {
//                         muxer not started
//                    }
                }

                mVideoEncoder.releaseOutputBuffer(bufferIndex, false);

                if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }

        mDrainHandler.postDelayed(mDrainEncoderRunnable, 10);
        return false;
    }

    private void releaseEncoders() {
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        if (mMuxer != null) {
            if (mMuxerStarted) {
                mMuxer.stop();
            }
            mMuxer.release();
            mMuxer = null;
            mMuxerStarted = false;
        }
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        if (mInputSurface != null) {
            mInputSurface.release();
            mInputSurface = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        mVideoBufferInfo = null;
        mDrainEncoderRunnable = null;
        mTrackIndex = -1;
    }

}

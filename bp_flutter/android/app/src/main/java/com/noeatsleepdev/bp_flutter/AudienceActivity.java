package com.noeatsleepdev.bp_flutter;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class AudienceActivity extends AppCompatActivity {


    private static final String TAG = "AudienceActivity";
    private static final int uid = new Random().nextInt(50);
    private RtcEngine mRtcEngine;


    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid1, int elapsed) {
            runOnUiThread(() -> {
                Log.d(TAG, "Join channel success" + uid1 + " " + uid);

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
        setContentView(R.layout.activity_audience);
        initializeAgoraEngine();
    }


    private void setupRemoteVideo(int uid) {
        Log.d(TAG, "setupRemoteVideo: ");
        FrameLayout container = (FrameLayout) findViewById(R.id.audience);

        if (container.getChildCount() >= 1) {
            return;
        }

        SurfaceView surfaceView = RtcEngine.CreateRendererView(getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_ADAPTIVE, uid));

        surfaceView.setTag(uid);
    }

    private void initializeAgoraEngine() {
        try {

            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));

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
                joinchannel("", "demoChannel1", uid);

            }
        }, 5000);

    }

    void joinchannel(String token, String channel, int uid) {
        mRtcEngine.joinChannel(token, channel, "", uid);
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

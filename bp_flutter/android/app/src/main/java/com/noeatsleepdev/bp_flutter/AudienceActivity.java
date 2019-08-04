package com.noeatsleepdev.bp_flutter;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Random;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.video.VideoCanvas;
import io.agora.rtc.video.VideoEncoderConfiguration;

public class AudienceActivity extends AppCompatActivity {


    private static final String TAG = "AudienceActivity";
//    private static final int uid = new Random().nextInt(50);
    private RtcEngine mRtcEngine;
    String channelName = "demoChannel1";
    public static final int SCREEN_SHARE_UID = 2;
    public static final int AUDIENCE_UID = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audience);

        mFlSS = (FrameLayout) findViewById(R.id.audience);

        initEngineAndJoin();
    }


    private FrameLayout mFlSS;

    private void initEngineAndJoin() {
        try {
            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.agora_app_id), new IRtcEngineEventHandler() {

                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.d(TAG, "onJoinChannelSuccess: " + (uid & 0xFFFFFFL));
                }

                @Override
                public void onUserJoined(final int mUid, int elapsed) {
                    Log.d(TAG, "onUserJoined: " + (mUid & 0xFFFFFFL));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
//                            if(mUid == uid){
                                Log.d(TAG, "run: setting");
                                setupRemoteView(2);
//                            }else{
//                                Log.d(TAG, "run: something else");
//                            }
                        }
                    });
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
        mRtcEngine.disableAudio();
        mRtcEngine.enableVideo();
        mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);

        delayTest();

    }

    private void delayTest() {

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: start join channel");
                mRtcEngine.joinChannel("", channelName, "", 4);

            }
        }, 5000);

    }


    private void setupRemoteView(int mUid) {
        SurfaceView surfaceV = RtcEngine.CreateRendererView(getApplicationContext());
        surfaceV.setZOrderOnTop(true);
        surfaceV.setZOrderMediaOverlay(true);
        mFlSS.addView(surfaceV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//
//        if (mUid == 1) {
//            Log.d(TAG, "setupRemoteView: not doing");
////            mFlCam.addView(surfaceV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        } else if (uid == mUid) {
//            mFlSS.addView(surfaceV, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//        } else {
//            Log.e(TAG, "unknown uid");
//        }

        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceV, VideoCanvas.RENDER_MODE_FIT, mUid));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRtcEngine.leaveChannel();

        RtcEngine.destroy();
        mRtcEngine = null;
    }


}

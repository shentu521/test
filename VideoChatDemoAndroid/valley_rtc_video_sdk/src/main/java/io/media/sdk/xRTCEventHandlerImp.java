package io.media.sdk;

import android.nfc.Tag;
import android.os.Handler;
import android.os.Looper;

import io.media.sdk.xRTCEventHandler;
import io.media.sdk.xRTCEventHandler;
import io.media.sdk.xRTCMessage;

/**
 * Created by sunhui on 2017/9/7.
 */

public class xRTCEventHandlerImp extends xRTCEventHandler
{
    private final static String TAG = "xRTCEventHandlerImp" ;
    protected xRTCEventHandler mEventHandler;

    public xRTCEventHandlerImp(xRTCEventHandler eventHandler)
    {
        mEventHandler = eventHandler;
    }
    public void onFirstRemoteVideoDecoded(final long uid, final int width, final int height, final int elapsed) {

        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {

                if ( mEventHandler != null ) {
                    mEventHandler.onFirstRemoteVideoDecoded(uid, width, height, elapsed);
                }
            }
        });
    }

    public void onEnterMCU(int nCode )
    {
        xRTCLogging.i(TAG, "onEnterMCU "+nCode);
        if ( nCode == 0 )
        {
            (new Handler(Looper.getMainLooper())).post(new Runnable() {
                @Override
                public void run() {
                    xRTCEngine.mRtcEngine.startMediaRecord() ;
                    if ( mEventHandler!= null ) {
                        mEventHandler.onRemoteReady();
                    }
                }
            });
        }
    }

    public void onLeaveMCU(int nCode )
    {
        xRTCLogging.i(TAG, "onLeaveMCU "+nCode);

        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                xRTCEngine.mRtcEngine.stopMediaRecord() ;
                if ( mEventHandler!= null ) {
                    mEventHandler.onRemoteLogout();
                }
            }
        });
    }

    public void onFirstRemoteVideoFrame(final long uid, final int width, final int height, final int elapsed) {
        xRTCLogging.i(TAG, "onFirstRemoteVideoFrame "+uid);

        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onFirstRemoteVideoFrame(uid, width, height, elapsed);
                }
            }
        });
    }

    public void onFirstRemoteAudioFrame(final  long uid , final int elapsed )
    {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onFirstRemoteAudioFrame(uid, elapsed);
                }
            }
        });
    }
    public void onFirstLocalVideoFrame(final int width, final int height, final int elapsed) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if (mEventHandler != null ) {
                    mEventHandler.onFirstLocalVideoFrame(width, height, elapsed);
                }
            }
        });
    }

    public void onUserJoined(final long uid, final int elapsed) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onUserJoined(uid, elapsed);
                }
            }
        });
    }

    public void onUserOffline(final long uid, final int reason) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    xRTCEngine.mRtcEngine.removeRemoteCanvas(uid) ;
                    mEventHandler.onUserOffline(uid, reason);
                }

            }
        });
    }

    public void onUserMuteAudio(final long uid, final boolean muted) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onUserMuteAudio(uid, muted);
                }
            }
        });
    }

    public void onUserMuteVideo(final long uid, final boolean muted) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onUserMuteVideo(uid, muted);
                }
            }
        });
    }

    public void onRtcStats(final int stats) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onRtcStats(stats);
                }
            }
        });
    }

    public void onCreateChannel(final long channelid, final int token) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onCreateChannel(channelid, token);
                }
            }
        });
    }

    public void onLeaveChannel(final int stats) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onLeaveChannel(stats);
                }
            }
        });
    }

    public void onLastmileQuality(final int quality) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onLastmileQuality(quality);
                }
            }
        });
    }

    public void onError(final int err, final String strError) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onError(err, strError);
                }
            }
        });
    }

    public void onJoinChannelSuccess(final String channel, final long uid, final int elapsed) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onJoinChannelSuccess(channel, uid, elapsed);
                }
            }
        });
    }

    public void onRejoinChannelSuccess(final String channel, final long uid, final int elapsed) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onRejoinChannelSuccess(channel, uid, elapsed);
                }
            }
        });
    }

    public void onReadOffLineMessage(final xRTCMessage[] userMessageArray, final int nEnd) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onReadOffLineMessage(userMessageArray, nEnd);
                }
            }
        });
    }

    public void onReadMessage(final xRTCMessage userMessage) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onReadMessage(userMessage);
                }
            }
        });
    }

    public void onReadChannelMessage(final xRTCMessage userMessage) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onReadChannelMessage(userMessage);
                }
            }
        });
    }

    public void onSendMessage(final int nCode, final xRTCMessage userMessage) {
        (new Handler(Looper.getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                if ( mEventHandler != null ) {
                    mEventHandler.onSendMessage(nCode, userMessage);
                }
            }
        });
    }
    public void onRemoteReady() {}
    public void onRemoteLogout() {}
}

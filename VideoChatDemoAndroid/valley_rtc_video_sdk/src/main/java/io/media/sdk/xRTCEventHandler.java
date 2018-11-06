package io.media.sdk;

/**
 * Created by sunhui on 2017/8/30.
 */
public abstract class xRTCEventHandler
{
    public void onRemoteReady()
    {

    }

    public void onRemoteLogout()
    {

    }

    public  void onFirstRemoteVideoDecoded(long uid, int width, int height, int elapsed)
    {
    }

    public void onFirstRemoteVideoFrame(long uid, int width, int height, int elapsed)
    {

    }
    public  void onFirstLocalVideoFrame(int width, int height, int elapsed)
    {

    }

    public void onFirstRemoteAudioFrame(long uid ,int elapsed )
    {

    }

    public  void onUserJoined(long uid, int elapsed)
    {

    }

    public void onUserOffline(long uid, int elapsed)
    {

    }

    public void onUserMuteAudio(long uid, boolean muted )
    {

    }

    public void onUserMuteVideo(long uid, boolean muted)
    {

    }

    public void onRtcStats(int stats)
    {

    }

    public void onCreateChannel( long channelid, int token )
    {

    }

    public void onLeaveChannel(int stats)
    {

    }
    public void onLastmileQuality(int quality)
    {

    }

    public static final int RTC_ERR_SESSION_CRTL     = 1 ;    // 回话管理错误
    public static final int RTC_ERR_SESSION_CHANNEL  = 2 ;   // 频道管理错误
    public static final int RTC_ERR_MCU              = 3 ;   // 媒体服务器
    public static final int RTC_ERR_MEDIA_SYS        = 4 ;   // 音视频引擎
    public static final int SAFE_ERROR_MSG_SIZE      = 500 ; // 错误代码字符串长度

    public void onError(int err, String strError )
    {

    }

    public void onJoinChannelSuccess(String channel, long uid, int elapsed)
    {

    }

    public void onRejoinChannelSuccess(String channel, long uid, int elapsed)
    {

    }

    public void onReadOffLineMessage( xRTCMessage [] userMessageArray, int nEnd )
    {

    }

    public void onReadMessage( xRTCMessage userMessage )
    {

    }

    public void onReadChannelMessage( xRTCMessage userMessage )
    {

    }

    public void onSendMessage( int nCode, xRTCMessage userMessage )
    {

    }
}

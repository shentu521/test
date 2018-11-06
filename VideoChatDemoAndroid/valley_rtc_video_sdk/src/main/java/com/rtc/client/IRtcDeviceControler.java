package com.rtc.client;

/**
 * Created by shawn on 2017/12/19.
 */

public class IRtcDeviceControler {

    public final static int IID = 0x08;
    public final static int TRACKINDEX_ALL = -1;
    public final static int NotifyPlayAudioEnd          = 1001;   // 播放背景音乐结束，循环播放表示一次播放结束 object_number.getInt()获取播放结束的音轨号

    public final static int typeCtrlByHeadset = 0x01;  //受耳机插拔控制(插上耳机，无回声，那么高音质，否则讲话模式)
    public final static int typeAec = 0x04;    //回声抑制控制
    public final static int typeNs = 0x08;     //噪声抑制控制
    public final static int typeAgc = 0x010;   //自动增益控制
    public final static int typeVad = 0x020;   //静音检测控制
    public final static int typeEcho = 0x40;   //耳返控制

    public final static int typeMusicMode   = 0x1000; // 音乐房间, 默认就是音乐房间
    public final static int typeBackgroundMusic = 0x8000; // 支持背景音乐  PushBackgroudAudioFrame 有效

    private ValleyRtcNative mNative     = null;
    private long           mInst        = 0;

    public final static int stream_audio = 0x01;
    public final static int stream_video = 0x02;


    protected IRtcDeviceControler(ValleyRtcNative ntv, long ins){mNative = ntv; mInst=ins;}

    public int  Enable(int types, boolean enable){return mNative.JNI_DeviceEnable(mInst, types, enable);}
    public boolean IsEnabled(int type){return mNative.JNI_IsDeviceEnable(mInst, type);}
    /*trackIndex 0 ~ 4 最多支持5个音轨同时播放  volume 0.0 ~ 1.0f*/
    public int  SetBackgroudMusic(int trackIndex, String filepath, boolean loopflag, float volume, boolean bSendToNet, boolean bPlayout){return mNative.JNI_SetBackgroudMusic(mInst, trackIndex, filepath, loopflag, volume, bSendToNet, bPlayout);}
    /*trackIndex=-1, 设置所有音轨的音量*/
    public int  SetBackgroudMusicVolume(int trackIndex, float volume){return mNative.JNI_SetBackgroudMusicVolume(mInst, trackIndex, volume);}


    /*stream_audio*/
    public int   StartRtmp(String url, int sreamtypes, boolean bUseServer){return mNative.JNI_StartRtmp(mInst, url, sreamtypes, bUseServer);}
    public void  StopRtmp(){mNative.JNI_StopRtmp(mInst);}

    /*stream_audio*/
    public int   StartRecordEx(int sreamtypes){return mNative.JNI_StartRecordEx(mInst, sreamtypes);}
    public void  StopRecordEx(){mNative.JNI_StopRecordEx(mInst);}
    public void  PauseRecordEx(boolean bPause){mNative.JNI_PauseRecordEx(mInst, bPause);}

}

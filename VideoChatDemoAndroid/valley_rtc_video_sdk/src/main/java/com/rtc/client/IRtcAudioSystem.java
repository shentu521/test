package com.rtc.client;

import android.content.Context;
import android.media.AudioManager;


public class IRtcAudioSystem {
    public final static int IID = 0x04;

    private ValleyRtcNative mNative     = null;
    private long           mInst        = 0;

    protected        IRtcAudioSystem(ValleyRtcNative ntv, long ins){mNative = ntv; mInst=ins;}

    public void      SetPlayoutVolume(int volume){mNative.JNI_SetPlayoutVolume(mInst, volume);}
    public int       GetPlayoutVolume(){return mNative.JNI_GetPlayoutVolume(mInst);}
    public void      SetSpeakerphoneOn(int on) {mNative.JNI_SetSpeakerphoneOn(mInst, on);}
    public boolean  GetSpeakphoneOn(){return mNative.JNI_GetSpeakphoneOn(mInst);}
}

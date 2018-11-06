package io.media.sdk.xRTCAudio;

/**
 * Created by sunhui on 2018/2/16.
 */

public abstract interface xRTCAudioEventHandler {
    public abstract boolean onRecordInit(int nSampleRate, int nChannels) ;
    public abstract boolean onRecordFrame(byte[] ArrayOfByte ) ;
    public abstract boolean onRecordClean() ;
    public abstract boolean onPlaybackFrame(byte[] ArrayOfByte, int nSampleRate, int nChannels, long uUserID);
}


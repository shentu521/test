package io.media.sdk.xRTCAudio;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import java.nio.ByteBuffer;

import io.media.sdk.xRTCEngine;
import io.media.sdk.xRTCLogging;

import static android.media.audiofx.AudioEffect.SUCCESS;

/**
 * Created by sunhui on 2017/11/15.
 */

public class xRTCAudioRecord implements Runnable {

    private final String TAG = "xRTCAudioRecord" ;
    static final int        kWaitForTime = 2000 ;

    protected Thread                    mThread = null ;
    protected AudioRecord               mAudioRecord = null ;
    protected long                      mAudioRecordHandle ;
    volatile  protected boolean         mActive = false ;
    protected ByteBuffer                mByteBuffer = null ;
    private int                         mFrameSize ;
    private int                         mSampleRate ;
    private int                         mChannelConfig ;
    private int                         mMicSrcType ;
    private final int                   kErrorStartReocord   = 1;
    private final int                   kErrorReadRecord     = 2 ;
    private boolean                     mHardwareAECEnable = false ;
    private AcousticEchoCanceler        mAec = null ;

    public xRTCAudioRecord(long hAudioRecord)
    {
        mAudioRecordHandle = hAudioRecord ;
    }

    @TargetApi(17)
    public int EnableHardwareAEC(boolean bEnable)
    {
        if (  false == HardwareAECIsAvailable() )
        {
            return -1 ;
        }

        if ( mHardwareAECEnable == bEnable || mAudioRecord == null )
        {
            return -2 ;
        }

        if ( mAec == null && mAudioRecord != null ) {
            mAec = AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());
        }

        if ( mAec == null )
        {
            return -3 ;
        }

        mHardwareAECEnable = bEnable ;
        int rc = mAec.setEnabled( bEnable ) ;

        if ( bEnable == false )
        {
            mAec.release();
            mAec = null ;
        }

        if ( rc == SUCCESS ) {
            return  0 ;
        }

        return -4 ;
    }

    private boolean HardwareAECIsAvailable()
    {
        try
        {
            if ( Build.VERSION.SDK_INT >= 17 ) {
                return AcousticEchoCanceler.isAvailable() ;
            }
        }
        catch (ExceptionInInitializerError eii)
        {
            Log.e(TAG, " create AEC object fail er: ", eii);
        }
        catch (Exception e)
        {
            Log.e( TAG, " query AECEffect fail: Audio Echo Cancellation");
        }

        return false;
    }


    public int set( int nMicSrcType , int nSampleRate , int nChannels, int nFrameSize )
    {

        if ( mAudioRecord != null )
        {
            return -1 ;
        }

        mChannelConfig = AudioFormat.CHANNEL_IN_MONO ;
        if ( nChannels == 2 )
        {
            mChannelConfig = AudioFormat.CHANNEL_IN_STEREO ;
        }
        else
        {
            if ( nChannels > 2 || nChannels == 0  )
            {
                return -2 ;
            }
        }

        mSampleRate = nSampleRate ;
        mMicSrcType = nMicSrcType ;

        try {
            mMinSize = AudioRecord.getMinBufferSize(mSampleRate, mChannelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2;
        }
        catch (Exception ex)
        {
            xRTCLogging.e(TAG, "AudioRecord.getMinBufferSize get exception msg:"+ex.getMessage());
            mMinSize = 1280 ;
        }

        mFrameSize = nFrameSize ;
        mByteBuffer = ByteBuffer.allocateDirect( mFrameSize ) ;

        initAudioBuffer( mAudioRecordHandle, mByteBuffer ) ;

        mActive = false ;

        try {
            mAudioRecord = new AudioRecord(
                    mMicSrcType, // AudioManager.STREAM_VOICE_CALL,
                    mSampleRate,
                    mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mMinSize );
        }
        catch (Exception ex)
        {
            xRTCLogging.i(TAG, "new AudioRecord fail..."+ex.getMessage() );
            return -3 ;
        }

        xRTCLogging.i(TAG,"set frame:"+mFrameSize+" srctype:"+mMicSrcType+" mindevsize:"+mMinSize );

        return 0 ;
    }

    public int start()
    {
        if ( mAudioRecord == null )
        {
            return -1 ;
        }

        mThread = new Thread(this) ;
        mThread.setPriority( Thread.MAX_PRIORITY ) ;

        mActive = true ;
        mThread.start() ;

        return 0 ;
    }

    public int stop() {
        if ( mAudioRecord == null || mThread == null)
        {
            return -1 ;
        }

        mActive = false ;
        try {
            mThread.join( kWaitForTime ) ;
        }
        catch(InterruptedException ex)
        {
            xRTCLogging.e(TAG,"mThread.join exception");
        }

        EnableHardwareAEC( false ) ;

        mThread = null ;
        mByteBuffer = null ;
        return 0 ;
    }


    private final int kSleepTime = 200 ;
    private int mMinSize = 0 ;

    protected boolean CreateAudioRecord()
    {
        try {
            mAudioRecord = new AudioRecord(
                    mMicSrcType, //AudioManager.STREAM_VOICE_CALL,
                    mSampleRate,
                    mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mMinSize ) ;

            mAudioRecord.startRecording() ;
        }
        catch (Exception ex)
        {
            if ( mAudioRecord != null ) {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
            mAudioRecord = null ;
            xRTCLogging.e(TAG, "startRecord fail st:" + mAudioRecord.getRecordingState());
            return false ;
        }

        return true ;
    }

    @Override
    public void run()
    {
        int nRestartCount = 0 ;
        int nStartCount = 0 ;

        xRTCLogging.e(TAG, "thread init...");

        Process.setThreadPriority( Process.THREAD_PRIORITY_URGENT_AUDIO ) ;

        try {
            mAudioRecord.startRecording();
        }
        catch (Exception ex)
        {
            xRTCLogging.e(TAG,"mAudioRecord.startRecording fail...", ex);
            mAudioRecord.stop();
            mAudioRecord.release();
            mAudioRecord = null ;
        }

        if ( mAudioRecord == null )
        {
            boolean bRc = false ;

            for(int i = 0 ; i < 16 ; ++i )
            {
                bRc =  CreateAudioRecord() ;
                if ( bRc ==true )
                {
                    break ;
                }

                try
                {
                    Thread.sleep(200);
                }
                catch (Exception ex)
                {
                }
            }


            if ( !bRc )
            {
                errorRecordAudioBuffer( kErrorStartReocord,
                        "mAudioRecord.startRecording is not record fail");
                return ;
            }
        }

        try {
            while ( true )
            {
                if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                    ++nStartCount;

                    if (nStartCount > 8) {
                        xRTCLogging.e(TAG, "track record status fail st:" + mAudioRecord.getRecordingState());

                        errorRecordAudioBuffer( kErrorStartReocord,
                                "mAudioRecord.getRecordingState() is not record fail");
                        return;
                    } else {
                        Thread.sleep(200);
                    }
                } else {
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            xRTCLogging.e(TAG,"mAudioRecord.getRecordingState", ex);
        }


        try {
            while (mActive) {
                int rc = mAudioRecord.read(mByteBuffer, mFrameSize);
                if (rc == mFrameSize) {
                    setAudioBuffer(mAudioRecordHandle, mFrameSize);
                } else {

                    xRTCLogging.e(TAG, "read FAIL retry rc:" + rc);

                    ++nRestartCount;

                    mAudioRecord.stop();
                    mAudioRecord.release();
                    mAudioRecord = null;
                    Thread.sleep(20 );

                    if ( nRestartCount == 16 ) {
                        xRTCLogging.e(TAG, "read FAIL Count：" + nRestartCount + " then break");
                        errorRecordAudioBuffer(kErrorReadRecord, "mAudioRecord.read fail size:" + rc);
                        break;
                    }

                    mAudioRecord = new AudioRecord(
                                        mMicSrcType, //AudioManager.STREAM_VOICE_CALL,
                                        mSampleRate,
                                        mChannelConfig,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                        mMinSize ) ;

                    try {
                        mAudioRecord.startRecording();
                    } catch (Exception ex) {
                        xRTCLogging.e(TAG, "read FAIL Count：" + nRestartCount + " then break");
                        errorRecordAudioBuffer(kErrorReadRecord, "mAudioRecord.read fail size:" + rc);
                        break;
                    }

                }

                mByteBuffer.rewind();
            }
        }
        catch (Exception ex)
        {
            xRTCLogging.e(TAG, "audio record thread run exception msg"+ex.getMessage() );
        }

        if ( mAudioRecord != null )
        {
            try {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
            catch (Exception ex)
            {
                xRTCLogging.e(TAG, "audiorecord stop fail msg:"+ex.getMessage(), ex.fillInStackTrace());
            }
            mAudioRecord = null ;
        }

        xRTCLogging.e(TAG, "thread clean...");

    }

    native static int initAudioBuffer( long hAudioRecord, ByteBuffer buffer ) ;
    native static int setAudioBuffer( long hAudioRecord, int nPlaySize ) ;
    native static void errorRecordAudioBuffer( int nErrorCode,String errorstring ) ;
}

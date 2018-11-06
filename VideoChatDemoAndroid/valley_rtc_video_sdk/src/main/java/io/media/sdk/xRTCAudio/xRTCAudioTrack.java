package io.media.sdk.xRTCAudio;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import java.nio.ByteBuffer;

import io.media.sdk.xRTCLogging;
import android.os.Process;

/**
 * Created by sunhui on 2017/11/15.
 */

public class xRTCAudioTrack implements Runnable {

    static final String TAG="xRTCAudioTrack";
    protected AudioTrack    mAudioTrack = null ;
    private ByteBuffer      mByteBuffer = null ;
    protected Thread        mThread = null ;
    private int             mSampleRate ;
    private int             mChannelConfig ;
    private long            mAudioTrackHandle ;
    static final int        kWaitForTime = 2000 ;
    private int             mFrameSize ;
    private int             mMinDevBufSize =  0;
    volatile  boolean       mActive = false ;

    // 错误代码
    private final int      kErrorStartPlay      = 1;
    private final int      kErrorWritePlay      = 2 ;
    private static final int BITS_PER_SAMPLE = 16;

    // Requested size of each recorded buffer provided to the client.
    private static final int CALLBACK_BUFFER_SIZE_MS = 10;

    // Average number of callbacks per second.
    private static final int BUFFERS_PER_SECOND = 1000 / CALLBACK_BUFFER_SIZE_MS;

    public xRTCAudioTrack(long hAudioTrack)
    {
        mAudioTrackHandle = hAudioTrack ;
    }

    public int set( int nStreamType, int nSampleRate , int nChannels, int nFrameSize )
    {

        if ( mAudioTrack != null )
        {
            return -1 ;
        }

        mChannelConfig = AudioFormat.CHANNEL_OUT_MONO ;
        if ( nChannels == 2 )
        {
            mChannelConfig = AudioFormat.CHANNEL_OUT_STEREO ;
        }
        else
        {
            if ( nChannels > 2 || nChannels == 0  )
            {
                return -2 ;
            }
        }

        mSampleRate = nSampleRate ;

        /*
        final int bytesPerFrame = nChannels * (BITS_PER_SAMPLE / 8);

        mFrameSize =  bytesPerFrame * ( nSampleRate / BUFFERS_PER_SECOND ) ;
        */

        try {
            mMinDevBufSize = AudioTrack.getMinBufferSize(
                    nSampleRate,
                    mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT) * 2;
        }
        catch (Exception ex)
        {
            xRTCLogging.e(TAG, "AudioTrack.getMinBufferSize exception msg:"+ ex.getMessage());
            mMinDevBufSize = 1280 ;
        }

        mFrameSize = nFrameSize ;
        mByteBuffer = ByteBuffer.allocateDirect( mFrameSize ) ;

        initAudioBuffer( mAudioTrackHandle, mByteBuffer ) ;

        mActive = false ;

        try {

            // AudioManager.STREAM_VOICE_CALL,
            mAudioTrack = new AudioTrack(
                    nStreamType, //AudioManager.STREAM_VOICE_CALL,
                    mSampleRate,
                    mChannelConfig,
                    AudioFormat.ENCODING_PCM_16BIT,
                    mMinDevBufSize,
                    AudioTrack.MODE_STREAM);

            xRTCLogging.i( TAG, "audiotrack sttype:"+nStreamType+" minbufsize:"+mMinDevBufSize ) ;

        }
        catch (Exception ex)
        {
            xRTCLogging.e( TAG, "audiotrack sttype:"+nStreamType+
                    " minbufsize:"+mMinDevBufSize+" fail msg:"+ex.getMessage() ) ;

            mAudioTrack = null ;
            return -3 ;
        }


        return 0 ;

    }

    public int start()
    {
        if ( mAudioTrack == null )
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
        if ( mAudioTrack == null || mThread == null)
        {
            if ( mAudioTrack != null )
            {
                mAudioTrack.release();
                mAudioTrack = null ;
            }

            if ( mByteBuffer != null )
            {
                mByteBuffer = null ;
            }

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

        mThread = null ;
        mByteBuffer = null ;
        return 0 ;
    }

    @Override
    public void run()
    {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

        int nWriteSize ;
        int nRestartCount = 0 ;

        xRTCLogging.i( TAG, "play thread init...") ;

        try {
            mAudioTrack.play();

            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                xRTCLogging.e(TAG, "track play status fail...");
                errorPlayAudioBuffer(kErrorStartPlay, "mAudioRecord.getRecordingState() is not record fail");
                return;
            }
        }catch (Exception ex)
        {
            xRTCLogging.e( TAG, "audio track play except msg:"+ex.getMessage() ) ;
            mAudioTrack.release();
            mAudioTrack = null ;
            return ;
        }

        try
        {
            while ( mActive )
            {
                int rc = getAudioBuffer( mAudioTrackHandle , mFrameSize ) ;
                if ( rc != 0 )
                {
                    break ;
                }

                nWriteSize = mAudioTrack.write( mByteBuffer.array(), mByteBuffer.arrayOffset(), mFrameSize ) ;

                mByteBuffer.rewind() ;

                if ( nWriteSize  < mFrameSize )
                {
                    ++nRestartCount ;

                    xRTCLogging.e(TAG, "mAudioTrack.write fail wsize:"+nWriteSize+
                            " fsize:"+mFrameSize+" errcount:"+nRestartCount );

                    if ( nRestartCount >= 16 )
                    {
                        errorPlayAudioBuffer( kErrorWritePlay, "mAudioTrack.write fail rc:"+nWriteSize) ;
                        break ;
                    }

                    mAudioTrack.stop();
                    mAudioTrack.release();
                    mAudioTrack = null ;

                    mAudioTrack = new AudioTrack(
                            AudioManager.STREAM_VOICE_CALL,
                            mSampleRate,
                            mChannelConfig,
                            AudioFormat.ENCODING_PCM_16BIT,
                            mMinDevBufSize,
                            AudioTrack.MODE_STREAM ) ;

                    mAudioTrack.play();
                }

            }

        }
        catch (Exception ex)
        {
            xRTCLogging.e( TAG, "audio track exception msg:"+ex.getMessage() );
        }

        if ( mAudioTrack != null )
        {
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null ;
        }

        xRTCLogging.i(TAG, "thread exit...");
    }

    @TargetApi(21)
    private int writeOnLollipop() {
        return mAudioTrack.write( mByteBuffer, mFrameSize, AudioTrack.WRITE_BLOCKING );
    }

    private int writePreLollipop() {
        return mAudioTrack.write(mByteBuffer.array(), mByteBuffer.arrayOffset(), mFrameSize ) ;
    }

    native static int initAudioBuffer( long hAudioTrack, ByteBuffer buffer ) ;
    native static int getAudioBuffer( long hAudioTrack, int nPlaySize ) ;
    native static void errorPlayAudioBuffer( int nErrorCode, String errorstring ) ;
};

package io.media.sdk.xRTCAudio;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.os.Build;
import android.system.Os;

import io.media.sdk.xRTCLogging;

/**
 * Created by sunhui on 2017/9/5.
 */

public class xRTCAudioManager {

    protected static final String TAG ="xRTCAudioManager";

    public static final int SPEAKER_TYPE_EARPHONE = 0 ;
    public static final int SPEAKER_TYPE_SPEAKER = 1 ;
    public static final int SPEAKER_TYPE_HEADSET = 2 ;
    public static final int SPEAKER_TYPE_BLUETOOTH = 3 ;

    private static final int DEFAULT_SAMPLING_RATE = 44100;
    private static final int DEFAULT_FRAMES_PER_BUFFER = 256;
    private static final int AUDIO_GET_SIZE_SAMPLING_RATE = 8000 ;

    private int mNativeOutputSampleRate;
    private boolean mAudioLowLatencySupported;
    private int mAudioLowLatencyOutputFrameSize;
    private int mRecordBufSize ;
    private int mPlayBufSize ;
    protected boolean mUseHardwareAEC = false ;

    private int mSysBufSize ;
    public static AudioManager mAudioManager = null ;
    private Context mContext ;
    private xRTCAudioRouteReceiver mRouteReceiver = null ;


    private AcousticEchoCanceler mAec = null ;


    public xRTCAudioManager(Context context)
    {
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE) ;
        mContext = context ;
        GetSysConfig(context) ;
    }

    protected void GetSysConfig(Context context)
    {
        mNativeOutputSampleRate = DEFAULT_SAMPLING_RATE;
        mAudioLowLatencyOutputFrameSize = DEFAULT_FRAMES_PER_BUFFER;
        if (Build.VERSION.SDK_INT >= 17)
        {
            String sampleRateString = mAudioManager.getProperty("android.media.property.OUTPUT_SAMPLE_RATE");
            if (sampleRateString != null) {
                this.mNativeOutputSampleRate = Integer.parseInt(sampleRateString);
            }
            String framesPerBuffer = mAudioManager.getProperty("android.media.property.OUTPUT_FRAMES_PER_BUFFER");
            if (framesPerBuffer != null) {
                this.mAudioLowLatencyOutputFrameSize = Integer.parseInt(framesPerBuffer);
            }
        }

        this.mAudioLowLatencySupported = context.getPackageManager().hasSystemFeature("android.hardware.audio.low_latency");

        mRecordBufSize = AudioRecord.getMinBufferSize( AUDIO_GET_SIZE_SAMPLING_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mPlayBufSize = AudioTrack.getMinBufferSize( AUDIO_GET_SIZE_SAMPLING_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mSysBufSize = mRecordBufSize + mPlayBufSize ;

        xRTCLogging.d(TAG, "play sysbufsize:"+mPlayBufSize+" record sysbufsize:"+mRecordBufSize );
    }

    protected int getSysBufSize()
    {
        return mSysBufSize ;
    }
    public int getSysBufDelay()
    {
        return getSysBufSize()/16 ;
    }

    private int getNativeOutputSampleRate()
    {
        return mNativeOutputSampleRate;
    }

    private boolean isAudioLowLatencySupported()
    {
        return mAudioLowLatencySupported;
    }

    private int getAudioLowLatencyOutputFrameSize()
    {
        return mAudioLowLatencyOutputFrameSize;
    }

    public void initRecordAndPlayout()
    {
        int nStatus = querySpeakerStatus();
        if ( nStatus == SPEAKER_TYPE_EARPHONE )
        {
            //
            // 非免提状态开免提，其他都不开免提
            //
            setPlayoutSpeaker( true ) ;
        }
        registerHeadsetPlugReceiver();
    }

    public void cleanRecordAndPlayout()
    {
        unRegisterHeadsetPlugReceiver();
    }

    public int setPlayoutSpeaker( boolean bSpeakerOn )
    {
        if ( bSpeakerOn )
        {
            mAudioManager.setMode( AudioManager.MODE_NORMAL );

           if ( !mAudioManager.isSpeakerphoneOn() ) {
                xRTCLogging.i(TAG, "open speaker");
                mAudioManager.setSpeakerphoneOn(true);
            }
        }
        else
        {
            if ( mAudioManager.isSpeakerphoneOn() )
            {
                xRTCLogging.i(TAG, "close speaker");
                mAudioManager.setSpeakerphoneOn( false );
            }

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
                mAudioManager.setMode( AudioManager.MODE_IN_COMMUNICATION );
            }
            else
            {
                if ( Build.BRAND.equalsIgnoreCase( "xiaomi")  ) {
                    mAudioManager.setMode(AudioManager.MODE_NORMAL );
                }
                else {
                    mAudioManager.setMode(AudioManager.MODE_IN_CALL);
                }

            }
        }

        return 0;
    }

    private int querySpeakerStatus()
    {
        if ( mAudioManager.isSpeakerphoneOn()) {
            return SPEAKER_TYPE_SPEAKER;
        }

        if ( mAudioManager.isWiredHeadsetOn()) {
            return SPEAKER_TYPE_HEADSET;
        }

        if ( mAudioManager.isBluetoothScoOn()) {
            return SPEAKER_TYPE_BLUETOOTH;
        }

        return SPEAKER_TYPE_EARPHONE ;
    }

    public int setAudioMode(int mode)
    {
        switch (mode)
        {
            case AudioManager.MODE_NORMAL:
                xRTCLogging.i(TAG, "audio set mode MODE_NORMAL");
                break;
            case AudioManager.MODE_RINGTONE:
                xRTCLogging.i(TAG, "audio set mode MODE_RINGTONE");
                break;
            case AudioManager.MODE_IN_CALL:
                xRTCLogging.i(TAG, "audio set mode MODE_IN_CALL");
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                xRTCLogging.i(TAG, "audio set mode MODE_IN_COMMUNICATION");
                break;
            default:
                xRTCLogging.i(TAG, "audio set mode MODE_NORMAL mode err:"+mode ) ;
        }

        mAudioManager.setMode(mode);

        return 0;
    }

    private int GetAudioMode(int mode)
    {
        return mAudioManager.getMode();
    }


    private void registerHeadsetPlugReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");

        mRouteReceiver = new xRTCAudioRouteReceiver(this) ;

        mContext.registerReceiver(mRouteReceiver, intentFilter);

        // for bluetooth headset connection receiver
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        mContext.registerReceiver(mRouteReceiver, bluetoothFilter);
        xRTCLogging.i(TAG,"registerHeadsetPlugReceiver...");
    }

    private void unRegisterHeadsetPlugReceiver()
    {
        if ( mRouteReceiver != null ) {
            mContext.unregisterReceiver(mRouteReceiver);
            mRouteReceiver = null;
            xRTCLogging.i(TAG,"unregisterHeadsetPlugReceiver...");
        }
    }


}

package com.audio.device;

import android.annotation.TargetApi;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import android.os.Build;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;


import android.util.Log;

public class AudioDevice {
    // error_code begin
    public static final int ERR_OK = 0;
    public static final int ERR_NOT_FOUND_RECORD_DEVICE = ERR_OK + 1;
    public static final int ERR_NOT_FOUND_PLAYOUT_DEVICE = ERR_NOT_FOUND_RECORD_DEVICE + 1;
    public static final int ERR_ALLOC_MEMORY_FAILED = ERR_NOT_FOUND_PLAYOUT_DEVICE + 1;
    public static final int ERR_OPEN_RECORD_DEVICE_FAILED = ERR_ALLOC_MEMORY_FAILED + 1;
    public static final int ERR_OPEN_PLAYOUT_DEVICE_FAILED = ERR_OPEN_RECORD_DEVICE_FAILED + 1;
    public static final int ERR_INIT_RECORD_DEVICE_FAILED = ERR_OPEN_PLAYOUT_DEVICE_FAILED + 1;
    public static final int ERR_INIT_PLAYOUT_DEVICE_FAILED = ERR_INIT_RECORD_DEVICE_FAILED + 1;
    public static final int ERR_START_RECORD_FAILED = ERR_INIT_PLAYOUT_DEVICE_FAILED + 1;
    public static final int ERR_STOP_RECORD_FAILED = ERR_START_RECORD_FAILED + 1;
    public static final int ERR_START_PLAYOUT_FAILED = ERR_STOP_RECORD_FAILED + 1;
    public static final int ERR_STOP_PLAYOUT_FAILED = ERR_START_PLAYOUT_FAILED + 1;
    public static final int ERR_READ_RECORD_DATA_FAILED = ERR_STOP_PLAYOUT_FAILED + 1;
    public static final int ERR_WRITE_PLAYOUT_DATA_FAILED = ERR_READ_RECORD_DATA_FAILED + 1;
    // error code end

    // for GetParameter
    private static final int TYPE_RECORD_SAMPLERATE = 1;
    private static final int TYPE_PLAYOUT_SAMPLERATE = 2;
    private static final int TYPE_RECORD_CHANNEL = 3;
    private static final int TYPE_PLAYOUT_CHANNEL = 4;
    private static final int TYPE_RECORD_BUFFER_SIZE = 5;
    private static final int TYPE_PLAYOUT_BUFFER_SIZE = 6;
    private static final int TYPE_RECORD_SOURCE = 7;
    private static final int TYPE_PLAYOUT_SOURCE = 8;
    private static final int TYPE_MODE = 9;

    private String TAG = "AudioDevice";
    private boolean mRunflag = false;
    private DeviceInfo mRecordParam = new DeviceInfo();
    private DeviceInfo mPlayoutParam = new DeviceInfo();

    private long mInstance = 0;
    private ByteBuffer mRecordBuffer;
    private ByteBuffer mPlayoutBuffer;

    private int mTryRecordSource = -1;
    private int mTryPlayoutSource = -1;
    private ContextRef mContext = null;
    private int mAudioDefaultMode = 0;
    private AudioTrack mAudioTrack = null;
    private AudioRecord mAudioRecord = null;
    private AudioManager mAudioManager = null;
    private boolean mIsPlaying = false;
    private boolean mIsRecording = false;
    private boolean mIsPlayInit = false;
    private boolean mIsRecordInit = false;
    private int mPlayPosition = 0;
    private int mBufferedPlaySamples = 0;
    private int mBufferedRecSamples = 0;
    private int mRestartPlayDevice = 0;
    private int mRestartRecordDevice = 0;
    private int mTryPlaySampleRate = 48000;
    private int mTryRecordSampleRate = 48000;
    private int mTryPlayChannel = 2;
    private int mTryRecordChannel = 2;
    private AcousticEchoCanceler m_aec = null;
    public native void CacheDirectBufferAddress(long nativeAudioRecord, ByteBuffer recordBuffer,
                                                ByteBuffer playBuffer);

    public AudioDevice(ContextRef ctx, long ins) {
        mContext = ctx;
        mInstance = ins;
    }

    private int GetDeviceSampleRate() {

        AudioManager mgr = (AudioManager) mContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (null == mgr) {
            return 48000;
        }

        if (Build.VERSION.SDK_INT >= 17)
        {
            String sampleRateString = mgr.getProperty("android.media.property.OUTPUT_SAMPLE_RATE");
            if (null != sampleRateString) {
                return  Integer.parseInt(sampleRateString);
            }
        }

        return 48000;
    }

    static public boolean checkPermissions() {
        boolean bfound = false;
        for (int rec_resource : new int[]{MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.CAMCORDER, MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.DEFAULT}) {
            for (int sampleRate : new int[]{16000, 32000, 48000, 44100, 22050, 11025, 8000}) {
                for (short channelConfig : new short[]{AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        int nBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                                AudioFormat.ENCODING_PCM_16BIT) * 2;
                        if (nBufSize < 0)
                            continue;

                        AudioRecord dev = new AudioRecord(rec_resource, sampleRate, channelConfig,
                                AudioFormat.ENCODING_PCM_16BIT, nBufSize);

                        if (dev.getState() == AudioRecord.STATE_INITIALIZED) {
                            bfound = true;
                        }

                        dev.release();
                        dev = null;

                        if (bfound) {

                            return true;
                        }
                    } catch (Exception e) {
                        Log.d("AudioDevice:", "audio record " + e.getMessage());
                    }
                }
            }
        }

        Log.d("AudioDevice", "audio record check, not found usable sample rate ");
        return bfound;

    }



    public int getParameter(int type) {
        switch (type) {
            case TYPE_RECORD_SAMPLERATE:
                return mRecordParam.samplerate;
            case TYPE_PLAYOUT_SAMPLERATE:
                return mPlayoutParam.samplerate;
            case TYPE_RECORD_CHANNEL:
                return (AudioFormat.CHANNEL_IN_STEREO == mRecordParam.channel) ? 2 : 1;
            case TYPE_PLAYOUT_CHANNEL:
                return (AudioFormat.CHANNEL_OUT_STEREO == mPlayoutParam.channel) ? 2 : 1;
            case TYPE_RECORD_BUFFER_SIZE:
                return mRecordParam.minbufsize;
            case TYPE_PLAYOUT_BUFFER_SIZE:
                return mPlayoutParam.minbufsize;
            case TYPE_RECORD_SOURCE:
                return mRecordParam.source;
            case TYPE_PLAYOUT_SOURCE:
                return mPlayoutParam.source;
            case TYPE_MODE:
            {
                AudioManager mgr = (AudioManager) mContext.getContext().getSystemService(Context.AUDIO_SERVICE);
                if(mgr == null)
                    return 0;
                else
                    return mgr.getMode();
            }
            default:
                return -1;
        }
    }

    public int initialize(int recSource, int plySource, int mode, int recFrameTime, int plyFrameTime, int recChannel, int recSampleRate, int plyChannel, int plySampleRate) {
        int err = ERR_OK;
        if (mRunflag) {
            return ERR_OK;
        }

        Log.e(TAG, "source rec " + recSource + " ply " + plySource);
        mTryRecordSource   = (-1 == recSource) ? MediaRecorder.AudioSource.VOICE_COMMUNICATION: recSource;//
        mTryRecordSampleRate  = recSampleRate;
        mTryRecordChannel = recChannel;
        mTryPlayoutSource  = (-1 == plySource) ?  AudioManager.STREAM_VOICE_CALL : plySource;
        mTryPlaySampleRate = plySampleRate;
        mTryPlayChannel = plyChannel;

        if (!initRecFormat()) {
            err = ERR_NOT_FOUND_RECORD_DEVICE;
            Log.e(TAG, "Can not open record device");
        }
        if (!initPlayFormat()) {
            err = ERR_NOT_FOUND_PLAYOUT_DEVICE;
            Log.e(TAG, "Can not open playout device");
        }

        Log.e(TAG, "support source " + mRecordParam.source + " ply " + mPlayoutParam.source);

        mAudioManager = ((AudioManager) mContext.getContext().getSystemService(Context.AUDIO_SERVICE));

        mRunflag = true;
        final int recbytesPerFrame = getParameter(TYPE_RECORD_CHANNEL) * 2;
        final int recframesPerBuffer = getParameter(TYPE_RECORD_SAMPLERATE) * recFrameTime / 1000 ;
        final int plybytesPerFrame = getParameter(TYPE_PLAYOUT_CHANNEL) * 2;
        final int plyframesPerBuffer = getParameter(TYPE_PLAYOUT_SAMPLERATE) * plyFrameTime / 1000 ;
        mRecordBuffer  = ByteBuffer.allocateDirect(recbytesPerFrame * recframesPerBuffer);
        mPlayoutBuffer = ByteBuffer.allocateDirect(plybytesPerFrame * plyframesPerBuffer);
        CacheDirectBufferAddress(mInstance, mRecordBuffer, mPlayoutBuffer);

        Log.e(TAG, "source rec " + mTryRecordSource + " ply " + mTryPlayoutSource + " ply-s "+  mPlayoutParam.source + " ply-bufsize " + mPlayoutBuffer);

        AudioManager mgr = (AudioManager) mContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (mgr != null) {
            mAudioDefaultMode = mgr.getMode();
            mgr.setMode(mode);
        }

        return err;
    }

    public void terminate() {
        if (!mRunflag)
            return;

        mRunflag = false;
        _stopRecord("4");
        stopPlayout();
        AudioManager mgr = (AudioManager) mContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (mgr != null) {
            mgr.setMode(mAudioDefaultMode);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private boolean enableAec(boolean enable){
        if (null == mAudioRecord)
            return false;

        if(null == m_aec)
             m_aec = AcousticEchoCanceler.create(mAudioRecord.getAudioSessionId());

        if(null == m_aec)
        {
            Log.e("AudioDevice", "enableAec failed, create aec failed");
            return  false;
        }


        int ret = m_aec.setEnabled(enable);

        Log.e("AudioDevice", "enableAec ec=" + ret);
        return (ret == AudioEffect.SUCCESS);
    }

    public boolean enableRecord(boolean enable) {
        if (enable) {
            if (!mIsRecording) {
                _startRecord("3");
            }
        } else {
            _stopRecord("3");
        }
        return true;
    }

    public boolean enableAEC(boolean enable) {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            boolean bEnabled = enableAec(enable);
            if(bEnabled)
                Log.e("AudioDevice", "enableAEC " + enable + " succeed");
            else
                Log.e("AudioDevice", "enableAEC " + enable + " failed");

            return  bEnabled;
        }
        else {
            Log.e("AudioDevice", "enableAec failed, version " + Build.VERSION.SDK_INT + " expert: " + Build.VERSION_CODES.JELLY_BEAN);
            return false;
        }
    }

    private int initRecord() {
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }
        try {

            Log.e("AudioDevice", "Record Init ... mTryRecordSource " + mRecordParam.source + " mRecordParam.samplerate + "
                    + mRecordParam.samplerate + " channel: " + mRecordParam.channel + " minbufsize:" + mRecordParam.minbufsize);

            this.mAudioRecord = new AudioRecord(mRecordParam.source, mRecordParam.samplerate, mRecordParam.channel, 2,
                    2*mRecordParam.minbufsize);

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("AudioDevice", "Record Init failed 1");
            return -1;
        }

        if (mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioDevice", "Record Init failed 2");
            return -2;
        }

        Log.e("AudioDevice", "Record Init OK");
        mRestartRecordDevice = 0;
        mBufferedRecSamples = (5 * mRecordParam.samplerate / 200);
        return mBufferedRecSamples;
    }

    public boolean startRecord() {
        return _startRecord("1");
    }

    private boolean _startRecord(String strfrom) {
        if(this.mIsRecording){
            Log.e("AudioDevice", "Record startRecord, but started ??" + strfrom);
            return false;
        }

        Log.e("AudioDevice", "Record start ... " + strfrom);
        initRecord();
        if(mAudioRecord == null)
        {
            Log.e("AudioDevice", "Record start failed mAudioRecord == null");
            return false;
        }
        try {
            mAudioRecord.startRecording();
        } catch (IllegalStateException e) {

            Log.e("AudioDevice Java", "Record start failed " + e.getMessage());
            return false;
        }

        this.mIsRecording = true;
        Log.e("AudioDevice", "Record start succeed " + strfrom);
        return true;
    }

    public int recordAudio(int length) {

        if (!mIsRecording) {
            return 0;
        }
        try {
            if (mAudioRecord == null) {
                Log.e("AudioDevice Java", "--Record not opened");
                return -2;
            }
            if (mIsRecordInit == false) {
                try {
                    android.os.Process.setThreadPriority(-19);
                } catch (Exception e) {
                    Log.e("AudioDevice", "Record thread error" +  e.getMessage());
                }
                mIsRecordInit = true;
            }

            mRecordBuffer.rewind();
            int readBytes = mAudioRecord.read(mRecordBuffer, length);



            if (readBytes != length) {
                Log.e("AudioDevice", " Record read failed, len=" + length + " ret=" +  readBytes + " state: " + mAudioRecord.getState());
                _stopRecord("2");
                if (mRestartRecordDevice < 20) {
                    mRestartRecordDevice++;
                    _startRecord("2");
                }

                return readBytes;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

        return mBufferedPlaySamples;
    }

    public void stopRecord()
    {
        _stopRecord("1");
    }

    private void _stopRecord(String from) {
        if(!mIsRecording) {
            Log.e("AudioDevice", "Record stopRecord stoped " + from);
            return;
        }

        mIsRecording = false;
        try {
            if (mAudioRecord == null) {
                return;
            }
            Log.e("AudioDevice", "Record stop... " + from);

            if (AudioRecord.RECORDSTATE_RECORDING == mAudioRecord.getRecordingState()) {
                try {
                    mAudioRecord.stop();
                } catch (IllegalStateException e) {
                    Log.e("AudioDevice", "Record failed " + e.getMessage());
                    return;
                }
            }

            mAudioRecord.release();
            mAudioRecord = null;
            m_aec = null;
            Log.e("AudioDevice", "Record stoped " + from);
        } finally {
        }
    }

    private boolean initPlayout() {
        mBufferedPlaySamples = 0;
        mPlayPosition = 0;
        if (this.mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        try {

            int nchannel    = (AudioFormat.CHANNEL_OUT_STEREO == mPlayoutParam.channel) ? 2 : 1;
            int avgPerBytes = mPlayoutParam.samplerate*nchannel*2;
            int buffersize  =  Math.max(mPlayoutParam.minbufsize+avgPerBytes/100, avgPerBytes/10);//mPlayoutParam.minbufsize;//
            mAudioTrack = new AudioTrack(mPlayoutParam.source, mPlayoutParam.samplerate, mPlayoutParam.channel,
                    AudioFormat.ENCODING_PCM_16BIT, buffersize, AudioTrack.MODE_STREAM);
            Log.e(TAG, "source:" + mTryPlayoutSource + " samplerate:" + mPlayoutParam.samplerate + " channel:"
                    + mPlayoutParam.channel + "minbufsize:" + mPlayoutParam.minbufsize);
        } catch (Exception e) {
             e.printStackTrace();
            return false;
        }

        if (this.mAudioTrack.getState() != 1) {
            return false;
        }
        mRestartPlayDevice = 0;
        return true;
    }

    public boolean startPlayout() {
        initPlayout();
        if (mAudioTrack == null) {
            return false;
        }
        try {
            mAudioTrack.play();
        } catch (IllegalStateException e) {
            e.printStackTrace();

            return false;
        }
        mIsPlaying = true;
        return true;
    }

    private int ccc = 0;
    private long fromtime = 0;
    private void testCount()
    {
           long ctime = System.currentTimeMillis();
           ccc ++;
           if(ctime - fromtime > 3000)
           {
               Log.e("AudioDevice", "testCount = " + ccc);
               fromtime = ctime;
               ccc = 0;
           }
    }

    public int playAudio(int length) {
        if (!mIsPlaying) {
            return 0;
        }

        if (mIsPlayInit == false) {
            try {
                android.os.Process.setThreadPriority(-19);
            } catch (Exception e) {
                Log.e("AudioDevice", "Set play thread priority failed: ", e);
            }
            mIsPlayInit = true;
        }
        int written = 0;

        try {
        //    mPlayLock.lock();
            if (mAudioTrack == null) {
                return written;
            }
            written = mAudioTrack.write(mPlayoutBuffer.array(), mPlayoutBuffer.arrayOffset(), length);
            if (written != length) {
                stopPlayout();
                mBufferedPlaySamples = 0;
                mPlayPosition = 0;
                if (mRestartPlayDevice < 20) {
                    mRestartPlayDevice++;
                    startPlayout();
                }

                mBufferedPlaySamples = written / 2;
                return mBufferedPlaySamples;
            }
            int pos = mAudioTrack.getPlaybackHeadPosition() * getParameter(TYPE_PLAYOUT_CHANNEL);
            if (pos < mPlayPosition) {
                mPlayPosition = 0;
            }
            mBufferedPlaySamples += written / 2;
            mBufferedPlaySamples -= (pos - mPlayPosition);
            mPlayPosition = pos;
        } finally {
     //       mPlayLock.unlock();
        }
        return mBufferedPlaySamples;
    }

    public void stopPlayout() {
        mIsPlaying = false;
        mIsPlayInit = false;
        Log.d("AudioDevice", "Audio playout StopPlayout...");
        try {
            if (mAudioTrack == null) {
                return;
            }
            if (mAudioTrack.getPlayState() == 3) {
                try {
                    mAudioTrack.stop();
                } catch (IllegalStateException e) {
                    Log.e("AudioDevice", "Unable to stop playback: ", e);
                    return;
                }

                mAudioTrack.flush();
            }
            mAudioTrack.release();
            mAudioTrack = null;
        } finally {
        }
        Log.d("AudioDevice", "audio playout StopPlayout ok");
    }

    private boolean initRecFormat() {
        boolean bfound = false;

        int nDeviceSampleRate = (mTryRecordSampleRate > 0) ? mTryRecordSampleRate:16000;//GetDeviceSampleRate();
        int mDeviceChannel = (2 == mTryRecordChannel) ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;

        for (int rec_resource : new int[]{mTryRecordSource, MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.CAMCORDER, MediaRecorder.AudioSource.MIC, MediaRecorder.AudioSource.DEFAULT}) {
            for (int sampleRate : new int[]{nDeviceSampleRate, 16000, 32000, 48000, 44100, 22050, 11025, 8000}) {
                for (int channelConfig : new int[]{mDeviceChannel, AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                    try {
                        int nBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
                                AudioFormat.ENCODING_PCM_16BIT);
                        if (nBufSize < 0)
                            continue;

                        AudioRecord dev = new AudioRecord(rec_resource, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, nBufSize);
                        if (dev.getState() == AudioRecord.STATE_INITIALIZED) {
                            mRecordParam.channel = channelConfig;
                            mRecordParam.samplerate = sampleRate;
                            mRecordParam.source = rec_resource;
                            mRecordParam.minbufsize = nBufSize;
                            bfound = true;
                        }

                        dev.release();
                        dev = null;

                        if (bfound) {
                            Log.d("AudioDevice", "audio record support sample rate: " + mRecordParam.samplerate
                                    + " channel: " + mRecordParam.channel + " rec_resource: " + mRecordParam.source);
                            return true;
                        }
                    } catch (Exception e) {
                        Log.d("AudioDevice:", "audio record " + e.getMessage());
                    }
                }
            }
        }

        Log.d("AudioDevice", "audio record check, not found samplerate ");
        return false;

    }

    private boolean initPlayFormat() {
        boolean bfound = false;

        int nDeviceSampleRate = (mTryPlaySampleRate > 0) ? mTryPlaySampleRate:48000;//GetDeviceSampleRate();
        int mDeviceChannel = (2 == mTryPlayChannel) ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
        for (int ply_resource : new int[]{mTryPlayoutSource, AudioManager.STREAM_VOICE_CALL, AudioManager.STREAM_MUSIC}) {
            for (int sampleRate : new int[]{nDeviceSampleRate, 48000, 16000, 32000, 44100, 22050, 11025, 8000}) {
                for (int channelConfig : new int[]{mDeviceChannel, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.CHANNEL_OUT_STEREO}) {
                    try {
                        int nBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig,
                                AudioFormat.ENCODING_PCM_16BIT);
                        if (nBufSize < 0)
                            continue;

                        AudioTrack dev = new AudioTrack(ply_resource, sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, nBufSize, AudioTrack.MODE_STREAM);

                        if (dev.getState() == AudioTrack.STATE_INITIALIZED) {
                            mPlayoutParam.channel = channelConfig;
                            mPlayoutParam.samplerate = sampleRate;
                            mPlayoutParam.source = ply_resource;
                            mPlayoutParam.minbufsize = nBufSize;
                            bfound = true;
                        }
                        dev.release();
                        dev = null;

                        if (bfound) {
                            Log.d("AudioDevice", "audio play out support sample rate: " + mPlayoutParam.samplerate
                                    + " channel: " + mPlayoutParam.channel + " ply_resource: " + mPlayoutParam.source);
                            return true;
                        }

                    } catch (Exception e) {
                        Log.e("AudioDevice:", "audio playout " + e.getMessage());
                    }
                }
            }
        }

        Log.d("AudioDevice", "audio playout check, not found sample rate ");
        return false;

    }

    private class DeviceInfo {
        public int source = 0;
        public int minbufsize = 0;
        public int channel = 0;
        public int samplerate = 0;
    }

}

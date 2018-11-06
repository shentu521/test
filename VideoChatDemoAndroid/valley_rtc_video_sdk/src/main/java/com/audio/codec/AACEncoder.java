package com.audio.codec;

import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
// using @TargeApi instead of @SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class AACEncoder{
private String MIME_TYPE="audio/mp4a-latm"; 
private final String TAG="AACEncoder";  
private MediaCodec mEncoder;
private MediaCodec.BufferInfo mBufferInfo; 
private ByteBuffer mDirectBuffer = null;
private long  mNativeInstance = -1;
private byte[] csd_0;
public native void CacheDirectBufferAddress( long nativeInstance, ByteBuffer directBuffer );
public AACEncoder(long nativeInstance)
{ 
	mNativeInstance = nativeInstance;
} 

//�����ã�ʵ����Ŀ����c++����
public byte[] DirectBuffer()
{
	if(mDirectBuffer != null && mDirectBuffer.hasArray())
	{
    	return mDirectBuffer.array();
	}
	return null;
}

public boolean start(int samplerate, int channel, int bitrate, int aacObjectType)
{
	Log.i(TAG,"samplerate:" + samplerate + " channel:" + channel + " bitrate: " + bitrate + "type:" + aacObjectType);
	try{
		if(mEncoder == null)
		{
		    mDirectBuffer = ByteBuffer.allocateDirect(8192);
		    mBufferInfo = new MediaCodec.BufferInfo();  
		    mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);  
		    MediaFormat mediaFormat = MediaFormat.createAudioFormat(MIME_TYPE,  
		    		samplerate, channel);  
		    mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);  
		    mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,  
		    		aacObjectType);
			mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192);
		    mEncoder.configure(mediaFormat, null, null,  
		            MediaCodec.CONFIGURE_FLAG_ENCODE);  
		    mEncoder.start();
		    
		    int ascPollCount = 0;
			while (csd_0 == null && ascPollCount < 10) {
				// Try to get the asc
				int encInBufIdx = -1;
				encInBufIdx = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);
				if (encInBufIdx >= 0) {
					if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
						csd_0 = new byte[mBufferInfo.size];
						mEncoder.getOutputBuffers()[encInBufIdx].get(csd_0, 0, mBufferInfo.size);
						mEncoder.getOutputBuffers()[encInBufIdx].clear();
						mEncoder.releaseOutputBuffer(encInBufIdx, false);
						mDirectBuffer.clear();
						mDirectBuffer.put(csd_0);
					}
				}
				ascPollCount++;
			}
		    if(mNativeInstance != -1)CacheDirectBufferAddress(mNativeInstance,mDirectBuffer);
	    	MediaFormat format = mEncoder.getOutputFormat();
	        channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
	    	if(aacObjectType == 29)
	    	{
	    		//Log.d(TAG,"unsupport aac profile");
	    		return false;
	    	}
		    return true;
		}
	}catch(Exception e)
	{
		e.printStackTrace();
		return false;
	}
	return false;
}
public void stop(){  
	try{
		if(mEncoder != null)
		{
			mEncoder.stop();
			mEncoder.release();
			mEncoder =null;
		}
	}catch(Exception e)
	{
		e.printStackTrace();
	}

}

public int encode(int length) {
	try{
		mDirectBuffer.position(0);
		mDirectBuffer.limit(length);
	    int inputBufferIndex = mEncoder.dequeueInputBuffer(-1);  
	    if (inputBufferIndex >= 0) {  
	        ByteBuffer[] inputBuffer = mEncoder.getInputBuffers();  
	        inputBuffer[inputBufferIndex].clear();
	        inputBuffer[inputBufferIndex].put(mDirectBuffer);  
	        inputBuffer[inputBufferIndex].limit(length);  
	        mEncoder.queueInputBuffer(inputBufferIndex, 0, length,  
	                System.nanoTime(), 0);  
	    }  

	    int outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, 0);  
	    if (outputBufferIndex >= 0)
	    {
	        ByteBuffer[] outputBuffer = mEncoder.getOutputBuffers();  
	        //������
	        outputBuffer[outputBufferIndex].position(0);
	        mDirectBuffer.position(0);
	        outputBuffer[outputBufferIndex].limit(mBufferInfo.size);
	        mDirectBuffer.put(outputBuffer[outputBufferIndex]);
	        outputBuffer[outputBufferIndex].clear();
	        mEncoder.releaseOutputBuffer(outputBufferIndex, false);
	        return mBufferInfo.size;
	    }
	    else if( outputBufferIndex == -2)
	    {
	    	MediaFormat format = mEncoder.getOutputFormat();
	    	int channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
	    	 
	    }
	}catch(Exception e)
	{
		e.printStackTrace();
	}
    return 0;
}  

}  
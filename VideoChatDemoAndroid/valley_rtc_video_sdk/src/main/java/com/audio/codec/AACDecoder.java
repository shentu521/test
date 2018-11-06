package com.audio.codec;

import java.nio.ByteBuffer;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;



@TargetApi(Build.VERSION_CODES.JELLY_BEAN) 
//using @TargeApi instead of @SuppressLint("NewApi")
@SuppressWarnings("deprecation")
final public class AACDecoder {
    String                 MIME_TYPE = "audio/mp4a-latm"; 
    private static final String TAG  = "AudioDecoder";
    private MediaCodec      mDecoder = null;  
    private byte[]     mPacketHeader = new byte[7];//aac header;
    private int            WAIT_TIME = 10000;
	private ByteBuffer mDirectBuffer = null;
    private MediaCodec.BufferInfo mBufferInfo;
    private long mNativeInstance = -1;
    private int mSamplerate = 48000;
    private int mChannel    = 2;
    private int mProfile    = 5;
    private int mFreqIdx    = 3;
    private MediaFormat mFormat;
    public native void CacheDirectBufferAddress( long nativeInstance, ByteBuffer directBuffer );
    public native void UpdateMediaFormat(long nativeInstance, int channel);
    public AACDecoder(long nativeInstance){
    	mNativeInstance = nativeInstance;
    }
    void UpdateFreqIndex(int samplerate, int channel, int aacobjectType)
    {
    	mSamplerate = samplerate;
    	mChannel = channel;
    	mProfile = 2;
    	int freqs[] = new int[]{96000,88200,64000,48000,44100,32000,24000,22050,16000,12000,11025,8000,7350};
        for (int i = 0; i<freqs.length;i++)
        {
        	if(mSamplerate == freqs[i])
        	{
        		mFreqIdx = i;
        		break;
        	}
        }
    }
    //�����ã�ʵ����Ŀ����c++����
    public byte[] DirectBuffer()
    {
    	if(mDirectBuffer != null && mDirectBuffer.hasArray())
    	{
    		mDirectBuffer.clear();
        	return mDirectBuffer.array();
    	}
    	return null;
    }
    public boolean start( int samplerate, int channel, int csd,int aacObjectType) {  
    	Log.i(TAG,"samplerate:" + samplerate + " channel:" + channel + " csd:" + (csd>>8) + (csd&0xff)+ " type:" + aacObjectType);
    	try{
            //�ȴ��ͻ���  
    		UpdateFreqIndex(samplerate,channel,aacObjectType);
            mBufferInfo = new MediaCodec.BufferInfo();  
            mDirectBuffer = ByteBuffer.allocateDirect(8192);
            mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);  
    		MediaFormat format = new MediaFormat();  
    		format.setString(MediaFormat.KEY_MIME,MIME_TYPE);  
    		format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channel);  
    		format.setInteger(MediaFormat.KEY_SAMPLE_RATE, samplerate);  
    		format.setInteger(MediaFormat.KEY_BIT_RATE, 32000);
    		format.setInteger(MediaFormat.KEY_IS_ADTS,1);  
    		format.setInteger(MediaFormat.KEY_AAC_PROFILE,aacObjectType);    
    		byte[] csd_0 = new byte[]{(byte) ((csd>>8)&0xff),(byte)(csd&0xff)};// ��Ҳ��֪����ɶ�ã����ܸ�����ɶ���й�ϵ�����ֵ���ı�Ҳ���������š�
    		ByteBuffer bb = ByteBuffer.wrap(csd_0);  
    		Log.i(TAG,"csd:"+csd_0[0]+csd_0[1]);
    		format.setByteBuffer("csd-0", bb);  
    		mDecoder.configure(format, null, null, 0); 
            if (mDecoder == null) {  
                Log.e(TAG, "create mediaDecode failed");  
                return false;  
            }
            mDecoder.start();  
		    if(mNativeInstance!=-1)
		    	CacheDirectBufferAddress(mNativeInstance, mDirectBuffer);
		    
            return true;  
    	}catch(Exception e)
    	{
    		mDecoder = null;
    		e.printStackTrace();
    	}
        return false;
    }  

/**
 *  ����AAC֡
 * @param frame frameΪAAC֡������Ϊ�ա����Ϊ�գ����ʾĩβ֡��
 * @return �п����ǿյ�Ҳ���ܰ�����֡��
 */
    public int decode(int length) {
    	mDirectBuffer.position(0);
    	mDirectBuffer.limit(length);
    	try{
    		int inputIndex = mDecoder.dequeueInputBuffer(-1);  
            if(inputIndex>=0){  
                ByteBuffer[] inputBuffer = mDecoder.getInputBuffers();   
                if(length > 0)
                {
                    inputBuffer[inputIndex].clear();
                    addADTStoPacket(mPacketHeader,length+7);
                    inputBuffer[inputIndex].put(mPacketHeader,0,7);
                    inputBuffer[inputIndex].put(mDirectBuffer);
                    mDecoder.queueInputBuffer(inputIndex, 0, length+7, 0, MediaCodec.BUFFER_FLAG_SYNC_FRAME);
                }
                else
                {
                    mDecoder.queueInputBuffer(inputIndex, 0, 0, 0,MediaCodec.BUFFER_FLAG_END_OF_STREAM);  
                }
                mBufferInfo.size = 0;
                int outputIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, 0);           
                
                if (outputIndex >= 0) { 
                	ByteBuffer[] outputBuffer = mDecoder.getOutputBuffers();
                	outputBuffer[outputIndex].position(0);
                	outputBuffer[outputIndex].limit(mBufferInfo.size);
                	mDirectBuffer.clear();
                	mDirectBuffer.put(outputBuffer[outputIndex]);
                    outputBuffer[outputIndex].clear();//����ȡ����һ���ǵ���մ�Buffer MediaCodec��ѭ��ʹ����ЩBuffer�ģ�������´λ�õ�ͬ��������    
                    mDecoder.releaseOutputBuffer(outputIndex, false);//�˲���һ��Ҫ������ȻMediaCodec�������е�Buffer�� �����������������  
                }
                else if( outputIndex == -2)
                {
                	mFormat = mDecoder.getOutputFormat();
                	int channel = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                	UpdateMediaFormat(mNativeInstance, channel);
                }
                return mBufferInfo.size;
        	}
    	}catch(Exception e)
    	{
    		Log.e(TAG,"aac decode crash..................");
    		e.printStackTrace();
    	}
    	return 0;
    }  
    
    public void stop(){
        try
        {
            if(mDecoder!=null){  
                mDecoder.stop();  
                mDecoder.release();
                mDecoder = null;
            }  
        }
       catch(Exception e)
       {
    	   e.printStackTrace();
       }
    }  
    /** 
     * ���������aac�������adtsͷ�ֶ� 
     * @param packet Ҫ�ճ�ǰ7���ֽڣ������������� 
     * @param packetLen 
     */  
    private void addADTStoPacket(byte[] packet, int packetLen) {  
        int profile = mProfile;  //AAC LC  
        int freqIdx = mFreqIdx;  //44.1KHz  
        int chanCfg = mChannel;  //CPE  
        packet[0] = (byte)0xFF;  
        packet[1] = (byte)0xF1;  
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));  
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));  
        packet[4] = (byte)((packetLen&0x7FF) >> 3);  
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);  
        packet[6] = (byte)0xFC;  
    } 

    }  

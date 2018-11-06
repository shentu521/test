package com.rtc.client;
import android.media.AudioManager;
import android.content.Context;

/*
import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.app.Application;
*/

public class IRtcAudio
{
	public final static int IID = 0x02;

	public final static int RespondDisableUserSpeak    = 201;   // object_disable_speak
	public final static int RespondBlockUser            = 202;   // object_block_speak
	public final static int NotifyDisableUserSpeak     = 201;   // object_disable_speak
	public final static int NotifyUserSpeaking          = 202;   // object_user_speaking

	private ValleyRtcNative mNative     = null;
	private long           mInst        = 0;

	protected        IRtcAudio(ValleyRtcNative ntv, long ins){mNative = ntv; mInst=ins;}

	public int       BlockUser(String uid, boolean block){return mNative.JNI_BlockUser(mInst, uid, block);}
	public int       DisableUserSpeak(String uid, boolean disspeak){return mNative.JNI_DisableUserSpeak(mInst, uid, disspeak);}
	public int       EnableSpeak(boolean enable){return mNative.JNI_EnableSpeak(mInst, enable);}
	public boolean  GetSpeakEnabled(){return mNative.JNI_GetSpeakEnabled(mInst);}
	public int       EnablePlayout(boolean enable){return mNative.JNI_EnablePlayout(mInst, enable);}
	public boolean  GetPlayoutEnabled(){return mNative.JNI_GetPlayoutEnabled(mInst);}
}
	

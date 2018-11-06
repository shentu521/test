package com.rtc.client;

import android.os.Handler;
import android.util.Log;

public class IRtcChannel {
	public final static int RespondLogin       = 1;		     // object_login
	public final static int RespondChannelAttr = 2;			 // object_channel_attr

	public final static int NotifyConnectionLost   = 1;       // null
	public final static int NotifyReConnected      = 2;      // null
	public final static int NotifyDuplicateLogined = 3;       // null
	public final static int NotifyChannelClose     = 4;       // object_error
	public final static int NotifyChannelAttr      = 5;       // object_channel_attr

	private ValleyRtcNative mNative     = new ValleyRtcNative(this);
	private long           mInst        = 0;
	private IRtcUsers       mUsers      = null;
	private IRtcAudio       mRtcAudio  = null;
	private IRtcAudioSystem mRtcAudioSystem  = null;
	private IRtcDeviceControler mRtcDeviceControler  = null;
	private IRtcMsger       mRtcMsger  = null;
	private IRtcSink        mSink       = null;
	private long           mUserData    = 0;


	Handler mHandler = new Handler();
	Runnable runnable = new Runnable() {
		public void run() {

			if(0 != mInst) {
				//Log.d("ValleyRtcAPI", "JNI_Poll " + mInst + " userdata" + mUserData);
				mNative.JNI_Poll(mInst);
			}
			if(mHandler != null)
				mHandler.postDelayed(this, 500);
		}
	};

	public IRtcChannel() {
	 	mInst = mNative.JNI_Constuctor();
	 	mHandler.postDelayed(runnable, 500);
	}

	public  void finalize() {
		if(0 != mInst){
			Log.d("ValleyRtcAPI", "destroy " + mInst);

			mNative.JNI_Destructor(mInst);
			mInst = 0;
			mHandler = null;
		}
	}



	public void RegisterRtcSink(IRtcSink sink, long userdata){mSink = sink;mUserData=userdata;};

	public int EnableInterface(int iids){

		int ec = mNative.JNI_EnableInterface(mInst, iids);
		if(0 != ec) {
			Log.d("ValleyRtcAPI", "EnableInterface " + iids + " ec=" + ec);
			return ec;
		}

		if(0 != (iids & IRtcUsers.IID)) {
			Log.d("ValleyRtcAPI", "IRtcUsers Init OK ");
			mUsers = new IRtcUsers(mNative, mInst);
		}

		if(0 != (iids & IRtcUsers.IID)) {
			Log.d("ValleyRtcAPI", "IRtcAudio Init OK ");
			mRtcAudio = new IRtcAudio(mNative, mInst);
		}

		if(0 != (iids & IRtcAudioSystem.IID)) {
			Log.d("ValleyRtcAPI", "IRtcAudioSystem Init OK ");
			mRtcAudioSystem = new IRtcAudioSystem(mNative, mInst);
		}

		if(0 != (iids & IRtcDeviceControler.IID)) {
			Log.d("ValleyRtcAPI", "IRtcDeviceControler Init OK ");
			mRtcDeviceControler = new IRtcDeviceControler(mNative, mInst);
		}

		if(0 != (iids & IRtcMsger.IID)) {
			Log.d("ValleyRtcAPI", "IRtcMsger Init OK ");
			mRtcMsger = new IRtcMsger(mNative, mInst);
		}
		return  ec;
	}


	public int  DisableInterface(int iid){
		int ec = mNative.JNI_DisableInterface(mInst, iid);
		if(0 != ec){
			Log.e("ValleyRtcAPI", "DisableInterface " + iid + " ec=" + ec);
			return ec;
		}

		if(iid == IRtcUsers.IID)
			mUsers = null;
		else if(iid == IRtcAudio.IID)
			mRtcAudio = null;
		else if(iid == IRtcAudioSystem.IID)
			mRtcAudioSystem = null;
		else if(iid == IRtcDeviceControler.IID)
			mRtcDeviceControler = null;
		else if(iid == IRtcMsger.IID)
			mRtcMsger = null;

		Log.e("ValleyRtcAPI", "DisableInterface Is null iid=" + iid);
		return  0;
	}

	public Object  GetInterface(int iid){
		if(iid == IRtcUsers.IID)
			return mUsers;
		else if(iid == IRtcAudio.IID)
			return mRtcAudio;
		else if(iid == IRtcAudioSystem.IID)
			return mRtcAudioSystem;
		else if(iid == IRtcDeviceControler.IID)
			return mRtcDeviceControler;
		else if(iid == IRtcMsger.IID)
			return mRtcMsger;

		Log.e("ValleyRtcAPI", "GetInterface Is null iid=" + iid);
		return  null;
	}

	public int     Login(String channelid, String userid, String userinfo){ return mNative.JNI_Login(mInst, channelid, userid, userinfo); }
	public void    Logout(){ mNative.JNI_Logout(mInst); }
	public int     GetLoginStatus(){return mNative.JNI_GetLoginStatus(mInst);}
	public int     SetChannelAttr(String name, String value){return mNative.JNI_SetChannelAttr(mInst, name, value);}
	public int     GetChannelAttr(String name, object_string value){return mNative.JNI_GetChannelAttr(mInst, name, value);}





	public void OnRespond(int type, int ec, Object obj){
		if (null == mSink)
			return;

		//Log.d("ValleyRtcAPI", "OnRespond type=" + type + " ec=" + ec + " userdata:" + mUserData);
		mSink.Respond(type, ec, obj, mUserData);
	}

	public void OnNotify(int type, Object obj){
		if (null == mSink)
			return;

		//Log.d("ValleyRtcAPI", "OnNotify type=" + type + " userdata:" + mUserData);
		mSink.Notify(type, obj, mUserData);
	}
}

 
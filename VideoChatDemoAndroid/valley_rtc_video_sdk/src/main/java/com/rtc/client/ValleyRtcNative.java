package com.rtc.client;




class ValleyRtcNative
{	
 	static protected native void JNI_Init(Object ctx, String wkfolder, String localConfig);
 	static protected native void JNI_Clean();
 	static protected native void JNI_SetAuthoKey(String authokey);
	static protected native String JNI_GetErrDesc(int ecode);
	static protected native String JNI_GetSDKVersion();

	protected native long 	     JNI_Constuctor();
	protected native void 	     JNI_Destructor(long ins);
	protected native int          JNI_EnableInterface(long ins, int iids);
	protected native int          JNI_DisableInterface(long ins, int iid);
	protected native int          JNI_Login(long ins, String roomkey, String uid, String userinfo);
	protected native void         JNI_Logout(long ins);
	protected native int          JNI_GetLoginStatus(long ins);
	protected native int          JNI_SetChannelAttr(long ins, String name, String value);
	protected native int          JNI_GetChannelAttr(long ins, String name, object_string value);

	protected native int          JNI_GetUserCount(long ins);
	protected native int          JNI_GetUserList(long ins, object_user_sheet userlist);
	protected native int          JNI_GetUser(long ins, String uid, object_user user);
	protected native int          JNI_KickOff(long ins, String uid);
	protected native int          JNI_SetUserAttr(long ins, String uid, String name, String value);
	protected native int          JNI_GetUserAttr(long ins, String uid, String name, object_string value);


	//real audio
	protected native int        JNI_BlockUser(long ins, String uid, boolean block);
	protected native int        JNI_DisableUserSpeak(long ins, String uid, boolean disspeak); //--
	protected native int        JNI_EnableSpeak(long ins, boolean enable);
	protected native boolean    JNI_GetSpeakEnabled(long ins); //--
	protected native int        JNI_EnablePlayout(long ins, boolean enable);
	protected native boolean   JNI_GetPlayoutEnabled(long ins); //--

	//audio msgr
	protected native int        JNI_StartRecord(long ins);
	protected native int        JNI_StopRecord(long ins, boolean cancel);
	protected native boolean    JNI_IsRecordingAudioMsg(long ins);
	protected native int        JNI_StartPlayout(long ins, String url);
	protected native int        JNI_StopPlayout(long ins);
	protected native boolean    JNI_IsPlayingAudioMsg(long ins);
	protected native int        JNI_GetAudioMsgTimeSpan(long ins, String url);
	protected native boolean    JNI_IsAudioMsgLocalExsit(long ins, String url);
	protected native void       JNI_Poll(long ins);

	//audiosystem ctrl
	protected native void       JNI_SetPlayoutVolume(long ins, int volume);
	protected native int        JNI_GetPlayoutVolume(long ins);
	protected native void       JNI_SetSpeakerphoneOn(long ins, int on);
	protected native boolean   JNI_GetSpeakphoneOn(long ins);


	//device ctrl
	protected native int        JNI_DeviceEnable(long ins, int types, boolean enable);
	protected native boolean   JNI_IsDeviceEnable(long ins, int type);
	protected native int        JNI_SetBackgroudMusic(long ins, int trackIndex, String filepath, boolean loopflag, float volume, boolean bSendToNet, boolean bPlayout);
	protected native int        JNI_SetBackgroudMusicVolume(long ins, int trackIndex, float volume);

	//RoomControler
	protected native  int       JNI_StartRtmp(long ins, String url, int sreamtypes, boolean bUseServer);
	protected native  void      JNI_StopRtmp(long ins);

	protected native  int       JNI_StartRecordEx(long ins, int sreamtypes);
	protected native  void      JNI_StopRecordEx(long ins);
	protected native  void      JNI_PauseRecordEx(long ins, boolean bPause);

	//msg
	protected native  int        JNI_SendMsgr(long ins, int type, String data, String token, String uid);
	protected native  int        JNI_GetMsgrList(long ins, int msgid,  int nCount);

	static
	{
		System.loadLibrary("ValleyRtcSDK");
	}

	protected IRtcChannel mChannel = null;
	protected ValleyRtcNative(IRtcChannel api){mChannel=api;}
	public void OnRecvRespond(int type, int ec, Object obj){mChannel.OnRespond(type, ec, obj);}
	public void OnRecvNotify(int type, Object obj){mChannel.OnNotify(type, obj);}
}
	

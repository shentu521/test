package com.rtc.client;


public class IRtcUsers
{
	public final static int IID = 0x01;
	public final static int RespondKickOff          = 101;	      // object_userid
	public final static int RespondUserAttr         = 102;	      // object_user_attr
	public final static int NotifyUserEnterChannel = 101;      // object_user
	public final static int NotifyUserLeaveChannel = 102;      // object_userid
	public final static int NotifyKickOff            = 103;     // object_userid
	public final static int NotifyUserAttr           = 104;     // object_user_attr

	private ValleyRtcNative mNative      = null;
	private long           mInst         = 0;
	protected IRtcUsers(ValleyRtcNative ntv, long ins){mNative = ntv; mInst=ins;}

	public int  GetUserCount(){ return mNative.JNI_GetUserCount(mInst); }
	public int  GetUserList(object_user_sheet userlist){ return mNative.JNI_GetUserList(mInst, userlist); }
	public int  GetUser(String uid, object_user user){ return mNative.JNI_GetUser(mInst, uid, user);}
	public int  KickOff(String uid){ return mNative.JNI_KickOff(mInst, uid);}
	public int  SetUserAttr(String uid, String name, String value){ return mNative.JNI_SetUserAttr(mInst, uid, name, value); }
	public int  GetUserAttr(String uid, String name, object_string value){ return mNative.JNI_GetUserAttr(mInst, uid, name, value); }
}
	

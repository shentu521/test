package com.rtc.client;

/*
import android.media.AudioManager;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.app.Application;
*/

public class IRtcMsger
{
	public final static int IID = 0x10;

	public final static int RespondSendMessage      = 301;   // object_msg
	public final static int RespondGetMessageList  = 302;   // object_msg_sheet
	public final static int NotifyRecvedMessage     = 301;   // object_msg

	private ValleyRtcNative mNative     = null;
	private long           mInst        = 0;


	protected IRtcMsger(ValleyRtcNative ntv, long ins){mNative = ntv; mInst=ins;}
	public int  SendMsgr(int type, String data, String token, String toUserID){return mNative.JNI_SendMsgr(mInst, type, data, token, toUserID);}
	public int  GetMsgrList(int msgid, int nCount){return mNative.JNI_GetMsgrList(mInst, msgid, nCount);}
}
	

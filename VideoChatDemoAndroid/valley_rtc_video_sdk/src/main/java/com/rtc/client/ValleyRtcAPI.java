package com.rtc.client;


import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Environment;

import java.io.File;


public class ValleyRtcAPI
{
	static public void  InitSDK(Application app, String wkfolder, String localconfig){
		Context ctx = (Context)app.getApplicationContext();
		if(null == wkfolder || wkfolder.isEmpty()) {
			File file = Environment.getExternalStorageDirectory();

			if(!file.exists())
				file = Environment.getDataDirectory();

			if(!file.exists()) {
				file =  ctx.getFilesDir();
			}

			String mDataPath = file.getAbsolutePath() + "/ValleyRtcSDK";
			ValleyRtcNative.JNI_Init(ctx, mDataPath,localconfig);
		}
		else {
			ValleyRtcNative.JNI_Init(ctx, wkfolder,localconfig);
		}
	};

	static public void  SetAuthoKey(String authokey){ValleyRtcNative.JNI_SetAuthoKey(authokey);}
	static public String GetErrDesc(int ec){return ValleyRtcNative.JNI_GetErrDesc(ec);}
	static public String GetSDKVersion(){return ValleyRtcNative.JNI_GetSDKVersion();}
	static public void  CleanSDK(){ValleyRtcNative.JNI_Clean();}
	static public IRtcChannel CreateChannel(){return new IRtcChannel();}
}
	


///////////////////////////////////



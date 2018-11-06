package valley.api;

import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Environment;

public class ValleyRtcAPI {

    protected static Application mapp = null;
    protected static String      mAuthKey = null;
    protected static String      mWorkfolder = null;

    static public boolean InitSDK(Application app, String workfolder, String localconfig){ //初始化SDK,一个进程只需要初始化一次
        mapp = app;
        mWorkfolder = workfolder;
        com.rtc.client.ValleyRtcAPI.InitSDK(app, workfolder, localconfig);
        return true;
    }

    static public void  SetAuthoKey(String authokey){
        mAuthKey = authokey;
        com.rtc.client.ValleyRtcAPI.SetAuthoKey(authokey);
    }

    static public String GetErrDesc(int ec){
        return com.rtc.client.ValleyRtcAPI.GetErrDesc(ec);
    }

    static public String GetSDKVersion(){
        return com.rtc.client.ValleyRtcAPI.GetSDKVersion();
    }

    static public void  CleanSDK(){
        com.rtc.client.ValleyRtcAPI.CleanSDK();
    }

    static public IRtcChannel CreateChannel(boolean withVideo,Context ctx){
        if(null == mapp)
            return null;

        return new IRtcChannel(withVideo, ctx);
    }
}

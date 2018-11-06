package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */
import android.app.Application;
import android.content.Context;
import android.media.AudioManager;
import android.os.Environment;

import com.rtc.client.IRtcAudio;
import com.rtc.client.IRtcMsger;
import com.rtc.client.IRtcUsers;


public class IRtcChannel {


    public final static int ERR_SUCCEED = 0;        // 操作成功
    public final static int ERR_NOT_LOGINED = -1;   // 未登录成功
    public final static int ERR_ALREADY_RUN  =-2;   // 已经在运行
    public final static int ERR_USER_NOTFOUND =-3;  // 未找到用户
    public final static int ERR_EXCUTING =-4;		  // 正在执行中
    public final static int ERR_NOT_INITIALIZE= -5; // 未初始化
    public final static int ERR_UNSUPPORT =-6;      // 功能不支持
    public final static int ERR_ARGUMENT  =-7;	  // 参数错误

    public final static int ERR_CHANNEL_EXPIRED =1;        // 频道已经失效
    public final static int ERR_CONNECT_SERVER_FAILED =2;  // 连接服务器失败
    public final static int ERR_REQUEST_TIMEOUT =3;  // 请求超时
    public final static int ERR_CONFIG =4;  // 配置信息错误
    public final static int ERR_NET_POOL =5;  // 网络不好
    public final static int ERR_VERSION_UNSUPPORTED =6;  // 版本不支持
    public final static int ERR_AUTHO_FAILED =7;  // 授权失败
    public final static int ERR_NOT_ENOUGH_CHANNEL =8;  // 频道资源不足
    public final static int ERR_SERVER_ERROR =9;  // 服务器错误
    public final static int ERR_OPEN_RECORD =10;  // 打开采集失败
    public final static int ERR_OPEN_PLAYOUT =11;  // 打开播放失败
    public final static int ERR_RECORD_PERMISSION =12;  // 没有录音权限

    public final static int ERR_UNDEFINED =100;  // 未定义错误


    public final static  int IID_USERS    = 0x01;
    public final static  int IID_AUDIO    = 0x02;
    public final static  int IID_RTCMSGR  = 0x10;
    public final static  String RESERVE_CHANNEL_ATTR_CONFIG ="##INNER##CHANNEL_SATTR_CONFIG";  //设置房间内部属性，保留用来设置内部服务器


    public final static int STATUS_NONE = 0;     // 未登录
    public final static int STATUS_LOGINING = 1; // 登录中
    public final static int STATUS_LOGINED  = 2; // 已经登录

    public final static int typeText = 1;         // 文本消息, SDK不改消息原始内容，只是建议各个平台统一采用utf8格式，以避免调用SDK的双方不统一形成乱码
    public final static int typeAudio = 2;         // 语音消息
    public final static int typeBinary = 3;      // 二进制消息
    public final static int typeCmd = 10;         // 命令消息，不在服务器上存储，仅仅转发

    public static final int VIDEO_CAPTURE_TYPE_16X9 = 0x000;
    public static final int VIDEO_SIZE_160 = 1;
    public static final int VIDEO_SIZE_320 = 2;
    public static final int VIDEO_SIZE_480 = 3;
    public static final int VIDEO_SIZE_640 = 4;
    public static final int VIDEO_SIZE_800 = 5;
    public static final int VIDEO_SIZE_960 = 6;
    public static final int VIDEO_SIZE_1280 = 8;
    public static final int VIDEO_SIZE_1920 = 0xf;

    // normal
    public static final int VIDEO_FRAME_COUNT_NORMAL = 0; //15 ;
    public static final int VIDEO_FRAME_COUNT_HIGH = 1; //20 ;
    public static final int VIDEO_FRAME_COUNT_MAX = 2; // 30

    public static final int DEF_VIDEO_CODEC_TYPE = 0; // x264 VIDEO_CODEC_TYPE_H264
    public static final int VIDEO_CODEC_TYPE_H264 = 1;
    public static final int VIDEO_CODEC_TYPE_X264 = 2;
    public static final int VIDEO_CODEC_TYPE_HARD264 = 3; // 硬件编码
    public static final int VIDEO_CODEC_TYPE_VP8 = 4;
    public static final int VIDEO_CODEC_TYPE_VP9 = 8;
    public static final int VIDEO_CODEC_TYPE_HIGH264 = 9;

    public static final int VIDEO_CAPTURE_TYPE_16X9_160 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_160;   // 160*120
    public static final int VIDEO_CAPTURE_TYPE_16X9_320 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_320;
    public static final int VIDEO_CAPTURE_TYPE_16X9_480 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_480;
    public static final int VIDEO_CAPTURE_TYPE_16X9_640 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_640;
    public static final int VIDEO_CAPTURE_TYPE_16X9_800 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_800;
    public static final int VIDEO_CAPTURE_TYPE_16X9_960 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_960;
    public static final int VIDEO_CAPTURE_TYPE_16X9_1280 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_1280;   // 1280*720
    public static final int VIDEO_CAPTURE_TYPE_16X9_1920 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_1920;


    public static final int VIDEO_CAPTURE_TYPE_4X3 = 0x100;
    public static final int VIDEO_CAPTURE_TYPE_4X3_160 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_160;   // 160*120
    public static final int VIDEO_CAPTURE_TYPE_4X3_320 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_320;
    public static final int VIDEO_CAPTURE_TYPE_4X3_480 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_480;
    public static final int VIDEO_CAPTURE_TYPE_4X3_640 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_640;
    public static final int VIDEO_CAPTURE_TYPE_4X3_800 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_800;
    public static final int VIDEO_CAPTURE_TYPE_4X3_960 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_960;
    public static final int VIDEO_CAPTURE_TYPE_4X3_1280 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_1280;   // 1280*720
    public static final int VIDEO_CAPTURE_TYPE_4X3_1920 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_1920;   // 1280*720

    public static final int VIDEO_CAPTURE_TYPE_16X16 = 0x200;

  //  #define NO_ROTATE_TYPE_90               ( 1 )

    public static int CAMERA_INDEX_FRONT  =  -1; //[DFT]  //windows [DFT] >= 0 windows
    public static int CAMERA_INDEX_BACK   =  -2;
    public static int CAMERA_INDEX_SWITCH  = -3;  //移动端，-3 切换， windows -3 逐个遍历


    private long           mInst        = 0;


    com.rtc.client.IRtcChannel mAudioChannel = null;
    io.media.sdk.xRTCEngine  mVideoEngine = null;
    xEventHandlerImp mEventHandler = null;
    boolean mDisableAudio = false;
    boolean mDisableVideo = false;

    protected class xEventHandlerImp extends io.media.sdk.xRTCEventHandler implements com.rtc.client.IRtcSink   {
        private IRtcSink        mSink       = null;
        private long           mUserData    = 0;
        private int             mStatus = STATUS_NONE;

        public void SetSink(IRtcSink sink, long userdata){
            mSink = sink;
            mUserData=userdata;
        };

        public int GetLoginStatus(){
            return mStatus;
        }

        public void Respond(int type, int ec, Object ob, long userdata){
            if(com.rtc.client.IRtcChannel.RespondLogin == type){
                if(0 == ec)
                    mStatus = STATUS_LOGINED;

                objRespLogin impl = new objRespLogin();
                impl.userid = ((com.rtc.client.object_login)ob).getUserid();
                mSink.Respond(IRtcSink.RTC_EVTID_RESP_LOGINED, ec, impl, mUserData);
            }
            else if(com.rtc.client.IRtcMsger.RespondSendMessage == type){
                objRespMsg impl = new objRespMsg();
                impl.m_msg = (com.rtc.client.object_msg)ob;
                mSink.Respond(IRtcSink.RTC_EVTID_RESP_SEND_MSG, ec, impl, mUserData);
            }
            else if(com.rtc.client.IRtcChannel.RespondChannelAttr == type){
                objRespSetChannelAttr impl = new objRespSetChannelAttr((com.rtc.client.object_channel_attr)ob);
                mSink.Respond(IRtcSink.RTC_EVTID_RESP_SET_CHANNEL_ATTR, ec, impl, mUserData);
            }
            else if(com.rtc.client.IRtcUsers.RespondUserAttr == type){
                objRespSetUserAttr impl = new objRespSetUserAttr((com.rtc.client.object_user_attr)ob);
                mSink.Respond(IRtcSink.RTC_EVTID_RESP_SET_USER_ATTR, ec, impl, mUserData);
            }
        }

        public void Notify(int type, Object ob, long userdata){
            if(com.rtc.client.IRtcUsers.NotifyUserEnterChannel == type){
                objNtfUserEnter impl = new objNtfUserEnter();
                impl.userid = ((com.rtc.client.object_user)ob).getUserID();
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_USER_ENTER, impl, mUserData);
            }
            if(com.rtc.client.IRtcUsers.NotifyUserLeaveChannel == type){
                objNtfUserLeave impl = new objNtfUserLeave();
                impl.userid = ((com.rtc.client.object_userid)ob).getUserID();
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_USER_LEAVE, impl, mUserData);
            }
            else if(com.rtc.client.IRtcMsger.NotifyRecvedMessage == type){
                objNtfMsg impl = new objNtfMsg();
                impl.m_msg = (com.rtc.client.object_msg)ob;
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_RECV_MSG, impl, mUserData);
            }
            else if(com.rtc.client.IRtcChannel.NotifyConnectionLost == type){
                mStatus = STATUS_LOGINING;
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_CONNECT_LOST, null, mUserData);
            }
            else if(com.rtc.client.IRtcChannel.NotifyReConnected == type){
                mStatus = STATUS_LOGINED;
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_CONNECT_RESUME, null, mUserData);
            }
            else if(com.rtc.client.IRtcChannel.NotifyDuplicateLogined == type){
                mStatus = STATUS_NONE;
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_DUP_LOGINED, null, mUserData);
            }
            else if(com.rtc.client.IRtcChannel.NotifyChannelAttr == type){
                objNtfSetChannelAttr impl = new objNtfSetChannelAttr((com.rtc.client.object_channel_attr)ob);
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_SET_CHANNEL_ATTR, impl, mUserData);
            }
            else if(com.rtc.client.IRtcUsers.NotifyUserAttr == type){
                objNtfSetUserAttr impl = new objNtfSetUserAttr((com.rtc.client.object_user_attr)ob);
                mSink.Notify(IRtcSink.RTC_EVTID_NTF_SET_USER_ATTR, impl, mUserData);
            }
        }

        public  void onUserJoined(long uid, int elapsed) {
            objNtfUserEnter impl = new objNtfUserEnter();
            impl.userid =  String.valueOf(uid);
            mSink.Notify(IRtcSink.RTC_EVTID_NTF_USER_ENTER, impl, mUserData);
        }

        public void onUserOffline(long uid, int elapsed) {
            objNtfUserLeave impl = new objNtfUserLeave();
            impl.userid =  String.valueOf(uid);
            mSink.Notify(IRtcSink.RTC_EVTID_NTF_USER_LEAVE, impl, mUserData);
        }

        public void onRtcStats(int stats)
        {

        }

        public void onLeaveChannel(int stats)
        {
            mStatus = STATUS_NONE;
        }

        public void onJoinChannelSuccess(String channel, long uid, int elapsed)
        {
            mStatus = STATUS_LOGINED;
            objRespLogin impl = new objRespLogin();
            impl.userid = String.valueOf(uid);
            mSink.Respond(IRtcSink.RTC_EVTID_RESP_LOGINED, 0, impl, mUserData);
        }

        public void onRejoinChannelSuccess(String channel, long uid, int elapsed)
        {
            mStatus = STATUS_LOGINED;
        }
    }

    protected IRtcChannel(boolean withvideo, Context ctx) {
        mEventHandler = new xEventHandlerImp();
        if(withvideo){
            mVideoEngine = io.media.sdk.xRTCEngine.create(ctx, "", mEventHandler);
        }
        else{
            mAudioChannel = com.rtc.client.ValleyRtcAPI.CreateChannel();
            mAudioChannel.RegisterRtcSink(mEventHandler, 0);
        }
    }

    public void Release() {

        if(null != mAudioChannel){
            mAudioChannel.finalize();
            mAudioChannel = null;
        }
        else if(null != mVideoEngine){
            io.media.sdk.xRTCEngine.destroy();
            mVideoEngine = null;
        }
    }

    public void RegisterRtcSink(IRtcSink sink, long userdata){ mEventHandler.SetSink(sink, userdata);}
    public int EnableInterface(int iids){  return mAudioChannel.EnableInterface(iids); }

    public int Login(String channelid, String userid) { //登录房间
        int ec = 0;
        mEventHandler.mStatus = STATUS_LOGINING;
        if(null != mAudioChannel){
            ec = mAudioChannel.Login(channelid, userid, "");
        }
        else{
            int nUserid = Integer.parseInt(userid);

            if(!mDisableAudio)
                mVideoEngine.enableAudio();

            if(!mDisableVideo)
                mVideoEngine.enableVideo();

            ec = TransLateVideoEcode(mVideoEngine.joinChannel(null, Long.valueOf(channelid), ValleyRtcAPI.mAuthKey, nUserid));
        }

        if(0 != ec)
            mEventHandler.mStatus = STATUS_NONE;

        return ec;
    }

    public void  Logout(){         //退出房间
        mEventHandler.mStatus = STATUS_NONE;
        if(null != mAudioChannel){
            mAudioChannel.Logout();
        }
        else{
            mVideoEngine.leaveChannel();
        }
    }


    public int   GetLoginStatus(){
        return mEventHandler.GetLoginStatus();
    }


    private int TransLateVideoEcode(int ec){
        return ERR_SUCCEED;
    }

    public int SendMsgr(int msgtype, String msg, String token, String toUserID) {  // 发送消息

        if(null != mAudioChannel){
            com.rtc.client.IRtcMsger msgr = (com.rtc.client.IRtcMsger)mAudioChannel.GetInterface(IID_RTCMSGR);
            if(null == msgr)
                return ERR_NOT_INITIALIZE;
            return msgr.SendMsgr(msgtype, msg, token, toUserID);
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int SetVideoProfile(int profile) {    // 设置本地视频属性
        if(null != mVideoEngine){
            return TransLateVideoEcode(mVideoEngine.setVideoProfile(profile, false));
        }
        else{
            return ERR_UNSUPPORT;
        }
    }


    public int  SetLocalVideo(hvideo_t hVideo) {   // 设置本地视频显示窗口
        if(null != mVideoEngine){
            io.media.sdk.xRTCVideo.xRTCVideoCanvas cvs = new io.media.sdk.xRTCVideo.xRTCVideoCanvas(hVideo.view, hVideo.rendertype, 0);
            cvs.setLocal(true);

            return TransLateVideoEcode(mVideoEngine.setupLocalVideo(cvs));
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  RemoveLocalVideo() {    // 关闭本地视频显示
        if(null != mVideoEngine){
            return TransLateVideoEcode(mVideoEngine.setupLocalVideo(null));
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  SetUserVideo(String userid, hvideo_t hVideo) {   // 设置用户视频显示窗口

        if(null != mVideoEngine){
            long nUserid = Long.parseLong(userid);

            io.media.sdk.xRTCVideo.xRTCVideoCanvas cvs = new io.media.sdk.xRTCVideo.xRTCVideoCanvas(hVideo.view, hVideo.rendertype, nUserid);
            cvs.setLocal(false);
            mVideoEngine.setupRemoteVideo(cvs);
            return ERR_SUCCEED;
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  RemoveUserVideo(String userid) {   // 关闭用户视频显示
        if(null != mVideoEngine){
            long nUserid = Long.parseLong(userid);
            mVideoEngine.removeRemoteCanvas(nUserid);
            return ERR_SUCCEED;
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  EnableLocalAudio(boolean bEnable) {   // 关闭或打开 本地语音
        if(null != mVideoEngine){
            return TransLateVideoEcode(mVideoEngine.muteLocalAudioStream(!bEnable));
        }
        else{
            com.rtc.client.IRtcAudio pAudio = (com.rtc.client.IRtcAudio) mAudioChannel.GetInterface(IID_AUDIO);
            if(null == pAudio) {
                return ERR_UNSUPPORT;
            }
            return pAudio.EnableSpeak(bEnable);
        }
    }

    public int  EnableLocalVideo(boolean bEnable) {   // 关闭或打开 本地视频
        if(null != mVideoEngine){
            return TransLateVideoEcode(mVideoEngine.muteLocalVideoStream(!bEnable));
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  EnableRemoteAudio(String userid, boolean bEnable) {  // 关闭或打开用户语音
        if(null != mVideoEngine){
            long nUserid = Long.parseLong(userid);
            return TransLateVideoEcode(mVideoEngine.muteRemoteAudioStream(nUserid, !bEnable));
        }
        else{
            com.rtc.client.IRtcAudio pAudio = (com.rtc.client.IRtcAudio) mAudioChannel.GetInterface(IID_AUDIO);
            if(null == pAudio) {
                return ERR_UNSUPPORT;
            }
            return pAudio.BlockUser(userid, !bEnable);
        }
    }

    public int  EnableRemoteVideo(String userid, boolean bEnable) {  // 关闭或打开用户视频
        if(null != mVideoEngine){
            long nUserid = Long.parseLong(userid);
            return TransLateVideoEcode(mVideoEngine.muteRemoteVideoStream(nUserid, !bEnable));
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public int  DisableAudio() {  // 房间支持语音，默认支持, 只能在登录前调用一次，否则无效
        if(STATUS_NONE != GetLoginStatus())
            return ERR_ALREADY_RUN;
        mDisableAudio = true;
        return ERR_SUCCEED;
    }

    public int  DisableVideo() { // 房间支持视频，默认支持, 只能在登录前调用一次，否则无效
        if(STATUS_NONE != GetLoginStatus())
            return ERR_ALREADY_RUN;
        mDisableVideo = true;
        return ERR_SUCCEED;
    }

    public int  SetCameraIndex(){  // 设置摄像头
        if(null != mVideoEngine){

           // mVideoEngine.setVideoCamera()
            return TransLateVideoEcode(0);
        }
        else{
            return ERR_UNSUPPORT;
        }
    }


    public int SetChannelAttr(String name, String value) {  // 设置房间属性
        if(null != mAudioChannel){
            return mAudioChannel.SetChannelAttr(name, value);
        }
        else{
            return ERR_UNSUPPORT;
        }
    }


    public String GetChannelAttr(String name){  // 获取房间属性
        if(null != mAudioChannel){
            com.rtc.client.object_string str = new com.rtc.client.object_string();
            mAudioChannel.GetChannelAttr(name, str);
            return str.getValue();
        }
        else{
            return "";
        }
    }

    public int SetUserAttr(String uid, String name, String value){  // 设置用户属性
        if(null != mAudioChannel){
            com.rtc.client.IRtcUsers pUsers = (com.rtc.client.IRtcUsers) mAudioChannel.GetInterface(IID_USERS);
            if(null == pUsers)
                return  ERR_UNSUPPORT;
            return pUsers.SetUserAttr(uid, name, value);
        }
        else{
            return ERR_UNSUPPORT;
        }
    }

    public String  GetUserAttr(String uid, String name){  //获取用户属性
        if(null != mAudioChannel){
            com.rtc.client.IRtcUsers pUsers = (com.rtc.client.IRtcUsers) mAudioChannel.GetInterface(IID_USERS);
            if(null == pUsers)
                return  "";

            com.rtc.client.object_string str = new com.rtc.client.object_string();
            pUsers.GetUserAttr(uid, name, str);
            return str.getValue();
        }
        else{
            return "";
        }
    }

    public objUser GetUser(String uid){  // 获取用户
        if(null != mAudioChannel){
            com.rtc.client.IRtcUsers pUsers = (com.rtc.client.IRtcUsers) mAudioChannel.GetInterface(IID_USERS);
            if(null == pUsers)
                return  null;

            com.rtc.client.object_user ins = new com.rtc.client.object_user();
            int ec = pUsers.GetUser(uid, ins);
            if(0 != ec)
                return null;

            return new objUser(ins);
        }
        else{
            return null;
        }
    }

    public objUserList GetUserList(){  // 获取用户列表
        if(null != mAudioChannel){
            com.rtc.client.IRtcUsers pUsers = (com.rtc.client.IRtcUsers) mAudioChannel.GetInterface(IID_USERS);
            if(null == pUsers)
                return  null;

            objUserList ins = new objUserList();
            int ec = pUsers.GetUserList(ins.list);
            if(0 != ec)
                return null;
            return ins;
        }
        else{
            return null;
        }
    }
}

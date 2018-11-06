package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */

public interface IRtcSink {
    public final static int RTC_EVTID_RESP_FROM = 0;
    public final static int RTC_EVTID_RESP_LOGINED       = (RTC_EVTID_RESP_FROM + 1);		     // objRespLogin 响应事件 收到用户登录成功或失败的应答
    public final static int RTC_EVTID_RESP_SET_CHANNEL_ATTR = (RTC_EVTID_RESP_FROM + 3);  //响应事件 收到发消息成功或失败的应答 objRespSetChannelAttr
    public final static int RTC_EVTID_RESP_SET_USER_ATTR = (RTC_EVTID_RESP_FROM + 4);  //响应事件 收到发消息成功或失败的应答 objRespSetUserAttr
    public final static int RTC_EVTID_RESP_SEND_MSG      = (RTC_EVTID_RESP_FROM + 50);			 // objRespMsg 响应事件 收到发消息成功或失败的应答

    public final static int RTC_EVTID_NTF_FROM = 1000;
    public final static int RTC_EVTID_NTF_USER_ENTER = (RTC_EVTID_NTF_FROM + 1);   //通知事件 收到用户进入房间 objNtfUserEnter
    public final static int RTC_EVTID_NTF_USER_LEAVE = (RTC_EVTID_NTF_FROM + 2);   //通知事件 收到用户离开房间 objNtfUserLeave
    public final static int RTC_EVTID_NTF_SET_CHANNEL_ATTR = (RTC_EVTID_NTF_FROM + 3);  //通知事件 收到设置房间属性的通知 objNtfSetChannelAttr
    public final static int RTC_EVTID_NTF_SET_USER_ATTR = (RTC_EVTID_NTF_FROM + 4);  //通知事件 收到设置用户属性的通知 objNtfSetUserAttr
    public final static int RTC_EVTID_NTF_RECV_MSG = (RTC_EVTID_NTF_FROM + 50);  //通知事件 收到用户发来的消息 objNtfMsg
    public final static int RTC_EVTID_NTF_CONNECT_LOST = (RTC_EVTID_NTF_FROM + 80);    //通知事件 收到网络断开的消息 null//objNtfNetLost
    public final static int RTC_EVTID_NTF_CONNECT_RESUME = (RTC_EVTID_NTF_FROM + 81);  //通知事件 收到网络重新连上的消息  null//objNtfNetResume
    public final static int RTC_EVTID_NTF_DUP_LOGINED = (RTC_EVTID_NTF_FROM + 82); //通知事件 收到账号异地登录的消息  null//objNtfDupLogined

    public abstract void Respond(int type, int ec, Object ob, long userdata);
    public abstract void Notify(int type, Object ob, long userdata);
}

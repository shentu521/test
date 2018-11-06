package io.media.sdk;

/**
 * Created by sunhui on 2017/9/5.
 */

public class xRTCMessage {

    // m_MsgSubType  选项
    public final int MSG_TYPE_PRIVATE  =   0 ; // 私人消息
    public final int MSG_TYPE_GROUP    =   1 ; // 群消息
    public final int MSG_TYPE_SYS      =   2 ; // 系统消息
    public final int MSG_TYPE_MEDIA    =   3 ; // 消息

    public int      m_ClientID ;        // 客户端保存的消息id
    public long     m_ToUserID ;        // 目标id
    public long     m_MessageID ;       // 消息id
    public long     m_FromUserID ;      // 来源id
    public long     m_GroupID ;         // 群id/房间id
    public long     m_MsgTime ;         // 时间戳 服务器生成
    public int      m_SendUserOS ;      // 发送系统 sdk内部生成，只读
    public byte     m_MsgType ;         // 消息类型 客户端使用，
    public byte     m_MsgSubType ;      // 子类型 客户端设置
    public byte []  m_Contextstring ;   // 消息内容

}

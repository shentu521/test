package org.vallery.videochatdemoandroid;

import android.content.Context;

import valley.api.IRtcChannel;



/**
 * pc 控制端
 */

public class DispatchAPIControlImpl extends DispatchAPIImpl {

    DispatchAPIControlImpl(Context context) {
        super(context);
        m_usertype = user_role.ROLE_CONTROL;
    }

    //业务逻辑:一个组内只允许存在一个高优先级的用户，设置后该用户变成高优先级，其它用户都是低优先级
    public int SetUserPriority(int userid) {
        if(!ChatRoomIsLogined())
        {
            return IRtcChannel.ERR_NOT_LOGINED;
        }

        //自己是大厅，才有权限
        if(m_usertype != USER_TYPE_VALUE_PC)
        {
            return DISP_ERROR.ECODE_NO_PERMISSION;
        }

        return m_chat_channel.SetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID),Integer.toString(userid));
    }


    //取消用户优先级，如果存在，那么取消
    public int RemoveUserPriority() {

        if(!ChatRoomIsLogined())
        {
            return IRtcChannel.ERR_NOT_LOGINED;
        }

        //自己是大厅，才有权限
        if(m_usertype != USER_TYPE_VALUE_PC)
        {
            return DISP_ERROR.ECODE_NO_PERMISSION;
        }

        return m_chat_channel.SetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID),"");
    }

    //直接把某个用户拉大某个对讲组里面
    public int ChangeUserToGroup(int userid) {
        return 0;
    }

    //踢人，将用户踢出对讲组或视频房间组
    public int KickOff(int userid) {

        if(!ChatRoomIsLogined())
        {
            return IRtcChannel.ERR_NOT_LOGINED;
        }

        if(m_usertype != USER_TYPE_VALUE_PC)
        {
            return DISP_ERROR.ECODE_NO_PERMISSION;
        }


        return SendCommand(ROOM_CHAT_CMD_KICKOFF,"","",Integer.toString(userid),true);
    }



    int SendCommand(int cmd,String content,String token,String toUserID,boolean bChatRoom){

        String msgcmd = String.format("c=%d&m=%s",cmd,content);

        if(bChatRoom)
        {
            return m_chat_channel.SendMsgr(IRtcChannel.typeCmd,msgcmd,token,toUserID);
        }
        else
        {
            return m_im_channel.SendMsgr(IRtcChannel.typeCmd,msgcmd,token,toUserID);
        }
    }

}

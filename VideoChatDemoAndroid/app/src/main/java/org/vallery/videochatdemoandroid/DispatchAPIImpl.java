package org.vallery.videochatdemoandroid;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import valley.api.IRtcChannel;
import valley.api.IRtcSink;
import valley.api.ValleyRtcAPI;
import valley.api.objNtfMsg;
import valley.api.objNtfSetChannelAttr;
import valley.api.objNtfUserEnter;
import valley.api.objNtfUserLeave;
import valley.api.objRespLogin;
import valley.api.objRespSetChannelAttr;
import valley.api.objRespSetUserAttr;
import valley.api.objUser;
import valley.api.objUserList;


public class DispatchAPIImpl implements  IDispachBaseAPI, IRtcSink {


    public static final int USER_DATA_IM_CHANNEL = 2;
    public static final int USER_DATA_CHAT_CHANNEL = 3;


    public static final int  USRE_TYPE_VALUE_NORMAL   =             0    ;   //普通用户，默认，不用设置
    public static final int  USER_TYPE_VALUE_HIGHT    =             3    ;   //高优先级用户
    public static final int  USER_TYPE_VALUE_PC       =             5    ; //用户类型大厅

    //对讲组房间属性定义
    public static final int  ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID  =     1    ;//麦上用户ID,可以发言的用户
    public static final int  ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID  =     2    ;

    //信令房间用户属性
    public static final int ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE  =  0    ;//用户类型 USER_TYPE_PC USRE_TYPE_NORMAL
    public static final int ROOM_GLOBAL_USER_ATTR_KEY_USER_AGID  =  1    ;//用户在哪个对讲组里面，可以通过global属性指定
    public static final int ROOM_GLOBAL_USER_ATTR_KEY_USER_VCID  =  2    ;//用户在哪个视频房间里面
    public static final int ROOM_GLOBAL_USER_ATTR_KEY_ON_CHATMIC =  3    ;//用户在对讲组mic上 0,1
    public static final int ROOM_GLOBAL_USER_ATTR_KEY_ON_VIDEO   =  4    ;//用户在视频中 0,1
    public static final int ROOM_GLOBAL_USER_ATTR_MAXCOUNT       =  5    ;//最大数量

    public static final int ROOM_CHAT_CMD_KICKOFF                =  1    ;


    public static final String IMROOMID = "998";


    class tChatRoomAttr {
        public int chatMicUserID; //组内正在讲话的人
        public int priUserID;//组内的高优先级用户
        public boolean bMicOpened;//是否允许打开麦
    }


    protected int m_userid;
    protected int m_chatroomid;
    protected int m_usertype;

    protected IRtcChannel m_im_channel;
    protected IRtcChannel m_chat_channel;

    //所有在线用户列表
    protected IUserListImpl m_im_userlist;

    //组内用户在线列表
    protected IUserListImpl m_chat_userlist;

    IEventCallback m_pCallback = null;

    protected tChatRoomAttr m_chatroomAttr;

    private Context mContext;

    static boolean m_bNotifyImUserStatus  = false;

    DispatchAPIImpl(Context context)
    {

        mContext = context;

        m_userid = m_chatroomid = 0;

        m_usertype = user_role.ROLE_NORMAL;//普通用户
        m_im_userlist = new IUserListImpl();
        m_chat_userlist = new IUserListImpl();

        m_chatroomAttr = new tChatRoomAttr();

        //初始化im channel

        m_im_channel = ValleyRtcAPI.CreateChannel(false,context);
        m_im_channel.RegisterRtcSink(this,USER_DATA_IM_CHANNEL);
        m_im_channel.EnableInterface(IRtcChannel.IID_RTCMSGR|IRtcChannel.IID_USERS|IRtcChannel.IID_AUDIO);

        m_chat_channel = null;
    }

    /**
     * 当前用户是否在讲话
     * @return
     */
    public boolean isSpeaking() {

        if(m_userid == m_chatroomAttr.chatMicUserID)
        {
            return true;
        }
        return false;

    }

    /**
     * 用户角色
     * @return
     */

    public int getRole() {
        return m_usertype;
    }


    protected boolean ChatRoomIsLogined() {
        return null != m_chat_channel && IRtcChannel.STATUS_LOGINED == m_chat_channel.GetLoginStatus();
    }

    private void CheckChatRoomSpeak() {

        boolean bOpenMic = m_chatroomAttr.chatMicUserID == m_userid;
        if(bOpenMic == m_chatroomAttr.bMicOpened)
        {
            return;
        }

        if( 0 == m_chat_channel.EnableLocalAudio(bOpenMic))
            m_chatroomAttr.bMicOpened = bOpenMic;

        if(m_bNotifyImUserStatus)
        {
            m_im_channel.SetUserAttr(Integer.toString(m_userid),Integer.toString(ROOM_GLOBAL_USER_ATTR_KEY_ON_CHATMIC),
                    bOpenMic?"1":"");
        }

    }


    @Override
    public void SetCallback(IEventCallback pCallback) {
        m_pCallback = pCallback;
    }

    @Override
    public int Start(int userid) {

        if(IRtcChannel.STATUS_NONE != m_im_channel.GetLoginStatus())
        {
            return IRtcChannel.ERR_ALREADY_RUN;
        }

        m_userid = userid;

        //设置自己的属性，可以在登录之前设置
//        int value = 0;
//        if(m_usertype == user_role.ROLE_NORMAL)
//            value = 0;
//        else if(m_usertype == user_role.ROLE_HIGHT)
//            value = 3;
//        else if(m_usertype == user_role.ROLE_CONTROL)
//            value = 5;

        int ec = m_im_channel.SetUserAttr("",Integer.toString(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE),
                Integer.toString(m_usertype));

        return m_im_channel.Login(IMROOMID,Integer.toString(userid));
    }

    @Override
    public void Stop() {

        if(IRtcChannel.STATUS_NONE == m_im_channel.GetLoginStatus())
        {
            return;
        }

        if(m_chat_channel != null)
        {
            m_chat_channel.Logout();
            m_chat_channel.Release();
            m_chat_channel = null;
        }

        m_im_channel.Logout();
    }

    @Override
    public int EnterGroup(int groupid) {

        if(null != m_chat_channel)
            return IRtcChannel.ERR_ALREADY_RUN;

        m_chat_channel = ValleyRtcAPI.CreateChannel(false,mContext);
        m_chat_channel.RegisterRtcSink(this,USER_DATA_CHAT_CHANNEL);
        m_chat_channel.EnableInterface(IRtcChannel.IID_RTCMSGR|IRtcChannel.IID_USERS| IRtcChannel.IID_AUDIO);

        m_chatroomid = groupid;

        int ec = m_chat_channel.Login(Integer.toString(m_chatroomid) , Integer.toString(m_userid));
        if(IRtcChannel.ERR_SUCCEED != ec)
        {
            m_chat_channel.Release();
            m_chat_channel = null;
        }

        return IRtcChannel.ERR_SUCCEED;
    }

    @Override
    public void LeaveGroup() {

        if(null == m_chat_channel)
            return;

        this.StopSpeak();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        m_chat_channel.Logout();
        m_chat_channel.Release();
        m_chat_channel = null;
    }

    @Override
    public int StartSpeak() {

        if(!ChatRoomIsLogined())
        {
            return IRtcChannel.ERR_NOT_LOGINED;
        }

        //首先，看看是否有人在麦上
        if(m_chatroomAttr.chatMicUserID == m_userid)
        {
            this.CheckChatRoomSpeak();
            return IRtcChannel.ERR_SUCCEED;
        }

        if(  0 != m_chatroomAttr.chatMicUserID ) //当前又用户占麦
        {
            objUser Micuser = m_im_channel.GetUser(Integer.toString(m_chatroomAttr.chatMicUserID));


            //检测麦上用户
            if( null != Micuser )
            {
                //用户在线的话，看优先级
                int micutype = 0;

                //当前占麦的用户的优先级:普通/高优先级
  //              int utype = Integer.parseInt( user.attr(Integer.toString(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE)) ) ;

//                String utypeStr = m_im_channel.GetUserAttr( Integer.toString(m_chatroomAttr.chatMicUserID),Integer.toString(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE));
//                int utype = utypeStr.equals("") ? 0 : Integer.parseInt(utypeStr) ;

                IUserImpl user = (IUserImpl)m_im_userlist.getUser(m_chatroomAttr.chatMicUserID);

                int utype = user.GetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE);
                if(utype == user_role.ROLE_NORMAL) //普通用户
                {
                    if(m_chatroomAttr.priUserID == m_chatroomAttr.chatMicUserID)
                        micutype = user_role.ROLE_NORMAL+1;
                    else
                        micutype = user_role.ROLE_NORMAL;
                }
                else if(utype == user_role.ROLE_HIGHT) //高优先级用户
                {
                    if(m_chatroomAttr.priUserID == m_chatroomAttr.chatMicUserID)
                        micutype = user_role.ROLE_HIGHT+ 1;
                    else
                        micutype = user_role.ROLE_HIGHT;
                }
                else
                {
                    micutype = user_role.ROLE_CONTROL; //ps是最高等级,只有等用户释放麦了才能去占麦
                }

                if(m_usertype <= micutype)
                    return DISP_ERROR.ECODE_MIC_INUSED;

            }
        }

        //设置麦上的人为自己
        return m_chat_channel.SetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID),Integer.toString(m_userid));
    }

    @Override
    public int StopSpeak() {

        if(!ChatRoomIsLogined())
            return IRtcChannel.ERR_NOT_LOGINED;

        //首先，看看是否有人在麦上
        if( 0 == m_chatroomAttr.chatMicUserID )
        {
            return IRtcChannel.ERR_SUCCEED;
        }

        //只有自己才能下麦
        if(m_chatroomAttr.chatMicUserID != m_userid)
            return DISP_ERROR.ECODE_NO_PERMISSION;

        //下麦
        return m_chat_channel.SetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID),"");
    }

    @Override
    public IUser GetUser(int userid) {
        return m_im_userlist.getUser(userid);
    }

    @Override
    public IUserList GetOnlineUserList() {
        return m_im_userlist;
    }

    @Override
    public IUserList GetUserListByGroup(int groupid) {
        return m_chat_userlist;
    }

    /**
     * 获取当前对讲组ID
     * @return
     */
    @Override
    public int GetCurrentGroupID() {

        if(null == m_chat_channel)
            return 0;

        return m_chatroomid;
    }

    @Override
    public void Release() {

        this.Stop();

        m_im_channel.Release();
        m_im_channel = null;

        ValleyRtcAPI.CleanSDK();

    }


    //定义内部类

    class IUserImpl implements  IUser {

        private int im_attr[];
        private int vchat_attr[];

        public int m_userid;

        IUserImpl(){
            im_attr = new int [ROOM_GLOBAL_USER_ATTR_MAXCOUNT];
            vchat_attr = new int [ROOM_GLOBAL_USER_ATTR_MAXCOUNT];
        }



        @Override
        public int role() {
            return im_attr[ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE];
        }

        @Override
        public int userid() {
            return m_userid;
        }

        @Override
        public int groupid() {
            return im_attr[ROOM_GLOBAL_USER_ATTR_KEY_USER_AGID];
        }

        @Override
        public boolean IsSpeaker() {
            return 0 != im_attr[ROOM_GLOBAL_USER_ATTR_KEY_ON_CHATMIC];
        }

        //method myself
        public boolean IsVideoChatting() {
            return 0 != im_attr[ROOM_GLOBAL_USER_ATTR_KEY_ON_VIDEO];
        }

        public void SetAttr(int name ,int value){
            if(name < ROOM_GLOBAL_USER_ATTR_MAXCOUNT)
                im_attr[name] = value;
        }

        public int GetAttr(int name){
            if(name < ROOM_GLOBAL_USER_ATTR_MAXCOUNT)
                return im_attr[name];
            else
                return 0;
        }

        public void ClearAttrs(boolean bIm){

            if(bIm)
            {
                for(int index = 0 ; index < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; index++)
                    im_attr[index] = 0;
            }
            else
            {
                for(int index = 0 ; index < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; index++)
                    vchat_attr[index] = 0;
            }

        }

    }


    class IUserListImpl implements  IUserList {

        private boolean m_bFromIM = false;
        private List<IUser> users;
        public int m_userid = 0;

        IUserListImpl()
        {
            users = new ArrayList<>();
            m_bFromIM = true;
        }

        @Override
        public int count() {
            return users.size();
        }

        @Override
        public IUser item(int index) {
            return users.get(index);
        }

        //method myself
        public IUser getUser(int userid){
            int count  = count();
            for(int i = 0 ; i < count ; i++)
            {
                IUser item = item(i);
                if(item.userid() == userid)
                {
                    return item;
                }
            }
            return null;
        }

        public void addUser(IUser user){
            users.add(user);
        }

        public boolean removeUser(int userid) {

            int count = count();
            for (int i = 0; i < count; i++) {
                IUser item = item(i);
                if (item.userid() == userid) {
                    users.remove(item);
                    return true;
                }
            }

            return false;
        }

        public void clear() {
            users.clear();
        }
    }



    //处理业务逻辑
    @Override
    public void Respond(int type, int ec, Object ob, long userdata) {

        if(m_im_channel != null && userdata == USER_DATA_IM_CHANNEL)
        {
            OnProcessIMRespond(type,ec,ob,userdata);
        }

        if(m_chat_channel != null && userdata == USER_DATA_CHAT_CHANNEL)
        {
            OnProcessChatRoomRespond(type,ec,ob,userdata);
        }

    }

    @Override
    public void Notify(int type, Object ob, long userdata) {

        if(m_im_channel != null && userdata == USER_DATA_IM_CHANNEL)
        {
            OnProcessIMNotify(type, ob,userdata);
        }

        if(m_chat_channel != null && userdata == USER_DATA_CHAT_CHANNEL)
        {
            OnProcessChatRoomNotify(type,ob);
        }
    }


    //////////////////// 处理Chat Channel Response/Notify //////////////////

    private int GetChatRoomUserRole(int userid){

        int usertype = m_usertype;
        if(userid != m_userid)
        {
            IUserImpl user = (IUserImpl)m_im_userlist.getUser(userid);
            if(null == user)
            {
                usertype = USRE_TYPE_VALUE_NORMAL;
            }
            else
            {
                usertype = user.GetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE);
            }
        }

        return usertype;
    }

    private void OnProcessChatRoomNotify(int type, Object ob) {

        switch (type)
        {
            case IRtcSink.RTC_EVTID_NTF_USER_ENTER:
            {
                objNtfUserEnter ntf = (objNtfUserEnter)ob;
                int userid = Integer.parseInt( ntf.getUserid() ) ;

                //一个新用户--
                boolean bNewFlag = false;
                IUser user =  m_chat_userlist.getUser(userid);
                if(user == null)
                    bNewFlag = true;

                AddUser(userid,false);

                if(bNewFlag)
                    m_pCallback.OnJoinGroup(userid,m_chatroomid,GetChatRoomUserRole(userid));

                if(userid == m_chatroomAttr.chatMicUserID) //当前正在占麦的用户id
                {
                    m_pCallback.OnGroupSpeakerChanged(m_chatroomid,userid,true);
                }

                if(userid == m_chatroomAttr.priUserID)
                {
                    m_pCallback.OnUserPriorityChanged(userid,true);
                }
            }
                break;
            case IRtcSink.RTC_EVTID_NTF_USER_LEAVE:
            {
                objNtfUserLeave ntf = (objNtfUserLeave)ob;

                int userid = Integer.parseInt(ntf.getUserid());
                if(m_chat_userlist.removeUser(userid))
                    m_pCallback.OnLeaveGroup(userid,m_chatroomid);//只有同组的用户才能收到通知

            }
                break;
            case IRtcSink.RTC_EVTID_NTF_RECV_MSG:
            {
                objNtfMsg msg = (objNtfMsg)ob;
                OnProcessMessageNotify(msg);

            }
                break;
            case IRtcSink.RTC_EVTID_NTF_SET_CHANNEL_ATTR:
            {
                objNtfSetChannelAttr ntf = (objNtfSetChannelAttr)ob;
                ProcessChatRoomAttrChanged(ntf.getAttrName(),ntf.getAttrValue());
            }
                break;
        }

    }


    IUserImpl AddUser(int userid ,boolean bIm ) {


        boolean bNew = false;

        IUserListImpl mainList = bIm ? m_im_userlist : m_chat_userlist;
        IUserListImpl assList = bIm ? m_chat_userlist : m_im_userlist;

        IUserImpl user = (IUserImpl) mainList.getUser(userid);
        if(user != null)
        {
            bNew = false;
            return user;
        }
        else
        {
            bNew = true;

            user = (IUserImpl) assList.getUser(userid);
            if(user == null)
            {
                user = new IUserImpl();
                user.m_userid = userid;
                user.ClearAttrs(true);
                user.ClearAttrs(false);
            }
            mainList.addUser(user);
            return user;
        }
    }

    private void OnProcessChatRoomRespond(int type, int ec, Object ob, long userdata) {
        switch(type) {
            case IRtcSink.RTC_EVTID_RESP_LOGINED:
            {
                objRespLogin resp = (objRespLogin)ob;
                if(ec == IRtcChannel.ERR_SUCCEED)
                {
                    m_chat_userlist.clear();

                    objUserList objlist = m_chat_channel.GetUserList();
                    int size = objlist.size();
                    for(int i = 0 ; i < size ; i++)
                    {
                        objUser item = objlist.item(i);
                        AddUser(Integer.parseInt(item.getUserid()),false);
                    }

//                    for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
//                    {
//                        String value = m_im_channel.GetUserAttr(item.getUserid(),Integer.toString(indexName));
//                        Log.e("Dispatch","user id: " + item.getUserid() + "  Dispach indexName:"+indexName+" --value:"+value);
//                        user.SetAttr(indexName,value.equals("") ? 0 :  Integer.parseInt(value));
//                    }


                    if(m_bNotifyImUserStatus)
                    {
                        m_im_channel.SetUserAttr(resp.getUserid(),Integer.toString(ROOM_GLOBAL_USER_ATTR_KEY_USER_AGID),
                                Integer.toString(m_chatroomid));
                    }

                    String attrVal = m_chat_channel.GetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID));
                    int    attrIntVal = 0;
                    if(!attrVal.equals(""))
                        attrIntVal = Integer.parseInt(attrVal);

                    //m_chatroomAttr.priUserID = Integer.parseInt(m_chat_channel.GetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID)));
                    m_chatroomAttr.priUserID = attrIntVal; //当前组的高优先级用户是谁

                    //must reset first
                    attrIntVal = 0;
                    attrVal = m_chat_channel.GetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID));
                    if(!attrVal.equals(""))
                        attrIntVal = Integer.parseInt(attrVal);

                    //m_chatroomAttr.chatMicUserID = Integer.parseInt(m_chat_channel.GetChannelAttr(Integer.toString(ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID)));
                    m_chatroomAttr.chatMicUserID = attrIntVal;//当前组占麦的用户id



                    //update im list

                    //高优先级的用户
                    if(m_chatroomAttr.priUserID != 0)
                    {
                        if(m_userid == m_chatroomAttr.priUserID) //当前用户是高优先级用户
                        {
                            m_usertype = USER_TYPE_VALUE_HIGHT;
                        }
                        else
                        {
                            IUserImpl user = (IUserImpl)m_im_userlist.getUser(m_chatroomAttr.priUserID);
                            if(user  != null)
                                user.SetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE,USER_TYPE_VALUE_HIGHT);
                        }
                    }

                    //for test
//                    IUserImpl user = (IUserImpl)m_im_userlist.getUser(m_chatroomAttr.priUserID);
//
//                    int value = user.GetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE);
//                    Log.e("Test","value:"+value);

                    CheckChatRoomSpeak();

                    m_pCallback.OnJoinGroupResult(0);
                }
                else
                {
                    m_chat_channel.Logout();
                    m_chat_channel.Release();
                    m_chat_channel = null;

                    m_pCallback.OnJoinGroupResult(ec);
                }
            }
                break;
            case IRtcSink.RTC_EVTID_RESP_SET_CHANNEL_ATTR://设置大厅属性的回掉
            {
                objRespSetChannelAttr resp = (objRespSetChannelAttr)ob;
                ProcessChatRoomAttrChanged(resp.getAttrName(),resp.getAttrValue());

            }
                break;
        }
    }

    private void ProcessChatRoomAttrChanged(String attrname,String attrval){

        int attrNameIndex = Integer.parseInt(attrname);
        if(ROOM_CHAT_ROOM_ATTR_KEY_PRI_UID == attrNameIndex) //高等级用户属性发生改变
        {
            int priUserID = attrval.equals("") ? 0 : Integer.parseInt(attrval);

            if(priUserID == m_chatroomAttr.priUserID) //房间属性发生了变化--重复设置一个相同的用户为高等级
                return;

            int oldpriUserID = m_chatroomAttr.priUserID;
            m_chatroomAttr.priUserID = priUserID;

            //这里的逻辑必须整明白。
            if(m_chatroomAttr.priUserID == 0)
            {
                priUserID = oldpriUserID;//取消了高优先级
            }
            else
            {
                priUserID = m_chatroomAttr.priUserID;//设置了高优先级别
            }




            if( m_userid == priUserID ) //如果是更改自己的身份
            {
                if(m_usertype == USER_TYPE_VALUE_HIGHT)
                {
                    m_usertype = USRE_TYPE_VALUE_NORMAL;
                }
                else if(m_usertype == USRE_TYPE_VALUE_NORMAL)
                {
                    m_usertype = USER_TYPE_VALUE_HIGHT;
                }
                else
                {
                    //pc属性不管 -- pc的属性也无法调用到这里
                }

                //更新属性://上一次列表里的高优先级需要恢复为普通
                if(oldpriUserID != priUserID)
                {
                    IUserImpl user =  (IUserImpl)m_im_userlist.getUser(oldpriUserID);
                    if(user != null)
                        user.SetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE,USRE_TYPE_VALUE_NORMAL);
                }
            }
            else
            {
                //更新数组里的元素
                IUserImpl user =  (IUserImpl)m_im_userlist.getUser(priUserID);
                if( user.role() == USRE_TYPE_VALUE_NORMAL)
                {
                    user.SetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE,USER_TYPE_VALUE_HIGHT);
                    if(m_usertype == USER_TYPE_VALUE_HIGHT)
                    {
                        m_usertype = USRE_TYPE_VALUE_NORMAL;
                    }
                }
                else if( user.role() == USER_TYPE_VALUE_HIGHT )
                {
                    user.SetAttr(ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE,USRE_TYPE_VALUE_NORMAL);
                }

//                String value =  m_im_channel.GetUserAttr(Integer.toString(priUserID),ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE);
//                Log.e("V","V:"+value);
            }

            //pc 大厅属性不用管 ，无法切换
            if(0 != oldpriUserID)
                m_pCallback.OnUserPriorityChanged(oldpriUserID,false);

            if(0 != priUserID)
                m_pCallback.OnUserPriorityChanged(priUserID,true);

        }
        else if(ROOM_CHAT_ROOM_ATTR_KEY_MIC_UID == attrNameIndex) //占麦时间发生了改变
        {
            int newMicUserID = attrval.equals("")?0:Integer.parseInt(attrval);

            if(m_chatroomAttr.chatMicUserID == newMicUserID)//设置同一个人占麦
            {
                return;
            }

            m_chatroomAttr.chatMicUserID = newMicUserID;
            if( 0 != m_chatroomAttr.chatMicUserID)
            {
                m_pCallback.OnGroupSpeakerChanged(m_chatroomid,m_chatroomAttr.chatMicUserID,false);
            }

            CheckChatRoomSpeak();

            if( newMicUserID != 0)
            {
                m_pCallback.OnGroupSpeakerChanged(m_chatroomid,newMicUserID,true);
            }

        }
    }


    //////////////////// 处理IM Response/Notify /////////////////////////

    private void OnProcessIMNotify(int type, Object ob, long userdata){
        switch(type)
        {
            case IRtcSink.RTC_EVTID_NTF_USER_ENTER:
            {
                objNtfUserEnter ntf = (objNtfUserEnter)ob;

                int userid = Integer.parseInt(ntf.getUserid());

                boolean bNew = false;
                IUserImpl user = (IUserImpl) m_im_userlist.getUser(userid);
                if(user == null)
                    bNew = true;

                user = AddUser(userid,true);

//                IUserImpl user = (IUserImpl)m_im_userlist.getUser(userid);
//
                boolean attroldInit = false;
                int attrold[] = new int [ROOM_GLOBAL_USER_ATTR_MAXCOUNT];

                if(!bNew)
                {
                    attroldInit = true;
                    for(int indexName = 0;  indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
                    {
                        attrold[indexName] = user.GetAttr(indexName);
                    }
                }

                //update new attr
                for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
                {
                    String attr = m_im_channel.GetUserAttr(ntf.getUserid(),Integer.toString(indexName));
                    user.SetAttr(indexName,attr.equals("")?0:Integer.parseInt(attr));
                }

//                if(user == null)
//                {
//                    user = new IUserImpl();
//                    m_im_userlist.addUser(user);
//                }
//                else
//                {
//                    attroldInit = true;
//                    for(int indexName = 0;  indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
//                    {
//                        attrold[indexName] = user.GetAttr(indexName);
//                    }
//                }
//
//                //update new attr
//                for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
//                {
//                    String attr = m_im_channel.GetUserAttr(ntf.getUserid(),Integer.toString(indexName));
//                    user.SetAttr(indexName,attr.equals("")?0:Integer.parseInt(attr));
//                }
//
                if(!attroldInit)
                {
                    m_pCallback.OnUserOnline(userid);

                    for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
                    {
                        NotifyUserAttrChanged(user,indexName,0,user.GetAttr(indexName));
                    }
                }
                else
                {
                    for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
                    {
                        NotifyUserAttrChanged(user,indexName,attrold[indexName],user.GetAttr(indexName));
                    }
                }

            }
                break;
            case IRtcSink.RTC_EVTID_NTF_USER_LEAVE:
            {
                objNtfUserLeave ntf = (objNtfUserLeave)ob;
                int userid = Integer.parseInt(ntf.getUserid());
                IUserImpl user = (IUserImpl)m_im_userlist.getUser(userid);
                if(user == null)
                    return ;

                if(m_im_userlist.removeUser(userid))
                    m_pCallback.OnUserOffline(userid);
            }
                break;

            case IRtcSink.RTC_EVTID_NTF_RECV_MSG://接收到了消息
            {
                OnProcessMessageNotify( ob); //发送一些踢人啊，设置优先级等操作.
            }
                break;
        }
    }

    private void OnProcessMessageNotify(Object ob){

        //处理命令
        objNtfMsg msg = (objNtfMsg)ob;

        if(msg.getMsgType() == IRtcChannel.typeCmd)
        {

            String msgStr = msg.getMessage();
            if(msgStr.length() < 3)
            {
                //format error
                return ;
            }

            if(msgStr.charAt(0) != 'c' || msgStr.charAt(1) != '=')
            {
                //format error
                return;
            }

            int cmd = msgStr.charAt(2) - '0' ;

            switch(cmd)
            {
                case ROOM_CHAT_CMD_KICKOFF:
                {
                    if( Integer.parseInt(msg.getToUserID()) != m_userid)
                        return;
                    LeaveGroup();
                    m_pCallback.OnKickoff();
                }
                    break;

                default:
                    break;
            }

        }
    }

    private void OnProcessIMRespond(int type, int ec, Object ob, long userdata){
        switch(type)
        {
            case  IRtcSink.RTC_EVTID_RESP_LOGINED:  //登录成功应答
            {
                objRespLogin resp = (objRespLogin)ob;
                if(ec == IRtcChannel.ERR_SUCCEED)
                {
                    m_im_userlist.clear();

                    objUserList objlist = m_im_channel.GetUserList();
                    int size = objlist.size();
                    for(int i = 0 ; i < size ; i++)
                    {
                        objUser item = objlist.item(i);

                        IUserImpl user = new IUserImpl();
                        user.m_userid = Integer.parseInt(item.getUserid());

                        //设置元素的属性
                        for(int indexName = 0 ; indexName < ROOM_GLOBAL_USER_ATTR_MAXCOUNT ; indexName++)
                        {
                           String value = m_im_channel.GetUserAttr(item.getUserid(),Integer.toString(indexName));
                           Log.e("Dispatch","user id: " + item.getUserid() + "  Dispach indexName:"+indexName+" --value:"+value);
                           user.SetAttr(indexName,value.equals("") ? 0 :  Integer.parseInt(value));
                        }

                        m_im_userlist.addUser(user);
                    }
                }
                else
                {
                    m_im_channel.Logout();
                }

                m_pCallback.OnStartResult(ec);
            }
                break;
            case IRtcSink.RTC_EVTID_RESP_SET_USER_ATTR: //设置用户属性的应答
            {
                objRespSetUserAttr resp = (objRespSetUserAttr)ob;
                if(ec != IRtcChannel.ERR_SUCCEED)
                {

                }
                else
                {
                    int userid = Integer.parseInt(resp.getUserID());
                    OnProcessIMUserAttrChanged(userid,resp.getAttrName(),resp.getAttrValue());

                }
            }
                break;
            case IRtcSink.RTC_EVTID_RESP_SEND_MSG://发送消息应答
            {

            }
                break;
        }
    }

//处理IM用户属性变化
//-(void) OnProcessIMUserAttrChanged:(int)userid name:(NSString*)attrName value:(NSString*)attrvalue
    void  OnProcessIMUserAttrChanged(int userid ,String attrName,String attrValue) {
        IUser item  = m_im_userlist.getUser(userid);

        IUserImpl user = (IUserImpl) item;
        if(user == null)
            return;

        int nameIndex = Integer.parseInt(attrName);
        int oldAttrVal = user.GetAttr(nameIndex);
        int newAttrVal = Integer.parseInt(attrValue);

        if(oldAttrVal == newAttrVal)
            return;

        user.SetAttr(nameIndex,newAttrVal);

        NotifyUserAttrChanged(user,nameIndex,oldAttrVal,newAttrVal);

    }


    void NotifyUserAttrChanged(IUserImpl user,int attrname,int oldvalue,int attrvalue) {

        if(oldvalue == attrvalue)
            return;

        switch(attrname)
        {
            case ROOM_GLOBAL_USER_ATTR_KEY_USER_TYPE:
                break;
            case ROOM_GLOBAL_USER_ATTR_KEY_USER_AGID:
            {
                //进入或离开对讲组
                if( 0 != oldvalue )
                {
                    m_pCallback.OnLeaveGroup(user.userid(),oldvalue);
                }

                if( 0 != attrvalue )
                {
                    m_pCallback.OnJoinGroup(user.userid(),attrvalue,user.role());
                }
            }
                break;
            case ROOM_GLOBAL_USER_ATTR_KEY_USER_VCID:
            {

            }
                break;
            case ROOM_GLOBAL_USER_ATTR_KEY_ON_CHATMIC:
            {
                m_pCallback.OnGroupSpeakerChanged(user.groupid(),user.userid(),attrvalue!= 0);
            }
                break;
            case ROOM_GLOBAL_USER_ATTR_KEY_ON_VIDEO:
            {
                //进入或离开视频会议通知
            }
                break;
            default:
                break;
        }

    }

}





















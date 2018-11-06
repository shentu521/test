package org.vallery.videochatdemoandroid;

class user_role
{
    public static final int  ROLE_NORMAL  = 0 ;
    public static final int  ROLE_HIGHT   = 3 ;
    public static final int  ROLE_CONTROL = 5 ;
};

class DISP_ERROR
{
    public static final int ECODE_SUCCEED = 0;
    public static final int ECODE_ERRORFROM = -100000;
    public static final int ECODE_MIC_INUSED = ECODE_ERRORFROM + 1; //麦被占用
    public static final int ECODE_NO_PERMISSION = ECODE_ERRORFROM + 2;//无权限

    public static final int ECODE_ERROREND = ECODE_ERRORFROM + 1000;
}



public interface IDispachBaseAPI {


    public interface IUser {
        int role();
        int userid();
        int groupid();
        boolean IsSpeaker();
    }

    public interface IUserList {
        int count();
        IUser item(int index);
    }

    public interface IEventCallback {

        //用户上线通知
        void OnUserOnline(int userid);


        //用户离线通知
        void OnUserOffline(int userid);

        //用户进入对讲组通知
        void OnJoinGroup(int userid,int groupid,int role);

        //用户离开对讲组通知
        void OnLeaveGroup(int userid , int groupid);

        //自己进入对讲组结果 ec=0 标识成功，否则失败，ec为错误码
        void OnJoinGroupResult(int ec);

        //自己启动结果， ec=0 标识成功，否则失败，ec为错误码 只有启动成功才能进对讲组
        void OnStartResult(int ec);

        //用户优先级发生变化，bSet=true 设置优先权，bSet=false 去掉优先权
        void OnUserPriorityChanged(int userid ,boolean bSet);

        //对讲组中可发言用户，抢到麦和下麦
        void OnGroupSpeakerChanged(int groupid , int userid , boolean isSpeak);


        //将某个用户剔出房间的操作结果
        void OnKickoffResult(int ec ,int userid);


        //自己被踢出房间
        void OnKickoff();

    };


    public void SetCallback(IEventCallback pCallback);

    // 登录IM，进入信令房间
    public int Start(int userid);

    //退出整个调度系统   退出所有房间
    public void Stop();

    //进入对讲组
    public int EnterGroup(int gourpid) ;

    //离开对讲组
    public void LeaveGroup() ;

    //请求开始讲话，抢麦
    public int StartSpeak() ;

    //结束自己讲话，释放麦
    public int StopSpeak() ;

    public IUser GetUser(int userid) ;

    //获取所有用户列表
    public IUserList GetOnlineUserList();

    //获取某个对讲组成员列表
    public IUserList GetUserListByGroup(int groupid) ;


    //获取当前对讲组ID
    public int GetCurrentGroupID() ;


    //退出整个调度系统，调用次函数释放资源
    public void Release() ;

}

package org.vallery.videochatdemoandroid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.renderscript.Script;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Random;

import butterknife.BindView;
import valley.api.IRtcChannel;
import valley.api.IRtcSink;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements IDispachBaseAPI.IEventCallback {

    @BindView(R.id.user_id)
    EditText editUserID;

    @BindView(R.id.group_id)
    EditText editGroupID;

    @BindView(R.id.tick_user_id)
    EditText editTickUserID;

    @BindView(R.id.priority_id)
    EditText editPriorityID;

    @BindView(R.id.state_info)
    TextView txStateLine;

    @BindView(R.id.join_group)
    Button tnJoinGroup;

    @BindView(R.id.leave_group)
    Button tnLeaveGroup;

    @BindView(R.id.startSpeaking)
    Button tnStartSpeaking;

    @BindView(R.id.stopSpeaking)
    Button tnStopSpeaking;

    @BindView(R.id.btnLogin)
    Button tnLogin;

    @BindView(R.id.userrole)
    TextView userRole;


    //测试--普通用户
    //DispatchAPIImpl normalUser;

    //测试
    DispatchAPIControlImpl normalUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        RequestPremission();

    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };




    private void RequestPremission() {

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
            else
            {
                Init();
            }
        }
        else
        {
            Init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean permissionOK = true;
        if(requestCode == REQUEST_EXTERNAL_STORAGE)
        {
            for(int i = 0 ; i < permissions.length ; i++)
            {
                if(grantResults[i] != PackageManager.PERMISSION_GRANTED)
                {
                    permissionOK = false;
                    break;
                }
            }
        }

        if(permissionOK)
        {
            Init();
        }

    }

    private void Init(){

        ApplicationEx applicationEx = (ApplicationEx)this.getApplication();
        applicationEx.InitSDK();

        //normalUser = new DispatchAPIImpl(this);
        normalUser = new DispatchAPIControlImpl(this);
        normalUser.SetCallback(this);

        //0-1000的随机数
        Random random = new Random();
        int useridRandom = random.nextInt(1000);

        editUserID.setText(Integer.toString(useridRandom));

        ShowUserRole();
    }


    private void ShowUserRole(){
        if(normalUser.getRole() == user_role.ROLE_CONTROL)
        {
            userRole.setText("此为大厅用户");
        }
        else
        {
            userRole.setText("非大厅用户");
        }
    }

    /**
     * 登录
     * @param view
     */
    public void LogIn(View view) {

        int ec = normalUser.Start(Integer.parseInt(editUserID.getText().toString()));
        if(ec != 0)
        {
            txStateLine.setText("登录失败:"+ec);
        }
        else
        {
            tnLogin.setEnabled(false);
        }
    }

    /**
     * 退出
     * @param view
     */
    public void LogOut(View view){
        if(normalUser != null)
        {
            normalUser.Release();
        }
        this.finish();
    }


    /**
     * 加入组
     * @param view
     */
    public void joinGroup(View view) {
        int ec = normalUser.EnterGroup( Integer.parseInt(editGroupID.getText().toString()));
        if( 0 != ec )
        {
            if(IRtcChannel.ERR_ALREADY_RUN == ec)
            {
                txStateLine.setText("已经在组内了");
            }
            else
            {
                txStateLine.setText("加入组失败:"+ec);
            }
        }
        else
        {
            tnJoinGroup.setEnabled(false);
            tnLeaveGroup.setEnabled(true);
        }
    }

    /**
     * 离开组
     * @param view
     */
    public void leaveGroup(View view){
        normalUser.LeaveGroup();


        tnLeaveGroup.setEnabled(false);
        tnJoinGroup.setEnabled(true);

        tnStartSpeaking.setEnabled(false);
        tnStopSpeaking.setEnabled(false);
    }

    /**
     * 开始抢麦
     * @param view
     */
    public void StartSpeaking(View view){

        int ec = normalUser.StartSpeak();
        if(ec != 0)
        {
            switch (ec)
            {
                case DISP_ERROR.ECODE_MIC_INUSED:
                {
                    txStateLine.setText("麦已被使用");
                }
                    break;
                case DISP_ERROR.ECODE_NO_PERMISSION:
                {
                    txStateLine.setText("没有权限");
                }
                    break;
                default:
                {
                    txStateLine.setText("占麦失败:"+ec);
                }
                    break;
            }
        }
        else
        {
            tnStartSpeaking.setEnabled(false);
            tnStopSpeaking.setEnabled(true);
        }

    }

    /**
     * 停止抢麦
     * @param view
     */
    public void StopSpeaking(View view){

        int ec = normalUser.StopSpeak();
        if( ec == 0)
        {
            txStateLine.setText("释放麦成功");
            tnStartSpeaking.setEnabled(true);
            tnStopSpeaking.setEnabled(false);
        }
        else
        {
            txStateLine.setText("释放麦失败:"+ec);
        }

    }


    /**
     * 踢某个人下线
     * @param view
     */
    public void KickOffUser(View view){
        String tickuserid = editTickUserID.getText().toString();
        if(tickuserid.equals(""))
        {
            txStateLine.setText("请输入要踢人的id");
        }
        else
        {
            if(normalUser.getRole() != user_role.ROLE_CONTROL)
            {
                txStateLine.setText("您无此操作权限");
            }
            else
            {
                int ec = normalUser.KickOff(Integer.parseInt(tickuserid));
                if(ec != 0)
                {
                    txStateLine.setText("踢人失败:"+ec);
                }
            }
        }
    }

    /**
     * 设置高等级用户
     * @param view
     */
    public void HightLevel(View view){
        String tickuserid = editPriorityID.getText().toString();
        if(tickuserid.equals(""))
        {
            txStateLine.setText("请输入要踢人的id");
        }
        else
        {
            if(normalUser.getRole() != user_role.ROLE_CONTROL)
            {
                txStateLine.setText("您无此操作权限");
            }
            else
            {
                int ec = normalUser.SetUserPriority(Integer.parseInt(editPriorityID.getText().toString()));
                if(ec == 0)
                {
                    txStateLine.setText("设置高优先级成功");
                }
                else
                {
                    txStateLine.setText("设置高优先级失败:"+ec);
                }
            }
        }
    }


    /**
     * 恢复为普通用户
     * @param view
     */
    public void NormalLevel(View view){
        String tickuserid = editPriorityID.getText().toString();
        if(tickuserid.equals(""))
        {
            txStateLine.setText("请输入要踢人的id");
        }
        else
        {
            if(normalUser.getRole() != user_role.ROLE_CONTROL)
            {
                txStateLine.setText("您无此操作权限");
            }
            else
            {
                int ec = normalUser.RemoveUserPriority();
                if(ec == 0)
                {
                    txStateLine.setText("设置高优先级成功");
                }
                else
                {
                    txStateLine.setText("设置高优先级失败:"+ec);
                }
            }
        }
    }


    ////////////// 事件通知接口回调 /////////////////

    @Override
    public void OnUserOnline(int userid) {
        txStateLine.setText("用户:"+userid+"上线");
    }

    @Override
    public void OnUserOffline(int userid) {
        txStateLine.setText("用户:"+userid+"已下线");
    }

    @Override
    public void OnJoinGroup(int userid, int groupid, int role) {

        String roleStr = "";
        if(role == 0) roleStr = "普通用户";
        if(role == 3) roleStr = "高等级用户";
        if(role == 5) roleStr = "大厅用户";

        txStateLine.setText("用户:"+userid+"--加入组:"+groupid+"--:"+roleStr);



    }

    @Override
    public void OnLeaveGroup(int userid, int groupid) {
        txStateLine.setText("用户:"+userid+"--离开组:"+groupid);
    }

    @Override
    public void OnJoinGroupResult(int ec) {

        if(ec == 0)
        {
            IDispachBaseAPI.IUserList users = normalUser.GetUserListByGroup(Integer.parseInt(editGroupID.getText().toString()));
            int count = users.count();
            txStateLine.setText("加入组成功，组内共有"+count+"人");


            if(normalUser.isSpeaking())
            {
                tnStartSpeaking.setEnabled(false);
                tnStopSpeaking.setEnabled(true);
            }
            else
            {
                tnLeaveGroup.setEnabled(true);

                //允许上麦
                tnStartSpeaking.setEnabled(true);
            }

        }
        else
        {
            txStateLine.setText("加入组失败:"+ec);
        }

    }

    @Override
    public void OnStartResult(int ec) {
        if(ec == 0)
        {
            int count = normalUser.GetOnlineUserList().count();
            txStateLine.setText("登录成功,当前在线人数:"+count);


            //允许进入组
            tnJoinGroup.setEnabled(true);

        }
        else
        {
            String res = String.format("登录失败:%d",ec);
            txStateLine.setText(res);
        }
    }

    @Override
    public void OnUserPriorityChanged(int userid, boolean bSet) {

        txStateLine.setText("用户:"+userid+"的优先级发生了变化,"+ (bSet?"高优先级":"普通用户") );
    }

    @Override
    public void OnGroupSpeakerChanged(int groupid, int userid, boolean isSpeak) {

        txStateLine.setText("用户:"+userid+"-在组内:"+groupid+"-:"+(isSpeak?"抢到麦":"释放麦"));

        if(!normalUser.isSpeaking())
        {
            tnStartSpeaking.setEnabled(true);
            tnStopSpeaking.setEnabled(false);
        }
    }

    @Override
    public void OnKickoffResult(int ec, int userid) {
        txStateLine.setText("用户:"+userid+",已被踢下线");
    }

    @Override
    public void OnKickoff() {
        txStateLine.setText("您已经被踢下线");

        tnJoinGroup.setEnabled(true);
        tnLeaveGroup.setEnabled(false);
        tnStartSpeaking.setEnabled(false);
        tnStopSpeaking.setEnabled(false);
    }

}

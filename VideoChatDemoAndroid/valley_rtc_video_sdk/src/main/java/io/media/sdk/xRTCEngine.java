package io.media.sdk;

import android.hardware.Camera;
import android.media.AudioManager;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.content.Context ;
import android.view.View ;

import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.media.sdk.xRTCAudio.xRTCAudioManager;
import io.media.sdk.xRTCVideo.xRTCCameraManager;
import io.media.sdk.xRTCVideo.xRTCProfile;
import io.media.sdk.xRTCVideo.xRTCVideoCanvas;
import io.media.sdk.xRTCVideo.xRTCVideoCapture;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.POWER_SERVICE;

/**
 * Created by sunhui on 2017/8/30.
 */

public class xRTCEngine
{
    static
    {
        System.loadLibrary("xRTCImCore");
        System.loadLibrary("xRTCVideoCodec");
        System.loadLibrary("xRTCAudioCore");
        System.loadLibrary("xRTCEngine");
    }

    protected static final String TAG="xRTCEngine";
    public static final int RTC_SUCC  = 0 ;
    public static final int RTC_ERR_SYS = -1 ;
    public static final int RTC_EXIST_SYS = 1 ;

    protected final String m_strSDKVersion ="1.0.0.1";

    public static final int RTC_TYPE_BASE = 1 ;
    public static final int USE_TYPE_NETMEET = 2 ;
    public static final int RTC_TYPE_VR   = 3 ;
    public static final int RTC_TYPE_GAME = 4 ;

    // only use speaker and video view
    public static final int RTC_TYPE_CHATROOM = 5 ;
    public static final int RTC_TYPE_LIVE = 6 ;

    public int mCameraIndex ;
    public Context mContext ;
    public boolean mUsingFrontCamera = true ;

    protected int mClientRole = 0 ;
    protected int mChannelProfile = 0 ;
    protected int mVideoProfile = 0 ;
    protected boolean mStart = false ;
    protected boolean mVideoEnabled = false ;
    protected boolean mAudioEnabled = false ;
    protected boolean mUseExternalVideoSource = false ;
    public static xRTCEngine mRtcEngine = null ;
    public xRTCVideoCapture mVideoCaptrue = null ;
    protected SurfaceView mMySurfaceView  = null ;
    protected PowerManager.WakeLock m_WLock ;
    TelephonyManager mTelephonyManager = null ;
    private xRTCPhoneStateListener mPhoneStateLinstner = null;
    protected xRTCEventHandlerImp mEventHandler ;
    protected int   mUseType = RTC_TYPE_GAME;
    public xRTCAudioManager mAudioManager ;
    protected ConcurrentHashMap<Long, xRTCVideoCanvas>    mCanvasMap ;
    protected boolean mEnterChannel =false ;
    protected xRTCVideoCanvas mLocalVideoCanvas  = null ;
    protected boolean mActiveFilter = false ;
    protected int     mFilterLevel = 3 ; // 0..5

    public static synchronized xRTCEngine create(Context context, String appId, xRTCEventHandler EventHandler )
    {
        if ( mRtcEngine != null )
        {
            return null ;
        }

        mRtcEngine = new xRTCEngine( context, appId , EventHandler ) ;
        return  mRtcEngine ;
    }
    public static synchronized int destroy()
    {
        if ( mRtcEngine != null )
        {
            mRtcEngine.free() ;
            mRtcEngine = null ;
            return RTC_SUCC ;
        }

        return RTC_ERR_SYS ;
    }


    private xRTCEngine(Context context, String appId, xRTCEventHandler handler)
    {
        mEventHandler = new xRTCEventHandlerImp( handler) ;
        try
        {
            mContext = context ;
            this.mPhoneStateLinstner = new xRTCPhoneStateListener() ;
            mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
            mTelephonyManager.listen( mPhoneStateLinstner,
                    PhoneStateListener.LISTEN_CALL_STATE |
                    PhoneStateListener.LISTEN_SIGNAL_STRENGTHS );
        }
        catch (Exception e)
        {
            xRTCLogging.e(TAG, "create PhoneStateListener fail, ", e);
        }

        int rc = initEngine( context, appId, mEventHandler, mUseType ) ;
        xRTCLogging.i(TAG, "initEngine rc:"+rc ) ;

        if ( mUseType <= RTC_TYPE_GAME )
        {
            mVideoEnabled = true ;
            mAudioEnabled = true ;
        }
        mCanvasMap = new ConcurrentHashMap<Long, xRTCVideoCanvas>() ;
    }

    private void free()
    {
        //
        // 防止回调
        //
        if ( mCanvasMap == null )
        {
            return;
        }

        mTelephonyManager.listen( mPhoneStateLinstner , PhoneStateListener.LISTEN_NONE );

        if ( mAudioManager != null)
        {
            mAudioManager.cleanRecordAndPlayout();
            mAudioManager = null ;
        }

        mEventHandler.mEventHandler = null ;
        mPhoneStateLinstner = null ;

        closeServer() ;
        stopMediaRecord()  ;
        cleanEngine() ;
        mCanvasMap = null ;
    }

    public String getSdkVersion()
    {
        return m_strSDKVersion ;
    }

    public int setVideoProfile( int nProfile, boolean bSwap )
    {
        boolean rc = xRTCProfile.AnalyzeCaptureSize( nProfile, bSwap ) ;
        if ( rc == false )
        {
            return -1 ;
        }

        int nSwap  = 0 ;
        if ( bSwap )
        {
            nSwap = 1;
        }

        mVideoProfile = nProfile ;
        return  setVideoProfile( mVideoProfile , nSwap ) ;
    }

    public int enableVideo()
    {
        int rc = doCheckPermission( mContext ) ;
        if ( rc != 0 )
        {
            return -1 ;
        }
        return RTC_SUCC ;
    }

    public int enableAudio()
    {
        int rc = doCheckPermission( mContext ) ;
        if ( rc != 0 )
        {
            return -1 ;
        }

        if ( mAudioManager == null )
        {
            mAudioManager = new xRTCAudioManager( mContext ) ;
            return RTC_SUCC;
        }
        else
        {
            return RTC_EXIST_SYS ;
        }
    }

    public int selectFrontCamera()
    {
        return Camera.getNumberOfCameras() > 1 ? 1 : 0;

    }
    public int selectBackCamera()
    {
        return Camera.getNumberOfCameras() > 1 ? 0 : 0;
    }

    protected long  lastOrientationTs = 0 ;
    protected int   mTotalRotation = 0 ;

    public void SetDeviceOrientation(int orientation)
    {
        long now = System.currentTimeMillis();
        if (now - this.lastOrientationTs < 100L) {
            return;
        }
        int newori = (int)(Math.round(orientation / 90.0D) * 90L) % 360;
        int update = 0;
        if (Math.abs(newori - orientation) < 20) {
            update = 1;
        } else if (Math.abs(newori - orientation) < 40) {
            update = 2;
        }
        if ((newori == 0) &&
                (orientation > 180)) {
            if (360 - orientation < 20) {
                update = 1;
            } else if (360 - orientation < 40) {
                update = 2;
            }
        }
        if (update > 0) {
            try
            {
                Camera.CameraInfo info = new Camera.CameraInfo();
                Camera.getCameraInfo(this.mCameraIndex, info);
                int cameraOrientation = info.orientation;
                int totalCameraRotation = 0;
                int cameraRotation = update == 1 ? newori : newori + 5;
                if (this.mUsingFrontCamera)
                {
                    int inverseCameraRotation = (360 - cameraRotation) % 360;
                    totalCameraRotation = (inverseCameraRotation + cameraOrientation) % 360;
                }
                else
                {
                    totalCameraRotation = (cameraRotation + cameraOrientation) % 360;
                }

                if (totalCameraRotation != this.mTotalRotation)
                {
                    xRTCLogging.d(TAG, "total rotation :"+totalCameraRotation+" rotation:"+cameraRotation) ;
                }

                this.mTotalRotation = totalCameraRotation;
            }
            catch (Exception e)
            {
                xRTCLogging.e(TAG, "Unable to get camera info, ", e);
            }
        }
        this.lastOrientationTs = now;
    }


    public int startMediaRecord()
    {
        if ( mStart == true || mAudioManager == null )
        {
            return  -1 ;
        }

        if ( mVideoProfile == 0 )
        {
            return -2 ;
        }

        if ( mUseType > RTC_TYPE_GAME)
        {
            return -3 ;
        }

        mCameraIndex = selectFrontCamera();
        PowerManager pManager = ((PowerManager) mContext.getSystemService(POWER_SERVICE));
        m_WLock = pManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, TAG );

        int vrc = 0 ;
        int arc = 0 ;

        if( mAudioEnabled )
        {
            mAudioManager.initRecordAndPlayout() ;
            arc = startVoiceCapture( 0, mAudioManager.getSysBufDelay() ) ;
        }

        if ( mVideoEnabled )
        {
            mVideoCaptrue = xRTCCameraManager.createVideoCapture( mCameraIndex, mContext ) ;
            mVideoCaptrue.allocate() ;
            setVideoCamera( mCameraIndex ) ;
            mVideoCaptrue.setCaptureFormat( xRTCVideoCapture.kVideoNV21 ) ;
            vrc  = mVideoCaptrue.startCapture( xRTCProfile.mFrameCount ) ;
            if ( mActiveFilter )
            {
                setActiveBeautifyFilter( 1, mFilterLevel ) ;
            }

        }

        if ( ( vrc == 0 || arc == 0 ) && m_WLock != null )
        {
            m_WLock.acquire() ;
        }

        mStart = true ;
        return  0 ;
    }


    public int stopMediaRecord()
    {
        if ( mStart == false )
        {
            return  -1 ;
        }

        if ( m_WLock != null )
        {
            m_WLock.release();
            m_WLock = null ;
        }

        if ( mVideoCaptrue != null )
        {
            mVideoCaptrue.deallocate();
        }

        if ( mAudioEnabled )
        {
            stopVoiceCapture();
        }

        if ( mAudioManager != null )
        {
            mAudioManager.cleanRecordAndPlayout() ;
        }

        xRTCLogging.i(TAG, "stopMediaRecord...");

        mVideoCaptrue = null ;
        mStart = false ;
        return 0 ;
    }

    public int setVideoCamera(int cameraIndex)
    {
        if ( !mVideoEnabled || mUseExternalVideoSource ) {
            return -1;
        }
        mCameraIndex = cameraIndex;
        mUsingFrontCamera = (this.mCameraIndex == 1);

        return  0 ;
    }

    private int addMySurface(SurfaceView view)
    {
        if ( mMySurfaceView != null )
        {
            if ( mMySurfaceView.equals( view ) )
            {
                return  1 ;
            }

            mMySurfaceView.setVisibility( View.INVISIBLE );
        }
        else
        {
            view.setVisibility( View.VISIBLE );
        }

        mMySurfaceView = view;
        if (mVideoCaptrue != null) {
            mVideoCaptrue.setPreview(view);
        }

        return 0;
    }

    public xRTCVideoCanvas removeRemoteCanvas(long userid)
    {
        xRTCVideoCanvas canvas = mCanvasMap.remove(userid) ;
        if ( canvas != null)
        {
            canvas.detachUser() ;
            canvas.freeUser();
        }

        return  canvas ;
    }

    public int joinChannel(byte [] channelname , long channelid, String strToken, long userid )
    {
        if ( mEnterChannel )
        {
            return  -101 ;
        }

        int rc1 = setUserInfo( userid, strToken );
        int rc2 = setChannelInfo( channelid, channelname, null ) ;
        if ( rc1 == 0 && rc2==0 )
        {
            mEnterChannel = true ;
            return loginServer( 0 ) ;
        }

        return -102 ;
    }

    static public SurfaceView CreateRendererView(Context context)
    {
        SurfaceView view = new SurfaceView( context ) ;
        view.setVisibility( View.VISIBLE ) ;
        return  view ;
    }

    //
    // 使用美颜滤镜 任意时候调用均可
    //
    public int setBeautityLevel(boolean active, int level )
    {
        mActiveFilter = active ;
        mFilterLevel = level ;
        if ( mStart )
        {
            int nActive = 1 ;
            if ( !active )
            {
                nActive = 0 ;
            }

            setActiveBeautifyFilter( nActive, level ) ;
        }

        return 0 ;
    }

    public int setupLocalVideo( xRTCVideoCanvas canvas )
    {
        if ( mLocalVideoCanvas != null )
        {
            mLocalVideoCanvas.detachUser() ;
            mLocalVideoCanvas.freeUser() ;
        }

        mLocalVideoCanvas = canvas ;

        if ( canvas != null ) {
            canvas.setLocal(true);
            canvas.attachUser() ;
        }

        return 0 ;
    }

    public int switchCamera(boolean bFrontCamera)
    {
        mUsingFrontCamera = bFrontCamera ;
        return 0 ;
    }

    public xRTCVideoCanvas setupRemoteVideo( xRTCVideoCanvas canvas )
    {
        if ( canvas == null )
        {
            return null ;
        }

        xRTCVideoCanvas oldCanvas = mCanvasMap.put( canvas.getUID(), canvas ) ;
        if ( oldCanvas != null )
        {
            oldCanvas.detachUser();
            oldCanvas.freeUser();
        }

        canvas.attachUser() ;
        return oldCanvas ;
    }

    public int closeRemoteVideo(xRTCVideoCanvas canvas)
    {
        if ( !mCanvasMap.remove( canvas.getUID(), canvas ) ) {
            xRTCLogging.e(TAG, "closeRemoteVideo no exist canvas uid:"+canvas.getUID());
        }
        else {
            xRTCLogging.i(TAG, "closeRemoteVideo close canvas uid:"+canvas.getUID());
        }
        canvas.detachUser();
        canvas.freeUser();
        return 0 ;
    }


    public int muteLocalVideoStream(boolean mute)
    {
        if ( mStart == false )
        {
            return -11 ;
        }

       return muteVideoCapture( mute )  ;
    }

    public int muteLocalAudioStream(boolean mute)
    {
        if ( mStart == false )
        {
            return -11 ;
        }
        return  muteVoiceCapture( mute ) ;
    }

    public int muteRemoteVideoStream( long userid, boolean mute )
    {
        if ( mStart == false )
        {
            return -11 ;
        }
        return muteVideoPlay( userid, mute ) ;
    }

    public int muteRemoteAudioStream( long userid, boolean mute )
    {
        if ( mStart == false )
        {
            return -11 ;
        }

        return  muteVoicePlay( userid, mute ) ;
    }

    public int leaveChannel()
    {
        mEnterChannel = false ;
        return leaveChannel( 0 ) ;
    }

    protected static native int  initEngine( Object contextObj, String strAppKey, Object hEvent,int nUserType ) ;

    protected static native int  cleanEngine() ;

    protected static native Object getContext() ;

    protected static native int setUserInfo( long uUserID ,String strToken ) ;
    protected static native int setChannelInfo(long uRoomID, byte [] szRoomName, byte[] szRoomToken ) ;

    protected static native int setVideoProfile( int Profile, int nSwap ) ;

    protected static native int enterChannel( int enterType ) ;
    protected static native int leaveChannel( int nReson ) ;

    protected static native int loginServer(int loginType ) ;
    protected static native int closeServer() ;

    public static native int startVoiceCapture(int nFixType, int nDevDelay ) ;
    protected static native int muteVoiceCapture(boolean bMute) ;
    protected static native int muteVoicePlay(long userid,boolean bMute);

    protected static native int muteVideoCapture(boolean bMute) ;
    protected static native int muteVideoPlay(long userid,boolean bMute);

    public static native int stopVoiceCapture() ;

    public static native int switchVideoCaptureCamera( int nRotate, int nFrontCamera, int nWidth, int nHeight ) ;
    public static native int startVideoCapture( int nFrameCount , int nFixType ) ;

    public static native int stopVideoCapture() ;

    public static native int resetVideoCapture( int nZoomWidth, int nZoomHeight, int nRotate ) ;
    public static native int  processCameraFrame( byte[] FrameData, int nExtSize ) ;

    public static native int addVideoView( long userid, Object surfaceView,int renderType ) ;
    public static native int updateVideoView( long userid, Object surfaceView, int renderType ) ;
    public static native int removeVideoView( long userid ) ;

    public static native int addLocalVideoView(Object surfaceView, int renderType ) ;
    public static native int removeLocalVideoView( int nType ) ;

    protected static native int sendIMMessage( Object messageObj ) ;
    protected static native int writeLog(int nTag, String strLog ) ;
    public static native void onCameraError(long error, String strError );
    protected static native int setActiveBeautifyFilter( int active, int level ) ;

    protected static String getRandomUUID()
    {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private void checkVoipPermissions(Context context, String perm)
            throws SecurityException
    {
        if ((context == null) || (context.checkCallingOrSelfPermission(perm) != 0)) {
            throw new SecurityException(perm + " is not granted");
        }
    }

    private void checkVoipPermissions(Context context)
            throws SecurityException
    {
        checkVoipPermissions(context, "android.permission.INTERNET");
        checkVoipPermissions(context, "android.permission.RECORD_AUDIO");
        checkVoipPermissions(context, "android.permission.MODIFY_AUDIO_SETTINGS");
        if ((this.mVideoEnabled) && (!this.mUseExternalVideoSource)) {
            checkVoipPermissions(context, "android.permission.CAMERA");
        }
    }

    private int checkVoipPermissions(Context context, int clientRole)
    {
        switch (clientRole)
        {
            case RTC_TYPE_BASE:
            case RTC_TYPE_VR:
            case RTC_TYPE_GAME:
            case USE_TYPE_NETMEET:
                try
                {
                    checkVoipPermissions(context);
                }
                catch (SecurityException e)
                {
                    xRTCLogging.e( TAG, "Do not have enough permission! ", e);
                    return -9;
                }
                break ;

            case RTC_TYPE_LIVE:
            case RTC_TYPE_CHATROOM:
                {
                try
                {
                    checkVoipPermissions(context, "android.permission.INTERNET");
                } catch (SecurityException e) {
                    xRTCLogging.e(TAG, "Do not have Internet permission!");
                    return -9;
                }
            }
                break ;

            default:
                return -2;
        }

        return 0;
    }


    private int doCheckPermission(Context context)
    {
        int role = RTC_TYPE_BASE ;
        if (this.mChannelProfile > 0 )
        {
            role = this.mClientRole;
        }

        if (checkVoipPermissions(context, role) != 0)
        {
            xRTCLogging.e(TAG, "join channel fail because no permission");
            return -9;
        }
        return 0;
    }


    private class xRTCPhoneStateListener extends PhoneStateListener
    {
        private SignalStrength mSignalStrenth;

        public int getRssi()
        {
            return invokeMethod("getDbm");
        }

        public int getLevel()
        {
            return invokeMethod("getLevel");
        }

        public int getAsuLevel()
        {
            return invokeMethod("getAsuLevel");
        }

        private int invokeMethod(String methodName)
        {
            try
            {
                if (this.mSignalStrenth != null)
                {
                    Method method = this.mSignalStrenth.getClass().getDeclaredMethod(methodName, new Class[0]);
                    if (method != null) {
                        return ((Integer)method.invoke(this.mSignalStrenth, new Object[0])).intValue();
                    }
                }
            }
            catch (Exception localException) {}
            return 0;
        }

        public void onSignalStrengthsChanged(SignalStrength signalStrength)
        {
            super.onSignalStrengthsChanged(signalStrength);
            this.mSignalStrenth = signalStrength;
        }

        public void onCallStateChanged(int state, String incomingNumber)
        {

            super.onCallStateChanged( state, incomingNumber ) ;

            switch (state)
            {
                case TelephonyManager.CALL_STATE_IDLE:
                    xRTCLogging.d(TAG, "call not answer...");
                    xRTCEngine.muteVoiceCapture( false ) ;
                    xRTCEngine.muteVoicePlay( 0, false ) ;
                    break;

                case TelephonyManager.CALL_STATE_RINGING:
                    xRTCLogging.i( TAG, "incoming ring...");
                    xRTCEngine.muteVoiceCapture( true ) ;
                    xRTCEngine.muteVoicePlay( 0, true ) ;
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    xRTCLogging.i(TAG, "in a call...");
                    xRTCEngine.muteVoiceCapture( true ) ;
                    xRTCEngine.muteVoicePlay( 0, true ) ;

                    break;
            }
        }
    }

}

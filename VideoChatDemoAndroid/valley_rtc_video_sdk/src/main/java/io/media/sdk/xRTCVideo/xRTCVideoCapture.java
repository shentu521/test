package io.media.sdk.xRTCVideo;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import io.media.sdk.xRTCDevice;
import io.media.sdk.xRTCEngine;
import io.media.sdk.xRTCLogging;

import static android.graphics.ImageFormat.NV21;
import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUY2;
import static android.graphics.ImageFormat.YV12;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MAX_INDEX;
import static android.hardware.Camera.Parameters.PREVIEW_FPS_MIN_INDEX;
import static io.media.sdk.xRTCVideo.xRTCProfile.VIDEO_CAPTURE_TYPE_16X16;
import static io.media.sdk.xRTCVideo.xRTCProfile.VIDEO_CAPTURE_TYPE_16X9;
import static io.media.sdk.xRTCVideo.xRTCProfile.VIDEO_CAPTURE_TYPE_4X3;

public class xRTCVideoCapture
        implements Camera.PreviewCallback, SurfaceHolder.Callback, Camera.ErrorCallback {
    private static final String TAG = "xRTCVideoCapture";
    private static final boolean DEBUG = false;


    public static final int kVideoI420 = 8 ;
    public static final int kVideoNV21 = 1 ;
    public static final int kVideoNV12 = 2 ;
    public static final int kVideoYV12 = 3 ;
    public static final int kVideoYUY2 = 9 ;
    public static final int kVideoRGB565 = 4 ;
    public static final int kVideoRGB24 = 5 ;
    public static final int kVideoRGBA = 6 ;
    public static final int kVideoRGBX = 7 ;
    public static final int kVideoUnknown = 100;
    private static final int kTVideoBufSize = 1024*4 ;
    private static final int kBackBufQueueSize = 3 ;
    protected int mCameraNativeOrientation;
    protected final Context mContext;
    protected final int mId;


    protected Camera mCamera;
    protected ReentrantLock mPreviewBufferLock = new ReentrantLock();
    private ReentrantLock mCaptureLock = new ReentrantLock();
    private boolean isCaptureStarted = false;
    private boolean isCaptureRunning = false;
    private boolean isFaceDetectionStarted = false;
    private SurfaceHolder mLocalPreview = null;
    private SurfaceTexture mDummySurfaceTexture = null;
    private int mExpectedFrameSize = 0;
    private int mCaptureWidth = -1;
    private int mCaptureHeight = -1;
    private int mCaptureFps = -1;
    private int mCaptureFormat = NV21;
    private long mCaptureTick = 0 ;

    xRTCVideoCapture(Context context, int id  ) {
        mId = id;
        mContext = context;
        xRTCCameraManager.calOritation(context, getCameraInfo(id));
    }

    protected static Camera.CameraInfo getCameraInfo(int id) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        try {
            Camera.getCameraInfo(id, cameraInfo);
        } catch (RuntimeException ex) {
            xRTCLogging.e(TAG, "getCameraInfo: Camera.getCameraInfo: ", ex);
            return null;
        }
        return cameraInfo;
    }

    static int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    static String getName(int id) {
        Camera.CameraInfo cameraInfo = getCameraInfo(id);
        if (cameraInfo == null) {
            return null;
        }
        return "camera " + id + ", facing " + (cameraInfo.facing == 1 ? "front" : "back");
    }

    public int CaptureWidth() { return  mCaptureWidth ;}
    public int CaptureHeight() { return mCaptureHeight ; }

    static int getSensorOrientation(int id) {
        Camera.CameraInfo cameraInfo = getCameraInfo(id);
        if (cameraInfo == null) {
            return -1;
        }
        return cameraInfo.orientation;
    }

    public static String fetchCapability(int id, Context appContext)
    {
        String PREFS_NAME = "CamCaps";
        SharedPreferences caps = appContext.getSharedPreferences(PREFS_NAME, 0);
        return caps.getString("Cam_" + id, null);
    }

    public static void cacheCapability(int id, Context appContext, String cap)
    {
        String PREFS_NAME = "CamCaps";
        SharedPreferences caps = appContext.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = caps.edit();
        editor.putString("Cam_" + id, cap);
        editor.commit();
    }

    public static int translateToEngineFormat(int fmt)
    {
        switch (fmt)
        {
            case kVideoI420:
                return kVideoI420;
            case NV21:
                return kVideoNV21;
            case kVideoYV12:
                return kVideoYV12;
            case kVideoYUY2:
                return kVideoYUY2;
        }
        return kVideoUnknown;
    }

    public static int translateToAndroidFormat(int fmt)
    {
        switch (fmt)
        {
            case kVideoI420:
                return YUV_420_888;
            case kVideoNV21:
                return NV21;
            case kVideoYV12:
                return YV12;
            case kVideoYUY2:
                return YUY2;
        }
        return 0;
    }

    public int allocate() {
        try {
            this.mCamera = Camera.open(this.mId);
        } catch (RuntimeException ex) {
            xRTCLogging.e(TAG, "allocate: Camera.open: ", ex);
            return -1;
        }
        Camera.CameraInfo cameraInfo = getCameraInfo(this.mId);
        if (cameraInfo == null) {
            this.mCamera.release();
            this.mCamera = null;
            return -2;
        }
        if ( fetchCapability(this.mId, this.mContext) == null) {
            createCapabilities();
        }
        this.mCameraNativeOrientation = cameraInfo.orientation;
        return 0;
    }

    public int setCaptureFormat(int format) {
        xRTCLogging.d(TAG, "setCaptureFormat: " + format);
        this.mCaptureFormat = translateToAndroidFormat(format);
        if (this.mCaptureFormat == 0) {
            xRTCLogging.e(TAG, "setCaptureFormat failed, unkonwn format: " + format);
            return -1;
        }
        return 0;
    }

    static protected int MAXIndex(int n1, int n2)
    {
        if ( n1>= n2 ) return 0 ;
        else return 1 ;
    }

    public int [] setFpsRange(Camera.Parameters parameters)
    {
        int [] useFPS = null ;
        int [] bestFPS = null ;

        try {

            List<int[]> range = parameters.getSupportedPreviewFpsRange() ;

            for (int i = 0 ; i < range.size() ; ++i )
            {
                int [] FpsArray = range.get(i) ;

                if ( FpsArray.length <2 )
                {
                    continue ;
                }

                int minfps = FpsArray[PREVIEW_FPS_MIN_INDEX] /1000 ;
                int maxfps = FpsArray[PREVIEW_FPS_MAX_INDEX] /1000 ;

                xRTCLogging.i(TAG,"range:"+FpsArray[0]+"/"+FpsArray[1] );
                if  ( minfps == mCaptureFps && maxfps == mCaptureFps )
                {
                    xRTCLogging.i(TAG,"use best range:"+FpsArray[0]+"/"+FpsArray[1] );
                    bestFPS = FpsArray ;
                    useFPS = bestFPS ;
                    break ;
                }

                int allfps =(minfps + maxfps) >>1 ;
                int sub1 = Math.abs(allfps-mCaptureFps) ;
                if ( sub1 <= 3  )
                {
                    xRTCLogging.i(TAG,"set use range:"+FpsArray[0]+"/"+FpsArray[1] );
                    useFPS = FpsArray ;
                    continue ;
                }

            }

            if ( bestFPS != null )
            {
                parameters.setPreviewFpsRange(bestFPS[PREVIEW_FPS_MIN_INDEX], bestFPS[PREVIEW_FPS_MAX_INDEX]);
            }
            else {
                if (useFPS != null) {
                    parameters.setPreviewFpsRange(useFPS[PREVIEW_FPS_MIN_INDEX], useFPS[PREVIEW_FPS_MAX_INDEX]);
                }
            }
        }
        catch (Exception ex)
        {
            useFPS = null ;
            bestFPS = null ;
        }

        return  useFPS;
    }

    private float getRadio(int n1, int n2 )
    {
        if ( n1 >= n2 ) {
            return (float)n1 / (float)n2 ;
        } else {
            return (float)n2/(float)n1 ;
        }
    }

    private boolean getEqualPreviewSize(Camera.Parameters parameters)
    {
        List<Camera.Size> List = parameters.getSupportedPreviewSizes() ;
        List<Camera.Size> EqualList = new ArrayList<>() ;

        int nVideoWidth = xRTCProfile.mZoomWidth ;
        int nVideoHeight = xRTCProfile.mZoomHeight ;

        boolean bEqual = false ;

        float fRadioMax  ;
        float fRadioMin ;

        if ( xRTCProfile.mRadio == VIDEO_CAPTURE_TYPE_16X9 )
        {
            // 1.777
            fRadioMin = 1.75f ;
            fRadioMax = 1.79f ;
        }
        else
        {
            // 1.3333
            if ( xRTCProfile.mRadio == VIDEO_CAPTURE_TYPE_4X3 ) {
                fRadioMax = 1.35f ;
                fRadioMin = 1.32f ;
            }
            else
            {
                if ( xRTCProfile.mRadio == VIDEO_CAPTURE_TYPE_16X16 ){
                    fRadioMax = 0.96f;
                    fRadioMin = 1.10f;
                }else{
                    return false ;
                }
            }

        }

        xRTCLogging.i(TAG, "preview size find w:"+nVideoWidth+" h:"+nVideoHeight ) ;

        for( Camera.Size size :List )
        {
            xRTCLogging.i(TAG, "preview size w:"+size.width+" h:"+size.height ) ;

            if ( size.width == nVideoWidth && size.height == nVideoHeight )
            {
                mCaptureWidth = size.width ;
                mCaptureHeight = size.height ;
                bEqual = true ;
                break ;
            }

            if ( size.width >= xRTCProfile.mZoomWidth &&
                    size.height >= xRTCProfile.mZoomHeight )
            {
                float fRadio = getRadio( size.width, size.height ) ;
                boolean bUse = false ;

                if ( fRadio > fRadioMin && fRadio < fRadioMax )
                {
                    if ( mCaptureHeight < 0 || size.width < mCaptureWidth ) {
                        mCaptureWidth = size.width;
                        mCaptureHeight = size.height;
                        bEqual = true ;
                        bUse = true ;
                    }
                }

                xRTCLogging.i(TAG, "preview size w:"+size.width+" h:"+size.height+
                        " radio:"+fRadio+" fmax:"+fRadioMax+" fmin:"+fRadioMin +" use:"+bUse ) ;

            }
        }

        if ( bEqual == false )
        {
            xRTCLogging.e(TAG, "getEqualPreviewSize fail...w:"+nVideoWidth+" h:"+nVideoHeight ) ;
        }
        else
        {
            xRTCLogging.i(TAG, "getEqualPreviewSize succ width:"+mCaptureWidth+
                            " height:"+mCaptureHeight ) ;
        }

        return bEqual ;
    }

    private boolean getEqualPreviewSize( Camera.Parameters parameters, int width , int height )
    {
        List<Camera.Size> List = parameters.getSupportedPreviewSizes() ;

        boolean bEqual = false ;
        for(Camera.Size size :List )
        {
            if ( size.height == height && size.width == width )
            {
                mCaptureHeight = height ;
                mCaptureWidth = width ;
                bEqual = true ;
                break ;
            }
            xRTCLogging.i(TAG, "preview size w:"+size.width+" h:"+size.height ) ;
        }

        float fRadio  ;
        if ( xRTCProfile.mRadio == VIDEO_CAPTURE_TYPE_16X9 )
        {
            fRadio = 1.70f ;
        }
        else
        {
            fRadio = 1.4f ;
        }

        if ( bEqual == false  )
        {
            for( Camera.Size size :List ) {
                if ( size.height == xRTCProfile.mZoomWidth &&
                        size.width == xRTCProfile.mZoomHeight )
                {
                    mCaptureWidth = size.width  ;
                    mCaptureHeight = size.height ;
                    bEqual = true ;
                    break ;
                }

                if ( size.width >= xRTCProfile.mZoomWidth &&
                        size.height >= xRTCProfile.mZoomHeight )
                {
                    float nRadio = getRadio( size.width, size.height ) ;

                    if ( nRadio > fRadio && fRadio == 1.70f )
                    {
                        mCaptureWidth = size.width ;
                        mCaptureHeight = size.height ;
                        bEqual = true ;
                        break ;
                    }
                    else
                    {
                        if ( nRadio < fRadio && fRadio == 1.4f )
                        {
                            mCaptureWidth = size.width ;
                            mCaptureHeight = size.height ;
                            bEqual = true ;
                            break ;
                        }
                    }
                }
                else
                {
                    continue;
                }

                xRTCLogging.i(TAG, "preview size w:"+size.width+" h:"+size.height ) ;
            }

            if ( bEqual )
            {
                xRTCLogging.i(TAG, "find preview size w:"+mCaptureWidth+" h:"+mCaptureHeight ) ;
            }
            else
            {
                xRTCLogging.i(TAG, "find preview fail size w:"+mCaptureWidth+" h:"+mCaptureHeight ) ;
            }
        }


        return  bEqual ;

    }

    private int tryStartCapture() {
        if ( mCamera == null ) {
            xRTCLogging.e(TAG, "Camera not initialized %d" + this.mId);
            return -1;
        }

        Camera.Parameters parameters = this.mCamera.getParameters();
        boolean rc = getEqualPreviewSize( parameters ) ;
        if ( rc == false )
        {
            return -1 ;
        }

        int width = mCaptureWidth ;
        int height = mCaptureHeight ;

        xRTCLogging.i(TAG, "tryStartCapture: " + width + "*" + height +
                ", frameRate: " + mCaptureFps );

        parameters.setPreviewSize( mCaptureWidth, mCaptureHeight );
        parameters.setPreviewFormat( this.mCaptureFormat ) ;

        int [] useFps = setFpsRange( parameters ) ;
        if (  useFps == null  ) {
            parameters.setPreviewFrameRate(mCaptureFps);
        }

        setAdvancedCameraParameters( parameters ) ;
        setDeviceSpecificParameters( parameters ) ;

        mCamera.setParameters( parameters ) ;

        xRTCLogging.d(TAG, "camera orient:"+xRTCCameraManager.GetDefaultOritation());

        if ( xRTCCameraManager.GetDefaultOritation() != 0 ) {
            mCamera.setDisplayOrientation(xRTCCameraManager.GetDefaultOritation());
        }

        int bufSize = width * height * ImageFormat.getBitsPerPixel(this.mCaptureFormat) / 8;

        bufSize += kTVideoBufSize ;

        for (int i = 0; i < kBackBufQueueSize; i++) {
            byte[]  buffer = new byte[bufSize];
            mCamera.addCallbackBuffer(buffer);
        }


        xRTCLogging.d( TAG, "camera buf size:"+bufSize ) ;

        mCamera.setPreviewCallbackWithBuffer( this );
        mExpectedFrameSize = bufSize ;

        int nRotate ;
        int nFrontCamera ;
        if ( xRTCCameraManager.GetDefaultOritation() % 180 != 0 )
        {
            nRotate = xRTCCameraManager.GetDisaplayOritation() ;
        }
        else
        {
            nRotate = xRTCCameraManager.GetDefaultOritation() ;
        }

        if ( xRTCEngine.mRtcEngine.mUsingFrontCamera  )
        {
            nFrontCamera = 1 ;
        }
        else
        {
            nFrontCamera = 0 ;
        }

        xRTCEngine.switchVideoCaptureCamera( nRotate,  nFrontCamera,
                                                mCaptureWidth, mCaptureHeight ) ;
        xRTCEngine.startVideoCapture(
                    mCaptureFps,
                    xRTCProfile.mCodecType ) ;

        mCamera.startPreview() ;

        mPreviewBufferLock.lock();
        isCaptureRunning = true ;
        mPreviewBufferLock.unlock();

        return 0;
    }

    public int startCapture(int frameRate) {
        if ( mCamera == null ) {
            xRTCLogging.e(TAG, "startCapture: camera is null!!");
            return -1;
        }

        int res = 0;

        if ( mLocalPreview != null)
        {
            if (( mLocalPreview.getSurface() != null) &&
                    ( mLocalPreview.getSurface().isValid())) {
                surfaceCreated( mLocalPreview);
            }

            mLocalPreview.addCallback(this);
        }
        else
        {
            mCaptureLock.lock();
            try
            {
                this.mDummySurfaceTexture = new SurfaceTexture(42);
                this.mCamera.setPreviewTexture(this.mDummySurfaceTexture);
            }
            catch (Exception e)
            {
                xRTCLogging.e(TAG, "failed to startPreview, invalid surfaceTexture!");
                this.mDummySurfaceTexture = null;
                res = -1;
            }
            finally
            {
                this.mCaptureLock.unlock();
                if (res != 0) {
                    return res;
                }
            }
        }

        mCaptureLock.lock();

        if ( isCaptureRunning || isCaptureStarted )
        {
            return -2 ;
        }

        isCaptureStarted = true;
        mCaptureFps = frameRate;
        try {
            res = tryStartCapture( ) ;
        } catch (Throwable t) {
            xRTCLogging.e(TAG, "try start capture failed " + t);
            res = -1;
            isCaptureStarted = false ;
        } finally {
            mCaptureLock.unlock();
        }
        return res;
    }

    public int stopCapture(  ) {
        if (! isCaptureStarted) {
            xRTCLogging.w(TAG, "already stop capture");
            return 0;
        }

        if ( mCamera == null )
        {
            xRTCLogging.w(TAG, "mCamera start fail mCamera is null...");
            return -1 ;
        }

        try {

            if ( isFaceDetectionStarted) {
                mCamera.stopFaceDetection();
                mCamera.setFaceDetectionListener(null);
                isFaceDetectionStarted = false;
            }

            mCamera.stopPreview() ;
            mPreviewBufferLock.lock();
            isCaptureRunning = false;
            mPreviewBufferLock.unlock();
            mCamera.setPreviewCallbackWithBuffer(null);
            xRTCEngine.stopVideoCapture() ;

        } catch (RuntimeException e) {
            xRTCLogging.e(TAG, "Failed to stop camera", e);
            return -1;
        }

        xRTCLogging.i(TAG, "stopCapture succ") ;
        this.isCaptureStarted = false;

        return 0;
    }


    public void onError(int error, Camera camera)
    {

    }


    public void deallocate() {
        if (this.mCamera == null) {
            return;
        }
        stopCapture( );
        mCaptureLock.lock();
        mCamera.release();
        mCamera = null;
        mCaptureLock.unlock();

       // if ( mCamera.setErrorCallback();)
    }

    public long getCaptureTick()
    {
        return mCaptureTick ;
    }

    public void setPreview(SurfaceView view)
    {
        mCaptureLock.lock();
        try {


            if ( isCaptureStarted )
            {
                SurfaceHolder newHolder = view.getHolder() ;

                if ( mLocalPreview != null )
                {
                    mLocalPreview.removeCallback( this );
                    restartVideoCapture( newHolder ) ;
                }

                mLocalPreview = newHolder ;
                mLocalPreview.addCallback( this ) ;

                if ( mCamera != null ) {
                    mCamera.startPreview();
                }

                xRTCLogging.i(TAG, "startPreview 2..." );
            }
            else
            {
                if ( mLocalPreview != null )
                {
                    mLocalPreview.removeCallback( this ) ;
                }

                mLocalPreview = view.getHolder();
            }

        }
        finally {
            mCaptureLock.unlock();
        }

    }

    public void onPreviewFrame(byte[] data, Camera camera)
    {
        if ( data == null )
        {
            xRTCLogging.e(TAG, "onPreviewFrame data is null..." );
            return ;
        }

        try
        {
            if ( data.length == mExpectedFrameSize )
            {
                mCaptureTick = xRTCEngine.processCameraFrame( data, mExpectedFrameSize );
            }
            else
            {
                xRTCLogging.e(TAG, "err length:"+ data.length );
            }
        }
        finally
        {
            if (camera != null) {
                camera.addCallbackBuffer(data);
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        xRTCLogging.d(TAG, "surfaceChanged");


        this.mCaptureLock.lock();
        try {
            if ( mCamera != null ) {
                mCamera.setPreviewDisplay( holder ) ;
                mCamera.startPreview() ;
            }
        } catch (IOException ioe) {
            xRTCLogging.e(TAG, "Failed to set preview surface!", ioe);
        } catch (RuntimeException re) {
            xRTCLogging.e(TAG, "Failed to stop preview!", re);
        }

        this.mCaptureLock.unlock();

    }

    static final int kTrySetPreviewDisplayCount = 32 ;

    public int restartVideoCapture(SurfaceHolder holder)
    {
        boolean rc = false ;

        if (mCamera == null) {
            xRTCLogging.e(TAG, "restartVideoCapture fail mCamera is null");
            return -1;
        }

        try {
            mCamera.stopPreview();
        } catch (RuntimeException re) {
            xRTCLogging.e(TAG, "Failed to stop preview!", re);
            return -2;
        }

        for (int i = 0; i < kTrySetPreviewDisplayCount; ++i) {

            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.setPreviewCallbackWithBuffer(this);
                rc = true;
            } catch (IOException ioe) {
                xRTCLogging.e(TAG, "Failed to set preview surface!", ioe);
            }

            if (rc) {
                break;
            } else {
                try {
                    Thread.sleep(40);
                } catch (Exception e) {
                }
            }
        }

        if (!rc) {
            xRTCLogging.e(TAG, "surfaceCreated fail...");
            return -3;
        }

        return 0;
    }

    public void surfaceCreated(SurfaceHolder holder)
    {
        mCaptureLock.lock();
        try
        {
            restartVideoCapture( holder ) ;
        }
        finally
        {
            mCaptureLock.unlock();
        }
        xRTCLogging.e(TAG, "surfaceCreated...!" );
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mCaptureLock.lock();
        try {
            if ( mCamera != null) {
                mCamera.setPreviewDisplay(null);
            }
        } catch (IOException e) {
            xRTCLogging.e(TAG, "Failed to clear preview surface!", e);
        }
        this.mCaptureLock.unlock();
        xRTCLogging.e(TAG, "surfaceDestroyed...!" );
    }

    private static boolean isSupported(String value, List<String> supported)
    {
        return  ( supported!= null && supported.indexOf( value )>=0  ) ;
    }

    private void setAdvancedCameraParameters(Camera.Parameters parameters) {
        String flashMode = "off";
        if (isSupported(flashMode, parameters.getSupportedFlashModes())) {
            xRTCLogging.i(TAG, "xRTC Video set flash mode = FLASH_MODE_OFF");
            parameters.setFlashMode(flashMode);
        }
        String whiteBalance = "auto";
        if (isSupported(whiteBalance, parameters.getSupportedWhiteBalance())) {
            xRTCLogging.i(TAG, "xRTC Video set white blance = WHITE_BALANCE_AUTO");
            parameters.setWhiteBalance(whiteBalance);
        }

        String focusMode = "continuous-video";
        if (isSupported(focusMode, parameters.getSupportedFocusModes())) {
            xRTCLogging.i(TAG, "xRTC Video set Focus mode = FOCUS_MODE_CONTINUOUS_VIDEO");
            parameters.setFocusMode(focusMode);
        }

        String antibandingSupported = "auto";
        if (isSupported(antibandingSupported, parameters.getSupportedAntibanding())) {
            xRTCLogging.i(TAG, "xRTC Video set anti-banding = ANTIBANDING_AUTO");
            parameters.setAntibanding(antibandingSupported);
        }

        String sceneMode = "auto" ;
        if (isSupported(sceneMode, parameters.getSupportedSceneModes())) {
            xRTCLogging.i(TAG, "xRTC Video set sence mode = " + sceneMode);
            if (parameters.getSceneMode() != sceneMode) {
                parameters.setSceneMode(sceneMode);
            }
        }
    }

    private void setDeviceSpecificParameters(Camera.Parameters parameters) {
        String device = xRTCDevice.getDeviceId();
        String cpuName = xRTCDevice.getCpuName();
        String cpuABI = xRTCDevice.getCpuABI();
        int cpuCores = xRTCDevice.getNumberOfCPUCores();
        int maxFreq = xRTCDevice.getCPUMaxFreqKHz();
        xRTCLogging.i(TAG, "Current Device: " + device);
        xRTCLogging.i(TAG, "CPU name: " + cpuName + ", with " + cpuCores + " cores, arch: " + cpuABI + ", max Freq: " + maxFreq);
        if (device.contains("xiaomi/mi note")) {
            xRTCLogging.i(TAG, "set MiNote config");
            parameters.set("scene-detect", "on");
            parameters.set("xiaomi-still-beautify-values", "i:3");
            parameters.set("skinToneEnhancement", "enable");
            parameters.set("auto-exposure", "center-weighted");
        }
        if (device.contains("oppo/r7c/r7c")) {
            xRTCLogging.i(TAG, "set oppo r7c config");
            parameters.set("skinToneEnhancement", 1);
            parameters.set("face-beautify", 100);
            parameters.set("auto-exposure", "center-weighted");
        }
    }

    public Camera.Parameters getCameraParameters()
    {
        Camera.Parameters parameters ;
        try {
            parameters = this.mCamera.getParameters();
        } catch (RuntimeException ex) {
            xRTCLogging.e(TAG, "getCameraParameters: Camera.getParameters: ", ex);
            if (this.mCamera != null) {
                this.mCamera.release();
            }
            return null;
        }

        return parameters;
    }

    public int createCapabilities() {
        String cap = null;
        Camera.Parameters param = getCameraParameters();
        if (param != null) {
            String cap_id = "\"id\":" + this.mId + ",";
            String cap_res_header = "\"resolution\":";
            String cap_res_value = "";
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            for (int i = 0; i < sizes.size(); i++) {
                String ss = "{\"w\":" + ((Camera.Size) sizes.get(i)).width + ",\"h\":" + ((Camera.Size) sizes.get(i)).height + "}";
                if (i != sizes.size() - 1) {
                    cap_res_value = cap_res_value + ss + ",";
                } else {
                    cap_res_value = cap_res_value + ss;
                }
            }
            String cap_fmt_header = "\"format\":";
            String cap_fmt_value = "";
            List<Integer> fmts = param.getSupportedPreviewFormats();
            for (int i = 0; i < fmts.size(); i++) {
                int fmt = translateToEngineFormat(((Integer) fmts.get(i)).intValue());
                if (i != fmts.size() - 1) {
                    cap_fmt_value = cap_fmt_value + fmt + ",";
                } else {
                    cap_fmt_value = cap_fmt_value + fmt;
                }
            }
            String cap_fps_header = "\"fps\":";
            String cap_fps_value = "";
            List<Integer> framerates = param.getSupportedPreviewFrameRates();
            for (int i = 0; i < framerates.size(); i++) {
                int fps = ((Integer) framerates.get(i)).intValue();
                if (i != framerates.size() - 1) {
                    cap_fps_value = cap_fps_value + fps + ",";
                } else {
                    cap_fps_value = cap_fps_value + fps;
                }
            }
            cap = "{" + cap_id + cap_res_header + "[" + cap_res_value + "]," + cap_fmt_header + "[" + cap_fmt_value + "]," + cap_fps_header + "[" + cap_fps_value + "]}";
        }
        cacheCapability( mId, mContext, cap);
        return 0;
    }
}

package io.media.sdk.xRTCVideo;

/**
 * Created by sunhui on 2017/9/7.
 */


import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;
import android.view.Surface;

import io.media.sdk.xRTCLogging;

public class xRTCCameraManager {
    private static final String TAG = "xRTCVideoCameraManager";

    protected static int mDefaultOritation = 90 ;
    protected static int mDisaplayOritation = 0 ;

    static class xRTCCameraInfo {
        private static int sNumberOfSystemCameras = -1;

        private static int getNumberOfCameras(Context appContext) {
            if (sNumberOfSystemCameras == -1) {
                if ((Build.VERSION.SDK_INT < 23) &&
                        (appContext.getPackageManager().checkPermission("android.permission.CAMERA", appContext
                                .getPackageName()) != 0)) {
                    sNumberOfSystemCameras = 0;
                    Log.e(TAG, "Missing android.permission.CAMERA permission, no system camera available");
                } else {
                    sNumberOfSystemCameras = xRTCVideoCapture.getNumberOfCameras();
                }
            }
            return sNumberOfSystemCameras;
        }
    }

    private static boolean isLReleaseOrLater() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static xRTCVideoCapture createVideoCapture(int id, Context context)
    {
        return new xRTCVideoCapture(context, id );
    }

    public static int getNumberOfCameras(Context appContext)
    {
        return xRTCCameraInfo.getNumberOfCameras(appContext);
    }

    public static String getDeviceName(int id, Context appContext) {

        return xRTCVideoCapture.getName(id);
    }

    public static int getDeviceOrientation(int id, Context appContext) {
        return xRTCVideoCapture.getSensorOrientation(id);
    }


    public static void  calOritation(Context context , Camera.CameraInfo info){
        mDefaultOritation =((Activity)context).getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (mDefaultOritation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mDefaultOritation = (info.orientation + degrees) % 360;
            mDefaultOritation = (360 - mDefaultOritation) % 360;   // compensate the mirror
            xRTCLogging.i(TAG,"mDefaultOritation:CAMERA_FACING_FRONT:"  + mDefaultOritation + " info.orientation:" + info.orientation);
        } else {
            // back-facing
            mDefaultOritation = ( info.orientation - degrees + 360) % 360;
            xRTCLogging.i(TAG,"mDefaultOritation:CAMERA_FACING_BACK:"  + mDefaultOritation + " info.orientation:" + info.orientation);
        }

        mDisaplayOritation = info.orientation;

        Log.i(TAG, "mDisaplayOritation:" + mDisaplayOritation + " mDefaultOritation:" + mDefaultOritation);

        xRTCLogging.i(TAG," info.orientation:" + info.orientation);
    }

    public static int GetDisaplayOritation() { return mDisaplayOritation ; }
    public static int GetDefaultOritation() { return mDefaultOritation ; }

    public static String getCapabilities(int id, Context appContext)
    {
        String cap = xRTCVideoCapture.fetchCapability(id, appContext);
        if (cap == null) {
            xRTCLogging.e(TAG, "Capability hasn't been created");
        }
        return cap;
    }
}

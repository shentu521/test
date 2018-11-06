package io.media.sdk.xRTCVideo;

/**
 * Created by sunhui on 2017/9/7.
 */


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.Surface;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.media.sdk.xRTCEngine;
import io.media.sdk.xRTCLogging;

@TargetApi(21)
public class xRTCVideoCapture2 {
    private static final boolean DEBUG = false;
    private CameraDevice mCameraDevice = null;
    private CaptureRequest.Builder mPreviewBuilder = null;
    private CameraCaptureSession mCaptureSession = null;
    private ImageReader mImageReader = null;
    private static final double kNanoSecondsToFps = 1.0E-9D;
    private static final String TAG = "xRTCVideoCapture2";
    private int mId = 0 ;
    private int mCameraNativeOrientation = 0 ;

    private static enum CameraState {
        OPENING, STARTED, EVICTED, STOPPED;

        private CameraState() {
        }
    }

    protected Context mContext ;
    private CameraState mCameraState = CameraState.STOPPED;
    private CameraManager mManager = null;
    private Handler mMainHandler = new Handler(this.mContext.getMainLooper());
    private HandlerThread mPreviewThread = null;
    private final Object mCameraStateLock = new Object();
    private int mExpectedFrameSize = 0;
    private int mCaptureWidth = -1;
    private int mCaptureHeight = -1;
    private int mCaptureFps = -1;
    private int mCaptureFormat = 35;
    private byte[] mCaptureData;
    private boolean mFaceDetectSupported;
    private int mFaceDetectMode;
    private static final MeteringRectangle[] ZERO_WEIGHT_3A_REGION = {new MeteringRectangle(0, 0, 0, 0, 0)};
    private MeteringRectangle[] mAFAERegions = ZERO_WEIGHT_3A_REGION;

    private static CameraCharacteristics getCameraCharacteristics(Context appContext, int id)
    {
        CameraManager manager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE) ;
        try {
            return manager.getCameraCharacteristics(Integer.toString(id));
        } catch (CameraAccessException ex) {
            xRTCLogging.i( TAG, "getNumberOfCameras: getCameraIdList(): " + ex);
        } catch (Exception ex) {
            xRTCLogging.i( TAG, "getNumberOfCameras: got exception: " + ex);
        }
        return null;
    }

    static boolean isLegacyDevice(Context appContext, int id) {
        try {
            CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(appContext, id);
            return (cameraCharacteristics != null) &&
                    (((Integer) cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() == 2);
        } catch (Exception e) {
           xRTCLogging.w(TAG, "this is a legacy camera device");
        }
        return true;
    }

    static int getNumberOfCameras(Context appContext) {
        CameraManager manager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            return manager.getCameraIdList().length;
        } catch (Exception ex) {
           xRTCLogging.e(TAG, "getNumberOfCameras: getCameraIdList(): ", ex);
        }
        return 0;
    }

    static String getName(int id, Context appContext) {
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(appContext, id);
        if (cameraCharacteristics == null) {
            return null;
        }
        int facing = ((Integer) cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)).intValue();

        return "camera2 " + id + ", facing " + (facing == 0 ? "front" : "back");
    }

    static int getSensorOrientation(int id, Context appContext) {
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(appContext, id);
        if (cameraCharacteristics == null) {
            return -1;
        }
        return ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();
    }

    public CameraManager.AvailabilityCallback mAvailabilityCallback = new CameraManager.AvailabilityCallback() {
        public synchronized void onCameraAvailable(String cameraId)
        {
            super.onCameraAvailable(cameraId);
            if (mCameraState ==  CameraState.EVICTED)
            {
                if ( tryOpenCamera() != 0) {
                   xRTCLogging.e(TAG, "start capture failed");
                }
            }
        }

        public synchronized void onCameraUnavailable(String cameraId) {
            super.onCameraUnavailable(cameraId);
           xRTCLogging.e(TAG, "Camera " + cameraId + " unavailable");
        }
    };

    xRTCVideoCapture2(Context context, int id)
    {
        mId = id ;
        mContext = context ;
    }

    private final CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        private long mLastFocusedTs;

        private void process(CaptureResult result) {
            Face[] faces = (Face[]) result.get(CaptureResult.STATISTICS_FACES);
            if ((faces != null) && (faces.length > 0)) {
                if (System.currentTimeMillis() - this.mLastFocusedTs < 3000L) {
                    return;
                }
                if (faces[0].getScore() <= 50) {
                    return;
                }
                mAFAERegions = new MeteringRectangle[]{new MeteringRectangle(faces[0].getBounds(), 1000)};

                addRegionsToCaptureRequestBuilder( mPreviewBuilder);
                if ( mCameraState != CameraState.STARTED) {
                    return;
                }
                try {
                     mCaptureSession.capture( mPreviewBuilder.build(),  mCaptureCallback, null);
                } catch (Exception ex) {
                   xRTCLogging.e(TAG, "capture: " + ex);
                    return;
                }

                createCaptureRequest();

                this.mLastFocusedTs = System.currentTimeMillis();
            } else {
                mAFAERegions =  ZERO_WEIGHT_3A_REGION;
            }
        }

        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            process(partialResult);
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            process(result);
        }
    };

    private class CrStateListener
            extends CameraDevice.StateCallback {
        private CrStateListener() {
        }

        public void onOpened(CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            if (doStartCapture() < 0) {
                if (mCameraState !=  CameraState.EVICTED) {
                    changeCameraStateAndNotify(CameraState.STOPPED);
                }
               xRTCLogging.e(TAG, "Camera startCapture failed!!");
               xRTCEngine.onCameraError( 0, "Error configuring camera");
            }
        }

        public void onDisconnected(CameraDevice cameraDevice) {
            if (mCameraState != CameraState.STOPPED) {
               xRTCLogging.w(TAG, "camera client is evicted by other application");

               xRTCEngine.onCameraError( 0, "Camera device evicted by other application");

               xRTCLogging.i(TAG, "Camera device enter state: EVICTED");
                if (mCameraDevice != null) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
                changeCameraStateAndNotify(CameraState.EVICTED);
                return;
            }
        }

        public void onError(CameraDevice cameraDevice, int error) {
            if (mCameraState == CameraState.EVICTED) {
                return;
            }
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            changeCameraStateAndNotify(CameraState.STOPPED);
           xRTCLogging.e(TAG, "CameraDevice Error :" + Integer.toString(error));
           xRTCEngine.onCameraError( 0, "Camera device error" +
                    Integer.toString(error));
        }
    }

    private class CaptureSessionListener
            extends CameraCaptureSession.StateCallback {
        private CaptureSessionListener() {
        }

        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            mCaptureSession = cameraCaptureSession;
            if (createCaptureRequest() != 0) {
                changeCameraStateAndNotify(CameraState.STOPPED);
                xRTCEngine.onCameraError( 0, "Fail to setup capture session");
            } else {
                changeCameraStateAndNotify(CameraState.STARTED);
            }
        }

        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
           xRTCLogging.e(TAG, "onConfigureFailed");
            if (mCameraState != CameraState.EVICTED) {
                changeCameraStateAndNotify(CameraState.STOPPED);
            }
           xRTCEngine.onCameraError( 0, "Camera session configuration error");
        }
    }

    private void changeCameraStateAndNotify(CameraState state) {
        synchronized (this.mCameraStateLock) {
            this.mCameraState = state;
            this.mCameraStateLock.notifyAll();
        }
    }

    private class ImageReaderListener
            implements ImageReader.OnImageAvailableListener {
        private ImageReaderListener() {
        }

        public void onImageAvailable(ImageReader reader) {
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image == null) {
                    return;
                }
                if ((image.getFormat() != 35) ||
                        (image.getPlanes().length != 3)) {
                   xRTCLogging.e(TAG, "Unexpected image format: " + image.getFormat() + "or #planes:" + image
                            .getPlanes().length);
                    return;
                }
                if ((reader.getWidth() != image.getWidth()) ||
                        (reader.getHeight() != image.getHeight())) {
                    throw new IllegalStateException("ImageReader size " + reader.getWidth() + "x" + reader.getHeight() + " did not match Image size: " + image.getWidth() + "x" + image.getHeight());
                }
                readImageIntoBuffer(image, mCaptureData);

                xRTCEngine.processCameraFrame(mCaptureData, mExpectedFrameSize );

            } catch (IllegalStateException ex) {
               xRTCLogging.e(TAG, "acquireLastest Image():", ex);
                return;
            } finally {
                if (image != null) {
                    image.close();
                }
            }
        }
    }

    private int createCaptureRequest() {
        try {
            this.mCaptureSession.setRepeatingRequest(this.mPreviewBuilder.build(), this.mCaptureCallback, null);
        } catch (CameraAccessException ex) {
           xRTCLogging.e(TAG, "setRepeatingRequest: ", ex);
            return -1;
        } catch (IllegalArgumentException ex) {
           xRTCLogging.e(TAG, "setRepeatingRequest: ", ex);
            return -2;
        } catch (SecurityException ex) {
           xRTCLogging.e(TAG, "setRepeatingRequest: ", ex);
            return -3;
        } catch (IllegalStateException ex) {
           xRTCLogging.e(TAG, "capture:" + ex);
            return -4;
        }
        return 0;
    }

    private static void readImageIntoBuffer(Image image, byte[] data) {
        int width = image.getWidth();
        int height = image.getHeight();
        Image.Plane[] planes = image.getPlanes();
        int offset = 0;
        for (int plane = 0; plane < planes.length; plane++) {
            ByteBuffer buffer = planes[plane].getBuffer();
            int rowStride = planes[plane].getRowStride();
            int pixelStride = planes[plane].getPixelStride();
            int planeWidth = plane == 0 ? width : width / 2;
            int planeHeight = plane == 0 ? height : height / 2;
            if ((pixelStride == 1) && (rowStride == planeWidth)) {
                buffer.get(data, offset, planeWidth * planeHeight);
                offset += planeWidth * planeHeight;
            } else {
                byte[] rowData = new byte[rowStride];
                for (int row = 0; row < planeHeight - 1; row++) {
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < planeWidth; col++) {
                        data[(offset++)] = rowData[(col * pixelStride)];
                    }
                }
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()));
                for (int col = 0; col < planeWidth; col++) {
                    data[(offset++)] = rowData[(col * pixelStride)];
                }
            }
        }
    }

    private int tryOpenCamera() {
        CrStateListener stateListener = new CrStateListener();
        try {
            mManager.openCamera(Integer.toString(this.mId), stateListener, this.mMainHandler) ;
        } catch (CameraAccessException ex) {
           xRTCLogging.e(TAG, "allocate: manager.openCamera: ", ex);
            return -1;
        } catch (IllegalArgumentException ex) {
           xRTCLogging.e(TAG, "allocate: manager.openCamera: ", ex);
            return -2;
        } catch (SecurityException ex) {
           xRTCLogging.e(TAG, "allocate: manager.openCamera: ", ex);
            return -3;
        } catch (Exception ex) {
           xRTCLogging.e(TAG, "unknown error", ex);
            return -4;
        }
        return 0;
    }

    private void addRegionsToCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(2));
        builder.set(CaptureRequest.CONTROL_AE_REGIONS, this.mAFAERegions);
        builder.set(CaptureRequest.CONTROL_AF_REGIONS, this.mAFAERegions);
        builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
        builder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
    }

    private int doStartCapture() {
        int maxImages = 2;

        int bufSize = this.mCaptureWidth * this.mCaptureHeight * ImageFormat.getBitsPerPixel(this.mCaptureFormat) / 8;

        this.mExpectedFrameSize = bufSize;
        this.mCaptureData = new byte[this.mExpectedFrameSize];
        this.mImageReader = ImageReader.newInstance(this.mCaptureWidth, this.mCaptureHeight, this.mCaptureFormat, 2);
        if (this.mPreviewThread == null) {
            this.mPreviewThread = new HandlerThread("CameraPreview");
            this.mPreviewThread.start();
        }
        Handler backgroundHandler = new Handler(this.mPreviewThread.getLooper());
        ImageReaderListener imageReaderListener = new ImageReaderListener();

        this.mImageReader.setOnImageAvailableListener(imageReaderListener, backgroundHandler);
        try {
            this.mPreviewBuilder = this.mCameraDevice.createCaptureRequest( CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException ex) {
           xRTCLogging.e(TAG, "createCaptureRequest: ", ex);
            return -1;
        } catch (IllegalArgumentException ex) {
           xRTCLogging.e(TAG, "createCaptureRequest: ", ex);
            return -2;
        } catch (SecurityException ex) {
           xRTCLogging.e(TAG, "createCaptureRequest ", ex);
            return -3;
        }
        if (this.mPreviewBuilder == null) {
           xRTCLogging.e(TAG, "mPreviewBuilder error");
            return -4;
        }
        this.mPreviewBuilder.addTarget(this.mImageReader.getSurface());

        this.mPreviewBuilder.set(CaptureRequest.CONTROL_MODE,
                Integer.valueOf(1));

        setFaceDetect(  mPreviewBuilder, mFaceDetectMode);

        List<Surface> surfaceList = new ArrayList(1);
        surfaceList.add(this.mImageReader.getSurface());

        CaptureSessionListener captureSessionListener = new CaptureSessionListener();
        try {
            this.mCameraDevice.createCaptureSession(surfaceList, captureSessionListener, null);
        } catch (CameraAccessException ex) {
           xRTCLogging.e(TAG, "createCaptureSession :", ex);
            return -1;
        } catch (IllegalArgumentException ex) {
           xRTCLogging.e(TAG, "createCaptureSession :", ex);
            return -2;
        } catch (SecurityException ex) {
           xRTCLogging.e(TAG, "createCaptureSession :", ex);
            return -3;
        }
        return 0;
    }

    private int doStopCapture() {
        if (this.mPreviewThread != null) {
            this.mPreviewThread.quitSafely();
            this.mPreviewThread = null;
        }
        try {
            this.mCaptureSession.abortCaptures();
        } catch (CameraAccessException ex) {
           xRTCLogging.e(TAG, "abortCaptures: ", ex);

            return -1;
        } catch (IllegalStateException ex) {
           xRTCLogging.e(TAG, "abortCaptures: ", ex);

            return -1;
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.close();
            this.mCameraDevice = null;
        }
        return 0;
    }

    public int allocate() {
        synchronized (this.mCameraStateLock) {
            if (this.mCameraState == CameraState.OPENING) {
               xRTCLogging.e(TAG, "allocate() invoked while Camera is busy opening/configuring");

                return -1;
            }
        }
        CameraCharacteristics cameraCharacteristics = getCameraCharacteristics(this.mContext, this.mId);
        if (cameraCharacteristics == null) {
            return -1;
        }
        if ( xRTCVideoCapture.fetchCapability(this.mId, this.mContext) == null) {
            createCapabilities(this.mId, this.mContext);
        }

        mCameraNativeOrientation = ((Integer) cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)).intValue();

        this.mManager = ((CameraManager) this.mContext.getSystemService( Context.CAMERA_SERVICE ));

        int[] FDM = (int[]) cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        int maxFDCount = ((Integer) cameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)).intValue();
        if (FDM.length > 0) {
            List<Integer> fdList = new ArrayList();
            for (int FaceDMode : FDM) {
                fdList.add(Integer.valueOf(FaceDMode));
            }
            if (maxFDCount > 0) {
                this.mFaceDetectSupported = true;
                this.mFaceDetectMode = ((Integer) Collections.max(fdList)).intValue();
            }
        }
       xRTCLogging.i(TAG, "allocate() face detection: " + this.mFaceDetectMode + " " + maxFDCount + " " + this.mFaceDetectSupported);

        this.mManager.registerAvailabilityCallback(this.mAvailabilityCallback, this.mMainHandler);
        return 0;
    }

    public int setCaptureFormat(int format) {
        int androidFormat = xRTCVideoCapture.translateToAndroidFormat(format);
        if (androidFormat != this.mCaptureFormat) {
           xRTCLogging.e(TAG, "For camera2 api, only YUV_420_888 format are supported");
            return -1;
        }
        return 0;
    }

    public int startCapture(int width, int height, int frameRate) {
       xRTCLogging.d(TAG, "startCapture, w=" + width + ", h=" + height + ", fps=" + frameRate);

        this.mCaptureWidth = width;
        this.mCaptureHeight = height;
        this.mCaptureFps = frameRate;
        synchronized (this.mCameraStateLock) {
            while ((this.mCameraState != CameraState.STARTED) && (this.mCameraState != CameraState.EVICTED) && (this.mCameraState != CameraState.STOPPED)) {
                try {
                    this.mCameraStateLock.wait();
                } catch (InterruptedException ex) {
                   xRTCLogging.e(TAG, "CaptureStartedEvent: ", ex);
                }
            }
            if (this.mCameraState == CameraState.STARTED) {
                return 0;
            }
        }
        changeCameraStateAndNotify(CameraState.OPENING);
        return tryOpenCamera();
    }

    public int stopCapture() {
        synchronized (this.mCameraStateLock) {
            while ((this.mCameraState != CameraState.STARTED) && (this.mCameraState != CameraState.EVICTED) && (this.mCameraState != CameraState.STOPPED)) {
                try {
                    this.mCameraStateLock.wait();
                } catch (InterruptedException ex) {
                   xRTCLogging.e(TAG, "CaptureStartedEvent: ", ex);
                }
            }
            if (this.mCameraState == CameraState.EVICTED) {
                this.mCameraState = CameraState.STOPPED;
            }
            if (this.mCameraState == CameraState.STOPPED) {
                return 0;
            }
        }
        doStopCapture();
        changeCameraStateAndNotify(CameraState.STOPPED);
        return 0;
    }

    public void deallocate() {
        this.mManager.unregisterAvailabilityCallback(this.mAvailabilityCallback);
    }

    private void setFaceDetect(CaptureRequest.Builder requestBuilder, int faceDetectMode) {
        if (this.mFaceDetectSupported) {
            requestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, Integer.valueOf(faceDetectMode));
        }
    }

    public static int createCapabilities(int id, Context context) {
        String cap = null;
        CameraCharacteristics ch = getCameraCharacteristics(context, id);
        if (ch == null) {
            return -1;
        }
        StreamConfigurationMap map = (StreamConfigurationMap) ch.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
           xRTCLogging.e(TAG, "Failed to create capabilities");
            return -1;
        }
       xRTCLogging.e(TAG, "dump configuration map:" + map.toString());

        List<Size> sizes = new ArrayList(Arrays.asList(map
                .getOutputSizes(35)));

        String cap_id = "\"id\":" + id + ",";
        String cap_res_header = "\"resolution\":";
        String cap_res_value = "";
        for (int i = 0; i < sizes.size(); i++) {
            String ss = "{\"w\":" + ((Size) sizes.get(i)).getWidth() + ",\"h\":" + ((Size) sizes.get(i)).getHeight() + "}";
            if (i != sizes.size() - 1) {
                cap_res_value = cap_res_value + ss + ",";
            } else {
                cap_res_value = cap_res_value + ss;
            }
        }
        String cap_fmt_header = "\"format\":";
        String cap_fmt_value = "" + xRTCVideoCapture.translateToEngineFormat(35);
        String cap_fps_header = "\"fps\":";
        String cap_fps_value = "30";
        cap = "{" + cap_id + cap_res_header + "[" + cap_res_value + "]," + cap_fmt_header + "[" + cap_fmt_value + "]," + cap_fps_header + "[" + cap_fps_value + "]}";

        xRTCVideoCapture.cacheCapability(id, context, cap);

        return 0;
    }
}

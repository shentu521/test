package io.media.sdk.xRTCVideo;

import android.util.Log;
import android.widget.Switch;

import io.media.sdk.xRTCLogging;

/**
 * Created by sunhui on 2017/9/7.
 */

public class xRTCProfile {
    private static final String TAG ="xRTCProfile" ;
    public static final int VIDEO_CAPTURE_TYPE_16X9 = 0x000;
    public static final int VIDEO_SIZE_160 = 1;
    public static final int VIDEO_SIZE_320 = 2;
    public static final int VIDEO_SIZE_480 = 3;
    public static final int VIDEO_SIZE_640 = 4;
    public static final int VIDEO_SIZE_800 = 5;
    public static final int VIDEO_SIZE_960 = 6;
    public static final int VIDEO_SIZE_1280 = 8;
    public static final int VIDEO_SIZE_1920 = 0xf;

    // normal
    public static final int VIDEO_FRAME_COUNT_NORMAL = 0; //15 ;
    public static final int VIDEO_FRAME_COUNT_HIGH = 1; //20 ;
    public static final int VIDEO_FRAME_COUNT_MAX = 2; // 30

    public static final int DEF_VIDEO_CODEC_TYPE = 0; // x264 VIDEO_CODEC_TYPE_H264
    public static final int VIDEO_CODEC_TYPE_H264 = 1;
    public static final int VIDEO_CODEC_TYPE_X264 = 2;
    public static final int VIDEO_CODEC_TYPE_HARD264 = 3; // 硬件编码
    public static final int VIDEO_CODEC_TYPE_VP8 = 4;
    public static final int VIDEO_CODEC_TYPE_VP9 = 8;
    public static final int VIDEO_CODEC_TYPE_HIGH264 = 9;

    public static final int VIDEO_CAPTURE_TYPE_16X9_160 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_160;   // 160*120
    public static final int VIDEO_CAPTURE_TYPE_16X9_320 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_320;
    public static final int VIDEO_CAPTURE_TYPE_16X9_480 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_480;
    public static final int VIDEO_CAPTURE_TYPE_16X9_640 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_640;
    public static final int VIDEO_CAPTURE_TYPE_16X9_800 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_800;
    public static final int VIDEO_CAPTURE_TYPE_16X9_960 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_960;
    public static final int VIDEO_CAPTURE_TYPE_16X9_1280 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_1280;   // 1280*720
    public static final int VIDEO_CAPTURE_TYPE_16X9_1920 = VIDEO_CAPTURE_TYPE_16X9 + VIDEO_SIZE_1920;


    public static final int VIDEO_CAPTURE_TYPE_4X3 = 0x100;
    public static final int VIDEO_CAPTURE_TYPE_4X3_160 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_160;   // 160*120
    public static final int VIDEO_CAPTURE_TYPE_4X3_320 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_320;
    public static final int VIDEO_CAPTURE_TYPE_4X3_480 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_480;
    public static final int VIDEO_CAPTURE_TYPE_4X3_640 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_640;
    public static final int VIDEO_CAPTURE_TYPE_4X3_800 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_800;
    public static final int VIDEO_CAPTURE_TYPE_4X3_960 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_960;
    public static final int VIDEO_CAPTURE_TYPE_4X3_1280 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_1280;   // 1280*720
    public static final int VIDEO_CAPTURE_TYPE_4X3_1920 = VIDEO_CAPTURE_TYPE_4X3 + VIDEO_SIZE_1920;   // 1280*720

    public static final int VIDEO_CAPTURE_TYPE_16X16 = 0x200;

    public static final int VIDEO_CODEC_TYPE_POS = 12;
    public static final int VIDEO_FRAME_COUNT_TYPE_POS = 16;
    public static int mZoomWidth;
    public static int mZoomHeight;
    public static int mFrameCount;
    public static int mCodecType = 0 ;
    public static int mRotate;
    public static int mExchange;
    public static int mFixType;
    public static int mRadio = VIDEO_CAPTURE_TYPE_16X9;

    public static boolean AnalyzeCaptureSize(int uProfile, boolean swapSize) {
        int uSizeType = uProfile & 0xff;
        int nFrameType = (uProfile >> VIDEO_FRAME_COUNT_TYPE_POS) & 0xf;
        int nCodecType = (uProfile >> VIDEO_CODEC_TYPE_POS) & 0xf ;

        Log.e("ccddccdd",nCodecType + "");


        if ( nCodecType != DEF_VIDEO_CODEC_TYPE &&
                nCodecType != VIDEO_CODEC_TYPE_H264 &&
                nCodecType != VIDEO_CODEC_TYPE_X264 &&
                nCodecType != VIDEO_CODEC_TYPE_HARD264 &&
                nCodecType != VIDEO_CODEC_TYPE_VP8 &&
                nCodecType != VIDEO_CODEC_TYPE_VP9 &&
                nCodecType !=VIDEO_CODEC_TYPE_HIGH264 ) {
            return false;
        }

        mCodecType = nCodecType ;
        switch (nFrameType) {
            case VIDEO_FRAME_COUNT_NORMAL:
                mFrameCount = 15;
                break;
            case VIDEO_FRAME_COUNT_HIGH:
                mFrameCount = 20;
                break;
            case VIDEO_FRAME_COUNT_MAX:
                mFrameCount = 30;
                break;
            default: {
                mFrameCount = 15;
            }
        }


        mZoomWidth = 160 * uSizeType ;
        mFrameCount = 15;

        int uRadio = (uProfile & 0xf00);

        switch (uRadio) {
            case VIDEO_CAPTURE_TYPE_16X9:
                mZoomHeight = uSizeType * 90 ;
                break ;

            case VIDEO_CAPTURE_TYPE_4X3:
                mZoomHeight = uSizeType * 120 ;
                break ;

            case VIDEO_CAPTURE_TYPE_16X16:
                mZoomHeight = uSizeType * 160 ;
                break ;

            default:
                return false;
        }

        if ( swapSize )
        {
            int nTemp = mZoomWidth ;
            mZoomWidth = mZoomHeight ;
            mZoomHeight = nTemp ;
        }


        xRTCLogging.i(TAG, "profile w:"+mZoomWidth+" h:"+mZoomHeight );

        return true;
    }


}

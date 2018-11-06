package io.media.sdk.xRTCVideo;

/**
 * Created by sunhui on 2018/2/16.
 */

public abstract class xRTCVideoEventHandler {

    // 采集回调函数
    public abstract boolean onCaptureFrame(byte[] yuvBuffer, int width, int height, int stride );

    // 渲染回调函数
    public abstract boolean onRenderFrame(byte[] yuvBuffer, int width, int height, int stride , long UserId);
}

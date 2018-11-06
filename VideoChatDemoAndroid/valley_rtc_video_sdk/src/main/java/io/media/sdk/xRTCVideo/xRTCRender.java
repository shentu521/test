package io.media.sdk.xRTCVideo;

/**
 * Created by sunhui on 2017/9/5.
 */

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class xRTCRender {
    private static SurfaceHolder g_Renderer;

    public static SurfaceView CreateLocalRenderer(Context context) {
        return new SurfaceView(context);
    }

    public static SurfaceHolder GetLocalRenderer() {
        return g_Renderer;
    }

    public static void setLocalView(SurfaceView local, int top, int left, int width, int height) {
        if (local == null) {
            g_Renderer = null;
        } else {
            g_Renderer = local.getHolder();
        }
    }
}

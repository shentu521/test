package io.media.sdk.xRTCVideo;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import io.media.sdk.xRTCEngine;
import io.media.sdk.xRTCLogging;

/**
 * Created by sunhui on 2017/9/5.
 */

public class xRTCVideoCanvas implements android.view.SurfaceHolder.Callback
{
    /*
#define RENDER_TYPE_FULL        ( 0 )       // 拉升或者缩放适应尺寸
#define RENDER_TYPE_ADAPTIVE    ( 1 )       // 自适应 如果尺寸比例有差异 上下、左右加黑边
#define RENDER_TYPE_CROP        ( 2 )       // 裁剪原始图片以适应显示view
#define RENDER_TYPE_AUTO        ( 3 )       // 自适应选择最佳模式
    * */
    public static final int RENDER_TYPE_FULL = 0 ;
    public static final int RENDER_TYPE_ADAPTIVE =  1 ;
    public static final int RENDER_TYPE_CROP = 2 ;
    public static final int RENDER_TYPE_AUTO = 3 ;

    protected static final String TAG = "xRTCVideoCanvas";
    protected SurfaceView   m_View ;
    protected int           m_Rendertype  = RENDER_TYPE_FULL ;
    protected long          m_Userid ;
    protected boolean       m_bLocal = false ;
    protected boolean       m_Free = true ;


    public xRTCVideoCanvas(SurfaceView view, int rendertype, long userid)
    {
        if ( rendertype > RENDER_TYPE_AUTO )
        {
            rendertype = RENDER_TYPE_AUTO ;
        }

        m_View = view ;
        m_Rendertype = rendertype ;
        m_Userid = userid ;

    }

    public SurfaceView getView()  {  return m_View ; }
    public long getUID() { return m_Userid ; }

    public void setLocal( boolean bLocal )
    {
        m_bLocal = bLocal ;
    }

    public void attachUser()
    {
        if ( !m_Free  )
        {
            return ;
        }

        SurfaceHolder holder = m_View.getHolder() ;
        m_View.setVisibility(View.VISIBLE) ;
        if ( holder.getSurface()!= null && holder.getSurface().isValid() )
        {
            surfaceCreated( holder ) ;
        }
        m_View.getHolder().addCallback( this );
        m_Free = false ;
    }

    public void freeUser()
    {
        m_View.getHolder().removeCallback( this ) ;
        m_View.setVisibility( View.INVISIBLE ) ;
    }

    public void detachUser()
    {
        if ( m_Free )
        {
            return;
        }

        m_Free = true ;
        if ( !m_bLocal ) {
            xRTCEngine.removeVideoView(m_Userid);
        }
        else
        {
            xRTCEngine.removeLocalVideoView( 0 ) ;
        }
    }

    //在surface的大小发生改变时激发
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        xRTCLogging.i( TAG, " userid:"+m_Userid+" surfaceChanged...");
    }

    //在surface的大小发生改变时激发
    public void surfaceCreated(SurfaceHolder holder)
    {
        xRTCLogging.i( TAG, " userid:"+m_Userid+" surfaceCreated...") ;
        if ( !m_bLocal ) {
            xRTCEngine.addVideoView( m_Userid, holder.getSurface(), m_Rendertype);
        }
        else
        {
            xRTCEngine.addLocalVideoView( holder.getSurface(), m_Rendertype ) ;
        }

    }

    //在创建时激发，一般在这里调用画图的线程。
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        xRTCLogging.i( TAG, " userid:"+m_Userid+" surfaceDestroyed...");
        if ( !m_bLocal ) {
            //detachUser();
        }
        else
        {
            //xRTCEngine.removeLocalVideoView( 0 ) ;
        }
    }

}

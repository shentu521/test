package io.media.sdk;

import android.util.Log;

/**
 * Created by sunhui on 2017/9/5.
 */

public class xRTCLogging
{
    private static final int LOG_DETAIL = 0 ;
    private static final int LOG_INFO = 1;
    private static final int LOG_WARN = 2;
    private static final int LOG_DEBUG = 3;
    private static final int LOG_ERROR = 4;


    private static void log(int level, String tag, String strLog)
    {
        xRTCEngine.writeLog(level, "[" + tag + "] " + strLog);
    }

    public static void d(String strLog)
    {
        xRTCEngine.writeLog( LOG_DEBUG, strLog);
    }
    public static void i(String strLog)
    {
        xRTCEngine.writeLog( LOG_INFO, strLog);
    }
    public static void e(String strLog)
    {
        xRTCEngine.writeLog( LOG_ERROR, strLog);
    }
    public static void w(String strLog)
    {
        xRTCEngine.writeLog( LOG_WARN, strLog);
    }
    public static void a(String strLog)
    {
        xRTCEngine.writeLog( LOG_DETAIL, strLog);
    }


    public static void d(String tag, String strLog)
    {
        log(LOG_DEBUG, tag, strLog);
    }

    public static void i(String tag, String strLog)
    {
        log( LOG_INFO, tag, strLog);
    }

    public static void e(String tag, String strLog)
    {
        log( LOG_ERROR, tag, strLog);
    }

    public static void w(String tag, String strLog)
    {
        log( LOG_WARN, tag, strLog);
    }

    public static void e(String tag, String strLog, Throwable e)
    {
        log( LOG_ERROR, tag, strLog);
        log( LOG_ERROR, tag, e.toString());
        log( LOG_ERROR, tag, Log.getStackTraceString(e));
    }
}

package org.vallery.videochatdemoandroid;

import android.app.Activity;
import android.app.Application;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import valley.api.IRtcChannel;
import valley.api.ValleyRtcAPI;

/**
 * Created by apple on 18/6/1.
 */

public class ApplicationEx extends Application
{
    protected IRtcChannel m_pClient  = null;
    protected Activity m_activemain  = null;
    protected Activity  	m_activelogin = null;
    protected Watcher   	m_watcher     = null;
    boolean             	m_bInit       = false;
    public static String  user_default = "";
    public static String  room_type = "";

    public void chmod777(File file, String root)
    {
        try
        {
            if (null == file || !file.exists())
            {
                return;
            }

            Runtime.getRuntime().exec("chmod 777 " + file.getAbsolutePath());
            File tempFile = file.getParentFile();
            String tempName = tempFile.getName();
            if (tempFile.getName() == null || "".equals(tempName))
            {
                return;
            }
            else if (!root.equals("") && root.equals(tempName))
            {
                Runtime.getRuntime().exec("chmod 777 " + tempFile.getAbsolutePath());
                return;
            }

            chmod777(file.getParentFile(), root);

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void mkdirs(String path)
    {
        try
        {
            File file = new File(path);
            if (!file.isDirectory())
            {
                if (!file.mkdirs())
                {
                    Log.d("ApplicationEx","mkdir failed");
                }
                chmod777(new File(path), null);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void InitSDK()
    {
        if(!m_bInit)
        {
            m_bInit      = true;
            String mDataPath = "";
            try
            {
                File file = Environment.getExternalStorageDirectory();

                if(!file.exists())
                    file = Environment.getDataDirectory();

                if(!file.exists())
                {
                    file =  this.getApplicationContext().getFilesDir();
                }

                mDataPath = file.getAbsolutePath() + "/Xrtc";
                mkdirs(mDataPath);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            ValleyRtcAPI.InitSDK(this, mDataPath,"");//mDataPath为谷人云相关目录，可以自定义
            //ValleyRtcAPI.InitSDK(this,mDataPath,"<?xml version=\"1.0\" encoding=\"utf-8\"?><root><localversion>1</localversion></root>");
            ValleyRtcAPI.SetAuthoKey("5b001900bc0366fcEnnQxsE");//SetAuthoKey为注册函数，填入从谷人申请的id，留空为测试
 //           m_pClient = ValleyRtcAPI.CreateChannel(false,this);
            m_watcher = new Watcher();
            m_watcher.start();

        }
    }

    public void registerActive(Activity a, boolean bMain)
    {
        if(bMain)
            m_activemain = a;
        else
            m_activelogin = a;
    }

    public void exit()
    {
        m_watcher.Stop();

        Log.d("Application",  "exit");

        if(null != m_activemain)
            m_activemain.finish();

        if(null != m_activelogin)
            m_activelogin.finish();

        if(null != m_pClient){
            m_pClient.Release();
            m_pClient = null;
        }
        ValleyRtcAPI.CleanSDK();
        System.exit(0);
    }

    private class Watcher extends Thread
    {

        String  command = "top -n 1 -m 3";
        public  boolean runing  = true;
        public  long    active_tick = SystemClock.uptimeMillis() - 5000;

        public void run()
        {
            Log.d("Application", "watch thread run");
            Process process = null;
            InputStream instream = null;
            BufferedReader bufferReader = null;
            int time_out = 10*1000;

            try
            {
                while(runing)
                {
                    if(SystemClock.uptimeMillis() - active_tick < 5000 && time_out < 500)
                    {
                        String result = "";
                        ProcessBuilder builder = new ProcessBuilder(command);
                        process = Runtime.getRuntime().exec(command);
                        instream = process.getInputStream();
                        bufferReader = new BufferedReader(new InputStreamReader(instream, "utf-8"));
                        Log.d("Application", "read cpu");
                        String readline;
                        while (runing && null != (readline = bufferReader.readLine()))
                        {
                            if(readline.contains("com.bodtech."))
                            {
                                result += readline+"\n";
                            }
                        }

                        process.destroy();
                        time_out   =  10*1000;

                        if(null != m_pClient)
                        {
                            ;
                        }
                        Log.d("Application", result);
                    }

                    if(runing)
                    {
                        Thread.sleep(500);
                        if(time_out > 0)
                            time_out -= 500;
                    }
                }

            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            Log.d("Application", "watch thread end");
        }


        public void Stop()
        {
            runing = false;
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
package com.audio.device;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.support.v4.content.ContextCompat;

public class DeviceNative {

	static ContextRef m_ctx = null;
	static private native void OnDeviceEvent(long evtid, int state);
	
	static public void DeviceEvent(long evtid, int state)
	{
		if(null == m_listener)
			return;
		
		OnDeviceEvent(evtid, state);
	}
	 
	
	static public void  StartListen(ContextRef ctx){

		m_ctx = ctx;
		if(null == m_listener)
			m_listener = new DeviceListener(ctx);
		
		m_listener.StartListen();
	}
	
	static public void StopListen(){
		if(null != m_listener) {
			m_listener.StopListen();
			m_listener = null;
		}
	}
	 
	static public String GetDeviceID(int type)
	{
		if(0 == type){
			String model = Build.MODEL;
			String menufacture = Build.MANUFACTURER;

			model.toLowerCase();
			menufacture.toLowerCase();
			int indexManuFacture = model.indexOf(menufacture);
			if(0 == indexManuFacture) {
				model = model.substring(menufacture.length());
				if(!model.isEmpty())
				{
					char sp = model.charAt(0);
					if(' ' == sp || '+' == sp || '-' == sp || '_' == sp || '+' == sp){
						model = model.substring(1);
					}
					else{
						model = Build.MODEL;
						model.toLowerCase();
					}
				}
				else{
					model = Build.MODEL;
					model.toLowerCase();
				}
			}

			String version = Build.VERSION.RELEASE;
			version.toLowerCase();

			String strBuildInfo =String.format("c:%s|id:%s|v:%s", menufacture, model, version );

			Log.i("AudioDevice",strBuildInfo);
			return strBuildInfo;
		}
		else if(1 == type) {

			String device = Build.DEVICE;
			String brand = Build.BRAND;

			device.toLowerCase();
			brand.toLowerCase();

			String version = Build.VERSION.RELEASE;
			version.toLowerCase();

			String strBuildInfo =String.format("c:%s|id:%s|v:%s", brand, device, version);
			Log.i("AudioDevice",strBuildInfo);
			return strBuildInfo;
		}
		else
		{
			Log.i("AudioDevice","c: null, type="+type);
			return "";
		}
	}

	static public void SetSpeakerphoneOn(int On)
	{
		if(null == m_listener) {
			Log.e("AudioDeviceListener","SetSpeakerphoneOn m_listener is null" );
			return;
		}

		m_listener.SetSpeakerphoneOn(On);
	}

	static public boolean GetSpeakerphoneOn()
	{
		if(null == m_listener) {
			Log.e("AudioDeviceListener","GetSpeakerphoneOn m_listener is null" );
			return false;
		}

		return m_listener.GetSpeakerphoneOn();
	}

	static  public boolean checkPermissions(int v) {
		if(null == m_ctx)
			return true;

		if(0 == v) {
			if (Build.VERSION.SDK_INT >= 23) {
				if (ContextCompat.checkSelfPermission(m_ctx.getContext(), Manifest.permission.RECORD_AUDIO) !=
						PackageManager.PERMISSION_GRANTED) {
					//代表没有授权
					return false;
				} else {

					//已经授权
				}
			} else {
				//低于23不需要关心这个权限
			}
		}

		return  true;
	}


	static DeviceListener m_listener = null; 
}

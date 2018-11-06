package com.audio.device;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
 

public class DeviceListener{
 
 	private static final int  AUDIO_INTERRUPT_RESUME = 0; 
 	private static final int  AUDIO_INTERRUPT_PAUSE  = 1; 
 	private static final int  AUDIO_HEADERSET_OUT  = 0;
	private static final int  AUDIO_HEADERSET_INSERTED_NOMIC  = 1;
	private static final int  AUDIO_HEADERSET_INSERTED_WITHMIC = 2;


	private static final int event_type_phone_state = 0;
 	private static final int event_type_headset_plugin = 1;
 	private static final int event_type_play_volume = 2;
 	private static final int event_type_network_state = 3;
 	
 	
 	protected boolean     m_bMySpeakerOn             = true;	
 	protected boolean     m_bDevSpeakerOn            = true;
 	protected Context     m_ctx                      = null;
 	protected theLintener m_spkListenner             = null;
 	protected HeadsetPlugListenner m_HeadsetListenner= null;
 	protected SystemVolumeReceiver m_volumeListenner = null;
 	protected NetworkStateReceiver m_networkListenner= null;
 	protected boolean     m_bInterrupted             = false;
 	protected int         m_nBeforeInsertSpkOn       = -1;
    protected boolean     m_bHeadsetInserted         = false;
	protected boolean     m_bHeadsetWithmic         = false;


	protected boolean     m_bStartListen	         = false; //start listen flag
    
 //   private  int m_cur_volume_sc = 0;				//璁板綍褰撳墠鐩戝惉鐘舵�锛屽綋璋冪敤SetHostID鏃剁涓�寮�璁剧疆Audio Engine  3/17/2017
    private  int m_cur_net_type = 0;
    private  int m_cur_phone_state = 0;
    private  int m_cur_headset_plugin_state = 0; 
    
    
	public DeviceListener(ContextRef ctx)
	{
 		m_ctx    = ctx.getContext();
 		
 		try
 		{
 			m_spkListenner     = new theLintener();
 		}
 		catch(Exception ex)
 		{
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 			m_spkListenner = null;
 		}
 		
 		try
 		{
 			m_HeadsetListenner = new HeadsetPlugListenner();
 		}
 		catch(Exception ex)
 		{
 			m_HeadsetListenner = null;
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		}
 		
 		try
 		{
 			m_volumeListenner  = new SystemVolumeReceiver();
 		}
 		catch(Exception ex)
 		{
 			m_volumeListenner = null;
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		} 
 		
 		try
 		{
 			m_networkListenner  = new NetworkStateReceiver();
 		}
 		catch(Exception ex)
 		{
 			m_volumeListenner = null;
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		} 
	}
	
  
	
 	class theLintener extends PhoneStateListener
 	{
	  
	        @Override
	        public void onCallStateChanged(int state, String incomingNumber) 
	        {
	        	boolean bPause = TelephonyManager.CALL_STATE_IDLE != state;

	        	AudioManager audioManager = (AudioManager)m_ctx.getSystemService(Context.AUDIO_SERVICE);	        	        
	        	
	        	if(bPause)
	        	{
	        		//璁颁綇鎴戜滑鐨勶紝璁剧疆绯荤粺鐨�
	        		m_bInterrupted = true;
	        		if(!m_bHeadsetInserted)
	        		{
		        		m_bMySpeakerOn = audioManager.isSpeakerphoneOn();
		        		audioManager.setSpeakerphoneOn(m_bDevSpeakerOn); 	        			
	        		}
	        		
	        		m_cur_phone_state = AUDIO_INTERRUPT_PAUSE;
	        		
	        		DeviceNative.DeviceEvent(event_type_phone_state, m_cur_phone_state);
	        	}
	        	else
	        	{
	        		//璁颁綇鎴戜滑鐨勶紝璁剧疆绯荤粺鐨�
	        		m_bInterrupted  = false;
					//audioManager.setSpeakerphoneOn(true);
					if(audioManager.isWiredHeadsetOn()){
						//audioManager.setSpeakerphoneOn(false);
						//m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
						//DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						//m_nBeforeInsertSpkOn = 1;
						//m_bHeadsetInserted = true;
						//Log.d("AudioDeviceListener","wiredheadset connected when the app resume:");
					}
					else{
						if(audioManager.isBluetoothA2dpOn()){
							try {
								audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
								audioManager.setSpeakerphoneOn(false);
								audioManager.startBluetoothSco();
								audioManager.setBluetoothScoOn(true);
								m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
								DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
								Log.d("AudioDeviceListener","Bluetooth device connected when the app resume");
							}catch (Exception ex){
								audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
								audioManager.stopBluetoothSco();
								audioManager.setBluetoothScoOn(false);
								audioManager.setSpeakerphoneOn(true);
								//am.setBluetoothA2dpOn(true);
								m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
								DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
								Log.d("AudioDeviceListener","Bluetooth Exception:" + ex.toString());
							}
							m_nBeforeInsertSpkOn = 1;
						}else if(audioManager.isBluetoothScoOn()){
							try {
								audioManager.setMode(AudioManager.MODE_NORMAL);
								audioManager.stopBluetoothSco();
								audioManager.setBluetoothScoOn(false);
								audioManager.setSpeakerphoneOn(false);
								audioManager.startBluetoothSco();
								audioManager.setBluetoothScoOn(true);
								m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
								DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
								Log.d("AudioDeviceListener","Bluetooth device connected during resume valleyren and mode is not a2dp");
							}catch (Exception ex){
								audioManager.setMode(AudioManager.MODE_NORMAL);
								audioManager.stopBluetoothSco();
								audioManager.setBluetoothScoOn(false);
								audioManager.setSpeakerphoneOn(true);
								//audioManager.setBluetoothA2dpOn(true);
								m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
								DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
								Log.d("AudioDeviceListener","Bluetooth Exception:" + ex.toString());
							}
						}
						else{
							audioManager.setMode(audioManager.isBluetoothA2dpOn() ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
							audioManager.stopBluetoothSco();
							audioManager.setBluetoothScoOn(false);
							audioManager.setSpeakerphoneOn(true);
							m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
							DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
							m_nBeforeInsertSpkOn = -1;
						}
					}
	        		m_cur_phone_state = AUDIO_INTERRUPT_RESUME;
	        		
	        		DeviceNative.DeviceEvent(event_type_phone_state, m_cur_phone_state);   
	        	}
	        	
	            super.onCallStateChanged(state, incomingNumber);
	        }  
	        
	        public void StartListen(){
	        	Log.d("AudioDeviceListener", "AudioDeviceListener StartListen");	        	
	         	AudioManager audioManager = (AudioManager)m_ctx.getSystemService(Context.AUDIO_SERVICE); 
	         	m_bDevSpeakerOn           = audioManager.isSpeakerphoneOn();
	         	
	     		TelephonyManager teleMgr = (TelephonyManager) m_ctx.getSystemService(Context.TELEPHONY_SERVICE);				//get telephone service 4/15/2016		      
	     	    teleMgr.listen(this, PhoneStateListener.LISTEN_CALL_STATE);							//register PhoneStateLister listen_call_state
	     	    
	     	    //get the phone sate when start listen
	     	    boolean bPause = TelephonyManager.CALL_STATE_IDLE != teleMgr.getCallState();
	     	    if(bPause)
	     	    {
	     	    	m_cur_phone_state = AUDIO_INTERRUPT_PAUSE;
	     	    	Log.d("startlisten","startlisten get current phone state:" + "not idle");
	     	    }
	     	    else
	     	    {
	     	    	m_cur_phone_state = AUDIO_INTERRUPT_RESUME;
	     	    	
	     	    	Log.d("startlisten","startlisten get current phone state:" + " idle");
	     	    }
	        }

		   public void StopListen(){

		   }
 	}
 	
 	    
 	public class HeadsetPlugListenner extends BroadcastReceiver 
 	{  	 
 	   	IntentFilter m_intentFilter = new IntentFilter();
		// for bluetooth headset connection receiver
		IntentFilter bluetoothFilter = new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
  
 	    @Override  
 	    public void onReceive(Context context, Intent intent) 
 	    {
			String action = intent.getAction();
			AudioManager  audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
			if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {

				if(m_bInterrupted)
					return;

				BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
				if (BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
					audioManager.setMode(audioManager.isBluetoothA2dpOn() ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
					audioManager.stopBluetoothSco();
					audioManager.setBluetoothScoOn(false);
					audioManager.setSpeakerphoneOn(true);
					m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
					DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
					m_nBeforeInsertSpkOn = -1;
					Log.d("AudioDeviceListener","Bluetooth device disconnected during using valleyren");
					//蓝牙断开
				} else {
					if(audioManager.isBluetoothA2dpOn()) {
						try {
							audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
							audioManager.setSpeakerphoneOn(false);
							audioManager.startBluetoothSco();
							audioManager.setBluetoothScoOn(true);
							m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
							DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
							Log.d("AudioDeviceListener", "Bluetooth device connected during using valleyren and mode is a2dp");
						} catch (Exception ex) {
							audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
							audioManager.stopBluetoothSco();
							audioManager.setBluetoothScoOn(false);
							audioManager.setSpeakerphoneOn(true);
							//am.setBluetoothA2dpOn(true);
							m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
							DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
							Log.d("AudioDeviceListener", "Bluetooth device Exception:" + ex.toString());
						}
					}else{
						try {
							audioManager.setMode(AudioManager.MODE_NORMAL);
							audioManager.stopBluetoothSco();
							audioManager.setBluetoothScoOn(false);
							audioManager.setSpeakerphoneOn(false);
							audioManager.startBluetoothSco();
							audioManager.setBluetoothScoOn(true);
							m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
							DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
							Log.d("AudioDeviceListener","Bluetooth device connected during using valleyren and mode is not a2dp");
						}catch (Exception ex){
							audioManager.setMode(AudioManager.MODE_NORMAL);
							audioManager.stopBluetoothSco();
							audioManager.setBluetoothScoOn(false);
							audioManager.setSpeakerphoneOn(true);
							//audioManager.setBluetoothA2dpOn(true);
							m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
							DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
							Log.d("AudioDeviceListener","Bluetooth Exception:" + ex.toString());
						}
					}
					m_nBeforeInsertSpkOn = 1;
					//蓝牙连接
				}
			}
			else if ("android.intent.action.HEADSET_PLUG".equals(action)) {

				if (intent.hasExtra("microphone")) {
					m_bHeadsetWithmic = intent.getIntExtra("microphone", 0) == 0 ? false:true;
				}
				if (intent.hasExtra("state")) {
					m_bHeadsetInserted = intent.getIntExtra("state", 0) == 0 ? false:true;		
 	    	 		if(m_bInterrupted) 
 	 	    	  		return;

 	    	  		if(m_bHeadsetInserted && !m_bHeadsetWithmic){
 	    		  		m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
 	    		  		DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
 	    		  		Log.i("Headset耳机连接状况","耳机连接不带麦克风");
 	    				if(audioManager.isSpeakerphoneOn())
 	    				{
 	    		       		audioManager.setSpeakerphoneOn(false);
							m_nBeforeInsertSpkOn = 1;
 	    				}
 	    	  		}else if(m_bHeadsetInserted && m_bHeadsetWithmic){
						m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_WITHMIC;
						DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.i("Headset耳机连接状况","耳机连接带麦克风");
						if(audioManager.isSpeakerphoneOn())
						{
							audioManager.setSpeakerphoneOn(false);
							m_nBeforeInsertSpkOn = 1;
						}
					}
 	    	  		else{
 	    		 		m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
 	    		 		DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.i("Headset耳机连接状况","耳机未连接");
 	    		    	audioManager.setSpeakerphoneOn(true);
 	    				m_nBeforeInsertSpkOn = -1;
 	    	  			} 
						//耳机连接
					}
				}
			}
 	    
 	   public void StartListen(){

		   m_intentFilter.addAction("android.intent.action.HEADSET_PLUG");
		   bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

		   m_ctx.registerReceiver(this, m_intentFilter);
		   m_ctx.registerReceiver(this,bluetoothFilter);

		   //get the current headset plugin state
		   AudioManager audioManager = (AudioManager)m_ctx.getSystemService(Context.AUDIO_SERVICE);
		   BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		   if(audioManager.isWiredHeadsetOn() ||
				   BluetoothProfile.STATE_CONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET))
		   {
			   m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
			   Log.d("HeadsetPlug","startlisten get current headset state:" + "plug-in");
		   }
		   else
		   {
			   m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
			   Log.d("HeadsetPlug","startlisten get current headset state:" + "plug-out");
		   }
	   }

		public void StopListen(){
			if(null != m_intentFilter) {
				m_ctx.unregisterReceiver(this);
				Log.d("HeadsetPlug","StopListen");
			}
		}
 	}  
 	      
	    
 	public class SystemVolumeReceiver extends BroadcastReceiver
 	{ 
 		IntentFilter m_intentFilter = new IntentFilter();
 		@Override
 		public void onReceive(Context context, Intent intent) {
 			//婵″倹鐏夐棅鎶藉櫤閸欐垹鏁撻崣妯哄閸掓瑦娲块弨绠俥ekbar閻ㄥ嫪缍呯純锟� 			if(intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION"))
 			{
 				//m_cur_volume_sc =  GetCurSystemVolume(context);
 				
 				//if(0 != m_host_id)
 				//	OnAudioListengerEvent(event_type_play_volume, m_host_id, m_cur_volume_sc);  
 			}
 		}
 		
 		 public void StartListen(){   
 		//	m_intentFilter.addAction("android.media.VOLUME_CHANGED_ACTION") ;
 		//	m_ctx.registerReceiver(this, m_intentFilter) ;
 			
 			//get current system volume
 		//	m_cur_volume_sc =  GetCurSystemVolume(m_ctx);
 		//	Log.d("startlisten","startlisten get current system volume percent:" + m_cur_volume_sc + "%"); 		
 			
 			//set volume percent to audio engine
 		//	if(0!=m_host_id)
			//	OnAudioListengerEvent(event_type_play_volume, m_host_id, m_cur_volume_sc);  
 			 
 			int volume = GetCurSystemVolume(m_ctx);
 			
 			if(volume < 60)
 			{
 				SetCurSystemVolume(m_ctx, 60);
 			}
 	     }

		public void StopListen() {

		}
		private int GetCurSystemVolume(Context  context)
 		 {
 			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE) ;
			int currVolume = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL) ;// 瑜版挸澧犻惃鍕崯娴ｆ捇鐓堕柌锟�
			int maxVolume  = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
			if(0 == maxVolume || maxVolume < currVolume)
				return 0;

			return (int)(100*currVolume/maxVolume);
 		 }
 		 
 		 private void SetCurSystemVolume(Context  context, int volume)
 		 {
 			AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE) ;
 			int maxVolume  = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);// 瑜版挸澧犻惃鍕崯娴ｆ捇鐓堕柌锟�
			if(0 == maxVolume)
				return;

			int vol = maxVolume*volume/100;
			audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, vol, 0);
 		 }
 	}
 	 
 	public class NetworkStateReceiver extends BroadcastReceiver
 	{ 
 		IntentFilter m_intentFilter = new IntentFilter();
 		@Override
 		public void onReceive(Context context, Intent intent) {
 	        
 			m_cur_net_type = getNetWorkType(context); 
 			DeviceNative.DeviceEvent(event_type_network_state, m_cur_net_type);  		//4G鍙婁互涓婄綉缁�	        		 	        	 	        	 
 	    }

 		private int getNetWorkType(Context context)
 		{ 			
 			ConnectivityManager connectivityManager=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
  	        NetworkInfo  mobNetInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
  	        if(null == mobNetInfo)
  	        {
  	        	Log.d("NetworkInfo","NetworkInfo mobNetInfo is null");
  	        	return 0;
  	        }
  	        
  	        NetworkInfo  wifiNetInfo=connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
  	        
  	        if(null == wifiNetInfo)
  	        {
  	        	Log.d("NetworkInfo","NetworkInfo wifiNetInfo is null");
  	        	return 0;
  	        } 	        	        
  	        
  	        //NetworkInfo activeInfo = connectivityManager.getActiveNetworkInfo();  
  	        
  	        if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) { 	     
  	        	Log.d("AudioDeviceListener","Network : no network ");
  	        	return 0;
  	        }
  	        else 
  	        {
  	        	int nType = 0;
  	        	
  	        	if(wifiNetInfo.isConnected())
  	        	{
  	        		Log.d("AudioDeviceListener","Network name: " + wifiNetInfo.getTypeName());	//鏃犵嚎缃戠粶
  	        		nType = 1;
  	        	}
  	        	else if(mobNetInfo.isConnected())											//mobile network
  	        	{
  	        		int nSubType = mobNetInfo.getSubtype();									//鐢典俊4G LTE 13
  	        		String strSubName = mobNetInfo.getSubtypeName(); 	        		
  	        		//Log.d("AudioDeviceListener","Network name: " + mobNetInfo.getTypeName());
  	        		//Log.d("AudioDeviceListener","Network sub type: " + nSubType + strSubName + " sub type ");
  	        		
  	        		if(getNetworkClass(nSubType)<=3)
  	        		{
  	        			Log.d("AudioDeviceListener","Network sub : " + strSubName + " sub type:" + nSubType);	
  	        			nType = 2;															//绉诲姩缃戠粶 	       			
  	        		}
  	        		else 
  	        		{
  	        			Log.d("AudioDeviceListener","Network sub : " + strSubName + " sub type:" + nSubType);  	        			
  	        			nType = 3;															//4G鍙婁互涓婄綉缁�	        		 	        	
  	        		}
  	        	} 	  
  	        	
  	        	return nType;
  	        } 	         	      
 		}
 		
 		private int getNetworkClass(int networkType) 
 	 	{ 		
 		   	  switch (networkType) 
 		   	  {
 			   	  case TelephonyManager.NETWORK_TYPE_GPRS:
 			   	  case TelephonyManager.NETWORK_TYPE_EDGE:
 			   	  case TelephonyManager.NETWORK_TYPE_CDMA:
 			   	  case TelephonyManager.NETWORK_TYPE_1xRTT:
 			   	  case TelephonyManager.NETWORK_TYPE_IDEN:
 			   		  return 2;//2G
 			   	  case TelephonyManager.NETWORK_TYPE_UMTS:
 			   	  case TelephonyManager.NETWORK_TYPE_EVDO_0:
 			   	  case TelephonyManager.NETWORK_TYPE_EVDO_A:
 			   	  case TelephonyManager.NETWORK_TYPE_HSDPA:
 			   	  case TelephonyManager.NETWORK_TYPE_HSUPA:
 			   	  case TelephonyManager.NETWORK_TYPE_HSPA:
 			   	  case TelephonyManager.NETWORK_TYPE_EVDO_B:
 			   	  case TelephonyManager.NETWORK_TYPE_EHRPD:
 			   	  case TelephonyManager.NETWORK_TYPE_HSPAP:
 			   		  return 3;//3G
 			   	  case TelephonyManager.NETWORK_TYPE_LTE:
 			   		  return 4;//4G
 			   	  default:
 			   		  return 5;//5G in the future		
 		   	  }
 	   	  }
 		
 		 public void StartListen(){   
 			m_intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE") ;
 			m_ctx.registerReceiver(this, m_intentFilter) ;
 			
 			//get current net type 
 			m_cur_net_type = getNetWorkType(m_ctx);
 			Log.d("network","startlisten get current net type :" + m_cur_net_type);
 	     }

		public void StopListen(){
			if(null != m_intentFilter) {
				m_ctx.unregisterReceiver(this);
				Log.d("network","StopListen");
			}
		}
 	}	
 	
 	 public void StartListen(){ 
 		 
 		 if(m_bStartListen)
 			 return;
 		 
 		m_bStartListen = true;
 		 
 		try
 		{ 
 			AudioManager am = (AudioManager) m_ctx.getSystemService(Context.AUDIO_SERVICE); 

			if(am.isWiredHeadsetOn()){
				//am.setSpeakerphoneOn(false);
				//m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
				//DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
				//m_nBeforeInsertSpkOn = 1;
				//m_bHeadsetInserted = true;
				//Log.d("AudioDeviceListener","wiredheadset connected when the app open:");
			}
			else{
				if(am.isBluetoothA2dpOn()){
					try {
						am.setMode(AudioManager.MODE_IN_COMMUNICATION);
						am.setSpeakerphoneOn(false);
						am.startBluetoothSco();
						am.setBluetoothScoOn(true);
						m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
						DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.d("AudioDeviceListener","Bluetooth device connected when the app open");
					}catch (Exception ex){
						am.setMode(AudioManager.MODE_IN_COMMUNICATION);
						am.stopBluetoothSco();
						am.setBluetoothScoOn(false);
						am.setSpeakerphoneOn(true);
						//am.setBluetoothA2dpOn(true);
						m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
						DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.d("AudioDeviceListener","Bluetooth Exception:" + ex.toString());
					}
					m_nBeforeInsertSpkOn = 1;
				}else if(am.isBluetoothScoOn()){
					try {
						am.setMode(AudioManager.MODE_NORMAL);
						am.stopBluetoothSco();
						am.setBluetoothScoOn(false);
						am.setSpeakerphoneOn(false);
						am.startBluetoothSco();
						am.setBluetoothScoOn(true);
						m_cur_headset_plugin_state = AUDIO_HEADERSET_INSERTED_NOMIC;
						DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.d("AudioDeviceListener","Bluetooth device connected when the app open and mode is not a2dp");
					}catch (Exception ex){
						am.setMode(AudioManager.MODE_NORMAL);
						am.stopBluetoothSco();
						am.setBluetoothScoOn(false);
						am.setSpeakerphoneOn(true);
						//audioManager.setBluetoothA2dpOn(true);
						m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
						DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
						Log.d("AudioDeviceListener","Bluetooth Exception:" + ex.toString());
					}
				}else{
					am.setMode(am.isBluetoothA2dpOn() ? AudioManager.MODE_IN_COMMUNICATION : AudioManager.MODE_NORMAL);
					am.stopBluetoothSco();
					am.setBluetoothScoOn(false);
					am.setSpeakerphoneOn(true);
					m_cur_headset_plugin_state = AUDIO_HEADERSET_OUT;
					DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state);
					m_nBeforeInsertSpkOn = -1;
				}
			}


 			if(null != m_HeadsetListenner)
 				m_HeadsetListenner.StartListen();
 		}
 		catch(Exception ex)
 		{
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		}
 		 		
 		try
  		{ 			
 			if(null != m_volumeListenner)
 				m_volumeListenner.StartListen(); 			 				 			 			 		
  		}
 		catch(Exception ex)
 		{
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		}
 		 		 		
 		try
 		{
 			if(null != m_networkListenner)
 				m_networkListenner.StartListen();
 		}
 		catch(Exception ex)
 		{
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		}
 		 		  		
 		try
 		{
 			if(null != m_spkListenner)
 	 		    m_spkListenner.StartListen();
 		}
 		catch(Exception ex)
 		{
 			//Exception:java.lang.NullPointerException: Attempt to read from field 'android.os.MessageQueue android.os.Looper.mQueue' on a null object reference
 			Log.d("AudioDeviceListener","AudioDeviceListener Exception:" + ex.toString());
 		} 
 		
 		DeviceNative.DeviceEvent(event_type_phone_state, m_cur_phone_state);
 		DeviceNative.DeviceEvent(event_type_headset_plugin, m_cur_headset_plugin_state); 
 		DeviceNative.DeviceEvent(event_type_network_state, m_cur_net_type); 
     }

     public void StopListen() {
		 if(!m_bStartListen)
			 return;

		 m_bStartListen = false;

		 try
		 {
			 if(null != m_HeadsetListenner)
				 m_HeadsetListenner.StopListen();

			 if(null != m_volumeListenner)
				 m_volumeListenner.StopListen();

			 if(null != m_networkListenner)
				 m_networkListenner.StopListen();

			 if(null != m_spkListenner)
				 m_spkListenner.StopListen();
		 }
		 catch(Exception ex)
		 {
			 Log.d("AudioDeviceListener","StopListen Exception:" + ex.toString());
		 }
	 }
 	  
     public void SetSpeakerphoneOn(int on)
 	{
 		AudioManager audioManager = (AudioManager)m_ctx.getSystemService(Context.AUDIO_SERVICE);

		if(-1 == on)
		{
			return;
		}

 		audioManager.setSpeakerphoneOn(1 == on);									   //switch speaker or ear piece
	}
     
     public boolean GetSpeakerphoneOn(){
  		AudioManager audioManager = (AudioManager)m_ctx.getSystemService(Context.AUDIO_SERVICE);	
  		return audioManager.isSpeakerphoneOn();	
     } 
}	 
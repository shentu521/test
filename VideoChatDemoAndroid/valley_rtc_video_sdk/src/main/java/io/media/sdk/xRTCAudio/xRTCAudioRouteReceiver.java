package io.media.sdk.xRTCAudio;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;

import io.media.sdk.xRTCLogging;


/**
 * Created by sunhui on 2017/11/24.
 */

public class xRTCAudioRouteReceiver extends BroadcastReceiver {
    private xRTCAudioManager mAudioManager ;
    private final String TAG = "xRTCAudioRouteReceiver" ;

    xRTCAudioRouteReceiver(xRTCAudioManager audioManager)
    {
        mAudioManager = audioManager ;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if(BluetoothProfile.STATE_DISCONNECTED == adapter.getProfileConnectionState(BluetoothProfile.HEADSET)) {
                //Bluetooth headset is now disconnected
                mAudioManager.setPlayoutSpeaker(true);
                xRTCLogging.i( TAG, "bluetooth remove...");

            }
            else
            {
                mAudioManager.setPlayoutSpeaker(false) ;
                xRTCLogging.i( TAG, "bluetooth add...");

            }
        } else if ("android.intent.action.HEADSET_PLUG".equals(action)) {
            if (intent.hasExtra("state")) {
                if (intent.getIntExtra("state", 0) == 0) {
                    //handleHeadsetDisconnected();
                    mAudioManager.setPlayoutSpeaker(true);
                    xRTCLogging.i( TAG, "headset remove...");

                }
                else
                {
                    if (intent.getIntExtra("state", 0) == 1)
                    {
                        mAudioManager.setPlayoutSpeaker(false) ;
                        xRTCLogging.i( TAG, "headset add...");
                    }
                }

            }
        }
    }
}

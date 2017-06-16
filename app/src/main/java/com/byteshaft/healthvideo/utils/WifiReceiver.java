package com.byteshaft.healthvideo.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.byteshaft.healthvideo.radar.WifiDirectActivity;

/**
 * Created by s9iper1 on 6/14/17.
 */

public class WifiReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("WifiReceiver", " Connection");
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            Log.d("WifiReceiver", "Have Wifi Connection");
            WifiManager wifi = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifi.isWifiEnabled()){
                if (WifiDirectActivity.foreground) {
                    WifiDirectActivity.getInstance().startService();
                }
            }
        }


    }
}

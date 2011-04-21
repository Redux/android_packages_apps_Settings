package com.android.settings.stats;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;

public class StatsReceiver extends BroadcastReceiver {
    private static final String TAG = "StatsReceiver";
    
    private static final String PREF_NAME = "STATS";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences settings = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            editor.putBoolean("sent_data", false);
            editor.commit();
        }
        
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            editor.putBoolean("sent_data", false);
            editor.commit();
        }
        
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            boolean sent = settings.getBoolean("sent_data", false);
            
            Bundle extras = intent.getExtras();
            boolean noConnectivity = extras.getBoolean (ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
            
            if (!sent && !noConnectivity) {
                Log.i(TAG, "Connectivity: " + !noConnectivity + ". Starting stats service.");
                ComponentName cn = new ComponentName(context.getPackageName(), StatsService.class.getName());
                context.startService(new Intent().setComponent(cn));
            }
        }
    }
}
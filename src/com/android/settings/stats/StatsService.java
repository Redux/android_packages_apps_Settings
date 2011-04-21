package com.android.settings.stats;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class StatsService extends Service {
    private static final String PREF_NAME = "STATS";
    private static final String TAG = "StatsService";
    
    private static final boolean DBG = true;
    
    private SharedPreferences settings;
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onCreate() {
        settings = getSharedPreferences(PREF_NAME, 0);
        
        if (!disabledAnonymousData() && !alreadySent()) {
            Log.i(TAG, "Sending anonymous data to Redux servers...");
            Thread thread = new Thread() {
                @Override
                public void run() {
                    sendData();
                }
            };
            thread.start();
        } else {
            Log.i(TAG, "User has disabled sending anonymous data, not sending.");
        }
    }
    
    private boolean disabledAnonymousData() {
        return !settings.getBoolean("send_data", true);
    }
    
    private boolean alreadySent() {
        return settings.getBoolean("sent_data", false);
    }
    
    private void sendData() {
        String deviceId = StatsHelper.getUniqueID(getApplicationContext());
        String deviceName = StatsHelper.getDevice();
        String deviceVersion = StatsHelper.getModVersion();
        String deviceCountry = StatsHelper.getCountryCode(getApplicationContext());
        String deviceCarrier = StatsHelper.getCarrier(getApplicationContext());
        String deviceCarrierId = StatsHelper.getCarrierId(getApplicationContext());
        
        Log.d(TAG, "Device ID: " + deviceId);
        Log.d(TAG, "Device name: " + deviceName);
        Log.d(TAG, "ROM version: " + deviceVersion);
        Log.d(TAG, "Country: " + deviceCountry);
        Log.d(TAG, "Carrier: " + deviceCarrier);
        Log.d(TAG, "Carrier ID: " + deviceCarrierId);
        
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://redux.heliohost.org/submit.php");
        
        try {
            List<NameValuePair> kv = new ArrayList<NameValuePair>(5);
            
            kv.add(new BasicNameValuePair("device_id", deviceId));
            kv.add(new BasicNameValuePair("device_name", deviceName));
            kv.add(new BasicNameValuePair("rom_version", deviceVersion));
            kv.add(new BasicNameValuePair("device_country", deviceCountry));
            kv.add(new BasicNameValuePair("device_carrier", deviceCarrier));
            kv.add(new BasicNameValuePair("device_carrier_id", deviceCarrierId));
            
            httppost.setEntity(new UrlEncodedFormEntity(kv));
            httpclient.execute(httppost);
            
            sentData(true);
            Log.i(TAG, "Data sent successfuly!");
        } catch (Exception e) {
            Log.e(TAG, "Error! Couldn't send data: ", e);
            sentData(false);
        }
        Log.i(TAG, "Self-stopping service.");
        stopSelf();
    }
    
    private void sentData(boolean sent) {
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putBoolean("sent_data", sent);
        editor.commit();
    }
}
package com.android.settings.cpu;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.CPUSettings;
import com.android.settings.cpu.CPUHelper;

import java.util.Iterator;
import java.util.List;

public class CPUService extends Service {

    private static final String TAG = "CPUSettings";
	
	private Context mContext;
	private ContentObserver mObserver;
	
	private BroadcastReceiver mScreenOnOffReceiver = null;
	
	/**
	 * True if should lower CPU frequency when screen is turned off
	 */
	private static boolean mScreenstateScaling = true;
	
	/**
	 * True if should apply settings at startup
	 */
	private static boolean mOnBoot = false;
	private static String governor;
	private static String minFreq;
	private static String maxFreq;
	
	private static SharedPreferences prefs;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {
		mContext = getApplicationContext();
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		readSettings();
		registerScreenOnOffListener();
		
		onBoot();
		return;
    }
	
	public static void readSettings () {
		governor = prefs.getString(CPUSettings.GOVERNOR, null);
		minFreq = prefs.getString(CPUSettings.MIN_FREQ, null);
		maxFreq = prefs.getString(CPUSettings.MAX_FREQ, null);
		mOnBoot = prefs.getBoolean(CPUSettings.ON_BOOT, false);
		mScreenstateScaling = prefs.getBoolean(CPUSettings.SCREENSTATE_SCALING, true);
	}
	
    /**
     * Registers an intent to listen for ACTION_SCREEN_ON/ACTION_SCREEN_OFF
     * notifications. This intent is called to know iwhen the screen is turned
     * on/off.
     */
    public void registerScreenOnOffListener() {
        if (mScreenOnOffReceiver == null) {
            mScreenOnOffReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_SCREEN_ON) && mScreenstateScaling && !isSetCpuRunning()) {
						onScreenOn();
                    } else if (action.equals(Intent.ACTION_SCREEN_OFF) && mScreenstateScaling && !isSetCpuRunning()) {
						onScreenOff();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_SCREEN_ON);
            iFilter.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(mScreenOnOffReceiver, iFilter);
        }
    }
	
	private void onBoot () {
		if (mOnBoot) {
			Log.i(TAG, "Applying OC settings at boot if any available.");
			if (governor != null) CPUHelper.setGovernor(governor);
			if (minFreq != null) CPUHelper.setMinFreq(minFreq);
			if (maxFreq != null) CPUHelper.setMaxFreq(maxFreq);
		} else {
			governor = CPUHelper.defaultGov();
			minFreq = CPUHelper.defaultMinFreq();
			maxFreq = CPUHelper.defaultMaxFreq();
		}
	}
	
	private void onScreenOn() {
		CPUHelper.setMinFreq (minFreq);
		CPUHelper.setMaxFreq (maxFreq);
	}
	
	private void onScreenOff() {
		CPUHelper.setMinFreq (minFreq);
		CPUHelper.setMaxFreq (minFreq);
	}
	
	private boolean isSetCpuRunning() {
		ActivityManager manager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> processList = manager.getRunningAppProcesses();
	
		for (Iterator iterator = processList.iterator(); iterator.hasNext();) {
			RunningAppProcessInfo info = (RunningAppProcessInfo) iterator.next();
			if (info.processName.equals("com.mhuang.overclocking")) {
				return true;
			}
		}
		return false;
	}
}

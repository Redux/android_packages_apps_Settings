/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.os.Handler;
import android.util.Log;

import com.android.settings.cpu.CPUHelper;
import com.android.settings.cpu.CPUService;

public class CPUSettings extends PreferenceActivity implements
	Preference.OnPreferenceChangeListener {
		
    private static final String TAG = "CPUSettings";
        
    private static final String INTENT_POWERSAVE_ON = "com.android.intent.POWERSAVE_ON";
    private static final String INTENT_POWERSAVE_OFF = "com.android.intent.POWERSAVE_OFF";
	
    public static final String POWERSAVE = "powersave";
	public static final String SCREENSTATE_SCALING = "screenstate_scaling";
	public static final String GOVERNOR = "cpu_governor";
	public static final String MIN_FREQ = "cpu_min_freq";
	public static final String MAX_FREQ = "cpu_max_freq";
	public static final String ON_BOOT = "set_on_boot";
	
	private String GOV_FMT;
	private String MIN_FMT;
	private String MAX_FMT;

    private CheckBoxPreference mPowersave;
	private CheckBoxPreference mScreenstateScaling;
	private ListPreference mGovernor;
	private ListPreference mMinFreq;
	private ListPreference mMaxFreq;
	private CheckBoxPreference mOnBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();
		
		GOV_FMT = getString(R.string.cpu_governors_list);
		MIN_FMT = getString(R.string.cpu_min_freq);
		MAX_FMT = getString(R.string.cpu_max_freq);
		
		String[] Governors = CPUHelper.getAvailableGovernors();
		String[] FreqValues = CPUHelper.getAvailableFreqs();
		String[] Freqs;
		String temp;
		
		Freqs = new String[FreqValues.length];
		for (int i = 0; i < Freqs.length; i++)
			Freqs[i] = MHerzed(FreqValues[i]);

        addPreferencesFromResource(R.xml.cpu_settings);
		
        mPowersave = (CheckBoxPreference)findPreference(POWERSAVE);
		mScreenstateScaling = (CheckBoxPreference)findPreference(SCREENSTATE_SCALING);
        mOnBoot = (CheckBoxPreference)findPreference(ON_BOOT);
		
		temp = CPUHelper.getCurrentGovernor();
		mGovernor = (ListPreference)findPreference(GOVERNOR);
		mGovernor.setEntryValues(Governors);
		mGovernor.setEntries(Governors);
		mGovernor.setValue(temp);
		mGovernor.setSummary(String.format(GOV_FMT, temp));
		mGovernor.setOnPreferenceChangeListener(this);
		
		temp = CPUHelper.getCurrentMinFreq();
		mMinFreq = (ListPreference)findPreference(MIN_FREQ);
		mMinFreq.setEntryValues(FreqValues);
		mMinFreq.setEntries(Freqs);
		mMinFreq.setValue(temp);
		mMinFreq.setSummary(String.format(MIN_FMT, MHerzed(temp)));
		mMinFreq.setOnPreferenceChangeListener(this);
		
		mMaxFreq = (ListPreference)findPreference(MAX_FREQ);
		mMaxFreq.setEntryValues(FreqValues);
		mMaxFreq.setEntries(Freqs);
        if (!mOnBoot.isChecked()) {
            mMaxFreq.setValue(CPUHelper.defaultMaxFreq());
        }
        mMaxFreq.setSummary(String.format(MAX_FMT, MHerzed(mMaxFreq.getValue())));
		mMaxFreq.setOnPreferenceChangeListener(this);
        
        powersaveChanged (!mPowersave.isChecked());
    }

    @Override
    protected void onResume() {
        super.onResume();
		
		/**
		 * Wait 0.5s for frequencies to settle down. This is needed
		 * in case onResume() is called when turning the screen on,
		 * so we need to make sure the screenstate scaling thingie
		 * finishes its job.
		 */
		
		final Handler hndl = new Handler();
		hndl.postDelayed (new Runnable() {
			public void run () {
				String temp;
						  
                if (!mPowersave.isChecked()) {
                    temp = CPUHelper.getCurrentMaxFreq();
                    mMaxFreq.setValue(temp);
                    mMaxFreq.setSummary(String.format(MAX_FMT, MHerzed(temp)));
                    temp = CPUHelper.getCurrentGovernor();
                    mGovernor.setValue(temp);
                    mGovernor.setSummary(String.format(GOV_FMT, temp));
                } else {
                    mMaxFreq.setSummary(String.format(MAX_FMT, MHerzed(mMaxFreq.getValue())));
                    mGovernor.setSummary(String.format(GOV_FMT, mGovernor.getValue()));
                }
	
				temp = CPUHelper.getCurrentMinFreq();
				mMinFreq.setValue(temp);
				mMinFreq.setSummary(String.format(MIN_FMT, MHerzed(temp)));
			}
		
		}, 500);
    }
	
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPowersave) {
            Context context = getApplicationContext();
            if (mPowersave.isChecked()) {
                Intent intent = new Intent (INTENT_POWERSAVE_ON, null);
                context.sendBroadcast(intent, null);
                
                powersaveChanged(false);
            } else {
                Intent intent = new Intent (INTENT_POWERSAVE_OFF, null);
                context.sendBroadcast(intent, null);
                
                powersaveChanged(true);
            }
        }
		/**
		 * Wait 0.5s until applying the new settings to CPUService
		 */
		final Handler hndl = new Handler();
		hndl.postDelayed (new Runnable() {
			public void run () {
				CPUService.updateSettings();
			}
		}, 500);
        return true;
    }

	public boolean onPreferenceChange(Preference preference, Object newValue) {		
		if (newValue != null) {
			if (preference == mGovernor)
				if (CPUHelper.setGovernor((String) newValue)) {
					mGovernor.setSummary(String.format(GOV_FMT, (String) newValue));
				}
				else
					Log.i(TAG, "CPUHelper.setGovernor failed");
			else if (preference == mMinFreq)
				if (CPUHelper.setMinFreq((String) newValue)) {
					mMinFreq.setSummary(String.format(MIN_FMT, MHerzed((String) newValue)));
				}
				else
					Log.i(TAG, "CPUHelper.setMinFreq failed");
 			else if (preference == mMaxFreq)
				if (CPUHelper.setMaxFreq((String) newValue)) {
					mMaxFreq.setSummary(String.format(MAX_FMT, MHerzed((String) newValue)));
				}
				else
					Log.i(TAG, "CPUHelper.setMaxFreq failed");
			final Handler hndl = new Handler();
			hndl.postDelayed (new Runnable() {
				public void run () {
					CPUService.updateSettings();
				}
			}, 500);
			return true;
		} else {
			return false;
		}
	}

	private String MHerzed(String str) {
		String temp;
		
		temp = str.substring(0, str.length() - 3);
		
		return (temp + " MHz");
	}
        
    private void powersaveChanged (boolean enabled) {
        mScreenstateScaling.setEnabled(enabled);
        mGovernor.setEnabled(enabled);
        mMinFreq.setEnabled(enabled);
        mMaxFreq.setEnabled(enabled);
    }
}

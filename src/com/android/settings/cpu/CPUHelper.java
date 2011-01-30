package com.android.settings.cpu;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class CPUHelper {
	private static final String TAG = "CPUSettings";

    public static final String GOVERNORS_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors";
    public static final String GOVERNOR = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

    public static final String FREQ_LIST_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_frequencies";
    public static final String FREQ_MAX_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_max_freq";
    public static final String FREQ_MIN_FILE = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq";
	
	
	/**
	 * Define preferred CPU governors. The order defines which should be preferred when
	 * setting the default governor. If none of these is available, then the first one
	 * returned by `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors`
	 * will be used. This should not happen though, as this list contains most of the
	 * current governors out there.
	 */
	private static final String[] prefGovernors = {
		"smartass",
		"interactive",
		"ondemand",
		"conservative",
		"performance",
		"powersave"
	};

    public static String readOneLine(String fname) {
        BufferedReader br;
        String line = null;

        try {
            br = new BufferedReader (new FileReader(fname), 512);
            try {
                line = br.readLine();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "IO Exception when reading /sys/ file", e);
        }
        return line;
    }

    public static boolean writeOneLine(String fname, String value) {
        try {
            FileWriter fw = new FileWriter(fname);
            try {
                fw.write(value);
            } finally {
                fw.close();
            }
        } catch(IOException e) {
            String Error = "Error writing to " + fname + ". Exception: ";
            Log.e(TAG, Error, e);
            return false;
        }
        return true;
    }
	
	/**
	 * Get lowest/highest/available frequencies supported by the kernel.
	 */
	public static String minFreq () {
		String[] freqs = readOneLine(FREQ_LIST_FILE).split(" ");
		return freqs[0];
	}
	
	public static String maxFreq () {
		String[] freqs = readOneLine(FREQ_LIST_FILE).split(" ");
		return freqs[freqs.length - 1];
	}
	
	public static String[] getAvailableFreqs() {
		return readOneLine(FREQ_LIST_FILE).split(" ");
	}
	
	/**
	 * Get a list of supported governors.
	 */
	public static String[] getAvailableGovernors() {
		return readOneLine(GOVERNORS_LIST_FILE).split(" ");
	}
	
	/**
	 * Get currently applied settings: Governor, minimum frequency, maximum frequency.
	 */
	public static String getCurrentGovernor () {
		return readOneLine(GOVERNOR);
	}
	
	public static String getCurrentMinFreq () {
		return readOneLine(FREQ_MIN_FILE);
	}
	
	public static String getCurrentMaxFreq () {
		return readOneLine(FREQ_MAX_FILE);
	}
	
	/**
	 * Apply settings, only if they're valid settings for our device/kernel.
	 * Otherwise, revert to fallback defaults.
	 */
	public static boolean setMinFreq (String value) {
		if (readOneLine(FREQ_LIST_FILE).indexOf(value) == -1)
			value = defaultMinFreq();
		
		if (writeOneLine(FREQ_MIN_FILE, value)) {
			Log.i(TAG, "Minimum frequency set to " + value);
			return true;
		}
		return false;
	}
	
	public static boolean setMaxFreq (String value) {
		if (readOneLine(FREQ_LIST_FILE).indexOf(value) == -1)
			value = defaultMaxFreq();
		
		if (writeOneLine(FREQ_MAX_FILE, value)) {
			Log.i(TAG, "Maximum frequency set to " + value);
			return true;
		}
		return false;
	}
	
	public static boolean setGovernor (String value) {
		if (readOneLine(GOVERNORS_LIST_FILE).indexOf(value) == -1)
			value = defaultGov();
		
		if (writeOneLine(GOVERNOR, value)) {
			Log.i(TAG, "CPU scaling governor set to " + value);
			return true;
		}
		return false;
	}
	
	/**
	 * Get fallback defaults.
	 */
	public static String defaultGov() {
		String governors = readOneLine(GOVERNORS_LIST_FILE);
		
		for (int i = 0; i < prefGovernors.length; i++) {
			if (governors.indexOf(prefGovernors[i]) != -1)
				return prefGovernors[i];
		}
		
		String[] avGovernors = governors.split(" ");
		return avGovernors[0];
	}
	
	public static String defaultMaxFreq() {
		/**
		 * Hard coded value for now...
		 */
		return "998400";
	}
	
	public static String defaultMinFreq() {
		return minFreq();
	}
}

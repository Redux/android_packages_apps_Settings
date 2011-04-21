package com.android.settings.stats;

import android.content.Context;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;

public class StatsHelper {
    public static String getUniqueID(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        
        String device_id = encode(tm.getDeviceId());
        // add compatibility with tablets and such
        if (device_id == null) {
            String wifiInterface = SystemProperties.get("wifi.interface");
            try {
                String wifiMac = new String(NetworkInterface.getByName(wifiInterface).getHardwareAddress());
                device_id = encode(wifiMac);
            } catch (Exception e) {
                device_id = null;
            }
        }
        
        return device_id;
    }
    
    public static String getCarrier(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrier = tm.getNetworkOperatorName();
        
        if (carrier.equals("")) {
            carrier = "Unknown";
        }
        
        return carrier;
    }
    
    public static String getCarrierId(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String carrierId = tm.getNetworkOperator();
        
        if (carrierId.equals("")) {
            carrierId = "0";
        }
        
        return carrierId;
    }
    
    public static String getCountryCode(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String countryCode = tm.getNetworkCountryIso();
        
        if (countryCode.equals("")) {
            countryCode = "N/A";
        }
        
        return countryCode;
    }
    
    public static String getDevice() {
        return SystemProperties.get("ro.product.device");
    }
    
    public static String getModVersion() {
        return SystemProperties.get("ro.modversion");
    }
    
    public static String encode(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }
}
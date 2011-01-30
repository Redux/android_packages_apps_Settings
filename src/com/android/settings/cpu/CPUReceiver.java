package com.android.settings.cpu;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class CPUReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            ComponentName cmp = new ComponentName(context.getPackageName(),
                    com.android.settings.cpu.CPUService.class.getName());
            context.startService(new Intent().setComponent(cmp));
        }
    }
}

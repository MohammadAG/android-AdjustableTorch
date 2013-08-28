package com.mohammadag.adjustabletorch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
	
	private void resetFlashValue(Context context) {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.SETTINGS_FLASH_KEY, 0);
		editor.commit();
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.ACTION_SHUTDOWN".equals(intent.getAction())) {
        	resetFlashValue(context);
        }
        
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        	resetFlashValue(context);
        }
	}
}

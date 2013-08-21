package com.mohammadag.adjustabletorch;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ResultActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme_Holo_Dialog);
		MainActivity.turnOffFlash();
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		
		SharedPreferences settings = getSharedPreferences(MainActivity.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.SETTINGS_FLASH_KEY, 0);
		editor.commit();
		finish();
	}
}

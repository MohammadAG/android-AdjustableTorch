package com.mohammadag.adjustabletorch;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class ResultsService extends IntentService {
	public ResultsService() {
		super("ResultsService");
	}

	public ResultsService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int action = Integer.parseInt(intent.getAction());
		
		int newValue = 0;
		if (action == -1) {
			newValue = Utils.changeFlashValue(getApplicationContext(), false);
		} else if (action == 0) {
			Utils.turnOffFlash(getApplicationContext());
			newValue = 0;
		} else if (action == 1) {
			newValue = Utils.changeFlashValue(getApplicationContext(), true);
		} else if(action == 256) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			int currentStatus = prefs.getInt(Constants.SETTINGS_FLASH_KEY, 0);
			if(currentStatus==0) {
				//Turn on
				boolean invertValues = prefs.getBoolean(Constants.SETTINGS_INVERT_VALUES, false);
				int maxValue = prefs.getInt(Constants.SETTINGS_MAX_VALUE, 16);
				int progress = prefs.getInt(Constants.SETTINGS_WIDGET_KEY, 4);
				newValue = progress;
				
				if(progress!=0 && invertValues) {
					//Invert nonzero values if required
					newValue = Math.max(1, maxValue+1-progress);
				}
				
				Utils.updateTorchValue(newValue, null);
				prefs.edit().putInt(Constants.SETTINGS_FLASH_KEY, progress).commit();
				newValue=progress;  //for the broadcast
			} else {
				//Turn off
				Utils.turnOffFlash(getApplicationContext());
				newValue = 0;
			}
		}

		Intent broadcastIntent = new Intent(Constants.FLASH_VALUE_UPDATED_BROADCAST_NAME);
		broadcastIntent.putExtra(Constants.KEY_NEW_VALUE, newValue);
		sendBroadcast(broadcastIntent);
		
		Utils.showOngoingNotification(getApplicationContext(), newValue > 0);
	}
}

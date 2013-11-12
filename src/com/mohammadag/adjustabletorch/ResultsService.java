package com.mohammadag.adjustabletorch;

import android.app.IntentService;
import android.content.Intent;

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
		
		int newValue = -1;
		if (action == -1) {
			newValue = Utils.changeFlashValue(getApplicationContext(), false);
		} else if (action == 0) {
			Utils.turnOffFlash(getApplicationContext());
			newValue = 0;
		} else if (action == 1) {
			newValue = Utils.changeFlashValue(getApplicationContext(), true);
		}

		Intent broadcastIntent = new Intent(Constants.FLASH_VALUE_UPDATED_BROADCAST_NAME);
		broadcastIntent.putExtra(Constants.KEY_NEW_VALUE, newValue);
		sendBroadcast(broadcastIntent);
		
		Utils.showOngoingNotification(getApplicationContext(), newValue > 0);
	}
}

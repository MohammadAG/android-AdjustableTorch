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
		
		if (action == -1) {
			MainActivity.changeFlashValue(getApplicationContext(), false);
		} else if (action == 0) {
			MainActivity.turnOffFlash(getApplicationContext());
			
		} else if (action == 1) {
			MainActivity.changeFlashValue(getApplicationContext(), true);
		}
		
		Intent broadcastIntent = new Intent(MainActivity.FLASH_VALUE_UPDATED_BROADCAST_NAME);
		sendBroadcast(broadcastIntent);
	}
}

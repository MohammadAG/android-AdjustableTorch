package com.mohammadag.adjustabletorch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

public class StaticTorchWidgetProvider extends AppWidgetProvider {
	public static final String WIDGET_TAG = "TorchWidget";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
		//Update the icon on the static widget
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		if(appWidgetManager!=null) {
			int newValue = intent.getIntExtra(Constants.KEY_NEW_VALUE, 0);
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.static_widget_layout);
			if(newValue==0) {
				views.setImageViewResource(R.id.icon_button, R.drawable.ic_flash_off);
			} else {
				views.setImageViewResource(R.id.icon_button, R.drawable.ic_flash_on);
			}
			int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, StaticTorchWidgetProvider.class));
			Log.i(WIDGET_TAG, "Hail! "+ids);
			for(int i=0; i<ids.length; i++)
				appWidgetManager.updateAppWidget(ids[i], views);
		}
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		Log.i(WIDGET_TAG, "onUpdateStatic");

		final int N = appWidgetIds.length;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		final int currentStatus = prefs.getInt(Constants.SETTINGS_FLASH_KEY, 0);
		
		for (int i=0; i<N; i++) {
			int appWidgetId = appWidgetIds[i];

			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.static_widget_layout);

			if(currentStatus==0) {
				views.setImageViewResource(R.id.icon_button, R.drawable.ic_flash_off);
			} else {
				views.setImageViewResource(R.id.icon_button, R.drawable.ic_flash_on);
			}

			Intent openAppIntent = new Intent(context, MainActivity.class);
			openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			openAppIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent openIntent =
					PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			views.setOnClickPendingIntent(R.id.icon_button, getPendingIntent(context, 256, appWidgetId));

			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	PendingIntent getPendingIntent(Context context, int action, int appWidgetId) {
		Intent intent = new Intent(context, ResultsService.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		intent.setAction(String.valueOf(action));
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
}

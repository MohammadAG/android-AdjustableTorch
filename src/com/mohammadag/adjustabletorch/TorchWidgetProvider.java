package com.mohammadag.adjustabletorch;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class TorchWidgetProvider extends AppWidgetProvider {
	public static final String WIDGET_TAG = "TorchWidget";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		Log.i(WIDGET_TAG, "onUpdate");

		final int N = appWidgetIds.length;

		for (int i=0; i<N; i++) {
			int appWidgetId = appWidgetIds[i];

			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.small_widget_layout);

			Intent openAppIntent = new Intent(context, MainActivity.class);
			openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			openAppIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent openIntent =
					PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			views.setOnClickPendingIntent(R.id.button_increase, getPendingIntent(context, 1, appWidgetId));
			views.setOnClickPendingIntent(R.id.button_decrease, getPendingIntent(context, -1, appWidgetId));
			views.setOnClickPendingIntent(R.id.icon_button, openIntent);

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

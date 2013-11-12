package com.mohammadag.adjustabletorch;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class Utils {
	private static String FLASH_FILE = null;

	public static String getSysFsFile() {
		if (FLASH_FILE != null) return FLASH_FILE;

		for (String filePath : Constants.listOfFlashFiles) {
			File flashFile = new File(filePath);
			if (flashFile.exists()) {
				FLASH_FILE = filePath;
			}
		}

		return FLASH_FILE;
	}

	public static void turnOffFlash(Context context) {
		if (Utils.getSysFsFile() == null)
			return;

		try {
			updateTorchValue(0, RootTools.getShell(true));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		settings.edit().putInt(Constants.SETTINGS_FLASH_KEY, 0).commit();
	}

	static boolean updateTorchValue(int value, Shell shell) {
		if (value > 30)
			return false;

		String commandString = "echo " + String.valueOf(value) + " > " + getSysFsFile();
		CommandCapture command = new CommandCapture(0, commandString);
		if (shell != null) {
			try {
				shell.add(command).waitForFinish();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
			return true;
		} else {
			try {
				updateTorchValue(value, RootTools.getShell(true));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return false;
	}

	public static int changeFlashValue(Context context, boolean increment) {
		if (Utils.getSysFsFile() == null)
			return -1;

		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			int newValue = prefs.getInt(Constants.SETTINGS_FLASH_KEY, 0);
			boolean invertValues = prefs.getBoolean(Constants.SETTINGS_INVERT_VALUES, false);

			if ((newValue >= 16 && increment && !invertValues) || (newValue >= 15 && invertValues && increment))
				return invertValues ? 1 : 16;

			if (increment) {
				newValue += 1;
			} else {
				newValue -= 1;
			}

			if (newValue <= 0) {
				Utils.turnOffFlash(context);
				return 0;
			}

			if (newValue != 0 && invertValues) {
				/* Some devices like the Galaxy S3 have inverted values for some reason
				 * For example, 15 == 1, 14 == 2
				 */
				if (newValue == 16) {
					newValue = 1;
				} else {
					newValue = 16 - newValue;
				}
			}
			Utils.updateTorchValue(newValue, RootTools.getShell(true));
			prefs.edit().putInt(Constants.SETTINGS_FLASH_KEY, newValue).commit();
			return newValue;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	@SuppressLint({ "InlinedApi", "NewApi" })
	public static void showOngoingNotification(Context context, boolean show) {
		NotificationManager mNotificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

		if (show) {
			NotificationCompat.Builder builder =
					new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(context.getString(R.string.notification_text))
			.setOngoing(true)
			.setTicker(context.getString(R.string.ticker_text))
			.addAction(R.drawable.ic_stat_minus, "", getPendingIntent(context, -1))
			.addAction(R.drawable.ic_stat_plus, "", getPendingIntent(context, 1));

			if (Build.VERSION.SDK_INT >= 16) {
				builder.setPriority(Notification.PRIORITY_MAX);
			}

			Intent notifyIntent = new Intent(context, ResultsService.class);
			notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

			builder.setContentIntent(getPendingIntent(context, 0));
			Notification notification = builder.build();
			if (Build.VERSION.SDK_INT >= 16) {
				notification.priority = Notification.PRIORITY_MAX;
			}
			mNotificationManager.notify(Constants.mNotificationId, notification);
		} else {
			mNotificationManager.cancelAll();
		}
	}

	public static PendingIntent getPendingIntent(Context context, int state) {
		// 0 = off, 1 == increase, -1 == decrease
		Intent notifyIntent = new Intent(context, ResultsService.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		notifyIntent.setAction(String.valueOf(state));
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, notifyIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		return pendingIntent;
	}
}

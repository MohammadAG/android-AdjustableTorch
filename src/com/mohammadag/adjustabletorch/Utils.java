package com.mohammadag.adjustabletorch;

import java.io.File;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

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

	public static void changeFlashValue(Context context, boolean increment) {
		if (Utils.getSysFsFile() == null)
			return;

		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			int newValue = prefs.getInt(Constants.SETTINGS_FLASH_KEY, 0);
			if (increment)
				newValue += 1;
			else
				newValue -= 1;

			if (newValue <= 0) {
				Utils.turnOffFlash(context);
				return;
			}

			if (newValue != 0 && prefs.getBoolean(Constants.SETTINGS_INVERT_VALUES, false)) {
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

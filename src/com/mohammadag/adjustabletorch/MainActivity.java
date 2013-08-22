package com.mohammadag.adjustabletorch;

import java.io.File;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;
import com.stericson.RootTools.execution.Shell;

public class MainActivity extends Activity {
	
	public static final String SETTINGS_FLASH_KEY = "flash_value";
	public static final String PREFS_NAME = "AdjustableTorch";
	
	private static final int mNotificationId = 1;
	public static final String FLASH_VALUE_UPDATED_BROADCAST_NAME = "com.mohammadag.adjustabletorch.FLASH_VALUE_UPDATED";
	
	private Shell mShell = null;
	private SeekBar mBrightnessSlider = null;
	private SharedPreferences mPreferences;
	
	private static String FLASH_FILE = null;
	
    private static String[] listOfFlashFiles = {
            "/sys/class/camera/flash/rear_flash",
            "/sys/class/camera/rear/rear_flash",
    };
	
	private enum Errors {
		NO_SYSFS_FILE,
		NO_ROOT,
		NO_ROOT_ACCESS,
		NO_BUSYBOX, // Just in case we need BusyBox for some reason later on.
		NO_BRAIN
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getViews();
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		toggleAds(mPreferences.getBoolean("enable_ads", true));
		
		IntentFilter iF = new IntentFilter(FLASH_VALUE_UPDATED_BROADCAST_NAME);
		registerReceiver(mFlashValueUpdatedReceiver, iF);
		
		if (mBrightnessSlider != null) {
			mBrightnessSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					updateTorchValue(progress);
					
					SharedPreferences.Editor editor = mPreferences.edit();
					editor.putInt(SETTINGS_FLASH_KEY, progress);
					editor.commit();
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) { }
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) { }
			});
			
			SharedPreferences prefs = getSharedPreferences(PREFS_NAME, 0);
			mBrightnessSlider.setProgress(prefs.getInt(SETTINGS_FLASH_KEY, 0));
		}
		
		if (getSysFsFile()) {
			checkForRoot();
		} else {
			complainAbout(Errors.NO_SYSFS_FILE);
		}
		
		updateTorchValue(mBrightnessSlider.getProgress());
	}
	
	BroadcastReceiver mFlashValueUpdatedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
			mBrightnessSlider.setProgress(prefs.getInt(SETTINGS_FLASH_KEY, 0));
		}
	};
	
	public static void turnOffFlash(Context context) {
		if (FLASH_FILE.isEmpty())
			getSysFsFile();
		try {
			updateTorchValue(0, RootTools.getShell(true));
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		NotificationManager mNotificationManager =
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancelAll();
		
		SharedPreferences settings = context.getSharedPreferences(MainActivity.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(MainActivity.SETTINGS_FLASH_KEY, 0);
		editor.commit();
	}
	
	public static void changeFlashValue(Context context, boolean increment) {
		if (FLASH_FILE == null || FLASH_FILE.isEmpty())
			getSysFsFile();
		
		try {
			SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
			int newValue = prefs.getInt(SETTINGS_FLASH_KEY, 0);
			if (increment)
				newValue += 1;
			else
				newValue -= 1;
			
			if (newValue <= 0) {
				turnOffFlash(context);
				return;
			}
			updateTorchValue(newValue, RootTools.getShell(true));
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(SETTINGS_FLASH_KEY, newValue);
			editor.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void getViews() {
		mBrightnessSlider = (SeekBar) findViewById(R.id.flashlightBrightnessSlider);
	}

	private void createDialog(int titleId, int messageId, int positiveTextId, DialogInterface.OnClickListener positiveAction,
			int negativeTextId, DialogInterface.OnClickListener negativeAction) {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this)
		.setTitle(titleId)
		.setMessage(messageId);
		
		if (positiveTextId != 0 && positiveAction != null)
			alertDialog.setPositiveButton(positiveTextId, positiveAction);
			
		if (negativeTextId != 0 && negativeAction != null)
			alertDialog.setNegativeButton(negativeTextId, negativeAction);

		alertDialog.show();
	}

	private void complainAbout(Errors error) {
		OnClickListener quitListener = new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				finish();
			}
		};
		
		switch (error) {
		case NO_SYSFS_FILE:
			createDialog(R.string.error, R.string.no_sysfs_file, 0, null, R.string.uninstall_and_quit, new OnClickListener() {		
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					
					Uri packageUri = Uri.parse("package:com.mohammadag.adjustabletorch");
					Intent uninstallIntent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
					startActivity(uninstallIntent);
		            
					finish();
				}
			});
			break;
		case NO_ROOT:
			createDialog(R.string.error, R.string.device_not_rooted, 0, null, R.string.quit, quitListener);
			break;
		case NO_ROOT_ACCESS:
			createDialog(R.string.error, R.string.root_access_not_given, 0, null, R.string.quit, quitListener);
			break;
		case NO_BUSYBOX:
			break;
		case NO_BRAIN:
			// Not yet implemented, need real life example first.
			break;
		default:
			break;
		}
	}

	private void checkForRoot() {
		if (!RootTools.isRootAvailable()) {
			complainAbout(Errors.NO_ROOT);
		} else {
			if (!RootTools.isAccessGiven()) {
				complainAbout(Errors.NO_ROOT_ACCESS);
			} else {
				openShell();
			}
		}
	}

	private static boolean getSysFsFile() {
		for (String filePath: listOfFlashFiles) {
			File flashFile = new File(filePath);
			if (flashFile.exists()) {
				FLASH_FILE = filePath;
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		MenuItem item = menu.findItem(R.id.enable_ads);
		item.setChecked(mPreferences.getBoolean("enable_ads", true));
		
		return true;
	}
	
	private boolean updateTorchValue(int value) {
		showOngoingNotification((value > 0));
		
		return updateTorchValue(value, mShell);
	}
	
	private static boolean updateTorchValue(int value, Shell shell) {
		if (value > 30)
			return false;
		
		String commandString = "echo " + String.valueOf(value) + " > " + FLASH_FILE;
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
	
	private void openShell() {
		try {
			mShell = RootTools.getShell(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressLint("InlinedApi")
	private void showOngoingNotification(boolean show) {
		NotificationManager mNotificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		if (show) {
			NotificationCompat.Builder builder =
			        new NotificationCompat.Builder(this)
			        .setSmallIcon(R.drawable.ic_launcher)
			        .setContentTitle(getString(R.string.app_name))
			        .setContentText(getString(R.string.notification_text))
			        .setOngoing(true)
			        .setTicker(getString(R.string.ticker_text))
			        .addAction(R.drawable.ic_stat_minus, "", getPendingIntent(-1))
			        .addAction(R.drawable.ic_stat_plus, "", getPendingIntent(1));
			
			if (Build.VERSION.SDK_INT >= 16) {
				 builder.setPriority(Notification.PRIORITY_MAX);
			}
			
			Intent notifyIntent = new Intent(this, ResultsService.class);
			notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

			builder.setContentIntent(getPendingIntent(0));
			Notification notification = builder.build();
			if (Build.VERSION.SDK_INT >= 16) {
				notification.priority = Notification.PRIORITY_MAX;
			}
			mNotificationManager.notify(mNotificationId, notification);
		} else {
			mNotificationManager.cancelAll();
		}
	}
	
	private PendingIntent getPendingIntent(int state) {
		// 0 = off, 1 == increase, -1 == decrease
		Intent notifyIntent = new Intent(this, ResultsService.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		notifyIntent.setAction(String.valueOf(state));
		PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		return pendingIntent;
	}
	
	@Override
	protected void onDestroy() {
		unregisterReceiver(mFlashValueUpdatedReceiver);
		super.onDestroy();
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_about:
                showAbout();
                return true;
            case R.id.action_donate:
                Intent intent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=DRYFYGTC7Z8V2"));
                startActivity(intent);
            	return true;
            case R.id.enable_ads:
            	toggleAds(!item.isChecked());
            	mPreferences.edit().putBoolean("enable_ads", !item.isChecked()).commit();
            	item.setChecked(!item.isChecked());
            	return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(SETTINGS_FLASH_KEY, mBrightnessSlider.getProgress());
		editor.commit();
	}
	
	public void showAbout() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this)
		.setTitle(R.string.about_dialog_title)
		.setMessage(R.string.about_text);
		
		alertDialog.show();
    }
	
    private void toggleAds(boolean enable) {
		if (enable) {
			showAd();
		} else {
			hideAd();
		}
    }
	 
    private void showAd() {
		final AdView adLayout = (AdView) findViewById(R.id.adView);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adLayout.setEnabled(true);
				adLayout.setVisibility(View.VISIBLE);
				adLayout.loadAd(new AdRequest());
			}
		});
    }
    
    private void hideAd() {
		final AdView adLayout = (AdView) findViewById(R.id.adView);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adLayout.setEnabled(false);
				adLayout.setVisibility(View.GONE);
			}
		});
    }
}
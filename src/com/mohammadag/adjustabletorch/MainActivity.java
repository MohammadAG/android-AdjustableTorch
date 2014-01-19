package com.mohammadag.adjustabletorch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

import com.google.ads.AdRequest;
import com.google.ads.AdView;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Shell;

public class MainActivity extends Activity {
	private SeekBar mBrightnessSlider = null;
	private Button mSetWidgetValueButton = null;
	private SharedPreferences mPreferences = null;
	private int mMaxValue = 16;
	private boolean mInvertValues = false;

	private Shell mShell = null;

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
		toggleAds(mPreferences.getBoolean(Constants.SETTINGS_ENABLE_ADS, true));
		mInvertValues = mPreferences.getBoolean(Constants.SETTINGS_INVERT_VALUES, false);
		mMaxValue = mPreferences.getInt(Constants.SETTINGS_MAX_VALUE, 16);

		registerReceiver(mFlashValueUpdatedReceiver,
				new IntentFilter(Constants.FLASH_VALUE_UPDATED_BROADCAST_NAME));

		if (mBrightnessSlider != null) {
			mBrightnessSlider.setMax(mMaxValue);
			mBrightnessSlider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if(!fromUser)  //Do not change torch value unless a human changed the progress bar
						return;
					
					int newValue = progress;

					if (progress != 0 && mInvertValues) {
						//Invert nonzero values if required
						newValue = Math.max(1, mMaxValue+1-progress);
					}

					updateTorchValue(newValue);
					mPreferences.edit().putInt(Constants.SETTINGS_FLASH_KEY, progress).commit();

					Intent broadcastIntent = new Intent(Constants.FLASH_VALUE_UPDATED_BROADCAST_NAME);
					broadcastIntent.putExtra(Constants.KEY_NEW_VALUE, progress);
					sendBroadcast(broadcastIntent);
				}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) { }
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) { }
			});

			mBrightnessSlider.setProgress(mPreferences.getInt(Constants.SETTINGS_FLASH_KEY, 0));
		}
		
		if(mSetWidgetValueButton != null) {
			mSetWidgetValueButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					int progress = mPreferences.getInt(Constants.SETTINGS_FLASH_KEY, 0);
					if (progress == 0) {
						Toast.makeText(MainActivity.this, R.string.toast_set_widget_off, Toast.LENGTH_SHORT).show();
					} else {
						mPreferences.edit().putInt(Constants.SETTINGS_WIDGET_KEY, progress).commit();
						Toast.makeText(MainActivity.this, R.string.toast_done, Toast.LENGTH_SHORT).show();
					}
				}
			});
		}

		if (Utils.getSysFsFile() != null) {
			checkForRoot();
		} else {
			complainAbout(Errors.NO_SYSFS_FILE);
		}
	}

	BroadcastReceiver mFlashValueUpdatedReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int newValue = intent.getIntExtra(Constants.KEY_NEW_VALUE, 0);
			mBrightnessSlider.setProgress(newValue);
		}
	};

	private void getViews() {
		mBrightnessSlider = (SeekBar) findViewById(R.id.flashlightBrightnessSlider);
		mSetWidgetValueButton = (Button) findViewById(R.id.widgetLevelButton);
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

		alertDialog.setCancelable(false);
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

					Uri packageUri = Uri.parse(String.format("package:%s", Constants.PACKAGE_NAME));
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
				int progress = mBrightnessSlider.getProgress();
				int newValue=progress;
				if (progress != 0 && mInvertValues) {
					//Invert nonzero values if required
					newValue = Math.max(1, mMaxValue+1-progress);
				}
				updateTorchValue(newValue);
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		MenuItem item = menu.findItem(R.id.enable_ads);
		item.setChecked(mPreferences.getBoolean("enable_ads", true));

		MenuItem invertValuesItem = menu.findItem(R.id.invert_values);
		invertValuesItem.setChecked(mPreferences.getBoolean("invert_values", false));

		return true;
	}

	private boolean updateTorchValue(int value) {
		showOngoingNotification((value > 0));
		return Utils.updateTorchValue(value, mShell);
	}

	private void openShell() {
		try {
			mShell = RootTools.getShell(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void showOngoingNotification(boolean show) {
		Utils.showOngoingNotification(this, show);
	}
	
	private void setNewMaximum() {
		final View content = LayoutInflater.from(this).inflate(R.layout.new_maximum_layout, null);
		final SeekBar seekBar = (SeekBar)content.findViewById(R.id.seekbar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			final TextView textView = (TextView)content.findViewById(R.id.instructions_textview);

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				textView.setText(getText(R.string.maximum_layout_instructions).toString().replace("%s", String.valueOf(progress+1)) );
				updateTorchValue(progress+1);
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
		});
		
		//Initial redraw
		seekBar.setProgress(0);

		new AlertDialog.Builder(this)
			.setTitle(R.string.set_maximum)
			.setView(content)
			.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mMaxValue = seekBar.getProgress()+1;
					mPreferences.edit().putInt(Constants.SETTINGS_MAX_VALUE, mMaxValue).commit();
					updateTorchValue(0);
					finish();
					startActivity(getIntent());
				}
			})
			.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					updateTorchValue(0);
				}
			})
			.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					updateTorchValue(0);
				}
			})
			.show();
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
			mPreferences.edit().putBoolean(Constants.SETTINGS_ENABLE_ADS, !item.isChecked()).commit();
			item.setChecked(!item.isChecked());
			return true;
		case R.id.set_maximum:
			setNewMaximum();
			return true;
		case R.id.invert_values:
			mBrightnessSlider.setProgress(0);
			updateTorchValue(0);
			mPreferences.edit().putBoolean(Constants.SETTINGS_INVERT_VALUES, !item.isChecked()).commit();
			item.setChecked(!item.isChecked());
			mInvertValues = item.isChecked();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mPreferences.edit().putInt(Constants.SETTINGS_FLASH_KEY, mBrightnessSlider.getProgress()).commit();
	}

	public void showAbout() {
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
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
		final Button widgetButton  = (Button)findViewById(R.id.widgetLevelButton);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adLayout.setEnabled(true);
				adLayout.setVisibility(View.VISIBLE);
				adLayout.loadAd(new AdRequest()); 
				RelativeLayout.LayoutParams widgetButtonLP = (RelativeLayout.LayoutParams)widgetButton.getLayoutParams();
				widgetButtonLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
				widgetButton.setLayoutParams(widgetButtonLP);
			}
		});
	}

	private void hideAd() {
		final AdView adLayout = (AdView) findViewById(R.id.adView);
		final Button widgetButton  = (Button)findViewById(R.id.widgetLevelButton);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				adLayout.setEnabled(false);
				adLayout.setVisibility(View.GONE);
				RelativeLayout.LayoutParams widgetButtonLP = (RelativeLayout.LayoutParams)widgetButton.getLayoutParams();
				widgetButtonLP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
				widgetButton.setLayoutParams(widgetButtonLP);
			}
		});
	}
}
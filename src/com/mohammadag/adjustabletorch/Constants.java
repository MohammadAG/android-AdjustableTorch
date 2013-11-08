package com.mohammadag.adjustabletorch;

public class Constants {
	public static final String PACKAGE_NAME = Constants.class.getPackage().getName();
	
	public static final String FLASH_VALUE_UPDATED_BROADCAST_NAME = "com.mohammadag.adjustabletorch.FLASH_VALUE_UPDATED";
	
	public static final String[] listOfFlashFiles = {
		"/sys/class/camera/flash/rear_flash",
		"/sys/class/camera/rear/rear_flash",
	};
	
	public static final String SETTINGS_FLASH_KEY = "flash_value";
	public static final String SETTINGS_INVERT_VALUES = "invert_values";
	public static final String SETTINGS_ENABLE_ADS = "enable_ads";
	public static final String PREFS_NAME = "AdjustableTorch";

	public static final int mNotificationId = 1;
}

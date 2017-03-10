/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package info.guardianproject.phoneypot;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferenceManager {
	
	private SharedPreferences appSharedPrefs;
    private Editor prefsEditor;
    
    public static final String LOW = "Low";
    public static final String MEDIUM = "Medium";
    public static final String HIGH = "High";
    
    public static final String FRONT = "Front";
    public static final String BACK = "Back";
	
	private static final String APP_SHARED_PREFS="me.ziccard.secureit";
	private static final String ACCELEROMETER_ACTIVE="accelerometer_active";
	private static final String ACCELEROMETER_SENSITIVITY="accelerometer_sensibility";
	private static final String CAMERA_ACTIVE="camera_active";
	private static final String CAMERA="camera";
	private static final String CAMERA_SENSITIVITY="camera_sensitivity";
	private static final String FLASH_ACTIVE="flash_active";
	private static final String MICROPHONE_ACTIVE="microphone_active";
	private static final String MICROPHONE_SENSITIVITY="microphone_sensitivity";
	private static final String SMS_ACTIVE="sms_active";
	private static final String SMS_NUMBER="sms_number";
	private static final String UNLOCK_CODE="unlock_code";
	
	private static final String ACCESS_TOKEN="access_token";
	private static final String DELEGATED_ACCESS_TOKEN="deferred_access_token";
	
	private static final String PHONE_ID="phone_id";
	
	private static final String DIR_PATH = "/secureit";

	private Context context;
	
    public PreferenceManager(Context context) {
        this.context = context;
        this.appSharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, Activity.MODE_PRIVATE);
        this.prefsEditor = appSharedPrefs.edit();
    }
    
    public void activateAccelerometer(boolean active) {
    	prefsEditor.putBoolean(ACCELEROMETER_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getAccelerometerActivation() {
    	return appSharedPrefs.getBoolean(ACCELEROMETER_ACTIVE, true);
    }
    
    public void setAccelerometerSensitivity(String sensitivity) {
    	prefsEditor.putString(ACCELEROMETER_SENSITIVITY, sensitivity);
    	prefsEditor.commit();
    }
    
    public String getAccelerometerSensitivity() {
    	return appSharedPrefs.getString(ACCELEROMETER_SENSITIVITY, "");
    }
    
    public void activateCamera(boolean active) {
    	prefsEditor.putBoolean(CAMERA_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getCameraActivation() {
    	return appSharedPrefs.getBoolean(CAMERA_ACTIVE, true);
    }
    
    public void setCamera(String camera) {
    	prefsEditor.putString(CAMERA, camera);
    	prefsEditor.commit();
    }
    
    public String getCamera() {
    	return appSharedPrefs.getString(CAMERA, BACK);
    }
    
    public void setCameraSensitivity(String sensitivity) {
    	prefsEditor.putString(CAMERA_SENSITIVITY, sensitivity);
    	prefsEditor.commit();
    }
    
    public String getCameraSensitivity() {
    	return appSharedPrefs.getString(CAMERA_SENSITIVITY, "");
    }
    
    public void activateFlash(boolean active) {
    	prefsEditor.putBoolean(FLASH_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getFlashActivation() {
    	return appSharedPrefs.getBoolean(FLASH_ACTIVE, false);
    }
    
    public void activateMicrophone(boolean active) {
    	prefsEditor.putBoolean(MICROPHONE_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getMicrophoneActivation() {
    	return appSharedPrefs.getBoolean(MICROPHONE_ACTIVE, true);
    }
    
    public void setMicrophoneSensitivity(String sensitivity) {
    	prefsEditor.putString(MICROPHONE_SENSITIVITY, sensitivity);
    	prefsEditor.commit();
    }
    
    public String getMicrophoneSensitivity() {
    	return appSharedPrefs.getString(MICROPHONE_SENSITIVITY, "");
    }
    
    public void activateSms(boolean active) {
    	prefsEditor.putBoolean(SMS_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getSmsActivation() {
    	return appSharedPrefs.getBoolean(SMS_ACTIVE, false);
    }
    
    public void setSmsNumber(String number) {
    	prefsEditor.putString(SMS_NUMBER, number);
    	prefsEditor.commit();
    }
    
    public String getSmsNumber() {
    	return appSharedPrefs.getString(SMS_NUMBER, "");
    }

    
    public void setUnlockCode(String unlockCode) {
    	prefsEditor.putString(UNLOCK_CODE, unlockCode);
    	prefsEditor.commit();
    }
    
    public String getUnlockCode() {
    	return appSharedPrefs.getString(UNLOCK_CODE, "");
    }

    public void setAccessToken(String accessToken) {
    	prefsEditor.putString(ACCESS_TOKEN, accessToken);
    	prefsEditor.commit();
    }
    
    public String getAccessToken() {
    	return appSharedPrefs.getString(ACCESS_TOKEN, "");
    }
    
    public void unsetAccessToken() {
    	prefsEditor.remove(ACCESS_TOKEN);
    }
    
    public void setDelegatedAccessToken(String deferredAccessToken) {
    	prefsEditor.putString(DELEGATED_ACCESS_TOKEN, deferredAccessToken);
    	prefsEditor.commit();
    }
    
    public String getDelegatedAccessToken() {
    	return appSharedPrefs.getString(DELEGATED_ACCESS_TOKEN, "");
    }
    
    public void unsetDelegatedAccessToken() {
    	prefsEditor.remove(DELEGATED_ACCESS_TOKEN);
    }

    public void setPhoneId(String phoneId) {
    	prefsEditor.putString(PHONE_ID, phoneId);
    	prefsEditor.commit();
    }
    
    public void unsetPhoneId() {
    	prefsEditor.remove(PHONE_ID);
    }
    
    public String getPhoneId() {
    	return appSharedPrefs.getString(PHONE_ID, "");
    }
    
    public String getDirPath() {
    	return DIR_PATH;
    }
    
    public String getSMSText() {
        return context.getString(R.string.intrusion_detected);
    }

    public String getImagePath ()
    {
        return "/phoneypot";
    }

    public int getMaxImages ()
    {
        return 10;
    }

    public String getAudioPath ()
    {
        return "/phoneypot";

    }

    public int getAudioLength ()
    {
        return 5000;
    }
}

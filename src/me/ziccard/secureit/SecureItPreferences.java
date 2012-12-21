package me.ziccard.secureit;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class SecureItPreferences {
	
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
	private static final String REMOTE_ACTIVE="remote_active";
	private static final String REMOTE_EMAIL="remote_email";
	
	
    public SecureItPreferences(Context context) {
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
    	return appSharedPrefs.getBoolean(FLASH_ACTIVE, true);
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
    	return appSharedPrefs.getBoolean(SMS_ACTIVE, true);
    }
    
    public void setSmsNumber(String number) {
    	prefsEditor.putString(SMS_NUMBER, number);
    	prefsEditor.commit();
    }
    
    public String getSmsNumber() {
    	return appSharedPrefs.getString(SMS_NUMBER, "");
    }
    
    public void activateRemote(boolean active) {
    	prefsEditor.putBoolean(REMOTE_ACTIVE, active);
    	prefsEditor.commit();
    }
    
    public boolean getRemoteActivation() {
    	return appSharedPrefs.getBoolean(REMOTE_ACTIVE, true);
    }
    
    public void setRemoteEmail(String email) {
    	prefsEditor.putString(REMOTE_EMAIL, email);
    	prefsEditor.commit();
    }
    
    public String getRemoteEmail() {
    	return appSharedPrefs.getString(REMOTE_EMAIL, "");
    }

}

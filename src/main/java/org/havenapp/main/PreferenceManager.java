
/*
 * Copyright (c) 2017 Nathanial Freitas
 *
 *   This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.havenapp.main;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.havenapp.main.sensors.motion.LuminanceMotionDetector;
import org.havenapp.main.storage.SharedPreferenceStringLiveData;

import java.io.File;
import java.util.Date;
import java.util.Objects;


public class PreferenceManager {
	
    private SharedPreferences appSharedPrefs;
    private Editor prefsEditor;
    
    public static final String LOW = "Low";
    public static final String MEDIUM = "Medium";
    public static final String HIGH = "High";
    public static final String OFF = "Off";


    public static final String FRONT = "Front";
    public static final String BACK = "Back";
    public static final String NONE = "None";
	
    private static final String APP_SHARED_PREFS="org.havenapp.main";
    private static final String ACCELEROMETER_ACTIVE="accelerometer_active";
    private static final String ACCELEROMETER_SENSITIVITY="accelerometer_sensibility";
    private static final String CAMERA_ACTIVE="camera_active";
    public static final String CAMERA="camera";
    public static final String CAMERA_SENSITIVITY="camera_sensitivity";
    public static final String CONFIG_MOVEMENT ="config_movement";
    public static final String HEARTBEAT_MONITOR_ACTIVE="heartbeat_monitor_active";
    public static final String HEARTBEAT_MONITOR_DELAY="heartbeat_monitor_delay";
    public static final String HEARTBEAT_MONITOR_MESSAGE="heartbeat_monitor_message";
    public static final String MONITOR_SERVICE_ACTIVE="monitor_service_active";
    private static final String FLASH_ACTIVE="flash_active";
    private static final String MICROPHONE_ACTIVE="microphone_active";
    private static final String MICROPHONE_SENSITIVITY="microphone_sensitivity";
    public static final String CONFIG_SOUND = "config_sound";
    public static final String CONFIG_TIME_DELAY = "config_delay_time";
    public static final String REGISTER_SIGNAL = "register_signal";
    public static final String VERIFY_SIGNAL = "verify_signal";
    public static final String VOICE_VERIFY_SIGNAL = "voice_verify_signal";
    public static final String RESET_SIGNAL_CONFIG = "reset_signal_config";
    public static final String SIMULTANEOUS_IMAGE_MONITORING = "simultaneous_image_monitoring";
    private static final String UNLOCK_CODE="unlock_code";
	
    private static final String ACCESS_TOKEN="access_token";
    private static final String DELEGATED_ACCESS_TOKEN="deferred_access_token";
	
    private static final String PHONE_ID="phone_id";
    private static final String TIMER_DELAY="timer_delay";
    private static final String VIDEO_LENGTH="video_length";
    public static final String CONFIG_VIDEO_LENGTH ="config_video_length";
    private static final String DIR_PATH = "/secureit";

    public static final String REMOTE_ACCESS_ACTIVE = "remote_access_active";
    public static final String REMOTE_ACCESS_ONION = "remote_access_onion";
    public static final String REMOTE_ACCESS_CRED = "remote_access_credential";

    private static final String SIGNAL_USERNAME = "signal_username";
    private static final String SIGNAL_VERIFIED_USERNAME = "signal_verified_username";

    private static final String FIRST_LAUNCH = "first_launch";

    public static final String NOTIFICATION_TIME = "notification_time";

    public static final String DISABLE_BATTERY_OPT = "config_battery_optimizations";

    private static final String CURRENT_EVENT_START_TIME = "current_event_start_time";

    public static final String CONFIG_BASE_STORAGE = "config_base_storage";
    private static final String CONFIG_BASE_STORAGE_DEFAULT = "/haven";

    // keeping the key value same for data migration.
    static final String REMOTE_PHONE_NUMBER = "sms_number";
    static final String REMOTE_NOTIFICATION_ACTIVE = "remote_notification_active";

    private Context context;
	
    public PreferenceManager(Context context) {
        this.context = context;
        this.appSharedPrefs = context.getSharedPreferences(APP_SHARED_PREFS, AppCompatActivity.MODE_PRIVATE);
        this.prefsEditor = appSharedPrefs.edit();
    }

    public boolean isFirstLaunch() {
        return appSharedPrefs.getBoolean(FIRST_LAUNCH, true);
    }

    public void setFirstLaunch(boolean firstLaunch) {
        prefsEditor.putBoolean(FIRST_LAUNCH, firstLaunch);
        prefsEditor.commit();
    }

    /**
     * Returns the Signal username registered. This may not be a good way to check for
     * Signal set up since this may not be verified.
     *
     * Usages should be checked with {@link #isSignalVerified()}
     *
     * @see #isSignalVerified()
     *
     * @return the Signal username; null when nothing is set up
     */
    public String getSignalUsername ()
    {
        return appSharedPrefs.getString(SIGNAL_USERNAME,null);
    }

    public void setSignalUsername (String signalUsername)
    {
        prefsEditor.putString(SIGNAL_USERNAME,signalUsername);
        prefsEditor.commit();
    }

    /**
     * Returns the Signal username verified. This may not be a good way to check for
     * Signal set up since this may invalidated by a call to register with a different username.
     *
     * Usages should be checked with {@link #isSignalVerified()}
     *
     * @see #isSignalVerified()
     *
     * @return the verified Signal username; null when no Signal username is verified even though registered.
     */
    @Nullable
    public String getVerifiedSignalUsername() {
        return appSharedPrefs.getString(SIGNAL_VERIFIED_USERNAME, null);
    }

    public void setVerifiedSignalUsername(String verifiedSignalUsername) {
        prefsEditor.putString(SIGNAL_VERIFIED_USERNAME, verifiedSignalUsername);
        prefsEditor.commit();
    }

    /**
     * Checks if Signal is registered and verified for the Signal username returned by
     * {@link #getSignalUsername()}
     *
     * @return true iff registered Signal username is same as that of the verified one.
     */
    public boolean isSignalVerified() {
        return !TextUtils.isEmpty(getSignalUsername()) &&
                getSignalUsername().equals(getVerifiedSignalUsername());
    }

    public void activateRemoteAccess (boolean active) {
        prefsEditor.putBoolean(REMOTE_ACCESS_ACTIVE,active);
        prefsEditor.commit();
    }

    public boolean getRemoteAccessActive ()
    {
        return appSharedPrefs.getBoolean(REMOTE_ACCESS_ACTIVE,false);
    }

    public void activateMonitorService (boolean active) {
        prefsEditor.putBoolean(MONITOR_SERVICE_ACTIVE,active);
        prefsEditor.commit();
    }

    public boolean getMonitorServiceActive ()
    {
        return appSharedPrefs.getBoolean(MONITOR_SERVICE_ACTIVE,false);
    }

    public void setRemoteAccessOnion (String onionAddress) {
        prefsEditor.putString(REMOTE_ACCESS_ONION,onionAddress);
        prefsEditor.commit();
    }

    public String getRemoteAccessOnion () {
        return appSharedPrefs.getString(REMOTE_ACCESS_ONION,"");
    }

    public void setRemoteAccessCredential (String remoteCredential) {
        prefsEditor.putString(REMOTE_ACCESS_CRED,remoteCredential);
        prefsEditor.commit();
    }

    public String getRemoteAccessCredential () {
        return appSharedPrefs.getString(REMOTE_ACCESS_CRED,null);
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
    	return appSharedPrefs.getString(ACCELEROMETER_SENSITIVITY, HIGH);
    }

    public void setActivateVideoMonitoring(boolean active) {
        prefsEditor.putBoolean(context.getResources().getString(R.string.video_active_preference_key), active);
        prefsEditor.commit();
    }

    public boolean getVideoMonitoringActive() {
        return appSharedPrefs.getBoolean(context.getResources().getString(R.string.video_active_preference_key), false);
    }

    public void setSimultaneousImageMonitoring(boolean active) {
        prefsEditor.putBoolean(SIMULTANEOUS_IMAGE_MONITORING, active);
        prefsEditor.commit();
    }

    public boolean isSimultaneousImageMonitoring() {
        return appSharedPrefs.getBoolean(SIMULTANEOUS_IMAGE_MONITORING, false);
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
    	return appSharedPrefs.getString(CAMERA, FRONT);
    }

    @NonNull
    public SharedPreferenceStringLiveData getCameraLiveData() {
        return new SharedPreferenceStringLiveData(appSharedPrefs, CAMERA, FRONT);
    }
    
    public void setCameraSensitivity(int sensitivity) {
    	prefsEditor.putInt(CAMERA_SENSITIVITY, sensitivity);
    	prefsEditor.commit();
    }
    
    public int getCameraSensitivity() {
    	return appSharedPrefs.getInt(CAMERA_SENSITIVITY, LuminanceMotionDetector.MOTION_MEDIUM);
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
    	return appSharedPrefs.getString(MICROPHONE_SENSITIVITY, MEDIUM);
    }

    public void setRemoteNotificationActive(boolean isRemoteNotificationActive) {
        prefsEditor.putBoolean(REMOTE_NOTIFICATION_ACTIVE, isRemoteNotificationActive);
        prefsEditor.apply();
    }

    public boolean isRemoteNotificationActive() {
        return appSharedPrefs.getBoolean(REMOTE_NOTIFICATION_ACTIVE, false);
    }

    public void setRemotePhoneNumber(@NonNull String remotePhoneNumber) {
        prefsEditor.putString(REMOTE_PHONE_NUMBER, remotePhoneNumber.trim());
        prefsEditor.apply();
    }

    @NonNull
    public String getRemotePhoneNumber() {
        return Objects.requireNonNull(appSharedPrefs.getString(REMOTE_PHONE_NUMBER, ""));
    }

    public int getTimerDelay ()
    {
        return appSharedPrefs.getInt(TIMER_DELAY,30);
    }

    public void setTimerDelay (int delay)
    {
        prefsEditor.putInt(TIMER_DELAY,delay);
        prefsEditor.commit();
    }

    public int getMonitoringTime ()
    {
        return appSharedPrefs.getInt(VIDEO_LENGTH,30);
    }

    public void setMonitoringTime (int delay)
    {
        prefsEditor.putInt(VIDEO_LENGTH,delay);
        prefsEditor.commit();
    }

    public void setVoiceVerification(boolean active) {
        prefsEditor.putBoolean(VOICE_VERIFY_SIGNAL, active);
        prefsEditor.commit();
    }

    public boolean getVoiceVerificationEnabled() {
        return appSharedPrefs.getBoolean(VOICE_VERIFY_SIGNAL, false);
    }

    public String getDirPath() {
    	return DIR_PATH;
    }
    
    public String getSMSText() {
        return context.getString(R.string.intrusion_detected);
    }

    public int getMaxImages ()
    {
        return 10;
    }

    public String getBaseStoragePath() {
        return appSharedPrefs.getString(CONFIG_BASE_STORAGE,CONFIG_BASE_STORAGE_DEFAULT);
    }

    public String getDefaultMediaStoragePath() {
        return appSharedPrefs.getString(CONFIG_BASE_STORAGE,CONFIG_BASE_STORAGE_DEFAULT) + File.separator + getCurrentSession(); //phoneypot is the old code name for Haven
    }

    public void setDefaultMediaStoragePath (String path)
    {
        prefsEditor.putString(CONFIG_BASE_STORAGE,path);
        prefsEditor.commit();
    }

    public int getAudioLength ()
    {
        return 15000; //30 seconds
    }

    public int getNotificationTimeMs () {
        return appSharedPrefs.getInt(NOTIFICATION_TIME,-1); //time in minutes times by seconds
    }

    public void setNotificationTimeMs (int notificationTimeMs) {
        prefsEditor.putInt(NOTIFICATION_TIME,notificationTimeMs);
        prefsEditor.commit();
    }

    public void activateHeartbeat(boolean active) {
        prefsEditor.putBoolean(HEARTBEAT_MONITOR_ACTIVE, active);
        prefsEditor.commit();
    }

    public void setHeartbeatMonitorNotifications (int notificationTimeMs) {
        prefsEditor.putInt(HEARTBEAT_MONITOR_DELAY,notificationTimeMs);
        prefsEditor.commit();
    }

    public boolean getHeartbeatActive() {
        return appSharedPrefs.getBoolean(HEARTBEAT_MONITOR_ACTIVE, false);
    }

    public int getHeartbeatNotificationTimeMs () {
        return appSharedPrefs.getInt(HEARTBEAT_MONITOR_DELAY,300000);
    }

    public String getHeartbeatMonitorMessage ()
    {
        return appSharedPrefs.getString(HEARTBEAT_MONITOR_MESSAGE,null);
    }

    public void setHeartbeatMonitorMessage (String hearbeatMessage)
    {
        prefsEditor.putString(HEARTBEAT_MONITOR_MESSAGE, hearbeatMessage);
        prefsEditor.commit();
    }

    public String getHeartbeatPrefix() {
        return context.getString(R.string.hearbeat_monitor_initial_message_1);
    }

    public String getHeartbeatSuffix() {
        return context.getString(R.string.hearbeat_monitor_initial_message_2);
    }


    /**
     * Set the {@link org.havenapp.main.model.Event#startTime} for the ongoing event.
     * Sets a string with the format {@link Utils#DATE_TIME_PATTERN}
     * representing current date and time for the key {@link #CURRENT_EVENT_START_TIME}.
     *
     * @param startTime the {@link org.havenapp.main.model.Event#startTime} for an
     * {@link org.havenapp.main.model.Event}
     */
    public void setCurrentSession(Date startTime) {
        prefsEditor.putString(CURRENT_EVENT_START_TIME, Utils.getDateTime(startTime));
        prefsEditor.commit();
    }

    /**
     * Get the {@link org.havenapp.main.model.Event#startTime} for the ongoing event.
     *
     * @return the string corresponding to pref key {@link #CURRENT_EVENT_START_TIME}.
     * Default value is unknown_session.
     */
    private String getCurrentSession() {
        return appSharedPrefs.getString(CURRENT_EVENT_START_TIME, "unknown_session");
    }
}

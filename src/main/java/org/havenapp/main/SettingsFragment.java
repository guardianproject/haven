package org.havenapp.main;

/**
 * Created by Anupam Das (opticod) on 29/12/17.
 */

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.service.SignalExecutorTask;
import org.havenapp.main.service.SignalSender;
import org.havenapp.main.service.WebServer;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraConfigureActivity;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;
import java.util.Locale;

import info.guardianproject.netcipher.proxy.OrbotHelper;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, TimePickerDialog.OnTimeSetListener {

    private PreferenceManager preferences;
    private HavenApp app;
    private AppCompatActivity mActivity;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
        mActivity = (AppCompatActivity) getActivity();
        preferences = new PreferenceManager(mActivity);
        setHasOptionsMenu(true);
        app = (HavenApp) mActivity.getApplication();


        /*
         * We create an application directory to store images and audio
         */
        File directory = new File(Environment.getExternalStorageDirectory() + preferences.getDirPath());
        directory.mkdirs();

        if (preferences.getCameraActivation()) {

            String camera = preferences.getCamera();
            switch (camera) {
                case PreferenceManager.FRONT:
                    ((ListPreference) findPreference(PreferenceManager.CAMERA)).setValueIndex(0);
                    break;
                case PreferenceManager.BACK:
                    ((ListPreference) findPreference(PreferenceManager.CAMERA)).setValueIndex(1);
                    break;
                case PreferenceManager.OFF:
                    ((ListPreference) findPreference(PreferenceManager.CAMERA)).setValueIndex(2);
                    break;
            }

        }

        SwitchPreference switchPreference =
                (SwitchPreference) findPreference(PreferenceManager.REMOTE_NOTIFICATION_ACTIVE);

        switchPreference.setChecked(preferences.isRemoteNotificationActive());

        switchPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    // user wants to enable/disable remote notification

                    boolean enabled = (Boolean) newValue;

                    if (enabled && !canSendRemoteNotification()) {
                        collectDataForRemoteNotification();
                    }

                    preferences.setRemoteNotificationActive(enabled && canSendRemoteNotification());
                    switchPreference.setChecked(enabled && canSendRemoteNotification());

                    return false;
                });

        findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).setOnPreferenceClickListener(preference -> {
            if (preferences.getRemotePhoneNumber().isEmpty()) {
                ((EditTextPreference) findPreference(PreferenceManager.REMOTE_PHONE_NUMBER)).setText(getCountryCode());
            }
            return false;
        });
        findPreference(PreferenceManager.REGISTER_SIGNAL).setOnPreferenceClickListener(preference -> {
            if (preferences.getSignalUsername() == null) {
                ((EditTextPreference) findPreference(PreferenceManager.REGISTER_SIGNAL)).setText(getCountryCode());
            }
            return false;
        });

        if (checkValidString(preferences.getRemotePhoneNumber())) {
            ((EditTextPreference) findPreference(PreferenceManager.REMOTE_PHONE_NUMBER))
                    .setText(preferences.getRemotePhoneNumber());
            findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).setSummary(preferences.getRemotePhoneNumber());
        } else {
            findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).setSummary(R.string.sms_dialog_summary);
        }

        if (preferences.getRemoteAccessActive()) {
            ((SwitchPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).setChecked(true);
        }

        if (checkValidString(preferences.getRemoteAccessOnion())) {
            ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ONION)).setText(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
            findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
        } else {
            findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(R.string.remote_access_hint);
        }

        if (checkValidString(preferences.getRemoteAccessCredential())) {
            ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_CRED)).setText(preferences.getRemoteAccessCredential().trim());
            findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.bullets);
        } else {
            findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.remote_access_credential_hint);
        }

        if (checkValidString(preferences.getSignalUsername())) {
            String signalNum = "+" + preferences.getSignalUsername().trim().replaceAll("[^0-9]", "");
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(signalNum);
        } else {
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.register_signal_desc);
        }

        if (preferences.getNotificationTimeMs()>0)
        {
            findPreference(PreferenceManager.NOTIFICATION_TIME).setSummary(preferences.getNotificationTimeMs()/60000 + " " + getString(R.string.minutes));
        }

        findPreference(PreferenceManager.RESET_SIGNAL_CONFIG).setOnPreferenceClickListener(preference -> {
            showResetSignalDialog();
            return true;
        });

        if (preferences.getHeartbeatActive())
        {
            ((SwitchPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_ACTIVE)).setChecked(true);
            if (preferences.getHeartbeatActive()) {
                findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(preferences.getHeartbeatNotificationTimeMs() / 60000 + " " + getString(R.string.minutes));
            }
            else
                findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(R.string.heartbeat_time_dialog);
        }

        if (preferences.getHeartbeatNotificationTimeMs()> 300000)
        {
            findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(preferences.getHeartbeatNotificationTimeMs() / 60000 + " " + getString(R.string.minutes));
        }

        if (preferences.getHeartbeatMonitorMessage() == null)
        {
            findPreference(PreferenceManager.HEARTBEAT_MONITOR_MESSAGE).setSummary(R.string.hearbeat_message_summary);
        } else {
            findPreference(PreferenceManager.HEARTBEAT_MONITOR_MESSAGE).setSummary(R.string.hearbeat_message_summary_on);
        }

        Preference prefCameraSensitivity = findPreference(PreferenceManager.CAMERA_SENSITIVITY);
        prefCameraSensitivity.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(mActivity, CameraConfigureActivity.class));
            return true;
        });

        Preference prefConfigMovement = findPreference(PreferenceManager.CONFIG_MOVEMENT);
        prefConfigMovement.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(mActivity, AccelConfigureActivity.class));
            return true;
        });

        Preference prefConfigSound = findPreference(PreferenceManager.CONFIG_SOUND);
        prefConfigSound.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(mActivity, MicrophoneConfigureActivity.class));
            return true;
        });

        Preference prefConfigTimeDelay = findPreference(PreferenceManager.CONFIG_TIME_DELAY);
        prefConfigTimeDelay.setOnPreferenceClickListener(preference -> {
            showTimeDelayDialog(PreferenceManager.CONFIG_TIME_DELAY);
            return true;
        });

        Preference prefConfigVideoLength = findPreference(PreferenceManager.CONFIG_VIDEO_LENGTH);
        prefConfigVideoLength.setOnPreferenceClickListener(preference -> {
            showTimeDelayDialog(PreferenceManager.CONFIG_VIDEO_LENGTH);
            return true;
        });

        Preference prefDisableBatteryOpt = findPreference(PreferenceManager.DISABLE_BATTERY_OPT);
        prefDisableBatteryOpt.setOnPreferenceClickListener(preference -> {
            requestChangeBatteryOptimizations();
            return true;
        });

        checkSignalUsername();
        checkSignalUsernameVerification();
        ((EditTextPreference) findPreference(PreferenceManager.VERIFY_SIGNAL)).setText("");
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

    }

    private void showResetSignalDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.reset_configuration_question)
                .setMessage(R.string.reset_configuration_desc)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    resetSignalAndClearPrefs();
                    findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.signal_dialog_summary);
                    findPreference(PreferenceManager.NOTIFICATION_TIME).setSummary(R.string.notification_time_summary);
                    checkSignalUsernameVerification();
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean canSendRemoteNotification() {
        String remotePhoneNumber = preferences.getRemotePhoneNumber();
        String signalUsername = preferences.getSignalUsername();
        return !remotePhoneNumber.isEmpty() && !getCountryCode().equalsIgnoreCase(remotePhoneNumber) &&
                !TextUtils.isEmpty(signalUsername) && !getCountryCode().equalsIgnoreCase(signalUsername);
    }

    /**
     * Collect data required for Remote notification with Signal.
     * We need a remote phone number and a verified signal Username.
     */
    @SuppressLint("RestrictedApi")
    private void collectDataForRemoteNotification() {
        String remotePhoneNumber = preferences.getRemotePhoneNumber();
        if (remotePhoneNumber.isEmpty() || getCountryCode().equalsIgnoreCase(remotePhoneNumber)) {
            findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).performClick();
        }
        String signalUsername = preferences.getSignalUsername();
        if (TextUtils.isEmpty(signalUsername)) {
            findPreference(PreferenceManager.REGISTER_SIGNAL).performClick();
        } else {
            if (getActivity() != null) {
                Utils.hideKeyboard(getActivity());
            }
            activateSignal(signalUsername, null);
        }
    }

    private void onRemoteNotificationParameterChange() {
        SwitchPreference switchPreference =
                (SwitchPreference) findPreference(PreferenceManager.REMOTE_NOTIFICATION_ACTIVE);

        boolean remoteNotificationActive = canSendRemoteNotification();
        preferences.setRemoteNotificationActive(remoteNotificationActive);

        switchPreference.setChecked(remoteNotificationActive);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                save();
                return true;
            default:
                break;
        }

        return false;
    }

    protected void save() {
        preferences.activateAccelerometer(true);

        preferences.activateCamera(true);

        preferences.activateMicrophone(true);

        setPhoneNumber();

        boolean videoMonitoringActive = ((SwitchPreference) findPreference(mActivity.getResources().getString(R.string.video_active_preference_key))).isChecked();

        preferences.setActivateVideoMonitoring(videoMonitoringActive);

        boolean remoteNotificationActive =
                ((SwitchPreference) findPreference(PreferenceManager.REMOTE_NOTIFICATION_ACTIVE)).isChecked();
        preferences.setRemoteNotificationActive(remoteNotificationActive);

        boolean remoteAccessActive = ((SwitchPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).isChecked();

        preferences.activateRemoteAccess(remoteAccessActive);
        String password = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_CRED)).getText();

        if (checkValidStrings(password, preferences.getRemoteAccessCredential()) && (TextUtils.isEmpty(preferences.getRemoteAccessCredential()) || !password.trim().equals(preferences.getRemoteAccessCredential().trim()))) {
            preferences.setRemoteAccessCredential(password.trim());
            app.stopServer();
            app.startServer();
        }

        preferences.setVoiceVerification(false);

        boolean heartbeatMonitorActive = ((SwitchPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_ACTIVE)).isChecked();

        preferences.activateHeartbeat(heartbeatMonitorActive);

        mActivity.setResult(AppCompatActivity.RESULT_OK);
        mActivity.finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            String onionHost = data.getStringExtra("hs_host");

            if (checkValidString(onionHost)) {
                preferences.setRemoteAccessOnion(onionHost.trim());
                ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ONION)).setText(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
                if (checkValidString(preferences.getRemoteAccessOnion())) {
                    findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
                } else {
                    findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(R.string.remote_access_hint);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.CAMERA, 2);
                break;
            case 2:
                askForPermission(Manifest.permission.RECORD_AUDIO, 3);
                break;

        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferenceManager.CAMERA:
                switch (Integer.parseInt(((ListPreference) findPreference(PreferenceManager.CAMERA)).getValue())) {
                    case 0:
                        preferences.setCamera(PreferenceManager.FRONT);
                        findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.FRONT);
                        break;
                    case 1:
                        preferences.setCamera(PreferenceManager.BACK);
                        findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.BACK);
                        break;
                    case 2:
                        preferences.setCamera(PreferenceManager.NONE);
                        findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.NONE);
                        break;

                }
                break;
            case PreferenceManager.REMOTE_ACCESS_ACTIVE:
                boolean remoteAccessActive = ((SwitchPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).isChecked();
                if (remoteAccessActive) {
                    checkRemoteAccessOnion();
                    app.startServer();
                } else {
                    app.stopServer();
                }
                break;
            case PreferenceManager.REGISTER_SIGNAL:
                String signalNum = ((EditTextPreference) findPreference(PreferenceManager.REGISTER_SIGNAL)).getText();

                if (checkValidString(signalNum) && !getCountryCode().equalsIgnoreCase(signalNum)) {
                    signalNum = "+" + signalNum.trim().replaceAll("[^0-9]", "");

                    preferences.setSignalUsername(signalNum);
                    findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(signalNum);

                    resetSignal(preferences.getSignalUsername());
                    if (getActivity() != null) {
                        Utils.hideKeyboard(getActivity());
                    }
                    activateSignal(preferences.getSignalUsername(), null);
                } else if (!getCountryCode().equalsIgnoreCase(signalNum)) {
                    preferences.setSignalUsername(null);
                    findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.register_signal_desc);
                }
                onRemoteNotificationParameterChange();
                checkSignalUsernameVerification();
                break;
            case PreferenceManager.VERIFY_SIGNAL: {
                String text = ((EditTextPreference) findPreference(PreferenceManager.VERIFY_SIGNAL)).getText();
                if (getActivity() != null) {
                    Utils.hideKeyboard(getActivity());
                }
                activateSignal(preferences.getSignalUsername(), text);
                onRemoteNotificationParameterChange();
                break;
            }
            case PreferenceManager.REMOTE_PHONE_NUMBER:
                setPhoneNumber();
                onRemoteNotificationParameterChange();
                if (getActivity() != null) {
                    Utils.hideKeyboard(getActivity());
                }
                break;
            case PreferenceManager.NOTIFICATION_TIME:
                try
                {
                    String text = ((EditTextPreference)findPreference(PreferenceManager.NOTIFICATION_TIME)).getText();
                    int notificationTimeMs = Integer.parseInt(text)*60000;
                    preferences.setNotificationTimeMs(notificationTimeMs);
                    findPreference(PreferenceManager.NOTIFICATION_TIME).setSummary(preferences.getNotificationTimeMs()/60000 + " " + getString(R.string.minutes));

                }
                catch (NumberFormatException ne)
                {
                    //error parsing user value
                }

                break;
            case PreferenceManager.REMOTE_ACCESS_ONION: {
                EditTextPreference preference = findPreference(PreferenceManager.REMOTE_ACCESS_ONION);
                assert preference != null;
                String text = preference.getText();
                if (checkValidString(text)) {
                    preferences.setRemoteAccessOnion(text.trim());
                    preference.setSummary(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
                } else {
                    preferences.setRemoteAccessOnion(text);
                    preference.setSummary(R.string.remote_access_hint);
                }
                break;
            }
            case PreferenceManager.REMOTE_ACCESS_CRED: {
                EditTextPreference preference = findPreference(PreferenceManager.REMOTE_ACCESS_CRED);
                assert preference != null;
                String text = preference.getText();
                if (checkValidString(text)) {
                    preferences.setRemoteAccessCredential(text.trim());
                    preference.setSummary(R.string.bullets);
                } else {
                    preferences.setRemoteAccessCredential(text);
                    preference.setSummary(R.string.remote_access_credential_hint);
                }
                break;
            }
            case PreferenceManager.HEARTBEAT_MONITOR_ACTIVE: {
                boolean isMonitoring = preferences.getHeartbeatActive();
                boolean hbSwitchOn = ((SwitchPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_ACTIVE)).isChecked();
                if (!isMonitoring && hbSwitchOn) {
                    preferences.activateHeartbeat(true);
                    findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(preferences.getHeartbeatNotificationTimeMs() / 60000 + " " + getString(R.string.minutes));
                    if (preferences.getMonitorServiceActive()) {
                        SignalSender sender = SignalSender.getInstance(getActivity(), preferences.getSignalUsername());
                        sender.startHeartbeatTimer(preferences.getHeartbeatNotificationTimeMs());
                    }
                } else if (!hbSwitchOn && isMonitoring) {
                    preferences.activateHeartbeat(false);
                    findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(R.string.hearbeat_monitor_dialog);
                    if (preferences.getMonitorServiceActive()) {
                        SignalSender sender = SignalSender.getInstance(getActivity(), preferences.getSignalUsername());
                        sender.stopHeartbeatTimer();
                    }
                }
                break;
            }
            case PreferenceManager.HEARTBEAT_MONITOR_DELAY: {
                try {
                    String text = ((EditTextPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY)).getText();
                    int notificationTimeMs = Integer.parseInt(text) * 60000;
                    if (notificationTimeMs <= 0)
                        notificationTimeMs = 300000;

                    preferences.setHeartbeatMonitorNotifications(notificationTimeMs);
                    findPreference(PreferenceManager.HEARTBEAT_MONITOR_DELAY).setSummary(preferences.getHeartbeatNotificationTimeMs() / 60000 + " " + getString(R.string.minutes));

                    boolean heartbeatActive = ((SwitchPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_ACTIVE)).isChecked();
                    if (heartbeatActive && preferences.getMonitorServiceActive()) {
                        SignalSender sender = SignalSender.getInstance(getActivity(), preferences.getSignalUsername());
                        sender.stopHeartbeatTimer();
                        sender.startHeartbeatTimer(preferences.getHeartbeatNotificationTimeMs());
                    }
                } catch (NumberFormatException ne) {
                    //error parsing user value
                }
                break;
            }
            case PreferenceManager.HEARTBEAT_MONITOR_MESSAGE: {
                String text = ((EditTextPreference) findPreference(PreferenceManager.HEARTBEAT_MONITOR_MESSAGE)).getText();

                if (checkValidString(text)) {
                    preferences.setHeartbeatMonitorMessage(text);
                    findPreference(PreferenceManager.HEARTBEAT_MONITOR_MESSAGE).setSummary(R.string.hearbeat_message_summary_on);
                }
                else {
                    preferences.setHeartbeatMonitorMessage(null);
                    findPreference(PreferenceManager.HEARTBEAT_MONITOR_MESSAGE).setSummary(R.string.hearbeat_message_summary);
                }
                break;
            }
            case PreferenceManager.CONFIG_BASE_STORAGE: {
                setDefaultStoragePath();
                break;
            }
        }
    }

    String getCountryCode() {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        return "+" + String.valueOf(phoneUtil.getCountryCodeForRegion(Locale.getDefault().getCountry()));
    }

    private void setDefaultStoragePath () {
        String defaultStoragePath = ((EditTextPreference) findPreference(PreferenceManager.CONFIG_BASE_STORAGE)).getText();
        preferences.setDefaultMediaStoragePath(defaultStoragePath);
    }

    private void setPhoneNumber() {
        String phoneNumber = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_PHONE_NUMBER)).getText();

        if (checkValidString(phoneNumber) && !getCountryCode().equalsIgnoreCase(phoneNumber)) {
            preferences.setRemotePhoneNumber(phoneNumber.trim());
            findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).setSummary(phoneNumber.trim());
        } else if (!getCountryCode().equalsIgnoreCase(phoneNumber)){
            preferences.setRemotePhoneNumber("");
            findPreference(PreferenceManager.REMOTE_PHONE_NUMBER).setSummary(R.string.sms_dialog_message);
        }
    }

    private void showTimeDelayDialog(String configVideoLength) {
        int totalSecs;
        if (configVideoLength.equalsIgnoreCase(PreferenceManager.CONFIG_TIME_DELAY)) {
            totalSecs = preferences.getTimerDelay();
        } else {
            totalSecs = preferences.getMonitoringTime();
        }
        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;


        TimePickerDialog mTimePickerDialog = TimePickerDialog.newInstance(this, hours, minutes, seconds, true);
        mTimePickerDialog.enableSeconds(true);
        if (configVideoLength.equalsIgnoreCase(PreferenceManager.CONFIG_TIME_DELAY)) {
            mTimePickerDialog.show(getFragmentManager(), "TimeDelayPickerDialog");
        } else {
            mTimePickerDialog.show(getFragmentManager(), "VideoLengthPickerDialog");
        }
    }

    private boolean checkValidString(String a) {
        return a != null && !a.trim().isEmpty();
    }

    private boolean checkValidStrings(String a, String b) {
        return a != null && !a.trim().isEmpty() && b != null && !b.trim().isEmpty();
    }

    private void checkSignalUsername() {
        if (checkValidString(preferences.getSignalUsername())) {
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(preferences.getSignalUsername().trim());
            ((EditTextPreference) findPreference(PreferenceManager.REGISTER_SIGNAL)).setText(preferences.getSignalUsername().trim());
        } else {
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.signal_dialog_summary);
        }
    }

    private void checkSignalUsernameVerification() {
        String signalUsername = preferences.getSignalUsername();

        // this will fail for all users currently has signal verified
        if (checkValidString(signalUsername) &&
                signalUsername.equals(preferences.getVerifiedSignalUsername())) {
            findPreference(PreferenceManager.VERIFY_SIGNAL)
                    .setSummary(R.string.verification_dialog_summary_verified);
        } else {
            findPreference(PreferenceManager.VERIFY_SIGNAL)
                    .setSummary(R.string.verification_dialog_summary);
        }
    }

    private void activateSignal(String username, String verifyCode) {
        SignalSender sender = SignalSender.getInstance(mActivity, username);

        if (TextUtils.isEmpty(verifyCode)) {
            ProgressDialog progressDialog = ProgressDialog.show(getContext(), getString(R.string.registering_to_signal),
                    getString(R.string.signal_registration_desc));
            sender.register(preferences.getVoiceVerificationEnabled(),
                    new SignalExecutorTask.TaskResult() {
                @Override
                public void onSuccess(@NonNull String msg) {
                    if (isAdded() && getActivity() != null) {
                        progressDialog.dismiss();
                    }
                    showRegistrationSuccessDialog();
                }

                @Override
                public void onFailure(@NonNull String msg) {
                    if (isAdded() && getActivity() != null) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            ProgressDialog progressDialog = ProgressDialog.show(getContext(), getString(R.string.verifying_signal),
                    getString(R.string.verifying_signal_desc));
            sender.verify(verifyCode, new SignalExecutorTask.TaskResult() {
                @Override
                public void onSuccess(@NonNull String msg) {
                    if (isAdded() && getActivity() != null) {
                        progressDialog.dismiss();
                    }
                    // mark that the current registered signal username is verified
                    preferences.setVerifiedSignalUsername(preferences.getSignalUsername());
                    checkSignalUsernameVerification();
                    showVerificationSuccessDialog();
                }

                @Override
                public void onFailure(@NonNull String msg) {
                    if (isAdded() && getActivity() != null) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showRegistrationSuccessDialog() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.registration_successful)
                .setMessage(getString(R.string.signal_reg_success_desc, preferences.getSignalUsername()))
                .setPositiveButton(R.string.verify, (dialog, which) -> {
                    dialog.dismiss();
                    findPreference(PreferenceManager.VERIFY_SIGNAL).performClick();
                })
                .setNegativeButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showVerificationSuccessDialog() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.verification_successful)
                .setMessage(R.string.signal_verification_success_desc)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void resetSignalAndClearPrefs() {
        resetSignal(preferences.getSignalUsername());
        preferences.setSignalUsername(null);
        preferences.setVerifiedSignalUsername(null);
        preferences.setNotificationTimeMs(-1);
    }

    private void resetSignal(String username) {
        if (checkValidString((username))) {
            SignalSender sender = SignalSender.getInstance(mActivity, username.trim());
            sender.reset();
        }
    }

    private void checkRemoteAccessOnion() {
        if (OrbotHelper.isOrbotInstalled(mActivity)) {
            OrbotHelper.requestStartTor(mActivity);

            if (preferences.getRemoteAccessOnion() != null && TextUtils.isEmpty(preferences.getRemoteAccessOnion().trim())) {
                OrbotHelper.requestHiddenServiceOnPort(mActivity, WebServer.LOCAL_PORT);
            }
        } else {
            Toast.makeText(mActivity, R.string.remote_access_onion_error, Toast.LENGTH_LONG).show();
        }
    }

    private void askForPermission(String permission, Integer requestCode) {
        if (mActivity != null && ContextCompat.checkSelfPermission(mActivity, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(mActivity, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(mActivity, new String[]{permission}, requestCode);
            }
        }
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        int Seconds = second + minute * 60 + hourOfDay * 60 * 60;
        if (view.getTag().equalsIgnoreCase("TimeDelayPickerDialog")) {
            preferences.setTimerDelay(Seconds);
        } else if (view.getTag().equalsIgnoreCase("VideoLengthPickerDialog")) {
            preferences.setMonitoringTime(Seconds);
        }
    }

    private void requestChangeBatteryOptimizations ()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getActivity().getPackageName();
            PowerManager pm = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
            if (pm.isIgnoringBatteryOptimizations(packageName))
                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            else {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
            }
            getActivity().startActivity(intent);
        }
    }

    public void checkCallToVerify (View v) {
        Switch callSwitch = v.findViewById(R.id.signalCallSwitch);
        if (callSwitch != null && callSwitch.isChecked()) {
            preferences.setVoiceVerification(true);
        } else {
            preferences.setVoiceVerification(false);
        }
    }
}

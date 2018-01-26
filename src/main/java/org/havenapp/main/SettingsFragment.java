package org.havenapp.main;

/**
 * Created by Anupam Das (opticod) on 29/12/17.
 */

import android.Manifest;
import android.app.Activity;
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
import android.support.annotation.NonNull;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.service.SignalSender;
import org.havenapp.main.service.WebServer;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraConfigureActivity;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, TimePickerDialog.OnTimeSetListener {

    private PreferenceManager preferences;
    private HavenApp app;
    private Activity mActivity;

    @Override
    public void onCreatePreferencesFix(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);
        mActivity = getActivity();
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

        if (preferences.getSmsActivation()) {
            ((SwitchPreferenceCompat) findPreference(PreferenceManager.SMS_ACTIVE)).setChecked(true);
        }

        if (checkValidString(preferences.getSmsNumber())) {
            ((EditTextPreference) findPreference(PreferenceManager.SMS_NUMBER)).setText(preferences.getSmsNumber().trim());
            findPreference(PreferenceManager.SMS_NUMBER).setSummary(preferences.getSmsNumber().trim());
        } else {
            findPreference(PreferenceManager.SMS_NUMBER).setSummary(R.string.sms_dialog_summary);
        }

        if (preferences.getRemoteAccessActive()) {
            ((SwitchPreferenceCompat) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).setChecked(true);
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
        ((EditTextPreference) findPreference(PreferenceManager.VERIFY_SIGNAL)).setText("");
        askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

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

        boolean remoteAccessActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).isChecked();

        preferences.activateRemoteAccess(remoteAccessActive);
        String password = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_CRED)).getText();

        if (checkValidStrings(password, preferences.getRemoteAccessCredential()) && (TextUtils.isEmpty(preferences.getRemoteAccessCredential()) || !password.trim().equals(preferences.getRemoteAccessCredential().trim()))) {
            preferences.setRemoteAccessCredential(password.trim());
            app.stopServer();
            app.startServer();
        }

        mActivity.setResult(Activity.RESULT_OK);
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
            case PreferenceManager.SMS_ACTIVE:

                setPhoneNumber();
                break;
            case PreferenceManager.REMOTE_ACCESS_ACTIVE:
                boolean remoteAccessActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).isChecked();
                if (remoteAccessActive) {
                    checkRemoteAccessOnion();
                    app.startServer();
                } else {
                    app.stopServer();
                }
                break;
            case PreferenceManager.REGISTER_SIGNAL:
                String signalNum = ((EditTextPreference) findPreference(PreferenceManager.REGISTER_SIGNAL)).getText();

                if (checkValidString(signalNum)) {
                    signalNum = "+" + signalNum.trim().replaceAll("[^0-9]", "");

                    preferences.setSignalUsername(signalNum);
                    findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(signalNum);

                    resetSignal(preferences.getSignalUsername());
                    activateSignal(preferences.getSignalUsername(), null);
                } else {
                    preferences.setSignalUsername("");
                    findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.register_signal_desc);
                }
                break;
            case PreferenceManager.VERIFY_SIGNAL: {
                String text = ((EditTextPreference) findPreference(PreferenceManager.VERIFY_SIGNAL)).getText();
                activateSignal(preferences.getSignalUsername(), text);
                break;
            }
            case PreferenceManager.SMS_NUMBER:
                boolean smsActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.SMS_ACTIVE)).isChecked();
                if (smsActive && TextUtils.isEmpty(preferences.getSignalUsername())) {
                    askForPermission(Manifest.permission.SEND_SMS, 6);
                    askForPermission(Manifest.permission.READ_PHONE_STATE, 6);
                }
                setPhoneNumber();
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
                String text = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ONION)).getText();
                if (checkValidString(text)) {
                    preferences.setRemoteAccessOnion(text.trim());
                    findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
                } else {
                    preferences.setRemoteAccessOnion(text);
                    findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(R.string.remote_access_hint);
                }
                break;
            }
            case PreferenceManager.REMOTE_ACCESS_CRED: {
                String text = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_CRED)).getText();
                if (checkValidString(text)) {
                    preferences.setRemoteAccessCredential(text.trim());
                    findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.bullets);
                } else {
                    preferences.setRemoteAccessCredential(text);
                    findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.remote_access_credential_hint);
                }
                break;
            }
        }
    }

    private void setPhoneNumber() {
        boolean smsActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.SMS_ACTIVE)).isChecked();
        String phoneNumber = ((EditTextPreference) findPreference(PreferenceManager.SMS_NUMBER)).getText();
        if (smsActive && checkValidString(phoneNumber)) {
            preferences.activateSms(true);
        } else {
            preferences.activateSms(false);
        }

        if (checkValidString(phoneNumber)) {
            preferences.setSmsNumber(phoneNumber.trim());
            findPreference(PreferenceManager.SMS_NUMBER).setSummary(phoneNumber.trim());
        } else {
            preferences.setSmsNumber("");
            findPreference(PreferenceManager.SMS_NUMBER).setSummary(R.string.sms_dialog_message);
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
            mTimePickerDialog.show(mActivity.getFragmentManager(), "TimeDelayPickerDialog");
        } else {
            mTimePickerDialog.show(mActivity.getFragmentManager(), "VideoLengthPickerDialog");
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

    private void activateSignal(String username, String verifyCode) {
        SignalSender sender = SignalSender.getInstance(mActivity, username);

        if (TextUtils.isEmpty(verifyCode)) {
            sender.register();
        } else {
            sender.verify(verifyCode);
        }
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
}

package org.havenapp.main;

/**
 * Created by Anupam Das (opticod) on 29/12/17.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.service.SignalSender;
import org.havenapp.main.service.WebServer;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import info.guardianproject.netcipher.proxy.OrbotHelper;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener, TimePickerDialog.OnTimeSetListener {

    private PreferenceManager preferences;
    private HavenApp app;
    private Activity mActivity;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
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
                    findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.FRONT);
                    break;
                case PreferenceManager.BACK:
                    ((ListPreference) findPreference(PreferenceManager.CAMERA)).setValueIndex(1);
                    findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.BACK);
                    break;
                case PreferenceManager.OFF:
                    ((ListPreference) findPreference(PreferenceManager.CAMERA)).setValueIndex(2);
                    findPreference(PreferenceManager.CAMERA).setSummary(PreferenceManager.NONE);
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
            findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(preferences.getRemoteAccessCredential().trim());
        } else {
            findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.remote_access_credential_hint);
        }

        if (checkValidString(preferences.getSignalUsername())) {
            String signalNum = "+" + preferences.getSignalUsername().trim().replaceAll("[^0-9]", "");
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(signalNum);
        } else {
            findPreference(PreferenceManager.REGISTER_SIGNAL).setSummary(R.string.register_signal_desc);
        }

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
            showTimeDelayDialog();
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
        if (PreferenceManager.CAMERA.equals(key)) {
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
        } else if (PreferenceManager.SMS_ACTIVE.equals(key)) {

            setPhoneNumber();
        } else if (PreferenceManager.REMOTE_ACCESS_ACTIVE.equals(key)) {
            boolean remoteAccessActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.REMOTE_ACCESS_ACTIVE)).isChecked();
            if (remoteAccessActive) {
                checkRemoteAccessOnion();
                app.startServer();
            } else {
                app.stopServer();
            }
        } else if (PreferenceManager.REGISTER_SIGNAL.equals(key)) {
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
        } else if (PreferenceManager.VERIFY_SIGNAL.equals(key)) {
            String text = ((EditTextPreference) findPreference(PreferenceManager.VERIFY_SIGNAL)).getText();
            activateSignal(preferences.getSignalUsername(), text);
        } else if (PreferenceManager.SMS_NUMBER.equals(key)) {
            boolean smsActive = ((SwitchPreferenceCompat) findPreference(PreferenceManager.SMS_ACTIVE)).isChecked();
            if (smsActive && TextUtils.isEmpty(preferences.getSignalUsername())) {
                askForPermission(Manifest.permission.SEND_SMS, 6);
                askForPermission(Manifest.permission.READ_PHONE_STATE, 6);
            }
            setPhoneNumber();
        } else if (PreferenceManager.REMOTE_ACCESS_ONION.equals(key)) {
            String text = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_ONION)).getText();
            if (checkValidString(text)) {
                preferences.setRemoteAccessOnion(text.trim());
                findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(preferences.getRemoteAccessOnion().trim() + ":" + WebServer.LOCAL_PORT);
            } else {
                preferences.setRemoteAccessOnion(text);
                findPreference(PreferenceManager.REMOTE_ACCESS_ONION).setSummary(R.string.remote_access_hint);
            }
        } else if (PreferenceManager.REMOTE_ACCESS_CRED.equals(key)) {
            String text = ((EditTextPreference) findPreference(PreferenceManager.REMOTE_ACCESS_CRED)).getText();
            if (checkValidString(text)) {
                preferences.setRemoteAccessCredential(text.trim());
                findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(preferences.getRemoteAccessCredential().trim());
            } else {
                preferences.setRemoteAccessCredential(text);
                findPreference(PreferenceManager.REMOTE_ACCESS_CRED).setSummary(R.string.remote_access_credential_hint);
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

    private void showTimeDelayDialog() {
        int totalSecs = preferences.getTimerDelay();

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;


        TimePickerDialog mTimePickerDialog = TimePickerDialog.newInstance(this, hours, minutes, seconds, true);
        mTimePickerDialog.enableSeconds(true);
        mTimePickerDialog.show(mActivity.getFragmentManager(), "TimePickerDialog");
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
        int delaySeconds = second + minute * 60 + hourOfDay * 60 * 60;
        preferences.setTimerDelay(delaySeconds);
    }
}
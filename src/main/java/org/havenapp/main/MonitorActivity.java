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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.service.MonitorService;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraFragment;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.havenapp.main.Utils.getTimerText;

public class MonitorActivity extends FragmentActivity implements TimePickerDialog.OnTimeSetListener {

    @BindView(R.id.timer_text_title)
    TextView timerTextTitle;
    @BindView(R.id.timer_text)
    TextView txtTimer;
    @BindView(R.id.btnStartNow)
    Button btnStartNow;
    @BindView(R.id.btnStartLater)
    Button btnStartLater;
    @BindView(R.id.btnCameraSwitch)
    ImageView btnCameraSwitch;
    @BindView(R.id.btnMicSettings)
    ImageView btnMicSettings;
    @BindView(R.id.btnAccelSettings)
    ImageView btnAccelSettings;
    @BindView(R.id.btnSettings)
    ImageView btnSettings;
    @BindView(R.id.timer_container)
    LinearLayout timerContainer;
    @BindView(R.id.main_content)
    CoordinatorLayout mainContent;
    private PreferenceManager preferences = null;

    private CountDownTimer cTimer;

    private boolean mIsMonitoring = false;
    private boolean mIsInitializedLayout = false;
    private boolean mOnTimerTicking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitor);
        ButterKnife.bind(this);
        boolean permsNeeded = askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);
        if (!permsNeeded)
            initLayout();
    }

    private void initLayout() {
        preferences = new PreferenceManager(getApplicationContext());
        int timeM = preferences.getTimerDelay() * 1000;
        txtTimer.setText(getTimerText(timeM));
        mIsInitializedLayout = true;
    }

    private void switchCamera() {

        String camera = preferences.getCamera();
        if (camera.equals(PreferenceManager.FRONT))
            preferences.setCamera(PreferenceManager.BACK);
        else if (camera.equals(PreferenceManager.BACK))
            preferences.setCamera(PreferenceManager.FRONT);

        ((CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera)).resetCamera();

    }

    private void updateTimerValue(int val) {
        preferences.setTimerDelay(val);
        int valM = val * 1000;
        txtTimer.setText(getTimerText(valM));
    }

    private void doCancel() {

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;

            if (mIsMonitoring) {
                mIsMonitoring = false;
                stopService(new Intent(this, MonitorService.class));
                finish();
            } else {

                btnStartNow.setVisibility(View.VISIBLE);
                timerTextTitle.setVisibility(View.VISIBLE);

                btnStartLater.setText(R.string.start_later);

                int timeM = preferences.getTimerDelay() * 1000;
                txtTimer.setText(getTimerText(timeM));
            }
        } else {

            close();
        }
    }

    private void showSettings() {

        Intent i = new Intent(this, SettingsActivity.class);

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            startActivityForResult(i, 9999);

        } else {
            startActivity(i);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9999) {
            initTimer();
        }
    }

    private void initTimer() {
        txtTimer.setTextColor(getResources().getColor(R.color.colorAccent));
        cTimer = new CountDownTimer((preferences.getTimerDelay()) * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                mOnTimerTicking = true;
                txtTimer.setText(getTimerText(millisUntilFinished));
            }

            public void onFinish() {

                txtTimer.setText(R.string.status_on);
                initMonitor();
                mOnTimerTicking = false;
            }

        };

        cTimer.start();


    }

    private void initMonitor() {

        mIsMonitoring = true;
        //ensure folder exists and will not be scanned by the gallery app

        try {
            File fileImageDir = new File(Environment.getExternalStorageDirectory(), preferences.getImagePath());
            fileImageDir.mkdirs();
            new FileOutputStream(new File(fileImageDir, ".nomedia")).write(0);
        } catch (IOException e) {
            Log.e("Monitor", "unable to init media storage directory", e);
        }

        //Do something after 100ms
        startService(new Intent(MonitorActivity.this, MonitorService.class));

    }

    /**
     * Closes the monitor activity and unset session properties
     */
    private void close() {

        stopService(new Intent(this, MonitorService.class));
        if (preferences != null) {
            preferences.unsetAccessToken();
            preferences.unsetDelegatedAccessToken();
            preferences.unsetPhoneId();
        }
        finish();

    }

    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        close();
    }

    private void showTimeDelayDialog() {
        int totalSecs = preferences.getTimerDelay();

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        TimePickerDialog mTimePickerDialog = TimePickerDialog.newInstance(this, hours, minutes, seconds, true);
        mTimePickerDialog.enableSeconds(true);
        mTimePickerDialog.show(getFragmentManager(), "TimePickerDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsInitializedLayout && !mOnTimerTicking) {
            int totalMilliseconds = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(totalMilliseconds));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.CAMERA, 2);
                break;
            case 2:
                initLayout();
                break;
        }

    }


    private boolean askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onTimeSet(TimePickerDialog view, int hourOfDay, int minute, int second) {
        int delaySeconds = second + minute * 60 + hourOfDay * 60 * 60;
        updateTimerValue(delaySeconds);
    }

    @OnClick({R.id.timer_text_title, R.id.timer_text, R.id.btnStartNow, R.id.btnStartLater, R.id.btnCameraSwitch, R.id.btnMicSettings, R.id.btnAccelSettings, R.id.btnSettings})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.timer_text_title:
                if (cTimer == null)
                    showTimeDelayDialog();
                break;
            case R.id.timer_text:
                if (cTimer == null)
                    showTimeDelayDialog();
                break;
            case R.id.btnStartNow:
                btnStartLater.setText(R.string.action_cancel);
                btnStartNow.setVisibility(View.INVISIBLE);
                timerTextTitle.setVisibility(View.INVISIBLE);
                initTimer();
                break;
            case R.id.btnStartLater:
                doCancel();
                break;
            case R.id.btnCameraSwitch:
                switchCamera();
                break;
            case R.id.btnMicSettings:
                startActivity(new Intent(MonitorActivity.this, MicrophoneConfigureActivity.class));
                break;
            case R.id.btnAccelSettings:
                startActivity(new Intent(MonitorActivity.this, AccelConfigureActivity.class));
                break;
            case R.id.btnSettings:
                showSettings();
                break;
        }
    }
}

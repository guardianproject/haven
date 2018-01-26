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
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.service.MonitorService;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraConfigureActivity;
import org.havenapp.main.ui.CameraFragment;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.havenapp.main.Utils.getTimerText;

public class MonitorActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener {

    private PreferenceManager preferences = null;

    private TextView txtTimer;

    private CountDownTimer cTimer;

    private boolean mIsMonitoring = false;
    private boolean mIsInitializedLayout = false;
    private boolean mOnTimerTicking = false;

    private final static int REQUEST_CAMERA = 999;
    private final static int REQUEST_TIMER = 1000;

    private CameraFragment mFragmentCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean permsNeeded = askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

        if (!permsNeeded) {

            initSetupLayout();

            if (MonitorService.getInstance() != null)
                if (MonitorService.getInstance().isRunning())
                    initActiveLayout();

        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    private void initActiveLayout() {

        ((Button) findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
        findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
        findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
        txtTimer.setText(R.string.status_on);

        mOnTimerTicking = false;
        mIsMonitoring = true;
    }

    private void initSetupLayout() {
        preferences = new PreferenceManager(getApplicationContext());
        setContentView(R.layout.activity_monitor);

        txtTimer = (TextView) findViewById(R.id.timer_text);
        View viewTimer = findViewById(R.id.timer_container);

        int timeM = preferences.getTimerDelay() * 1000;

        txtTimer.setText(getTimerText(timeM));
        txtTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cTimer == null)
                    showTimeDelayDialog();

            }
        });
        findViewById(R.id.timer_text_title).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cTimer == null)
                    showTimeDelayDialog();

            }
        });

        findViewById(R.id.btnStartLater).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doCancel();
            }
        });

        findViewById(R.id.btnStartNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Button) findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
                findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
                findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
                initTimer();
            }
        });

        findViewById(R.id.btnAccelSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MonitorActivity.this, AccelConfigureActivity.class));
            }
        });

        findViewById(R.id.btnMicSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MonitorActivity.this, MicrophoneConfigureActivity.class));
            }
        });

        findViewById(R.id.btnCameraSwitch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configCamera();
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettings();
            }
        });

        mFragmentCamera =  ((CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera));

        mIsInitializedLayout = true;
    }

    private void configCamera() {

        mFragmentCamera.stopCamera();
        startActivityForResult(new Intent(this, CameraConfigureActivity.class),REQUEST_CAMERA);
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
            mOnTimerTicking = false;
        }

        if (mIsMonitoring) {
            mIsMonitoring = false;
            stopService(new Intent(this, MonitorService.class));
            finish();
        } else {

            findViewById(R.id.btnStartNow).setVisibility(View.VISIBLE);
            findViewById(R.id.timer_text_title).setVisibility(View.VISIBLE);

            ((Button) findViewById(R.id.btnStartLater)).setText(R.string.start_later);

            int timeM = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(timeM));
        }

    }

    private void showSettings() {

        Intent i = new Intent(this, SettingsActivity.class);

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            startActivityForResult(i, REQUEST_TIMER);

        } else {
            startActivity(i);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TIMER) {
            initTimer();
        }
        else if (requestCode == REQUEST_CAMERA)
        {
            mFragmentCamera.resetCamera();
        }
    }

    @Override
    protected void onDestroy() {
        if (!mIsMonitoring)
        {
            mFragmentCamera.stopCamera();
        }
        super.onDestroy();
    }

    private void initTimer() {
        txtTimer.setTextColor(getResources().getColor(R.color.colorAccent));
        cTimer = new CountDownTimer((preferences.getTimerDelay()) * 1000, 1000) {

            public void onTick(long millisUntilFinished) {
                mOnTimerTicking = true;
                txtTimer.setText(getTimerText(millisUntilFinished));
            }

            public void onFinish() {

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        if (mIsInitializedLayout && (!mOnTimerTicking) && (!mIsMonitoring)) {
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
                initSetupLayout();
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
}

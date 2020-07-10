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
import android.app.PictureInPictureParams;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.model.EventTrigger;
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

    private View mBtnCamera, mBtnMic, mBtnAccel;
    private Animation mAnimShake;
    private TextView txtStatus;

    private ProgressDialog progressDialog;

    private int lastEventType = -1;

    /**
     * Handler used to update back the UI after motion detection
     */
    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (mIsMonitoring) {

                String message = null;

                if (msg.what == EventTrigger.CAMERA) {
                    mBtnCamera.startAnimation(mAnimShake);
                    message = getString(R.string.motion_detected);

                } else if (msg.what == EventTrigger.POWER) {
                    message = getString(R.string.power_detected);
                    mBtnAccel.startAnimation(mAnimShake);

                } else if (msg.what == EventTrigger.MICROPHONE) {
                    mBtnMic.startAnimation(mAnimShake);
                    message = getString(R.string.sound_detected);


                } else if (msg.what == EventTrigger.ACCELEROMETER || msg.what == EventTrigger.BUMP) {
                    mBtnAccel.startAnimation(mAnimShake);
                    message = getString(R.string.device_move_detected);

                } else if (msg.what == EventTrigger.LIGHT) {
                    message = getString(R.string.status_light);
                    mBtnCamera.startAnimation(mAnimShake);

                }

                if (lastEventType != msg.what) {
                    if (!TextUtils.isEmpty(message))
                        txtStatus.setText(message);
                }

                lastEventType = msg.what;
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int eventType = intent.getIntExtra("type",-1);
            if (eventType == MonitorService.MSG_STOP_SELF) {
                notifyMonitoringEnded();
            } else {
                boolean detected = intent.getBooleanExtra("detected", true);
                if (detected)
                    handler.sendEmptyMessage(eventType);
            }
        }
    };

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

        txtTimer = findViewById(R.id.timer_text);

        int timeM = preferences.getTimerDelay() * 1000;

        txtTimer.setText(getTimerText(timeM));
        txtTimer.setOnClickListener(v -> {
            if (cTimer == null)
                showTimeDelayDialog();

        });
        findViewById(R.id.timer_text_title).setOnClickListener(v -> {
            if (cTimer == null)
                showTimeDelayDialog();

        });

        findViewById(R.id.btnStartLater).setOnClickListener(v -> doCancel());

        findViewById(R.id.btnStartNow).setOnClickListener(v -> {
            ((Button) findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
            findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
            findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
            initTimer();
        });

        mBtnAccel = findViewById(R.id.btnAccelSettings);
        mBtnAccel.setOnClickListener(v -> {
            if (!mIsMonitoring)
                startActivity(new Intent(MonitorActivity.this, AccelConfigureActivity.class));
        });

        mBtnMic = findViewById(R.id.btnMicSettings);
        mBtnMic.setOnClickListener(v -> {
            if (!mIsMonitoring)
                startActivity(new Intent(MonitorActivity.this, MicrophoneConfigureActivity.class));
        });

        mBtnCamera = findViewById(R.id.btnCameraSwitch);
        mBtnCamera.setOnClickListener(v -> {
            if (!mIsMonitoring)
                configCamera();
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());

        mFragmentCamera =  ((CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera));

        txtStatus = findViewById(R.id.txtStatus);

        mAnimShake = AnimationUtils.loadAnimation(this, R.anim.shake);

        mIsInitializedLayout = true;
    }

    private void configCamera() {
        startActivityForResult(new Intent(this, CameraConfigureActivity.class),REQUEST_CAMERA);
    }



    private void updateTimerValue(int val) {
        preferences.setTimerDelay(val);
        int valM = val * 1000;
        txtTimer.setText(getTimerText(valM));
    }

    private void doCancel() {

        boolean wasTimer = false;

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            mOnTimerTicking = false;
            wasTimer = true;
        }

        if (mIsMonitoring) {
            mIsMonitoring = false;
            showAlertDialog();
            mFragmentCamera.stopMonitoring();
        } else {

            findViewById(R.id.btnStartNow).setVisibility(View.VISIBLE);
            findViewById(R.id.timer_text_title).setVisibility(View.VISIBLE);

            ((Button) findViewById(R.id.btnStartLater)).setText(R.string.start_later);

            int timeM = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(timeM));

            if (!wasTimer)
                finish();
        }

    }

    @Override
    public void onPictureInPictureModeChanged (boolean isInPictureInPictureMode, Configuration newConfig) {
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            findViewById(R.id.buttonBar).setVisibility(View.GONE);
        } else {
            // Restore the full-screen UI.
            findViewById(R.id.buttonBar).setVisibility(View.VISIBLE);

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
//            mFragmentCamera.initCamera();
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
            File fileImageDir = new File(Environment.getExternalStorageDirectory(), preferences.getDefaultMediaStoragePath());
            fileImageDir.mkdirs();
            new FileOutputStream(new File(fileImageDir, ".nomedia")).write(0);
        } catch (IOException e) {
            Log.e("Monitor", "unable to init media storage directory", e);
        }

        //Do something after 100ms
        startService(new Intent(MonitorActivity.this, MonitorService.class));

    }


    @Override
    public void onUserLeaveHint () {
        if (mIsMonitoring) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
            }
        }
    }
    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {

        if (mIsMonitoring) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(new PictureInPictureParams.Builder().build());
            }
            else
            {
                finish();
            }
        }
        else
        {
            finish();
        }


    }

    private void showTimeDelayDialog() {
        int totalSecs = preferences.getTimerDelay();

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;

        TimePickerDialog mTimePickerDialog = TimePickerDialog.newInstance(this, hours, minutes, seconds, true);
        mTimePickerDialog.enableSeconds(true);
        mTimePickerDialog.show(getSupportFragmentManager(), "TimePickerDialog");
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mIsInitializedLayout && (!mOnTimerTicking) && (!mIsMonitoring)) {
            int totalMilliseconds = preferences.getTimerDelay() * 1000;
            txtTimer.setText(getTimerText(totalMilliseconds));
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction("event");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,filter );

    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

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

    private void showAlertDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.finishing_up));
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void notifyMonitoringEnded() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        finish();
    }
}

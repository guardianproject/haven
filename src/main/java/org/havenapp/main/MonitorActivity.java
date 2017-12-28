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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


import org.havenapp.main.service.MonitorService;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.CameraFragment;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

public class MonitorActivity extends FragmentActivity {
	
	private PreferenceManager preferences = null;

    private TextView txtTimer;
    private View viewTimer;

    private CountDownTimer cTimer;

    private boolean mIsMonitoring = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean permsNeeded = askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

        if (!permsNeeded)
            initLayout();
    }

    private void initLayout ()
    {
        preferences = new PreferenceManager(getApplicationContext());
        setContentView(R.layout.activity_monitor);

        txtTimer = findViewById(R.id.timer_text);
        viewTimer = findViewById(R.id.timer_container);

        int timeM = preferences.getTimerDelay()*1000;
        String timerText = String.format(Locale.getDefault(), "%02dm %02ds",
                TimeUnit.MILLISECONDS.toMinutes(timeM) % 60,
                TimeUnit.MILLISECONDS.toSeconds(timeM) % 60);

        txtTimer.setText(timerText);
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
           ((Button)findViewById(R.id.btnStartLater)).setText(R.string.action_cancel);
           findViewById(R.id.btnStartNow).setVisibility(View.INVISIBLE);
           findViewById(R.id.timer_text_title).setVisibility(View.INVISIBLE);
           initTimer();
       });

       findViewById(R.id.btnAccelSettings).setOnClickListener(v -> startActivity(new Intent(MonitorActivity.this, AccelConfigureActivity.class)));

        findViewById(R.id.btnMicSettings).setOnClickListener(v -> startActivity(new Intent(MonitorActivity.this, MicrophoneConfigureActivity.class)));

        findViewById(R.id.btnCameraSwitch).setOnClickListener(v -> switchCamera());

        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettings());



    }

	private void switchCamera ()
    {

        String camera = preferences.getCamera();
        if (camera.equals(PreferenceManager.FRONT))
            preferences.setCamera(PreferenceManager.BACK);
        else if (camera.equals(PreferenceManager.BACK))
            preferences.setCamera(PreferenceManager.FRONT);

        ((CameraFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_camera)).resetCamera();

    }

	private void updateTimerValue (int val)
    {
        preferences.setTimerDelay(val);
        int valM = val * 1000;
        String timerText = String.format(Locale.getDefault(), "%02dm %02ds",
                TimeUnit.MILLISECONDS.toMinutes(valM) % 60,
                TimeUnit.MILLISECONDS.toSeconds(valM) % 60);

        txtTimer.setText(timerText);
    }

	private void doCancel ()
    {

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;

            if (mIsMonitoring) {
                mIsMonitoring = false;
                stopService(new Intent(this, MonitorService.class));
                finish();
            }
            else {

                findViewById(R.id.btnStartNow).setVisibility(View.VISIBLE);
                findViewById(R.id.timer_text_title).setVisibility(View.VISIBLE);

                ((Button) findViewById(R.id.btnStartLater)).setText(R.string.start_later);

                int timeM = preferences.getTimerDelay() * 1000;
                String timerText = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(timeM) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(timeM) % 60);

                txtTimer.setText(timerText);
            }
        }
        else {

            close();
        }
    }

	private void showSettings ()
    {

        Intent i = new Intent(this,SettingsActivity.class);

        if (cTimer != null) {
            cTimer.cancel();
            cTimer = null;
            startActivityForResult(i,9999);

        }
        else
        {
            startActivity(i);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9999)
        {
            initTimer();
        }
    }

    private void initTimer ()
    {
        txtTimer.setTextColor(getResources().getColor(R.color.colorAccent));
        cTimer = new CountDownTimer((preferences.getTimerDelay())*1000, 1000) {

            public void onTick(long millisUntilFinished) {
                String timerText = String.format(Locale.getDefault(), "%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60);

                txtTimer.setText(timerText);
            }

            public void onFinish() {

                txtTimer.setText(R.string.status_on);
                initMonitor();
            }

        };

        cTimer.start();


    }

	private void initMonitor ()
    {

        mIsMonitoring = true;
        //ensure folder exists and will not be scanned by the gallery app

        try {
            File fileImageDir = new File(Environment.getExternalStorageDirectory(), preferences.getImagePath());
            fileImageDir.mkdirs();
            new FileOutputStream(new File(fileImageDir, ".nomedia")).write(0);
        }
        catch (IOException e){
            Log.e("Monitor","unable to init media storage directory",e);
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

    private void showTimeDelayDialog ()
    {
        int totalSecs = preferences.getTimerDelay();

        int hours = totalSecs / 3600;
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;


        final NumberPicker pickerMinutes = new NumberPicker(this);
        pickerMinutes.setMinValue(0);
        pickerMinutes.setMaxValue(59);
        pickerMinutes.setValue(minutes);

        final NumberPicker pickerSeconds = new NumberPicker(this);
        pickerSeconds.setMinValue(0);
        pickerSeconds.setMaxValue(59);
        pickerSeconds.setValue(seconds);

        final TextView textViewMinutes = new TextView(this);
        textViewMinutes.setText("m");
        textViewMinutes.setTextSize(30);
        textViewMinutes.setGravity(Gravity.CENTER_VERTICAL);

        final TextView textViewSeconds = new TextView(this);
        textViewSeconds.setText("s");
        textViewSeconds.setTextSize(30);
        textViewSeconds.setGravity(Gravity.CENTER_VERTICAL);


        final LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(pickerMinutes, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT));

        layout.addView(textViewMinutes, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Gravity.LEFT|Gravity.BOTTOM));

        layout.addView(pickerSeconds, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Gravity.LEFT));

        layout.addView(textViewSeconds, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                Gravity.LEFT|Gravity.BOTTOM));


        new AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                    // do something with picker.getValue()
                    int delaySeconds = pickerSeconds.getValue() + (pickerMinutes.getValue() * 60);
                    updateTimerValue (delaySeconds);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.CAMERA,2);
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

}

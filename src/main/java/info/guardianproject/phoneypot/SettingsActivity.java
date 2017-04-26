
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package info.guardianproject.phoneypot;


import java.io.File;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class SettingsActivity extends AppCompatActivity {
	
	private PreferenceManager preferences = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_start);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = new PreferenceManager(this.getApplicationContext());
        
        /*
         * We create an application directory to store images and audio
         */
        File directory = new File(Environment.getExternalStorageDirectory()+preferences.getDirPath());
        directory.mkdirs();
        
        /**
         * Checkboxes for enabled app options
         */
        final CheckBox smsCheck = (CheckBox) this.findViewById(R.id.sms_check);
        
        /*
         * Detecting if the device has a front camera
         * and configuring the spinner of camera selection
         * properly 
         */
        final Spinner selectCameraSpinner = (Spinner) this.findViewById(R.id.camera_spinner);
        PackageManager pm = getPackageManager();
        boolean frontCam;
        
        frontCam = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
        if (!frontCam) {
        	selectCameraSpinner.setEnabled(false);
        }
        
        /*
         * Detecting if the device has the flash 
         * and configuring properly the check box
         */


        final Spinner accelerometerSensitivity = (Spinner) 
        		this.findViewById(R.id.accelerometer_sensitivity_spinner);
        final Spinner cameraSensitivity = (Spinner) 
        		this.findViewById(R.id.camera_sensitivity_spinner);
        final Spinner microphoneSensitivity = (Spinner) 
        		this.findViewById(R.id.microphone_sensitivity_spinner);
        
        final EditText phoneNumber = (EditText)
        		this.findViewById(R.id.phone_number);

        smsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
                    askForPermission(Manifest.permission.SEND_SMS,6);
        		}
			}
		});

        /**
         * Loads preferences and sets view
         */
        if (preferences.getAccelerometerActivation()) {
        	String sensitivity = preferences.getAccelerometerSensitivity();
        	if (sensitivity.equals(PreferenceManager.LOW))
        		accelerometerSensitivity.setSelection(0);
        	else if (sensitivity.equals(PreferenceManager.MEDIUM))
        		accelerometerSensitivity.setSelection(1);
        	else if (sensitivity.equals(PreferenceManager.HIGH))
        		accelerometerSensitivity.setSelection(2);
        }
        if (preferences.getCameraActivation()) {
        	String sensitivity = preferences.getCameraSensitivity();
        	if (sensitivity.equals(PreferenceManager.LOW))
        		cameraSensitivity.setSelection(0);
        	else if (sensitivity.equals(PreferenceManager.MEDIUM))
        		cameraSensitivity.setSelection(1);
        	else if (sensitivity.equals(PreferenceManager.HIGH))
        		cameraSensitivity.setSelection(2);
        	String camera = preferences.getCamera();
        	if (camera.equals(PreferenceManager.FRONT))
        		selectCameraSpinner.setSelection(0);
        	else
        		selectCameraSpinner.setSelection(1);
        }
        if (preferences.getMicrophoneActivation()) {
        	String sensitivity = preferences.getMicrophoneSensitivity();
        	if (sensitivity.equals(PreferenceManager.LOW))
        		microphoneSensitivity.setSelection(0);
        	else if (sensitivity.equals(PreferenceManager.MEDIUM))
        		microphoneSensitivity.setSelection(1);
        	else if (sensitivity.equals(PreferenceManager.HIGH))
        		microphoneSensitivity.setSelection(2);
        }
        if (preferences.getSmsActivation()) {
        	smsCheck.setChecked(true);
        	phoneNumber.setText(preferences.getSmsNumber());
        }

		askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		switch (requestCode) {
            case 1:
                askForPermission(Manifest.permission.CAMERA,2);
                break;
			case 2:
				askForPermission(Manifest.permission.RECORD_AUDIO,3);
				break;

		}

	}


	private void askForPermission(String permission, Integer requestCode) {
		if (ContextCompat.checkSelfPermission(SettingsActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(SettingsActivity.this, permission)) {

				//This is called if user has denied the permission before
				//In this case I am just asking the permission again
				ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{permission}, requestCode);

			} else {

				ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{permission}, requestCode);
			}
		} else {
		}
	}

    private void startMonitoring ()
    {

        Spinner accelerometerSensitivity = (Spinner)
                this.findViewById(R.id.accelerometer_sensitivity_spinner);
        Spinner cameraSensitivity = (Spinner)
                this.findViewById(R.id.camera_sensitivity_spinner);
        Spinner microphoneSensitivity = (Spinner)
                this.findViewById(R.id.microphone_sensitivity_spinner);

        EditText phoneNumber = (EditText)
                this.findViewById(R.id.phone_number);

        Spinner selectCameraSpinner = (Spinner) this.findViewById(R.id.camera_spinner);
        CheckBox smsCheck = (CheckBox) this.findViewById(R.id.sms_check);


        preferences.activateAccelerometer(true);
        preferences.setAccelerometerSensitivity(
                (String)accelerometerSensitivity.getSelectedItem());

        preferences.activateCamera(true);
        preferences.setCamera(
                (String)selectCameraSpinner.getSelectedItem());
        preferences.setCameraSensitivity(
                (String)cameraSensitivity.getSelectedItem());


        preferences.activateMicrophone(true);
        preferences.setMicrophoneSensitivity(
                (String)microphoneSensitivity.getSelectedItem());

        if (smsCheck.isChecked() && !phoneNumber.getText().toString().equals("")) {
            Log.i("StartActivity", "Send message alert is active");
            preferences.activateSms(true);
            preferences.setSmsNumber(
                    phoneNumber.getText().toString());
        } else {
            preferences.activateSms(false);
        }

        launchMonitoringMode();

    }
    private void launchMonitoringMode ()
    {

        Intent intent = new Intent(
                SettingsActivity.this,
                MonitorActivity.class);

        startActivity(intent);

        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monitor_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_start:
                startMonitoring();
                break;
        }
        return true;
    }

}

/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package info.guardianproject.phoneypot;


import java.io.File;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class StartActivity extends Activity {
	
	private PreferenceManager preferences = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_start);
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

        final Button startButton = (Button) this.findViewById(R.id.start_button);


        smsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
                    askForPermission(Manifest.permission.SEND_SMS,6);
        		}
			}
		});
        
        startButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {

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

                AlertDialog.Builder alert = new AlertDialog.Builder(StartActivity.this);
                final EditText input = new EditText(StartActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                alert.setView(input);
                alert.setTitle(R.string.unlock_title);
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        preferences.setUnlockCode(input.getText().toString());
                        startMonitoring();
                    }
                });
                alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {


                    }
                });
                alert.show();
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
		if (ContextCompat.checkSelfPermission(StartActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(StartActivity.this, permission)) {

				//This is called if user has denied the permission before
				//In this case I am just asking the permission again
				ActivityCompat.requestPermissions(StartActivity.this, new String[]{permission}, requestCode);

			} else {

				ActivityCompat.requestPermissions(StartActivity.this, new String[]{permission}, requestCode);
			}
		} else {
		}
	}

    private void startMonitoring ()
    {

        Intent intent = new Intent(
                StartActivity.this,
                MonitorActivity.class);

        StartActivity.this.startActivity(intent);
    }

}

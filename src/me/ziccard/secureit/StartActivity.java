package me.ziccard.secureit;

import me.ziccard.secureit.async.AuthenticatorTask;
import me.ziccard.secureit.async.BluetoothServerTask;
import me.ziccard.secureit.async.BluetoothServerTask.NoBluetoothException;
import me.ziccard.secureit.async.MicRecorderTask;
import me.ziccard.secureit.async.MicRecorderTaskFactory;
import me.ziccard.secureit.async.MicRecorderTaskFactory.RecordLimitExceeded;
import android.os.Bundle;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Spinner;

public class StartActivity extends Activity {
	
	private SecureItPreferences preferences = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //TODO ELIMINARE SOLO PROVA
        try {
			MicRecorderTaskFactory.makeRecorder().start();
		} catch (RecordLimitExceeded e) {
			// WE HAVE AN UpDATED RECORD
			e.printStackTrace();
		}
        
        setContentView(R.layout.activity_start);
        preferences = new SecureItPreferences(this.getApplicationContext());
        
        /**
         * Checkboxes for enabled app options
         */
        final CheckBox accelerometerCheck = (CheckBox) this.findViewById(R.id.accelerometer_check);
        final CheckBox cameraCheck = (CheckBox) this.findViewById(R.id.camera_check);
        final CheckBox microphoneCheck = (CheckBox) this.findViewById(R.id.microphone_check);
        final CheckBox smsCheck = (CheckBox) this.findViewById(R.id.sms_check);
        final CheckBox remoteCheck = (CheckBox) this.findViewById(R.id.remote_check);
        
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
        final CheckBox flashCheck = (CheckBox) findViewById(R.id.flash_check);
        if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
        	flashCheck.setEnabled(false);
        }
        
        final RelativeLayout accelerometerOptions = (RelativeLayout) this.findViewById(R.id.accelerometer_options);
        final RelativeLayout cameraOptions = (RelativeLayout) this.findViewById(R.id.camera_options);
        final RelativeLayout microphoneOptions = (RelativeLayout) this.findViewById(R.id.microphone_options);
        final RelativeLayout smsOptions = (RelativeLayout) this.findViewById(R.id.sms_options);
        final RelativeLayout remoteOptions = (RelativeLayout) this.findViewById(R.id.remote_options);
        
        final Spinner accelerometerSensitivity = (Spinner) 
        		this.findViewById(R.id.accelerometer_sensitivity_spinner);
        final Spinner cameraSensitivity = (Spinner) 
        		this.findViewById(R.id.camera_sensitivity_spinner);
        final Spinner microphoneSensitivity = (Spinner) 
        		this.findViewById(R.id.microphone_sensitivity_spinner);
        
        final EditText phoneNumber = (EditText)
        		this.findViewById(R.id.phone_number);
        final EditText email = (EditText)
        		this.findViewById(R.id.email);
        final EditText password = (EditText)
        		this.findViewById(R.id.password);
        
        
        final Button startButton = (Button) this.findViewById(R.id.start_button);
        
        accelerometerCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					accelerometerOptions.setVisibility(View.VISIBLE);
				} else {
					accelerometerOptions.setVisibility(View.GONE);
				}				
			}
		});
        cameraCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					cameraOptions.setVisibility(View.VISIBLE);
				} else {
					cameraOptions.setVisibility(View.GONE);
				}				
			}
		});
        microphoneCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					microphoneOptions.setVisibility(View.VISIBLE);
				} else {
					microphoneOptions.setVisibility(View.GONE);
				}				
			}
		});
        smsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					smsOptions.setVisibility(View.VISIBLE);
				} else {
					smsOptions.setVisibility(View.GONE);
				}				
			}
		});
        remoteCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					remoteOptions.setVisibility(View.VISIBLE);
				} else {
					remoteOptions.setVisibility(View.GONE);
				}				
			}
		});
        
        startButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				
				if (accelerometerCheck.isChecked()) {
					preferences.activateAccelerometer(true);
					preferences.setAccelerometerSensitivity(
							(String)accelerometerSensitivity.getSelectedItem());
				} else {
					preferences.activateAccelerometer(false);
				}
				if (cameraCheck.isChecked()) {
					preferences.activateCamera(true);
					preferences.activateFlash(
							flashCheck.isChecked());
					preferences.setCamera(
							(String)selectCameraSpinner.getSelectedItem());
					preferences.setCameraSensitivity(
							(String)cameraSensitivity.getSelectedItem());
				} else {
					preferences.activateCamera(false);
				}
				if (microphoneCheck.isChecked()) {
					preferences.activateMicrophone(true);
					preferences.setMicrophoneSensitivity(
							(String)microphoneSensitivity.getSelectedItem());
				} else {
					preferences.activateMicrophone(false);
				}
				if (smsCheck.isChecked()) {
					preferences.activateSms(true);
					preferences.setSmsNumber(
							phoneNumber.getText().toString());
				} else {
					preferences.activateSms(false);
				}
				if (remoteCheck.isChecked()) {
					preferences.activateRemote(true);
					preferences.setRemoteEmail(
							email.getText().toString());					
				} else {
					preferences.activateRemote(false);
				}
				
				if (preferences.getRemoteActivation()) {
					AuthenticatorTask task = new AuthenticatorTask(StartActivity.this,
							preferences.getRemoteEmail(),
							password.getText().toString());
					task.execute();
				} else {
					Intent intent = new Intent(
							StartActivity.this,
							MonitorActivity.class);
					
					StartActivity.this.startActivity(intent);
				}
				
				
			}
		});
        
        /**
         * Loads preferences and sets view
         */
        if (preferences.getAccelerometerActivation()) {
        	accelerometerCheck.setChecked(true);
        	String sensitivity = preferences.getAccelerometerSensitivity();
        	if (sensitivity.equals(SecureItPreferences.LOW))
        		accelerometerSensitivity.setSelection(0);
        	else if (sensitivity.equals(SecureItPreferences.MEDIUM))
        		accelerometerSensitivity.setSelection(1);
        	else if (sensitivity.equals(SecureItPreferences.HIGH))
        		accelerometerSensitivity.setSelection(2);
        }
        if (preferences.getCameraActivation()) {
        	cameraCheck.setChecked(true);
        	String sensitivity = preferences.getCameraSensitivity();
        	if (sensitivity.equals(SecureItPreferences.LOW))
        		cameraSensitivity.setSelection(0);
        	else if (sensitivity.equals(SecureItPreferences.MEDIUM))
        		cameraSensitivity.setSelection(1);
        	else if (sensitivity.equals(SecureItPreferences.HIGH))
        		cameraSensitivity.setSelection(2);
        	flashCheck.setChecked(preferences.getFlashActivation());
        	String camera = preferences.getCamera();
        	if (camera.equals(SecureItPreferences.FRONT))
        		selectCameraSpinner.setSelection(0);
        	else
        		selectCameraSpinner.setSelection(1);
        }
        if (preferences.getMicrophoneActivation()) {
        	microphoneCheck.setChecked(true);
        	String sensitivity = preferences.getMicrophoneSensitivity();
        	if (sensitivity.equals(SecureItPreferences.LOW))
        		microphoneSensitivity.setSelection(0);
        	else if (sensitivity.equals(SecureItPreferences.MEDIUM))
        		microphoneSensitivity.setSelection(1);
        	else if (sensitivity.equals(SecureItPreferences.HIGH))
        		microphoneSensitivity.setSelection(2);
        }
        if (preferences.getSmsActivation()) {
        	smsCheck.setChecked(true);
        	phoneNumber.setText(preferences.getSmsNumber());
        }
        if (preferences.getRemoteActivation()) {
        	remoteCheck.setChecked(true);
        	email.setText(preferences.getRemoteEmail());
        }
    }
}

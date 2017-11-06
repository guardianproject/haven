
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
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.Toast;


import info.guardianproject.netcipher.proxy.OrbotHelper;
import info.guardianproject.phoneypot.service.SignalSender;
import info.guardianproject.phoneypot.service.WebServer;
import info.guardianproject.phoneypot.ui.MicrophoneConfigureActivity;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class SettingsActivity extends AppCompatActivity {
	
	private PreferenceManager preferences = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_settings);

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
        final CheckBox remoteAccessCheck = (CheckBox) this.findViewById(R.id.remote_access_check);


        final EditText phoneNumber = (EditText)
        		this.findViewById(R.id.phone_number);

        final EditText remoteAccessOnion = (EditText)
                this.findViewById(R.id.remote_access_onion);

        final ActualNumberPicker timerDelay = (ActualNumberPicker)
                this.findViewById(R.id.timer_delay);

        smsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
                    askForPermission(Manifest.permission.SEND_SMS,6);
        		}
			}
		});

        remoteAccessCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    checkRemoteAccessOnion();
                }
            }
        });


        if (preferences.getCameraActivation()) {

            String camera = preferences.getCamera();
        	if (camera.equals(PreferenceManager.FRONT))
                ((RadioButton)findViewById(R.id.radio_camera_front)).setChecked(true);
        	else if (camera.equals(PreferenceManager.BACK))
                ((RadioButton)findViewById(R.id.radio_camera_back)).setChecked(true);
            else if (camera.equals(PreferenceManager.OFF))
                ((RadioButton)findViewById(R.id.radio_camera_none)).setChecked(true);

        }

        if (preferences.getSmsActivation()) {
        	smsCheck.setChecked(true);
        	phoneNumber.setText(preferences.getSmsNumber());
        }

        if (preferences.getRemoteAccessActive())
        {
            remoteAccessCheck.setChecked(true);
            remoteAccessOnion.setText(preferences.getRemoteAccessOnion() + ":" + WebServer.LOCAL_PORT);

        }

        timerDelay.setMaxValue(600);
        timerDelay.setMinValue(0);
        timerDelay.setValue(preferences.getTimerDelay());

        findViewById(R.id.action_activate_signal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateSignal();

            }
        });

        findViewById(R.id.action_configure_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, MicrophoneConfigureActivity.class));
            }
        });

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

    private void save ()
    {

        EditText phoneNumber = (EditText)
                this.findViewById(R.id.phone_number);

        ActualNumberPicker timerDelay = (ActualNumberPicker)
                this.findViewById(R.id.timer_delay);

        CheckBox smsCheck = (CheckBox) this.findViewById(R.id.sms_check);
        CheckBox remoteAccessCheck = (CheckBox) this.findViewById(R.id.remote_access_check);

        preferences.activateAccelerometer(true);

        preferences.activateCamera(true);


        preferences.activateMicrophone(true);

        if (smsCheck.isChecked() && !phoneNumber.getText().toString().equals("")) {
            Log.i("StartActivity", "Send message alert is active");
            preferences.activateSms(true);
            preferences.setSmsNumber(
                    phoneNumber.getText().toString());
        } else {
            preferences.activateSms(false);
        }

        preferences.activateRemoteAccess(remoteAccessCheck.isChecked());

        preferences.setTimerDelay(timerDelay.getValue());

        setResult(RESULT_OK);

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
            case R.id.menu_save:
                save();
                break;
        }
        return true;
    }

    private void checkRemoteAccessOnion ()
    {
        if (OrbotHelper.isOrbotInstalled(this))
        {
            OrbotHelper.requestStartTor(this);

            if (TextUtils.isEmpty(preferences.getRemoteAccessOnion()))
                OrbotHelper.requestHiddenServiceOnPort(this, WebServer.LOCAL_PORT);
        }
        else
        {
            Toast.makeText(this,"This feature requires the Orbot: Tor for Android app to be installed.",Toast.LENGTH_LONG);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null)
        {
            String onionHost = data.getStringExtra("hs_host");

            if (!TextUtils.isEmpty(onionHost)) {
                preferences.setRemoteAccessOnion(onionHost);
                final EditText remoteAccessOnion = (EditText)
                        this.findViewById(R.id.remote_access_onion);
                remoteAccessOnion.setText(onionHost + ":" + WebServer.LOCAL_PORT);
            }

        }
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.radio_camera_back:
                if (checked)
                    preferences.setCamera(PreferenceManager.BACK);
                    break;
            case R.id.radio_camera_front:
                if (checked)
                    preferences.setCamera(PreferenceManager.FRONT);
                break;
            case R.id.radio_camera_none:
                if (checked)
                    preferences.setCamera(PreferenceManager.OFF);
                break;

        }
    }

    private void activateSignal ()
    {
        SignalSender.getInstance(this).register("+17185697272");
      //  SignalSender.getInstance(this).verify("+17185697272","484177");

    }
}


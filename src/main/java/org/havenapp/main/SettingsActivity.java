
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


import java.io.File;
import java.util.ArrayList;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;


import info.guardianproject.netcipher.proxy.OrbotHelper;

import org.havenapp.main.service.SignalSender;
import org.havenapp.main.service.WebServer;
import org.havenapp.main.ui.AccelConfigureActivity;
import org.havenapp.main.ui.MicrophoneConfigureActivity;

public class SettingsActivity extends AppCompatActivity {
	
	private PreferenceManager preferences = null;
    private HavenApp app = null;
    private EditText remoteAccessCredential;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        preferences = new PreferenceManager(this.getApplicationContext());

        app = (HavenApp)getApplication();

        /*
         * We create an application directory to store images and audio
         */
        File directory = new File(Environment.getExternalStorageDirectory() + preferences.getDirPath());
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

        remoteAccessCredential = (EditText)
                this.findViewById(R.id.remote_access_credential);


        smsCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked && TextUtils.isEmpty(preferences.getSignalUsername())) {
                    askForPermission(Manifest.permission.SEND_SMS, 6);
                    askForPermission(Manifest.permission.READ_PHONE_STATE,6);
                }
            }
        });

        remoteAccessCheck.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    checkRemoteAccessOnion();
                    app.startServer();
                }
                else
                {
                    app.stopServer();
                }
            }
        });

        if (preferences.getCameraActivation()) {

            String camera = preferences.getCamera();
            if (camera.equals(PreferenceManager.FRONT))
                ((RadioButton) findViewById(R.id.radio_camera_front)).setChecked(true);
            else if (camera.equals(PreferenceManager.BACK))
                ((RadioButton) findViewById(R.id.radio_camera_back)).setChecked(true);
            else if (camera.equals(PreferenceManager.OFF))
                ((RadioButton) findViewById(R.id.radio_camera_none)).setChecked(true);

        }

        if (preferences.getSmsActivation()) {
            smsCheck.setChecked(true);
            phoneNumber.setText(preferences.getSmsNumber());
        }

        if (preferences.getRemoteAccessActive()) {
            remoteAccessCheck.setChecked(true);
            remoteAccessOnion.setText(preferences.getRemoteAccessOnion() + ":" + WebServer.LOCAL_PORT);
        }

        if (!TextUtils.isEmpty(preferences.getRemoteAccessCredential()))
        {
            ((EditText)findViewById(R.id.remote_access_credential)).setText(preferences.getRemoteAccessCredential());
        }

        findViewById(R.id.action_register_signal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerSignalPrompt();
            }
        });

        findViewById(R.id.action_verify_signal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifySignalPrompt();
            }
        });

        checkSignalUsername();

        findViewById(R.id.action_configure_mic).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, MicrophoneConfigureActivity.class));
            }
        });

        findViewById(R.id.action_configure_accel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SettingsActivity.this, AccelConfigureActivity.class));
            }
        });



        findViewById(R.id.action_configure_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimeDelayDialog();
            }
        });

		askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

	}

	private void checkSignalUsername ()
    {
        if (preferences.getSignalUsername() != null)
        {
            TextView tv = (TextView)findViewById(R.id.label_signal_status);
            tv.setText("Current Signal Number: " + preferences.getSignalUsername());

            Button btnTestSignal = (Button)findViewById(R.id.action_test_signal);
            btnTestSignal.setVisibility(View.VISIBLE);
            btnTestSignal.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendTestSignal();
                }
            });
        }
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

        String password = remoteAccessCredential.getText().toString();
        if (TextUtils.isEmpty(preferences.getRemoteAccessCredential())
                || (!password.equals(preferences.getRemoteAccessCredential()))) {
            preferences.setRemoteAccessCredential(password);
            app.stopServer();
            app.startServer();
        }

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

    private void verifySignalPrompt ()
    {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //number of code
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        // add a button
        builder.setPositiveButton(R.string.verify, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activateSignal(preferences.getSignalUsername(),input.getText().toString());
            }
        });
        // add a button
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // create and show the alert dialog
        AlertDialog dialog = builder.create();

        dialog.setTitle(getString(R.string.verify_signal));
        dialog.setMessage(getString(R.string.enter_verification));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(6,6,6,6);
        input.setLayoutParams(lp);
        dialog.setView(input); // uncomment this line

        dialog.show();

    }

    private void registerSignalPrompt ()
    {
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //number of code
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        input.setLines(1);
        input.setHint(R.string.phone_hint);

        // add a button
        builder.setPositiveButton(R.string.register, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String signalNum = input.getText().toString();
                signalNum = "+" + signalNum.replaceAll("[^0-9]", "");

                preferences.setSignalUsername(signalNum);
                resetSignal(preferences.getSignalUsername());
                activateSignal(preferences.getSignalUsername(),null);
                checkSignalUsername();
            }
        });

        // add a button
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        // create and show the alert dialog
        AlertDialog dialog = builder.create();

        dialog.setTitle(getString(R.string.register_title));
        dialog.setMessage(getString(R.string.register_signal_desc));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(6,6,6,6);
        input.setLayoutParams(lp);
        dialog.setView(input); // uncomment this line

        dialog.show();

    }

    private void resetSignal (String username)
    {
        SignalSender sender =SignalSender.getInstance(this, username);
        sender.reset();
    }

    private void activateSignal (String username, String verifyCode)
    {
        SignalSender sender =SignalSender.getInstance(this, username);

        if (TextUtils.isEmpty(verifyCode))
            sender.register();
        else
            sender.verify(verifyCode);

    }

    private void sendTestSignal ()
    {

        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //number of code
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);

        if (!TextUtils.isEmpty(preferences.getSmsNumber()))
        {
            input.setText(preferences.getSmsNumber());
        }

        // add a button
        builder.setPositiveButton("Send Test", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SignalSender sender =SignalSender.getInstance(SettingsActivity.this, preferences.getSignalUsername());
                ArrayList<String> recip = new ArrayList<>();
                recip.add(input.getText().toString());
                sender.sendMessage(recip,getString(R.string.signal_test_message),null);
            }
        });

        // add a button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        // create and show the alert dialog
        AlertDialog dialog = builder.create();

        dialog.setTitle("Send Test Signal");
        dialog.setMessage("Enter a phone number (+12125551212) to send a test message to");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(6,6,6,6);
        input.setLayoutParams(lp);
        dialog.setView(input); // uncomment this line

        dialog.show();
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


        new android.app.AlertDialog.Builder(this)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do something with picker.getValue()
                        int delaySeconds = pickerSeconds.getValue() + (pickerMinutes.getValue() * 60);
                        preferences.setTimerDelay(delaySeconds);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}


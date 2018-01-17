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
package org.havenapp.main.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.SettingsActivity;
import org.havenapp.main.service.MonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class CameraConfigureActivity extends AppCompatActivity {
	
    private PreferenceManager preferences = null;

    private boolean mIsMonitoring = false;
    private boolean mIsInitializedLayout = false;

    private CameraFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean permsNeeded = askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

        if (!permsNeeded)
            initLayout();
    }

    private void initLayout() {
        preferences = new PreferenceManager(getApplicationContext());
        setContentView(R.layout.activity_camera_configure);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera);

        findViewById(R.id.btnCameraSwitch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        SeekBar sBar = ((SeekBar)findViewById(R.id.seekCameraSensitivity));
        sBar.setProgress(preferences.getCameraSensitivity());
        sBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                mFragment.setMotionSensitivity(i);
                preferences.setCameraSensitivity(i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

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
        startService(new Intent(CameraConfigureActivity.this, MonitorService.class));

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

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                close();
                break;
        }
        return true;
    }

    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        close();
    }

    @Override
    public void onResume() {
        super.onResume();
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

}

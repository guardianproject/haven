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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

import me.angrybyte.numberpicker.listener.OnValueChangeListener;
import me.angrybyte.numberpicker.view.ActualNumberPicker;


public class CameraConfigureActivity extends AppCompatActivity {

    private PreferenceManager mPrefManager = null;

    private boolean mIsMonitoring = false;
    private boolean mIsInitializedLayout = false;

    private CameraFragment mFragment;
    private ActualNumberPicker mNumberTrigger;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean permsNeeded = askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 1);

        if (!permsNeeded)
            initLayout();
    }

    private void initLayout() {
        mPrefManager = new PreferenceManager(getApplicationContext());
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

        mNumberTrigger = findViewById(R.id.number_trigger_level);
        mNumberTrigger.setValue(mPrefManager.getCameraSensitivity());

        mNumberTrigger.setListener(new OnValueChangeListener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                mFragment.setMotionSensitivity(newValue);
                mPrefManager.setCameraSensitivity(newValue);
                setResult(RESULT_OK);
            }
        });
        mIsInitializedLayout = true;
    }

    private void switchCamera() {

        String camera = mPrefManager.getCamera();
        if (camera.equals(PreferenceManager.FRONT))
            mPrefManager.setCamera(PreferenceManager.BACK);
        else if (camera.equals(PreferenceManager.BACK))
            mPrefManager.setCamera(PreferenceManager.FRONT);

        ((CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera)).resetCamera();
        setResult(RESULT_OK);
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mFragment.stopCamera();
                finish();
                break;
        }
        return true;
    }


    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        mFragment.stopCamera();
        finish();
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


/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.hardware.SensorEvent;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.cameraview.CameraView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.motion.CameraViewHolder;

public final class CameraFragment extends Fragment {

    private CameraViewHolder cameraViewHolder;
    private ImageView newImage;
    private PreferenceManager prefs;
    private TextView txtCameraStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.camera_fragment, container, false);

        newImage = view.findViewById(R.id.new_image);
        txtCameraStatus = view.findViewById(R.id.camera_status_display);

        return view;

    }

    public void setMotionSensitivity (int threshold)
    {
        cameraViewHolder.setMotionSensitivity(threshold);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new PreferenceManager(getContext());
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
       // if (cameraViewHolder == null)
            initCamera();


        cameraViewHolder.setMotionSensitivity(prefs.getCameraSensitivity());
    }

    public void updateCamera ()
    {
        if (cameraViewHolder != null) {
            cameraViewHolder.updateCamera();
        }
    }

    public void stopCamera ()
    {
        if (cameraViewHolder != null) {
            cameraViewHolder.stopCamera();
        }
    }

    /**
    public void resetCamera ()
    {
        stopCamera();
        initCamera();
    }**/

    public void initCamera ()
    {


        PreferenceManager prefs = new PreferenceManager(getActivity());

        if (prefs.getCameraActivation()) {
            //Uncomment to see the camera

            CameraView cameraView = getActivity().findViewById(R.id.camera_view);

            if (cameraViewHolder == null) {
                cameraViewHolder = new CameraViewHolder(getActivity(), cameraView);

                cameraViewHolder.addListener((oldBitmap, newBitmap, rawBitmap, motionDetected) -> {
                    if (motionDetected)
                        newImage.setImageBitmap(newBitmap);
                    else
                        newImage.setImageResource(R.drawable.blankimage);

                    if (txtCameraStatus != null) {
                        if (cameraViewHolder.doingVideoProcessing()) {
                            txtCameraStatus.setText("Recording...");
                        } else {
                            txtCameraStatus.setText("");
                        }
                    }

                });
            }

        }


        cameraViewHolder.startCamera();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (cameraViewHolder != null)
            cameraViewHolder.destroy();

    }

    public void onSensorChanged(SensorEvent event) {

    }
}
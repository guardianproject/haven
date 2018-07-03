
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
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.cameraview.CameraView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.media.ImageCodec;
import org.havenapp.main.sensors.motion.Preview;

public final class CameraFragment extends Fragment {

    private Preview cameraViewHolder;
    private ImageView newImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.camera_fragment, container, false);

    }

    public void setMotionSensitivity (int threshold)
    {
        cameraViewHolder.setMotionSensitivity(threshold);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (cameraViewHolder == null)
            initCamera();
        else
            resetCamera();
    }

    public void stopCamera ()
    {
        if (cameraViewHolder != null) {
            cameraViewHolder.stopCamera();
            cameraViewHolder = null;
        }
    }

    public void resetCamera ()
    {
        stopCamera();
        initCamera();
    }

    private void initCamera ()
    {

        PreferenceManager prefs = new PreferenceManager(getActivity());

        if (prefs.getCameraActivation()) {
            //Uncomment to see the camera

            CameraView cameraView = getActivity().findViewById(R.id.camera_view);
            cameraViewHolder = new Preview(getActivity(),cameraView);

            newImage = getActivity().findViewById(R.id.new_image);

            cameraViewHolder.addListener((oldBitmap, newBitmap, rawBitmap, motionDetected) -> {
                if (motionDetected)
                    newImage.setImageBitmap(newBitmap);
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onSensorChanged(SensorEvent event) {

    }
}
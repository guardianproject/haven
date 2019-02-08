
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.graphics.Bitmap;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.otaliastudios.cameraview.Audio;
import com.otaliastudios.cameraview.CameraView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

import androidx.fragment.app.Fragment;

public final class CameraFragment extends Fragment {

    private CameraViewHolder cameraViewHolder;
    private ImageView newImage;
    private PreferenceManager prefs;
    private TextView txtCameraStatus;
    private Bitmap lastBitmap;

    /**
     * Handler used to update back the UI after motion detection
     */
    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (!isDetached()) {
                if (txtCameraStatus != null) {

                    if (msg.what == 0) {
                        //                newImage.setImageResource(R.drawable.blankimage);
                        txtCameraStatus.setText("");

                    } else if (msg.what == 1) {
                        //               newImage.setImageBitmap(lastBitmap);
                        txtCameraStatus.setText(getString(R.string.motion_detected));

                    }


                    /**
                    if (cameraViewHolder.doingVideoProcessing()) {
                        txtCameraStatus.setText("Recording...");
                    } else {
                        txtCameraStatus.setText("");
                    }**/
                }
            }
        }
    };

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

    public void initCamera ()
    {


        PreferenceManager prefs = new PreferenceManager(getActivity());

        if (prefs.getCameraActivation()) {
            //Uncomment to see the camera

            CameraView cameraView = getActivity().findViewById(R.id.camera_view);
            cameraView.setAudio(Audio.OFF);

            if (cameraViewHolder == null) {
                cameraViewHolder = new CameraViewHolder(getActivity(), cameraView);

                cameraViewHolder.addListener((newBitmap, rawBitmap, motionDetected) -> {

                  lastBitmap = rawBitmap;

                  handler.sendEmptyMessage(motionDetected?1:0);


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
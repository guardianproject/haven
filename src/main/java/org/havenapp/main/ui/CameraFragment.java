
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import org.havenapp.main.model.EventTrigger;

import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public final class CameraFragment extends Fragment {

    private CameraViewHolder cameraViewHolder;
    private ImageView newImage;
    private PreferenceManager prefs;
    private TextView txtCameraStatus;

    private boolean isAttached = false;

    /**
     * Handler used to update back the UI after motion detection
     */
    private final Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (isAttached) {
                if (txtCameraStatus != null) {

                    if (msg.what == EventTrigger.CAMERA) {
                         if (cameraViewHolder.doingVideoProcessing()) {
                             txtCameraStatus.setText(getString(R.string.motion_detected)
                                     + "\n" + getString(R.string.status_recording_video));
                         } else {
                             txtCameraStatus.setText(getString(R.string.motion_detected));
                         }
                    }
                    else if (msg.what == EventTrigger.POWER) {
                        txtCameraStatus.setText(getString(R.string.power_detected));
                    }
                    else if (msg.what == EventTrigger.MICROPHONE) {
                        txtCameraStatus.setText(getString(R.string.sound_detected));
                    }
                    else if (msg.what == EventTrigger.ACCELEROMETER || msg.what == EventTrigger.BUMP) {
                        txtCameraStatus.setText(getString(R.string.device_move_detected));
                    }
                    else if (msg.what == EventTrigger.LIGHT) {
                        txtCameraStatus.setText(getString(R.string.status_light));
                    }


                }
            }
        }
    };

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int eventType = intent.getIntExtra("type",-1);

            //String path = intent.getData().getPath();

            handler.sendEmptyMessage(eventType);
        }
    };

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(receiver);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached = true;
        IntentFilter filter = new IntentFilter();
        filter.addAction("event");
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(receiver,filter );
    }

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

                    handler.sendEmptyMessage(motionDetected?EventTrigger.CAMERA:-1);


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
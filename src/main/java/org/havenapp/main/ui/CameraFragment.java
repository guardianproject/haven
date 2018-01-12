/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.media.ImageCodec;
import org.havenapp.main.sensors.media.MotionAsyncTask;
import org.havenapp.main.sensors.motion.Preview;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public final class CameraFragment extends Fragment {

    @BindView(R.id.preview)
    FrameLayout preview1;
    @BindView(R.id.new_image)
    ImageView newImage;
    Unbinder unbinder;
    private Preview preview;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.camera_fragment, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;

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

        initCamera();
    }

    public void resetCamera() {
        preview1.removeAllViews();
        preview = null;
        initCamera();
    }

    private void initCamera() {
        if (preview == null) {

            PreferenceManager prefs = new PreferenceManager(getActivity());

            if (!prefs.getCameraSensitivity().equals(PreferenceManager.OFF)) {
                //Uncomment to see the camera
                preview = new Preview(getActivity());

                preview1.addView(preview);

                preview.addListener(new MotionAsyncTask.MotionListener() {

                    public void onProcess(Bitmap oldBitmap, Bitmap newBitmap, Bitmap rawBitmap,
                                          boolean motionDetected) {
                        int rotation = 0;
                        boolean reflex = false;
                        if (preview.getCameraFacing() == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            rotation = 90;
                        } else {
                            rotation = 270;
                            reflex = true;
                        }

                        // oldImage.setImageBitmap(ImageCodec.rotate(oldBitmap, rotation, reflex));
                        newImage.setImageBitmap(ImageCodec.rotate(newBitmap, rotation, reflex));
                    }
                });
            }
        }
    }

    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
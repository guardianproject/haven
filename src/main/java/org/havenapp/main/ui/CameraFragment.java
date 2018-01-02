
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package org.havenapp.main.ui;

import android.os.Bundle;
import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.FrameLayout;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.media.MotionAsyncTask;
import org.havenapp.main.sensors.media.ImageCodec;
import org.havenapp.main.sensors.motion.Preview;

public final class CameraFragment extends Fragment {

    private Preview preview;

//    private ImageView oldImage;
    private ImageView newImage;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.camera_fragment, container, false);

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

        initCamera ();
    }

    public void resetCamera ()
    {
        ((FrameLayout) getActivity().findViewById(R.id.preview)).removeAllViews();
        preview = null;
        initCamera();
    }

    private void initCamera ()
    {
        if (preview == null) {

            PreferenceManager prefs = new PreferenceManager(getActivity());

            if (!prefs.getCameraSensitivity().equals(PreferenceManager.OFF)) {
                //Uncomment to see the camera
                preview = new Preview(getActivity());

                ((FrameLayout) getActivity().findViewById(R.id.preview)).addView(preview);

                // oldImage = (ImageView) getActivity().findViewById(R.id.old_image);
                newImage = getActivity().findViewById(R.id.new_image);

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
}
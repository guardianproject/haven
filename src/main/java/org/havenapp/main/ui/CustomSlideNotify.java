package org.havenapp.main.ui;

/**
 * Created by n8fr8 on 10/30/17.
 */


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class CustomSlideNotify extends Fragment {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private EditText mEditNumber;
    private View.OnClickListener mListener;
    public static CustomSlideNotify newInstance(int layoutResId) {
        CustomSlideNotify sampleSlide = new CustomSlideNotify();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    public void setSaveListener (View.OnClickListener listener)
    {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
            layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layoutResId, container, false);

        mEditNumber = view.findViewById(R.id.editNumber);
        mEditNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { }
        });
        PreferenceManager pm = new PreferenceManager(getActivity());
        if (!TextUtils.isEmpty(pm.getRemotePhoneNumber()))
            mEditNumber.setText(pm.getRemotePhoneNumber());

        // todo describe why we are asking this maybe

        Button button = view.findViewById(R.id.btnSaveNumber);
        button.setOnClickListener(mListener);
        return view;

    }

    @NonNull
    public String getPhoneNumber ()
    {
        return mEditNumber.getText() != null ? mEditNumber.getText().toString() : "";
    }

    private void askForPermission(String permission, Integer requestCode) {
        if (ContextCompat.checkSelfPermission(getActivity(), permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(getActivity(), new String[]{permission}, requestCode);
            }
        } else {
        }
    }

}
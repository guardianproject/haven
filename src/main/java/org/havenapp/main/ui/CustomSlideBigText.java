package org.havenapp.main.ui;

/**
 * Created by n8fr8 on 10/30/17.
 */


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.havenapp.main.R;


public class CustomSlideBigText extends Fragment {

    private static final String ARG_LAYOUT_RES_ID = "layoutResId";
    private int layoutResId;
    private String mTitle;
    private String mButtonText;
    private View.OnClickListener mButtonListener;

    public static CustomSlideBigText newInstance(int layoutResId) {
        CustomSlideBigText sampleSlide = new CustomSlideBigText();

        Bundle args = new Bundle();
        args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
        sampleSlide.setArguments(args);

        return sampleSlide;
    }

    public void setTitle (String title)
    {
        mTitle = title;
    }

    public void showButton (String buttonText, View.OnClickListener buttonListener)
    {
        mButtonText = buttonText;
        mButtonListener = buttonListener;
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
        ((TextView)view.findViewById(R.id.custom_slide_big_text)).setText(mTitle);

        if (mButtonText != null)
        {
            Button button = view.findViewById(R.id.custom_slide_button);
            button.setVisibility(View.VISIBLE);
            button.setText(mButtonText);
            button.setOnClickListener(mButtonListener);
        }
        return view;

    }
}

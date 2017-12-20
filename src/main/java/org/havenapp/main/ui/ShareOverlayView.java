package org.havenapp.main.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import com.stfalcon.frescoimageviewer.ImageViewer;

import org.havenapp.main.R;


/*
 * Created by Alexander Krol (troy379) on 29.08.16.
 */
public class ShareOverlayView extends RelativeLayout {

    private ImageViewer viewer;

    public ShareOverlayView(Context context) {
        super(context);
        init();
    }

    public ShareOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShareOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setImageViewer (ImageViewer viewer)
    {
        this.viewer = viewer;
    }

    private void sendShareIntent() {

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(viewer.getUrl()));
        shareIntent.setType("*/*");
        getContext().startActivity(shareIntent);
    }

    private void init() {
        View view = inflate(getContext(), R.layout.view_image_overlay, this);
        view.findViewById(R.id.btnShare).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendShareIntent();
            }
        });
    }
}
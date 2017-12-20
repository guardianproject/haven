package org.havenapp.main.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

import com.maxproj.simplewaveform.SimpleWaveform;

/**
 * Created by n8fr8 on 10/30/17.
 */

public class SimpleWaveformExtended extends SimpleWaveform {


    private int mThreshold = 0;

    public SimpleWaveformExtended(Context context) {
        super(context);
    }

    public SimpleWaveformExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setThreshold (int threshold)
    {
        mThreshold  = threshold;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float percDb = (((float)mThreshold)/120f)*100f;
        int lineY = getHeight()/2-(int)percDb*4;
        canvas.drawLine(0,lineY,getWidth(),lineY,peakPencilFirst);
    }
}

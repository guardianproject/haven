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
    private int lineY;
    private int maxVal = 100; // default max value of slider

    public SimpleWaveformExtended(Context context) {
        super(context);
    }

    public SimpleWaveformExtended(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMaxVal(int max_val) {
        this.maxVal = max_val;
    }

    public void setThreshold (int threshold)
    {
        mThreshold  = threshold;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int midY = getHeight()/2;
        lineY =  midY - (int) (((float) mThreshold/ maxVal) * midY);
        canvas.drawLine(0,lineY,getWidth(),lineY,peakPencilFirst);
    }
}

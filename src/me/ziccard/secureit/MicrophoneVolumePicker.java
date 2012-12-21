package me.ziccard.secureit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.View;

public class MicrophoneVolumePicker extends View {
    Paint paint = new Paint();
    
    final int GREEN = 8453888;
    final int ORANGE = 16744448;
    final int RED = 14549506;
    
    public MicrophoneVolumePicker(Context context) {
        super(context);            
    }

    @Override
    public void onDraw(Canvas canvas) {
    	int paddingBorder = 30;  // padding from border 
    	int scaleSize = canvas.getWidth()-paddingBorder*2; // due bordi destro e sinistro.
         	
    	int zeroDBPoint = scaleSize/3*2;  // 0db is placed 2/3 of the available screen
    	int ambientNoisePoint = scaleSize/3;
    	
    	final int bottomScale = -50;
    	final int ambientDBValue = -10; // in db;
    	int currentValue =  2; //value recorded by mic
    	double currentValueWithinScale = currentValue - bottomScale; // bottomscale = 0 
    	if(currentValue < ambientDBValue){
    		final double underAmbientGrain = Math.abs(bottomScale-ambientDBValue);
    
    		paint.setColor(GREEN);
    		//(currentValueWithinScale)*ambientNoisePoint
    		canvas.drawRect(new Rect(paddingBorder,
    								paddingBorder,
    								(int) (paddingBorder+(currentValueWithinScale/underAmbientGrain)*ambientNoisePoint),
    								paddingBorder+30), 
    								paint);
    		Log.i("DEBUG:",""+(currentValueWithinScale/underAmbientGrain)+":"+ambientNoisePoint);
    	} else {
    		// paint the undernoise part
    		paint.setColor(Color.GREEN);
    		canvas.drawRect(new Rect(paddingBorder,
    								paddingBorder,
    								paddingBorder+ambientNoisePoint,
    								paddingBorder+30), paint);
    		
    		if(currentValue < 0){
    			// paint the noise to 0 db part
    		
    		} else {
    		paint.setColor(Color.YELLOW);
    		canvas.drawRect(paddingBorder+ambientNoisePoint,
    						paddingBorder,
    						paddingBorder+zeroDBPoint,
    						paddingBorder+30, 
    						paint );
    			
    			
    			
    		}
    	}
    	
    	
       /*
        paint.setColor(Color.YELLOW);
        canvas.drawRect(33, 60, 77, 77, paint );
        paint.setColor(Color.RED);
        canvas.drawRect(33, 33, 77, 60, paint );
        */

    }

}
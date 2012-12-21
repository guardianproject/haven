package me.ziccard.secureit;

import android.util.Log;

import com.androidplot.series.XYSeries;

public class VolumeDynamicSeries implements XYSeries{
	
	    @SuppressWarnings("unused")
		private int seriesIndex;
	    private String title;
	    short[] data = new short[0];
	 
	    public VolumeDynamicSeries(int seriesIndex, String title) {
	        this.seriesIndex = seriesIndex;
	        this.title = title;
	    }
	    
	    synchronized public void updateData(short[] data) {
	    	this.data = data;
	    }
	    
	    public String getTitle() {
	        return title;
	    }
	 
	    public int size() {
	        return data.length;
	    }
	 
	    public Number getX(int index) {
	    	if (index >= data.length) {
	             throw new IllegalArgumentException();
	        }
	        return index;
	    }
	 
	    public Number getY(int index) {
	    	if (index >= data.length) {
	             throw new IllegalArgumentException();
	        }
	    	double val = 0.0;
	    	if (data[index]!=0) {
	    		Log.i("VolumDynamicSeries", "Non zero value");
	    		val = 20*Math.log10(Math.abs(data[index])/1);
	    	}
	    	//Log.i("LOGGGARTI", ""+val);
	        return val;
	    }
}

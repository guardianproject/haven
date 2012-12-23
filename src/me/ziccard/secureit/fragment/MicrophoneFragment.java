package me.ziccard.secureit.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import me.ziccard.secureit.MicrophoneVolumePicker;
import me.ziccard.secureit.R;
import me.ziccard.secureit.VolumeDynamicSeries;
import me.ziccard.secureit.async.MicSamplerTask;
import com.androidplot.xy.*;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class MicrophoneFragment extends Fragment implements MicSamplerTask.MicListener {


    private MicSamplerTask microphone;
      
    private XYPlot plot;

    private TextView microphoneText;
    
    
    private VolumeDynamicSeries series = new VolumeDynamicSeries(0, "Volume");
 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		
		return inflater.inflate(R.layout.microphone_fragment, container, false);

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
    	
		microphoneText = (TextView) getActivity().findViewById(R.id.microphone);

//		// initialize our XYPlot reference:
//        plot = (XYPlot) getActivity().findViewById(R.id.mySimpleXYPlot);
//        
//        // Define formatting and add series
//        LineAndPointFormatter f1 = new LineAndPointFormatter(Color.rgb(0, 0, 200), null, Color.rgb(0, 0, 80));
//        plot.addSeries(series, f1);
// 
//        // apply some transparency to the series
//        f1.getFillPaint().setAlpha(50);
//
//        plot.setDomainStepMode(XYStepMode.SUBDIVIDE);
//        plot.setDomainStepValue(1);
// 
//        // thin out domain/range tick labels so they dont overlap each other:
//        //plot.setTicksPerDomainLabel(5);
//        //plot.setTicksPerRangeLabel(3);
// 
//        // freeze the range boundaries:
//        plot.setRangeBoundaries(0, 120, BoundaryMode.FIXED);
//        //plot.setTicksPerRangeLabel(10000);
//        //mySimpleXYPlot.setTicksPerDomainLabel(5);
// 
//        // customize our domain/range labels
//        plot.setDomainLabel("");
//        plot.setRangeLabel("");
//        plot.getLegendWidget().setVisible(false);
//        
//        // get rid of decimal points in our domain labels:
//        plot.setDomainValueFormat(new DecimalFormat("0"));
//        //plot.setRangeValueFormat(new DecimalFormat("0"));
// 
//        // by default, AndroidPlot displays developer guides to aid in laying out your plot.
//        // To get rid of them call disableAllMarkup():
//        plot.disableAllMarkup();
    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    	
    	LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.linear_layout);
    	MicrophoneVolumePicker picker = new MicrophoneVolumePicker(this.getActivity());
    	layout.addView(picker, params);
				
        microphone = new MicSamplerTask();
        microphone.setMicListener(this);
        microphone.execute();
    }
    
    @Override
    public void onDestroy() {
    	Log.i("MircorphoneFramgnet", "Fragment destroyed");
    	super.onDestroy();
    	microphone.cancel(true); 
    }
 

	public void onSignalReceived(short[] signal) {
		
		/*
		 * We do and average of the 512 samples
		 */
		int total = 0;
		int count = 0;
		for (short peak : signal) {
			//Log.i("MicrophoneFragment", "Sampled values are: "+peak);
			if (peak != 0) {
				total+=Math.abs(peak);
				count++;
			}
		}
		Log.i("MicrophoneFragment", "Total value: "+total);
		int average = 0;
		if (count > 0) average = total/count;
		/*
		 * We compute a value in decibels 
		 */
		double averageDB = 0.0;
    	if (average!=0) {
    		averageDB = 20*Math.log10(Math.abs(average)/1);
    	}
    	
    	microphoneText.setText("Sampled DBs: "+averageDB);
   }

	public void onMicError() {
		Log.e("MicrophoneActivity", "Microphone is not ready");	
	}
}
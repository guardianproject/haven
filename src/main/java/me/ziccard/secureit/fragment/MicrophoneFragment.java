/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.util.Log;
import me.ziccard.secureit.MicrophoneVolumePicker;
import me.ziccard.secureit.R;
import me.ziccard.secureit.SecureItPreferences;
import me.ziccard.secureit.async.MicSamplerTask;
import me.ziccard.secureit.async.MicrophoneTaskFactory;
import me.ziccard.secureit.async.MicrophoneTaskFactory.RecordLimitExceeded;
import me.ziccard.secureit.service.UploadService;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MicrophoneFragment extends Fragment implements MicSamplerTask.MicListener {


    private MicSamplerTask microphone;

    private TextView microphoneText;
    
    /**
     * View for microphone data
     */
    private MicrophoneVolumePicker picker;
    
    /**
     * Object used to fetch application dependencies
     */
    private SecureItPreferences prefs;
    
    /**
     * Threshold for the decibels sampled
     */
    private double NOISE_THRESHOLD = 60.0; 
    
    /**
     * Messenger used to communicate with alert service
     */
	private Messenger serviceMessenger = null;
	
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
        	Log.i("MicrophoneFragment", "SERVICE CONNECTED");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            serviceMessenger = new Messenger(service);
        }
        
        public void onServiceDisconnected(ComponentName arg0) {
        	Log.i("MicrophoneFragment", "SERVICE DISCONNECTED");
            serviceMessenger = null;
        }
    };
 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return inflater.inflate(R.layout.microphone_fragment, container, false);
	}
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prefs = new SecureItPreferences(getActivity());    
		
		if (prefs.getMicrophoneSensitivity().equals("High")) {
			NOISE_THRESHOLD = 30.0;
		} else if (prefs.getMicrophoneSensitivity().equals("Medium")) {
			NOISE_THRESHOLD = 40.0; 
		}
		
		getActivity().bindService(new Intent(getActivity(), 
				UploadService.class), mConnection, Context.BIND_ABOVE_CLIENT);
		
  	  	try {
			microphone = MicrophoneTaskFactory.makeSampler(getActivity());
	    	microphone.setMicListener(this);
	        microphone.execute();
		} catch (RecordLimitExceeded e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
    @Override
    public void onPause() {
    	super.onPause();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	Log.i("MicrophoneFragment", "Resumed");
    	
		microphoneText = (TextView) getActivity().findViewById(R.id.microphone);

    	LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    	
    	LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.linear_layout);
    	if (layout.getChildCount() == 1) {
    		picker = new MicrophoneVolumePicker(this.getActivity());
    		picker.setNoiseThreshold(NOISE_THRESHOLD);
    		layout.addView(picker, params);
    	}
    	
//    	if (microphone == null || microphone.isCancelled()) {
//
//    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.i("MicrophoneFramgnet", "Fragment destroyed");
    	getActivity().unbindService(mConnection);
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
    	
    	picker.setValues(averageDB, averageDB);
    	picker.invalidate();
    	
    	if (averageDB > NOISE_THRESHOLD) {
    		Message message = new Message();
    		message.what = UploadService.MICROPHONE_MESSAGE;
    		try {
    		  if (serviceMessenger != null)
				serviceMessenger.send(message);
			} catch (RemoteException e) {
				// Cannot happen
			}
    	}
   }

	public void onMicError() {
		Log.e("MicrophoneActivity", "Microphone is not ready");	
	}
}
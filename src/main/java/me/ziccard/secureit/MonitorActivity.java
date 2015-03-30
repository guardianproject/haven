/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit;

import me.ziccard.secureit.fragment.AccelerometerFragment;
import me.ziccard.secureit.fragment.CameraFragment;
import me.ziccard.secureit.fragment.EmptyFragment;
import me.ziccard.secureit.fragment.MicrophoneFragment;
import me.ziccard.secureit.service.UploadService;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitlePageIndicator.IndicatorStyle;

public class MonitorActivity extends FragmentActivity {
	
	private SecureItPreferences preferences = null;
	
	private static final String[] CONTENT = new String[] { "Accel.", "Camera",
			"Mic."};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preferences = new SecureItPreferences(getApplicationContext());

		setContentView(R.layout.activity_monitor);

		FragmentPagerAdapter adapter = new MonitorAdapter(
				getSupportFragmentManager());

        ViewPager pager = (ViewPager)findViewById(R.id.pager);
        pager.setAdapter(adapter);

        TitlePageIndicator indicator = (TitlePageIndicator)findViewById(R.id.indicator);
        indicator.setViewPager(pager);
        final float density = getResources().getDisplayMetrics().density;
        indicator.setBackgroundColor(0x18FF0000);
        indicator.setFooterColor(0xFFAA2222);
        indicator.setFooterLineHeight(1 * density); //1dp
        indicator.setFooterIndicatorHeight(3 * density); //3dp
        indicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
        indicator.setTextColor(0xAA000000);
        indicator.setSelectedColor(0xFF000000);
        indicator.setSelectedBold(true);

		pager.setCurrentItem(1);
		
		/**
		 * Binding to the bluetooth service
		 */
        startService(new Intent(this, UploadService.class));
	}

	class MonitorAdapter extends FragmentPagerAdapter {
		
		private Fragment accelerometerFragment;
		
		private Fragment cameraFragment;
		
		private Fragment microphoneFragment;
		
		public MonitorAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position) {
			case 0: 
				if (preferences.getAccelerometerActivation()) {
					if (accelerometerFragment == null)
						accelerometerFragment = new AccelerometerFragment();
					return accelerometerFragment;
				} else
					return EmptyFragment.newInstance(CONTENT[0]);
					
			case 1: 
				if (preferences.getCameraActivation()) {
					if (cameraFragment == null)
						cameraFragment = new CameraFragment();
					return cameraFragment;
				} else 
					return EmptyFragment.newInstance(CONTENT[1]);
			case 2: 
				if (preferences.getMicrophoneActivation()) {
					if (microphoneFragment == null) 
						microphoneFragment = new MicrophoneFragment();
					return microphoneFragment;
				} else
					return EmptyFragment.newInstance(CONTENT[2]);
			}
			return null;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return CONTENT[position % CONTENT.length].toUpperCase();
		}


		@Override
		public int getCount() {
			return CONTENT.length;
		}
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monitor_activity, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
	    switch (item.getItemId()){
	          case R.id.menu_settings:
	        	  createUnlockDialog();
	        	  break;
	    }
	    return true;
    }
    
    /**
     * Closes the monitor activity and unset session properties
     */
    private void close() {
    	Intent intent = new Intent(
  			  getApplicationContext(), StartActivity.class);
  	  intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
  	  startActivity(intent);
  	  stopService(new Intent(this, UploadService.class));
  	  preferences.unsetAccessToken();
  	  preferences.unsetDelegatedAccessToken();
  	  preferences.unsetPhoneId();
    	
    }
    
    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
    	createUnlockDialog();
    }
    
    /**
     * Shows a dialog prompting the unlock code
     */
    private void createUnlockDialog() {
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setTitle("Stop monitoring?");
    	final EditText input = new EditText(this);
    	input.setHint("Unlock code");
    	input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    	builder.setView(input);

    	builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int whichButton) {
    			if (input.getText().toString().equals(preferences.getUnlockCode())) {
    				Log.i("MonitorActivity", "INPUT "+input.getText().toString());
    				Log.i("MonitorActivity", "STORED "+preferences.getUnlockCode());
    				dialog.dismiss();
    				close();
    			} else {
    				dialog.dismiss();
    				Toast.makeText(
    						getApplicationContext(), 
    						"Wrong unlock code", 
    						Toast.LENGTH_SHORT).show();
    	        }
    		}
    	});

    	builder.show();
    }
}

package me.ziccard.secureit;

import me.ziccard.secureit.fragment.AccelerometerFragment;
import me.ziccard.secureit.fragment.CameraFragment;
import me.ziccard.secureit.fragment.EmptyFragment;
import me.ziccard.secureit.fragment.MicrophoneFragment;
import me.ziccard.secureit.service.BluetoothService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

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
        startService(new Intent(this, BluetoothService.class));
	}

	class MonitorAdapter extends FragmentPagerAdapter {
		public MonitorAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position) {
			case 0: 
				if (preferences.getAccelerometerActivation()) 
					return new AccelerometerFragment();
				else
					return EmptyFragment.newInstance(CONTENT[0]);
					
			case 1: 
				if (preferences.getCameraActivation()) 
					return new CameraFragment();
				else 
					return EmptyFragment.newInstance(CONTENT[1]);
			case 2: 
				if (preferences.getMicrophoneActivation()) 
					return new MicrophoneFragment();
				else
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
	        	  Intent intent = new Intent(
	        			  getApplicationContext(), StartActivity.class);
	        	  intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        	  startActivity(intent);
	        	  stopService(new Intent(this, BluetoothService.class));
	        	  break;
	    }
	    return true;
    }
}

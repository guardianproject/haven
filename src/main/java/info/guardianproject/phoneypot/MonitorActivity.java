
/*
 * Copyright (c) 2017 Nathanial Freitas / Guardian Project
 *  * Licensed under the GPLv3 license.
 *
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */
package info.guardianproject.phoneypot;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import info.guardianproject.phoneypot.service.MonitorService;

public class MonitorActivity extends FragmentActivity {
	
	private PreferenceManager preferences = null;
    private TextView txtTimer;
    private CountDownTimer cTimer;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		preferences = new PreferenceManager(getApplicationContext());
        setContentView(R.layout.layout_running);

        txtTimer = (TextView)findViewById(R.id.timer_text);

        initTimer();

        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab_cancel);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (cTimer != null) {
                    cTimer.cancel();
                    cTimer = null;
                }

                close();
            }
        });


	}

	private void initTimer ()
    {
        cTimer = new CountDownTimer((preferences.getTimerDelay()+1)*1000, 1000) {

            public void onTick(long millisUntilFinished) {
               // mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext

                txtTimer.setText(""+millisUntilFinished/1000);
            }

            public void onFinish() {

                txtTimer.setVisibility(View.GONE);
                initMonitor();
            }

        };

        cTimer.start();


    }

	private void initMonitor ()
    {

        //Do something after 100ms
        startService(new Intent(MonitorActivity.this, MonitorService.class));

    }
    
    /**
     * Closes the monitor activity and unset session properties
     */
    private void close() {

  	  stopService(new Intent(this, MonitorService.class));
  	  preferences.unsetAccessToken();
  	  preferences.unsetDelegatedAccessToken();
  	  preferences.unsetPhoneId();
        finish();
    	
    }
    
    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
		close();
    }
    

}

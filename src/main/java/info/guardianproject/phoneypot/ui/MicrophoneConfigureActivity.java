package info.guardianproject.phoneypot.ui;

import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import info.guardianproject.phoneypot.PreferenceManager;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.sensors.media.AudioRecorderTask;
import info.guardianproject.phoneypot.sensors.media.MicSamplerTask;
import info.guardianproject.phoneypot.sensors.media.MicrophoneTaskFactory;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

import static info.guardianproject.phoneypot.R.id.microphone;

public class MicrophoneConfigureActivity extends AppCompatActivity implements MicSamplerTask.MicListener {

    private MicSamplerTask microphone;
    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_configure);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("Sound Sensitivity");

        mTextLevel = (TextView)findViewById(R.id.text_display_level);
        mNumberTrigger = (ActualNumberPicker)findViewById(R.id.number_trigger_level);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(120);

        mPrefManager = new PreferenceManager(this.getApplicationContext());

        if (mPrefManager.getMicrophoneSensitivity().equals("High")) {
            mNumberTrigger.setValue(40);
        } else if (mPrefManager.getMicrophoneSensitivity().equals("Medium")) {
            mNumberTrigger.setValue(60);
        }
        else
        {
            try {
                //maybe it is a threshold value?
                mNumberTrigger.setValue(Integer.parseInt(mPrefManager.getMicrophoneSensitivity()));
            }
            catch (Exception e){}
        }

        startMic();
    }

    private void startMic ()
    {
        try {
            microphone = MicrophoneTaskFactory.makeSampler(this);
            microphone.setMicListener(this);
            microphone.execute();
        } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (microphone != null)
            microphone.cancel(true);

        mPrefManager.setMicrophoneSensitivity(mNumberTrigger.getValue()+"");
    }

    @Override
    public void onSignalReceived(short[] signal) {
        /*
		 * We do and average of the 512 samples
		 */
        int total = 0;
        int count = 0;
        for (short peak : signal) {
            //Log.i("MicrophoneFragment", "Sampled values are: "+peak);
            if (peak != 0) {
                total += Math.abs(peak);
                count++;
            }
        }
        //  Log.i("MicrophoneFragment", "Total value: " + total);
        int average = 0;
        if (count > 0) average = total / count;
		/*
		 * We compute a value in decibels
		 */
        double averageDB = 0.0;
        if (average != 0) {
            averageDB = 20 * Math.log10(Math.abs(average) / 1);
        }

        mTextLevel.setText(((int)averageDB)+"");

        if (averageDB > mNumberTrigger.getValue())
        {
            Toast.makeText(this,"Sound Threshold Crossed!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMicError() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.monitor_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_save:
                finish();
                break;
        }
        return true;
    }
}

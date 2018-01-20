package org.havenapp.main.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;

import com.maxproj.simplewaveform.SimpleWaveform;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.sensors.media.MicSamplerTask;
import org.havenapp.main.sensors.media.MicrophoneTaskFactory;

import java.util.LinkedList;

import me.angrybyte.numberpicker.listener.OnValueChangeListener;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class MicrophoneConfigureActivity extends AppCompatActivity implements MicSamplerTask.MicListener {

    private MicSamplerTask microphone;
    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;
    private SimpleWaveformExtended mWaveform;
    private LinkedList<Integer> mWaveAmpList;
    private static final int MAX_SLIDER_VALUE = 120;

    private double maxAmp = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microphone_configure);
        mPrefManager = new PreferenceManager(this.getApplicationContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextLevel = findViewById(R.id.text_display_level);
        mNumberTrigger = findViewById(R.id.number_trigger_level);
        mWaveform = findViewById(R.id.simplewaveform);
        mWaveform.setMaxVal(100);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(MAX_SLIDER_VALUE);

        if (!mPrefManager.getMicrophoneSensitivity().equals(PreferenceManager.MEDIUM))
            mNumberTrigger.setValue(Integer.parseInt(mPrefManager.getMicrophoneSensitivity()));
        else
            mNumberTrigger.setValue(60);

        mNumberTrigger.setListener((oldValue, newValue) -> {
            mWaveform.setThreshold(newValue);
            mPrefManager.setMicrophoneSensitivity(newValue+"");
        });


        initWave();
        startMic();
    }

    private void initWave ()
    {
        mWaveform.init();

        mWaveAmpList = new LinkedList<>();

        mWaveform.setDataList(mWaveAmpList);

        //define bar gap
        mWaveform.barGap = 30;

        //define x-axis direction
        mWaveform.modeDirection = SimpleWaveform.MODE_DIRECTION_RIGHT_LEFT;

        //define if draw opposite pole when show bars
        mWaveform.modeAmp = SimpleWaveform.MODE_AMP_ABSOLUTE;
        //define if the unit is px or percent of the view's height
        mWaveform.modeHeight = SimpleWaveform.MODE_HEIGHT_PERCENT;
        //define where is the x-axis in y-axis
        mWaveform.modeZero = SimpleWaveform.MODE_ZERO_CENTER;
        //if show bars?
        mWaveform.showBar = true;

        mWaveform.setMaxVal(100);

        //define how to show peaks outline
        mWaveform.modePeak = SimpleWaveform.MODE_PEAK_ORIGIN;
        //if show peaks outline?
        mWaveform.showPeak = true;

        //show x-axis
        mWaveform.showXAxis = true;
        Paint xAxisPencil = new Paint();
        xAxisPencil.setStrokeWidth(1);
        xAxisPencil.setColor(0x88ffffff);
        mWaveform.xAxisPencil = xAxisPencil;

        //define pencil to draw bar
        Paint barPencilFirst = new Paint();
        Paint barPencilSecond = new Paint();
        Paint peakPencilFirst = new Paint();
        Paint peakPencilSecond = new Paint();

        barPencilFirst.setStrokeWidth(15);
        barPencilFirst.setColor(getResources().getColor(R.color.colorAccent));
        mWaveform.barPencilFirst = barPencilFirst;

        barPencilFirst.setStrokeWidth(15);

        barPencilSecond.setStrokeWidth(15);
        barPencilSecond.setColor(getResources().getColor(R.color.colorPrimaryDark));
        mWaveform.barPencilSecond = barPencilSecond;

        //define pencil to draw peaks outline
        peakPencilFirst.setStrokeWidth(5);
        peakPencilFirst.setColor(getResources().getColor(R.color.colorAccent));
        mWaveform.peakPencilFirst = peakPencilFirst;
        peakPencilSecond.setStrokeWidth(5);
        peakPencilSecond.setColor(getResources().getColor(R.color.colorPrimaryDark));
        mWaveform.peakPencilSecond = peakPencilSecond;
        mWaveform.firstPartNum = 0;


        //define how to clear screen
        mWaveform.clearScreenListener = new SimpleWaveform.ClearScreenListener() {
            @Override
            public void clearScreen(Canvas canvas) {
                canvas.drawColor(Color.WHITE, PorterDuff.Mode.CLEAR);
            }
        };
        //show...
        mWaveform.refresh();
    }
    private void startMic () {
        String permission = Manifest.permission.RECORD_AUDIO;
        int requestCode = 999;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {

                //This is called if user has denied the permission before
                //In this case I am just asking the permission again
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            }
        } else {

            try {
                microphone = MicrophoneTaskFactory.makeSampler(this);
                microphone.setMicListener(this);
                microphone.execute();
            } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 999:
                startMic();
                break;

        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (microphone != null)
            microphone.cancel(true);

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
            averageDB = 20 * Math.log10(Math.abs(average));
        }

        if (averageDB > maxAmp) {
            maxAmp = averageDB + 5d; //add 5db buffer
            mNumberTrigger.setValue((int) maxAmp);
            mNumberTrigger.invalidate();
        }

        int perc = (int)((averageDB/120d)*100d)-10;
        mWaveAmpList.addFirst(perc);

        if (mWaveAmpList.size() > mWaveform.width / mWaveform.barGap + 2) {
            mWaveAmpList.removeLast();
        }

        mWaveform.refresh();
        mTextLevel.setText(getString(R.string.current_noise_base).concat(" ").concat(Integer.toString((int) averageDB)).concat("db"));

    }

    @Override
    public void onMicError() {

    }




    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    /**
     * When user closes the activity
     */
    @Override
    public void onBackPressed() {
        finish();
    }
}

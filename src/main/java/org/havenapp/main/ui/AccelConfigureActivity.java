package org.havenapp.main.ui;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.maxproj.simplewaveform.SimpleWaveform;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;

import java.util.LinkedList;

import me.angrybyte.numberpicker.listener.OnValueChangeListener;
import me.angrybyte.numberpicker.view.ActualNumberPicker;

public class AccelConfigureActivity extends AppCompatActivity implements SensorEventListener {

    private TextView mTextLevel;
    private ActualNumberPicker mNumberTrigger;
    private PreferenceManager mPrefManager;
    private SimpleWaveformExtended mWaveform;
    private LinkedList<Integer> mWaveAmpList;

    private static final int MAX_SLIDER_VALUE = 100;

    private double maxAmp = 0;

    /**
     * Last update of the accelerometer
     */
    private long lastUpdate = -1;

    /**
     * Current accelerometer values
     */
    private float accel_values[];

    /**
     * Last accelerometer values
     */
    private float last_accel_values[];


    /**
     * Shake threshold
     */
    private int shakeThreshold = -1;

    /**
     * Text showing accelerometer values
     */
    private int maxAlertPeriod = 30;
    private int remainingAlertPeriod = 0;
    private boolean alert = false;
    private final static int CHECK_INTERVAL = 1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accel_configure);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mTextLevel = findViewById(R.id.text_display_level);
        mNumberTrigger = findViewById(R.id.number_trigger_level);
        mWaveform = findViewById(R.id.simplewaveform);
        mWaveform.setMaxVal(MAX_SLIDER_VALUE);

        mNumberTrigger.setMinValue(0);
        mNumberTrigger.setMaxValue(MAX_SLIDER_VALUE);
        mNumberTrigger.setListener(new OnValueChangeListener() {
            @Override
            public void onValueChanged(int oldValue, int newValue) {
                mWaveform.setThreshold(newValue);
            }
        });

        mPrefManager = new PreferenceManager(this.getApplicationContext());



        initWave();
        startAccel();
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
    private void startAccel () {

            try {

                SensorManager sensorMgr = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
                Sensor sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

                if (sensor == null) {
                    Log.i("AccelerometerFrament", "Warning: no accelerometer");
                } else {
                    sensorMgr.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);

                }


            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

    }

    public void onSensorChanged(SensorEvent event) {
        long curTime = System.currentTimeMillis();
        // only allow one update every 100ms.
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if ((curTime - lastUpdate) > CHECK_INTERVAL) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                accel_values = event.values.clone();

                if (alert && remainingAlertPeriod > 0) {
                    remainingAlertPeriod = remainingAlertPeriod - 1;
                } else {
                    alert = false;
                }

                if (last_accel_values != null) {

                    int speed = (int)(Math.abs(
                            accel_values[0] + accel_values[1] + accel_values[2] -
                                    last_accel_values[0] + last_accel_values[1] + last_accel_values[2])
                            / diffTime * 1000);

                    if (speed > shakeThreshold) {
						/*
						 * Send Alert
						 */

                        alert = true;
                        remainingAlertPeriod = maxAlertPeriod;

                        double averageDB = 0.0;
                        if (speed != 0) {
                            averageDB = 20 * Math.log10(Math.abs(speed));
                        }

                        if (averageDB > maxAmp) {
                            maxAmp = averageDB + 5d; //add 5db buffer
                            mNumberTrigger.setValue((int) maxAmp);
                            mNumberTrigger.invalidate();
                        }

                        mWaveAmpList.addFirst(speed);

                        if (mWaveAmpList.size() > mWaveform.width / mWaveform.barGap + 2) {
                            mWaveAmpList.removeLast();
                        }

                        mWaveform.refresh();
                        mTextLevel.setText(getString(R.string.current_accel_base) + ' ' + speed);



                    }
                }
                last_accel_values = accel_values.clone();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void save ()
    {
        //mPrefManager.setMicrophoneSensitivity(mNumberTrigger.getValue()+"");

        mPrefManager.setAccelerometerSensitivity(mNumberTrigger.getValue()+"");
        finish();
    }


    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                save();
                finish();
                break;
        }
        return true;
    }
}

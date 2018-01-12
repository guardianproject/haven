package org.havenapp.main.sensors;

/**
 * Created by n8fr8 on 3/10/17.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import org.havenapp.main.PreferenceManager;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.sensors.media.AudioRecorderTask;
import org.havenapp.main.sensors.media.MicSamplerTask;
import org.havenapp.main.sensors.media.MicrophoneTaskFactory;
import org.havenapp.main.service.MonitorService;


public final class MicrophoneMonitor implements MicSamplerTask.MicListener {


    private MicSamplerTask microphone;

    /**
     * Object used to fetch application dependencies
     */
    private PreferenceManager prefs;

    /**
     * Threshold for the decibels sampled
     */
    private double mNoiseThreshold = 70.0;

    /**
     * Messenger used to communicate with alert service
     */
    private Messenger serviceMessenger = null;

    private Context context;

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


    public MicrophoneMonitor(Context context)
    {
        this.context = context;

        prefs = new PreferenceManager(context);

        switch (prefs.getMicrophoneSensitivity()) {
            case "High":
                mNoiseThreshold = 40;
                break;
            case "Medium":
                mNoiseThreshold = 60;
                break;
            default:
                try {
                    //maybe it is a threshold value?
                    mNoiseThreshold = Double.parseDouble(prefs.getMicrophoneSensitivity());
                } catch (Exception ignored) {
                }
                break;
        }

        context.bindService(new Intent(context,
                MonitorService.class), mConnection, Context.BIND_ABOVE_CLIENT);

        try {
            microphone = MicrophoneTaskFactory.makeSampler(context);
            microphone.setMicListener(this);
            microphone.execute();
        } catch (MicrophoneTaskFactory.RecordLimitExceeded e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }



    }

    public void stop (Context context)
    {
        context.unbindService(mConnection);
        if (microphone != null)
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

        if (averageDB > mNoiseThreshold) {

            if (!MicrophoneTaskFactory.isRecording()) {
                try {
                    AudioRecorderTask audioRecorderTask = MicrophoneTaskFactory.makeRecorder(context);
                    audioRecorderTask.setAudioRecorderListener(new AudioRecorderTask.AudioRecorderListener() {
                        @Override
                        public void recordingComplete(String path) {

                            Message message = new Message();
                            message.what = EventTrigger.MICROPHONE;
                            message.getData().putString("path",path);
                            try {
                                if (serviceMessenger != null)
                                    serviceMessenger.send(message);
                            } catch (RemoteException e) {
                                // Cannot happen
                            }
                        }
                    });
                    audioRecorderTask.start();


                } catch (MicrophoneTaskFactory.RecordLimitExceeded rle) {
                    Log.w("MicrophoneMonitor", "We are already recording!");
                }
            }
        }
    }

    public void onMicError() {
        Log.e("MicrophoneActivity", "Microphone is not ready");
    }
}
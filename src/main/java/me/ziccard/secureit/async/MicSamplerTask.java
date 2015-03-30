/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */


package me.ziccard.secureit.async;

import java.io.IOException;

import me.ziccard.secureit.codec.AudioCodec;
import android.os.AsyncTask;
import android.util.Log;

public class MicSamplerTask extends AsyncTask<Void,Object,Void> {
	
	private MicListener listener = null;
	private AudioCodec volumeMeter = new AudioCodec();
	private boolean sampling = true;
	private boolean paused = false;
	
	public static interface MicListener {
		public void onSignalReceived(short[] signal);
		public void onMicError();
	}
	
	public void setMicListener(MicListener listener) {
		this.listener = listener;
	}
	
	protected Void onPreExecute(Void...params) {
		return null;
	}

	@Override
	protected Void doInBackground(Void... params) {
		
		try {
			volumeMeter.start();
		} catch (Exception e) {
			Log.e("MicSamplerTask", "Failed to start VolumeMeter");
			e.printStackTrace();
			if (listener != null) {
				listener.onMicError();
			}
			return null;
		}
		
		while (true) {

			if (listener != null) {
				Log.i("MicSamplerTask", "Requesting amplitude");
				publishProgress(volumeMeter.getAmplitude());
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) { 
				//Nothing to do we exit next line 
				
			}
			boolean restartVolumeMeter = false;
			if (paused) {
				restartVolumeMeter = true;
				volumeMeter.stop();
				sampling = false;
			}
			while (paused) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (restartVolumeMeter) {
				try {
					Log.i("MicSamplerTask", "Task restarted");
					volumeMeter = new AudioCodec();
					volumeMeter.start();
					sampling = true;
				} catch (IllegalStateException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (isCancelled()) { volumeMeter.stop(); sampling = false; return null; }
		}	
	}
	
	public boolean isSampling() {
		return sampling;
	}
	
	public void restart() {
		paused = false;
		sampling = true;
	}
	
	public void pause() {
		paused = true;		
	}
	
	@Override
    protected void onProgressUpdate(Object... progress) {
		short[] data = (short[]) progress[0];
        listener.onSignalReceived(data);
    }
}

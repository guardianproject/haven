package me.ziccard.secureit.async;

import me.ziccard.secureit.codec.AudioCodec;
import android.os.AsyncTask;
import android.util.Log;

public class MicSamplerTask extends AsyncTask<Void,Object,Void> {
	
	private MicListener listener = null;
	private AudioCodec volumeMeter = new AudioCodec();
	
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
				Thread.sleep(1000);
			} catch (InterruptedException e) { 
				//Nothing to do we exit next line 
				
			}
			if (isCancelled()) { volumeMeter.stop(); return null; }
		}	
	}
	
	@Override
    protected void onProgressUpdate(Object... progress) {
		short[] data = (short[]) progress[0];
        listener.onSignalReceived(data);
    }
}

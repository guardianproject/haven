package me.ziccard.secureit.codec;

import java.io.IOException;
import java.util.Arrays;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioCodec {
	
	private AudioRecord recorder = null;
	private int minSize;
		
	/**
	 * Configures the recorder and starts it
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	public void start() throws IllegalStateException, IOException {
		if (recorder == null) {
			minSize = AudioRecord.getMinBufferSize(
					8000,
					AudioFormat.CHANNEL_IN_DEFAULT, 
					AudioFormat.ENCODING_PCM_16BIT);
			recorder = new AudioRecord(
					MediaRecorder.AudioSource.MIC, 
					8000,
					AudioFormat.CHANNEL_IN_DEFAULT,
					AudioFormat.ENCODING_PCM_16BIT,
					minSize);
			recorder.startRecording();
		}
	}
	
	/**
	 * Returns current sound level
	 * @return sound level
	 */
    public short[] getAmplitude() {
    	if (recorder != null) {
    		short[] buffer = new short[minSize];
    		recorder.read(buffer, 0, minSize);

    		return Arrays.copyOf(buffer, 512);
    	}
    	return null;
    }
    
    
    
    public void stop() {
        if (recorder != null
            && recorder.getState() != AudioRecord.STATE_UNINITIALIZED) {
        	recorder.stop();
        	recorder.release();
        }
        recorder = null;
    }
}

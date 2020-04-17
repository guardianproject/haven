/*
 * Copyright (c) 2013-2015 Marco Ziccardi, Luca Bonato
 * Licensed under the MIT license.
 */

package org.havenapp.main.sensors.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.util.Arrays;

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
					44100,
					AudioFormat.CHANNEL_IN_DEFAULT,
					AudioFormat.ENCODING_PCM_16BIT);
            Log.e("AudioCodec", "Minimum size is " + minSize);
			recorder = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					44100,
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
    		short[] buffer = new short[8192];
            int readBytes = 0;
            while (readBytes < 8192) {
                readBytes += recorder.read(buffer, readBytes, 8192-readBytes);
            }

            short[] copyToReturn = Arrays.copyOf(buffer, 512);
            Arrays.sort(buffer);
            Log.e("AudioCodec", "Recorder has read: " + readBytes + " the maximum is: " +
                    buffer[minSize-1]);

    		return copyToReturn;
    	}
    	return null;
    }
      
    
    public void stop() {
        if (recorder != null
            && recorder.getState() != AudioRecord.STATE_UNINITIALIZED) {
        	recorder.stop();
        	recorder.release();
        	Log.i("AudioCodec", "Sampling stopped");
        }
        Log.i("AudioCodec", "Recorder set to null");
        recorder = null;
    }
}

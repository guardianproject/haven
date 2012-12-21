package me.ziccard.secureit.async;

import android.content.ContentValues;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;

public class MicRecorderTask extends Thread {
	
	/**
	 * Name of the file to wich we are going to record audio
	 */
	private static final String FILENAME = "SecureIt_Audio";
	
	/**
	 * True iff the thread is recording
	 */
	private boolean recording = false;
	
	/**
	 * Duration in milliseconds of the audio track
	 */
	private static final long DURATION = 10000;
	
	/**
	 * Getter for recording data field
	 */
	public boolean isRecording() {
		return recording;
	}
	
	/**
	 * We make recorder protected in order to forse
	 * Factory usage
	 */
	protected MicRecorderTask() {
		super();
	}
	@Override
	public void run() {
		recording = true;
		final MediaRecorder recorder = new MediaRecorder();
        ContentValues values = new ContentValues(3);
        values.put(MediaStore.MediaColumns.TITLE, FILENAME);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);

        recorder.setOutputFile(Environment.getExternalStorageDirectory().getPath()+
        		"/"+FILENAME+".mp3");
        try {
          recorder.prepare();
        } catch (Exception e){
            e.printStackTrace();
        }

        recorder.start();
        try {
			Thread.sleep(DURATION);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        recorder.stop();
        recorder.release();
        recording = false;
        
	}

}

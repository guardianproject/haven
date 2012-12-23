package me.ziccard.secureit.async.upload;

import android.content.Context;

public class AudioRecorderTaskFactory {
	
	public static class RecordLimitExceeded extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7030672869928993643L;
		
	}
	
	private static AudioRecorderTask task;
	
	public static AudioRecorderTask makeRecorder(Context context) throws RecordLimitExceeded {
		if (task != null && task.isRecording()) 
			throw new RecordLimitExceeded();
		task = new AudioRecorderTask(context);
		return task;
	}

}

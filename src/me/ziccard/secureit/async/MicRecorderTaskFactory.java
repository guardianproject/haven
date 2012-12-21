package me.ziccard.secureit.async;

public class MicRecorderTaskFactory {
	
	public static class RecordLimitExceeded extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 7030672869928993643L;
		
	}
	
	private static MicRecorderTask task;
	
	public static MicRecorderTask makeRecorder() throws RecordLimitExceeded {
		if (task != null && task.isRecording()) 
			throw new RecordLimitExceeded();
		task = new MicRecorderTask();
		return task;
	}

}

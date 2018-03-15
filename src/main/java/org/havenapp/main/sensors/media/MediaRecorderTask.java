package org.havenapp.main.sensors.media;

import android.hardware.Camera;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;

/**
 * Created by percy on 14/1/18.
 */

public class MediaRecorderTask  {

    private String mOutputFile;
    private android.media.MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private int mSeconds;

    public MediaRecorderTask(Camera camera, String fileImageDir, int seconds) {
        mCamera = camera;
        mOutputFile = fileImageDir;
        mSeconds = seconds;
        if(prepare(mCamera)){
            Log.d("Done", "Media Recorder prepared");
        } else {
            Log.d("Error", "Media Recorder not prepared");
        }
    }

    public MediaRecorder getPreparedMediaRecorder(){

        return mMediaRecorder;
    }

    private boolean prepare(Camera camera) {

        mCamera = camera;
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
        mMediaRecorder.setMaxDuration(mSeconds);
        mMediaRecorder.setOutputFile(mOutputFile);
        try {
            mMediaRecorder.prepare();
        } catch(IllegalStateException e) {
            Log.d("ERROR", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d("ERROR", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if(mMediaRecorder != null){
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }
}

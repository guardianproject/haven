package org.havenapp.main.model;

import android.content.Context;

import com.orm.SugarRecord;

import org.havenapp.main.R;

import java.util.Date;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTrigger extends SugarRecord {

    private int mType;
    private Date mTime;
    private long mEventId;

    private String mPath;

    /**
     * Acceleration detected message
     */
    public static final int ACCELEROMETER = 0;

    /**
     * Camera motion detected message
     */
    public static final int CAMERA = 1;

    /**
     * Mic noise detected message
     */
    public static final int MICROPHONE = 2;

    /**
     * Pressure change detected message
     */
    public static final int PRESSURE = 3;

    /**
     * Light change detected message
     */
    public static final int LIGHT = 4;

    /**
     * Power change detected message
     */
    public static final int POWER = 5;
    /**
     * Significant motion detected message
     */
    public static final int BUMP = 6;

    /**
     * Significant motion detected message
     */
    public static final int CAMERA_VIDEO = 7;


    public EventTrigger ()
    {
        mTime = new Date();
    }

    public void setType (int type)
    {
        mType = type;
    }

    public int getType ()
    {
        return mType;
    }

    public Date getTriggerTime ()
    {
        return mTime;
    }

    public void setEventId (long eventId)
    {
        mEventId = eventId;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String mPath) {
        this.mPath = mPath;
    }


    public String getStringType (Context context)
    {
        String sType = "";

        switch (getType()) {
            case EventTrigger.ACCELEROMETER:
                sType = context.getString(R.string.sensor_accel);
                break;
            case EventTrigger.LIGHT:
                sType = context.getString(R.string.sensor_light);
                break;
            case EventTrigger.CAMERA:
                sType = context.getString(R.string.sensor_camera);
                break;
            case EventTrigger.MICROPHONE:
                sType = context.getString(R.string.sensor_sound);
                break;
            case EventTrigger.POWER:
                sType = context.getString(R.string.sensor_power);
                break;
            case EventTrigger.BUMP:
                sType = context.getString(R.string.sensor_bump);
                break;
            case EventTrigger.CAMERA_VIDEO:
                sType = context.getString(R.string.sensor_camera_video);
                break;
            default:
                sType = context.getString(R.string.sensor_unknown);
        }

        return sType;

    }

    public String getMimeType ()
    {
        String sType = "";

        switch (getType()) {
            case EventTrigger.CAMERA:
                sType = "image/*";
                break;
            case EventTrigger.MICROPHONE:
                sType = "audio/*";
                break;
            case EventTrigger.CAMERA_VIDEO:
                sType = "video/*";
                break;
            default:
                sType = null;
        }

        return sType;

    }

}

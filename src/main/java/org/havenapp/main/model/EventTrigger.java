package org.havenapp.main.model;

import com.orm.SugarRecord;

import java.util.Date;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTrigger extends SugarRecord {

    int mType;
    Date mTime;
    long mEventId;

    String mPath;

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
    public static final int PRESSURE = 2;

    /**
     * Light change detected message
     */
    public static final int LIGHT = 3;

    /**
     * Power change detected message
     */
    public static final int POWER = 4;


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

    public String getStringType ()
    {
        String sType = "";

        switch (getType()) {
            case EventTrigger.ACCELEROMETER:
                sType = "ACCELEROMETER";
                break;
            case EventTrigger.LIGHT:
                sType = "LIGHT";
                break;
            case EventTrigger.CAMERA:
                sType = "CAMERA MOTION";
                break;
            case EventTrigger.MICROPHONE:
                sType = "SOUND";
                break;
            case EventTrigger.POWER:
                sType = "POWER";
                break;
            default:
                sType = "UNKNOWN";
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
            default:
                sType = null;
        }

        return sType;

    }

}

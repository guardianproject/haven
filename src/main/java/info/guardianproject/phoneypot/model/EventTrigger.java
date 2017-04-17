package info.guardianproject.phoneypot.model;

import com.orm.SugarRecord;

import java.util.Date;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTrigger extends SugarRecord {

    int mType;
    Date mTime;
    long mEventId;


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
}

package info.guardianproject.phoneypot.model;

import com.orm.SugarRecord;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class Event extends SugarRecord {

    private Date mStartTime;

    private ArrayList<EventTrigger> mEventTriggers;

    public final static long EVENT_WINDOW_TIME = 1000 * 60; //1 minutes

    public Event ()
    {
        mStartTime = new Date();
        mEventTriggers = new ArrayList<>();
    }

    public void addEventTrigger (EventTrigger eventTrigger)
    {
        mEventTriggers.add(eventTrigger);
    }

    /**
    * Are we within the time window of this event, or should we start a new event?
     */
    public boolean insideEventWindow (Date now)
    {
        if (mEventTriggers.size() == 0)
            return now.getTime() - mStartTime.getTime() <= EVENT_WINDOW_TIME;
        else
            return now.getTime() <= mEventTriggers.get(mEventTriggers.size()-1).getTriggerTime().getTime();
    }
}

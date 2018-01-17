package org.havenapp.main.model;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class Event extends SugarRecord {

    private Date mStartTime;

    @Ignore
    private ArrayList<EventTrigger> mEventTriggers;

    public Event ()
    {
        mStartTime = new Date();
        mEventTriggers = new ArrayList<>();
    }

    public Date getStartTime ()
    {
        return mStartTime;
    }

    public void addEventTrigger (EventTrigger eventTrigger)
    {
        mEventTriggers.add(eventTrigger);
        eventTrigger.setEventId(getId());
    }

    public ArrayList<EventTrigger> getEventTriggers ()
    {
        if (mEventTriggers.size() == 0) {
            List<EventTrigger> eventTriggers = EventTrigger.find(EventTrigger.class, "M_EVENT_ID = ?", getId() + "");

            mEventTriggers.addAll(eventTriggers);

        }

        return mEventTriggers;
    }

}

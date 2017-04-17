package info.guardianproject.phoneypot.model;

import com.orm.SugarRecord;

import java.util.Date;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTrigger extends SugarRecord {

    public enum TriggerType {
        CAMERA, SOUND, ACCEL
    }

    public TriggerType mType;
    public Date mTime;

    public EventTrigger (TriggerType type)
    {
        mTime = new Date();
        mType = type;
    }

    public TriggerType getType ()
    {
        return mType;
    }

    public Date getTriggerTime ()
    {
        return mTime;
    }
}

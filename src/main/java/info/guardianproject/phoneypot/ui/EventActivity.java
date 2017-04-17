package info.guardianproject.phoneypot.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;

public class EventActivity extends AppCompatActivity {

    StringBuffer mEventLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        long eventId = getIntent().getLongExtra("eventid",-1);

        if (eventId != -1) {
            Event event = Event.findById(Event.class, eventId);

            mEventLog = new StringBuffer();

            setTitle("Event @ " + event.getStartTime().toLocaleString());

            for (EventTrigger eventTrigger : event.getEventTriggers())
            {

                mEventLog.append("Event Triggered @ " + eventTrigger.getTriggerTime().toLocaleString()).append("\n");

                String sType = "";

                switch (eventTrigger.getType())
                {
                    case EventTrigger.ACCELEROMETER:
                        sType = "ACCELEROMETER";
                        break;
                    case EventTrigger.CAMERA:
                        sType = "CAMERA MOTION";
                        break;
                    case EventTrigger.MICROPHONE:
                        sType = "SOUND";
                        break;
                    default:
                        sType = "UNKNOWN";
                }

                mEventLog.append("Event Type: " + sType);
                mEventLog.append("\n==========================\n");
            }

            TextView tvEventLog = (TextView)findViewById(R.id.event_log);
            tvEventLog.setText(mEventLog.toString());

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    shareEvent();
                }
            });
        }
        else
            finish();
    }

    private void shareEvent ()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plan");
        intent.putExtra(Intent.EXTRA_TEXT,mEventLog.toString());
        startActivity(intent);
    }
}

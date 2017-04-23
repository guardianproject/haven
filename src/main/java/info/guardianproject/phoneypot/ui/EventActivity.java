package info.guardianproject.phoneypot.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import java.io.File;

import info.guardianproject.phoneypot.ListActivity;
import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;

public class EventActivity extends AppCompatActivity {


    private RecyclerView mRecyclerView;
    private Event mEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);

        long eventId = getIntent().getLongExtra("eventid",-1);

        if (eventId != -1) {

            mEvent = Event.findById(Event.class, eventId);
            mRecyclerView = (RecyclerView)findViewById(R.id.event_trigger_list);

            setTitle(mEvent.getStartTime().toLocaleString());

            EventTriggerAdapter adapter = new EventTriggerAdapter(this, mEvent.getEventTriggers());

            LinearLayoutManager llm = new LinearLayoutManager(this);
            mRecyclerView.setLayoutManager(llm);
            mRecyclerView.setAdapter(adapter);

            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    shareEvent();
                }
            });

            /**
            adapter.SetOnItemClickListener(new EventTriggerAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    EventTrigger eventTrigger = mEvent.getEventTriggers().get(position);

                    if (eventTrigger.getPath() != null) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setDataAndType(Uri.fromFile(new File(eventTrigger.getPath())),eventTrigger.getMimeType());
                        startActivity(i);
                    }
                }
            });**/
        }
        else
            finish();
    }

    private void shareEvent ()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plan");
        intent.putExtra(Intent.EXTRA_TEXT,generateLog());
        startActivity(intent);
    }

    private String generateLog () {
        StringBuffer mEventLog = new StringBuffer();

        setTitle("Event @ " + mEvent.getStartTime().toLocaleString());

        for (EventTrigger eventTrigger : mEvent.getEventTriggers()) {

            mEventLog.append("Event Triggered @ " + eventTrigger.getTriggerTime().toLocaleString()).append("\n");

            String sType = "";

            switch (eventTrigger.getType()) {
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

        return mEventLog.toString();
    }

}

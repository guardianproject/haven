package org.havenapp.main.ui;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.MenuItem;

import com.stfalcon.frescoimageviewer.ImageViewer;

import org.havenapp.main.R;
import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.database.async.EventTriggerDeleteAsync;
import org.havenapp.main.database.async.EventTriggerInsertAsync;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.resources.IResourceManager;
import org.havenapp.main.resources.ResourceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EventActivity extends AppCompatActivity implements EventTriggerAdapter.EventTriggerClickListener {

    private IResourceManager resourceManager;
    private RecyclerView mRecyclerView;
    private Event mEvent;
    private List<EventTrigger> eventTriggerList = new ArrayList<>();
    private EventTriggerAdapter mAdapter;

    private ArrayList<Uri> eventTriggerImagePaths;
    private final static String AUTHORITY = "org.havenapp.main.fileprovider";

    private Observer<Event> eventObserver = event -> {
        if (event != null) {
            onEventFetched(event);
        }
    };

    private Observer<List<EventTrigger>> eventTriggerListObserver = eventTriggerList -> {
        if (eventTriggerList != null) {
            onEventTriggerListFetched(eventTriggerList);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX);

        resourceManager = new ResourceManager(this);

        long eventId = getIntent().getLongExtra("eventid",-1);

        if (eventId != -1) {

            setUpRecyclerView();

            HavenEventDB.getDatabase(this).getEventDAO().findByIdAsync(eventId)
                    .observe(this, eventObserver);

            HavenEventDB.getDatabase(this).getEventTriggerDAO().getEventTriggerListAsync(eventId)
                    .observe(this, eventTriggerListObserver);

            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(view -> shareEvent());

        }
        else
            finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * On event fetched update {@link #mEvent} and set Activity title
     */
    private void onEventFetched(@NonNull Event event) {
        mEvent = event;
        setTitle(mEvent.getStartTime().toLocaleString());
    }

    /**
     * On event trigger list fetched for {@link #mEvent} update {@link #eventTriggerList},
     * {@link #eventTriggerImagePaths} and {@link #mAdapter} data set
     */
    private void onEventTriggerListFetched(@NonNull List<EventTrigger> eventTriggerList) {
        this.eventTriggerList = eventTriggerList;
        setEventTriggerImagePaths(eventTriggerList);
        mAdapter.setEventTriggers(eventTriggerList);
    }

    private void setUpRecyclerView() {
        mRecyclerView = findViewById(R.id.event_trigger_list);

        mAdapter = new EventTriggerAdapter(this, eventTriggerList,
                resourceManager, this);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setAdapter(mAdapter);

        // Handling swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //Remove swiped item from list and notify the RecyclerView

                final int position = viewHolder.getAdapterPosition();
                final EventTrigger eventTrigger = eventTriggerList
                        .get(viewHolder.getAdapterPosition());

                deleteEventTrigger (eventTrigger, position);


            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void deleteEventTrigger (final EventTrigger eventTrigger, final int position)
    {
        new EventTriggerDeleteAsync(() -> onEventDeleted(eventTrigger)).execute(eventTrigger);
    }

    private void onEventDeleted(EventTrigger eventTrigger) {
        Snackbar.make(mRecyclerView, R.string.event_trigger_deleted, Snackbar.LENGTH_SHORT)
                .setAction(R.string.undo, v -> new EventTriggerInsertAsync(eventTrigger::setId)
                        .execute(eventTrigger))
                .show();
    }

    private void shareEvent ()
    {
        String title = "Phoneypot: " + mEvent.getStartTime().toLocaleString();

        //need to "send multiple" to get more than one attachment
        final Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        emailIntent.setType("text/plain");

        emailIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        emailIntent.putExtra(Intent.EXTRA_TEXT, generateLog());
        //has to be an ArrayList
        ArrayList<Uri> uris = new ArrayList<>();
        //convert from paths to Android friendly Parcelable Uri's
        for (EventTrigger trigger : eventTriggerList)
        {
            // ignore triggers for which we do not have valid file/file-paths
            if (trigger.getMimeType() == null || trigger.getPath() == null)
                continue;

            File fileIn = new File(trigger.getPath());
            Uri u = Uri.fromFile(fileIn);
            uris.add(u);
        }

        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        startActivity(Intent.createChooser(emailIntent, getString(R.string.share_event_action)));
    }

    private String generateLog () {
        StringBuilder mEventLog = new StringBuilder();

        setTitle("Event @ " + mEvent.getStartTime().toLocaleString());

        for (EventTrigger eventTrigger : eventTriggerList) {

            mEventLog.append("Event Triggered @ ").append(eventTrigger.getTime().toString()).append("\n");

            String sType = eventTrigger.getStringType(resourceManager);

            mEventLog.append("Event Type: ").append(sType);
            mEventLog.append("\n==========================\n");
        }

        return mEventLog.toString();
    }

    @Override
    public void onVideoClick(EventTrigger eventTrigger) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.setData(Uri.parse("file://" + eventTrigger.getPath()));
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(EventTrigger eventTrigger) {
        shareMedia(eventTrigger);
    }

    @Override
    public void onImageClick(EventTrigger eventTrigger) {
        int startPosition = 0;

        /**
         for (int i = 0; i < eventTriggerImagePaths.size(); i++) {
         if (eventTriggerImagePaths.get(i).contains(eventTrigger.getPath())) {
         startPosition = i;
         break;
         }
         }**/

        ShareOverlayView overlayView = new ShareOverlayView(this);
        ImageViewer viewer = new ImageViewer.Builder<>(this, eventTriggerImagePaths)
                .setStartPosition(startPosition)
                .setOverlayView(overlayView)
                .show();
        overlayView.setImageViewer(viewer);
    }

    @Override
    public void onImageLongClick(EventTrigger eventTrigger) {
        shareMedia(eventTrigger);
    }

    private void shareMedia (EventTrigger eventTrigger) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(eventTrigger.getPath())));
        shareIntent.setType(eventTrigger.getMimeType());
        startActivity(shareIntent);
    }

    private void setEventTriggerImagePaths(List<EventTrigger> eventTriggerList) {
        this.eventTriggerImagePaths = new ArrayList<>();
        for (EventTrigger trigger : eventTriggerList)
        {
            if (trigger.getType() == EventTrigger.CAMERA
                    && (!TextUtils.isEmpty(trigger.getPath())))
            {
                Uri fileUri = FileProvider.getUriForFile(this, AUTHORITY,
                        new File(trigger.getPath()));

                eventTriggerImagePaths.add(fileUri);
            }
        }
    }
}

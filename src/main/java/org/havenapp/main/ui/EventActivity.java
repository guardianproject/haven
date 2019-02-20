package org.havenapp.main.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.stfalcon.frescoimageviewer.ImageViewer;

import org.apache.http.client.utils.URIBuilder;
import org.havenapp.main.R;
import org.havenapp.main.Utils;
import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.database.async.EventTriggerDeleteAsync;
import org.havenapp.main.database.async.EventTriggerInsertAsync;
import org.havenapp.main.model.Event;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.resources.IResourceManager;
import org.havenapp.main.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class EventActivity extends AppCompatActivity implements EventTriggerAdapter.EventTriggerClickListener {

    private IResourceManager resourceManager;
    private RecyclerView mRecyclerView;
    private Event mEvent;
    private List<EventTrigger> eventTriggerList = new ArrayList<>();
    private EventTriggerAdapter mAdapter;
    private Toolbar toolbar;

    private ArrayList<Uri> eventTriggerImagePaths;
    //private final static String AUTHORITY = "org.havenapp.main.fileprovider";

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
        toolbar = findViewById(R.id.toolbar);
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
        String title = mEvent.getStartTime().toLocaleString();
        setTitle(title);
        CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.toolbar_layout);
        collapsingToolbarLayout.setTitle(title);

        //((TextView)findViewById(R.id.toolbar_title)).setText(mEvent.getStartTime().toLocaleString());
    }

    /**
     * On event trigger list fetched for {@link #mEvent} update {@link #eventTriggerList},
     * {@link #eventTriggerImagePaths} and {@link #mAdapter} data set
     */
    private void onEventTriggerListFetched(@NonNull List<EventTrigger> eventTriggerList) {
        this.eventTriggerList = eventTriggerList;
        setEventTriggerImagePaths();
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

                final EventTrigger eventTrigger = eventTriggerList.get(viewHolder.getAdapterPosition());

                deleteEventTrigger (eventTrigger);


            }

        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);
    }

    private void deleteEventTrigger(final EventTrigger eventTrigger)
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

//        setTitle("Event @ " + mEvent.getStartTime().toLocaleString());

        for (EventTrigger eventTrigger : eventTriggerList) {

            mEventLog.append("Event Triggered @ ").append(
                     new SimpleDateFormat(Utils.DATE_TIME_PATTERN,
                            Locale.getDefault()).format(eventTrigger.getTime().toString())).append("\n");

            String sType = eventTrigger.getStringType(resourceManager);

            mEventLog.append("Event Type: ").append(sType);
            mEventLog.append("\n==========================\n");
        }

        return mEventLog.toString();
    }

    @Override
    public void onVideoClick(@NotNull EventTrigger eventTrigger) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.setData(Uri.fromFile(new File(eventTrigger.getPath())));
        startActivity(intent);
    }

    @Override
    public void onVideoLongClick(@NotNull EventTrigger eventTrigger) {
        shareMedia(eventTrigger);
    }

    @Override
    public void onImageClick(@NotNull EventTrigger eventTrigger, int position) {
        int startPosition = getPositionOfImagePath(position);

        ShareOverlayView overlayView = new ShareOverlayView(this);
        ImageViewer viewer = new ImageViewer.Builder<>(this, eventTriggerImagePaths)
                .setStartPosition(startPosition)
                .setOverlayView(overlayView)
                .show();
        overlayView.setImageViewer(viewer);
    }

    @Override
    public void onImageLongClick(@NotNull EventTrigger eventTrigger) {
        shareMedia(eventTrigger);
    }

    private void shareMedia (EventTrigger eventTrigger) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(eventTrigger.getPath())));
        shareIntent.setType(eventTrigger.getMimeType());
        startActivity(shareIntent);
    }

    private void setEventTriggerImagePaths() {
        this.eventTriggerImagePaths = new ArrayList<>();
        for (EventTrigger trigger : eventTriggerList)
        {
            if (trigger.getType() == EventTrigger.CAMERA
                    && (!TextUtils.isEmpty(trigger.getPath())))
            {
               eventTriggerImagePaths.add(Uri.fromFile(new File(trigger.getPath())));
            }
        }
    }

    private int getPositionOfImagePath(int position) {
        int pos = -1;
        for (int i = 0; i <= position; i++) {
            if (eventTriggerList.get(i).getType() == EventTrigger.CAMERA &&
                    (!TextUtils.isEmpty(eventTriggerList.get(i).getPath()))) {
                pos++;
            }
        }
        return pos;
    }
}

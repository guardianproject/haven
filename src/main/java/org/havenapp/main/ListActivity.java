/*
 * Copyright (c) 2017 Nathanial Freitas
 *
 *   This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.havenapp.main;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.database.async.EventDeleteAllAsync;
import org.havenapp.main.database.async.EventDeleteAsync;
import org.havenapp.main.database.async.EventInsertAllAsync;
import org.havenapp.main.database.async.EventInsertAsync;
import org.havenapp.main.model.Event;
import org.havenapp.main.resources.IResourceManager;
import org.havenapp.main.resources.ResourceManager;
import org.havenapp.main.service.RemoveDeletedFilesJob;
import org.havenapp.main.service.SignalSender;
import org.havenapp.main.ui.EventActivity;
import org.havenapp.main.ui.EventAdapter;
import org.havenapp.main.ui.PPAppIntro;

import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

import static org.havenapp.main.database.DbConstantsKt.DB_INIT_END;
import static org.havenapp.main.database.DbConstantsKt.DB_INIT_START;
import static org.havenapp.main.database.DbConstantsKt.DB_INIT_STATUS;

public class ListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private PreferenceManager preferences;
    private IResourceManager resourceManager;

    private final static int REQUEST_CODE_INTRO = 1001;

    private LiveData<List<Event>> eventListLD;

    private Observer<List<Event>> eventListObserver = events -> {
        if (events != null) {
            setEventListToRecyclerView(events);
            observeEvents(events);
        }
    };

    private Observer<Integer> eventCountObserver = count -> {
        if (count != null && count > events.size()) {
            showNonEmptyState();
        } else if (count != null && count == 0) {
            showEmptyState();
        }
    };

    private Observer<Pair<Long, Integer>> eventTriggerCountObserver = pair -> {
        if (pair != null && adapter != null && events != null) {
            int pos = -1;
            for (int  i = 0; i < events.size(); i++) {
                if (events.get(i).getId().equals(pair.getFirst())) {
                    pos = i;
                    break;
                }
            }
            if (pos != -1) {
                adapter.notifyItemChanged(pos);
            }
        }
    };

    private BroadcastReceiver dbBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getIntExtra(DB_INIT_STATUS, 0) == DB_INIT_START) {
                progressDialog = new ProgressDialog(ListActivity.this);
                progressDialog.setTitle(resourceManager.getString(R.string.please_wait));
                progressDialog.setMessage(resourceManager.getString(R.string.migrating_data));
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();
            } else if (intent.getIntExtra(DB_INIT_STATUS, 0) == DB_INIT_END) {
                if (progressDialog != null)
                    progressDialog.dismiss();
            }
        }
    };

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Log.d("Main", "onCreate");

        resourceManager = new ResourceManager(this);
        preferences = new PreferenceManager(this.getApplicationContext());
        recyclerView = findViewById(R.id.main_list);
        FloatingActionButton fab = findViewById(R.id.fab);
        Toolbar toolbar = findViewById(R.id.toolbar);
        final Drawable overflowIcon = toolbar.getOverflowIcon();
        overflowIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_ATOP);
        toolbar.setOverflowIcon(overflowIcon);
        setSupportActionBar(toolbar);
        LocalBroadcastManager.getInstance(this).registerReceiver(dbBroadcastReceiver,
                new IntentFilter(DB_INIT_STATUS));

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);


        // Handling swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //Remove swiped item from list and notify the RecyclerView

                final Event event = events.get(viewHolder.getAdapterPosition());
                deleteEvent(event);
            }

        };


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow);
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, Color.WHITE);
                DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                fab.setImageDrawable(drawable);
            }
        }


        fab.setOnClickListener(v -> {
            Intent i = new Intent(ListActivity.this, MonitorActivity.class);
            startActivity(i);
        });

        if (preferences.isFirstLaunch()) {
            showOnboarding();
        }

        initializeRecyclerViewComponents();

        fetchEventList();

        RemoveDeletedFilesJob.Companion.schedule();
    }

    private void initializeRecyclerViewComponents() {
        adapter = new EventAdapter(events, resourceManager);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(adapter);

        adapter.SetOnItemClickListener((view, position) -> {

            Intent i = new Intent(ListActivity.this, EventActivity.class);
            i.putExtra("eventid", events.get(position).getId());

            startActivity(i);
        });
    }

    private void setEventListToRecyclerView(@NonNull List<Event> events) {
        this.events = events;

        if (events.size() > 0) {
            findViewById(R.id.empty_view).setVisibility(View.GONE);
        }

        adapter.setEvents(events);
    }

    private void observeEvents(@NonNull List<Event> events) {
        for (Event event: events) {
            if (event.getEventTriggersCountLD() == null)
                continue;
            event.getEventTriggersCountLD().observe(this, eventTriggerCountObserver);
        }
    }

    private void fetchEventList() {
        try {
            eventListLD = HavenEventDB.getDatabase(this).getEventDAO().getAllEventDesc();
            eventListLD.observe(this, eventListObserver);
        } catch (SQLiteException sqe) {
            Log.d(getClass().getName(), "database not yet initiatied", sqe);
        }
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
    }

    private void showNonEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        findViewById(R.id.empty_view).setVisibility(View.GONE);
    }

    private void deleteEvent(final Event event)
    {
        new EventDeleteAsync(() -> onEventDeleted(event)).execute(event);
    }

    private void onEventDeleted(Event event) {
        Snackbar.make(recyclerView, resourceManager.getString(R.string.event_deleted), Snackbar.LENGTH_SHORT)
                .setAction(resourceManager.getString(R.string.undo),
                        v -> new EventInsertAsync(eventId -> {
                    event.setId(eventId);
                }).execute(event))
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_INTRO)
        {
            preferences.setFirstLaunch(false);
            Intent i = new Intent(ListActivity.this, MonitorActivity.class);
            startActivity(i);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resourceManager = new ResourceManager(this);
        HavenEventDB.getDatabase(this).getEventDAO().count().observe(this, eventCountObserver);
    }

    private void showOnboarding()
    {
        startActivityForResult(new Intent(this, PPAppIntro.class),REQUEST_CODE_INTRO);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this,SettingsActivity.class));
                break;
            case R.id.action_remove_all_events:
                removeAllEvents();
                break;
            case R.id.action_about:
                showOnboarding();
                break;
            case R.id.action_licenses:
                showLicenses();
                break;
            case R.id.action_test_notification:
                testNotifications();
                break;
            case R.id.action_run_cleanup_job:
                runCleanUpJob();
                break;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dbBroadcastReceiver);
    }

    private void removeAllEvents()
    {
        final List<Event> removedEvents = new ArrayList<>(events);
        new EventDeleteAllAsync(() -> onAllEventsRemoved(removedEvents)).execute(removedEvents);
    }

    private void runCleanUpJob() {
        RemoveDeletedFilesJob.Companion.runNow();
    }

    private void onAllEventsRemoved(List<Event> removedEvents) {
        Snackbar.make(recyclerView, resourceManager.getString(R.string.events_deleted), Snackbar.LENGTH_SHORT)
                .setAction(resourceManager.getString(R.string.undo),
                        v -> new EventInsertAllAsync(eventIdList -> {
                    for (int i = 0; i < removedEvents.size(); i++) {
                        Event event = removedEvents.get(i);
                        event.setId(eventIdList.get(i));
                    }
                }).execute(removedEvents)
                )
                .show();
    }

    private void showLicenses ()
    {
        new LibsBuilder()
                //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withAboutAppName(resourceManager.getString(R.string.app_name))
                                //start the activity
                .start(this);
    }

    private void testNotifications ()
    {

        if (preferences.isSignalVerified()) {
            SignalSender sender = SignalSender.getInstance(this, preferences.getSignalUsername().trim());
            ArrayList<String> recip = new ArrayList<>();
            recip.add(preferences.getRemotePhoneNumber());
            sender.sendMessage(recip, resourceManager.getString(R.string.signal_test_message),
                    null, null);
        } else {
            Toast.makeText(this, getString(R.string.setup_signal_toast), Toast.LENGTH_SHORT).show();
        }
    }
}

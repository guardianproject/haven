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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
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
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import org.havenapp.main.service.RemoveDeletedFilesService;
import org.havenapp.main.service.SignalSender;
import org.havenapp.main.ui.EventActivity;
import org.havenapp.main.ui.EventAdapter;
import org.havenapp.main.ui.PPAppIntro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import static org.havenapp.main.database.DbConstantsKt.DB_INIT_END;
import static org.havenapp.main.database.DbConstantsKt.DB_INIT_START;
import static org.havenapp.main.database.DbConstantsKt.DB_INIT_STATUS;
import static org.havenapp.main.service.RemoveDeletedFilesServiceKt.SERVICE_TAG;

public class ListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FloatingActionButton fab;
    private Toolbar toolbar;
    private EventAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private PreferenceManager preferences;
    private IResourceManager resourceManager;

    private int modifyPos = -1;

    private int REQUEST_CODE_INTRO = 1001;

    private LiveData<List<Event>> eventListLD;

    private Observer<List<Event>> eventListObserver = events -> {
        if (events != null) {
            setEventListToRecyclerView(events);
        }
    };

    private Observer<Integer> eventCountObserver = count -> {
        if (count != null && count > events.size()) {
            fetchEventList();
            showNonEmptyState();
        } else if (count != null && count == 0) {
            showEmptyState();
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
        fab = findViewById(R.id.fab);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        LocalBroadcastManager.getInstance(this).registerReceiver(dbBroadcastReceiver,
                new IntentFilter(DB_INIT_STATUS));

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        if (savedInstanceState != null)
            modifyPos = savedInstanceState.getInt("modify");


        // Handling swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                //Remove swiped item from list and notify the RecyclerView

                final int position = viewHolder.getAdapterPosition();
                final Event event = events.get(viewHolder.getAdapterPosition());

                deleteEvent(event, position);


            }

        };


        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);


        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_play_arrow);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, Color.WHITE);
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);

            fab.setImageDrawable(drawable);

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

        scheduleCleanupJob(this);
    }

    private void initializeRecyclerViewComponents() {
        adapter = new EventAdapter(events, resourceManager);
        recyclerView.setVisibility(View.VISIBLE);
        recyclerView.setAdapter(adapter);

        adapter.SetOnItemClickListener((view, position) -> {

            Intent i = new Intent(ListActivity.this, EventActivity.class);
            i.putExtra("eventid", events.get(position).getId());
            modifyPos = position;

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

    private void fetchEventList() {
        try {
            eventListLD = HavenEventDB.getDatabase(this).getEventDAO().getAllEventDesc();
            eventListLD.observe(this, eventListObserver);
        } catch (SQLiteException sqe) {
            Log.d(getClass().getName(), "database not yet initiatied", sqe);
        }
    }

    private static void scheduleCleanupJob(Context context) {
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) {
            return;
        }

        FirebaseJobDispatcher dispatcher = new
                FirebaseJobDispatcher(new GooglePlayDriver(context));
        dispatcher.mustSchedule(createJob(dispatcher));
    }

    private static Job createJob(FirebaseJobDispatcher dispatcher) {
        return dispatcher.newJobBuilder()
                .setLifetime(Lifetime.FOREVER)
                .setService(RemoveDeletedFilesService.class)
                .setTag(SERVICE_TAG)
                .setReplaceCurrent(true)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(0, 24 * 60 * 60))
                .setRetryStrategy(RetryStrategy.DEFAULT_LINEAR)
                .setConstraints(Constraint.DEVICE_CHARGING)
                .build();
    }

    private void showEmptyState() {
        recyclerView.setVisibility(View.GONE);
        findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
    }

    private void showNonEmptyState() {
        recyclerView.setVisibility(View.VISIBLE);
        findViewById(R.id.empty_view).setVisibility(View.GONE);
    }

    private void deleteEvent (final Event event, final int position)
    {
        new EventDeleteAsync(() -> onEventDeleted(event, position)).execute(event);
        events.remove(position);
        adapter.notifyItemRemoved(position);
    }

    private void onEventDeleted(Event event, int position) {
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("modify", modifyPos);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        modifyPos = savedInstanceState.getInt("modify");
    }

    @Override
    protected void onResume() {
        super.onResume();

        resourceManager = new ResourceManager(this);
        HavenEventDB.getDatabase(this).getEventDAO().count().observe(this, eventCountObserver);

        if (modifyPos != -1) {
            //Event.set(modifyPos, Event.listAll(Event.class).get(modifyPos));
            adapter.notifyItemChanged(modifyPos);
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static String getDateFormat(long date) {
        return new SimpleDateFormat("dd MMM yyyy").format(new Date(date));
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
        final List<Event> removedEvents = new ArrayList<Event>(events);
        events.clear();
        adapter.notifyDataSetChanged();
        new EventDeleteAllAsync(() -> onAllEventsRemoved(removedEvents)).execute(removedEvents);
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

        if (!TextUtils.isEmpty(preferences.getSignalUsername())) {
            SignalSender sender = SignalSender.getInstance(this, preferences.getSignalUsername().trim());
            ArrayList<String> recip = new ArrayList<>();
            recip.add(preferences.getSmsNumber());
            sender.sendMessage(recip, resourceManager.getString(R.string.signal_test_message), null);
        }
        else if (!TextUtils.isEmpty(preferences.getSmsNumber())) {

            SmsManager manager = SmsManager.getDefault();

            StringTokenizer st = new StringTokenizer(preferences.getSmsNumber(),",");
            while (st.hasMoreTokens())
                manager.sendTextMessage(st.nextToken(), null, resourceManager.getString(R.string.signal_test_message), null, null);

        }
    }
}

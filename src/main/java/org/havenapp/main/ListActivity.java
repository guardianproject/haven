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
import android.content.Intent;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import org.havenapp.main.model.Event;
import org.havenapp.main.service.SignalSender;
import org.havenapp.main.ui.EventActivity;
import org.havenapp.main.ui.EventAdapter;
import org.havenapp.main.ui.PPAppIntro;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> events = new ArrayList<>();
    private PreferenceManager preferences;

    private int modifyPos = -1;

    private final static int REQUEST_CODE_INTRO = 1001;


    final private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Log.d("Main", "onCreate");

        preferences = new PreferenceManager(this.getApplicationContext());
        recyclerView = findViewById(R.id.main_list);
        FloatingActionButton fab = findViewById(R.id.fab);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(llm);

        if (savedInstanceState != null)
            modifyPos = savedInstanceState.getInt("modify");


        // Handling swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
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
            if (drawable != null) {
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable, Color.WHITE);
                DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
                fab.setImageDrawable(drawable);
            }
        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(ListActivity.this, MonitorActivity.class);
                startActivity(i);

            }
        });

        if (preferences.isFirstLaunch()) {
            showOnboarding();
        }

        try {
            events = Event.listAll(Event.class, "id DESC");

            if (events.size() > 0) {
                findViewById(R.id.empty_view).setVisibility(View.GONE);
            }

            adapter = new EventAdapter(ListActivity.this, events);
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAdapter(adapter);


            adapter.SetOnItemClickListener(new EventAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    Intent i = new Intent(ListActivity.this, EventActivity.class);
                    i.putExtra("eventid", events.get(position).getId());
                    modifyPos = position;

                    startActivity(i);
                }
            });
        } catch (SQLiteException sqe) {
            Log.d(getClass().getName(), "database not yet initiatied", sqe);
        }


    }

    private void deleteEvent (final Event event, final int position)
    {

        final Runnable runnableDelete = () -> event.delete();

        handler.postDelayed(runnableDelete,5000);
        events.remove(position);
        adapter.notifyItemRemoved(position);

        Snackbar.make(recyclerView, getString(R.string.event_deleted), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.undo), v -> {
                    handler.removeCallbacks(runnableDelete);
                    event.save();
                    events.add(position, event);
                    adapter.notifyItemInserted(position);
                    recyclerView.scrollToPosition(position);
                })
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

        final long newCount = Event.count(Event.class);

        if (newCount > events.size()) {
            events = Event.listAll(Event.class, "id DESC");
            adapter = new EventAdapter(ListActivity.this, events);
            recyclerView.setAdapter(adapter);

            adapter.SetOnItemClickListener(new EventAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {

                    Intent i = new Intent(ListActivity.this, EventActivity.class);
                    i.putExtra("eventid", events.get(position).getId());
                    modifyPos = position;

                    startActivity(i);
                }
            });
            /**
            // Just load the last added note (new)
            Event event = Event.last(Event.class);

            events.add(0,event);
            adapter.notifyItemInserted(0);
            adapter.notifyDataSetChanged();
            
            initialCount = newCount;
            **/

            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view).setVisibility(View.GONE);
        }
        else if (newCount == 0)
        {
            recyclerView.setVisibility(View.GONE);
            findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
        }

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

    private void removeAllEvents()
    {
        final List<Event> removedEvents = new ArrayList<>();
        final Runnable runnableDelete = new Runnable ()
        {
            public void run ()
            {
                for (Event event : removedEvents) {
                    event.delete();
                }
            }
        };

        for (int i = 0, size = events.size(); i < size; i++) {
            removedEvents.add(events.remove(0));
            adapter.notifyItemRemoved(0);
        }

        handler.postDelayed(runnableDelete, 3000);

        Snackbar.make(recyclerView, getString(R.string.events_deleted), Snackbar.LENGTH_SHORT)
                .setAction(getString(R.string.undo), v -> {
                    handler.removeCallbacks(runnableDelete);

                    for (Event event : removedEvents) {
                        event.save();
                        events.add(event);
                        adapter.notifyItemInserted(events.size() - 1);
                    }
                })
                .show();
    }

    private void showLicenses ()
    {
        new LibsBuilder()
                //provide a style (optional) (LIGHT, DARK, LIGHT_DARK_TOOLBAR)
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withAboutAppName(getString(R.string.app_name))
                                //start the activity
                .start(this);
    }

    private void testNotifications ()
    {

        if (!TextUtils.isEmpty(preferences.getSignalUsername())) {
            SignalSender sender = SignalSender.getInstance(this, preferences.getSignalUsername().trim());
            ArrayList<String> recip = new ArrayList<>();
            recip.add(preferences.getSmsNumber());
            sender.sendMessage(recip, getString(R.string.signal_test_message), null);
        }
        else if (!TextUtils.isEmpty(preferences.getSmsNumber())) {

            SmsManager manager = SmsManager.getDefault();

            StringTokenizer st = new StringTokenizer(preferences.getSmsNumber(),",");
            while (st.hasMoreTokens())
                manager.sendTextMessage(st.nextToken(), null, getString(R.string.signal_test_message), null, null);

        }
    }
}
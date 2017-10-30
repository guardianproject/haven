package info.guardianproject.phoneypot;

import android.database.sqlite.SQLiteException;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;

import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;
import info.guardianproject.phoneypot.service.WebServer;
import info.guardianproject.phoneypot.ui.EventActivity;
import info.guardianproject.phoneypot.ui.EventAdapter;
import info.guardianproject.phoneypot.ui.PPAppIntro;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class ListActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    FloatingActionButton fab;

    EventAdapter adapter;
    List<Event> events = new ArrayList<>();

    long initialCount;

    int modifyPos = -1;

    int REQUEST_CODE_INTRO = 1001;


    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        Log.d("Main", "onCreate");

        recyclerView = (RecyclerView) findViewById(R.id.main_list);
        fab = (FloatingActionButton) findViewById(R.id.fab);

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

            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_forward_white);
            drawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(drawable, Color.WHITE);
            DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);

            fab.setImageDrawable(drawable);

        }


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(ListActivity.this, MonitorActivity.class);
                startActivity(i);

            }
        });

        initialCount = Event.count(Event.class);

        if (initialCount <= 0) {

            showOnboarding();

        } else {
            recyclerView.setVisibility(View.VISIBLE);
            findViewById(R.id.empty_view).setVisibility(View.GONE);
        }

        try {
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
        } catch (SQLiteException sqe) {
            Log.d(getClass().getName(), "database not yet initiatied", sqe);
        }


    }

    private void deleteEvent (final Event event, final int position)
    {

        final Runnable runnableDelete = new Runnable ()
        {
            public void run ()
            {
                for (EventTrigger trigger : event.getEventTriggers())
                {
                    new File(trigger.getPath()).delete();
                    trigger.delete();
                }

            }
        };

        handler.postDelayed(runnableDelete,3000);

        events.remove(position);
        adapter.notifyItemRemoved(position);

        event.delete();
        initialCount -= 1;

        Snackbar.make(recyclerView, "Event deleted", Snackbar.LENGTH_SHORT)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handler.removeCallbacks(runnableDelete);
                        event.save();
                        events.add(position, event);
                        adapter.notifyItemInserted(position);
                        initialCount += 1;

                    }
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_INTRO)
        {
            Intent i = new Intent(ListActivity.this, MonitorActivity.class);
            i.putExtra("firsttime",true);
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

        if (newCount > initialCount) {

            // Just load the last added note (new)
            Event event = Event.last(Event.class);

            events.add(0,event);
            adapter.notifyItemInserted(0);
            adapter.notifyDataSetChanged();
            
            initialCount = newCount;

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

    /**
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_remote:
                enableRemoteAccess();
                break;
        }
        return true;
    }**/

}
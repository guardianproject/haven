package org.havenapp.main.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.havenapp.main.R;
import org.havenapp.main.model.Event;

import java.util.List;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventVH> {

    private Context context;
    private List<Event> events;

    private OnItemClickListener clickListener;

    public EventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;

    }


    @Override
    public EventVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(view);
    }

    @Override
    public void onBindViewHolder(EventVH holder, int position) {

        Event event = events.get(position);

        String title = event.getStartTime().toLocaleString();
        String desc = event.getEventTriggers().size() + " " + context.getString(R.string.detection_events);

        holder.title.setText(title);
        holder.note.setText(desc);

    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title, note;

        public EventVH(View itemView) {
            super(itemView);

           title = itemView.findViewById(R.id.event_item_title);
            note = itemView.findViewById(R.id.event_item_desc);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(v, getAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
    }

}

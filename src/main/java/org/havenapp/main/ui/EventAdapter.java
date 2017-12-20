package org.havenapp.main.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import org.havenapp.main.R;
import org.havenapp.main.model.Event;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventVH> {

    Context context;
    List<Event> events;

    OnItemClickListener clickListener;

    public EventAdapter(Context context, List<Event> events) {
        this.context = context;
        this.events = events;

    }


    @Override
    public EventVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        EventVH viewHolder = new EventVH(view);
        return viewHolder;
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

           title = (TextView) itemView.findViewById(R.id.event_item_title);
            note = (TextView) itemView.findViewById(R.id.event_item_desc);

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

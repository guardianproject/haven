package org.havenapp.main.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.havenapp.main.R;
import org.havenapp.main.model.Event;
import org.havenapp.main.resources.IResourceManager;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventVH> {

    private List<Event> events;
    private IResourceManager resourceManager;

    private OnItemClickListener clickListener;

    public EventAdapter(@NonNull List<Event> events, @NonNull IResourceManager resourceManager) {
        this.events = events;
        this.resourceManager = resourceManager;
    }


    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new EventVH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {

        Event event = events.get(position);

        String title = event.getStartTime().toLocaleString();
        String desc = event.getEventTriggerCount() + " " +
                resourceManager.getString(R.string.detection_events);

        holder.index.setText("#" + (position + 1));
        holder.title.setText(title);
        holder.note.setText(desc);

    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    class EventVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView index, title, note;

        EventVH(View itemView) {
            super(itemView);

            index = itemView.findViewById(R.id.index_number);
           title = itemView.findViewById(R.id.title);
            note = itemView.findViewById(R.id.event_item_desc);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            clickListener.onItemClick(v, getAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
        notifyDataSetChanged();
    }
}

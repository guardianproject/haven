package info.guardianproject.phoneypot.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTriggerAdapter extends RecyclerView.Adapter<EventTriggerAdapter.EventTriggerVH> {

    Context context;
    List<EventTrigger> eventTriggers;

    OnItemClickListener clickListener;

    public EventTriggerAdapter(Context context, List<EventTrigger> eventTriggers) {
        this.context = context;
        this.eventTriggers = eventTriggers;

    }


    @Override
    public EventTriggerVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.event_item, parent, false);
        EventTriggerVH viewHolder = new EventTriggerVH(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(EventTriggerVH holder, int position) {

        EventTrigger eventTrigger = eventTriggers.get(position);

        String title = eventTrigger.getStringType();
        String desc = eventTrigger.getTriggerTime().toString();

        holder.title.setText(title);
        holder.note.setText(desc);

    }

    @Override
    public int getItemCount() {
        return eventTriggers.size();
    }

    class EventTriggerVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title, note;

        public EventTriggerVH(View itemView) {
            super(itemView);

           title = (TextView) itemView.findViewById(R.id.event_item_title);
            note = (TextView) itemView.findViewById(R.id.event_item_desc);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {

            if (clickListener != null)
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

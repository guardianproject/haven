package info.guardianproject.phoneypot.ui;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.Event;
import info.guardianproject.phoneypot.model.EventTrigger;
import nl.changer.audiowife.AudioWife;

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

        holder.image.setVisibility(View.GONE);
        holder.extra.setVisibility(View.GONE);

        if (eventTrigger.getPath() != null)
        {
            if (eventTrigger.getType() == EventTrigger.CAMERA)
            {
                holder.image.setVisibility(View.VISIBLE);
                Picasso.with(context).load(new File(eventTrigger.getPath())).into(holder.image);

            }
            else if (eventTrigger.getType() == EventTrigger.MICROPHONE)
            {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );

                holder.extra.setVisibility(View.VISIBLE);
                AudioWife audioWife = new AudioWife();
                audioWife.init(context, Uri.fromFile(new File(eventTrigger.getPath())))
                        .useDefaultUi(holder.extra, inflater);

            }

        }


    }

    @Override
    public int getItemCount() {
        return eventTriggers.size();
    }

    class EventTriggerVH extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView title, note;
        ImageView image;
        ViewGroup extra;

        public EventTriggerVH(View itemView) {
            super(itemView);

           title = (TextView) itemView.findViewById(R.id.event_item_title);
            note = (TextView) itemView.findViewById(R.id.event_item_desc);
            image = (ImageView) itemView.findViewById(R.id.event_item_image);
            extra = (ViewGroup)itemView.findViewById(R.id.event_item_extra);

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

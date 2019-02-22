package org.havenapp.main.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.havenapp.main.R;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.resources.IResourceManager;
import org.havenapp.main.ui.viewholder.AudioVH;
import org.havenapp.main.ui.viewholder.EventTriggerVH;
import org.havenapp.main.ui.viewholder.ImageVH;
import org.havenapp.main.ui.viewholder.VideoVH;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import nl.changer.audiowife.AudioWife;

/**
 * Created by n8fr8 on 4/16/17.
 */

public class EventTriggerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context context;
    private IResourceManager resourceManager;
    private List<EventTrigger> eventTriggers;

    private EventTriggerClickListener eventTriggerClickListener;

    EventTriggerAdapter(Context context, @NonNull List<EventTrigger> eventTriggers,
                        IResourceManager resourceManager, EventTriggerClickListener eventTriggerClickListener) {
        this.context = context;
        this.resourceManager = resourceManager;
        this.eventTriggers = eventTriggers;
        this.eventTriggerClickListener = eventTriggerClickListener;
    }

    void setEventTriggers(@NonNull List<EventTrigger> eventTriggers) {
        this.eventTriggers = eventTriggers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        switch (viewType) {
            case EventTrigger.CAMERA_VIDEO:
                return new VideoVH(eventTriggerClickListener, context, resourceManager, parent);
            case EventTrigger.CAMERA:
                return new ImageVH(resourceManager, eventTriggerClickListener, parent);
            case EventTrigger.MICROPHONE:
                return new AudioVH(resourceManager, parent);
            case EventTrigger.ACCELEROMETER:
            case EventTrigger.LIGHT:
            case EventTrigger.PRESSURE:
            case EventTrigger.POWER:
            case EventTrigger.BUMP:
                return new EventTriggerVH(resourceManager, parent);
        }
        return new RecyclerView.ViewHolder(new View(context)) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        final EventTrigger eventTrigger = eventTriggers.get(position);

        if (eventTrigger.getPath() != null && eventTrigger.getType() != null)
        {
            switch (eventTrigger.getType()) {
                case EventTrigger.CAMERA_VIDEO:
                    ((VideoVH) holder).bind(eventTrigger, position);
                    break;
                case EventTrigger.CAMERA:
                    ((ImageVH) holder).bind(eventTrigger, position);
                    break;
                case EventTrigger.MICROPHONE:
                    ((AudioVH) holder).bind(eventTrigger, context, position);
                    break;
                case EventTrigger.ACCELEROMETER:
                case EventTrigger.BUMP:
                    ((EventTriggerVH) holder)
                            .bind(eventTrigger, resourceManager.getString(R.string.data_speed), position);
                    break;
                case EventTrigger.LIGHT:
                    ((EventTriggerVH) holder)
                            .bind(eventTrigger, resourceManager.getString(R.string.data_light), position);
                    break;
                case EventTrigger.PRESSURE:
                    ((EventTriggerVH) holder)
                            .bind(eventTrigger, resourceManager.getString(R.string.data_pressure), position);
                    break;
                case EventTrigger.POWER:
                    ((EventTriggerVH) holder)
                            .bind(eventTrigger, resourceManager.getString(R.string.data_power), position);
                    break;
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        AudioWife.getInstance().release();
    }

    @Override
    public int getItemCount() {
        return eventTriggers.size();
    }

    @Override
    public int getItemViewType(int position) {
        return eventTriggers.get(position).getType();
    }

    public interface EventTriggerClickListener extends VideoVH.VideoClickListener,
            ImageVH.ImageClickListener {}
}

package org.havenapp.main.ui.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.havenapp.main.R
import org.havenapp.main.model.EventTrigger
import org.havenapp.main.resources.IResourceManager

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 21/02/19
 **/
class EventTriggerVH(private val resourceManager: IResourceManager, viewGroup: ViewGroup)
    : RecyclerView.ViewHolder(LayoutInflater.from(viewGroup.context)
        .inflate(R.layout.item_event_trigger, viewGroup, false)) {

    private val triggerTitle = itemView.findViewById<TextView>(R.id.item_trigger_title)
    private val triggerDesc = itemView.findViewById<TextView>(R.id.item_trigger_desc)

    fun bind(eventTrigger: EventTrigger, string: String) {
        triggerTitle.text = eventTrigger.getStringType(resourceManager)
        triggerDesc.text = """${eventTrigger.time?.toLocaleString() ?: ""}\n$string: ${eventTrigger.path}"""
    }
}

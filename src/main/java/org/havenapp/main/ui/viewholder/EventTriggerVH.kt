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

    private val indexNumber = itemView.findViewById<TextView>(R.id.index_number)
    private val triggerTitle = itemView.findViewById<TextView>(R.id.title)
    private val triggerDesc = itemView.findViewById<TextView>(R.id.item_trigger_desc)

    fun bind(eventTrigger: EventTrigger, string: String, position: Int) {
        indexNumber.text = "#${position + 1}"
        triggerTitle.text = eventTrigger.getStringType(resourceManager)
        triggerDesc.text = """${eventTrigger.time?.toLocaleString() ?: ""}
            |$string: ${eventTrigger.path}""".trimMargin()
    }
}

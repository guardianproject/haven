package org.havenapp.main.ui.viewholder

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.facebook.drawee.view.SimpleDraweeView
import org.havenapp.main.R
import org.havenapp.main.model.EventTrigger
import org.havenapp.main.resources.IResourceManager

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 21/02/19
 **/
class ImageVH(private val resourceManager: IResourceManager,
              private val listener: ImageClickListener, viewGroup: ViewGroup)
    : RecyclerView.ViewHolder(LayoutInflater.from(viewGroup.context)
        .inflate(R.layout.item_photo, viewGroup, false)) {

    private val indexNumber = itemView.findViewById<TextView>(R.id.index_number)
    private val imageTitle = itemView.findViewById<TextView>(R.id.title)
    private val imageDesc = itemView.findViewById<TextView>(R.id.item_camera_desc)
    private val imageView = itemView.findViewById<SimpleDraweeView>(R.id.item_camera_image)

    fun bind(eventTrigger: EventTrigger, position: Int) {
        indexNumber.text = "#${position + 1}"
        imageTitle.text = eventTrigger.getStringType(resourceManager)
        imageDesc.text = eventTrigger.time?.toLocaleString() ?: ""

        /**
        Uri fileUri = FileProvider.getUriForFile(
        context,
        AUTHORITY,
        new File(eventTrigger.getPath()));
        holder.image.setImageURI(fileUri);
         **/

        val fileUri = Uri.parse("file://" + eventTrigger.path!!)
        imageView.setImageURI(fileUri)


        imageView.setOnClickListener {
            listener.onImageClick(eventTrigger, position)
        }

        imageView.setOnLongClickListener {
            listener.onImageLongClick(eventTrigger)
            false
        }
    }

    interface ImageClickListener {
        fun onImageClick(eventTrigger: EventTrigger, position: Int)

        fun onImageLongClick(eventTrigger: EventTrigger)
    }
}

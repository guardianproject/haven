package org.havenapp.main.ui.viewholder

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import org.havenapp.main.R
import org.havenapp.main.model.EventTrigger
import org.havenapp.main.resources.IResourceManager

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 21/02/19
 **/
class VideoVH(private val clickListener: VideoClickListener, private val context: Context,
              private val resourceManager: IResourceManager, viewGroup: ViewGroup)
    : RecyclerView.ViewHolder(LayoutInflater.from(viewGroup.context)
        .inflate(R.layout.item_video, viewGroup, false)) {

    private val indexNumber = itemView.findViewById<TextView>(R.id.index_number)
    private val title = itemView.findViewById<TextView>(R.id.title)
    private val desc = itemView.findViewById<TextView>(R.id.item_video_desc)
    private val videoView = itemView.findViewById<VideoView>(R.id.item_video_view)

    fun bind(eventTrigger: EventTrigger, position: Int) {
        indexNumber.text = "#${position + 1}"
        title.text = eventTrigger.getStringType(resourceManager)
        desc.text = eventTrigger.time?.toLocaleString() ?: ""

        val bitmapD = BitmapDrawable(context.resources,
                ThumbnailUtils.createVideoThumbnail(eventTrigger.path.toString(),
                        MediaStore.Video.Thumbnails.FULL_SCREEN_KIND))
        videoView.background = bitmapD
        videoView.setOnClickListener {
            clickListener.onVideoClick(eventTrigger)
        }

        videoView.setOnLongClickListener {
            clickListener.onVideoLongClick(eventTrigger)
            false
        }
    }

    interface VideoClickListener {
        fun onVideoClick(eventTrigger: EventTrigger)

        fun onVideoLongClick(eventTrigger: EventTrigger)
    }
}

package org.havenapp.main.ui.viewholder

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.derlio.waveform.SimpleWaveformView
import com.github.derlio.waveform.soundfile.SoundFile
import nl.changer.audiowife.AudioWife
import org.havenapp.main.R
import org.havenapp.main.model.EventTrigger
import org.havenapp.main.resources.IResourceManager
import java.io.File

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 21/02/19
 **/
class AudioVH(private val resourceManager: IResourceManager, viewGroup: ViewGroup)
    : RecyclerView.ViewHolder(LayoutInflater.from(viewGroup.context)
        .inflate(R.layout.item_audio, viewGroup, false)) {

    private val indexNumber = itemView.findViewById<TextView>(R.id.index_number)
    private val audioTitle = itemView.findViewById<TextView>(R.id.title)
    private val audioDesc = itemView.findViewById<TextView>(R.id.item_audio_desc)
    private val waveFormView = itemView.findViewById<SimpleWaveformView>(R.id.item_sound)
    private val playerContainer = itemView.findViewById<LinearLayout>(R.id.item_player_container)

    fun bind(eventTrigger: EventTrigger, context: Context, position: Int) {
        indexNumber.text = "#${position + 1}"
        audioTitle.text = eventTrigger.getStringType(resourceManager)
        audioDesc.text = eventTrigger.time?.toLocaleString() ?: ""

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        val fileSound = File(eventTrigger.path)
        try {
            val soundFile = SoundFile.create(fileSound.path, object : SoundFile.ProgressListener {
                var lastProgress = 0

                override fun reportProgress(fractionComplete: Double): Boolean {
                    val progress = (fractionComplete * 100).toInt()
                    if (lastProgress == progress) {
                        return true
                    }
                    lastProgress = progress

                    return true
                }
            })
            waveFormView.setAudioFile(soundFile)
            waveFormView.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        playerContainer.removeAllViews()

        AudioWife().init(context, Uri.fromFile(fileSound)).useDefaultUi(playerContainer, inflater)
    }
}

package org.havenapp.main.service

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

/**
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 04/11/18.
 */
class HavenJobCreator : JobCreator {
    override fun create(tag: String): Job? {
        return when (tag) {
            SERVICE_TAG -> {
                RemoveDeletedFilesJob()
            }
            else -> {
                null
            }
        }
    }
}

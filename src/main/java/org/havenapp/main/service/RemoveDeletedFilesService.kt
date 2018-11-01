package org.havenapp.main.service

import android.util.Log
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.havenapp.main.PreferenceManager
import org.havenapp.main.database.HavenEventDB
import org.havenapp.main.model.EventTrigger
import java.io.File

/**
 * A [JobService] to delete files related to deleted logs.
 * <p>
 * Created by Arka Prava Basu <arka.basu@zomato.com> on 28/10/18.
 */

const val SERVICE_TAG = "HavenCleanupService"

class RemoveDeletedFilesService: JobService() {
    override fun onStopJob(job: JobParameters?): Boolean {
        Log.d(SERVICE_TAG, "Cleanup Service interrupted")
        return false
    }

    override fun onStartJob(job: JobParameters): Boolean {
        Log.d(SERVICE_TAG, "Starting Cleanup Service")
        Thread {
            // remove all deleted logs from disk and reschedule this task
            removeDeletedLogsFromDisk()
            jobFinished(job, true)
            Log.d(SERVICE_TAG, "Stopping Cleanup service")
        }

        return true
    }

    private fun removeDeletedLogsFromDisk() {
        val database = HavenEventDB.getDatabase(this)

        val eventList = database.getEventDAO().getAllEvent()
        val eventTriggerList = database.getEventTriggerDAO().getAllEventTriggers()

        // keep a list of all invalid event triggers
        val inValidEventTriggerList = mutableListOf<EventTrigger>()
        eventTriggerList.filter { it.mEventId !in eventList.map { it.id } }.mapTo(inValidEventTriggerList) { it }

        val currentFileList = mutableListOf<File>()
        val storageDir = File(PreferenceManager(this).defaultMediaStoragePath)
        currentFileList.addAll(storageDir.listFiles())

        val targetFileList = mutableListOf<File>()
        currentFileList.filter { it.absolutePath in inValidEventTriggerList.map { it.mPath } }.mapTo(targetFileList) { it }

        // delete these files from disk
        for (file in targetFileList) {
            file.delete()
        }

        // remove entry of all invalid event triggers from database
        for (inValidEventTrigger in inValidEventTriggerList) {
            database.getEventTriggerDAO().delete(inValidEventTrigger)
        }
    }
}

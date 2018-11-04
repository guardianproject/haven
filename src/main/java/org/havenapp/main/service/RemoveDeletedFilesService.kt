package org.havenapp.main.service

import android.os.Environment
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
        }.start()

        return true
    }

    private fun removeDeletedLogsFromDisk() {
        val database = HavenEventDB.getDatabase(this)

        val eventList = database.getEventDAO().getAllEvent()
        val eventTriggerList = database.getEventTriggerDAO().getAllEventTriggers()

        // delete all invalid event triggers
        val inValidEventTriggerList = mutableListOf<EventTrigger>()
        eventTriggerList.filter { it.mEventId !in eventList.map { it.id } }.mapTo(inValidEventTriggerList) { it }
        database.getEventTriggerDAO().deleteAll(inValidEventTriggerList)

        val targetFileList = mutableListOf<File>()
        getAllFilesToBeDeleted(targetFileList)

        Log.d(SERVICE_TAG, targetFileList.toString())

        // delete these files from disk
        targetFileList.filter { !it.isDirectory }.forEach { it.delete() }

        // delete empty directories remaining in our storage directory
        deleteEmptyDirs()
    }

    private fun getAllFilesToBeDeleted(targetFileList: MutableList<File>) {
        val storageDir = File(Environment.getExternalStorageDirectory(),
                PreferenceManager(this).baseStoragePath)

        if (!storageDir.exists())
            return

        val allFilePaths = getAllFilePathInStorage(storageDir)
        val eventTriggerPaths = getAllEventTriggerPath()

        allFilePaths.filter { it !in eventTriggerPaths }.mapTo(targetFileList) { File(it) }
    }

    private fun deleteEmptyDirs() {
        val storageDir = File(Environment.getExternalStorageDirectory(),
                PreferenceManager(this).baseStoragePath)

        if (!storageDir.exists())
            return

        val subDir = storageDir.list { dir, name -> File(dir, name).isDirectory }
        subDir.filter { it != null }.forEach {
            val dir = File(storageDir, it)
            if (dir.exists() && dir.isDirectory && dir.list().isEmpty())
                dir.delete()
        }
    }

    private fun getAllFilePathInStorage(storageDir: File): List<String> {
        val filePaths = mutableListOf<String>()

        val currentFileList = mutableListOf<File>()

        getAllFileInStorageDir(storageDir, currentFileList)
        currentFileList.addAll(storageDir.listFiles())

        currentFileList.mapTo(filePaths) {
            it.absolutePath
        }

        return filePaths
    }

    private fun getAllFileInStorageDir(storageDir: File, currentFileList: MutableList<File>) {
        if (!storageDir.exists() || !storageDir.isDirectory)
            return

        currentFileList.addAll(storageDir.listFiles())

        val subDir = storageDir.list { dir, name -> File(dir, name).isDirectory }
        subDir.filter { it != null }.forEach { getAllFileInStorageDir(File(storageDir, it), currentFileList) }
    }

    private fun getAllEventTriggerPath(): List<String> {
        val database = HavenEventDB.getDatabase(this)
        val eventTriggerPathList = mutableListOf<String>()
        database.getEventTriggerDAO().getAllEventTriggers().filter { it.mPath != null }
                .mapTo(eventTriggerPathList) { it.mPath!! }
        return eventTriggerPathList
    }
}

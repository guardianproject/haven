package org.havenapp.main.service

import android.os.Environment
import android.util.Log
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import org.havenapp.main.HavenApp
import org.havenapp.main.PreferenceManager
import org.havenapp.main.model.EventTrigger
import java.io.File

/**
 * A [Job] to delete files related to deleted logs.
 * <p>
 * Created by Arka Prava Basu <arkaprava94@gmail.com> on 28/10/18.
 */

const val SERVICE_TAG = "HavenCleanupJob"

class RemoveDeletedFilesJob: Job() {

    companion object {
        fun schedule(): Int {
            return JobRequest.Builder(SERVICE_TAG)
                    .setPeriodic(24 * 60 * 60 * 1000L) // run once every 24 hrs
                    .setRequiresCharging(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }

        fun runNow(): Int {
            return JobRequest.Builder(SERVICE_TAG)
                    .startNow()
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params): Result {
        Log.d(SERVICE_TAG, "Starting Cleanup. Job Id: ${params.id}")

        // remove all deleted logs from disk and reschedule this task
        removeDeletedLogsFromDisk()

        Log.d(SERVICE_TAG, "Stopping Cleanup. Job Id: ${params.id}")
        return Result.SUCCESS
    }

    private fun removeDeletedLogsFromDisk() {
        val database = HavenApp.getDataBaseInstance()

        val eventList = database.getEventDAO().getAllEvent()
        val eventTriggerList = database.getEventTriggerDAO().getAllEventTriggers()

        // delete all invalid event triggers
        val inValidEventTriggerList = mutableListOf<EventTrigger>()
        eventTriggerList.filter { it.eventId !in eventList.map { it.id } }.mapTo(inValidEventTriggerList) { it }
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
                PreferenceManager(HavenApp.getInstance()).baseStoragePath)

        if (!storageDir.exists())
            return

        val allFilePaths = getAllFilePathInStorage(storageDir)
        val eventTriggerPaths = getAllEventTriggerPath()

        allFilePaths.filter { it !in eventTriggerPaths }.mapTo(targetFileList) { File(it) }
    }

    private fun deleteEmptyDirs() {
        val storageDir = File(Environment.getExternalStorageDirectory(),
                PreferenceManager(HavenApp.getInstance()).baseStoragePath)

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

        storageDir.listFiles()?.let {
            currentFileList.addAll(it)
        }

        val subDir = storageDir.list { dir, name -> File(dir, name).isDirectory }
        subDir.filter { it != null }.forEach { getAllFileInStorageDir(File(storageDir, it), currentFileList) }
    }

    private fun getAllEventTriggerPath(): List<String> {
        val database = HavenApp.getDataBaseInstance()
        val eventTriggerPathList = mutableListOf<String>()
        database.getEventTriggerDAO().getAllEventTriggers().filter { it.path != null }
                .mapTo(eventTriggerPathList) { it.path!! }
        return eventTriggerPathList
    }
}

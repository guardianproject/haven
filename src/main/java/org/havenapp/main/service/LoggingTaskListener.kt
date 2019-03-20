package org.havenapp.main.service

import android.util.Log

/**
 * An implementation of [SignalExecutorTask.TaskResult] which just logs the
 * msg on [SignalExecutorTask.TaskResult.onSuccess] and [SignalExecutorTask.TaskResult.onFailure]
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 20/03/19
 **/
class LoggingTaskListener: SignalExecutorTask.TaskResult {
    override fun onSuccess(msg: String) {
        Log.i(LoggingTaskListener::class.java.simpleName, msg)
    }

    override fun onFailure(msg: String) {
        Log.i(LoggingTaskListener::class.java.simpleName, msg)
    }
}

package org.havenapp.main.service

import android.os.AsyncTask
import android.util.Log
import net.sourceforge.argparse4j.inf.Namespace
import org.asamk.signal.Main

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 20/03/19
 **/
private val TAG = SignalExecutorTask::class.java.simpleName
class SignalExecutorTask(private val commandMap: HashMap<String, Any>,
                         private val mainSignal: Main,
                         private val listener: TaskResult?)
    : AsyncTask<Unit, Unit, Int>() {

    override fun onPreExecute() {
        super.onPreExecute()
        Log.i(TAG, "Requesting with request map $commandMap")
    }

    override fun doInBackground(vararg params: Unit?): Int {
        val namespace = Namespace(commandMap)
        return mainSignal.handleCommands(namespace)
    }

    override fun onPostExecute(result: Int) {
        super.onPostExecute(result)
        Log.i(TAG, "result = $result for request map $commandMap")
        if (result == 0) {
            listener?.onSuccess("Success for request map $commandMap")
        } else {
            listener?.onFailure("Failure for request map $commandMap")
        }
    }

    interface TaskResult {
        /**
         * Indicates that the command executed successfully
         */
        fun onSuccess(msg: String)

        /**
         * A failure. Should probably return a failure msg
         * todo
         */
        fun onFailure(msg: String)
    }
}

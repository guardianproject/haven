package org.havenapp.main.service

import android.os.AsyncTask
import net.sourceforge.argparse4j.inf.Namespace
import org.asamk.signal.Main

/**
 * Created by Arka Prava Basu<arkaprava94@gmail.com> on 20/03/19
 **/
class SignalExecutorTask(private val commandMap: HashMap<String, Any>,
                         private val mainSignal: Main,
                         private val listener: TaskResult?)
    : AsyncTask<Unit, Unit, Int>() {

    override fun doInBackground(vararg params: Unit?): Int {
        val namespace = Namespace(commandMap)
        return mainSignal.handleCommands(namespace)
    }

    override fun onPostExecute(result: Int) {
        super.onPostExecute(result)
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

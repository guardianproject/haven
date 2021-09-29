package org.havenapp.main.senders

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import java.util.*

interface AlertSender {
    fun init (context: Context,  viewLifecycleOwner : LifecycleOwner)
    fun setCredentials(username: String, password: String, server: String)
    fun reset()
    fun register()
    fun verify(verificationCode: String?)
    fun stopHeartbeatTimer()
    fun startHeartbeatTimer(countMs: Int)
    fun sendMessage(
        recipients: ArrayList<String?>?, message: String?,
        attachment: String?
    )
}
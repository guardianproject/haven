package org.havenapp.main.senders

import java.util.*

interface AlertSender {
    fun setUsername(username: String?)
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
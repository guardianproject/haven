package org.havenapp.main.senders

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.Matrix
import org.matrix.android.sdk.api.auth.data.HomeServerConnectionConfig
import org.matrix.android.sdk.api.session.Session
import java.util.ArrayList

class MatrixSender : AlertSender {

    lateinit var mMatrix : Matrix
    lateinit var mLifecycle : LifecycleOwner
    lateinit var mSession : Session

    override fun init(context: Context, viewLifecycleOwner : LifecycleOwner) {
        mMatrix = Matrix.getInstance(context)
        mLifecycle = viewLifecycleOwner
    }

    /**
     * login with existing account
     */
    override fun setCredentials(username: String, password: String, homeserver : String) {

        // First, create a homeserver config
        // Be aware than it can throw if you don't give valid info
        val homeServerConnectionConfig = try {
            HomeServerConnectionConfig
                .Builder()
                .withHomeServerUri(Uri.parse(homeserver))
                .build()
        } catch (failure: Throwable) {
        //    Toast.makeText(requireContext(), "Home server is not valid", Toast.LENGTH_SHORT).show()
            return
        }
        // Then you can retrieve the authentication service.
        // Here we use the direct authentication, but you get LoginWizard and RegistrationWizard for more advanced feature
        //
        mLifecycle.lifecycleScope.launch {
            try {
                mMatrix.authenticationService().directAuthentication(
                    homeServerConnectionConfig,
                    username,
                    password,
                    "haven"
                )
            } catch (failure: Throwable) {
             //   Toast.makeText(requireContext(), "Failure: $failure", Toast.LENGTH_SHORT).show()
                null
            }?.let { session ->

             //   SessionHolder.currentSession = session
                mSession = session
                session.open()
                session.startSync(true)


            }
        }
    }

    override fun reset() {
        TODO("Not yet implemented")
    }

    override fun register() {
        //register new account for Haven instance... may need to show user captcha to solve
    }

    override fun verify(verificationCode: String?) {
        TODO("Not yet implemented")

    }

    override fun stopHeartbeatTimer() {
        //stop heartbeat emoji send
    }

    override fun startHeartbeatTimer(countMs: Int) {
        //send a heartbeat emoji on countMs interval
    }

    override fun sendMessage(
        recipients: ArrayList<String?>?,
        message: String?,
        attachment: String?
    ) {


        //val roomSum = mSession.getRoomSummaries(queryParams);

        // get rooms, if already in a direct room with recipient then send it there
        //otherwise create new direct room with recipient
        val room = mSession.getRoom("foo")

        room?.sendTextMessage(message.toString())

        //if aattachment isn't null
        //room?.sendMedia(attachment,false,room?.roomId)



    }
}
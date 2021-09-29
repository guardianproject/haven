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

    }

    override fun verify(verificationCode: String?) {
        TODO("Not yet implemented")
    }

    override fun stopHeartbeatTimer() {
        TODO("Not yet implemented")
    }

    override fun startHeartbeatTimer(countMs: Int) {
        TODO("Not yet implemented")
    }

    override fun sendMessage(
        recipients: ArrayList<String?>?,
        message: String?,
        attachment: String?
    ) {
        TODO("Not yet implemented")
    }
}
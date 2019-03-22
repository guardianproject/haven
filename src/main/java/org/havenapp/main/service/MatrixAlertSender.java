package org.havenapp.main.service;

import android.content.Context;

import org.matrix.androidsdk.rest.model.MatrixError;

import java.util.ArrayList;

import info.guardianproject.keanu.core.model.ChatSession;
import info.guardianproject.keanu.core.model.ChatSessionListener;
import info.guardianproject.keanu.core.model.ImConnection;
import info.guardianproject.keanu.core.model.Message;
import info.guardianproject.keanu.matrix.plugin.MatrixConnection;


public class MatrixAlertSender implements AlertSender {

    MatrixConnection mConn = null;
    Context mContext = null;
    String mUsername = null;
    String mPassword = null;

    long mAccountId = -1;
    long mProviderId = -1;

    public MatrixAlertSender (Context context, String username, String password)
    {
        mConn = new MatrixConnection(context);
        mContext = context;
        mUsername = username;
        mPassword = password;
    }

    @Override
    public void setUsername(String username) {


    }

    @Override
    public void reset() {

    }

    @Override
    public void register(boolean callEnabled) {

        mConn.register(mContext, mUsername, mPassword, new MatrixConnection.RegistrationListener() {
            @Override
            public void onRegistrationSuccess() {

            }

            @Override
            public void onRegistrationFailed(String s) {

            }

            @Override
            public void onResourceLimitExceeded(MatrixError matrixError) {

            }
        });
    }

    @Override
    public void verify(String verificationCode) {

    }

    @Override
    public void stopHeartbeatTimer() {

    }

    @Override
    public void startHeartbeatTimer(int countMs) {

    }

    @Override
    public void sendMessage(ArrayList<String> recipients, String body, String attachment) {


        if (mConn.getState() != ImConnection.LOGGED_IN)
        {
            mConn.loginAsync(mAccountId, mPassword, mProviderId, new MatrixConnection.LoginListener() {
                @Override
                public void onLoginSuccess() {

                    //try again!
                    sendMessage(recipients, body, attachment);
                }

                @Override
                public void onLoginFailed(String s) {

                }
            });
        }
        else
        {
           //find or join room to send to
            ChatSession session = null;

            Message message = new Message (body);

            //send message
            session.sendMessageAsync(message, new ChatSessionListener() {
                @Override
                public void onChatSessionCreated(ChatSession chatSession) {

                }

                @Override
                public void onMessageSendSuccess(Message message, String s) {

                }

                @Override
                public void onMessageSendFail(Message message) {

                }
            });
        }
    }
}

package org.havenapp.main.service.signal;

import android.content.Context;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import org.asamk.signal.Main;
import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.Utils;
import org.havenapp.main.service.SignalExecutorTask;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.internal.configuration.SignalCdnUrl;
import org.whispersystems.signalservice.internal.configuration.SignalServiceConfiguration;
import org.whispersystems.signalservice.internal.configuration.SignalServiceUrl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by n8fr8 on 11/6/17.
 */

public class SignalSender {

    private Context mContext;
    private static SignalSender mInstance;
    private String mUsername; //aka your signal phone number
    private String mPinCode;
    private CountDownTimer mCountdownTimer;
    private PreferenceManager preferences;
    private String messageString;
    private String prefix;
    private String suffix;
    private int interval;
    private int mAlertCount;

    private final String     URL         = "https://my.signal.server.com";
    private final SignalProtocolStore PROTOCOL_STORE = new HavenSignalProtocolStore();
    private final String     USER_AGENT  = "(Haven/Android)";

    private SignalServiceAccountManager mManager;
    private SignalServiceConfiguration mService;

    private SignalSender(Context context) throws FileNotFoundException {
        mContext = context;
        mAlertCount = 0;
        preferences = new PreferenceManager(mContext);
        prefix = preferences.getHeartbeatPrefix();
        suffix = preferences.getHeartbeatSuffix();
        messageString = preferences.getHeartbeatMonitorMessage();
        interval = preferences.getHeartbeatNotificationTimeMs() / 60000;

        SignalServiceUrl[] signalServiceUrls = {};
        SignalCdnUrl[] signalCdnUrls = {};
        mService = new SignalServiceConfiguration(signalServiceUrls,signalCdnUrls);
    }

    public static synchronized SignalSender getInstance (Context context) {
        if (mInstance == null)
        {
            try {
                mInstance = new SignalSender(context);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        return mInstance;
    }

    public void setCredentials (String username, String pinCode)
    {
        mUsername = username;
        mPinCode = pinCode;
    }

    public void reset ()
    {
        Main main = new Main(mContext);
        main.resetUser();
        mInstance = null;
    }

    public void register (boolean callEnabled, @Nullable SignalExecutorTask.TaskResult taskResult) {

        try {
            int deviceId = 9999;
            String userAgent = "(Haven/Android)";

            mManager = new SignalServiceAccountManager(mService, mUsername, mPinCode, deviceId, userAgent);

            if (callEnabled)
                mManager.requestVoiceVerificationCode();
            else
                mManager.requestSmsVerificationCode();

            taskResult.onSuccess("");
        }
        catch (Exception e)
        {
            taskResult.onFailure(e.getMessage());
        }

    }

    public void verify (final String receivedSmsVerificationCode, @Nullable SignalExecutorTask.TaskResult taskResult) {

        try {

            mManager.verifyAccountWithCode(receivedSmsVerificationCode, generateRandomSignalingKey(),
                    generateRandomInstallId(), false, mPinCode);

            taskResult.onSuccess("");

        } catch (Exception e) {

            taskResult.onFailure(e.getMessage());

        }

    }

    public void initKeys () throws InvalidKeyException, IOException {
        int signedPreKeyId = 9999;

        IdentityKeyPair identityKey        = KeyHelper.generateIdentityKeyPair();
        List<PreKeyRecord> oneTimePreKeys     = KeyHelper.generatePreKeys(0, 100);
        SignedPreKeyRecord signedPreKeyRecord = KeyHelper.generateSignedPreKey(identityKey, signedPreKeyId);

        mManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, oneTimePreKeys);

    }

    private String generateRandomSignalingKey ()
    {
        return UUID.randomUUID().toString();
    }

    private int generateRandomInstallId ()
    {
        return (int)(Math.random()*100000);
    }

    public void stopHeartbeatTimer ()
    {
        mAlertCount = 0;

        if (mCountdownTimer != null) {
            mCountdownTimer.cancel();
            mCountdownTimer = null;
            Log.d("HEARTBEAT MONITOR", "Stopped" );
        } else
            Log.d("HEARTBEAT MONITOR", "null");

    }

    public void startHeartbeatTimer (int countMs)
    {
        if (countMs <= 10000)
            countMs = 300000;

        mCountdownTimer =  new CountDownTimer(countMs,1000) {
            public void onTick(long millisUntilFinished) {
                // Log.d("HEARTBEAT MONITOR," seconds remaining: " + millisUntilFinished / 1000);
            }
            public void onFinish() {
                beatingHeart();
                start();
            }
        }.start();
    }

    private void beatingHeart () {

        int unicodeBeat = 0x1F493;
        String emojiString = new String(Character.toChars(unicodeBeat));
        messageString = preferences.getHeartbeatMonitorMessage();

        if (mAlertCount < 1 )
            messageString = prefix + " " + interval + " " + suffix + "\n" + mContext.getString(R.string.battery_level_msg_text) + ": " + Utils.getBatteryPercentage(mContext) + "%";
        else if (messageString != null)
            messageString = messageString + "\n" + mContext.getString(R.string.battery_level_msg_text) + ": " + Utils.getBatteryPercentage(mContext) + "%";
        else
            messageString = emojiString + "\n" + mContext.getString(R.string.battery_level_msg_text) + ": " + Utils.getBatteryPercentage(mContext) + "%";

        try {
            initHbMessage(messageString);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UntrustedIdentityException e) {
            e.printStackTrace();
        }
    }

    private void initHbMessage (String message) throws IOException, UntrustedIdentityException {
        if (!TextUtils.isEmpty(mUsername)) {
            mAlertCount ++;
            getInstance(mContext);
            ArrayList<String> recipient = new ArrayList<>();
            recipient.add(preferences.getRemotePhoneNumber());
            sendMessage(recipient, message,null, null);
        }
    }

    public void sendMessage (final ArrayList<String> recipients, final String message,
                             final String attachmentPath, @Nullable SignalExecutorTask.TaskResult listener) throws IOException, UntrustedIdentityException {


        SignalServiceMessageSender messageSender =
                new SignalServiceMessageSender(mService, mUsername, mPinCode, PROTOCOL_STORE, USER_AGENT, Optional.absent(), Optional.absent());

        if (attachmentPath != null) {

            File myAttachment     = new File(attachmentPath);
            FileInputStream attachmentStream = new FileInputStream(myAttachment);
            SignalServiceAttachment attachment       = SignalServiceAttachment.newStreamBuilder()
                    .withStream(attachmentStream)
                    .withContentType("image/jpeg")
                    .withLength(myAttachment.length())
                    .build();

            for (String recipient : recipients) {
                messageSender.sendMessage(new SignalServiceAddress(recipient),
                        SignalServiceDataMessage.newBuilder()
                                .withBody(message)
                                .withAttachment(attachment)
                                .build());
            }
        }
        else
        {
            for (String recipient: recipients) {
                messageSender.sendMessage(new SignalServiceAddress(recipient),
                        SignalServiceDataMessage.newBuilder()
                                .withBody(message)
                                .build());
            }
        }

    }



}

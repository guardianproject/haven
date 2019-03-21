package org.havenapp.main.service;

import android.content.Context;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

import net.sourceforge.argparse4j.inf.Namespace;

import org.asamk.signal.Main;
import org.havenapp.main.PreferenceManager;
import org.havenapp.main.R;
import org.havenapp.main.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by n8fr8 on 11/6/17.
 */

public class SignalSender {

    private Context mContext;
    private static SignalSender mInstance;
    private String mUsername; //aka your signal phone number
    private CountDownTimer mCountdownTimer;
    private PreferenceManager preferences;
    private String messageString;
    private String prefix;
    private String suffix;
    private int interval;
    private int mAlertCount;

    private SignalSender(Context context, String username)
    {
        mContext = context;
        mUsername = username;
        mAlertCount = 0;
        preferences = new PreferenceManager(mContext);
        prefix = preferences.getHeartbeatPrefix();
        suffix = preferences.getHeartbeatSuffix();
        messageString = preferences.getHeartbeatMonitorMessage();
        interval = preferences.getHeartbeatNotificationTimeMs() / 60000;
    }

    public static synchronized SignalSender getInstance (Context context, String username)
    {
        if (mInstance == null)
        {
            mInstance = new SignalSender(context, username);
        }

        return mInstance;
    }

    public void setUsername (String username)
    {
        mUsername = username;
    }

    public void reset ()
    {
        Main main = new Main(mContext);
        main.resetUser();
        mInstance = null;
    }

    public void register (boolean callEnabled, @Nullable SignalExecutorTask.TaskResult taskResult) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("username", mUsername);
        map.put("command", "register");
        if (!callEnabled)
            map.put("voice", false);
        else
            map.put("voice", true);

        execute(map, taskResult);
    }

    public void verify (final String verificationCode, @Nullable SignalExecutorTask.TaskResult taskResult) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("username", mUsername);
        map.put("command", "verify");
        map.put("verificationCode", verificationCode);

        execute(map, taskResult);
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

        initHbMessage(messageString);
    }

    private void initHbMessage (String message)
    {
        if (!TextUtils.isEmpty(mUsername)) {
            mAlertCount ++;
            getInstance(mContext, mUsername.trim());
            ArrayList<String> recipient = new ArrayList<>();
            recipient.add(preferences.getRemotePhoneNumber());
            sendMessage(recipient, message,null, null);
        }
    }

    public void sendMessage (final ArrayList<String> recipients, final String message,
                             final String attachment, @Nullable SignalExecutorTask.TaskResult listener) {

        HashMap<String, Object> map = new HashMap<>();
        map.put("username", mUsername);
        map.put("endsession",false);
        map.put("recipient", recipients);
        map.put("command", "send");
        map.put("message", message);

        if (attachment != null) {
            ArrayList<String> attachments = new ArrayList<>();
            attachments.add(attachment);
            map.put("attachment",attachments);
        }

        execute(map, listener);
    }

    private void execute (HashMap<String, Object> map,
                          @Nullable SignalExecutorTask.TaskResult taskResult) {
        Main main = new Main(mContext);
        new SignalExecutorTask(map, main, taskResult)
                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}

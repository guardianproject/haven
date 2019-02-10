package org.havenapp.main.service;

import android.content.Context;
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
        prefix = preferences.getHearbeatPrefix();
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
        Main mainSignal = new Main(mContext);
        mainSignal.resetUser();
        mInstance = null;
    }

    public void register (boolean callEnabled)
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", mUsername);
                map.put("command", "register");
                if (!callEnabled)
                    map.put("voice", false);
                else
                    map.put("voice", true);

                Namespace ns = new Namespace(map);
                mainSignal.handleCommands(ns);
            }
        });
    }

    public void verify (final String verificationCode)
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", mUsername);
                map.put("command", "verify");
                map.put("verificationCode", verificationCode);

                Namespace ns = new Namespace(map);
                mainSignal.handleCommands(ns);
            }
        });
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
                try {
                    beatingHeart();
                } catch(Throwable e) {
                    e.printStackTrace();
                }
                start();
            }
        }.start();
    }

    private void beatingHeart () {
        int unicodeBeat = 0x1F493;
        String emojiString = new String(Character.toChars(unicodeBeat));
        messageString = preferences.getHeartbeatMonitorMessage();

        /**
         * Use compiler for optimized concatenation.
         * Send an explanatory message first, then the unicode symbol.
         * Ensure above message sent before updating count.
         * Check for a custom message, send that instead.
         **/

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
            getInstance(mContext, mUsername.trim());
            ArrayList<String> recipient = new ArrayList<>();
            recipient.add(preferences.getSmsNumber());
            sendMessage(recipient, message,null);
        } else if (!TextUtils.isEmpty(preferences.getSmsNumber())) {
            SmsManager manager = SmsManager.getDefault();
            StringTokenizer st = new StringTokenizer(preferences.getSmsNumber(),",");
            while (st.hasMoreTokens())
                manager.sendTextMessage(st.nextToken(), null, message, null, null);
        }

        mAlertCount ++; //moved outside of the send functions for now
        Log.d("HEARTBEAT MONITOR", "Sent: " + message);
    }

    public void sendMessage (final ArrayList<String> recipients, final String message, final String attachment)
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", mUsername);
                map.put("endsession",false);
                map.put("recipient", recipients);
                map.put("command", "send");
                map.put("message", message);

                if (attachment != null)
                {
                    ArrayList<String> attachments = new ArrayList<>();
                    attachments.add(attachment);
                    map.put("attachment",attachments);
                }

                Namespace ns = new Namespace(map);
                mainSignal.handleCommands(ns);
            }
        });
    }

    private void execute (Runnable runnable)
    {
        new Thread (runnable).start();
    }
}

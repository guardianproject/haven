package org.havenapp.main.service;

import android.content.Context;

import net.sourceforge.argparse4j.inf.Namespace;

import org.asamk.signal.Main;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by n8fr8 on 11/6/17.
 */

public class SignalSender {

    private Context mContext;
    private static SignalSender mInstance;
    private String mUsername; //aka your signal phone number

    private SignalSender(Context context, String username)
    {
        mContext = context;
        mUsername = username;
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

    public void register ()
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", mUsername);
                map.put("command", "register");
                map.put("voice", false);

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

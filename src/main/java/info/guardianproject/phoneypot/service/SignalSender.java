package info.guardianproject.phoneypot.service;

import android.content.Context;

import net.sourceforge.argparse4j.inf.Namespace;

import org.asamk.Signal;
import org.asamk.signal.Main;

import java.util.HashMap;

/**
 * Created by n8fr8 on 11/6/17.
 */

public class SignalSender {

    private Context mContext;
    private static SignalSender mInstance;

    private SignalSender(Context context)
    {
        mContext = context;
    }

    public static synchronized SignalSender getInstance (Context context)
    {
        if (mInstance == null)
        {
            mInstance = new SignalSender(context);
        }

        return mInstance;
    }

    public void register (final String phoneNumber)
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", phoneNumber);
                map.put("command", "register");
                map.put("voice", false);

                Namespace ns = new Namespace(map);
                mainSignal.handleCommands(ns);
            }
        });
    }

    public void verify (final String phoneNumber, final String verificationCode)
    {
        execute (new Runnable() {
            public void run() {
                Main mainSignal = new Main(mContext);
                HashMap<String, Object> map = new HashMap<>();

                map.put("username", phoneNumber);
                map.put("command", "verify");
                map.put("verificationCode", verificationCode);

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

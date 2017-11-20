package info.guardianproject.phoneypot;

import android.app.Application;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.util.Log;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.orm.SugarApp;
import com.orm.SugarContext;

import java.io.IOException;
import java.util.prefs.Preferences;

import info.guardianproject.phoneypot.service.WebServer;

/**
 * Created by n8fr8 on 8/9/17.
 */

public class PhoneyPotApp extends MultiDexApplication {


    /*
    ** Onion-available Web Server for optional remote access
     */
    WebServer mOnionServer = null;

    PreferenceManager mPrefs = null;

    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = new PreferenceManager(this);

        Fresco.initialize(this);
        SugarContext.init(this);

        if (mPrefs.getRemoteAccessActive())
            startServer();

    }


    public void startServer ()
    {
        if (mOnionServer == null || (!mOnionServer.isAlive()))
        {
            try {
                mOnionServer = new WebServer();

                if (!TextUtils.isEmpty(mPrefs.getRemoteAccessCredential()))
                    mOnionServer.setPassword(mPrefs.getRemoteAccessCredential());
            } catch (IOException ioe) {
                Log.e("OnioNServer", "unable to start onion server", ioe);
            }
        }
    }

    public void stopServer ()
    {
        if (mOnionServer != null && mOnionServer.isAlive())
        {
            mOnionServer.stop();
        }
    }
}

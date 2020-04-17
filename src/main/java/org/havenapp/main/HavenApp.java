/*
 * Copyright (c) 2017 Nathanial Freitas
 *
 *   This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.havenapp.main;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

import com.evernote.android.job.JobManager;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.nativecode.ImagePipelineNativeLoader;

import org.havenapp.main.database.HavenEventDB;
import org.havenapp.main.service.HavenJobCreator;
import org.havenapp.main.service.WebServer;

import java.io.IOException;

public class HavenApp extends MultiDexApplication {


    /*
    ** Onion-available Web Server for optional remote access
     */
    private WebServer mOnionServer = null;

    private PreferenceManager mPrefs = null;

    private static HavenEventDB dataBaseInstance = null;

    private static HavenApp havenApp;

    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = new PreferenceManager(this);

        ImagePipelineConfig.Builder b = ImagePipelineConfig.newBuilder(this);
        ImagePipelineConfig config = b
                .setProgressiveJpegConfig(new SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build();

        Fresco.initialize(this,config);

        try {
            ImagePipelineNativeLoader.load();
        } catch (UnsatisfiedLinkError e) {
            Fresco.shutDown();
            b.experiment().setNativeCodeDisabled(true);
            config = b.build();
            Fresco.initialize(this, config);
            e.printStackTrace();
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        if (mPrefs.getRemoteAccessActive())
            startServer();

        havenApp = this;
        dataBaseInstance = HavenEventDB.getDatabase(this);

        JobManager.create(this).addJobCreator(new HavenJobCreator());
    }


    public void startServer ()
    {
        if (mOnionServer == null || (!mOnionServer.isAlive()))
        {
            if ( mPrefs.getRemoteAccessCredential() != null) {
                try {
                    mOnionServer = new WebServer(this, mPrefs.getRemoteAccessCredential());
                } catch (IOException ioe) {
                    Log.e("OnioNServer", "unable to start onion server", ioe);
                }
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

    @NonNull
    public static HavenApp getInstance() {
        return havenApp;
    }

    @NonNull
    public static HavenEventDB getDataBaseInstance() {
        return dataBaseInstance;
    }
}

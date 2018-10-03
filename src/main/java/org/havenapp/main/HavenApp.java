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

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;

import org.havenapp.main.service.WebServer;

import java.io.IOException;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.multidex.MultiDexApplication;

public class HavenApp extends MultiDexApplication {


    /*
    ** Onion-available Web Server for optional remote access
     */
    private WebServer mOnionServer = null;

    private PreferenceManager mPrefs = null;

    @Override
    public void onCreate() {
        super.onCreate();

        SugarContext.init(getApplicationContext());

        SchemaGenerator schemaGenerator = new SchemaGenerator(this);
        schemaGenerator.createDatabase(new SugarDb(this).getDB());

        mPrefs = new PreferenceManager(this);

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setProgressiveJpegConfig(new SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build();

        Fresco.initialize(this,config);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        if (mPrefs.getRemoteAccessActive())
            startServer();
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
}

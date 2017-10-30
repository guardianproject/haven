package info.guardianproject.phoneypot;

import android.app.Application;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.orm.SugarApp;
import com.orm.SugarContext;

/**
 * Created by n8fr8 on 8/9/17.
 */

public class PhoneyPotApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Fresco.initialize(this);
        SugarContext.init(this);
    }
}

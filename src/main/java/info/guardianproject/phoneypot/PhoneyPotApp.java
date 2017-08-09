package info.guardianproject.phoneypot;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.orm.SugarApp;

/**
 * Created by n8fr8 on 8/9/17.
 */

public class PhoneyPotApp extends SugarApp {

    @Override
    public void onCreate() {
        super.onCreate();

        Fresco.initialize(this);
    }
}

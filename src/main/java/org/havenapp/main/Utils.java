package org.havenapp.main;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anupam Das (opticod) on 28/12/17.
 * <p>
 * Class containing util functions which will be used multiple times throughout the app.
 */

class Utils {
    static String getTimerText(long milliseconds) {
        String timerText;
        if (TimeUnit.MILLISECONDS.toHours(milliseconds) % 24 == 0) {
            if (TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60 == 0) {
                timerText = String.format(Locale.getDefault(), "%02ds",
                        TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
            } else {
                timerText = String.format(Locale.getDefault(), "%02dm %02ds",
                        TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60,
                        TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
            }
        } else {
            timerText = String.format(Locale.getDefault(), "%02dh %02dm %02ds",
                    TimeUnit.MILLISECONDS.toHours(milliseconds) % 24,
                    TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60,
                    TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60);
        }

        return timerText;
    }
}

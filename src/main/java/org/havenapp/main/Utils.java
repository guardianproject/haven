package org.havenapp.main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Anupam Das (opticod) on 28/12/17.
 * <p>
 * Class containing util functions which will be used multiple times throughout the app.
 */

public class Utils {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss.SSS";

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

    /**
     * Get a user friendly date and time representation from a given {@link Date}.
     * The default {@link Locale} is used.
     *
     * @param date concerned {@link Date} instance
     * @return a string of the format "yyyy-MM-dd_HH-mm-ss.SSS" for the corresponding date
     */
    public static String getDateTime(Date date) {
        return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(date);
    }
}

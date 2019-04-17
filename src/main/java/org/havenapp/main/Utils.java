package org.havenapp.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.inputmethod.InputMethodManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;

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

    /**
     * Get the battery level from the device, from official docs:
     * https://developer.android.com/training/monitoring-device-state/battery-monitoring#MonitorLevel
     * @param context
     * @return an integer corresponding to the battery percentage without any symbols
     */
    public static int getBatteryPercentage(Context context) {

        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, iFilter);

        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

        float batteryPct = level / (float) scale;

        return (int) (batteryPct * 100);
    }

    public static void hideKeyboard(@NonNull Activity activity) {
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(),
                    0);
        }
    }
}

package org.havenapp.main.sensors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;

import org.havenapp.main.R;
import org.havenapp.main.Utils;
import org.havenapp.main.model.EventTrigger;
import org.havenapp.main.service.MonitorService;

/**
 * Created by n8fr8 on 10/31/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        // Can't use intent.getIntExtra(BatteryManager.EXTRA_STATUS), as the extra is not provided.
        // The code example at
        // https://developer.android.com/training/monitoring-device-state/battery-monitoring.html
        // is wrong
        // see https://stackoverflow.com/questions/10211609/problems-with-action-power-connected

        // explicitly check the intent action
        // avoids lint issue UnsafeProtectedBroadcastReceiver
        if(intent.getAction() == null) return;
        switch(intent.getAction()){
            case Intent.ACTION_POWER_CONNECTED:
                break;
            case Intent.ACTION_POWER_DISCONNECTED:
                break;
            default:
                return;
        }

        if (MonitorService.getInstance() != null
                && MonitorService.getInstance().isRunning()) {
            MonitorService.getInstance().alert(EventTrigger.POWER,
                    Utils.getBatteryPercentage(context) + "%" + " \n" +
                            context.getString(R.string.power_source_status) + " " +
                            getBatteryStatus(context));
        }
    }

    //Ref: https://developer.android.com/training/monitoring-device-state/battery-monitoring.html

    private String getBatteryStatus(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        String battStatus;
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
        boolean wirelessCharge = false;

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            wirelessCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS;

        if (usbCharge)
            battStatus = context.getString(R.string.power_source_status_usb);
        else if (acCharge)
            battStatus = context.getString(R.string.power_source_status_ac);
        else if (wirelessCharge)
            battStatus = context.getString(R.string.power_source_status_wireless);
        else battStatus = context.getString(R.string.power_disconnected);

        return battStatus;
    }
}

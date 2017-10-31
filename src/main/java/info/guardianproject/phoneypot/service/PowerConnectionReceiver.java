package info.guardianproject.phoneypot.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import info.guardianproject.phoneypot.R;
import info.guardianproject.phoneypot.model.EventTrigger;

/**
 * Created by n8fr8 on 10/31/17.
 */

public class PowerConnectionReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
        boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;

        MonitorService.getInstance().alert(EventTrigger.POWER,context.getString(R.string.status_charging) + isCharging + " USB:"+usbCharge + " AC:"+acCharge);

    }
}

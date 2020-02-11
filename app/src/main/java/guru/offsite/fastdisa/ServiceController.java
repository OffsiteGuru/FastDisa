package guru.offsite.fastdisa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class ServiceController extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Log.d("FastDisa", "ServiceController::onReceive");
        this.autoStart(context);
    }

    public void autoStart(Context context) {
        Log.d("FastDisa", "ServiceController::autoStart");
        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);

        Intent backgroundService = new Intent(context.getApplicationContext(), FastDisaService.class);

        if (sharedPref.getBoolean("EnableDisa", false)) {
            Log.d("FastDisa", "Starting Background Service");
            context.startForegroundService(backgroundService);
        } else {
            Log.d("FastDisa", "Stopping Background Service");
            context.stopService(backgroundService);
        }
    }
}

package guru.offsite.fastdisa;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class FastDisaService extends Service {

    private CallRedirectDisa callRedirectDisa = null;
    private NotificationManager mNM;

    private String NOTEID = "fastdisa_service";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create an IntentFilter instance.
        IntentFilter intentFilter = new IntentFilter();

        // Add New Outgoing Call action.
        intentFilter.addAction("android.intent.action.NEW_OUTGOING_CALL");

        // Set broadcast receiver priority. Default is 0. Range is 1000 though -1000.
        intentFilter.setPriority(100);

        // Create a new outgoing call broadcast receiver.
        callRedirectDisa = new CallRedirectDisa();

        // Register the broadcast receiver with the intent filter object.
        registerReceiver(callRedirectDisa, intentFilter);

        this.showNotification();

        Log.d("FastDisa", "Service onCreate: CallRedirectDisa is registered.");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister callRedirectDisa when destroy.
        if(callRedirectDisa!=null)
        {
            unregisterReceiver(callRedirectDisa);
            Log.d("FastDisa", "Service onDestroy: CallRedirectDisa is unregistered.");
        }
    }

    private void showNotification() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNM.createNotificationChannel(new NotificationChannel(NOTEID, "FastDisa Notifications", NotificationManager.IMPORTANCE_DEFAULT));
        Notification notification = new NotificationCompat.Builder(this, NOTEID)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_stat_name)
                .build();
        startForeground(101,  notification);
    }
}

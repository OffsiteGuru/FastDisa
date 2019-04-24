package guru.offsite.fastdisa;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;

import java.util.Date;
import java.util.Set;

public class RewriteLog extends BroadcastReceiver {

    private static String mLastState;

    @Override
    public void onReceive(Context context, Intent intent) {

        final Context thiscontext = context;

        // Check to see if FastDisa is enabled. Return if not.
        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        if (!sharedPref.getBoolean("EnableDisa", false)) {
            Log.d("FastDisa", "disabled");
            return;
        }

        // Get the phone state. If 'IDLE' we do the thing.
        // Also, we check against the previous state change, since we tend to get duplicates.
        Bundle bundle = intent.getExtras();
        String state = bundle.getString("state");
        if ( state.equals("IDLE") && !state.equals(mLastState) ) {
            mLastState = state;
            Log.d("FastDisa", "Phone is Idle");

            // Do we have a last number dialed stored? If so, pull it into a local string and
            // remove it from the preferences. We use this later to fix the log.
            final String realNumberDialed = sharedPref.getString("LastDialed", "");
            Log.d("FastDisa", "realNumberDialed: " + realNumberDialed);
            if ( !realNumberDialed.isEmpty() ) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("LastDialed", "");
                editor.apply();
            } else {
                Log.d("FastDisa", "Doesn't look like we just ended a FastDisa call");
                return;
            }

            // We change the last dialed number
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    long datedialed;
                    String columns[]=new String[] {
                            CallLog.Calls._ID,
                            CallLog.Calls.NUMBER,
                            CallLog.Calls.DATE,
                            CallLog.Calls.DURATION,
                            CallLog.Calls.TYPE};

                    Cursor c;
                    c = thiscontext.getContentResolver().query(Uri.parse("content://call_log/calls"), columns, null, null, "Calls._ID DESC");
                    c.moveToFirst();
                    String callid = c.getString(0);
                    String duration = c.getString(3);
                    datedialed=c.getLong(c.getColumnIndex(CallLog.Calls.DATE));
                    Log.i("CallLog","id: " + callid +", type: " + c.getString(4) + ", Call to number: "+ c.getString(1) +", registered at: "+new Date(datedialed).toString() + ", Duration: " + duration);

                    // Delete the last call logged
                    thiscontext.getContentResolver().delete(Uri.parse("content://call_log/calls"),CallLog.Calls._ID + "= ?", new String[] {callid});

                    // Insert the call back with correct data
                    ContentValues values = new ContentValues();
                    values.put(CallLog.Calls.NUMBER, realNumberDialed);
                    values.put(CallLog.Calls.DATE, datedialed);
                    values.put(CallLog.Calls.DURATION, duration);
                    values.put(CallLog.Calls.TYPE, CallLog.Calls.OUTGOING_TYPE);
                    values.put(CallLog.Calls.NEW, 1);
                    values.put(CallLog.Calls.CACHED_NAME, "");
                    values.put(CallLog.Calls.CACHED_NUMBER_TYPE, 0);
                    values.put(CallLog.Calls.CACHED_NUMBER_LABEL, "");
                    thiscontext.getContentResolver().insert(CallLog.Calls.CONTENT_URI, values);

                }
            }, 2000);

        } else {
            mLastState = state;
            Log.d("FastDisa", state + " = " + mLastState);
        }

        // This just outputs all the data for testing.
        Set<String> keys = bundle.keySet();
        for (String key : keys) {
            Log.i("FastDisa", key + "=" + bundle.getString(key));
        }
    }
}

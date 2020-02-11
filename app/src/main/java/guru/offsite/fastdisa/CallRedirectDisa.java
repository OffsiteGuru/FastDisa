package guru.offsite.fastdisa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CallRedirectDisa extends BroadcastReceiver {
    String jsonString;
    int apiVersion = 0;
    String disaNumber;
    String status;
    String verbose;
    int exitCode = 1;
    String fallbackDisa;

    @Override
    public void onReceive(Context context, Intent intent) {

        // We're going to need this throughout
        TelephonyManager mTelephonyMgr = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        // Pull the originally dialed phone number into a string variable
        String originalRawNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        // Check if we're dialing an emergency number. GTFO of the way if we are.
        if (originalRawNumber.equals("911")) {
            Log.d("FastDisa", "EMERGENCY NUMBER DIALED");
            return;
        }

        // Clean the number up
        String workWithNumber = originalRawNumber;

        if (originalRawNumber.contains(",") || originalRawNumber.contains(";")) {
            Pattern pattern = Pattern.compile("[,;]");
            Matcher matcher = pattern.matcher(originalRawNumber);
            if (matcher.find()) {
                Log.d("FastDisa", "found at " + matcher.start());
                workWithNumber = workWithNumber.substring(0, matcher.start());
            }
        }
        final String originalNumber = workWithNumber.substring(workWithNumber.length() - 10);

        Log.d("FastDisa", "Original Number: " + originalNumber + " From dialed number: " + originalRawNumber);

        // Pull FastDisa preferences into variables.
        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        String PushURL = sharedPref.getString("PushURL", "https://");
        String PushPassword = sharedPref.getString("PushPassword", "");
        Boolean EnableDisa = sharedPref.getBoolean("EnableDisa", false);
        this.fallbackDisa = sharedPref.getString("FallbackDisa", null);

        // Check if we're enabled.
        if (!EnableDisa.booleanValue()) {
            Log.d("FastDisa", "FastDisa Disabled");
            return;
        }

        // Get the Caller ID and pull it into a variable
        String mPhoneNumber = mTelephonyMgr.getLine1Number();
        mPhoneNumber = mPhoneNumber.substring(mPhoneNumber.length() - 10);
        Log.d("fastdisa", "mynumber " + mPhoneNumber);


        // Create the http post and process the JSON returned
        PostForm postObj = new PostForm();

        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            this.jsonString = postObj.run(PushURL, mPhoneNumber, originalNumber, PushPassword);
            Log.d("Json String Returned", this.jsonString);
            JSONObject jo = new JSONObject(this.jsonString);
            this.apiVersion = jo.getInt("api_version");
            this.disaNumber = jo.getString("disanumber");
            this.status = jo.getString("status");
            this.verbose = jo.getString("verbose");
            this.exitCode = jo.getInt("exitcode");
            this.fallbackDisa = jo.getString("fallback_disanumber");
        } catch (Exception e) {
            Log.e("EXCEPTION", e.toString());
            String msg = "EXCEPTION!";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            this.setResultData(null);
        }

        if (this.exitCode == 0) {
            this.setResultData(this.disaNumber); // This changes the actual outgoing number
            String msg = "Fast Routing Outgoing Call";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("DisaNum", this.disaNumber);
            editor.putString("LastDialed", originalRawNumber);
            editor.putString("FallbackDisa", this.fallbackDisa);
            editor.apply();
        } else {
            String msg = "Slow Routing Outgoing Call";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            String newdialstring = this.fallbackDisa + "," + PushPassword + "#," + originalNumber + "#";
            Log.d("fastdisa", "dialing: " + newdialstring);
            this.setResultData(newdialstring);
        }
    }
}

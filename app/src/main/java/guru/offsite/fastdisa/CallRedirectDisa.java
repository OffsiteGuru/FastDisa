package guru.offsite.fastdisa;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONObject;

public class CallRedirectDisa extends BroadcastReceiver {
    String jsonString;
    int apiVersion = 0;
    String disaNumber;
    String status;
    String verbose;
    int exitCode = 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        // Pull FastDisa preferences into variables.
        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        String PushURL = sharedPref.getString("PushURL", "https://");
        String PushPassword = sharedPref.getString("PushPassword", "");
        Boolean EnableDisa = sharedPref.getBoolean("EnableDisa", false);

        // Check if we're configured and enabled.
        if (PushURL.equals("https://") || !EnableDisa.booleanValue()) {
            return; // TODO: This should be handled somehow. Should we stop the call?
        }

        // Pull the originally dialed phone number into a string variable
        final String originalNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        // Get the Caller ID and pull it into a variable
        TelephonyManager mTelephonyMgr = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = mTelephonyMgr.getLine1Number(); // TODO: We need the propper permissions for this to work. Check/Handle it.
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
        } catch (Exception e) {
            Log.e("EXCEPTION", e.toString());
        }

        if (this.exitCode == 0) {
            this.setResultData(this.disaNumber);
            String msg = "Intercepted outgoing call. Old number " + originalNumber + ", new number " + this.getResultData();
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("DisaNum", this.disaNumber);
            editor.putString("LastDialed", originalNumber);
            editor.apply();
        } else {
            String msg = "FAILURE! " + this.verbose;
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            this.setResultData("");
        }
    }
}

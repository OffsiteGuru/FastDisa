package guru.offsite.fastdisa;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;


public class CallRedirectDisa extends BroadcastReceiver {
    String jsonString;
    int apiVersion = 0;
    String disaNumber;
    String status;
    String verbose;
    int exitCode = 1;

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        String PushURL = sharedPref.getString("PushURL", "https://");
        String PushPassword = sharedPref.getString("PushPassword", "");
        String DisaNum = sharedPref.getString("DisaNum", "");
        Boolean EnableDisa = sharedPref.getBoolean("EnableDisa", false);


        if (PushURL.equals("https://") || !EnableDisa.booleanValue()) {
            return;
        }

        final String originalNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);

        TelephonyManager mTelephonyMgr = (TelephonyManager)context.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = mTelephonyMgr.getLine1Number();
        mPhoneNumber = mPhoneNumber.substring(mPhoneNumber.length() - 10);
        Log.d("fastdisa", "mynumber " + mPhoneNumber);


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

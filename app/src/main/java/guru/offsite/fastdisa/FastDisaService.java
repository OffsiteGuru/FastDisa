package guru.offsite.fastdisa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.StrictMode;
import android.telecom.CallRedirectionService;
import android.telecom.PhoneAccountHandle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.json.JSONObject;

public class FastDisaService extends CallRedirectionService {

    Context context;

    String jsonString;
    int apiVersion = 0;
    Uri disaNumber;
    String status;
    String verbose;
    int exitCode = 1;
    Uri fallbackDisa;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onPlaceCall(
            @NonNull Uri originalNumber,
            @NonNull PhoneAccountHandle initialPhoneAccount,
            boolean allowInteractiveResponse
    ) {
        Log.d("FastDisaService:onPlaceCall", "originalNumber: " + originalNumber);

        // Reset the exit code
        this.exitCode = 9;

        // We'll need these
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);

        // Get the Caller ID and pull it into a variable
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            String msg = "Cannot read device phone number.";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            cancelCall();
            return;
        }
        String devicePhoneNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID);
        devicePhoneNumber = devicePhoneNumber.substring(devicePhoneNumber.length() - 10);
        Log.d("FastDisaService:onPlaceCall", "mynumber " + devicePhoneNumber);

        // Pull FastDisa preferences into variables.
        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        String PushURL = sharedPref.getString("PushURL", "https://");
        String PushPassword = sharedPref.getString("PushPassword", "");
        String localFallbackDisa = sharedPref.getString("FallbackDisa", null);
        if(localFallbackDisa!= null) {
            this.fallbackDisa = Uri.parse(localFallbackDisa);
        }

        // Format original number
        String numberToDial = PhoneNumberUtils.formatNumberToE164(
                        Uri.decode(originalNumber.toString()),
                        telephonyManager.getNetworkCountryIso().toUpperCase()
                    );
        Log.d("FastDisaService:onPlaceCall", "numberToDial: " + numberToDial);

        // Connect to server, post vars
        PostForm postObj = new PostForm();
        try {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            this.jsonString = postObj.run(
                    PushURL,
                    devicePhoneNumber,
                    numberToDial,
                    PushPassword
            );
            Log.d("FastDisaService:onPlaceCall", "JSON Returned: " + this.jsonString);
            JSONObject jo = new JSONObject(this.jsonString);
            this.apiVersion = jo.getInt("api_version");
            this.disaNumber = Uri.parse("tel:" + jo.getString("disanumber"));
            this.status = jo.getString("status");
            this.verbose = jo.getString("verbose");
            this.exitCode = jo.getInt("exitcode");
            this.fallbackDisa = Uri.parse("tel:" + jo.getString("fallback_disanumber"));
        } catch (Exception e) {
            Log.e("FastDisaService:onPlaceCall", "EXCEPTION: " + e);
        }

        // Process Server Response. Fallback to manual dial (Slow Routing) if needed.
        if (this.exitCode == 0) {
            String msg = "Fast Routing Outgoing Call";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("DisaNum", this.disaNumber.toString());
            editor.putString("LastDialed", originalNumber.toString());
            editor.putString("FallbackDisa", this.fallbackDisa.toString());
            editor.apply();
        } else {
            String msg = "Slow Routing Outgoing Call";
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            this.disaNumber = Uri.parse(
                    this.fallbackDisa.toString() +
                            PhoneNumberUtils.PAUSE +
                            PushPassword +
                            Uri.encode("#") +
                            PhoneNumberUtils.PAUSE +
                            numberToDial +
                            Uri.encode("#"));
            Log.d("FastDisaService:onPlaceCall", "dialing: " + this.disaNumber.toString());
        }

        redirectCall2Disa(this.disaNumber, initialPhoneAccount);
    }

    public void redirectCall2Disa(Uri newNumber, PhoneAccountHandle phoneAccount) {
        Log.d("FastDisaService:redirectCall2Disa","Begin Function");
        redirectCall(newNumber, phoneAccount, false);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Populate Context
        this.context = getApplicationContext();
        Log.d("FastDisaService:onCreate", "Service Created");
    }

    @Override
    public void onDestroy() {
        Log.d("FastDisaService:onDestroy", "Service Destroyed");
        super.onDestroy();
    }
}

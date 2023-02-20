package guru.offsite.fastdisa;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        EditText string_url = findViewById(R.id.txt_url);
        EditText string_password = findViewById(R.id.txt_password);
        TextView textAppInfo = findViewById(R.id.txtAppInfo);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        Date buildDate = new Date(BuildConfig.BUILDTIMESTAMP);
        String buildDateLocal = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", buildDate).toString();

        String appInfo = "Version: " + versionName + " (build:" + versionCode + ")\r\n" + buildDateLocal;
        textAppInfo.setText(appInfo);

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        string_url.setText(sharedPref.getString("PushURL", "https://"));
        string_password.setText(sharedPref.getString("PushPassword", ""));

        // An ArrayList of our required permissions. If you add them to AndroidManifest.xml, add them here too.
        ArrayList<String> reqPermsArrayList = new ArrayList<>();
        reqPermsArrayList.add(Manifest.permission.POST_NOTIFICATIONS);
        reqPermsArrayList.add(Manifest.permission.RECEIVE_BOOT_COMPLETED);
        reqPermsArrayList.add(Manifest.permission.READ_PHONE_STATE);
        reqPermsArrayList.add(Manifest.permission.INTERNET);
        reqPermsArrayList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        reqPermsArrayList.add(Manifest.permission.ANSWER_PHONE_CALLS);
        reqPermsArrayList.add(Manifest.permission.FOREGROUND_SERVICE);
        reqPermsArrayList.add(Manifest.permission.READ_PHONE_NUMBERS);

        // Loop through the reqPermsArrayList, and check each one. Add the ones we don't have permission for to a new list.
        ArrayList<String> permsArrayList = new ArrayList<>();
        for (int i=0; i < reqPermsArrayList.size(); i++) {
            if (ContextCompat.checkSelfPermission(this, reqPermsArrayList.get(i)) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                Log.d("MainActivity:onCreate", "Permission: " + reqPermsArrayList.get(i) + " Missing");
                permsArrayList.add(reqPermsArrayList.get(i));
            }
        }

        // Request Permissions
        if (!permsArrayList.isEmpty()) {
            Log.d("MainActivity:onCreate", "Requesting Missing Permissions");
            String[] permsArray = permsArrayList.toArray(new String[permsArrayList.size()]);
            ActivityCompat.requestPermissions(this, permsArray, 43278);
        } else {
            Log.d("MainActivity:onCreate", "All Requested Permissions Granted");
        }
    }

    /** Called when user taps the Save button */
    public void saveSettings(View view) {
        EditText string_url = findViewById(R.id.txt_url);
        EditText string_password = findViewById(R.id.txt_password);

        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("PushURL", string_url.getText().toString());
        editor.putString("PushPassword", string_password.getText().toString());

        boolean save_return = editor.commit();

        if(save_return){
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            // Ask user to enable call redirection if not already granted
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (!roleManager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION);
                startActivityForResult(intent, 8112);
            }
            this.finish();
        }else{
            Toast.makeText(context, "Saving Failed!", Toast.LENGTH_LONG).show();
        }
    }

    /** Called when user taps the Cancel button */
    public void cancelSettings(View view) {
        Context context = getApplicationContext();
        Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show();
        this.finish();
    }
}

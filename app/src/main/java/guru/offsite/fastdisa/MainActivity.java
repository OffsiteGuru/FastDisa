package guru.offsite.fastdisa;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        EditText string_url = (EditText) findViewById(R.id.txt_url);
        EditText string_password = (EditText) findViewById(R.id.txt_password);
        ToggleButton toggle_disa = (ToggleButton) findViewById(R.id.toggle_enableDISA);
        TextView textAppInfo = (TextView) findViewById(R.id.txtAppInfo);

        int versionCode = BuildConfig.VERSION_CODE;
        String versionName = BuildConfig.VERSION_NAME;
        Date buildDate = new Date(BuildConfig.BUILDTIMESTAMP);
        String buildDateLocal = android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss a", buildDate).toString();

        String appInfo = "Version: " + versionName + " (build:" + versionCode + ")\r\n" + buildDateLocal;
        textAppInfo.setText(appInfo);

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        string_url.setText(sharedPref.getString("PushURL", "https://"));
        string_password.setText(sharedPref.getString("PushPassword", ""));
        toggle_disa.setChecked(sharedPref.getBoolean("EnableDisa", false));

        ArrayList<String> permsArrayList = new ArrayList<String>();

        // Check for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("FastDisa", "PROCESS_OUTGOING_CALLS permission missing");
            permsArrayList.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
            //ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS}, 1234);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("FastDisa", "PROCESS_OUTGOING_CALLS permission missing");
            permsArrayList.add(Manifest.permission.READ_PHONE_STATE);
            //ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_PHONE_STATE}, 4321);
        }

        // Request Permissions
        if (!permsArrayList.isEmpty()) {
            Log.d("FastDisa", "Requesting Permissions");
            String[] permsArray = (String[]) permsArrayList.toArray(new String[permsArrayList.size()]);
            ActivityCompat.requestPermissions(this, permsArray, 43278);
        }
    }

    /** Called when user taps the Save button */
    public void saveSettings(View view) {
        //Intent intent_save = new Intent(this, DisplayMessageActivity.class);
        EditText string_url = (EditText) findViewById(R.id.txt_url);
        EditText string_password = (EditText) findViewById(R.id.txt_password);
        ToggleButton toggle_disa = (ToggleButton) findViewById(R.id.toggle_enableDISA);


        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("PushURL", string_url.getText().toString());
        editor.putString("PushPassword", string_password.getText().toString());
        editor.putBoolean("EnableDisa", toggle_disa.isChecked());

        boolean save_return = editor.commit();

        Intent backgroundService = new Intent(getApplicationContext(), FastDisaService.class);

        if (sharedPref.getBoolean("EnableDisa", false)) {
            Log.d("FastDisa", "Starting Background Service");
            startService(backgroundService);
        } else {
            Log.d("FastDisa", "Stopping Background Service");
            stopService(backgroundService);
        }

        if(save_return){
            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
            this.finish();
        }else{
            Toast.makeText(context, "Saving Failed!", Toast.LENGTH_LONG).show();
        }
    }

    /** Called when user taps the Save button */
    public void cancelSettings(View view) {
        Context context = getApplicationContext();
        Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show();
        this.finish();
    }
}

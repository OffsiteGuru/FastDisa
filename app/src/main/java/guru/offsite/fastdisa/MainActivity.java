package guru.offsite.fastdisa;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.DateFormat;
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

package guru.offsite.fastdisa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
        EditText string_url = (EditText) findViewById(R.id.txt_url);
        EditText string_password = (EditText) findViewById(R.id.txt_password);
        EditText string_disanum = (EditText) findViewById(R.id.txt_disaNum);
        ToggleButton toggle_disa = (ToggleButton) findViewById(R.id.toggle_enableDISA);

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        string_url.setText(sharedPref.getString("PushURL", "https://"));
        string_password.setText(sharedPref.getString("PushPassword", ""));
        string_disanum.setText(sharedPref.getString("DisaNum", ""));
        toggle_disa.setChecked(sharedPref.getBoolean("EnableDisa", false));
    }

    /** Called when user taps the Save button */
    public void saveSettings(View view) {
        //Intent intent_save = new Intent(this, DisplayMessageActivity.class);
        EditText string_url = (EditText) findViewById(R.id.txt_url);
        EditText string_password = (EditText) findViewById(R.id.txt_password);
        EditText string_disanum = (EditText) findViewById(R.id.txt_disaNum);
        ToggleButton toggle_disa = (ToggleButton) findViewById(R.id.toggle_enableDISA);


        Context context = getApplicationContext();

        SharedPreferences sharedPref = context.getSharedPreferences("guru.offsite.fastdisa", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("PushURL", string_url.getText().toString());
        editor.putString("PushPassword", string_password.getText().toString());
        editor.putString("DisaNum", string_disanum.getText().toString());
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

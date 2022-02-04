package com.example.k_trader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.k_trader.base.NumberTextWatcherForThousand;

public class SettingActivity extends AppCompatActivity {
    Button btnSave;
    EditText txtApiKey;
    EditText txtApiSecret;
    EditText txtApiUnitPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSave = findViewById(R.id.buttonSave);
        txtApiKey = findViewById(R.id.editTextApiKey);
        txtApiSecret = findViewById(R.id.editTextApiSecret);
        txtApiUnitPrice = findViewById(R.id.editTextUnitPrice);
        txtApiUnitPrice.addTextChangedListener(new NumberTextWatcherForThousand(txtApiUnitPrice));

        // Load app settings (data/data/(package_name)/shared_prefs/SharedPreference)
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        txtApiKey.setText(sharedPreferences.getString("API_KEY", ""));
        txtApiSecret.setText(sharedPreferences.getString("API_SECRET", ""));
        txtApiUnitPrice.setText(Integer.toString(sharedPreferences.getInt("UNIT_PRICE", 0)));

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences.Editor prefsEditr = sharedPreferences.edit();
//                Log.e("TAG", txtApiKey.getText().toString());
//                Toast.makeText(SettingActivity.this, txtApiKey.getText().toString(), Toast.LENGTH_SHORT).show();
                prefsEditr.putString("API_KEY", txtApiKey.getText().toString());
                prefsEditr.putString("API_SECRET", txtApiSecret.getText().toString());
                prefsEditr.putInt("UNIT_PRICE", Integer.parseInt(txtApiUnitPrice.getText().toString().replaceAll(",", "")));
                prefsEditr.apply();

                MainActivity.API_KEY = txtApiKey.getText().toString();
                MainActivity.API_SECRET = txtApiSecret.getText().toString();
                MainActivity.UNIT_PRICE = Integer.parseInt(txtApiUnitPrice.getText().toString().replaceAll(",", ""));

                finish();
            }
        });
    }
}

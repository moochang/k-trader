package com.example.k_trader;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.NumberTextWatcherForThousand;

public class SettingActivity extends AppCompatActivity {
    Button btnSave;
    EditText txtApiKey;
    EditText txtApiSecret;
    EditText txtApiUnitPrice;
    EditText txtApiTradeInterval;

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
        txtApiTradeInterval = findViewById(R.id.editTextTradeInterval);

        // Load app settings (data/data/(package_name)/shared_prefs/SharedPreference)
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        txtApiKey.setText(sharedPreferences.getString("API_KEY", ""));
        txtApiSecret.setText(sharedPreferences.getString("API_SECRET", ""));
        txtApiUnitPrice.setText(Integer.toString(sharedPreferences.getInt("UNIT_PRICE", GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)));
        txtApiTradeInterval.setText(Integer.toString(sharedPreferences.getInt("TRADE_INTERVAL", GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)));

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int tradeInterval = Integer.parseInt(txtApiTradeInterval.getText().toString().replaceAll(",", ""));
                if (tradeInterval < GlobalSettings.TRADE_INTERVAL_MIN_VALUE) {
                    Toast.makeText(SettingActivity.this, "거래 주기 값이 너무 작습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                int unitPrice = Integer.parseInt(txtApiUnitPrice.getText().toString().replaceAll(",", ""));
                if (unitPrice < GlobalSettings.UNIT_PRICE_MIN_VALUE) {
                    Toast.makeText(SettingActivity.this, "1회 거래 금액 값이 너무 작습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }

                SharedPreferences.Editor prefsEditr = sharedPreferences.edit();
//                Log.e("TAG", txtApiKey.getText().toString());
//                Toast.makeText(SettingActivity.this, txtApiKey.getText().toString(), Toast.LENGTH_SHORT).show();
                prefsEditr.putString("API_KEY", txtApiKey.getText().toString());
                prefsEditr.putString("API_SECRET", txtApiSecret.getText().toString());
                prefsEditr.putInt("UNIT_PRICE", unitPrice);
                prefsEditr.putInt("TRADE_INTERVAL", tradeInterval);
                prefsEditr.apply();

                GlobalSettings.getInstance().setApiKey(txtApiKey.getText().toString());
                GlobalSettings.getInstance().setApiSecret(txtApiSecret.getText().toString());
                GlobalSettings.getInstance().setUnitPrice(unitPrice);
                GlobalSettings.getInstance().setTradeInterval(tradeInterval);

                finish();
            }
        });
    }
}

package com.example.k_trader;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.k_trader.base.GlobalSettings;
import com.example.k_trader.base.NumberTextWatcherForThousand;

public class SettingActivity extends AppCompatActivity {
    Button btnSave;
    EditText txtApiKey;
    EditText txtApiSecret;
    EditText txtUnitPrice;
    EditText txtTradeInterval;
    EditText txtEarningRate;
    EditText txtSlotIntervalRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        
        // Status bar 색상을 App bar와 동일하게 설정
        getWindow().setStatusBarColor(Color.parseColor("#FF8C42"));
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnSave = findViewById(R.id.buttonSave);
        txtApiKey = findViewById(R.id.editTextApiKey);
        txtApiSecret = findViewById(R.id.editTextApiSecret);
        txtUnitPrice = findViewById(R.id.editTextUnitPrice);
        txtUnitPrice.addTextChangedListener(new NumberTextWatcherForThousand(txtUnitPrice));
        txtTradeInterval = findViewById(R.id.editTextTradingInterval);
        txtEarningRate = findViewById(R.id.editTextEarningRate);
        txtSlotIntervalRate = findViewById(R.id.editTextSlotIntervalRate);

        // Load app settings (data/data/(package_name)/shared_prefs/SharedPreference)
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        txtApiKey.setText(sharedPreferences.getString(GlobalSettings.API_KEY_KEY_NAME, ""));
        txtApiSecret.setText(sharedPreferences.getString(GlobalSettings.API_SECRET_KEY_NAME, ""));
        txtUnitPrice.setText(Integer.toString(sharedPreferences.getInt(GlobalSettings.UNIT_PRICE_KEY_NAME, GlobalSettings.UNIT_PRICE_DEFAULT_VALUE)));
        txtTradeInterval.setText(Integer.toString(sharedPreferences.getInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, GlobalSettings.TRADE_INTERVAL_DEFAULT_VALUE)));
        txtEarningRate.setText(Float.toString(sharedPreferences.getFloat(GlobalSettings.EARNING_RATE_KEY_NAME, GlobalSettings.EARNING_RATE_DEFAULT_VALUE)));
        txtSlotIntervalRate.setText(Float.toString(sharedPreferences.getFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, GlobalSettings.SLOT_INTERVAL_RATE_DEFAULT_VALUE)));

        btnSave.setOnClickListener(v -> {
            int tradeInterval = Integer.parseInt(txtTradeInterval.getText().toString().replaceAll(",", ""));
            if (tradeInterval < GlobalSettings.TRADE_INTERVAL_MIN_VALUE) {
                Toast.makeText(SettingActivity.this, "거래 주기 값이 너무 작습니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            int unitPrice = Integer.parseInt(txtUnitPrice.getText().toString().replaceAll(",", ""));
            if (unitPrice < GlobalSettings.UNIT_PRICE_MIN_VALUE) {
                Toast.makeText(SettingActivity.this, "1회 거래 금액 값이 너무 작습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor prefsEditr = sharedPreferences.edit();
            prefsEditr.putString(GlobalSettings.API_KEY_KEY_NAME, txtApiKey.getText().toString());
            prefsEditr.putString(GlobalSettings.API_SECRET_KEY_NAME, txtApiSecret.getText().toString());
            prefsEditr.putInt(GlobalSettings.UNIT_PRICE_KEY_NAME, unitPrice);
            prefsEditr.putInt(GlobalSettings.TRADE_INTERVAL_KEY_NAME, tradeInterval);
            prefsEditr.putFloat(GlobalSettings.EARNING_RATE_KEY_NAME, Float.parseFloat(txtEarningRate.getText().toString()));
            prefsEditr.putFloat(GlobalSettings.SLOT_INTERVAL_RATE_KEY_NAME, Float.parseFloat(txtSlotIntervalRate.getText().toString()));
            prefsEditr.apply();

            GlobalSettings.getInstance().setApiKey(txtApiKey.getText().toString())
                                        .setApiSecret(txtApiSecret.getText().toString())
                                        .setUnitPrice(unitPrice)
                                        .setTradeInterval(tradeInterval)
                                        .setEarningRate(Float.parseFloat(txtEarningRate.getText().toString()))
                                        .setSlotIntervalRate(Float.parseFloat(txtSlotIntervalRate.getText().toString()));

            Toast.makeText(SettingActivity.this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}

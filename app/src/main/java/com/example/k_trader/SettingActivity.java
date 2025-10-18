package com.example.k_trader;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
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
        
        // 테마에 따라 Status Bar 색상 동적 설정
        setStatusBarColorByTheme();
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // App bar 색상 설정
        setAppBarColorByTheme();

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
    
    /**
     * 현재 테마에 따라 Status Bar 색상을 설정하는 메서드
     */
    private void setStatusBarColorByTheme() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 현재 테마가 Light 테마인지 확인
            boolean isLightTheme = isLightTheme();
            
            int statusBarColor;
            if (isLightTheme) {
                statusBarColor = getResources().getColor(R.color.status_bar_light);
                // Light 테마에서는 Status bar 아이콘을 어둡게 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            } else {
                statusBarColor = getResources().getColor(R.color.status_bar_dark);
                // Dark 테마에서는 Status bar 아이콘을 밝게 설정
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    getWindow().getDecorView().setSystemUiVisibility(0);
                }
            }
            
            getWindow().setStatusBarColor(statusBarColor);
        }
    }
    
    /**
     * 현재 테마가 Light 테마인지 확인하는 메서드
     */
    private boolean isLightTheme() {
        // Android 시스템의 다크 모드 설정 확인
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags != android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }
    
    /**
     * 현재 테마에 따라 App Bar 색상을 설정하는 메서드
     */
    private void setAppBarColorByTheme() {
        // 현재 테마가 Light 테마인지 확인
        boolean isLightTheme = isLightTheme();
        
        int appBarColor;
        if (isLightTheme) {
            appBarColor = getResources().getColor(R.color.app_bar_light);
        } else {
            appBarColor = getResources().getColor(R.color.app_bar_dark);
        }
        
        // Toolbar 색상 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setBackgroundColor(appBarColor);
            
            // Toolbar 그림자 효과 제거
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                toolbar.setElevation(0);
            }
            
            // 텍스트 색상 설정 (흰색 배경에서는 어두운 색 사용)
            if (isLightTheme) {
                // Light 테마에서는 어두운 텍스트 색상 사용
                toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
            } else {
                // Dark 테마에서는 밝은 텍스트 색상 사용
                toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            }
        }
        
        // AppBarLayout 그림자 효과 제거
        android.support.design.widget.AppBarLayout appBarLayout = findViewById(R.id.appbar);
        if (appBarLayout != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            appBarLayout.setElevation(0);
        }
    }
}

package com.example.k_trader;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    int MAX_PAGE = 3;
    Fragment cur_fragment = new Fragment();
    ViewPager viewPager;

    public static String API_KEY;
    public static String API_SECRET;
    public static int UNIT_PRICE;

    public static final String BROADCAST_PROGRESS_MESSAGE = "PROGRESS_MESSAGE";
    private static Timer mTimer;
    private static ProgressDialog dialog;
    static int progress;

    JobScheduler jobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        int id = 0;
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new adapter(getSupportFragmentManager()));

        IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(BROADCAST_PROGRESS_MESSAGE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(new MyReceiver(), theFilter);

        // Load app settings (data/data/(package_name)/shared_prefs/SharedPreference)
        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        UNIT_PRICE = sharedPreferences.getInt("UNIT_PRICE", 0);
        API_KEY = sharedPreferences.getString("API_KEY", "");
        API_SECRET = sharedPreferences.getString("API_SECRET", "");

//        if (UNIT_PRICE == 0 || API_KEY.isEmpty() || API_SECRET.isEmpty()) {
//            // Launch setting activity
//            SharedPreferences.Editor editor = sharedPreferences.edit();
//            editor.putInt("UNIT_PRICE", 1 * 1000 * 1000);
//            editor.commit();
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Resources res = getResources();

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setContentTitle("Exit program")
                .setContentText("OnDestroy is called")
                .setTicker("OnDestroy is called")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL);

        builder.setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm != null)
            nm.notify((int) System.currentTimeMillis(), builder.build());
    }

    private class adapter extends FragmentPagerAdapter {
        public adapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position < 0 || MAX_PAGE <= position)
                return null;

            switch (position) {
                case 0:
                    cur_fragment = new MainPage();
                    break;
                case 1:
                    cur_fragment = new PlacedOrderPage();
                    break;
                case 2:
                    cur_fragment = new ProcessedOrderPage();
                    break;
            }
            return cur_fragment;
        }

        @Override
        public int getCount() {
            return MAX_PAGE;
        }
    }

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            progress = intent.getIntExtra("progress", 0);
//            Log.d("KTrader", "received progress : " + String.valueOf(progress));

            mTimer = new Timer();
            mTimer.schedule(new MyTask(), 0, progress);

            dialog = ProgressDialog.show(MainActivity.this, "Wait",
                    "다음 Request가 허용될 때까지 대기.. 최대 10초", true);
        }
    }

    static class MyTask extends TimerTask {
        public void run() {
            // Do what you wish here with the dialog
            if (dialog != null) {
                dialog.cancel();
                dialog.dismiss();
            }
        }
    }

    @Override
    public void onBackPressed() {
//        Toast.makeText(this, "Back button pressed.", Toast.LENGTH_SHORT).show();
        // ��׶��� ������ ��ȯ�Ѵ�.
        moveTaskToBack(true);
    }
}

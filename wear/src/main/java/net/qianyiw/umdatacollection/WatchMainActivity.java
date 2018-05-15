package net.qianyiw.umdatacollection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;

import java.util.ArrayList;

public class
WatchMainActivity extends Activity implements View.OnClickListener, DataApi.DataListener {

    BroadcastReceiver broadcastReceiver;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;
    public static final String MY_PREFS_NAME = "MyPrefsFile";
    Vibrator vibrator;

    ImageView heart_image;
    TextView heart_rate_val, hr_val, light_val, acc_val, gyo_val;
    boolean sensorOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_main);

        // keep always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        heart_image = (ImageView) findViewById(R.id.heartrate_image);
        heart_image.setOnClickListener(this);
        heart_rate_val = (TextView) findViewById(R.id.heart_rate_value);
        hr_val = (TextView)findViewById(R.id.hr_value);
        light_val = (TextView)findViewById(R.id.light_value);
        acc_val = (TextView)findViewById(R.id.acc_val);
        gyo_val = (TextView)findViewById(R.id.gyo_val);
        editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sensorOn = prefs.getBoolean("sensor status", false);

        if (sensorOn) {
            heart_image.setImageResource(R.drawable.heart_color_big);
            heart_image.startAnimation(AnimationUtils.loadAnimation(this, R.anim.animation));
//            registerReceiver(broadcastReceiver, new IntentFilter(HearRateService.BROADCAST_ACTION));
        } else {
            heart_image.setImageResource(R.drawable.heart_rate_off);
            heart_image.clearAnimation();
        }

//        broadcastReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                updateUI(intent);
//            }
//        };
//        registerReceiver(broadcastReceiver, new IntentFilter(HearRateService.BROADCAST_ACTION));
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateUI(intent);
            }
        };

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.heartrate_image:
                if (!sensorOn) {
                    vibrator.vibrate(50);
                    sensorOn = true;
                    editor.putBoolean("sensor status", sensorOn).commit();
                    heart_image.setImageResource(R.drawable.heart_color_big);
                    heart_image.startAnimation(AnimationUtils.loadAnimation(this, R.anim.animation));
                    registerReceiver(broadcastReceiver, new IntentFilter(SensorService.BROADCAST_ACTION));
                    startService(new Intent(getBaseContext(), SensorService.class));
                } else {
                    vibrator.vibrate(50);
                    sensorOn = false;
                    editor.putBoolean("sensor status", sensorOn).commit();
                    heart_image.setImageResource(R.drawable.heart_rate_off);
                    heart_image.clearAnimation();
                    unregisterReceiver(broadcastReceiver);
                    stopService(new Intent(getBaseContext(), SensorService.class));
                    heart_rate_val.setText("");
                }
                break;
        }
    }

    private void updateUI(Intent intent) {
//        heart_image.startAnimation(AnimationUtils.loadAnimation(this, R.anim.animation));
        String hr = intent.getStringExtra("HR");
        String lightVal = intent.getStringExtra("Light");
        String acc = intent.getStringExtra("ACC");
        String gyr = intent.getStringExtra("GYR");
        heart_rate_val.setText(hr);
        light_val.setText(lightVal);
        acc_val.setText(acc);
        gyo_val.setText(gyr);
    }
}

package net.qianyiw.umdatacollection;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SensorService extends Service implements SensorEventListener{

    Sensor mHeartRateSensor,senAccelerometer, senGyroscope, senTemperature, senLight;;
    SensorManager mSensorManager;
    MessageServer myMessage;
    public static final String BROADCAST_ACTION = "com.websmithing.broadcasttest.displayevent";
//    private final Handler handler = new Handler();
    Intent intent;

    Timer timer;
    TimerTask timerTask;

    String accelerometer, gyroscope, hrStr;
    public SensorService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Let it continue running until it is stopped.
        // start heart rate sensor

        accelerometer = "";
        gyroscope = "";
        hrStr = "";
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        senAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        senLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (mHeartRateSensor == null) {
            List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
            for (Sensor sensor1 : sensors) {
                Log.i("Sensor Type", sensor1.getName() + ": " + sensor1.getType());
            }
        }
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, senGyroscope , SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, senLight , SensorManager.SENSOR_DELAY_NORMAL);
        myMessage = new MessageServer(this);
        myMessage.myApiClient.connect();
        startTimer();
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
    }

    private void DisplaySensorInfo(String msg) {
        if(msg.contains("HR_Watch")){
            intent.putExtra("HR", msg);
        }else if(msg.contains("ACC")){
            intent.putExtra("ACC", msg);
        }else if(msg.contains("GYR")){
            intent.putExtra("GYR", msg);
        }else if(msg.contains("Light")){
            intent.putExtra("Light", msg);

        }
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        stopTimerTask();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            hrStr = "HR_Watch:"+(int) event.values[0];
        }
        else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            accelerometer = "ACC_Watch:"+"x:"+x+"y:"+y+"z:"+z;


            /*
            JSONObject acc_json = new JSONObject();
            try {
                acc_json.put("x", x);
                acc_json.put("y",y);
                acc_json.put("z",z);
            }catch (Exception e) {
                ;
            }
             */
        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            gyroscope = "GYR_Watch:"+"x:"+x+"y:"+y+"z:"+z;
        }
        else if (event.sensor.getType() == Sensor.TYPE_LIGHT){
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 0ms the TimerTask will run every 5000ms
        timer.schedule(timerTask, 0, 1000); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                myMessage.sendMessage(hrStr);
                DisplaySensorInfo(hrStr);
                myMessage.sendMessage(accelerometer);
                DisplaySensorInfo(accelerometer);
                myMessage.sendMessage(gyroscope);
                DisplaySensorInfo(gyroscope);
            }
        };
    }
}

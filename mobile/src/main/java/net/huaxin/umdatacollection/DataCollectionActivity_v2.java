package net.huaxin.umdatacollection;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import net.huaxin.umdatacollection.BluetoothService;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashMap;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import net.huaxin.umdatacollection.CalDistance;

public class DataCollectionActivity_v2 extends AppCompatActivity implements SensorEventListener, MessageApi.MessageListener, AdapterView.OnItemClickListener {

    TextView userInfo, phoneInfo, gpsInfo, wifiInfo, accelerometerInfo, gyroscopeInfo, heartRateInfo, tempInfo, lightInfo, accTextWatch, gyrTextWatch, vehicleInfo;
    LocationManager locationManager;
    Location location;
    String path = "http://HostNeedToUpdate/SaveData.php";

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String user, freqStr;
    boolean connection;
    int freq;
    File file;
    FileOutputStream fos;
    Map<String, String> collectedData=new HashMap<String, String>();
    Timer timer;
    TimerTask timerTask;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer, senGyroscope, senTemperature, senLight;
    private static final String WEAR_PATH = "/from-watch";
    GoogleApiClient apiClient;

    // data collected
    String myDeviceModel, ssid;
    JSONObject gyroscope, accelerometer;
    String heart_rate, gyrWatch, accWatch, lightWatch;
    String vehicle_info;
    double lat, lng, lightVal, tempVal;
    float x, y, z;

    // for bluetooth
    ListView deviceList;
    ArrayList<BluetoothDevice> deviceAround = new ArrayList<BluetoothDevice>();
    ArrayAdapter<String> listAdapter;
    BroadcastReceiver messageReceiver, deviceReceiver;
    BluetoothAdapter bluetoothAdapter;
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    private BluetoothService bluetoothServiceReference;
    String strPhone;
    String strWatch;
    String strVeh;

    String dataEncryptSettings;
    String contentAwareSettings;
    RSAPublicKey rsaPublicKey;
    RSAPrivateKey rsaPrivateKey;
    Cipher cipher_encrypt;

    SharedPreferences preferences;
    String[] selectedFields;

    SQLiteDatabase mSQLiteDatabase;

    private ArrayList<String> assignedLocs;
    private ArrayList<String> topLocs;
    float preservedDistance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        apiClient.connect();
        Wearable.MessageApi.addListener(apiClient, this);

        myDeviceModel = "";
        ssid = "";
        heart_rate = "";
        gyrWatch = "";
        accWatch = "";
        lightWatch = "";
        vehicle_info = "";

        tempVal = 0;
        lightVal = 0;
        lat = 0;
        lng = 0;
        x = 0; y = 0; z = 0;

        strPhone = "";
        strWatch = "";
        strVeh = "";

        Bundle extras = getIntent().getExtras();
        user = extras.getString("user information");
        freqStr = extras.getString("frequency");
        freq = Integer.parseInt(freqStr)*1000;
        connection = extras.getBoolean("connection");
        if (!connection){
            try{
                createFile();
            } catch (Exception e) {
                ;
            }
        }

        userInfo = (TextView) findViewById(R.id.user_info);
        userInfo.setText("UserInfo: "+user);
        phoneInfo = (TextView) findViewById(R.id.phone_info);
        gpsInfo = (TextView) findViewById(R.id.gps_location);
        wifiInfo = (TextView) findViewById(R.id.wifi_info);
        accelerometerInfo = (TextView)findViewById(R.id.accelerometer);
        gyroscopeInfo = (TextView)findViewById(R.id.gyroscope);
        heartRateInfo = (TextView)findViewById(R.id.heart_rate);
        tempInfo = (TextView)findViewById(R.id.temperature);
        lightInfo = (TextView)findViewById(R.id.light);
        accTextWatch = (TextView)findViewById(R.id.acc_watch);
        gyrTextWatch = (TextView)findViewById(R.id.gyr_watch);
        vehicleInfo = (TextView)findViewById(R.id.vehile_info);
        findPhoneInfo(getApplicationContext());
//        getCurrentSsid(getApplicationContext());
        findGpsInfo();
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senGyroscope = senSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senTemperature = senSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        senLight = senSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // initial bluetooth setting
        initialBluetooth();


        preferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        dataEncryptSettings = preferences.getString("data_encrypt_settings", "-1");
        contentAwareSettings = preferences.getString("data_collect_settings", "-1");
        preservedDistance = 1;
        if (contentAwareSettings.equals("1") || contentAwareSettings.equals("2")){
            if (isDouble(preferences.getString("preserve_distance", "1"))) {
                preservedDistance = Float.valueOf(preferences.getString("preserve_distance", "1"));
            } else {
                Toast.makeText(this, "Preserved distance is not a number! Set to 1 mile", 0).show();
                preservedDistance = 1;
            }
        }



        if (dataEncryptSettings.equals("3") || dataEncryptSettings.equals("1")){
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
            rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(rsaPublicKey.getEncoded());
            KeyFactory keyFactory= KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            cipher_encrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding");
            cipher_encrypt.init(Cipher.ENCRYPT_MODE, publicKey);

            /*
            PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(rsaPrivateKey.getEncoded());
            KeyFactory keyFactory2 = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory2.generatePrivate(pkcs8EncodedKeySpec);
            Cipher cipher5 = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding");
            cipher5.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] result2 = cipher5.doFinal(result);
            byte[] result3 = cipher5.doFinal(result1);
            */

            if ( dataEncryptSettings.equals("1")) {
                Set<String> selections = preferences.getStringSet("mode_repeat", null);
                selectedFields = selections.toArray(new String[]{});
            }
            }catch (Exception e){}
        }

        mSQLiteDatabase = this.openOrCreateDatabase("location.db", MODE_PRIVATE, null);
        //mSQLiteDatabase.execSQL("DROP TABLE Locations");
        try{
            //mSQLiteDatabase.execSQL("DROP TABLE Locations");
            //mSQLiteDatabase.execSQL("create table Locations (" +
            //        "Lat REAL, " +
            //        "Lon REAL)");


        Cursor cur = mSQLiteDatabase.rawQuery("SELECT * FROM Locations", null);
        assignedLocs = new ArrayList<String>();
        if(cur != null){
            if(cur.moveToFirst()){
                do{
                    HashMap<String, String> map = new HashMap<String, String>();
                    assignedLocs.add(cur.getFloat(cur.getColumnIndex("Lat"))+" , "+cur.getFloat(cur.getColumnIndex("Lon")));
                }while(cur.moveToNext());
            }
        }
        }catch(Exception e){}
        try{
            //mSQLiteDatabase.execSQL("create table TopLocations (" +
            //        "Lat REAL, " +
             //       "Lon REAL)");
         //   mSQLiteDatabase.execSQL("DROP TABLE TopLocations");

        Cursor cur1 = mSQLiteDatabase.rawQuery("SELECT * FROM TopLocations order by CSize DESC limit 2", null);
        topLocs = new ArrayList<String>();
        if(cur1 != null){
            if(cur1.moveToFirst()){
                do{
                    HashMap<String, String> map = new HashMap<String, String>();
                    topLocs.add(cur1.getFloat(cur1.getColumnIndex("Lat"))+" , "+cur1.getFloat(cur1.getColumnIndex("Lon")));
                }while(cur1.moveToNext());
            }
        }
        }catch(Exception e){}
    }

    private void createFile() throws IOException {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this,"No storage connected!",0).show();
            return;
        }
        File sd = Environment.getExternalStorageDirectory();
        String path = sd.getPath()+"/UMD_DataCollections";
        File folder=new File(path);
        if(!folder.exists())
            folder.mkdir();
        file = new File(Environment.getExternalStorageDirectory()+"/UMD_DataCollections", user.substring(0,user.length()-1)+"_"+sdf.format(new Date()));
        try {
            fos = new FileOutputStream(file);
        } catch (Exception e) {
            ;
        }
    }


    private void initialBluetooth() {
        listAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,0);

        deviceList = (ListView)findViewById(R.id.deviceList);
        deviceList.setOnItemClickListener(this);
        deviceList.setAdapter(listAdapter);

        pairedDevices = new ArrayList<String>();
        // discover bluetooth device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT);
        }
        else
        {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }

        getPairedDevices();
        discoverDevice(); // find devices around

        deviceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receiveInfo(intent);
            }
        };
        registerReceiver(deviceReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                receiveMessage(intent);
            }
        };
        registerReceiver(messageReceiver, new IntentFilter(BluetoothService.BROADCAST_ACTION));

        bluetoothServiceReference = new BluetoothService();
    }

// *************************bluetooth methods***************************************************
private void getPairedDevices() {

    devicesArray = bluetoothAdapter.getBondedDevices();
    if(devicesArray.size()>0){
        for(BluetoothDevice device:devicesArray){
            Log.v("getPairedDevice",device.getName());
            pairedDevices.add(device.getName());
        }
    }
}

    private void discoverDevice() {
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();
    }

    private void receiveInfo(Intent intent) {
        // When discovery finds a device
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            deviceAround.add(device);

            String pairedString = "";
            for(int a = 0; a < pairedDevices.size(); a++){
                //Check Here
                Log.v("paired device",pairedDevices.get(a));
                if (device.getName() != null) {
                    if (device.getName().equals(pairedDevices.get(a))) {
                        //append
                        pairedString = "(PAIRED)";
                        break;
                    }
                }
            }
            if(device.getName()!=null){
                listAdapter.add(device.getName() + pairedString + "\n" + device.getAddress());
            }
        }
    }

    private void receiveMessage(Intent intent) {
        String msg = intent.getStringExtra("INFO");
        vehicle_info = vehicle_info + ","+msg;
        vehicleInfo.setText(msg);
    }

    // *************************end bluetooth methods***************************************************
    // *************************fetch info methods***************************************************
    public void findPhoneInfo(Context context) {
        myDeviceModel = android.os.Build.MODEL;

        TelephonyManager manager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        String carrierName = manager.getNetworkOperatorName();

        phoneInfo.setText("Phone Info: "+myDeviceModel + " " + carrierName);

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
                wifiInfo.setText("Wifi Info: "+ssid);
                int idx1 = ssid.indexOf('"');
                int idx2 = ssid.indexOf('"', 2);
                ssid = ssid.substring(idx1+1,idx2);
            }
        }
    }

    public void getCurrentSsid(Context context) {
    }

    public void findGpsInfo() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
               ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        //if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
         //       checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        }
        else
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) { // show some info to user why you want this permission
                Toast.makeText(this, "Allow Location Permission to use this functionality.", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 123 /*LOCATION_PERMISSION_REQUEST_CODE*/);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 123 /*LOCATION_PERMISSION_REQUEST_CODE*/);
            }

            Toast.makeText(this,"permission not allowed hahah",0).show();
            Log.v("permission","permission not allowed");
        }
    }

    private LocationListener listener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location) {
            lat = location.getLatitude();
            lng = location.getLongitude();
            Log.v("location", "Lat: " + lat + " Lng: " + lng);
            //Toast.makeText(DataCollectionActivity_v2.this,"location change",0).show();
            gpsInfo.setText("Lat: " + lat + " Lng: " + lng);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onPause() {
        locationManager.removeUpdates(listener);
        senSensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senGyroscope , SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senTemperature , SensorManager.SENSOR_DELAY_NORMAL);
        senSensorManager.registerListener(this, senLight , SensorManager.SENSOR_DELAY_NORMAL);

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);
        startTimer();
        super.onResume();
    }

    public void startTimer() {
        //set a new Timer
        timer = new Timer();
        //initialize the TimerTask's job
        initializeTimerTask();
        //schedule the timer, after the first 0ms the TimerTask will run every 5000ms
        timer.schedule(timerTask, 0, freq); //
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();

            timer = null;
        }
        mSQLiteDatabase.close();
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                if ((int)lat!=0 && (int)lng!=0) {
                    ContentValues cv = new ContentValues();
                    cv.put("Lat", lat);
                    cv.put("Lon", lng);
                    try{
                        mSQLiteDatabase.execSQL("create table ForTopLocations (" +
                                "Lat REAL, " +
                                "Lon REAL)");
                    }catch(Exception e){}
                    try {
                        mSQLiteDatabase.insert("ForTopLocations", null, cv);
                    }catch (Exception e){;}
                }

                boolean flagForCollection = true;
                if (contentAwareSettings.equals("1")){
                    for (String j : assignedLocs) {
                        if (CalDistance.getDistance(Double.valueOf(j.split(",")[0]),Double.valueOf(j.split(",")[1]),Double.valueOf(lat),Double.valueOf(lng))<preservedDistance*1609){
                            flagForCollection = false;
                        }
                    }
                } else if (contentAwareSettings.equals("2")){
                    for (String j : topLocs) {
                        if (CalDistance.getDistance(Double.valueOf(j.split(",")[0]),Double.valueOf(j.split(",")[1]),Double.valueOf(lat),Double.valueOf(lng))<preservedDistance*1609){
                            flagForCollection = false;
                            Log.v("here","hereget??");
                        }
                    }
                }

                if (flagForCollection){
                    addList();
                    if (connection) {
                        PostDataToDatabase_v2 postData = new PostDataToDatabase_v2(collectedData, path, user);
                        postData.execute();
                    } else{
                        String strForOutput = collectedData.get("phoneData")+"\t"+collectedData.get("watchData")+"\t"+collectedData.get("vehicleData")+"\n";

                        byte[] buffer = strForOutput.getBytes();
                        try {
                            fos.write(buffer);
                        }catch(Exception e){}
                        //fos.close();
                        //Toast.makeText(DataCollectionActivity_v2.this, "文件写入成功", 0).show();
                    }
                }
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopTimerTask();
        try {
            fos.close();
        }catch(Exception e){}
        if(messageReceiver!=null){
            unregisterReceiver(messageReceiver);
        }
        if(deviceReceiver!=null){
            unregisterReceiver(deviceReceiver);
        }

        stopService(new Intent(getBaseContext(), BluetoothService.class));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            accelerometer = new JSONObject();
            accelerometer.put("x",x);
            accelerometer.put("y",y);
            accelerometer.put("z",z);
            accelerometerInfo.setText("PhoneACC:"+"x:"+x+"y:"+y+"z:"+z);
        }
        else if(mySensor.getType() == Sensor.TYPE_GYROSCOPE)
        {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            gyroscope = new JSONObject();
            gyroscope.put("x",x);
            gyroscope.put("y",y);
            gyroscope.put("z",z);
            gyroscopeInfo.setText("PhoneGYR:"+"x:"+x+"y:"+y+"z:"+z);
        }
        else if (mySensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE){
            tempVal = event.values[0];
            tempInfo.setText("Temperature: "+tempVal);
        }
        else if (mySensor.getType() == Sensor.TYPE_LIGHT){
            lightVal = event.values[0];
            lightInfo.setText("Light: "+lightVal);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void addList()
    {
        if (dataEncryptSettings.equals("3")){
            try {
                JSONObject phoneJSON = new JSONObject();
                String currentDateTime = sdf.format(new Date());

                phoneJSON.put("PhoneWifi",cipher_encrypt.doFinal(ssid.getBytes()));
                phoneJSON.put("PhoneTime",cipher_encrypt.doFinal(currentDateTime.getBytes()));
                phoneJSON.put("PhoneModel", cipher_encrypt.doFinal(myDeviceModel.getBytes()));
                JSONObject gpsJSON = new JSONObject();
                gpsJSON.put("lat", lat);
                gpsJSON.put("lng", lng);
                phoneJSON.put("PhoneGPS", cipher_encrypt.doFinal(gpsJSON.toString().getBytes()));
                phoneJSON.put("PhoneTemperature", cipher_encrypt.doFinal(String.valueOf(tempVal).getBytes()));
                phoneJSON.put("PhoneLight", cipher_encrypt.doFinal(String.valueOf(lightVal).getBytes()));
                phoneJSON.put("PhoneGYR", cipher_encrypt.doFinal(gyroscope.toString().getBytes()));
                phoneJSON.put("PhoneACC", cipher_encrypt.doFinal(accelerometer.toString().getBytes()));
                collectedData.put("phoneData", phoneJSON.toString());
                JSONObject watchJSON = new JSONObject();
                watchJSON.put("WatchHeartRate", cipher_encrypt.doFinal(heart_rate.toString().getBytes()));
                watchJSON.put("WatchLight", cipher_encrypt.doFinal(lightWatch.toString().getBytes()));
                watchJSON.put("WatchACC", cipher_encrypt.doFinal(accWatch.toString().getBytes()));
                watchJSON.put("WatchGYR", cipher_encrypt.doFinal(gyrWatch.toString().getBytes()));
                collectedData.put("watchData", watchJSON.toString());
                collectedData.put("vehicleData", vehicle_info);
            }catch(Exception e){
                Log.e("something is wrong",e.toString());
            }
        }else{
            JSONObject phoneJSON = new JSONObject();
            String currentDateTime = sdf.format(new Date());
            phoneJSON.put("PhoneModel", myDeviceModel);
            phoneJSON.put("PhoneWifi",ssid );
            phoneJSON.put("PhoneTime",currentDateTime );
            JSONObject gpsJSON = new JSONObject();
          /*  if (location!=null){
                lat = location.getLatitude();
                lng = location.getLongitude();
                Log.v("location", "Lat: " + lat + " Lng: " + lng);
            }*/
            gpsJSON.put("lat",lat);
            gpsJSON.put("lng",lng);
            phoneJSON.put("PhoneGPS",gpsJSON);
            phoneJSON.put("PhoneTemperature",tempVal);
            phoneJSON.put("PhoneLight",lightVal);
            phoneJSON.put("PhoneGYR",gyroscope);
            phoneJSON.put("PhoneACC",accelerometer);
            JSONObject watchJSON = new JSONObject();
            watchJSON.put("WatchHeartRate",heart_rate);
            watchJSON.put("WatchLight",lightWatch);
            watchJSON.put("WatchACC",accWatch);
            watchJSON.put("WatchGYR",gyrWatch);
            if (dataEncryptSettings.equals("1")){
                for (int i = 0; i < selectedFields.length ; i++){
                    System.out.println("\ntest" + i +" : " + selectedFields[i]);
                    try {
                        if (selectedFields[i].indexOf("Phone")!=-1){
                            System.out.println("\ncipher" +cipher_encrypt.doFinal(phoneJSON.get(selectedFields[i]).toString().getBytes()));
                            phoneJSON.put(selectedFields[i],cipher_encrypt.doFinal(phoneJSON.get(selectedFields[i]).toString().getBytes()));
                        } else if (selectedFields[i].indexOf("Watch")!=-1){
                            watchJSON.put(selectedFields[i],cipher_encrypt.doFinal(watchJSON.get(selectedFields[i]).toString().getBytes()));
                       }
                    }catch (Exception e){}
                }
            }
            collectedData.put("phoneData",phoneJSON.toString());
            collectedData.put("watchData",watchJSON.toString());
            collectedData.put("vehicleData",vehicle_info);
        }
    }

    @Override
     public void onMessageReceived(final MessageEvent messageEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (messageEvent.getPath().equalsIgnoreCase(WEAR_PATH)) {
                    String msg = new String(messageEvent.getData());
                    Log.v("message from watch", msg);
                    // adas control
                    if (msg.contains("HR_Watch")) {
                        heart_rate = msg;
                        heartRateInfo.setText(heart_rate);
                    }
                    else if (msg.contains("ACC_Watch")) {
                        accWatch = msg;
                        accTextWatch.setText(accWatch);
                    }
                    else if (msg.contains("GYR_Watch")){
                        gyrWatch = msg;
                        gyrTextWatch.setText(gyrWatch);
                    }
                    else if (msg.contains("Light_Watch")){
                        lightWatch = msg;
                    }
                }
            }
        });
    }

    public static boolean isDouble(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[.\\d]*$");
        return pattern.matcher(str).matches();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        bluetoothAdapter.cancelDiscovery();
        BluetoothDevice device = deviceAround.get(i);
        startService(new Intent(getBaseContext(), BluetoothService.class));
        bluetoothServiceReference.connectDevice(device, this);
    }
}

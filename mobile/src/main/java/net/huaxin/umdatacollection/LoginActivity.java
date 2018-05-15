package net.huaxin.umdatacollection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import net.huaxin.umdatacollection.SettingsActivity.GeneralPreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EncodingUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener{

    Button startBT,uploadBT,settingsBT,topLocationBT;
    EditText userName, collectFreq;
    String myUserName, myFreq;
    String path = "http://HostNeedToUpdate/Login.php";
    String uploadPath = "http://HostNeedToUpdate/SaveData.php";
    //没有网络连接
    public static final int NETWORK_NONE = -1;
    //wifi连接
    public static final int NETWORK_WIFI = 1;
    //手机网络数据连接类型
    public static final int NETWORK_CELLULAR = 2;
    //public static final int NETWORN_3G = 3;
    //public static final int NETWORN_4G = 4;
    //public static final int NETWORN_MOBILE = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        /*
        if (Build.VERSION.SDK_INT > 9){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        */

        userName = (EditText)findViewById(R.id.user_name);
        collectFreq = (EditText)findViewById(R.id.collect_freq);
        startBT = (Button)findViewById(R.id.start);
        startBT.setOnClickListener(this);
        uploadBT = (Button)findViewById(R.id.upload_local_data);
        uploadBT.setOnClickListener(this);
        settingsBT = (Button)findViewById(R.id.settings);
        settingsBT.setOnClickListener(this);
        topLocationBT = (Button)findViewById(R.id.location_visualization);
        topLocationBT.setOnClickListener(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        String networkStateSettings = preferences.getString("network_state_settings", "-1");

        int networkState = getNetworkState(LoginActivity.this);
        if (networkState == NETWORK_WIFI && networkState == Integer.parseInt(networkStateSettings)){
            UploadLocalData uploadLocalData = new UploadLocalData(LoginActivity.this,path,uploadPath);
            uploadLocalData.execute();
        } else if (networkState > NETWORK_WIFI && networkState <=  Integer.parseInt(networkStateSettings) ) {
            UploadLocalData uploadLocalData = new UploadLocalData(LoginActivity.this,path,uploadPath);
            uploadLocalData.execute();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.start:
                postRequestFunction(v);
                break;
            case R.id.upload_local_data:
                uploadLocalDataFunction(v);
                break;
            case R.id.settings:
                Intent intent = new Intent();
                intent.setClass(LoginActivity.this , SettingsActivity.class );
                //used to hide headers
                //intent.putExtra( PreferenceActivity.EXTRA_SHOW_FRAGMENT, GeneralPreferenceFragment.class.getName() );
                //intent.putExtra( PreferenceActivity.EXTRA_NO_HEADERS, true );
                startActivity(intent);
                //getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
                break;
            case R.id.location_visualization:
                Intent intentForLocation = new Intent();
                intentForLocation.setClass(LoginActivity.this, MapsActivity.class);
                startActivity(intentForLocation);
                break;
        }
    }

    public void postRequestFunction(View v)
    {
        myUserName = String.valueOf(userName.getText());
        myFreq = String.valueOf(collectFreq.getText());
//        JSONObject post_info = new JSONObject();
        if(!myUserName.isEmpty()&&!myFreq.isEmpty())
        {
            if (Float.valueOf(myFreq)<5) {
                Toast.makeText(this, "Period should be equal or more than 5s", 0).show();
                return;
            }
//            try {
//                post_info.put("username" , myUserName);
//                post_info.put("tripnumber", myTripNumber);
//
//            }  catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            if (post_info.length() > 0) {
//                new SetUpCollectionTask(myUserName, myTripNumber).execute(String.valueOf(post_info));
//            }
            SetUpCollectionTask sendJsonDataToServer = new SetUpCollectionTask(myUserName,myFreq,LoginActivity.this,path);
            sendJsonDataToServer.execute();
           }
        else {
            Toast.makeText(this, "Please fill all the information !", 0).show();
        }

    }


    public void uploadLocalDataFunction(View v)
    {
        UploadLocalData uploadLocalData = new UploadLocalData(LoginActivity.this,path,uploadPath);
        uploadLocalData.execute();
    }

    public static int getNetworkState(Context context) {
        //获取系统的网络服务
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        //如果当前没有网络
        if (null == connManager)
            return NETWORK_NONE;

        //获取当前网络类型，如果为空，返回无网络
        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
        if (activeNetInfo == null || !activeNetInfo.isAvailable()) {
            return NETWORK_NONE;
        }

        // 判断是不是连接的是不是wifi
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (null != wifiInfo) {
            NetworkInfo.State state = wifiInfo.getState();
            if (null != state)
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    return NETWORK_WIFI;
                }
        }

        // 如果不是wifi，则判断当前连接的是运营商的哪种网络2g、3g、4g等
        TelephonyManager mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_CELLULAR;
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            /**
             From this link https://goo.gl/R2HOjR ..NETWORK_TYPE_EVDO_0 & NETWORK_TYPE_EVDO_A
             EV-DO is an evolution of the CDMA2000 (IS-2000) standard that supports high data rates.

             Where CDMA2000 https://goo.gl/1y10WI .CDMA2000 is a family of 3G[1] mobile technology standards for sending voice,
             data, and signaling data between mobile phones and cell sites.
             */
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            //Log.d("Type", "3g");
            //For 3g HSDPA , HSPAP(HSPA+) are main  networktype which are under 3g Network
            //But from other constants also it will 3g like HSPA,HSDPA etc which are in 3g case.
            //Some cases are added after  testing(real) in device with 3g enable data
            //and speed also matters to decide 3g network type
            //http://goo.gl/bhtVT
                return NETWORK_CELLULAR;
            case TelephonyManager.NETWORK_TYPE_LTE:
            //No specification for the 4g but from wiki
            //I found(LTE (Long-Term Evolution, commonly marketed as 4G LTE))
            //https://goo.gl/9t7yrR
                return NETWORK_CELLULAR;
            default:
                return NETWORK_NONE;
        }
    }
}

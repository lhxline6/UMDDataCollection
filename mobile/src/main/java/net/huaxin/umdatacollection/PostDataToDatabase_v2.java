package net.huaxin.umdatacollection;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PostDataToDatabase_v2 extends AsyncTask<Void, Void, Void> {
    String path, tableName, valueWatch, valuePhone, valueVehicle;
    PostDataToDatabase_v2(Map<String, String> collectedData, String path, String tableName)
    {
        this.valueWatch = collectedData.get("watchData");
        this.valuePhone = collectedData.get("phoneData");
        this.valueVehicle = collectedData.get("vehicleData");
        this.path = path;
        this.tableName = tableName;
    }

    @Override
    protected Void doInBackground(Void... params) {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(path);

        JSONObject jsonobj = new JSONObject();

        try {
            jsonobj.put("phone_value",  valuePhone);
            jsonobj.put("watch_value", valueWatch);
            jsonobj.put("vehicle_value",valueVehicle);
            jsonobj.put("tableName", tableName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("tableName", tableName));
        nameValuePairs.add(new BasicNameValuePair("phone_value", valuePhone));
        nameValuePairs.add(new BasicNameValuePair("watch_value", valueWatch));
        nameValuePairs.add(new BasicNameValuePair("vehicle_value", valueVehicle));

        Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());
        try {
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            //httpclient.execute(httppost);
            HttpResponse response = httpclient.execute(httppost);
            InputStream inputStream = response.getEntity().getContent();
            InputStreamToStringExample str = new InputStreamToStringExample();
            String serverResponse = str.getStringFromInputStream(inputStream);
            Log.v("response", "response -----" + serverResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}

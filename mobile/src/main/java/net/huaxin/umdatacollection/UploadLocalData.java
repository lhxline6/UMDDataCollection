package net.huaxin.umdatacollection;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class UploadLocalData extends AsyncTask <String, String, String>{

    String TAG = "UploadLocalData Class";
    String loginPath,savedataPath;
    String userName, collectFreq;
    Context context;
    boolean connection = false;
    ProgressDialog progressDialog;
    int idx;

    UploadLocalData(Context context, String loginPath,String savedataPath)
    {
        this.context = context;
        this.loginPath = loginPath;
        this.savedataPath = savedataPath;
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(context);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("Processing");
        progressDialog.setMessage("Please wait...");
        progressDialog.show();
    }

    @Override
    protected String doInBackground(String... params) {
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            return null;
        }
        File folder=new File(Environment.getExternalStorageDirectory().getPath()+"/UMD_DataCollections/");
        if(!folder.exists()){
            return null;
        }
        File[] files = new File(Environment.getExternalStorageDirectory().getPath()+"/UMD_DataCollections/").listFiles();
        if (files.length==0){
            return null;
        }
        for (File f : files){
            String[] splitedName = f.getName().split("_");
            if (!splitedName[splitedName.length-1].equals("uploaded")){
                try{
                    Log.v("checked",splitedName[splitedName.length-1]);
                    Log.v("Readfile correct",f.getName());

                    String tableName = splitedName[0];

                    Log.v("tableName",tableName);


                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httppost = new HttpPost(loginPath);
                    try {
                        JSONObject jsonobj = new JSONObject();
                        jsonobj.put("userName", tableName);
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                        nameValuePairs.add(new BasicNameValuePair("req", jsonobj.toString()));
                        Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());
                        // Use UrlEncodedFormEntity to send in proper format which we need
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                        // Execute HTTP Post Request
                        try{
                            HttpResponse response = httpclient.execute(httppost);
                            InputStream inputStream = response.getEntity().getContent();
                            InputStreamToStringExample str = new InputStreamToStringExample();
                            String serverResponse = str.getStringFromInputStream(inputStream);
                            Log.v("Tag", "response -----" + serverResponse);
                            idx = Integer.parseInt(serverResponse);
                            Log.v("tag", "idx -----" + idx);
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    InputStream instream = new FileInputStream(Environment.getExternalStorageDirectory().getPath()+"/UMD_DataCollections/"+f.getName());
                    if (instream != null)
                    {
                        InputStreamReader inputreader = new InputStreamReader(instream);
                        BufferedReader buffreader = new BufferedReader(inputreader);
                        String line;
                        //分行读取
                        while (( line = buffreader.readLine()) != null) {
                            Map<String, String> collectedData=new HashMap<String, String>();
                            String valuePhone = line.split("\t")[0];
                            String valueWatch = line.split("\t")[1];
                            String valueVehicle = "";
                            if (line.split("\t").length > 2) {
                                valueVehicle = line.split("\t")[2];
                            }

                            HttpClient httpclient1 = new DefaultHttpClient();
                            HttpPost httppost1 = new HttpPost(savedataPath);

                            JSONObject jsonobj = new JSONObject();

                            try {
                                jsonobj.put("phone_value",  valuePhone);
                                jsonobj.put("watch_value", valueWatch);
                                jsonobj.put("vehicle_value",valueVehicle);
                                jsonobj.put("tableName", tableName+String.valueOf(idx));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                            nameValuePairs.add(new BasicNameValuePair("upload", jsonobj.toString()));
                            Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());
                            try {
                                httppost1.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            try {
                                HttpResponse response = httpclient1.execute(httppost1);
                                InputStream inputStream = response.getEntity().getContent();
                                InputStreamToStringExample str = new InputStreamToStringExample();
                                String serverResponse = str.getStringFromInputStream(inputStream);
                                Log.v("response", "response -----" + serverResponse);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        instream.close();
                    }
                    f.renameTo(new File(Environment.getExternalStorageDirectory()+"/UMD_DataCollections", f.getName()+"_uploaded"));
                    Log.v("Rename",f.getName());
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    return null;
    }

    @Override
    protected void onPostExecute(String s) {
        progressDialog.dismiss();
        super.onPostExecute(s);
    }
}
